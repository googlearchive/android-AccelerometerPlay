package com.example.android.accelerometerplay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.DisplayMetrics
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout

class SimulationView(context: Context) : FrameLayout(context), SensorEventListener {

    companion object {
        val NUM_PARTICLES = 5
        val ballDiameter = 0.006f
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val mDisplay = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay

    val mDstWidth: Int
    val mDstHeight: Int

    private val mAccelerometer: Sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    var mLastT: Long = 0

    private val mXDpi: Float
    private val mYDpi: Float
    private val mMetersToPixelsX: Float
    private val mMetersToPixelsY: Float
    private var mXOrigin: Float = 0f
    private var mYOrigin: Float = 0f
    private var mSensorX: Float = 0f
    private var mSensorY: Float = 0f

    var mHorizontalBound: Float = 0f
    var mVerticalBound: Float = 0f
    private val mParticleSystem : ParticleSystem

    /*
     * A particle system is just a collection of particles
     */
    internal inner class ParticleSystem {
        internal val mBalls = Array(NUM_PARTICLES, { Particle(context) })

        init {
            /*
             * Initially our particles have no speed or acceleration
             */
            for (i in mBalls.indices) {
                mBalls[i] = Particle(context)
                mBalls[i].setBackgroundResource(R.drawable.ball)
                mBalls[i].setLayerType(View.LAYER_TYPE_HARDWARE, null)
                addView(mBalls[i], ViewGroup.LayoutParams(mDstWidth, mDstHeight))
            }
        }

        /*
         * Update the position of each particle in the system using the
         * Verlet integrator.
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
                        if (dd <= ballDiameter * ballDiameter) {
                            /*
                             * add a little bit of entropy, after nothing is
                             * perfect in the universe.
                             */
                            dx += (Math.random().toFloat() - 0.5f) * 0.0001f
                            dy += (Math.random().toFloat() - 0.5f) * 0.0001f
                            dd = dx * dx + dy * dy
                            // simulate the spring
                            val d = Math.sqrt(dd.toDouble()).toFloat()
                            val c = 0.5f * (ballDiameter - d) / d
                            val effectX = dx * c
                            val effectY = dy * c
                            curr.posX -= effectX
                            curr.posY -= effectY
                            ball.posX += effectX
                            ball.posY += effectY
                            more = true
                        }
                    }
                    curr.resolveCollisionWithBounds(mHorizontalBound, mVerticalBound)
                }
                k++
            }
        }

        fun getPosX(i: Int): Float = mBalls[i].posX

        fun getPosY(i: Int): Float = mBalls[i].posY
    }

    /*
             * It is not necessary to get accelerometer events at a very high
             * rate, by using a slower rate (SENSOR_DELAY_UI), we get an
             * automatic low-pass filter, which "extracts" the gravity component
             * of the acceleration. As an added benefit, we use less power and
             * CPU resources.
             */
    fun startSimulation() =
            sensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME)


    fun stopSimulation() = sensorManager.unregisterListener(this)

    init {

        val metrics = DisplayMetrics()
        mDisplay.getMetrics(metrics)
        mXDpi = metrics.xdpi
        mYDpi = metrics.ydpi
        mMetersToPixelsX = mXDpi / 0.0254f
        mMetersToPixelsY = mYDpi / 0.0254f

        // rescale the mBalls so it's about 0.5 cm on screen
        mDstWidth = (ballDiameter * mMetersToPixelsX + 0.5f).toInt()
        mDstHeight = (ballDiameter * mMetersToPixelsY + 0.5f).toInt()
        mParticleSystem = ParticleSystem()

        val opts = BitmapFactory.Options()
        opts.inPreferredConfig = Bitmap.Config.RGB_565
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        // compute the origin of the screen relative to the origin of the bitmap
        mXOrigin = (w - mDstWidth) * 0.5f
        mYOrigin = (h - mDstHeight) * 0.5f
        mHorizontalBound = (w / mMetersToPixelsX - ballDiameter) * 0.5f
        mVerticalBound = (h / mMetersToPixelsY - ballDiameter) * 0.5f
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        /*
         * record the accelerometer data, the event's timestamp as well as
         * the current time. The latter is needed so we can calculate the
         * "present" time during rendering. In this application, we need to
         * take into account how the screen is rotated with respect to the
         * sensors (which always return data in a coordinate space aligned
         * to with the screen in its native orientation).
         */

        when (mDisplay.rotation) {
            Surface.ROTATION_0 -> {
                mSensorX = event.values[0]
                mSensorY = event.values[1]
            }
            Surface.ROTATION_90 -> {
                mSensorX = -event.values[1]
                mSensorY = event.values[0]
            }
            Surface.ROTATION_180 -> {
                mSensorX = -event.values[0]
                mSensorY = -event.values[1]
            }
            Surface.ROTATION_270 -> {
                mSensorX = event.values[1]
                mSensorY = -event.values[0]
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        /*
         * Compute the new position of our object, based on accelerometer
         * data and present time.
         */
        val particleSystem = mParticleSystem
        val now = System.currentTimeMillis()
        val sx = mSensorX
        val sy = mSensorY

        particleSystem.update(sx, sy, now)

        val xc = mXOrigin
        val yc = mYOrigin
        val xs = mMetersToPixelsX
        val ys = mMetersToPixelsY
        for (i in particleSystem.mBalls.indices) {
            /*
             * We transform the canvas so that the coordinate system matches
             * the sensors coordinate system with the origin in the center
             * of the screen and the unit is the meter.
             */
            val x = xc + particleSystem.getPosX(i) * xs
            val y = yc - particleSystem.getPosY(i) * ys
            particleSystem.mBalls[i].translationX = x
            particleSystem.mBalls[i].translationY = y
        }

        // and make sure to redraw asap
        invalidate()
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
}