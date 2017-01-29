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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SocketCubeClient implements CubeClient {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String hostname;
    private final int port;
    private final Socket socket;
    private final Parser parser;
    private Cube cube;
    private BufferedReader reader;

    public SocketCubeClient(String hostname) {
        this(hostname, 62910);
    }

    public SocketCubeClient(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
        this.socket = new Socket();
        this.parser = new Parser();
    }

    public Cube connect() throws IOException {
        InetSocketAddress endpoint = new InetSocketAddress(hostname, port);
        logger.debug("Connecting to {}", endpoint);
        socket.connect(endpoint, 2000);
        // needs to be configurable in the future, when waiting for pairing
        // but two seconds are enough to return the standard info
        socket.setSoTimeout(3000);
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        logger.trace("Waiting for header line");
        String supposedHeaderLine = reader.readLine();
        logger.trace("Reading header line [{}]", supposedHeaderLine);
        cube = parser.parseHeader(supposedHeaderLine);

        String input;
        while ((input = reader.readLine()) != null) {
            logger.trace("Reading input line: [{}]", input);
            parser.parse(cube, input);
            if (input.startsWith("L:")) break;
        }

        return cube;
    }

    @Override
    public boolean boost(Room room) throws Exception {
        String data = Generator.writeBoostRequest(room);
        return this.sendSetTemperatureRequest(data);
    }

    @Override
    public boolean holiday(Room room, LocalDateTime endTime, int temperature) throws Exception {
        String data = Generator.writeHolidayRequest(room, endTime, temperature);
        return this.sendSetTemperatureRequest(data);
    }

    private boolean sendSetTemperatureRequest(String base64encodedData) throws Exception {
        String dataToSend = base64encodedData + "\r\n";
        socket.getOutputStream().write(dataToSend.getBytes(UTF_8));
        socket.getOutputStream().flush();
        logger.info("Sent data [{}] to cube, now waiting for response", base64encodedData);
        Thread.sleep(1000);
        String line = reader.readLine();
        if (line == null) {
            logger.info("Stream reached end");
            return false;
        }
        logger.info("Got response: [{}]", line);
        return parser.parseResponseS(line);
    }

    @Override
    public void close() throws IOException {
        socket.getOutputStream().write("q:/r/n".getBytes(UTF_8));
        socket.getOutputStream().flush();

        reader.close();
        socket.close();
    }
}
