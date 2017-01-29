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

import com.carrotsearch.randomizedtesting.annotations.Repeat;
import de.spinscale.maxcube.data.ParserTest;
import de.spinscale.maxcube.entities.Cube;
import de.spinscale.maxcube.test.CubeTestCase;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.LineDelimiter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;

import static java.lang.Integer.toHexString;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@Ignore
public abstract class CubeClientTest extends CubeTestCase {

    private CubeServer server;
    private CubeServerHandler handler;

    @Before
    public void startServer() throws IOException {
        handler = new CubeServerHandler();
        server = new CubeServer();
        server.start();
    }

    @After
    public void stopServer() throws IOException, InterruptedException {
        server.close();
    }

    abstract CubeClient getCubeClient(String host, int port);

    @Test
    public void testClient() throws Exception {
        try (CubeClient client = getCubeClient("localhost", server.getPort())) {
            Cube cube = client.connect();
            assertThat(cube.getRooms(), hasSize(5));
        }
    }

    @Test
    public void testBoost() throws Exception {
        boolean successfulResponse = randomBoolean();
        int dutyCycle = randomIntBetween(1, 100);
        int freeMemorySlots = randomIntBetween(1, 255);
        handler.configureBoostResponse(dutyCycle, successfulResponse, freeMemorySlots);

        try (CubeClient client = getCubeClient("localhost", server.getPort())) {
            Cube cube = client.connect();
            boolean boostConfigured = client.boost(cube.getRooms().get(0));
            assertThat(boostConfigured, is(successfulResponse));
        }
    }

    @Test
    public void testHolidayMode() throws Exception {
        // holiday mode isnt different to boosting from a send command perspective of the client...
        boolean successfulResponse = randomBoolean();
        int dutyCycle = randomIntBetween(1, 100);
        int freeMemorySlots = randomIntBetween(1, 255);
        handler.configureBoostResponse(dutyCycle, successfulResponse, freeMemorySlots);

        try (CubeClient client = getCubeClient("localhost", server.getPort())) {
            Cube cube = client.connect();
            boolean boostConfigured = client.holiday(cube.getRooms().get(0), LocalDateTime.now(), 20);
            assertThat(boostConfigured, is(successfulResponse));
        }
    }

    private class CubeServer implements Closeable {

        private final NioSocketAcceptor acceptor;

        public CubeServer() {
            acceptor = new NioSocketAcceptor(1);
        }

        public int getPort() {
            return acceptor.getLocalAddress().getPort();
        }

        public void start() throws IOException {
            // enable for debug
            acceptor.getFilterChain().addLast("logger", new LoggingFilter());
            TextLineCodecFactory codecFactory = new TextLineCodecFactory(UTF_8, LineDelimiter.CRLF, LineDelimiter.CRLF);
            acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(codecFactory));
            acceptor.setHandler(handler);
            acceptor.bind(new InetSocketAddress(0) );
            acceptor.setCloseOnDeactivation(true);
            logger.info("Cube server bound to [{}]", acceptor.getLocalAddress());
        }

        @Override
        public void close() throws IOException {
            acceptor.dispose(true);
        }
    }

    private static class CubeServerHandler extends IoHandlerAdapter {

        private static final Logger LOGGER = LoggerFactory.getLogger(IoHandlerAdapter.class);
        private volatile int boostDutyCycle;
        private volatile boolean boostSuccessful;
        private volatile int boostFreeMemorySlots;

        @Override
        public void sessionOpened(IoSession session) throws Exception {
            // dump the standard infos...
            session.write(ParserTest.input);
        }

        @Override
        public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
            session.closeNow();
            LOGGER.error("Caught exception", cause);
        }

        @Override
        public void messageReceived(IoSession session, Object message) throws Exception {
            String str = message.toString();
            LOGGER.info("Recevied message: {}", str);

            if (str.trim().startsWith("s:")) {
                String data = "S:" + toHexString(boostDutyCycle) + "," + (boostSuccessful ? "0" : "1")  + "," + toHexString(boostFreeMemorySlots);
                LOGGER.info("Sending reply: {}", data);
                session.write(data);
            }

            if( str.trim().equalsIgnoreCase("q:") ) {
                session.closeOnFlush();
                return;
            }
        }

        public void configureBoostResponse(int dutyCycle, boolean successful, int freeMemorySlots) {
            this.boostDutyCycle = dutyCycle;
            this.boostSuccessful = successful;
            this.boostFreeMemorySlots = freeMemorySlots;
        }
    }
}
