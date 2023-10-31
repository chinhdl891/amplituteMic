package com.example.audioamplie

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.crazylegend.audiopicker.pickers.SingleAudioPicker


private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    val requestCode = 6688

    val permissionsUnder33 = arrayOf(
        Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    val permissionsUpper33 = arrayOf(
        Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    var permissions = arrayOf<String>()
    val SAMPLE_RATE = 44100
    val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    lateinit var audioRecord: AudioRecord


    private lateinit var button: Button
    private lateinit var buttonClickMuic: Button
    private lateinit var textAmplitude: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        permissions =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permissionsUpper33 else permissionsUnder33

        // Kiểm tra xem quyền đã được cấp chưa
        val granted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        button = findViewById(R.id.btn_main)
        buttonClickMuic = findViewById(R.id.btn_main_click_music)

        buttonClickMuic.setOnClickListener {
            //    //simple usage without customization
            SingleAudioPicker.showPicker(this) {
             playAudioFromUri(it.contentUri)
            }
            //
            //
            //    //customized
            //    SingleAudioPicker.showPicker(this, {
            //            setupViewHolderTitleText {
            //                textColor = Color.BLACK
            //                textPadding = 10 // use dp or sp this is only for demonstration purposes
            //            }
            //            setupBaseModifier(
            //                    loadingIndicatorColor = R.color.minusColor,
            //                    titleTextModifications = {
            //                        textAlignment = TextView.TEXT_ALIGNMENT_VIEW_START
            //                        textStyle = TitleTextModifier.TextStyle.ITALIC
            //                        textColor = Color.BLACK
            //                        marginBottom = 30 // use dp or sp this is only for demonstration purposes
            //                        textPadding = 5 // use dp or sp this is only for demonstration purposes
            //                        textSize = 30f  // use sp this is only for demonstration purposes
            //                        textString = "Pick an audio"
            //                    },
            //                    placeHolderModifications = {
            //                        resID = R.drawable.ic_image
            //                    }
            //            )
            //        }, ::loadAudio)
        }
        textAmplitude = findViewById(R.id.tv_main_amplitude)

        button.setOnClickListener {
            if (!granted) {
                ActivityCompat.requestPermissions(this, permissions, requestCode)
            } else {
                recordAndAnalyzeAudio()
            }
        }


    }
    val mediaPlayer = MediaPlayer()

    fun playAudioFromUri(uri: Uri) {
        mediaPlayer.apply {
            reset()
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setDataSource(this@MainActivity, uri)
            prepare()
            start()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            requestCode -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    recordAndAnalyzeAudio()
                } else {
                    Toast.makeText(this, "Từ Chối", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    val handler = Handler()
    val delay: Long = 100 // milliseconds
    lateinit var runnable: Runnable

    private fun recordAndAnalyzeAudio() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT
            )

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                minBufferSize
            )

            val buffer = ShortArray(minBufferSize)

            audioRecord.startRecording()

            runnable = object : Runnable {
                override fun run() {
                    audioRecord.read(buffer, 0, minBufferSize)
                    // Xử lý dữ liệu âm thanh tại đây, ví dụ:
                    val amplitude = buffer.maxOrNull()
                    textAmplitude.setText(amplitude.toString())

                    handler.postDelayed(this, delay)
                }
            }
            handler.postDelayed(runnable, delay)

            // Bắt đầu ghi âm

//
//            while (true) {
//                audioRecord.read(buffer, 0, minBufferSize)
//                // Xử lý dữ liệu âm thanh tại đây, ví dụ:
//                val amplitude = buffer.maxOrNull()
//                Log.d(TAG, "Amplitude: $amplitude")
//            }

        }

    }


    fun stopRecording() {
        if (::audioRecord.isInitialized) {
            audioRecord.stop()
            audioRecord.release()
            handler.removeCallbacks(runnable)
        }
    }
}