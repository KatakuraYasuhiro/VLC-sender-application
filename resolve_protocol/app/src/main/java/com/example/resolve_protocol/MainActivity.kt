package com.example.resolve_protocol

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    lateinit var editText_send_ms: EditText
    lateinit var textView: TextView
    lateinit var button: Button
    lateinit var manager: CameraManager

    var cameraId: String = ""
    var flashOn = false
    var flashFlag = false
    var flash_freq = 10

    val h = Handler()
    var sig_idx = 0
    var str = ""
    var freq_num = 1

    //テスト用の10bitデータ
    var bin_list = mutableListOf<Int>(1,0,1,1,0,0,1,0,1,1)


    // data2sig : 送信データ data を光信号の点滅パターンに変換
    private fun data2sig(data: List<Int>): MutableList<Boolean>{
        val sig = mutableListOf<Boolean>()

        //start: 5T時間だけ点灯
        sig.add(true)
        sig.add(true)
        sig.add(true)
        sig.add(true)
        sig.add(true)

        //data: 送信データを点滅パターンに変換('0'->T時間消灯，T時間点灯;'1'->T時間消灯，2T時間点灯)
        // data には0と1に変換されたデータが格納されている
        for (n in data) {
            sig.add(false)
            // 0なら1個，1なら2個分点灯データを格納 -> 0は1T，1は2T点灯させる
            for (i in 0..n) {
                sig.add(true)
            }
        }

        //end
        sig.add(false)
        for(i in 1..10) {
            sig.add(false)
        }
        Log.d("finally_data", "data : $sig")
        return sig
    }

    private val run = object : Runnable {
        var freq = flash_freq
        var sig = data2sig(bin_list)
        override fun run() {
            if (sig_idx < sig.size){
                manager.setTorchMode(cameraId, sig[sig_idx])
                if (flashFlag && 0 < freq) {
                    h.postDelayed(this, freq.toLong())
                }
                sig_idx++
            }else {
                manager.setTorchMode(cameraId, false)
                sig_idx = 0
                flashFlag = false
            }
        }
        fun set_signal(bin: MutableList<Int>) {
            sig = data2sig(bin)
        }
        fun set_freq(d: Int) {
            freq = d
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button = findViewById(R.id.button)
        textView = findViewById(R.id.textView)
        editText_send_ms = findViewById(R.id.editTextText_send_ms)

        manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        manager.registerTorchCallback(
            @RequiresApi(Build.VERSION_CODES.M)
            object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(id: String, enabled: Boolean) {
                    super.onTorchModeChanged(id, enabled)

                    id.also { cameraId = it }
                    flashOn = enabled
                }
            }, null
        )

        button.setOnClickListener {

            flashFlag = !flashFlag
            val flashStatus = if (flashFlag) "点灯" else "消灯"

            freq_num = editText_send_ms.text.toString().toInt()  //入力された数値を取得

            run.set_signal(bin_list)  //文字列を点滅パターンに変換
            run.set_freq(freq_num)  //数値を点滅周期に設定
            textView.text = "ライトの状態：$flashStatus"
            if (flashFlag) {
                h.post(run)
            } else {
                h.removeCallbacks(run)
                manager.setTorchMode(cameraId, false)
            }
        }
    }
}
