package com.example.android.accelerometerplay

import android.content.Context
import android.view.View


/*
 * Each of our particle holds its previous and current position, its
 * acceleration. for added realism each particle has its own friction
 * coefficient.
 */
class Particle(context: Context) : View(context) {

    var posX = Math.random().toFloat()
    var posY = Math.random().toFloat()
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

    /*
     * Resolving constraints and collisions with the Verlet integrator
     * can be very simple, we simply need to move a colliding or
     * constrained particle in such way that the constraint is
     * satisfied.
     */
    fun resolveCollisionWithBounds() {

        val xMax = SimulationView.mHorizontalBound//0.031000065f
        val yMax = SimulationView.mVerticalBound//0.053403694f

        val xx = posX
        val yy = posY

        if (xx >= xMax) {
            posX = xMax
            mVelX = 0f
        }
        else if (xx <= -xMax) {
            posX = -xMax
            mVelX = 0f
        }
        if (yy >= yMax) {
            posY = yMax
            mVelY = 0f
        } else if (yy <= -yMax) {
            posY = -yMax
            mVelY = 0f
        }
    }
}