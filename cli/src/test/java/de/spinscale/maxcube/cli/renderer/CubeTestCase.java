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
package de.spinscale.maxcube.cli.renderer;

import com.carrotsearch.randomizedtesting.RandomizedRunner;
import com.carrotsearch.randomizedtesting.RandomizedTest;
import junit.framework.AssertionFailedError;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * TODO: Remove this class and reuse CubeTestCase from client or sth
 */
@RunWith(RandomizedRunner.class)
abstract public class CubeTestCase extends RandomizedTest {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Checks a specific exception class is thrown by the given runnable, and returns it.
     */
    public static <T extends Throwable> T expectThrows(Class<T> expectedType, ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Throwable e) {
            if (expectedType.isInstance(e)) {
                return expectedType.cast(e);
            }
            AssertionFailedError assertion = new AssertionFailedError("Unexpected exception type, expected " + expectedType.getSimpleName());
            assertion.initCause(e);
            throw assertion;
        }
        throw new AssertionFailedError("Expected exception " + expectedType.getSimpleName());
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Throwable;
    }
}
