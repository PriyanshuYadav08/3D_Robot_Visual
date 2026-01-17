package com.example.a3d_robot_visual.ui.roomviewer

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.a3d_robot_visual.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RoomViewerFragment : Fragment(R.layout.fragment_room_viewer) {

    private lateinit var glSurfaceView: GLSurfaceView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        glSurfaceView = view.findViewById(R.id.gl_surface_view)
        glSurfaceView.setEGLContextClientVersion(2)

        val renderer = com.example.a3d_robot_visual.renderer.OpenGLRenderer()
        glSurfaceView.setRenderer(renderer)

        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
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