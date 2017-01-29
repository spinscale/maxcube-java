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
package de.spinscale.maxcube.discovery;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.ExpiringSessionRecycler;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionRecycler;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioDatagramAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

public class MinaDiscoveryClient implements DiscoveryClient {

    private static final int PORT = 23272;
    public static final byte[] DISCOVERY_BYTES = "eQ3Max*.**********I".getBytes(UTF_8);

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final int port;
    private final NioDatagramAcceptor acceptor;

    public MinaDiscoveryClient() {
        this(PORT);
    }

    MinaDiscoveryClient(int port) {
        this.port = port;
        this.acceptor = new NioDatagramAcceptor();
    }

    @Override
    public List<DiscoveredCube> discover(final NetworkInterface networkInterface, int timeout) throws Exception {
        List<DiscoveredCube> cubes = new ArrayList<>();
        startServer(networkInterface, cubes);
        sendUdpBroadcastPacket();

        // now wait for the cube to answer
        try {
            Thread.sleep(timeout * 1000);
        } catch (InterruptedException e) {}

        return cubes;
    }

    void sendUdpBroadcastPacket() throws IOException {
        try (DatagramSocket clientSocket = new DatagramSocket()) {
            DatagramPacket sendPacket = new DatagramPacket(DISCOVERY_BYTES, DISCOVERY_BYTES.length, InetAddress.getByName("255.255.255.255"), port);
            clientSocket.send(sendPacket);
        }
    }

    void startServer(final NetworkInterface networkInterface, List<DiscoveredCube> cubes) throws Exception {
        if (networkInterface == null) {
            throw new IllegalArgumentException("Network interface may not be null");
        }

        acceptor.getFilterChain().addLast("logging", new LoggingFilter());
        acceptor.setHandler(new DiscoveryServerHandler(cubes));
        acceptor.getSessionConfig().setReuseAddress(true);
        acceptor.setCloseOnDeactivation(true);
        Optional<InetAddress> ipv4Address = Collections.list(networkInterface.getInetAddresses()).stream()
                .filter(addr -> addr instanceof Inet4Address).findFirst();
        if (!ipv4Address.isPresent()) {
            throw new IllegalStateException("No ipv4 address found for interface: " + networkInterface.getName());
        }
        acceptor.bind(new InetSocketAddress(ipv4Address.get(), port));
        logger.debug("MinaDiscoveryClient bound to [{}]", acceptor.getLocalAddress());
    }

    @Override
    public void close() throws IOException {
        // somehow stop expiring is not called by the acceptor... WTF?
        IoSessionRecycler sessionRecycler = acceptor.getSessionRecycler();
        if (sessionRecycler instanceof ExpiringSessionRecycler) {
            ((ExpiringSessionRecycler) sessionRecycler).stopExpiring();
        }
        acceptor.dispose(true);
    }

    static class DiscoveryServerHandler extends IoHandlerAdapter {

        private final List<DiscoveredCube> cubes;

        DiscoveryServerHandler(List<DiscoveredCube> cubes) {
            this.cubes = cubes;
        }

        @Override
        public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
            session.closeNow();
        }

        @Override
        public void messageReceived(IoSession session, Object message) throws Exception {
            if (message instanceof IoBuffer) {
                IoBuffer buffer = (IoBuffer) message;
                String data = new String(buffer.array(), UTF_8);
                // ignore the discovery bytes response
                if (!data.contains("*") && data.length() >= 18) {
                    String name = data.substring(8, 18);
                    if (session.getRemoteAddress() instanceof InetSocketAddress) {
                        cubes.add(new DiscoveredCube(name, ((InetSocketAddress) session.getRemoteAddress()).getHostString()));
                    }
                }
            }
        }
    }
}
