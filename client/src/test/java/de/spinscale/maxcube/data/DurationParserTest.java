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
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class DurationParserTest extends CubeTestCase {

    @Test
    public void testDurationParsing() {
        assertThat(DurationParser.parse("10m").getSeconds(), is(600L));
        assertThat(DurationParser.parse("600s").getSeconds(), is(600L));
        assertThat(DurationParser.parse("1h").getSeconds(), is(3600L));
        assertThat(DurationParser.parse("2d").getSeconds(), is(172800L));
    }

    @Test
    public void testTimeUnitMustBeSupplied() {
        expectThrows(IllegalArgumentException.class, () -> DurationParser.parse("1000"));
        expectThrows(IllegalArgumentException.class, () -> DurationParser.parse(""));
        expectThrows(IllegalArgumentException.class, () -> DurationParser.parse(null));
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> DurationParser.parse("100hours"));
        assertThat(e.getMessage(), is("For input string: \"100hour\""));
    }

}