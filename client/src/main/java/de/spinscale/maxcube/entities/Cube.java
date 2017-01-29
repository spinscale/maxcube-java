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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class Cube extends Device {

    final List<Room> rooms = new ArrayList<>();
    private final String firmwareVersion;
    private final LocalDateTime date;

    public Cube(String serial, int rfAddress, String firmwareVersion, LocalDateTime date) {
        super(DeviceType.CUBE, "CUBE", serial, rfAddress);
        this.firmwareVersion = firmwareVersion;
        this.date = date;
    }

    public Device findDeviceBySerial(String serial) {
        if (this.serial.equals(serial)) {
            return this;
        }

        for (Room room : rooms) {
            Optional<Device> device = room.devices.stream().filter(d -> d.getSerial().equals(serial)).findFirst();
            if (device.isPresent()) {
                return device.get();
            }
        }

        throw new IllegalArgumentException(String.format(Locale.ROOT, "Device with serial [%s] does not exist", serial));
    }

    public Device findDeviceByRfAddress(int rfAddress) {
        if (this.getRfaddress() == rfAddress) {
            return this;
        }

        for (Room room : rooms) {
            Optional<Device> device = room.devices.stream().filter(d -> d.getRfaddress() == rfAddress).findFirst();
            if (device.isPresent()) {
                return device.get();
            }
        }

        throw new IllegalArgumentException(String.format(Locale.ROOT, "Device with rfAddress [%s] does not exist", rfAddress));
    }

    public Room findRoomForDevice(int rfaddress) {
        for (Room room : rooms) {
            long count = room.devices.stream().filter(device -> device.rfaddress == rfaddress).count();
            if (count > 0) {
                return room;
            }
        }

        throw new IllegalArgumentException(String.format(Locale.ROOT, "No room with rfaddress [%s] found", rfaddress));
    }

    public Room findRoom(String name) {
        Optional<Room> optional = rooms.stream().filter(room -> room.name.equals(name)).findFirst();
        if (optional.isPresent()) {
            return optional.get();
        }

        throw new IllegalArgumentException(String.format(Locale.ROOT, "Room with name [%s] does not exist", name));
    }

    public Room findRoom(int id) {
        Optional<Room> optional = rooms.stream().filter(room -> room.id == id).findFirst();
        if (optional.isPresent()) {
            return optional.get();
        }

        throw new IllegalArgumentException(String.format(Locale.ROOT, "Room with id [%s] does not exist", id));
    }

    public List<Room> getRooms() {
        return rooms;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public LocalDateTime getDate() {
        return date;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "serial [%s], rooms %s", serial, rooms);
    }
}
