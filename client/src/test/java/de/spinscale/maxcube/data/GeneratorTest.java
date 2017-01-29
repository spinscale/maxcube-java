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

import de.spinscale.maxcube.entities.Device;
import de.spinscale.maxcube.entities.DeviceType;
import de.spinscale.maxcube.entities.Room;
import de.spinscale.maxcube.test.CubeTestCase;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

public class GeneratorTest extends CubeTestCase {

    @Test
    public void testWritingRfAddress() throws Exception {
        byte[] b = new byte[] { randomByte(), randomByte(), randomByte() };
        try (ByteArrayInputStream bis = new ByteArrayInputStream(b)) {
            int rfAddress = Parser.readRfAddress(bis);

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                Generator.writeRfAddress(rfAddress, bos);

                byte[] bytes = bos.toByteArray();
                assertThat(b, is(bytes));
            }
        }

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            int rfAddress = Integer.parseInt("0FDAED", 16);
            Generator.writeRfAddress(rfAddress, bos);

            try (ByteArrayOutputStream bos2 = new ByteArrayOutputStream()) {
                Generator.writeRfAddress(String.valueOf(rfAddress), bos2);
                assertThat(Arrays.equals(bos.toByteArray(), bos2.toByteArray()), is(true));
            }
        }
    }

    @Test
    public void testWritingBoost() throws Exception {
        Room room = new Room(0, "myroom", 12345);
        DeviceType type = randomFrom(DeviceType.THERMOSTAST, DeviceType.THERMOSTAST_PLUS, DeviceType.WALLTHERMOSTAT);

        Device thermostat = new Device(type, "name", "0abd3d", 67890);
        room.getDevices().add(thermostat);

        String request = Generator.writeBoostRequest(room);
        assertThat(request, startsWith("s:"));

        byte[] decode = Base64.getDecoder().decode(request.substring(2).getBytes(StandardCharsets.UTF_8));
        try (ByteArrayInputStream bis = new ByteArrayInputStream(decode)) {
            bis.skip(6);
            // rfaddress from the thermostat
            int readRfAddress = Parser.readRfAddress(bis);
            assertThat(readRfAddress, is(67890));

            // request should contain the room id at byte 10
            assertThat(bis.read(), is(room.getId()));
        }
    }

    @Test
    public void testWritingBoostWithoutThermostatFails() {
        Room room = new Room(0, "myroom", 12345);
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> Generator.writeBoostRequest(room));
        assertThat(e.getMessage(), is("Room has no thermostat"));
    }

    @Test
    public void testWriteDateTimeUntil() throws Exception {
        Month month = randomFrom(Month.values());
        int day = randomIntBetween(1, 28);
        int hourOfDay = randomIntBetween(0, 23);
        int minute = randomIntBetween(0, 59);
        int year = randomIntBetween(2016, 2025);
        LocalDateTime dateTime = LocalDateTime.of(year, month, day, hourOfDay, minute);

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            // move to next 30 minute window like we do in our code
            LocalDateTime expectedDateTime = Generator.roundEndDateToTheNextHalfHour(dateTime);

            logger.info("Configured date [{}] expected date [{}]", dateTime, expectedDateTime);
            Generator.writeDateTimeUntil(expectedDateTime, bos);

            byte[] bytes = bos.toByteArray();
            assertThat(bytes.length, is(3));

            int firstByte = bytes[0] & 255;
            int secondByte = bytes[1] & 255;

            // shift by four bytes, but loose last bit intentionally, as it is hidden in the second byte
            int extractedMonth = (firstByte >> 5 << 1) + (secondByte >> 7);
            assertThat(extractedMonth, is(expectedDateTime.getMonthValue()));

            int extractedDay = bytes[0] & 0x1f;
            assertThat(extractedDay, is(expectedDateTime.getDayOfMonth()));

            int thirdByte = bytes[2] & 0xff;
            if (thirdByte > 48) {
                throw new IllegalArgumentException("Number of half hours cannot be bigger than 48 per day");
            }
            if (expectedDateTime.getMinute() >= 30) {
                assertThat(thirdByte, is(expectedDateTime.getHour() * 2 + 1));
            } else {
                assertThat(thirdByte, is(expectedDateTime.getHour() * 2));
            }
        }
    }

    @Test
    public void testRoundingDateNull() {
        expectThrows(IllegalArgumentException.class, () -> Generator.roundEndDateToTheNextHalfHour(null));
    }

    @Test
    public void testRoundingDate() {
        int month = randomIntBetween(1, 12);
        int day = randomIntBetween(1, 28);
        int hourOfDay = randomIntBetween(0, 23);
        int minute = randomIntBetween(0, 59);
        int year = randomIntBetween(2016, 2025);
        LocalDateTime dateTime = LocalDateTime.of(year, month, day, hourOfDay, minute);
        LocalDateTime roundedDate = Generator.roundEndDateToTheNextHalfHour(dateTime);
        assertThat(roundedDate.isAfter(dateTime), is(true));

        LocalDateTime expectedDate = dateTime.with(temporal -> {
            int currentMinute = temporal.get(ChronoField.MINUTE_OF_HOUR);
            if (currentMinute >= 30) {
                return temporal.plus(60 - currentMinute, ChronoUnit.MINUTES);
            } else {
                return temporal.plus(30 - currentMinute, ChronoUnit.MINUTES);
            }
        });

        assertThat(roundedDate, is(expectedDate));
    }

    // full example from https://github.com/Bouni/max-cube-protocol/blob/master/S-Message.md
    @Test
    public void testGeneratorHolidayRequest() throws IOException {
        String expectedOutput = "s:AARAAAAAD9rtAaadCwQ=";
        LocalDateTime dateTime = LocalDateTime.of(2011, Month.AUGUST, 29, 1, 59);
        // TODO MAKE HOLIDAY REQUEST
        Room room = new Room(1, "foo", 123456);
        room.getDevices().add(new Device(DeviceType.THERMOSTAST, "foo", "serial", 1039085));

        String output = Generator.writeHolidayRequest(room, dateTime, 19);
        assertThat(output, is(expectedOutput));
    }
}
