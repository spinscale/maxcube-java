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
package de.spinscale.maxcube.data;

import de.spinscale.maxcube.entities.Configuration;
import de.spinscale.maxcube.entities.Cube;
import de.spinscale.maxcube.entities.Device;
import de.spinscale.maxcube.entities.DeviceType;
import de.spinscale.maxcube.entities.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A parser that understands the initial reply of a cube after connecting to it
 * Sources:
 *   https://github.com/aleszoulek/maxcube/
 *   https://github.com/ivesdebruycker/maxcube
 *   https://github.com/ivesdebruycker/maxcube-cli
 *   http://www.domoticaforum.eu/viewtopic.php?f=66&t=6654
 *   https://github.com/Bouni/max-cube-protocol
 */
public class Parser {

    private static final Logger logger = LoggerFactory.getLogger(Parser.class);

    public void parse(Cube cube, String input) throws IOException {
        if (input == null || input.trim().length() == 0) {
            throw new IllegalArgumentException("Empty input string");
        }

        String command = input.substring(0, 2);
        switch (command) {
            case "C:":
                parseConfiguration(cube, input);
                break;
            case "L:":
                parseDeviceList(cube, input);
                break;
            case "M:":
                parseMeta(cube, input);
                break;
        }
    }

    public enum Mode {
        AUTO,
        BOOST,
        MANUAL,
        VACATION;

        public static Mode from(int number) {
            if ((number & 3) == 3) return BOOST;
            if ((number & 2) == 2) return VACATION;
            if ((number & 1) == 1) return MANUAL;
            return AUTO;
        }

    }

    void parseDeviceList(Cube cube, String input) throws IOException {
        byte[] data = Base64.getDecoder().decode(input.substring(2).getBytes(UTF_8));

        // http://www.domoticaforum.eu/viewtopic.php?f=66&t=6654
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data)) {
            while (bis.available() > 0) {
                // eco buttons have 8, vales have 11, wall mounted thermostat have 12
                int submessageLength = bis.read();

                int rfaddress = readRfAddress(bis);
                Room room = cube.findRoomForDevice(rfaddress);
                Device device = cube.findDeviceByRfAddress(rfaddress);

                logger.info("Reading device list for device {}, ", cube.findDeviceByRfAddress(rfaddress));

                // unknown
                bis.read();
                // status
                int flagsTwo = bis.read();
                // another status, see above link, contains current mode
                int flagsOne = bis.read();
                // boolean dstActive =    (currentMode & (1 << 3)) == 0;
                // boolean gatewayKnown = (currentMode & (1 << 4)) == 0;
                // boolean panelLocked =  (currentMode & (1 << 5)) == 0;
                // boolean linkError =    (currentMode & (1 << 6)) == 0;
                boolean lowBattery =   (flagsOne & (1 << 7)) == 1;
                Mode mode = Mode.from(flagsOne);


                if (device.getType() == DeviceType.SHUTTER_CONTACT) {
                    room.setWindowOpen(mode == Mode.VACATION);
                }

                if (submessageLength <= 6) {
                    continue;
                }

                int valvePositionInPercent = bis.read();
                room.setValvePositionInPercent(valvePositionInPercent);
                double temperatureSetPoint = bis.read() / 2.0;
                room.setConfiguredTemperature(temperatureSetPoint);

                // this is a shitty hack and does not show the real temp
                // the real temp is only available when you have a wall mounted thermostat
                if (device.getType().isThermostat()) {
                    if (mode == Mode.VACATION) {
                        LocalDateTime endTime = readDateTimeUntil(bis);
                        room.findThermostat().setEndTime(endTime);
                    } else {
                        // see https://github.com/ivesdebruycker/maxcube/blob/master/maxcube-commandparser.js#L278
                        int firstDateOrTemp = bis.read();
                        int secondDateOrTemp = bis.read();
                        bis.read(); // half hours, we dont care as this is not a date;
                        double temp = firstDateOrTemp != 0 ? 25.5 : 0.0 + secondDateOrTemp / 10.0;
                        room.setCurrentTemperature(temp);
                    }
                }

                //  this is where the real temperate from the wall mounted thermostat can be read
                if (submessageLength > 11) {
                    bis.skip(submessageLength - 11);
                }

                device.setMode(mode);
                device.setLowBattery(lowBattery);
            }
        }
    }

    /**
     * C:0b9792,BASE64
     */
    void parseConfiguration(Cube cube, String input) throws IOException {
        // TODO deal with the non base64 encoded part
        String[] metadata = input.split(",");
        String base64 = metadata[metadata.length - 1];
        byte[] data = Base64.getDecoder().decode(base64.getBytes(UTF_8));

        try (ByteArrayInputStream bis = new ByteArrayInputStream(data)) {
            Configuration configuration = Configuration.readFrom(bis);
            Device device = cube.findDeviceBySerial(configuration.getSerial());
            device.setConfiguration(configuration);
        }
    }

    /**
     * M:00,01,BASE64
     *
     * First two parts are not known, the base64 encoded string contains the devices and the rooms
     *
     */
    void parseMeta(Cube cube, String input) throws IOException {
        String[] metadata = input.split(",");
        String base64 = metadata[metadata.length - 1];
        byte[] data = Base64.getDecoder().decode(base64.getBytes(UTF_8));

        try (ByteArrayInputStream bis = new ByteArrayInputStream(data)) {
            // first numbers are unknown
            bis.read(); bis.read();

            // read rooms
            int roomCount = bis.read();
            for (int i = 0; i < roomCount; i++) {
                cube.getRooms().add(Room.read(bis));
            }

            // read devices and map to rooms
            int deviceCount = bis.read();
            for (int i = 0; i < deviceCount; i++) {
                Device device = Device.readFrom(bis);
                int roomId = bis.read();
                Room room = cube.findRoom(roomId);
                room.getDevices().add(device);
            }

            // another last unknown byte
            bis.read();
        }
    }

    /**
     * Format of H: type
     * 1 2          3      4    5        6        7  8  9      10   11 12
     * H:KEQ0537741,0b9792,0113,00000000,14e4b1c9,01,32,100c1f,0e19,03,0000
     *
     *  1: always H here
     *  2: The serial of the cube, split with a colon
     *  3: rfaddress
     *  4: firmware version
     *  5: unknown
     *  6: HTTP connection id
     *  7: duty cycle as hex number
     *  8: free memory slots
     *  9: cube date
     * 10: cube time
     * 11: state cube time
     * 12: ntp counter
     */
    public Cube parseHeader(String input) {
        if (!input.startsWith("H:")) {
            throw new IllegalStateException("Expecting input to start with header line");
        }

        String[] fields = input.split(",");
        String serial = fields[0].substring(2);
        int rfAddress = Integer.parseInt(fields[1], 16);
        String version = fields[2].replaceFirst("^0", "");
        String firmwareVersion = version.chars().mapToObj(i -> String.valueOf(Integer.parseInt(String.valueOf((char) i), 16))).collect(Collectors.joining("."));

        int year = 2000 + Integer.parseInt(fields[7].substring(0, 2), 16);
        // month starts at 1
        int month = Integer.parseInt(fields[7].substring(2, 4), 16);
        int day = Integer.parseInt(fields[7].substring(4, 6), 16);
        int hour = Integer.parseInt(fields[8].substring(0, 2), 16);
        int minute = Integer.parseInt(fields[8].substring(2, 4), 16);
        LocalDateTime cubeDateTime = LocalDateTime.of(year, month, day, hour, minute);
        return new Cube(serial, rfAddress, firmwareVersion, cubeDateTime);
    }

    // S:00,0,31
    public boolean parseResponseS(String input) {
        if (input == null || input.trim().length() == 0) {
            throw new IllegalArgumentException("Empty input string");
        }

        if (!input.startsWith("S:")) {
            throw new IllegalStateException("Expecting input to start with [S:], but was " + input);
        }

        String data = input.substring(2);
        String[] entries = data.split(",", 3);
        int dutyCycle = Integer.valueOf(entries[0], 16);
        boolean commandResult = entries[1].equals("0");
        int freeMemorySlots = Integer.valueOf(entries[2], 16);

        return commandResult;
    }

    public static int readRfAddress(ByteArrayInputStream bis) {
        int rfAddress = bis.read() << 16;
        rfAddress += bis.read() << 8;
        rfAddress += bis.read();
        return rfAddress;
    }

    // see https://github.com/Bouni/max-cube-protocol/blob/master/L-Message.md
    static LocalDateTime readDateTimeUntil(ByteArrayInputStream bis) {
        int first = bis.read();
        int second = bis.read();
        int third = bis.read();

        int firstPositive = first & 0xFF; // unsigned
        int month = (firstPositive >> 5 << 1) + (second >> 7 & 1);
        int day = firstPositive & 31;
        int year = (second & 31) + 2000;

        int halfhours = third;

        logger.debug("Found date: {}-{}-{} half hours {}, orig ints [{}, {}, {}]", year, month, day, halfhours, first, second, third);
        return LocalDateTime.of(year, month, day, halfhours / 2, (halfhours % 2) * 30);
    }
}
