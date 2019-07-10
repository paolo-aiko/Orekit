/* Copyright 2002-2019 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.gnss;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.data.GzipFilter;
import org.orekit.data.NamedData;
import org.orekit.data.UnixCompressFilter;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

public class HatanakaCompressFilterTest {
    
    
    @Test
    public void testNotFiltered() throws IOException {
        
        final String name = "rinex/aaaa0000.00o";
        final NamedData raw = new NamedData(name,
                                            () -> Utils.class.getClassLoader().getResourceAsStream(name));
        final NamedData filtered = new HatanakaCompressFilter().filter(new UnixCompressFilter().filter(raw));
        Assert.assertSame(raw, filtered);
    }

    @Test
    public void testWrongVersion() throws IOException {
        doTestWrong("rinex/vers9990.01d", OrekitMessages.UNSUPPORTED_FILE_FORMAT);
    }

    @Test
    public void testWrongFirstLabel() throws IOException {
        doTestWrong("rinex/labl8880.01d", OrekitMessages.NOT_A_SUPPORTED_HATANAKA_COMPRESSED_FILE);
    }

    @Test
    public void testWrongSecondLabel() throws IOException {
        doTestWrong("rinex/labl9990.01d", OrekitMessages.NOT_A_SUPPORTED_HATANAKA_COMPRESSED_FILE);
    }

    @Test
    public void testTruncatedAtReceiverClockLine() throws IOException {
        doTestWrong("rinex/truncated-crinex.crx", OrekitMessages.UNEXPECTED_END_OF_FILE);
    }

    private void doTestWrong(final String name, final OrekitMessages expectedError)
        throws IOException {
        final NamedData raw = new NamedData(name.substring(name.indexOf('/') + 1),
                                            () -> Utils.class.getClassLoader().getResourceAsStream(name));
        try {
            try (InputStream       is  = new HatanakaCompressFilter().filter(raw).getStreamOpener().openStream();
                 InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                 BufferedReader    br  = new BufferedReader(isr)) {
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    // nothing to do here
                }
                Assert.fail("an exception should have been thrown");
            }
        } catch (OrekitException oe) {
            Assert.assertEquals(expectedError, oe.getSpecifier());
        }
    }

    @Test
    public void testRinex2MoreThan12Satellites() throws IOException {

        final String name = "rinex/bogi1210.09d.Z";
        final NamedData raw = new NamedData(name.substring(name.indexOf('/') + 1),
                                            () -> Utils.class.getClassLoader().getResourceAsStream(name));
        NamedData filtered = new HatanakaCompressFilter().filter(new UnixCompressFilter().filter(raw));
        RinexLoader loader = new RinexLoader(filtered.getStreamOpener().openStream(), filtered.getName());

        List<ObservationDataSet> ods = loader.getObservationDataSets();
        Assert.assertEquals(135, ods.size());
        AbsoluteDate lastEpoch = AbsoluteDate.PAST_INFINITY;
        int epochCount = 0;
        for (final ObservationDataSet ds : ods) {
            if (ds.getDate().durationFrom(lastEpoch) > 1.0e-3) {
                ++epochCount;
                lastEpoch = ds.getDate();
            }
        }
        Assert.assertEquals(9, epochCount);
    }

    @Test
    public void testHatanakaRinex2() throws IOException {

        final String name = "rinex/arol0090.01d.Z";
        final NamedData raw = new NamedData(name.substring(name.indexOf('/') + 1),
                                            () -> Utils.class.getClassLoader().getResourceAsStream(name));
        NamedData filtered = new HatanakaCompressFilter().filter(new UnixCompressFilter().filter(raw));
        RinexLoader loader = new RinexLoader(filtered.getStreamOpener().openStream(), filtered.getName());

        AbsoluteDate t0 = new AbsoluteDate(2001, 1, 9, TimeScalesFactory.getGPS());
        List<ObservationDataSet> ods = loader.getObservationDataSets();
        Assert.assertEquals(921, ods.size());

        Assert.assertEquals("AROL",              ods.get(0).getHeader().getMarkerName());
        Assert.assertEquals(SatelliteSystem.GPS, ods.get(0).getSatelliteSystem());
        Assert.assertEquals(24,                  ods.get(0).getPrnNumber());
        Assert.assertEquals(90.0,                ods.get(0).getDate().durationFrom(t0), 1.0e-15);
        Assert.assertEquals(7,                   ods.get(0).getObservationData().size());
        Assert.assertEquals(-3351623.823,        ods.get(0).getObservationData().get(0).getValue(), 1.0e-3);
        Assert.assertEquals(-2502276.763,        ods.get(0).getObservationData().get(1).getValue(), 1.0e-3);
        Assert.assertEquals(21472157.836,        ods.get(0).getObservationData().get(2).getValue(), 1.0e-3);
        Assert.assertEquals(21472163.602,        ods.get(0).getObservationData().get(3).getValue(), 1.0e-3);
        Assert.assertTrue(Double.isNaN(ods.get(0).getObservationData().get(4).getValue()));
        Assert.assertEquals(18.7504,             ods.get(0).getObservationData().get(5).getValue(), 1.0e-3);
        Assert.assertEquals(19.7504,             ods.get(0).getObservationData().get(6).getValue(), 1.0e-3);

        Assert.assertEquals("AROL",              ods.get(447).getHeader().getMarkerName());
        Assert.assertEquals(SatelliteSystem.GPS, ods.get(447).getSatelliteSystem());
        Assert.assertEquals(10,                  ods.get(447).getPrnNumber());
        Assert.assertEquals(2310.0,              ods.get(447).getDate().durationFrom(t0), 1.0e-15);
        Assert.assertEquals(7,                   ods.get(447).getObservationData().size());
        Assert.assertEquals(-8892260.422,        ods.get(447).getObservationData().get(0).getValue(), 1.0e-3);
        Assert.assertEquals(-6823186.119,        ods.get(447).getObservationData().get(1).getValue(), 1.0e-3);
        Assert.assertEquals(22280029.148,        ods.get(447).getObservationData().get(2).getValue(), 1.0e-3);
        Assert.assertEquals(22280035.160,        ods.get(447).getObservationData().get(3).getValue(), 1.0e-3);
        Assert.assertTrue(Double.isNaN(ods.get(447).getObservationData().get(4).getValue()));
        Assert.assertEquals(14.2504,             ods.get(447).getObservationData().get(5).getValue(), 1.0e-3);
        Assert.assertEquals(13.2504,             ods.get(447).getObservationData().get(6).getValue(), 1.0e-3);

        Assert.assertEquals("AROL",              ods.get(920).getHeader().getMarkerName());
        Assert.assertEquals(SatelliteSystem.GPS, ods.get(920).getSatelliteSystem());
        Assert.assertEquals(31,                  ods.get(920).getPrnNumber());
        Assert.assertEquals(71430.0,             ods.get(920).getDate().durationFrom(t0), 1.0e-15);
        Assert.assertEquals(7,                   ods.get(920).getObservationData().size());
        Assert.assertEquals(-3993480.91843,      ods.get(920).getObservationData().get(0).getValue(), 1.0e-3);
        Assert.assertEquals(-3363000.11542,      ods.get(920).getObservationData().get(1).getValue(), 1.0e-3);
        Assert.assertEquals(24246301.1804,       ods.get(920).getObservationData().get(2).getValue(), 1.0e-3);
        Assert.assertEquals(24246308.9304,       ods.get(920).getObservationData().get(3).getValue(), 1.0e-3);
        Assert.assertTrue(Double.isNaN(ods.get(920).getObservationData().get(4).getValue()));
        Assert.assertEquals(6.2504,              ods.get(920).getObservationData().get(5).getValue(), 1.0e-3);
        Assert.assertEquals(2.2504,              ods.get(920).getObservationData().get(6).getValue(), 1.0e-3);

    }

    @Test
    public void testCompressedRinex3() throws IOException {
        
        //Tests Rinex 3 with Hatanaka compression
        final String name = "rinex/GANP00SVK_R_20151890000_01H_10M_MO.crx.gz";
        final NamedData raw = new NamedData(name.substring(name.indexOf('/') + 1),
                                            () -> Utils.class.getClassLoader().getResourceAsStream(name));
        NamedData filtered = new HatanakaCompressFilter().filter(new GzipFilter().filter(raw));
        RinexLoader loader = new RinexLoader(filtered.getStreamOpener().openStream(), filtered.getName());

        AbsoluteDate t0 = new AbsoluteDate(2015, 7, 8, TimeScalesFactory.getGPS());
        List<ObservationDataSet> ods = loader.getObservationDataSets();
        Assert.assertEquals(188, ods.size());

        Assert.assertEquals("GANP",                  ods.get(0).getHeader().getMarkerName());
        Assert.assertEquals(SatelliteSystem.BEIDOU,  ods.get(0).getSatelliteSystem());
        Assert.assertEquals(2,                       ods.get(0).getPrnNumber());
        Assert.assertEquals(0.0,                     ods.get(0).getDate().durationFrom(t0), 1.0e-15);
        Assert.assertEquals(6,                       ods.get(0).getObservationData().size());
        Assert.assertEquals(40517356.773,            ods.get(0).getObservationData().get(0).getValue(), 1.0e-3);
        Assert.assertEquals(40517351.688,            ods.get(0).getObservationData().get(1).getValue(), 1.0e-3);
        Assert.assertEquals(210984654.306,           ods.get(0).getObservationData().get(2).getValue(), 1.0e-3);
        Assert.assertEquals(163146718.773,           ods.get(0).getObservationData().get(3).getValue(), 1.0e-3);
        Assert.assertEquals(35.400,                  ods.get(0).getObservationData().get(4).getValue(), 1.0e-3);
        Assert.assertEquals(37.900,                  ods.get(0).getObservationData().get(5).getValue(), 1.0e-3);

        Assert.assertEquals("GANP",                  ods.get(96).getHeader().getMarkerName());
        Assert.assertEquals(SatelliteSystem.GLONASS, ods.get(96).getSatelliteSystem());
        Assert.assertEquals(20,                      ods.get(96).getPrnNumber());
        Assert.assertEquals(1200.0,                  ods.get(96).getDate().durationFrom(t0), 1.0e-15);
        Assert.assertEquals(12,                      ods.get(96).getObservationData().size());
        Assert.assertEquals(21579038.953,            ods.get(96).getObservationData().get(0).getValue(), 1.0e-3);
        Assert.assertEquals(21579038.254,            ods.get(96).getObservationData().get(1).getValue(), 1.0e-3);
        Assert.assertEquals(21579044.469,            ods.get(96).getObservationData().get(2).getValue(), 1.0e-3);
        Assert.assertEquals(21579043.914,            ods.get(96).getObservationData().get(3).getValue(), 1.0e-3);
        Assert.assertEquals(115392840.925,           ods.get(96).getObservationData().get(4).getValue(), 1.0e-3);
        Assert.assertEquals(115393074.174,           ods.get(96).getObservationData().get(5).getValue(), 1.0e-3);
        Assert.assertEquals(89750072.711,            ods.get(96).getObservationData().get(6).getValue(), 1.0e-3);
        Assert.assertEquals(89750023.963,            ods.get(96).getObservationData().get(7).getValue(), 1.0e-3);
        Assert.assertEquals(43.800,                  ods.get(96).getObservationData().get(8).getValue(), 1.0e-3);
        Assert.assertEquals(42.500,                  ods.get(96).getObservationData().get(9).getValue(), 1.0e-3);
        Assert.assertEquals(44.000,                  ods.get(96).getObservationData().get(10).getValue(), 1.0e-3);
        Assert.assertEquals(44.000,                  ods.get(96).getObservationData().get(11).getValue(), 1.0e-3);

        Assert.assertEquals("GANP",                  ods.get(187).getHeader().getMarkerName());
        Assert.assertEquals(SatelliteSystem.SBAS,    ods.get(187).getSatelliteSystem());
        Assert.assertEquals(126,                     ods.get(187).getPrnNumber());
        Assert.assertEquals(3000.0,                  ods.get(187).getDate().durationFrom(t0), 1.0e-15);
        Assert.assertEquals(3,                       ods.get(187).getObservationData().size());
        Assert.assertEquals(38446689.984,            ods.get(187).getObservationData().get(0).getValue(), 1.0e-3);
        Assert.assertEquals(202027899.813,           ods.get(187).getObservationData().get(1).getValue(), 1.0e-3);
        Assert.assertEquals(40.200,                  ods.get(187).getObservationData().get(2).getValue(), 1.0e-3);

    }

    @Test
    public void testWith5thOrderDifferencesClockOffsetReinitialization() throws IOException {

        // the following file has several specific features with respect to Hatanaka compression
        //  - we created it using 5th order differences instead of standard 3rd order
        //  - epoch lines do contain a clock offset (which is a dummy value manually edited from original IGS file)
        //  - differences are reinitialized every 20 epochs
        final String name = "rinex/ZIMM00CHE_R_20190320000_15M_30S_MO.crx.gz";
        final NamedData raw = new NamedData(name.substring(name.indexOf('/') + 1),
                                            () -> Utils.class.getClassLoader().getResourceAsStream(name));
        NamedData filtered = new HatanakaCompressFilter().filter(new GzipFilter().filter(raw));
        RinexLoader loader = new RinexLoader(filtered.getStreamOpener().openStream(), filtered.getName());

        List<ObservationDataSet> ods = loader.getObservationDataSets();
        Assert.assertEquals(30, ods.size());
        for (final ObservationDataSet dataSet : ods) {
            Assert.assertEquals(0.123456789012, dataSet.getRcvrClkOffset(), 1.0e-15);
        }
        ObservationDataSet last = ods.get(ods.size() - 1);
        Assert.assertEquals( 24815572.703, last.getObservationData().get(0).getValue(), 1.0e-4);
        Assert.assertEquals(130406727.683, last.getObservationData().get(1).getValue(), 1.0e-4);
    }

    @Test
    public void testDifferential3rdOrder() {
        doTestDifferential(15, 3, 3,
                           new long[] {
                               40517356773l, -991203l, -38437l,
                               3506l, -630l, 2560l
                           }, new String[] {
                               "   40517356.773", "   40516365.570", "   40515335.930",
                               "   40514271.359", "   40513171.227", "   40512038.094"
                           });
    }

    @Test
    public void testDifferential5thOrder() {
        doTestDifferential(12, 5, 5,
                           new long[] {
                               23439008766l, -19297641l, 30704l, 3623l, -8215l,
                               14517l, -6644l, -2073l, 4164l, -2513l
                           }, new String[] {
                               "234390.08766", "234197.11125", "234004.44188", "233812.11578", "233620.08703",
                               "233428.37273", "233236.98656", "233045.91805", "232855.17422", "232664.75445"
                           });
    }

    private void doTestDifferential(final int fieldLength, final int decimalPlaces, final int order,
                                    final long[] compressed, final String[] uncompressed) {
        try {
            Class<?> differentialClass = null;
            for (final Class<?> c : HatanakaCompressFilter.class.getDeclaredClasses()) {
                if (c.getName().endsWith("NumericDifferential")) {
                    differentialClass = c;
                }
            }
            final Constructor<?> cstr = differentialClass.getDeclaredConstructor(Integer.TYPE, Integer.TYPE, Integer.TYPE);
            cstr.setAccessible(true);
            final Object differential = cstr.newInstance(fieldLength, decimalPlaces, order);
            final Method acceptMethod = differentialClass.getDeclaredMethod("accept", Long.TYPE);

            for (int i = 0; i < compressed.length; ++i) {
                Assert.assertEquals(uncompressed[i], acceptMethod.invoke(differential, compressed[i]));
            }

        } catch (NoSuchMethodException | SecurityException | InstantiationException |
                 IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            Assert.fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testTextDates() {
        doTestText(35,
                   new String[] {
                       "------@> 2015 07 08 00 00 00.0000000  0 34@----------",
                       "------@                1@",
                       "------@                2                 2@----------",
                       "------@                3                 1@----------",
                       "------@                4                 0@----------",
                       "------@                5                27@----------"
                   }, new String[] {
                       "> 2015 07 08 00 00 00.0000000  0 34",
                       "> 2015 07 08 00 10 00.0000000  0 34",
                       "> 2015 07 08 00 20 00.0000000  0 32",
                       "> 2015 07 08 00 30 00.0000000  0 31",
                       "> 2015 07 08 00 40 00.0000000  0 30",
                       "> 2015 07 08 00 50 00.0000000  0 27"
                   });
    }

    @Test
    public void testTextSats() {
        doTestText(108,
                   new String[] {
                       "> 2015 07 08 00 00 00.0000000  0 34@      C02C05C07C10C14E11E12E19E20G01G02G03G06G07G09G10G16G17G23G26G31G32R03R04R05R12R13R14R15R19R20R21S20S26@",
                       "                1@@",
                       "                2                 2@                                                          23  6 31  2R03  4  5 13  4  5  9 20 21S  S 6&&&&&&@",
                       "                3                 1@                  E 1  2  9 20G01  2  3  6  7  9 10  6 23  6 31  2R03  4  5 13  4  5  9 20  1S 0  6&&&@",
                       "                4                 0@                                2  3  6  7  9 10  6 23  6 31  2R03  4  5 13  4  5  9 20  1S 0  6&&&@",
                       "                5                27@                                                         R03R04  5 13 14  5 20 21S20S 6&&&&&&&&&@"
                   }, new String[] {
                       "      C02C05C07C10C14E11E12E19E20G01G02G03G06G07G09G10G16G17G23G26G31G32R03R04R05R12R13R14R15R19R20R21S20S26",
                       "      C02C05C07C10C14E11E12E19E20G01G02G03G06G07G09G10G16G17G23G26G31G32R03R04R05R12R13R14R15R19R20R21S20S26",
                       "      C02C05C07C10C14E11E12E19E20G01G02G03G06G07G09G10G16G23G26G31G32R03R04R05R13R14R15R19R20R21S20S26      ",
                       "      C02C05C07C10E11E12E19E20G01G02G03G06G07G09G10G16G23G26G31G32R03R04R05R13R14R15R19R20R21S20S26         ",
                       "      C02C05C07C10E11E12E19E20G02G03G06G07G09G10G16G23G26G31G32R03R04R05R13R14R15R19R20R21S20S26            ",
                       "      C02C05C07C10E11E12E19E20G02G03G06G07G09G10G16G23G26R03R04R05R13R14R15R20R21S20S26                     "
                   });
    }

    private void doTestText(final int fieldLength, final String[] compressed, final String[] uncompressed) {
        try {
            Class<?> textClass = null;
            for (final Class<?> c : HatanakaCompressFilter.class.getDeclaredClasses()) {
                if (c.getName().endsWith("TextDifferential")) {
                    textClass = c;
                }
            }
            final Constructor<?> cstr = textClass.getDeclaredConstructor(Integer.TYPE);
            cstr.setAccessible(true);
            final Object differentialClass = cstr.newInstance(fieldLength);
            final Method acceptMethod = textClass.getDeclaredMethod("accept", CharSequence.class,
                                                                    Integer.TYPE, Integer.TYPE);

            for (int i = 0; i < compressed.length; ++i) {
                Assert.assertEquals(uncompressed[i],
                                    acceptMethod.invoke(differentialClass,
                                    compressed[i], compressed[i].indexOf('@') + 1, compressed[i].lastIndexOf('@')));
            }

        } catch (NoSuchMethodException | SecurityException | InstantiationException |
                 IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            Assert.fail(e.getLocalizedMessage());
        }
    }

}
