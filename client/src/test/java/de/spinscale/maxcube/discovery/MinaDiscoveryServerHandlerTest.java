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
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MinaDiscoveryServerHandlerTest extends CubeTestCase {

    @Test
    public void testServerHandler() throws Exception {
        List<DiscoveredCube> cubes = new ArrayList<>();
        MinaDiscoveryClient.DiscoveryServerHandler handler = new MinaDiscoveryClient.DiscoveryServerHandler(cubes);

        IoSession session = mock(IoSession.class);
        String host = randomAsciiOfLength(10);
        when(session.getRemoteAddress()).thenReturn(InetSocketAddress.createUnresolved(host, randomIntBetween(10000, 65000)));

        IoBuffer buffer = IoBuffer.wrap("1234567890abcdefgh".getBytes(UTF_8));
        handler.messageReceived(session, buffer);
        assertThat(cubes, hasSize(1));
        assertThat(cubes.get(0).id, is("90abcdefgh"));
        assertThat(cubes.get(0).host, is(host));
    }

    @Test
    public void testServerHandlerNoCubeFound() throws Exception {
        List<DiscoveredCube> cubes = new ArrayList<>();
        MinaDiscoveryClient.DiscoveryServerHandler handler = new MinaDiscoveryClient.DiscoveryServerHandler(cubes);

        IoSession session = mock(IoSession.class);
        String host = randomAsciiOfLength(10);
        when(session.getRemoteAddress()).thenReturn(InetSocketAddress.createUnresolved(host, randomIntBetween(10000, 65000)));

        IoBuffer buffer = IoBuffer.wrap("nothingfound****".getBytes(UTF_8));
        handler.messageReceived(session, buffer);
        assertThat(cubes,  hasSize(0));
    }

    @Test
    public void testServerHandlerNotEnoughCharsReturned() throws Exception {
        List<DiscoveredCube> cubes = new ArrayList<>();
        MinaDiscoveryClient.DiscoveryServerHandler handler = new MinaDiscoveryClient.DiscoveryServerHandler(cubes);

        IoSession session = mock(IoSession.class);
        String host = randomAsciiOfLength(10);
        when(session.getRemoteAddress()).thenReturn(InetSocketAddress.createUnresolved(host, randomIntBetween(10000, 65000)));

        IoBuffer buffer = IoBuffer.wrap("a".getBytes(UTF_8));
        handler.messageReceived(session, buffer);
        assertThat(cubes,  hasSize(0));
    }

    @Test
    public void testServerHandlerClosesSessionOnException() throws Exception {
        MinaDiscoveryClient.DiscoveryServerHandler handler = new MinaDiscoveryClient.DiscoveryServerHandler(Collections.emptyList());
        IoSession session = mock(IoSession.class);
        handler.exceptionCaught(session, new RuntimeException("anything"));
        verify(session, times(1)).closeNow();
    }
}
