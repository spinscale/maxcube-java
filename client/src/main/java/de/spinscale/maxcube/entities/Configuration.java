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
package de.spinscale.maxcube.entities;

import de.spinscale.maxcube.data.Parser;

import java.io.ByteArrayInputStream;
import java.util.Locale;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Configuration {

    final DeviceType type;
    final int rfaddress;
    final String serial;

    public Configuration(DeviceType type, int rfaddress, String serial) {
        this.type = type;
        this.rfaddress = rfaddress;
        this.serial = serial;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "serial [%s] type [%s]", serial, type);
    }

    public static Configuration readFrom(ByteArrayInputStream bis) {
        int len = bis.read();

        int rfAddress = Parser.readRfAddress(bis);
        DeviceType type = DeviceType.fromId(bis.read());

        byte[] unknown = new byte[3];
        bis.read(unknown, 0, unknown.length);

        byte[] serialBytes = new byte[10];
        bis.read(serialBytes, 0, serialBytes.length);
        String serial = new String(serialBytes, UTF_8);

        if (type == DeviceType.THERMOSTAST_PLUS || type == DeviceType.THERMOSTAST) {
            double temperatureComfort = bis.read() / 2.0;
            double temperatureEco = bis.read() / 2.0;
            double temperatureSetpointMax = bis.read() / 2.0;
            double temperatureSetpointMin = bis.read() / 2.0;
            double temperatureOffset = bis.read() / 2.0 - 3.5;
            double temperatureWindowOpen = bis.read() / 2.0;
            int durationWindowOpen = bis.read();
            int durationBoost = bis.read();
            int decalcification = bis.read();
            double valveMaximum = bis.read() * 100/255.0;
            double valveOffset = bis.read() * 100/255.0;

            // TODO decipher the weekly program, always has a length of 182
            bis.skip(bis.available());

            return new ValveConfiguration(type, rfAddress, serial,
                    temperatureComfort, temperatureEco, temperatureSetpointMax, temperatureSetpointMin, temperatureOffset,
                    temperatureWindowOpen, durationWindowOpen, durationBoost, decalcification, valveMaximum, valveOffset);
        } else if (type == DeviceType.CUBE) {
            // TODO decipher the cube configuration
            bis.skip(bis.available());
        }

        if (bis.available() > 0) {
            String message = String.format(Locale.ROOT, "Device of type [%s] claims a length of [%s] and has [%s] unread bytes", type,
                    len, bis.available());
            throw new IllegalStateException(message);
        }

        return new Configuration(type, rfAddress, serial);
    }

    public String getSerial() {
        return serial;
    }

    public static class ValveConfiguration extends Configuration {

        final double temperatureComfort;
        final double temperatureEco;
        final double temperatureSetpointMax;
        final double temperatureSetpointMin;
        final double temperatureOffset;
        final double temperatureWindowOpen;
        final int durationWindowOpen;
        final int durationBoost;
        final int decalcification;
        final double valveMaximum;
        final double valveOffset;

        public ValveConfiguration(DeviceType type, int rfaddress, String serial, double temperatureComfort, double temperatureEco, double
                temperatureSetpointMax, double temperatureSetpointMin, double temperatureOffset, double temperatureWindowOpen, int
                durationWindowOpen, int durationBoost, int decalcification, double valveMaximum, double valveOffset) {
            super(type, rfaddress,  serial);
            this.temperatureComfort = temperatureComfort;
            this.temperatureEco = temperatureEco;
            this.temperatureSetpointMax = temperatureSetpointMax;
            this.temperatureSetpointMin = temperatureSetpointMin;
            this.temperatureOffset = temperatureOffset;
            this.temperatureWindowOpen = temperatureWindowOpen;
            this.durationWindowOpen = durationWindowOpen;
            this.durationBoost = durationBoost;
            this.decalcification = decalcification;
            this.valveMaximum = valveMaximum;
            this.valveOffset = valveOffset;
        }
    }
}
