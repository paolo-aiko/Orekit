/* Copyright 2002-2018 CS Systèmes d'Information
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
package org.orekit.gnss.attitude;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Decimal64;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.ExtendedPVCoordinatesProvider;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

public abstract class AbstractGNSSAttitudeProviderTest {

    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data:gnss");
    }

    protected abstract GNSSAttitudeProvider createProvider(final AbsoluteDate validityStart,
                                                           final AbsoluteDate validityEnd,
                                                           final ExtendedPVCoordinatesProvider sun,
                                                           final Frame inertialFrame,
                                                           final int prnNumber);

    protected enum CheckAxis {

        X_AXIS(Vector3D.PLUS_I, (x, z) -> x),
        Y_AXIS(Vector3D.PLUS_J, (x, z) -> Vector3D.crossProduct(z, x)),
        Z_AXIS(Vector3D.PLUS_K, (x, z) -> z);

        final Vector3D canonical;
        final BiFunction<Vector3D, Vector3D, Vector3D> getRefAxis;

        CheckAxis(final Vector3D canonical, final BiFunction<Vector3D, Vector3D, Vector3D> getRefAxis) {
            this.canonical  = canonical;
            this.getRefAxis = getRefAxis;
        }

        double error(final Attitude attitude, final Vector3D x, final Vector3D z) {
            final Vector3D computedAxis  = attitude.getRotation().applyInverseTo(canonical);
            final Vector3D referenceAxis = getRefAxis.apply(x, z);
            return Vector3D.angle(computedAxis, referenceAxis);
        }

    }

    protected void doTestAxes(final String fileName, final double tolX, double tolY, double tolZ)
        throws OrekitException {

        if (getClass().getResource("/gnss/attitude/" + fileName) == null) {
            Assert.fail("file not found: " + fileName);
        }

        // the transforms between EME2000 and ITRF will not really be correct here
        // because the corresponding EOP are not present in the resources used
        // however, this is not a problem because we rely only on data generated
        // in ITRF and fully consistent (both EOP and Sun ephemeris were used at
        // data generation phase). The test performed here will convert back
        // to EME2000 (which will be slightly offset due to missing EOP), but
        // Sun/Earth/spacecraft relative geometry will remain consistent
        final Frame eme2000 = FramesFactory.getEME2000();
        final Frame itrf    = FramesFactory.getITRF(IERSConventions.IERS_2010, false);
        final List<List<ParsedLine>> dataBlocks = parseFile(fileName, eme2000, itrf);
        double maxErrorX = 0;
        double maxErrorY = 0;
        double maxErrorZ = 0;
        for (final List<ParsedLine> dataBlock : dataBlocks) {
            final AbsoluteDate validityStart = dataBlock.get(0).date;
            final AbsoluteDate validityEnd   = dataBlock.get(dataBlock.size() - 1).date;
            final int          prnNumber     = dataBlock.get(0).prnNumber;
            final ExtendedPVCoordinatesProvider fakedSun = new FakedSun(dataBlock);
            final GNSSAttitudeProvider attitudeProvider =
                            createProvider(validityStart, validityEnd, fakedSun, eme2000, prnNumber);
            Assert.assertEquals(attitudeProvider.validityStart(), dataBlock.get(0).date);
            Assert.assertEquals(attitudeProvider.validityEnd(), dataBlock.get(dataBlock.size() - 1).date);

            for (final ParsedLine parsedLine : dataBlock) {

                // test on primitive double
                final Attitude attitude1 = attitudeProvider.getAttitude(parsedLine.orbit, parsedLine.date, parsedLine.orbit.getFrame());
                final Vector3D x = parsedLine.eclipsX;
                final Vector3D z = parsedLine.orbit.getPVCoordinates().getPosition().normalize().negate();
                maxErrorX = FastMath.max(maxErrorX, CheckAxis.X_AXIS.error(attitude1, x, z));
                maxErrorY = FastMath.max(maxErrorY, CheckAxis.Y_AXIS.error(attitude1, x, z));
                maxErrorZ = FastMath.max(maxErrorZ, CheckAxis.Z_AXIS.error(attitude1, x, z));

                // test on field
                final Field<Decimal64> field = Decimal64Field.getInstance();
                final FieldPVCoordinates<Decimal64> pv64 = new FieldPVCoordinates<>(field, parsedLine.orbit.getPVCoordinates());
                final FieldAbsoluteDate<Decimal64> date64 =  new FieldAbsoluteDate<>(field, parsedLine.date);
                final FieldCartesianOrbit<Decimal64> orbit64 = new FieldCartesianOrbit<>(pv64,
                                                                                         parsedLine.orbit.getFrame(),
                                                                                         date64,
                                                                                         parsedLine.orbit.getMu());
                final FieldAttitude<Decimal64> attitude64 =
                                attitudeProvider.getAttitude(orbit64, orbit64.getDate(), parsedLine.orbit.getFrame());
                final Attitude attitude2 = attitude64.toAttitude();
                maxErrorX = FastMath.max(maxErrorX, CheckAxis.X_AXIS.error(attitude2, x, z));
                maxErrorY = FastMath.max(maxErrorY, CheckAxis.Y_AXIS.error(attitude2, x, z));
                maxErrorZ = FastMath.max(maxErrorZ, CheckAxis.Z_AXIS.error(attitude2, x, z));

            }

        }

        Assert.assertEquals(0, maxErrorX, tolX);
        Assert.assertEquals(0, maxErrorY, tolY);
        Assert.assertEquals(0, maxErrorZ, tolZ);

    }

    private List<List<ParsedLine>> parseFile(final String fileName, final Frame eme2000, final Frame itrf)
        throws OrekitException {
        final List<List<ParsedLine>> dataBlocks = new ArrayList<>();
        try (InputStream is = getClass().getResourceAsStream("/gnss/attitude/" + fileName);
             Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(reader)) {

            // parse the reference data file into contiguous blocks
            dataBlocks.add(new ArrayList<>());
            ParsedLine parsedLine = null;
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                line = line.trim();
                if (line.startsWith("#")) {
                    continue;
                }
                final ParsedLine previous = parsedLine;
                parsedLine = new ParsedLine(line, eme2000, itrf);
                if (previous != null &&
                    (parsedLine.prnNumber != previous.prnNumber ||
                     parsedLine.date.durationFrom(previous.date) > 3600)) {
                    dataBlocks.add(new ArrayList<>());
                }
                dataBlocks.get(dataBlocks.size() - 1).add(parsedLine);
            }

        } catch (IOException ioe) {
            Assert.fail(ioe.getLocalizedMessage());
        }

        return dataBlocks;

    }

    private static class FakedSun implements ExtendedPVCoordinatesProvider {

        final List<ParsedLine> parsedLines;

        FakedSun(final List<ParsedLine> parsedLines) {
            this.parsedLines = parsedLines;
        }

        @Override
        public TimeStampedPVCoordinates getPVCoordinates(AbsoluteDate date,
                                                         Frame frame) {
            return TimeStampedPVCoordinates.interpolate(date,
                                                        CartesianDerivativesFilter.USE_P,
                                                        parsedLines.stream().
                                                        filter(parsedLine ->
                                                        FastMath.abs(date.durationFrom(parsedLine.date)) < 300).
                                                        map(parsedLine ->
                                                            new TimeStampedPVCoordinates(parsedLine.date,
                                                                                         parsedLine.sunP,
                                                                                         Vector3D.ZERO,
                                                                                         Vector3D.ZERO)));
        }

        @Override
        public <T extends RealFieldElement<T>> TimeStampedFieldPVCoordinates<T>
            getPVCoordinates(FieldAbsoluteDate<T> date, Frame frame) {
            final Field<T> field = date.getField();
            return TimeStampedFieldPVCoordinates.interpolate(date,
                                                             CartesianDerivativesFilter.USE_P,
                                                             parsedLines.stream().
                                                             filter(parsedLine ->
                                                             FastMath.abs(date.durationFrom(parsedLine.date)).getReal() < 300).
                                                             map(parsedLine ->
                                                                 new TimeStampedFieldPVCoordinates<>(parsedLine.date,
                                                                                                     new FieldVector3D<>(field, parsedLine.sunP),
                                                                                                     FieldVector3D.getZero(field),
                                                                                                     FieldVector3D.getZero(field))));
        }

    }

    private static class ParsedLine {

        final AbsoluteDate date;
        final int          prnNumber;
        final Orbit        orbit;
        final Vector3D     sunP;
        final Vector3D     eclipsX;

        ParsedLine(final String line, final Frame eme2000, final Frame itrf) throws OrekitException {
            final String[] fields = line.split("\\s+");
            date       = AbsoluteDate.createGPSDate(Integer.parseInt(fields[1]),
                                                    Double.parseDouble(fields[2]));
            final Transform t = itrf.getTransformTo(eme2000, date);
            prnNumber  = Integer.parseInt(fields[3].substring(1));
            orbit      = new CartesianOrbit(new TimeStampedPVCoordinates(date,
                                                                         t.transformPosition(new Vector3D(Double.parseDouble(fields[ 6]),
                                                                                                          Double.parseDouble(fields[ 7]),
                                                                                                          Double.parseDouble(fields[ 8]))),
                                                                         t.transformVector(new Vector3D(Double.parseDouble(fields[ 9]),
                                                                                                        Double.parseDouble(fields[10]),
                                                                                                        Double.parseDouble(fields[11])))),
                                            eme2000, Constants.EIGEN5C_EARTH_MU);
            sunP       = t.transformPosition(new Vector3D(Double.parseDouble(fields[12]),
                                                          Double.parseDouble(fields[13]),
                                                          Double.parseDouble(fields[14])));
//            beta       = FastMath.toRadians(Double.parseDouble(fields[15]));
//            delta      = FastMath.toRadians(Double.parseDouble(fields[16]));
//            nominalX   = t.transformVector(new Vector3D(Double.parseDouble(fields[17]),
//                                                        Double.parseDouble(fields[18]),
//                                                        Double.parseDouble(fields[19])));
//            nominalPsi = FastMath.toRadians(Double.parseDouble(fields[20]));
            eclipsX    = t.transformVector(new Vector3D(Double.parseDouble(fields[21]),
                                                        Double.parseDouble(fields[22]),
                                                        Double.parseDouble(fields[23])));
//            eclipsPsi  = FastMath.toRadians(Double.parseDouble(fields[24]));
        }

    }

}
