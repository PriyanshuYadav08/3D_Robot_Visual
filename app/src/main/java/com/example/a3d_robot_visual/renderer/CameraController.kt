package com.example.a3d_robot_visual.renderer

import android.opengl.Matrix
import kotlin.math.cos
import kotlin.math.sin

class CameraController {

    private var distance = 2.5f
    private var yaw = 0f
    private var pitch = 0f

    private val viewMatrix = FloatArray(16)

    fun getViewMatrix(): FloatArray {
        val eyeX = (distance * cos(pitch) * sin(yaw)).toFloat()
        val eyeY = (distance * sin(pitch)).toFloat()
        val eyeZ = (distance * cos(pitch) * cos(yaw)).toFloat()

        Matrix.setLookAtM(
            viewMatrix, 0,
            eyeX, eyeY, eyeZ,
            0f, 0f, 0f,
            0f, 1f, 0f
        )

        return viewMatrix
    }

    fun rotate(deltaX: Float, deltaY: Float) {
        yaw += deltaX * 0.01f
        pitch += deltaY * 0.01f
        pitch = pitch.coerceIn(-1.2f, 1.2f)
    }

    fun zoom(amount: Float) {
        distance += amount * 0.05f
        distance = distance.coerceIn(1.5f, 8f)
    }
}