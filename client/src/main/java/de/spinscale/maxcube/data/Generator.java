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
import de.spinscale.maxcube.entities.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public class Generator {

    protected static Logger logger = LoggerFactory.getLogger(Generator.class);

    private Generator() {
    }

    public static void writeRfAddress(String address, ByteArrayOutputStream bos) {
        int addr = Integer.valueOf(address);
        writeRfAddress(addr, bos);
    }

    public static void writeRfAddress(int address, ByteArrayOutputStream bos) {
        bos.write(address >> 16);
        bos.write(address >> 8);
        bos.write(address);
    }

    public static String writeBoostRequest(Room room) throws IOException {
        Device thermostat = room.findThermostat();
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            writeSetTemperatureRequest(bos, room.getId(), thermostat.getRfaddress());
            // mode & temperature, boost mode is 11 at the beginning, temperature does not matter
            bos.write(192);

            String base64 = Base64.getEncoder().encodeToString(bos.toByteArray());
            return "s:" + base64;
        }
    }

    /**
     * https://github.com/Bouni/max-cube-protocol/blob/master/S-Message.md
     */
    public static String writeSetTemperatureRequest(Room room, double temperature) throws
        IOException {
        if (temperature < 0 || temperature > 31) {
            throw new IllegalArgumentException("Temperature must be between 0 and 31 °C");
        }

        double doubledTemp = temperature * 2;
        double isIntegerOrDotFive = doubledTemp % 2;

        if (isIntegerOrDotFive == 1 || isIntegerOrDotFive == 0) {
            Device thermostat = room.findThermostat();
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                writeSetTemperatureRequest(bos, room.getId(), thermostat.getRfaddress());
                /*
                hex:  |    66     |
                dual: | 0110 1100 |
                        |||| ||||
                        ||++-++++-- temperature: 10 1100 -> 38 = temp * 2
                        ||                     (to get the temperature, the value must be divided by 2: 38/2 = 19)
                        ++--------- mode:
                                    00=auto/weekly program
                                    01=manual ( => 0100 0000 => Decimal 64)
                                    10=vacation
                                    11=boost */
                bos.write(64 + (int) doubledTemp);

                String base64 = Base64.getEncoder().encodeToString(bos.toByteArray());
                return "s:" + base64;
            }
        }
        else {
            throw new IllegalArgumentException("only xx.5 is supported");
        }
    }

    public static String writeHolidayRequest(Room room, LocalDateTime endDate, int temperature) throws IOException {
        if (temperature > 31) {
            throw new IllegalArgumentException("Temperature must be between 0 and 31 °C");
        }

        endDate = roundEndDateToTheNextHalfHour(endDate);
        DateTimeFormatter df = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm");
        logger.info("Setting room [{}] to [{}] °C till [{}]", room.getName(), temperature, endDate.format(df));

        Device thermostat = room.findThermostat();
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            writeSetTemperatureRequest(bos, room.getId(), thermostat.getRfaddress());

            // mode & temperature, boost mode is 11 at the beginning, temperature does not matter
            // boost is 192/11, vacation is 128/10, plus the temperature times 2
            bos.write(128 + temperature * 2);

            // date until
            writeDateTimeUntil(endDate, bos);

            String base64 = Base64.getEncoder().encodeToString(bos.toByteArray());
            return "s:"  + base64;
        }
    }

    static LocalDateTime roundEndDateToTheNextHalfHour(LocalDateTime date) {
        if (date == null) {
            throw new IllegalArgumentException("Date may not be null");
        }

        if (date.getMinute() >= 30) {
            // to the next full hour, if 7:45 => 8:00
            return date.plusMinutes(60 - date.getMinute());
        } else {
            // to the next :30... if 7:14 => 7:30
            return date.plusMinutes(30 - date.getMinute());
        }
    }

    public static void writeDateTimeUntil(LocalDateTime dateTime, ByteArrayOutputStream bos) {
        // this is frigging confusing.. who the hell did think about this...
        // the first three bits are the months
        // next five bits are the day
        // next bit is always 0
        // next bit is the month again (the very first one)
        // next bit is always 0
        // last five bits are the year

        // only the first three bits!

        // int month = (positiveFirst >> 5 << 1) + (64 >> 6 & 1);
        int month = dateTime.getMonthValue();
        int monthAndDay = (month << 4 & 224) + dateTime.getDayOfMonth();
        bos.write(monthAndDay);

        int monthAndYear = (month % 2 == 1 ? 128 : 0) + (dateTime.getYear() - 2000);
        bos.write(monthAndYear);

        int halfhours = (dateTime.getHour() * 2) + (dateTime.getMinute() == 30 ? 1 : 0);
        bos.write(halfhours);
    }

    /**
     * https://github.com/Bouni/max-cube-protocol/blob/master/S-Message.md
     */
    private static void writeSetTemperatureRequest(ByteArrayOutputStream bos, int roomId, int thermostatRfAddress) {
        bos.write(0); // unknown
        bos.write(4); // rf flags
        bos.write(64); // 0x40 => 64 (decimal) => Set temperature

        // rf address from
        Generator.writeRfAddress(0, bos);

        // rf address to
        Generator.writeRfAddress(thermostatRfAddress, bos);
        //Generator.writeRfAddress(room.getRfaddress(), bos);

        // room number
        bos.write(roomId);
    }
}
