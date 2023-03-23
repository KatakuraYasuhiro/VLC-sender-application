package com.example.flash_for_and_postdelayed

import android.Manifest.permission.CAMERA
import android.Manifest.permission_group.CAMERA
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    lateinit var textView: TextView
    lateinit var button: Button
    lateinit var button2: Button
    lateinit var button3: Button
    lateinit var manager: CameraManager

    var cameraId: String = ""
    var flashOn = false
    var flashFlag = false
    var a = 0

    val h = Handler()


    private val run = object : Runnable{
        override fun run(){
            if (flashFlag) {
                manager.setTorchMode(cameraId, true)
                flashFlag = !flashFlag
            }
            else{
                manager.setTorchMode(cameraId, false)
                flashFlag = !flashFlag
            }
            h.postDelayed(this, 1)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button = findViewById(R.id.button)
        button2 = findViewById(R.id.button2)
        button3 = findViewById(R.id.button3)
        textView = findViewById(R.id.textView)

        manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        manager.registerTorchCallback(@RequiresApi(Build.VERSION_CODES.M)
        object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(id: String, enabled: Boolean) {
                super.onTorchModeChanged(id, enabled)

                id.also { cameraId = it }
                flashOn = enabled
            }
        }, null
        )

        button.setOnClickListener {
            //Handler version
            flashFlag = !flashFlag
            val flashStatus = if (flashFlag) "点灯" else "消灯"
            textView.text = "ライトの状態：$flashStatus"
            if (flashFlag) {
                h.post(run)
            } else {
                h.removeCallbacks(run)
                manager.setTorchMode(cameraId, false)
            }
        }
        button2.setOnClickListener {
            //For Version 500times flash
            for (i in 1..500) {


                manager.setTorchMode(cameraId, true)   //flashlight ON
                manager.setTorchMode(cameraId, false) //flashlight OFF


            }
        }
        button3.setOnClickListener {
            if(a==0) {
                manager.setTorchMode(cameraId,true)
                a = 1
            }
            else if(a == 1){
                manager.setTorchMode(cameraId,false)
                a = 0
            }
        }
    }
}