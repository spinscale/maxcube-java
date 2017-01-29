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

import de.spinscale.maxcube.test.CubeTestCase;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Base64;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

// This class tests both generator and parser and thus might not find all the bugs
// if both implementations are broken
public class GeneratorParserTest extends CubeTestCase {

    // example date taken from https://github.com/Bouni/max-cube-protocol/blob/master/S-Message.md
    @Test
    public void testSimpleDateTime() throws Exception{
        // Aug 29, 2011. 02:00
        LocalDateTime dateTime = LocalDateTime.of(2011, Month.AUGUST, 29, 2, 0);
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Generator.writeDateTimeUntil(dateTime, bos);
            byte[] bytes = bos.toByteArray();
            assertThat(bytes.length, Matchers.is(3));
            LocalDateTime parsedDateTime = Parser.readDateTimeUntil(new ByteArrayInputStream(bytes));
            assertThat(parsedDateTime, Matchers.is(dateTime));
            logger.info(Base64.getEncoder().encodeToString(bytes));
        }
    }

    @Test
    public void parseReadDateTimeUntil() throws Exception {
        Month month = randomFrom(Month.values());
        int day = randomIntBetween(1, 28);
        int hourOfDay = randomIntBetween(0, 23);
        int minute = randomFrom(0, 30);
        int year = randomIntBetween(2016, 2025);
        LocalDateTime dateTime = LocalDateTime.of(year, month, day, hourOfDay, minute);

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Generator.writeDateTimeUntil(dateTime, bos);
            LocalDateTime extractedDate = Parser.readDateTimeUntil(new ByteArrayInputStream(bos.toByteArray()));
            assertThat(extractedDate, is(dateTime));
        }
    }
}
