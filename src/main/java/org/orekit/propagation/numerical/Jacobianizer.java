/* Copyright 2010-2011 Centre National d'Études Spatiales
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
package org.orekit.propagation.numerical;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math.geometry.Vector3D;
import org.apache.commons.math.util.FastMath;
import org.apache.commons.math.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.PVCoordinates;

/** Class enabling basic {@link ForceModel} instances
 *  to be used when processing spacecraft state partial derivatives.
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision$ $Date$
 */
class Jacobianizer implements AccelerationJacobiansProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = -352915943197943037L;

    /** Wrapped force model instance. */
    private final ForceModel forceModel;

    /** Step used for finite difference computation with respect to spacecraft position. */
    private double hPos;

    /** Step used for finite difference computation with respect to spacecraft velocity. */
    private double hVel;

    /** Step used for finite difference computation with respect to spacecraft mass. */
    private double hMass;

    /** Step used for finite difference computation with respect to parameters value. */
    private final Map<String, Double> hParam;

    /** Dedicated adder used to retrieve nominal acceleration. */
    private final AccelerationRetriever nominal;

    /** Dedicated adder used to retrieve shifted acceleration. */
    private final AccelerationRetriever shifted;

    /** Simple constructor.
     * @param forceModel force model instance to wrap
     * @param paramsAndSteps collection of parameters and their associated steps
     * @param hPos step used for finite difference computation with respect to spacecraft position (m)
     * @param hVel step used for finite difference computation with respect to spacecraft velocity (m/s)
     * @param hMass step used for finite difference computation with respect to spacecraft mass (kg)
     */
    public Jacobianizer(final ForceModel forceModel, final Collection<ParameterConfiguration> paramsAndSteps,
                        final double hPos, final double hVel, final double hMass) {

        this.forceModel = forceModel;
        this.hParam     = new HashMap<String, Double>();
        this.hPos       = hPos;
        this.hVel       = hVel;
        this.hMass      = hMass;
        this.nominal    = new AccelerationRetriever();
        this.shifted    = new AccelerationRetriever();

        // set up parameters for jacobian computation
        for (final ParameterConfiguration param : paramsAndSteps) {
            final String name = param.getParameterName();
            if (forceModel.isSupported(name)) {
                double step = param.getHP();
                if (Double.isNaN(step)) {
                    step = forceModel.getParameter(name) * FastMath.sqrt(MathUtils.EPSILON);
                }
                hParam.put(name, step);
            }
        }

    }

    /** Compute acceleration.
     * @param retriever acceleration retriever to use for storing acceleration
     * @param s original state
     * @param p shifted position
     * @param v shifted velocity
     * @param m shifted mass
     * @exception OrekitException if the underlying force models cannot compute the acceleration
     */
    private void computeShiftedAcceleration(final AccelerationRetriever retriever, final SpacecraftState s,
                                            final Vector3D p, final Vector3D v, final double m)
        throws OrekitException {
        final Orbit shiftedORbit = new CartesianOrbit(new PVCoordinates(p, v), s.getFrame(), s.getDate(), s.getMu());
        retriever.initDerivatives(null, shiftedORbit);
        forceModel.addContribution(new SpacecraftState(shiftedORbit, s.getAttitude(), m), retriever);
    }

    /** {@inheritDoc} */
    public void addDAccDState(final SpacecraftState s,
                              final double[][] dAccdPos, final double[][] dAccdVel, final double[] dAccdM)
        throws OrekitException {

        // compute df/dy where f is the ODE and y is the state array

        final Vector3D p0 = s.getPVCoordinates().getPosition();
        final Vector3D v0 = s.getPVCoordinates().getVelocity();
        final double m0 = s.getMass();
        computeShiftedAcceleration(nominal, s, p0, v0, m0);

        // jacobian with respect to position
        computeShiftedAcceleration(shifted, s, new Vector3D(p0.getX() + hPos, p0.getY(), p0.getZ()), v0, m0);
        dAccdPos[0][0] += (shifted.getX() - nominal.getX()) / hPos;
        dAccdPos[1][0] += (shifted.getY() - nominal.getY()) / hPos;
        dAccdPos[2][0] += (shifted.getZ() - nominal.getZ()) / hPos;

        computeShiftedAcceleration(shifted, s, new Vector3D(p0.getX(), p0.getY() + hPos, p0.getZ()), v0, m0);
        dAccdPos[0][1] += (shifted.getX() - nominal.getX()) / hPos;
        dAccdPos[1][1] += (shifted.getY() - nominal.getY()) / hPos;
        dAccdPos[2][1] += (shifted.getZ() - nominal.getZ()) / hPos;

        computeShiftedAcceleration(shifted, s, new Vector3D(p0.getX(), p0.getY(), p0.getZ() + hPos), v0, m0);
        dAccdPos[0][2] += (shifted.getX() - nominal.getX()) / hPos;
        dAccdPos[1][2] += (shifted.getY() - nominal.getY()) / hPos;
        dAccdPos[2][2] += (shifted.getZ() - nominal.getZ()) / hPos;

        // jacobian with respect to velocity
        computeShiftedAcceleration(shifted, s, p0, new Vector3D(v0.getX() + hVel, v0.getY(), v0.getZ()), m0);
        dAccdVel[0][0] += (shifted.getX() - nominal.getX()) / hPos;
        dAccdVel[1][0] += (shifted.getY() - nominal.getY()) / hPos;
        dAccdVel[2][0] += (shifted.getZ() - nominal.getZ()) / hPos;

        computeShiftedAcceleration(shifted, s, p0, new Vector3D(v0.getX(), v0.getY() + hVel, v0.getZ()), m0);
        dAccdVel[0][1] += (shifted.getX() - nominal.getX()) / hPos;
        dAccdVel[1][1] += (shifted.getY() - nominal.getY()) / hPos;
        dAccdVel[2][1] += (shifted.getZ() - nominal.getZ()) / hPos;

        computeShiftedAcceleration(shifted, s, p0, new Vector3D(v0.getX(), v0.getY(), v0.getZ() + hVel), m0);
        dAccdVel[0][2] += (shifted.getX() - nominal.getX()) / hPos;
        dAccdVel[1][2] += (shifted.getY() - nominal.getY()) / hPos;
        dAccdVel[2][2] += (shifted.getZ() - nominal.getZ()) / hPos;

        if (dAccdM != null) {
            // jacobian with respect to mass
            computeShiftedAcceleration(shifted, s, p0, v0, m0 + hMass);
            dAccdM[0] += (shifted.getX() - nominal.getX()) / hMass;
            dAccdM[1] += (shifted.getY() - nominal.getY()) / hMass;
            dAccdM[2] += (shifted.getZ() - nominal.getZ()) / hMass;
        }


    }

    /** {@inheritDoc} */
    public void addDAccDParam(final SpacecraftState s, final String paramName,
                              final double[] dAccdParam) throws OrekitException {
        final double hP = hParam.get(paramName);
        nominal.initDerivatives(null, s.getOrbit());
        forceModel.addContribution(s, nominal);

        final double paramValue = forceModel.getParameter(paramName);
        forceModel.setParameter(paramName,  paramValue + hP);
        shifted.initDerivatives(null, s.getOrbit());
        forceModel.addContribution(s, shifted);
        forceModel.setParameter(paramName,  paramValue);

        dAccdParam[0] += (shifted.getX() - nominal.getX()) / hP;
        dAccdParam[1] += (shifted.getY() - nominal.getY()) / hP;
        dAccdParam[2] += (shifted.getZ() - nominal.getZ()) / hP;
    }

    /** {@inheritDoc} */
    public double getParameter(final String name) throws IllegalArgumentException {
        return forceModel.getParameter(name);
    }

    /** {@inheritDoc} */
    public Collection<String> getParametersNames() {
        return forceModel.getParametersNames();
    }

    /** {@inheritDoc} */
    public boolean isSupported(final String name) {
        return forceModel.isSupported(name);
    }

    /** {@inheritDoc} */
    public void setParameter(final String name, final double value) throws IllegalArgumentException {
        forceModel.setParameter(name, value);
    }

    /** Internal class for retrieving accelerations. */
    private static class AccelerationRetriever extends TimeDerivativesEquations {

        /** Serializable UID. */
        private static final long serialVersionUID = 6410400549499020323L;

        /** Stored acceleration. */
        private final double[] acceleration;

        /** Current orbit. */
        private Orbit orbit;

        /** Simple constructor.
         */
        protected AccelerationRetriever() {
            acceleration = new double[3];
            this.orbit   = null;
        }

        /** Get X component of acceleration.
         * @return X component of acceleration
         */
        public double getX() {
            return acceleration[0];
        }

        /** Get Y component of acceleration.
         * @return Y component of acceleration
         */
        public double getY() {
            return acceleration[1];
        }

        /** Get Z component of acceleration.
         * @return Z component of acceleration
         */
        public double getZ() {
            return acceleration[2];
        }

        /** {@inheritDoc} */
        void initDerivatives(final double[] yDot, final Orbit currentOrbit) {

            // in fact, we won't really use this field from the base class
            storedYDot      = yDot;

            acceleration[0] = 0;
            acceleration[1] = 0;
            acceleration[2] = 0;
            this.orbit      = currentOrbit;

        }

        /** {@inheritDoc} */
        public void addKeplerContribution(final double mu) {
            final Vector3D position = orbit.getPVCoordinates().getPosition();
            final double r2         = position.getNormSq();
            final double coeff      = -mu / (r2 * FastMath.sqrt(r2));
            acceleration[0] += coeff * position.getX();
            acceleration[1] += coeff * position.getY();
            acceleration[2] += coeff * position.getZ();
        }

        /** {@inheritDoc} */
        public void addXYZAcceleration(final double x, final double y, final double z) {
            acceleration[0] += x;
            acceleration[1] += y;
            acceleration[2] += z;
        }

        /** {@inheritDoc} */
        public void addAcceleration(final Vector3D gamma, final Frame frame)
            throws OrekitException {
            final Transform t = frame.getTransformTo(orbit.getFrame(), orbit.getDate());
            final Vector3D gammInRefFrame = t.transformVector(gamma);
            addXYZAcceleration(gammInRefFrame.getX(), gammInRefFrame.getY(), gammInRefFrame.getZ());
        }

        /** {@inheritDoc} */
        @Override
        public void addMassDerivative(final double q) {
            // we don't compute (yet) the mass part of the Jacobian, we just ignore this
        }

    }

}
