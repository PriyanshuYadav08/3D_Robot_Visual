package com.example.a3d_robot_visual.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.a3d_robot_visual.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
    }
}