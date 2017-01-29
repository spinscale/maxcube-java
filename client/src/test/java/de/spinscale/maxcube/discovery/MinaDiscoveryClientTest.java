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

import de.spinscale.maxcube.test.CubeTestCase;
import org.junit.Test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MinaDiscoveryClientTest extends CubeTestCase {

    @Test
    public void testGoodCaseDiscovery() throws Exception {
        int port = randomIntBetween(60000, 65000);
        String data = randomAsciiOfLength(18);
        try (MinaDiscoveryClient client = new MinaDiscoveryClient(port)) {
            List<DiscoveredCube> cubes = new ArrayList<>();
            NetworkInterface localhostInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            client.startServer(localhostInterface, cubes);

            try (DatagramSocket clientSocket = new DatagramSocket()) {
                clientSocket.setReuseAddress(true);
                byte[] discoverBytes = data.getBytes(UTF_8);
                DatagramPacket sendPacket = new DatagramPacket(discoverBytes, discoverBytes.length, InetAddress.getLocalHost(), port);
                logger.info("Sending UDP packet to [{}:{}]", InetAddress.getLocalHost(), port);
                clientSocket.send(sendPacket);
            }

            // waiting without sleeping would be better
            Thread.sleep(500);

            assertThat(cubes, hasSize(greaterThan(0)));
            assertThat(cubes.stream().filter(cube -> cube.id.equals(data.substring(8, 18))).count(), is(1L));
        }
    }

    /*
    TODO make me better
    @Test
    public void testUdpPacketIsSent() throws Exception {
        int port = randomIntBetween(60000, 65000);
        NetworkInterface localhostInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
        List<DiscoveredCube> cubes = new ArrayList<>();
        try (MinaDiscoveryClient server = new MinaDiscoveryClient(port)) {
            server.startServer(localhostInterface, cubes);

            try (MinaDiscoveryClient client = new MinaDiscoveryClient(port)) {
                client.sendUdpBroadcastPacket();
            }

        }
    }
    */

    @Test
    public void testNullInterface() throws IOException {
        int port = randomIntBetween(60000, 65000);
        try (DiscoveryClient client = new MinaDiscoveryClient(port)) {
            IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> client.discover(null, randomIntBetween(1, 10)));
            assertThat(e.getMessage(), is("Network interface may not be null"));
        }
    }

    // this relies on mocking of static methods, added in recent mockito releases, looks nice!
    // OTOH this seems to slow down the test by a fair bit.. if we can get rid of it again would be nice as well
    @Test
    public void testClientFailsWithIpv6OnlyAddress() throws Exception {
        try (DiscoveryClient client = new MinaDiscoveryClient(randomIntBetween(60000, 65000))) {
            NetworkInterface mockInterface = mock(NetworkInterface.class);
            when(mockInterface.getName()).thenReturn("HAHA");
            Enumeration<InetAddress> enumeration = mock(Enumeration.class);
            when(enumeration.hasMoreElements()).thenReturn(true, false);
            Inet6Address mockInet6Address = mock(Inet6Address.class);
            when(enumeration.nextElement()).thenReturn(mockInet6Address);
            when(mockInterface.getInetAddresses()).thenReturn(enumeration);
            IllegalStateException e = expectThrows(IllegalStateException.class, () -> client.discover(mockInterface, randomIntBetween(1, 10)));
            assertThat(e.getMessage(), is("No ipv4 address found for interface: HAHA"));
        }
    }
}
