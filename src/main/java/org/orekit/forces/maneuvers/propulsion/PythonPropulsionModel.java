/* Copyright 2002-2020 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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

// this file was created by SCC 2020 and is largely a derived work from the
// original java class/interface

package org.orekit.forces.maneuvers.propulsion;

import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;

public class PythonPropulsionModel implements PropulsionModel {

    /** Part of JCC Python interface to object */
    private long pythonObject;

    /** Part of JCC Python interface to object */
    public void pythonExtension(long pythonObject)
    {
        this.pythonObject = pythonObject;
    }

    /** Part of JCC Python interface to object */
    public long pythonExtension()
    {
        return this.pythonObject;
    }

    /** Part of JCC Python interface to object */
    public void finalize()
            throws Throwable
    {
        pythonDecRef();
    }

    /** Part of JCC Python interface to object */
    public native void pythonDecRef();


    /**
     * Initialization method.
     * Called in when Maneuver.init(...) is called (from ForceModel.init(...))
     *
     * @param initialState initial spacecraft state (at the start of propagation).
     * @param target       date of propagation. Not equal to {@code initialState.getDate()}.
     */
    @Override
    public native void init(SpacecraftState initialState, AbsoluteDate target);

    /**
     * Get the acceleration of the spacecraft during maneuver and in maneuver frame.
     *
     * @param s                current spacecraft state
     * @param maneuverAttitude current attitude in maneuver
     * @param parameters       propulsion model parameters
     * @return acceleration
     */
    @Override
    public native Vector3D getAcceleration(SpacecraftState s, Attitude maneuverAttitude, double[] parameters);

    /**
     * Get the acceleration of the spacecraft during maneuver and in maneuver frame.
     *
     * @param s                current spacecraft state
     * @param maneuverAttitude current attitude in maneuver
     * @param parameters       propulsion model parameters
     * @return acceleration
     */
    @Override
    public <T extends RealFieldElement<T>> FieldVector3D<T> getAcceleration(FieldSpacecraftState<T> s, FieldAttitude<T> maneuverAttitude, T[] parameters) {
        return this.getAcceleration_FFT(s, maneuverAttitude, parameters);
    }

    public native <T extends RealFieldElement<T>> FieldVector3D<T> getAcceleration_FFT(FieldSpacecraftState<T> s, FieldAttitude<T> maneuverAttitude, T[] parameters);


        /**
         * Get the mass derivative (i.e. flow rate in kg/s) during maneuver.
         *
         * @param s          current spacecraft state
         * @param parameters propulsion model parameters  @return mass derivative in kg/s
         */
    @Override
    public native double getMassDerivatives(SpacecraftState s, double[] parameters);

    /**
     * Get the mass derivative (i.e. flow rate in kg/s) during maneuver.
     *
     * @param s          current spacecraft state
     * @param parameters propulsion model parameters  @return mass derivative in kg/s
     */
    @Override
    public <T extends RealFieldElement<T>> T getMassDerivatives(FieldSpacecraftState<T> s, T[] parameters) {
        return this.getMassDerivatives_FT(s, parameters);
    }

    public native <T extends RealFieldElement<T>> T getMassDerivatives_FT(FieldSpacecraftState<T> s, T[] parameters);


    /**
     * Get the propulsion model parameter drivers.
     *
     * @return propulsion model parameter drivers
     */
    @Override
    public native ParameterDriver[] getParametersDrivers();

    /**
     * Get the maneuver name.
     *
     * @return the maneuver name
     */
    @Override
    public native String getName();
}
