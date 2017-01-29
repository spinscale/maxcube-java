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

import de.spinscale.maxcube.entities.Cube;
import de.spinscale.maxcube.entities.Device;
import de.spinscale.maxcube.entities.Room;
import de.spinscale.maxcube.test.CubeTestCase;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class ParserTest extends CubeTestCase {

    public static final String input = "H:KEQ0537741,0b9792,0113,00000000,78c816bb,01,32,11010f,0d22,03,0000\n" +
            "M:00,01," +
            "VgIFAQpXb2huemltbWVyC3GDAgxTY2hsYWZ6aW1tZXILcVkDA0JhZAtxTgQNQXJiZWl0c3ppbW1lcg5bywUGS8O8Y2hlDly7CAELcYNLRVEwNDQ5NjE3ElRoZXJtb3N0YXQgSGVpenVuZwEEBVZ+SkVRMDM5ODEzMA5GZW5zdGVya29udGFrdAEBC3FZS0VRMDQ0OTY1NAdIZWl6dW5nAgQFUZ9KRVEwMzk2ODAzB0ZlbnN0ZXICAQtxTktFUTA0NDk2NzUHSGVpenVuZwMCDlvLTEVRMDAyMjkzNwxUaGVybW9zdGF0IDEEAg5cu0xFUTAwMjMxODESVGhlcm1vc3RhdCBIZWl6dW5nBQQWMrZORVEwODU2MzE1DkZlbnN0ZXJrb250YWt0BQE=\n" +
            "C:0b9792,7QuXkgATAf9LRVEwNTM3NzQxAQsABEAAAAAAAAAAAP///////////////////////////wsABEAAAAAAAAAAQf" +
            "///////////////////////////2h0dHA6Ly9tYXguZXEtMy5kZTo4MC9jdWJlADAvbG9va3VwAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAENFVAAACgADAAAOEENFU1QAAwACAAAcIA==\n" +
            "C:0e5cbb,0g5cuwIFEABMRVEwMDIzMTgxKyE9CQcYA1JK" +
            "/wBIZFTBSSBJIEkgSSBJIEUgRSBFIEUgRSBFIEhkVMFJIEkgSSBJIEkgRSBFIEUgRSBFIEUgSGRUwUkgSSBJIEkgSSBFIEUgRSBFIEUgRSBIZFTBSSBJIEkgSSBJIEUgRSBFIEUgRSBFIEhkVMFJIEkgSSBJIEkgRSBFIEUgRSBFIEUgSGRUwUkgSSBJIEkgSSBFIEUgRSBFIEUgRSBIZFTBSSBJIEkgSSBJIEUgRSBFIEUgRSBFIA==\n" +
            "C:0b7159,0gtxWQECGP9LRVEwNDQ5NjU0KyE9CQcYA5JK" +
            "/wBOT0hxOORbE00gTSBNIEUgRSBFIEUgRSBFIE5PSHE45FsTTSBNIE0gRSBFIEUgRSBFIEUgTk9IcTjkWxNNIE0gTSBFIEUgRSBFIEUgRSBOT0hxOORbE00gTSBNIEUgRSBFIEUgRSBFIE5PSHE45FsTTSBNIE0gRSBFIEUgRSBFIEUgTk9IcTjkWxNNIE0gTSBFIEUgRSBFIEUgRSBOT0hxOORbE00gTSBNIEUgRSBFIEUgRSBFIA==\n" +
            "C:05567e,EQVWfgQBEw9KRVEwMzk4MTMw\n" +
            "C:0e5bcb,0g5bywIEEP9MRVEwMDIyOTM3KyE9CQcYA3JK" +
            "/wAsSixuLNktGi0gLSAtIEUgRSBFIEUgRSBFICxKLG4s2S0aLSAtIC0gRSBFIEUgRSBFIEUgLEosZizYLRotIC0gLSBFIEUgRSBFIEUgRSAsSixmLNgtGi0gLSAtIEUgRSBFIEUgRSBFICxKLGYs2C0aLSAtIC0gRSBFIEUgRSBFIEUgLEosZizYLRotIC0gLSBFIEUgRSBFIEUgRSAsSixmLNgtGi0gLSAtIEUgRSBFIEUgRSBFIA==\n" +
            "C:1632b6,ERYytgQFEABORVEwODU2MzE1\n" +
            "C:0b7183,0gtxgwEBGP9LRVEwNDQ5NjE3KyE9CQcYA5JK" +
            "/wBKYlbiVQlRIFEgUSBRIEUgRSBFIEUgRSBFIEpiVuJVCVEgUSBRIFEgRSBFIEUgRSBFIEUgSmJW4lUJUSBRIFEgUSBFIEUgRSBFIEUgRSBKYlbiVQlRIFEgUSBRIEUgRSBFIEUgRSBFIEpiVuJVCVEgUSBRIFEgRSBFIEUgRSBFIEUgSmJW4lUJUSBRIFEgUSBFIEUgRSBFIEUgRSBKYlbiVQlRIFEgUSBRIEUgRSBFIEUgRSBFIA==\n" +
            "C:0b714e,0gtxTgEDGP9LRVEwNDQ5Njc1KyE9CQcYA7JK" +
            "/wA4eEEIOSA5IDkgOSA5IEUgRSBFIEUgRSBFIDh4QQg5IDkgOSA5IDkgRSBFIEUgRSBFIEUgOENoVDkdNyA3IDcgNyBFIEUgRSBFIEUgRSA4Q2hUOR03IDcgNyA3IEUgRSBFIEUgRSBFIDhDaFQ5HTcgNyA3IDcgRSBFIEUgRSBFIEUgOENoVDkdNyA3IDcgNyBFIEUgRSBFIEUgRSA4Q2hUOR03IDcgNyA3IEUgRSBFIEUgRSBFIA==\n" +
            "C:05519f,EQVRnwQCEw9KRVEwMzk2ODAz\n" +
            "L:Cw5cuwkSGBkqANcACwtxWQkSGAAYALAABgVWfgkSEAsOW8sJEhgAFgCoAAYWMrYJEhALC3GDCRIYCSsA7wALC3FOCRIYBCAAtgAGBVGfCRIS";

    @Test
    public void testGoodcase() throws Exception {
        Parser parser = new Parser();
        String[] lines = input.split("\n");
        Cube cube = parser.parseHeader(lines[0]);

        for (int i = 1; i < lines.length; i++) {
            parser.parse(cube, lines[i]);
        }

        assertThat(cube.getSerial(), is("KEQ0537741"));
        assertThat(cube.getRooms(), hasSize(5));
        assertThat(cube.getConfiguration().getSerial(), is("KEQ0537741"));

        //assertThat(cube.getRfaddress(), is("0b9792"));
        assertThat(cube.getRfaddress(), is(759698));
        assertThat(cube.getFirmwareVersion(), is("1.1.3"));
        assertThat(cube.getDate().toString(), is("2017-01-15T13:34"));

        assertThat(cube.findRoom("Bad").getCurrentTemperature(), is(18.2));
        assertThat(cube.findRoom("Küche").getCurrentTemperature(), is(21.5));
        assertThat(cube.findRoom("Arbeitszimmer").getCurrentTemperature(), is(16.8));

        Room sleepingRoom = cube.findRoom("Schlafzimmer");
        assertThat(sleepingRoom.getCurrentTemperature(), is(17.6));
        assertThat(sleepingRoom.getValvePositionInPercent(), is(0));
        assertThat(sleepingRoom.isWindowOpen(), is(true));

        Room livingRoom = cube.findRoom("Wohnzimmer");
        assertThat(livingRoom.getCurrentTemperature(), is(23.9));
        assertThat(livingRoom.getValvePositionInPercent(), is(9));

        List<Device> devices = getAllDevices(cube);
        devices.forEach(device -> assertThat(device.isLowBattery(), is(false)));
        // all other windows except sleeping room are closed
        cube.getRooms().stream().filter(r -> !r.getName().equals("Schlafzimmer")).forEach(room -> assertThat(room.isWindowOpen(), is(false)));

        // check for correct rfAddress
        Device deviceBySerial = cube.findDeviceBySerial("JEQ0396803");
        Device deviceByRadioAddress = cube.findDeviceByRfAddress(348575);
        assertThat(deviceBySerial, is(deviceByRadioAddress));
    }

    private List<Device> getAllDevices(Cube cube) {
        return cube.getRooms().stream()
                .map(room -> room.getDevices())
                .flatMap(devices -> devices.stream())
                .collect(Collectors.toList());
    }

    @Test
    public void testEmptyLine() {
        expectThrows(IllegalArgumentException.class, () -> new Parser().parse(null, null));
        expectThrows(IllegalArgumentException.class, () -> new Parser().parse(null, ""));
        expectThrows(IllegalArgumentException.class, () -> new Parser().parse(null, " "));
    }

    @Test
    public void testReadRfAddress() throws IOException {
        byte[] b = new byte[] { 11, -105, -110 };
        try (ByteArrayInputStream bis = new ByteArrayInputStream(b)) {
            int rfAddress = Parser.readRfAddress(bis);
            assertThat(rfAddress, is(759698));
        }
    }

    @Test
    public void testParseSuccessfulResponseS() throws Exception {
        assertThat(new Parser().parseResponseS("S:A2,0,34"), is(true));
    }

    @Test
    public void testParseFailedResponseS() throws Exception {
        assertThat(new Parser().parseResponseS("S:12,1,34"), is(false));
    }

    @Test
    public void testParseResponseSValidation() throws Exception {
        expectThrows(IllegalArgumentException.class, () -> new Parser().parseResponseS(null));
        expectThrows(IllegalArgumentException.class, () -> new Parser().parseResponseS(""));
        expectThrows(IllegalArgumentException.class, () -> new Parser().parseResponseS(" "));
        expectThrows(NumberFormatException.class, () -> new Parser().parseResponseS("S:X2,1,34"));
        expectThrows(NumberFormatException.class, () -> new Parser().parseResponseS("S:A2,1,X4"));
    }

    @Test
    public void parseReadDateTimeUntil() throws Exception {
        // actual reply from a cube I got
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(29);
        bos.write(145);
        bos.write(3);
        LocalDateTime localDateTime = Parser.readDateTimeUntil(new ByteArrayInputStream(bos.toByteArray()));
        assertThat(localDateTime.getDayOfMonth(), is(29));
        assertThat(localDateTime.getYear(), is(2017));
        assertThat(localDateTime.getMonth(), is(Month.JANUARY));
        assertThat(localDateTime.getHour(), is(1));
        assertThat(localDateTime.getMinute(), is(30));
    }
}
