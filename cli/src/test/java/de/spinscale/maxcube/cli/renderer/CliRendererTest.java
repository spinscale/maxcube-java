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
package de.spinscale.maxcube.cli.renderer;

import de.spinscale.maxcube.data.Parser;
import de.spinscale.maxcube.entities.Cube;
import de.spinscale.maxcube.entities.Device;
import de.spinscale.maxcube.entities.DeviceType;
import de.spinscale.maxcube.entities.Room;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class CliRendererTest extends CubeTestCase {

    @Test
    public void checkThatAsciiTableFormattingWorls() throws Exception {
        Cube cube = new Cube(randomAsciiOfLength(10), randomInt(10), randomAsciiOfLength(10), LocalDateTime.now());
        int roomCount = randomIntBetween(1, 10);
        for (int i = 0; i < roomCount; i++) {
            cube.getRooms().add(createRandomRoom(i));
        }

        Renderer renderer = new CliRenderer();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        renderer.render(cube, bos);

        String output = bos.toString(StandardCharsets.UTF_8.name());
        logger.info(output);

        String[] lines = output.split("\n");

        // drawing lines: roomCount * 2 + 3 (start line, headers, separator) + 6 (cube info table)
        int expectedTotalLines = roomCount * 2 + 3 + 6;
        assertThat(lines.length, is(expectedTotalLines));

        // second line should contain all the headers
        for (String header : CliRenderer.HEADERS) {
            // skip cube info table
            assertThat(lines[7], containsString(header));
        }

        // ensure all rooms are displayed
        cube.getRooms().stream().forEach(room -> {
            String line = findLineForRoom(room, lines);
            assertThat(line, containsString(String.valueOf(room.isWindowOpen())));
            assertThat(line, containsString(String.valueOf(room.isLowBattery())));
            assertThat(line, containsString(String.valueOf(room.getValvePositionInPercent())));
            assertThat(line, containsString(String.valueOf(room.getId())));
            DecimalFormat df = new DecimalFormat("##.#");
            assertThat(line, containsString(String.valueOf(df.format(room.getCurrentTemperature()))));
        });
    }

    private String findLineForRoom(Room room, String[] lines) {
        for (String line : lines) {
            if (line.contains(room.getName())) {
                return line;
            }

        }
        throw new IllegalArgumentException("No line for room " + room);
    }

    private Room createRandomRoom(int id) {
        Room room = new Room(id, "name_"+ id, randomInt(10));
        room.setWindowOpen(randomBoolean());
        room.setConfiguredTemperature(randomDouble());
        room.setCurrentTemperature(randomDouble());
        room.setValvePositionInPercent(randomIntBetween(0, 100));

        boolean lowBattery = randomBoolean();
        Device device = new Device(randomFrom(DeviceType.values()), randomAsciiOfLength(5), randomAsciiOfLength(5), randomIntBetween(10, 20));
        if (device.getType().isThermostat()) {
            device.setMode(randomFrom(Parser.Mode.values()));
        }
        device.setLowBattery(lowBattery);
        room.getDevices().add(device);
        return room;
    }

}
