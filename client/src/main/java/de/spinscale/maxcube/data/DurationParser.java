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

import java.time.Duration;

public class DurationParser {

    public static Duration parse(String input) {
        if (input == null || input.trim().length() < 1) {
            throw new IllegalArgumentException("Duration must be a number and a unit");
        }

        input = input.trim();

        String number = input.substring(0, input.length() - 1);
        Long value = Long.valueOf(number);

        if (input.endsWith("s")) {
            return Duration.ofSeconds(value);
        } else if (input.endsWith("m")) {
            return Duration.ofMinutes(value);
        } else if (input.endsWith("h")) {
            return Duration.ofHours(value);
        } else if (input.endsWith("d")) {
            return Duration.ofDays(value);
        }

        throw new IllegalArgumentException("Unknown unit, must be one of s/m/h/d");
    }

}
