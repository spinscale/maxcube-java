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

import de.spinscale.maxcube.test.CubeTestCase;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class CubeTest extends CubeTestCase {

    private Cube cube = new Cube(randomAsciiOfLength(10), randomIntBetween(1, 1000), randomAsciiOfLength(4), LocalDateTime.now());
    private Room room = new Room(1, randomAsciiOfLength(4), randomIntBetween(1000, 2000));
    private Device device = new Device(randomFrom(DeviceType.values()), randomAsciiOfLength(5), randomAsciiOfLength(5), randomIntBetween(2000, 3000));

    @Before
    public void setupRoomAndDevice() {
        cube.rooms.add(room);
        room.getDevices().add(device);
    }

    @Test
    public void testFindingRooms() {
        assertThat(cube.findRoom(room.getId()), is(room));
        assertThat(cube.findRoom(room.getName()), is(room));
        assertThat(cube.findRoom(room.getName()), is(room));
        assertThat(cube.findRoomForDevice(device.getRfaddress()), is(room));

        expectThrows(IllegalArgumentException.class, () -> cube.findRoom(0));
        expectThrows(IllegalArgumentException.class, () -> cube.findRoom(randomAsciiOfLength(8)));
        expectThrows(IllegalArgumentException.class, () -> cube.findRoomForDevice(randomInt(10)));
    }

    @Test
    public void testFindingDevices() {
        assertThat(cube.findDeviceBySerial(device.getSerial()), is(device));
        assertThat(cube.findDeviceByRfAddress(device.getRfaddress()), is(device));

        expectThrows(IllegalArgumentException.class, () -> cube.findDeviceBySerial(randomAsciiOfLength(8)));
        expectThrows(IllegalArgumentException.class, () -> cube.findDeviceByRfAddress(randomInt(10)));
    }
}
