/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.time;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.orekit.time.TimeComponents;


public class TimeComponentsTest {

    @Test
    public void testOutOfRange() {
        checkConstructorCompletion(-1, 10, 10, false);
        checkConstructorCompletion(24, 10, 10, false);
        checkConstructorCompletion(10, -1, 10, false);
        checkConstructorCompletion(10, 60, 10, false);
        checkConstructorCompletion(10, 10, -1, false);
        checkConstructorCompletion(10, 10, 61, false);
        checkConstructorCompletion(-1.0, false);
        checkConstructorCompletion(86401.0, false);
    }

    @Test
    public void testInRange() {
        checkConstructorCompletion(10, 10, 10, true);
        checkConstructorCompletion(0.0, true);
        checkConstructorCompletion(10, 10, 60.999, true);
        checkConstructorCompletion(43200.0, true);
        checkConstructorCompletion(86399.999, true);
    }

    @Test
    public void testValues() {
        assertEquals(    0.0, new TimeComponents( 0, 0, 0).getSecondsInDay(), 1.0e-10);
        assertEquals(21600.0, new TimeComponents( 6, 0, 0).getSecondsInDay(), 1.0e-10);
        assertEquals(43200.0, new TimeComponents(12, 0, 0).getSecondsInDay(), 1.0e-10);
        assertEquals(64800.0, new TimeComponents(18, 0, 0).getSecondsInDay(), 1.0e-10);
        assertEquals(86399.9, new TimeComponents(23, 59, 59.9).getSecondsInDay(), 1.0e-10);
    }

    public void testString() {
        assertEquals("00:00:00.000", new TimeComponents(0).toString());
        assertEquals("06:00:00.000", new TimeComponents(21600).toString());
        assertEquals("12:00:00.000", new TimeComponents(43200).toString());
        assertEquals("18:00:00.000", new TimeComponents(64800).toString());
        assertEquals("23:59:59.900", new TimeComponents(86399.9).toString());
    }

    @Test
    public void testComparisons() {
        TimeComponents[] times = {
                 new TimeComponents( 0,  0,  0.0),
                 new TimeComponents( 0,  0,  1.0e-15),
                 new TimeComponents( 0, 12,  3.0),
                 new TimeComponents(15,  9,  3.0),
                 new TimeComponents(23, 59, 59.0),
                 new TimeComponents(23, 59, 60.0 - 1.0e-12)
        };
        for (int i = 0; i < times.length; ++i) {
            for (int j = 0; j < times.length; ++j) {
                if (times[i].compareTo(times[j]) < 0) {
                    assertTrue(times[j].compareTo(times[i]) > 0);
                    assertFalse(times[i].equals(times[j]));
                    assertFalse(times[j].equals(times[i]));
                    assertTrue(times[i].hashCode() != times[j].hashCode());
                    assertTrue(i < j);
                } else if (times[i].compareTo(times[j]) > 0) {
                    assertTrue(times[j].compareTo(times[i]) < 0);
                    assertFalse(times[i].equals(times[j]));
                    assertFalse(times[j].equals(times[i]));
                    assertTrue(times[i].hashCode() != times[j].hashCode());
                    assertTrue(i > j);
                } else {
                    assertTrue(times[j].compareTo(times[i]) == 0);
                    assertTrue(times[i].equals(times[j]));
                    assertTrue(times[j].equals(times[i]));
                    assertTrue(times[i].hashCode() == times[j].hashCode());
                    assertTrue(i == j);
                }
            }
        }
        assertFalse(times[0].equals(this));
    }

    private void checkConstructorCompletion(int hour, int minute, double second,
                                            boolean expectedCompletion) {
        try {
            TimeComponents time = new TimeComponents(hour, minute, second);
            assertEquals(hour,   time.getHour());
            assertEquals(minute, time.getMinute());
            assertEquals(second, time.getSecond(), 1.0e-10);
            assertTrue(expectedCompletion);
        } catch (IllegalArgumentException iae) {
            assertTrue(! expectedCompletion);
        } catch (Exception e) {
            fail("wrong exception caught");
        }
    }

    private void checkConstructorCompletion(double seconds,
                                            boolean expectedCompletion) {
        try {
            TimeComponents time = new TimeComponents(seconds);
            assertEquals(seconds, time.getSecondsInDay(), 1.0e-10);
            assertTrue(expectedCompletion);
        } catch (IllegalArgumentException iae) {
            assertTrue(! expectedCompletion);
        } catch (Exception e) {
            fail("wrong exception caught");
        }
    }

}
