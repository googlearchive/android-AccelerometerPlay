package com.example.android.accelerometerplay

/*
 * A particle system is just a collection of particles
 */
class ParticleSystem(private var mBalls: Array<Particle>) {

    private var mLastT: Long = 0

    /*
     * Update the position of each particle in the system using the Verlet integrator.
     */
    private fun updatePositions(sx: Float, sy: Float, timestamp: Long) {
        if (mLastT != 0L) {
            val dT = (timestamp - mLastT).toFloat() / 1000f /* (1.0f / 1000000000.0f)*/
            for (ball in mBalls) {
                ball.computePhysics(sx, sy, dT)
            }
        }
        mLastT = timestamp
    }

    /*
     * Performs one iteration of the simulation. First updating the
     * position of all the particles and resolving the constraints and
     * collisions.
     */
    fun update(sx: Float, sy: Float, now: Long) {
        // update the system's positions
        updatePositions(sx, sy, now)

        // We do no more than a limited number of iterations
        val maxIterations = 10

        /*
         * Resolve collisions, each particle is tested against every
         * other particle for collision. If a collision is detected the
         * particle is moved away using a virtual spring of infinite
         * stiffness.
         */
        var more = true
        val count = mBalls.size
        var k = 0
        while (k < maxIterations && more) {
            more = false
            for (i in 0 until count) {
                val curr = mBalls[i]
                for (j in i + 1 until count) {
                    val ball = mBalls[j]
                    var dx = ball.posX - curr.posX
                    var dy = ball.posY - curr.posY
                    var dd = dx * dx + dy * dy
                    // Check for collisions
                    if (dd <= SimulationView.ballDiameter * SimulationView.ballDiameter) {
                        /*
                         * Add a little bit of entropy, after nothing is perfect in the universe.
                         */
                        dx += (Math.random().toFloat() - 0.5f) * 0.0001f
                        dy += (Math.random().toFloat() - 0.5f) * 0.0001f
                        dd = dx * dx + dy * dy
                        // simulate the spring
                        val d = Math.sqrt(dd.toDouble()).toFloat()
                        val c = 0.5f * (SimulationView.ballDiameter - d) / d
                        val effectX = dx * c
                        val effectY = dy * c
                        curr.posX -= effectX
                        curr.posY -= effectY
                        ball.posX += effectX
                        ball.posY += effectY
                        more = true
                    }
                }
            }
            k++
        }
    }

    fun getPosX(i: Int): Float = mBalls[i].posX

    fun getPosY(i: Int): Float = mBalls[i].posY
}