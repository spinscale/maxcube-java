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

public enum DeviceType {
    CUBE(0),
    THERMOSTAST(1),
    THERMOSTAST_PLUS(2),
    WALLTHERMOSTAT(3),
    SHUTTER_CONTACT(4),
    PUSH_BUTTON(5),
    UNKNOWN(99);

    int id;

    DeviceType(int id) {
        this.id = id;
    }

    static DeviceType fromId(int id) {
        for (DeviceType deviceType : values()) {
            if (deviceType.id == id) {
                return deviceType;
            }
        }
        throw new IllegalArgumentException("Non existent device type serial: " + id);
    }

    public boolean isThermostat() {
        return id == THERMOSTAST.id || id == THERMOSTAST_PLUS.id || id == WALLTHERMOSTAT.id;
    }

    @Override
    public String toString() {
        return this.name();
    }
}
