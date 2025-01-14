/* Copyright 2011-2012 Space Applications Services
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
package org.orekit.models.earth.displacement;


import java.util.List;

import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.data.BodiesElements;
import org.orekit.data.FundamentalNutationArguments;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;

public class OceanLoadingCoefficientsBLQFactoryTest {

    @Test
    public void testTruncated() {
        try {
            OceanLoadingCoefficientsBLQFactory factory = new OceanLoadingCoefficientsBLQFactory("^truncated\\.blq$");
            factory.getSites();
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNEXPECTED_END_OF_FILE_AFTER_LINE, oe.getSpecifier());
            Assert.assertEquals(10, oe.getParts()[1]);
        }
    }

    @Test
    public void testCorrupted() {
        try {
            OceanLoadingCoefficientsBLQFactory factory = new OceanLoadingCoefficientsBLQFactory("^corrupted\\.blq$");
            factory.getSites();
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assert.assertEquals(11, oe.getParts()[0]);
        }
    }

    @Test
    public void testOrganization() throws OrekitException {
        TimeScale                          ut1     = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true);
        FundamentalNutationArguments       fna     = IERSConventions.IERS_2010.getNutationArguments(ut1);
        final BodiesElements               el      = fna.evaluateAll(AbsoluteDate.J2000_EPOCH);
        OceanLoadingCoefficientsBLQFactory factory = new OceanLoadingCoefficientsBLQFactory("^hardisp\\.blq$");
        List<String> sites = factory.getSites();
        for (String site : sites) {
            OceanLoadingCoefficients coeffs = factory.getCoefficients(site);
            Assert.assertEquals(3, coeffs.getNbSpecies());
            Assert.assertEquals(3, coeffs.getNbTides(0));
            Assert.assertEquals(4, coeffs.getNbTides(1));
            Assert.assertEquals(4, coeffs.getNbTides(2));
            for (int i = 0; i < coeffs.getNbSpecies(); ++i) {
                for (int j = 1; j < coeffs.getNbTides(i); ++j) {
                    // for each species, tides are sorted in increasing rate order
                    Assert.assertTrue(coeffs.getTide(i, j - 1).getRate(el) < coeffs.getTide(i, j).getRate(el));
                }
            }
        }
    }

    @Test
    public void testHardisp() throws OrekitException {
        OceanLoadingCoefficientsBLQFactory factory = new OceanLoadingCoefficientsBLQFactory("^hardisp\\.blq$");
        List<String> sites = factory.getSites();
        Assert.assertEquals(2, sites.size());
        Assert.assertEquals("Onsala", sites.get(0));
        Assert.assertEquals("Reykjavik", sites.get(1));

        OceanLoadingCoefficients onsalaCoeffs = factory.getCoefficients("OnSaLa");
        Assert.assertEquals("Onsala", onsalaCoeffs.getSiteName());
        Assert.assertEquals(11.9264, FastMath.toDegrees(onsalaCoeffs.getSiteLocation().getLongitude()), 1.0e-15);
        Assert.assertEquals(57.3958, FastMath.toDegrees(onsalaCoeffs.getSiteLocation().getLatitude()),  1.0e-15);
        Assert.assertEquals( 0.0000, onsalaCoeffs.getSiteLocation().getAltitude(),                      1.0e-15);
        Assert.assertEquals( .00352, onsalaCoeffs.getZenithAmplitude(2, 1),                             1.0e-15);
        Assert.assertEquals( .00123, onsalaCoeffs.getZenithAmplitude(2, 2),                             1.0e-15);
        Assert.assertEquals( .00035, onsalaCoeffs.getWestAmplitude(2, 0),                               1.0e-15);
        Assert.assertEquals( .00008, onsalaCoeffs.getWestAmplitude(2, 3),                               1.0e-15);
        Assert.assertEquals( .00029, onsalaCoeffs.getSouthAmplitude(1, 3),                              1.0e-15);
        Assert.assertEquals( .00028, onsalaCoeffs.getSouthAmplitude(1, 1),                              1.0e-15);
        Assert.assertEquals(  -65.6, FastMath.toDegrees(-onsalaCoeffs.getZenithPhase(1, 2)),            1.0e-15);
        Assert.assertEquals( -138.1, FastMath.toDegrees(-onsalaCoeffs.getZenithPhase(1, 0)),            1.0e-15);
        Assert.assertEquals( -167.4, FastMath.toDegrees(-onsalaCoeffs.getWestPhase(0, 2)),              1.0e-15);
        Assert.assertEquals( -170.0, FastMath.toDegrees(-onsalaCoeffs.getWestPhase(0, 1)),              1.0e-15);
        Assert.assertEquals(    5.2, FastMath.toDegrees(-onsalaCoeffs.getSouthPhase(0, 0)),             1.0e-15);
        Assert.assertEquals(  109.5, FastMath.toDegrees(-onsalaCoeffs.getSouthPhase(2, 1)),             1.0e-15);

        // the coordinates for Reykjavik are *known* to be wrong in this test file
        // these test data have been extracted from the HARDISP.F file in September 2017
        // and it seems longitude and latitude have been exchanged...
        // With the file coordinates, Reykjavik would be somewhere in the Indian Ocean, about 1800km East of Madagascar
        // The error has been reported to IERS conventions center.
        OceanLoadingCoefficients reykjavikCoeffs = factory.getCoefficients("Reykjavik");
        Assert.assertEquals("Reykjavik", reykjavikCoeffs.getSiteName());
        Assert.assertEquals( 64.1388, FastMath.toDegrees(reykjavikCoeffs.getSiteLocation().getLongitude()), 1.0e-15);
        Assert.assertEquals(-21.9555, FastMath.toDegrees(reykjavikCoeffs.getSiteLocation().getLatitude()),  1.0e-15);
        Assert.assertEquals(  0.0000, reykjavikCoeffs.getSiteLocation().getAltitude(),                      1.0e-15);
        Assert.assertEquals(  .00034, reykjavikCoeffs.getZenithAmplitude(0, 0),                             1.0e-15);
        Assert.assertEquals(  .00034, reykjavikCoeffs.getZenithAmplitude(0, 1),                             1.0e-15);
        Assert.assertEquals(  .00004, reykjavikCoeffs.getWestAmplitude(0, 2),                               1.0e-15);
        Assert.assertEquals(  .00018, reykjavikCoeffs.getWestAmplitude(1, 0),                               1.0e-15);
        Assert.assertEquals(  .00047, reykjavikCoeffs.getSouthAmplitude(1, 2),                              1.0e-15);
        Assert.assertEquals(  .00066, reykjavikCoeffs.getSouthAmplitude(1, 1),                              1.0e-15);
        Assert.assertEquals(   -52.0, FastMath.toDegrees(-reykjavikCoeffs.getZenithPhase(1, 3)),            1.0e-15);
        Assert.assertEquals(   104.1, FastMath.toDegrees(-reykjavikCoeffs.getZenithPhase(2, 3)),            1.0e-15);
        Assert.assertEquals(    38.9, FastMath.toDegrees(-reykjavikCoeffs.getWestPhase(2, 0)),              1.0e-15);
        Assert.assertEquals(    93.8, FastMath.toDegrees(-reykjavikCoeffs.getWestPhase(2, 2)),              1.0e-15);
        Assert.assertEquals(   156.2, FastMath.toDegrees(-reykjavikCoeffs.getSouthPhase(2, 1)),             1.0e-15);
        Assert.assertEquals(   179.7, FastMath.toDegrees(-reykjavikCoeffs.getSouthPhase(0, 0)),             1.0e-15);

    }

    @Test
    public void testCompleteFormat() throws OrekitException {
        OceanLoadingCoefficientsBLQFactory factory = new OceanLoadingCoefficientsBLQFactory("^complete-format\\.blq$");
        List<String> sites = factory.getSites();
        Assert.assertEquals(4, sites.size());
        Assert.assertEquals("GMRT",           sites.get(0));
        Assert.assertEquals("Goldstone",      sites.get(1));
        Assert.assertEquals("Noumea",         sites.get(2));
        Assert.assertEquals("Pleumeur-Bodou", sites.get(3));

        OceanLoadingCoefficients noumeaCoeffs = factory.getCoefficients("NOUMEA");
        Assert.assertEquals("Noumea", noumeaCoeffs.getSiteName());
        Assert.assertEquals(166.4433, FastMath.toDegrees(noumeaCoeffs.getSiteLocation().getLongitude()), 1.0e-15);
        Assert.assertEquals(-22.2711, FastMath.toDegrees(noumeaCoeffs.getSiteLocation().getLatitude()),  1.0e-15);
        Assert.assertEquals(  0.0000, noumeaCoeffs.getSiteLocation().getAltitude(),                      1.0e-15);
        Assert.assertEquals(  .02070, noumeaCoeffs.getZenithAmplitude(2, 1),                             1.0e-15);
        Assert.assertEquals(  .00346, noumeaCoeffs.getZenithAmplitude(2, 2),                             1.0e-15);
        Assert.assertEquals(  .00153, noumeaCoeffs.getWestAmplitude(2, 0),                               1.0e-15);
        Assert.assertEquals(  .00021, noumeaCoeffs.getWestAmplitude(2, 3),                               1.0e-15);
        Assert.assertEquals(  .00125, noumeaCoeffs.getSouthAmplitude(1, 3),                              1.0e-15);
        Assert.assertEquals(  .00101, noumeaCoeffs.getSouthAmplitude(1, 1),                              1.0e-15);
        Assert.assertEquals(  -152.1, FastMath.toDegrees(-noumeaCoeffs.getZenithPhase(1, 2)),            1.0e-15);
        Assert.assertEquals(   164.2, FastMath.toDegrees(-noumeaCoeffs.getZenithPhase(1, 0)),            1.0e-15);
        Assert.assertEquals(   -89.7, FastMath.toDegrees(-noumeaCoeffs.getWestPhase(0, 2)),              1.0e-15);
        Assert.assertEquals(  -133.6, FastMath.toDegrees(-noumeaCoeffs.getWestPhase(0, 1)),              1.0e-15);
        Assert.assertEquals(  -179.0, FastMath.toDegrees(-noumeaCoeffs.getSouthPhase(0, 0)),             1.0e-15);
        Assert.assertEquals(   -56.2, FastMath.toDegrees(-noumeaCoeffs.getSouthPhase(2, 1)),             1.0e-15);

    }

    @Test
        public void testSeveralFiles() throws OrekitException {
            OceanLoadingCoefficientsBLQFactory factory =
                            new OceanLoadingCoefficientsBLQFactory("^(?:(?:hardisp)|(?:complete-format))\\.blq$");
            List<String> sites = factory.getSites();
            Assert.assertEquals(6, sites.size());
            Assert.assertEquals("GMRT",           sites.get(0));
            Assert.assertEquals("Goldstone",      sites.get(1));
            Assert.assertEquals("Noumea",         sites.get(2));
            Assert.assertEquals("Onsala",         sites.get(3));
            Assert.assertEquals("Pleumeur-Bodou", sites.get(4));
            Assert.assertEquals("Reykjavik",      sites.get(5));
    }

    @Before
    public void setUp() throws Exception {
        Utils.setDataRoot("regular-data:oso-blq");
    }

}
