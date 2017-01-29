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

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static de.spinscale.maxcube.data.Parser.readRfAddress;
import static java.nio.charset.StandardCharsets.UTF_8;

public final class Room {

    final int id;
    final String name;
    final List<Device> devices = new ArrayList<>();
    final int rfaddress;
    private double currentTemperature;
    private boolean windowOpen;
    private double configuredTemperature;
    private int valvePositionInPercent;

    public Room(int id, String name, int rfaddress) {
        this.id = id;
        this.name = name;
        this.rfaddress = rfaddress;
    }

    public Device findThermostat() {
        Optional<Device> thermostatOptional = devices.stream().filter(device -> device.getType().isThermostat()).findFirst();
        if (!thermostatOptional.isPresent()) {
            throw new IllegalArgumentException("Room has no thermostat");
        }

        return thermostatOptional.get();
    }

    public List<Device> getDevices() {
        return devices;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return String.format("Room: id[%s] name[%s] devices %s", id, name, devices);
    }

    public static Room read(ByteArrayInputStream bis) {
        int roomId = bis.read();
        int nameLength = bis.read();
        byte[] roomName = new byte[nameLength];
        bis.read(roomName, 0, nameLength);

        // next three bytes are the rf address
        int rfaddress = readRfAddress(bis);

        return new Room(roomId, new String(roomName, UTF_8), rfaddress);
    }

    public void setCurrentTemperature(double currentTemperature) {
        this.currentTemperature = currentTemperature;
    }

    public double getCurrentTemperature() {
        return currentTemperature;
    }

    public boolean isWindowOpen() {
        return windowOpen;
    }

    public void setWindowOpen(boolean windowOpen) {
        this.windowOpen = windowOpen;
    }

    public void setConfiguredTemperature(double configuredTemperature) {
        this.configuredTemperature = configuredTemperature;
    }

    public double getConfiguredTemperature() {
        return configuredTemperature;
    }

    public void setValvePositionInPercent(int valvePositionInPercent) {
        this.valvePositionInPercent = valvePositionInPercent;
    }

    public int getValvePositionInPercent() {
        return valvePositionInPercent;
    }

    public boolean isLowBattery() {
        return devices.stream().anyMatch(Device::isLowBattery);
    }
}
