/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

package org.orekit.estimation.measurements.gnss;

public class PythonAmbiguityAcceptance implements AmbiguityAcceptance {
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
     * Get the number of candidate solutions to search for.
     *
     * @return number of candidate solutions to search for
     */
    @Override
    public native int numberOfCandidates();

    /**
     * Check if one of the candidate solutions can be accepted.
     *
     * @param candidates candidate solutions of the Integer Least Squares problem,
     *                   in increasing squared distance order (the array contains at least
     *                   {@link #numberOfCandidates()} candidates)
     * @return the candidate solution to accept (normally the one at index 0), or
     * null if we should still use the float solution
     */
    @Override
    public native IntegerLeastSquareSolution accept(IntegerLeastSquareSolution[] candidates);
}