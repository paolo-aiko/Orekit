/* Copyright 2002-2014 CS Systèmes d'Information
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
package org.orekit.attitudes;


import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.util.FastMath;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.RRASampleFilter;


public class TabulatedProviderTest {

    // Computation date
    private AbsoluteDate date;

    // Reference frame = ITRF
    private Frame itrf;

    // Satellite position
    CircularOrbit circOrbit;

    // Earth shape
    OneAxisEllipsoid earthShape;

    @Test
    public void testWithoutRate() throws OrekitException {
        Assert.assertEquals(0.0, checkError(60.0, 10.0, 8, RRASampleFilter.SAMPLE_R), 1.0e-11);
    }

    @Test
    public void testWithRate() throws OrekitException {
        Assert.assertEquals(0.0, checkError(60.0, 10.0, 8, RRASampleFilter.SAMPLE_RR), 1.0e-12);
    }

    @Test
    public void testWithAcceleration() throws OrekitException {
        Assert.assertEquals(0.0, checkError(60.0, 10.0, 8, RRASampleFilter.SAMPLE_RRA), 1.0e-12);
    }

    private double checkError(double samplingRate, double checkingRate, int n, RRASampleFilter filter)
        throws PropagationException {

        // reference propagator, using a yaw compensation law
        final KeplerianPropagator referencePropagator = new KeplerianPropagator(circOrbit);
        referencePropagator.setAttitudeProvider(new YawCompensation(new NadirPointing(earthShape)));

        // create sample
        final List<Attitude> sample = new ArrayList<Attitude>();
        referencePropagator.setMasterMode(samplingRate, new OrekitFixedStepHandler() {

            public void init(SpacecraftState s0, AbsoluteDate t) {
            }
            
            public void handleStep(SpacecraftState currentState, boolean isLast) {
                sample.add(currentState.getAttitude());
            }

        });
        referencePropagator.propagate(circOrbit.getDate().shiftedBy(2 * circOrbit.getKeplerianPeriod()));

        // prepare an interpolating provider, using only internal steps
        // (i.e. ignoring interpolation near boundaries)
        final double margin      = samplingRate * n / 2;
        final AbsoluteDate start = sample.get(0).getDate().shiftedBy(margin);
        final AbsoluteDate end   = sample.get(sample.size() - 1).getDate().shiftedBy(-margin);
        Propagator interpolatingPropagator = new KeplerianPropagator(circOrbit.shiftedBy(start.durationFrom(circOrbit.getDate())));
        interpolatingPropagator.setAttitudeProvider(new TabulatedProvider(sample, n, filter));
 
        // compute interpolation error on the internal steps .
        final double[] error = new double[1];
        interpolatingPropagator.setMasterMode(checkingRate, new OrekitFixedStepHandler() {
            
            public void init(SpacecraftState s0, AbsoluteDate t) {
                error[0] = 0.0;
            }
            
            public void handleStep(SpacecraftState currentState, boolean isLast) throws PropagationException {
                try {
                    Attitude interpolated = currentState.getAttitude();
                    Attitude reference    = referencePropagator.getAttitudeProvider().getAttitude(currentState.getOrbit(),
                                                                                                  currentState.getDate(),
                                                                                                  currentState.getFrame());
                    double localError = Rotation.distance(interpolated.getRotation(), reference.getRotation());
                    error[0] = FastMath.max(error[0], localError);
                } catch (OrekitException oe) {
                    throw new PropagationException(oe);
                }
            }

        });

        interpolatingPropagator.propagate(end);

        return error[0];

    }

    @Before
    public void setUp() {
        try {
            Utils.setDataRoot("regular-data");

            // Computation date
            date = new AbsoluteDate(new DateComponents(2008, 04, 07),
                                    TimeComponents.H00,
                                    TimeScalesFactory.getUTC());

            // Body mu
            final double mu = 3.9860047e14;

            // Reference frame = ITRF 2005
            itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);

            //  Satellite position
            circOrbit =
                new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, FastMath.toRadians(50.), FastMath.toRadians(270.),
                                       FastMath.toRadians(5.300), PositionAngle.MEAN,
                                       FramesFactory.getEME2000(), date, mu);

            // Elliptic earth shape */
            earthShape =
                new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, itrf);

        } catch (OrekitException oe) {
            Assert.fail(oe.getMessage());
        }

    }

    @After
    public void tearDown() {
        date = null;
        itrf = null;
        circOrbit = null;
        earthShape = null;
    }

}

