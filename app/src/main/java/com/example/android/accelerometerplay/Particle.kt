/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.accelerometerplay

import android.content.Context
import android.view.View


/*
 * Each of our particle holds its previous and current position, its
 * acceleration. for added realism each particle has its own friction
 * coefficient.
 */
class Particle(context: Context) : View(context) {

    /*
     * Resolving constraints and collisions with the Verlet integrator
     * can be very simple, we simply need to move a colliding or
     * constrained particle in such way that the constraint is
     * satisfied.
     */
    var posX = Math.random().toFloat()
        set(value) {
            val xMax = SimulationView.xBound//0.031000065f
            field = value
            if (value >= xMax) {
                field = xMax
                mVelX = 0f
            } else if (value <= -xMax) {
                field = -xMax
                mVelX = 0f
            }
        }
    var posY = Math.random().toFloat()
        set(value) {
            val yMax = SimulationView.yBound//0.053403694f
            field = value
            if (value >= yMax) {
                field = yMax
                mVelY = 0f
            } else if (posY <= -yMax) {
                field = -yMax
                mVelY = 0f
            }
        }


    private var mVelX: Float = 0f
    private var mVelY: Float = 0f

    fun computePhysics(sx: Float, sy: Float, dT: Float) {

        val ax = -sx / 5
        val ay = -sy / 5

        posX += mVelX * dT + ax * dT * dT / 2
        posY += mVelY * dT + ay * dT * dT / 2

        mVelX += ax * dT
        mVelY += ay * dT
    }

}
