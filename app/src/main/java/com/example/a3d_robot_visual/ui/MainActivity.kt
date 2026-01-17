package com.example.a3d_robot_visual.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.a3d_robot_visual.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(
                    R.id.fragment_container,
                    com.example.a3d_robot_visual.ui.roomviewer.RoomViewerFragment()
                ).commit()
        }
    }
}