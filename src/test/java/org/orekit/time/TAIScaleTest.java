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

import org.junit.Test;


public class TAIScaleTest {

    @Test
    public void testZero() {
        TimeScale scale = TAIScale.getInstance();
        for (double dt = -10000; dt < 10000; dt += 123.456789) {
            AbsoluteDate date = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, dt * 86400);
            assertEquals(0, scale.offsetFromTAI(date), 0);
            DateTimeComponents components = date.getComponents(scale);
            assertEquals(0, scale.offsetToTAI(components.getDate(), components.getTime()), 0);
        }
    }

}
