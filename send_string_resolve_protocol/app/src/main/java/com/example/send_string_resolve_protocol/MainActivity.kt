package com.example.send_string_resolve_protocol


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
import java.sql.Types.NULL

class MainActivity : AppCompatActivity() {
    lateinit var editText_send_ms: EditText
    lateinit var editText_send_string: EditText
    lateinit var textView: TextView
    lateinit var send_button: Button
    lateinit var binary_button: Button
    lateinit var manager: CameraManager

    var cameraId: String = ""
    var flashOn = false
    var flashFlag = false
    var flash_freq = 10

    val h = Handler()
    var sig_idx = 0
    var str = ""
    var freq_num = 1
    var start = System.nanoTime()
    var end = System.nanoTime()


    // dec2bin ： 10進数の数値をバイナリ形式のデータに変換
    private fun dec2bin(dec: Int): List<Int> {
        var n: Int = dec
        val bin = mutableListOf<Int>()
        while (n > 0) {
            bin.add(0, n % 2)
            n /= 2
        }
        Log.d("dec2bin", "bin size : ${bin.size}")
        Log.d("dec2bin", "bin : $bin")

        while (bin.size < 8) {
            bin.add(0, 0)
        }

        return bin
    }

    //str2data : 文字列データをバイナリデータに変換
    private fun str2data(str: String): MutableList<Int> {
        val data = mutableListOf<Int>()
        for (char in str) {
            data.addAll(dec2bin(char.code))
        }

        Log.d("str2data", "data : $data")

        return data
    }


    // data2sig : 送信データ data を光信号の点滅パターンに変換
    private fun data2sig(data: List<Int>): MutableList<Boolean>{
        val sig = mutableListOf<Boolean>()

        //start: 5T時間だけ点灯
        sig.add(true)
        sig.add(true)
        sig.add(true)
        sig.add(true)
        sig.add(true)

        //data: 送信データを点滅パターンに変換('0'->T時間点灯;'1'->2T時間点灯)
        // data には0と1に変換されたデータが格納されている
        for (n in data) {
            sig.add(false)
            // 0なら1個，1なら2個分点灯データを格納 -> 0は1T，1は2T点灯させる
            for (i in 0..n) {
                sig.add(true)
            }
        }

        //end
        for(i in 1..10) {
            sig.add(false)
        }
        Log.i("finally_data", "data : $sig")
        return sig
    }

    private val run = object : Runnable {
        var freq = flash_freq
        var sig = data2sig(str2data(str))
        override fun run() {
            if (sig_idx < sig.size){
                manager.setTorchMode(cameraId, sig[sig_idx])
                h.postDelayed(this, freq.toLong())
                sig_idx++
            }else {
                manager.setTorchMode(cameraId, false)
                end = System.nanoTime()
                sig_idx = 0
                flashFlag = false
                Log.i("MyApplication", "performance " + (end - start) + " nsec")
            }
        }
        fun set_signal(s:String) {
            sig = data2sig(str2data(s))
        }
        fun set_freq(d: Int) {
            freq = d
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        send_button = findViewById(R.id.send_button)
        binary_button = findViewById(R.id.binary_button)
        textView = findViewById(R.id.textView)
        editText_send_string = findViewById(R.id.editTextText_send_string)
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

        send_button.setOnClickListener {
            if(editText_send_string.text.isNotEmpty() && editText_send_ms.text.isNotEmpty()){
                str = editText_send_string.getText().toString() //入力された文字列を取得
                freq_num = editText_send_ms.text.toString().toInt()  //入力された数値を取得

                flashFlag = !flashFlag
                val flashStatus = if (flashFlag) "点灯" else "消灯"
                run.set_signal(str)  //文字列を点滅パターンに変換
                run.set_freq(freq_num)  //数値を点滅周期に設定
                textView.text = "ライトの状態：$flashStatus"
                if (flashFlag) {
                    start = System.nanoTime()
                    h.post(run)
                } else {
                    h.removeCallbacks(run)
                    manager.setTorchMode(cameraId, false)
                }
            }
        }

        binary_button.setOnClickListener {
            str = editText_send_string.getText().toString() //入力された文字列を取得
            var data_time = 0
            var zero_data_time = 0
            var one_data_time = 0
            val data = mutableListOf<Int>()
            var binary_data = mutableListOf<Int>()
            for(char in str){
                data.add(char.code)
            }
            for(i in data){
                val a = dec2bin(i)
                binary_data.addAll(a)
            }
            for (j in binary_data){
                if (j == 0) {
                    zero_data_time += 1
                    data_time += 2
                }
                if (j == 1) {
                    one_data_time += 1
                    data_time += 3
                }
            }
            Log.i("Binary_Data","data: $binary_data")
            Log.i("zero_data_time","Zero_count: $zero_data_time")
            Log.i("one_data_time","One_count: $one_data_time")
            Log.i("Data_time","data_time: $data_time T")
        }
    }
}
