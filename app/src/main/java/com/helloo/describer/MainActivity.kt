package com.helloo.describer

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.label.FirebaseVisionCloudImageLabelerOptions
import com.wonderkiln.camerakit.*
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.HashMap


class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts:TextToSpeech? = null
    private var listenToText:String? = ""
    private var textTrue:Boolean? = false
    private var textFromImage:String? = ""

    override fun onResume() {
        super.onResume()
        camera_view.start()
    }

    override fun onPause() {
        super.onPause()
        camera_view.stop()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tts = TextToSpeech(this, this)

        button_detect.setOnClickListener{
            camera_view.start()
//            Toast.makeText(applicationContext,"this is toast message",Toast.LENGTH_SHORT).show()

            camera_view.captureImage()
        }

        camera_view.addCameraKitListener(object:CameraKitEventListener {
            override fun onVideo(p0: CameraKitVideo?) {
                
            }

            override fun onEvent(p0: CameraKitEvent?) {
                
            }

            override fun onImage(p0: CameraKitImage?) {

                var bitmap = p0!!.bitmap

                bitmap = Bitmap.createScaledBitmap(bitmap, camera_view.width, camera_view.height, false)
                camera_view.stop()

                runDetector(bitmap)
                
            }

            override fun onError(p0: CameraKitError?) {

            }

        })
    }

    private fun runDetector(bitmap: Bitmap?) {
        val image = FirebaseVisionImage.fromBitmap(bitmap!!)
        Log.d("random", "runDetectore")

//        InternetCheck(object:InternetCheck.Consumer {
//            override fun accept(isConnected: Boolean?) {
//                if (isConnected!!) {
                    Log.d("random", "isConnected")
                    val options = FirebaseVisionCloudImageLabelerOptions.Builder()
                        .setConfidenceThreshold(0.9f)
                        .build()
                    val detector = FirebaseVision.getInstance().getCloudImageLabeler(options)

                    detector.processImage(image)
                        .addOnSuccessListener { labels ->
                            Log.d("random", "success")
                            Log.d("random", labels.toString())
                            for (label in labels) {
                                val text = label.text
                                val entityId = label.entityId
                                val confidence = label.confidence
                                if (text == "Text") {
                                    textTrue = true
                                    val detector = FirebaseVision.getInstance().cloudTextRecognizer
                                    val result = detector.processImage(image)
                                        .addOnSuccessListener { firebaseVisionText ->
                                            // Task completed successfully
                                            // ...
                                            textFromImage = firebaseVisionText.text
                                        }
                                        .addOnFailureListener { e ->
                                            // Task failed with an exception
                                            // ...
                                            Log.d("random", e.message.toString())
                                        }
                                }

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    tts!!.speak(text, TextToSpeech.QUEUE_ADD, null,null)
                                }
                                else{
                                    val hash = HashMap<String, String>()
                                    hash.put(
                                        TextToSpeech.Engine.KEY_PARAM_STREAM,
                                        AudioManager.STREAM_NOTIFICATION.toString())
                                    tts!!.speak(text, TextToSpeech.QUEUE_ADD,hash)

                                }
                                Toast.makeText(applicationContext, text,Toast.LENGTH_SHORT).show()

                            }
                            if (textTrue!!)
                            {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    tts!!.speak("Text found in image. Would you like to listen to text?", TextToSpeech.QUEUE_ADD, null,null)
                                }
                                else{
                                    val hash = HashMap<String, String>()
                                    hash.put(
                                        TextToSpeech.Engine.KEY_PARAM_STREAM,
                                        AudioManager.STREAM_NOTIFICATION.toString())
                                    tts!!.speak("Text found in image. Would you like to listen to text?", TextToSpeech.QUEUE_ADD,hash)

                                }
                                getSpeechInput()
                            }

                        }
                        .addOnFailureListener { e ->
                            // Task failed with an exception
                            // ...
                            Log.d("random", e.message.toString())
                        }
//                }
//            }
//
//        })

    }
    override fun onInit(status: Int) {
        if(status == TextToSpeech.SUCCESS){
            val result = tts!!.setLanguage(Locale.US)

        }
    }

    public override fun onDestroy() {
        if(tts != null){
            tts!!.stop()
            tts!!.shutdown()
        }

        super.onDestroy()
    }

    fun getSpeechInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, 10)
        } else {
            Toast.makeText(this, "Your Device Don't Support Speech Input", Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            10 -> if (resultCode == Activity.RESULT_OK && data != null) {
                val result =
                    data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                listenToText = result!![0]
                if (listenToText.toString().toLowerCase() == "yes")
                {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        tts!!.speak(textFromImage.toString(), TextToSpeech.QUEUE_ADD, null,null)
                    }
                    else{
                        val hash = HashMap<String, String>()
                        hash.put(
                            TextToSpeech.Engine.KEY_PARAM_STREAM,
                            AudioManager.STREAM_NOTIFICATION.toString())
                        tts!!.speak(textFromImage.toString(), TextToSpeech.QUEUE_ADD,hash)

                    }
                } else
                {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        tts!!.speak("Ok", TextToSpeech.QUEUE_ADD, null,null)
                    }
                    else{
                        val hash = HashMap<String, String>()
                        hash.put(
                            TextToSpeech.Engine.KEY_PARAM_STREAM,
                            AudioManager.STREAM_NOTIFICATION.toString())
                        tts!!.speak("Ok", TextToSpeech.QUEUE_ADD,hash)

                    }
                }
            }
        }
    }
}