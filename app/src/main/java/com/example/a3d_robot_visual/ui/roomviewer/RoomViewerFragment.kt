package com.example.a3d_robot_visual.ui.roomviewer

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.fragment.app.Fragment
import com.example.a3d_robot_visual.R
import com.example.a3d_robot_visual.renderer.OpenGLRenderer
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs

@AndroidEntryPoint
class RoomViewerFragment : Fragment(R.layout.fragment_room_viewer) {

    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var renderer: OpenGLRenderer

    private var previousX = 0f
    private var previousY = 0f
    private var previousDistance = 0f

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        glSurfaceView = view.findViewById(R.id.gl_surface_view)

        // Request OpenGL ES 2.0
        glSurfaceView.setEGLContextClientVersion(2)

        // Request depth buffer (important for 3D)
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0)

        renderer = OpenGLRenderer()
        glSurfaceView.setRenderer(renderer)

        // Continuous rendering for smooth interaction
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        // Touch handling
        glSurfaceView.setOnTouchListener { _, event ->
            when (event.pointerCount) {
                1 -> handleRotation(event)
                2 -> handleZoom(event)
            }
            true
        }
    }

    private fun handleRotation(event: MotionEvent) {
        val x = event.x
        val y = event.y

        if (event.action == MotionEvent.ACTION_MOVE) {
            val dx = x - previousX
            val dy = y - previousY
            renderer.rotateCamera(dx, dy)
        }

        previousX = x
        previousY = y
    }

    private fun handleZoom(event: MotionEvent) {
        val x1 = event.getX(0)
        val y1 = event.getY(0)
        val x2 = event.getX(1)
        val y2 = event.getY(1)

        val distance = distanceBetween(x1, y1, x2, y2)

        if (previousDistance != 0f) {
            val delta = distance - previousDistance
            if (abs(delta) > 5f) {
                renderer.zoomCamera(-delta)
            }
        }

        previousDistance = distance
    }

    private fun distanceBetween(
        x1: Float, y1: Float,
        x2: Float, y2: Float
    ): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
    }
}