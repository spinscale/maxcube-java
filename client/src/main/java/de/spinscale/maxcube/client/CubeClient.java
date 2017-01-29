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
package de.spinscale.maxcube.client;

import de.spinscale.maxcube.entities.Cube;
import de.spinscale.maxcube.entities.Room;

import java.io.Closeable;
import java.time.LocalDateTime;

public interface CubeClient extends Closeable {

    Cube connect() throws Exception;

    /**
     * Boosts the room for the standard configure time
     * @param room The room to boost
     * @return true if the command was send successfully, false otherwise
     */
    boolean boost(Room room) throws Exception;

    /**
     * Sets the holiday temperature for a room for a certain amount of time
     * @param room          The room to set the temperature
     * @param endTime       The time to end the heating
     * @param temperature   The target temperature in degrees celsius
     * @return              true if the command was send successfully, false otherwise
     */
    boolean holiday(Room room, LocalDateTime endTime, int temperature) throws Exception;
}
