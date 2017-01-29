/*
 * Copyright [2017] [Alexander Reelsen]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.spinscale.maxcube.client;

import de.spinscale.maxcube.data.Generator;
import de.spinscale.maxcube.data.Parser;
import de.spinscale.maxcube.entities.Cube;
import de.spinscale.maxcube.entities.Room;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.LineDelimiter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;

import static java.nio.charset.StandardCharsets.UTF_8;

public class MinaCubeClient implements CubeClient {

    private final AttributeKey CUBE = new AttributeKey(getClass(), "context");
    private final AttributeKey BOOST_RESPONSE = new AttributeKey(getClass(), "boostResponse");

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String hostname;
    private final int port;
    private final NioSocketConnector connector;
    private final ConnectHandler handler;
    private IoSession session;

    public MinaCubeClient(String hostname) {
        this(hostname, 62910);
    }

    public MinaCubeClient(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
        connector = new NioSocketConnector(1);
        connector.setConnectTimeoutMillis(2000);

        connector.getFilterChain().addLast("logger", new LoggingFilter());
        TextLineCodecFactory codecFactory = new TextLineCodecFactory(UTF_8, LineDelimiter.CRLF, LineDelimiter.AUTO);
        codecFactory.setDecoderMaxLineLength(4096);
        codecFactory.setEncoderMaxLineLength(4096);
        connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(codecFactory));

        handler = new ConnectHandler();
        connector.setHandler(handler);
    }

    public Cube connect() throws Exception {
        ConnectFuture future = connector.connect(new InetSocketAddress(hostname, port));
        future.awaitUninterruptibly();
        session = future.getSession();
        handler.awaitAndReset();
        return (Cube) session.getAttribute(CUBE);
    }

    @Override
    public boolean boost(Room room) throws Exception {
        String data = Generator.writeBoostRequest(room);
        WriteFuture future = session.write(data + "\r\n");
        boolean successfulWait = future.awaitUninterruptibly(10000);
        if (!successfulWait) {
            logger.debug("Was not able to write data to cube");
            return false;
        }

        handler.awaitAndReset();
        return (boolean) session.getAttribute(BOOST_RESPONSE);
    }

    @Override
    public boolean holiday(Room room, LocalDateTime endTime, int temperature) throws Exception {
        throw new RuntimeException("Not yet implemented");
    }

    @Override
    public void close() throws IOException {
        session.write("q:\r\n".getBytes(UTF_8));
        session.closeOnFlush().awaitUninterruptibly();
        connector.dispose(true);
    }

    private class ConnectHandler extends IoHandlerAdapter {

        private final Parser parser = new Parser();
        private CountDownLatch latch = new CountDownLatch(1);

        @Override
        public void sessionOpened(IoSession session) throws Exception {
            logger.debug("Connected to {}" + session.getRemoteAddress());
        }

        public void awaitAndReset() throws InterruptedException {
            latch.await();
            latch = new CountDownLatch(1);
        }

        @Override
        public void messageReceived(IoSession session, Object message) throws Exception {
            // no cube in session implies there must be a header line
            Cube cube = (Cube) session.getAttribute(CUBE);
            String line = (String) message;
            logger.info("### PARSING [{}]", message);
            if (cube == null) {
                cube = parser.parseHeader(line);
                session.setAttribute(CUBE, cube);
            } else {
                if (message != null && ((String) message).length() > 0) {
                    parser.parse(cube, line);
                    if (line.startsWith("L:")) {
                        latch.countDown();
                    } else if (line.startsWith("S:")) {
                        boolean successfulBoost = parser.parseResponseS(line);
                        session.setAttribute(BOOST_RESPONSE, successfulBoost);
                        latch.countDown();
                    }
                }
            }
        }

        @Override
        public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
            logger.error("Exception in session from [{}<->{}]", cause, session.getLocalAddress(), session.getRemoteAddress());
            session.closeNow();
        }
    }
}
