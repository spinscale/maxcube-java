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
import java.time.LocalDateTime;
import java.util.Locale;

import static de.spinscale.maxcube.data.Parser.readRfAddress;
import static java.nio.charset.StandardCharsets.UTF_8;

public class Device {

    final DeviceType type;
    final String name;
    final String serial;
    final int rfaddress;
    private Configuration configuration = null;
    private boolean lowBattery;
    private Parser.Mode mode;
    private LocalDateTime endTime;

    public Device(DeviceType type, String name, String serial, int rfaddress) {
        this.type = type;
        this.name = name;
        this.serial = serial;
        this.rfaddress = rfaddress;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "name [%s], type [%s] serial [%s] rfaddress [%s]", name, type, serial, rfaddress);
    }

    public static Device readFrom(ByteArrayInputStream bis) {
        int type = bis.read();

        // next three bytes are the rf address
        int rfaddress = readRfAddress(bis);

        // serial addr
        byte[] serial = new byte[10];
        bis.read(serial, 0, 10);

        // name of device
        int nameLength = bis.read();
        byte[] deviceName = new byte[nameLength];
        bis.read(deviceName, 0, nameLength);

        return new Device(DeviceType.fromId(type), new String(deviceName, UTF_8), new String(serial), rfaddress);
    }

    public DeviceType getType() {
        return type;
    }

    public String getSerial() {
        return serial;
    }

    public int getRfaddress() {
        return rfaddress;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        if (this.configuration != null) {
            throw new IllegalStateException("Cannot set configuration twice for " + toString());
        }
        this.configuration = configuration;
    }

    public void setLowBattery(boolean lowBattery) {
        this.lowBattery = lowBattery;
    }

    public boolean isLowBattery() {
        return lowBattery;
    }

    public void setMode(Parser.Mode mode) {
        this.mode = mode;
    }

    public Parser.Mode getMode() {
        return mode;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }
}
