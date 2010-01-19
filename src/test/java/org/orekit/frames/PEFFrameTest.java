/* Copyright 2002-2010 CS Communication & Systèmes
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
package org.orekit.frames;


import org.apache.commons.math.geometry.Vector3D;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;


public class PEFFrameTest {

    @Test
    public void testAASReferenceLEO() throws OrekitException {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2004, 04, 06),
                                           new TimeComponents(07, 51, 28.386009),
                                           TimeScalesFactory.getUTC());

        // PEF iau76
        PVCoordinates pvPEF =
           new PVCoordinates(new Vector3D(-1033475.0313, 7901305.5856, 6380344.5328),
                             new Vector3D(-3225.632747, -2872.442511, 5531.931288));

        // TOD iau76
        PVCoordinates pvTEME =
            new PVCoordinates(new Vector3D(5094514.7804, 6127366.4612, 6380344.5328),
                              new Vector3D(-4746.088567, 786.077222, 5531.931288));
        
        Transform t = FramesFactory.getTEME(true).getTransformTo(FramesFactory.getPEF(true), t0);

        // this test gives worst result than PEFFrameAlternateConfigurationTest because
        // at 2004-04-06 there is a 0.471ms difference in dut1 and a 0.077ms difference
        // in lod with the data used by Vallado to set up this test case
        PVCoordinates delta = new PVCoordinates(1.0, pvPEF, -1.0, t.transformPVCoordinates(pvTEME));
        Assert.assertEquals(0.283011, delta.getPosition().getNorm(), 1.0e-8);
        Assert.assertEquals(1.533846e-4, delta.getVelocity().getNorm(), 3.0e-11);

        // if dut1 and lod corrections are ignored, results must be really bad
        t = FramesFactory.getTEME(false).getTransformTo(FramesFactory.getPEF(false), t0);
        delta = new PVCoordinates(1.0, pvPEF, -1.0, t.transformPVCoordinates(pvTEME));
        Assert.assertEquals(255.644, delta.getPosition().getNorm(), 4.0e-6);
        Assert.assertEquals(0.13856, delta.getVelocity().getNorm(), 9.0e-7);

    }

    @Test
    public void testAASReferenceGEO() throws OrekitException {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2004, 06, 01),
                                           TimeComponents.H00,
                                           TimeScalesFactory.getUTC());

        Transform t = FramesFactory.getTEME(true).getTransformTo(FramesFactory.getPEF(true), t0);

        // TOD iau76
        PVCoordinates pvTEME =
            new PVCoordinates(new Vector3D(-40577427.7501, -11500096.1306, 10293.2583),
                              new Vector3D(837.552338, -2957.524176, -0.928772));

        //PEF iau76
        PVCoordinates pvPEF =
            new PVCoordinates(new Vector3D(24796919.2956, -34115870.9001, 10293.2583),
                              new Vector3D(-0.979178, -1.476540, -0.928772));

        // this test gives worst result than PEFFrameAlternateConfigurationTest because
        // at 2004-06-01 there is a 0.047ms difference in dut1 and a 0.416ms difference
        // in lod with the data used by Vallado to set up this test case
        PVCoordinates delta = new PVCoordinates(1.0, pvPEF, -1.0, t.transformPVCoordinates(pvTEME));
        Assert.assertEquals(0.193876, delta.getPosition().getNorm(), 2.0e-7);
        Assert.assertEquals(1.427464e-5, delta.getVelocity().getNorm(), 9.0e-12);

        // if dut1 and lod corrections are ignored, results must be really bad
        t = FramesFactory.getTEME(false).getTransformTo(FramesFactory.getPEF(false), t0);
        delta = new PVCoordinates(1.0, pvPEF, -1.0, t.transformPVCoordinates(pvTEME));
        Assert.assertEquals(1448.217, delta.getPosition().getNorm(), 4.0e-4);
        Assert.assertEquals(6.1e-5, delta.getVelocity().getNorm(), 2.0e-8);

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

}
