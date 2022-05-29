
package com.example.engage2022_face_recog

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.View
import android.view.View.VISIBLE
import android.view.WindowInsets
import android.view.animation.TranslateAnimation
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.engage2022_face_recog.model.FaceNetModel
import com.example.engage2022_face_recog.model.Models
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import java.io.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    private var isSerializedDataStored = false

    // Serialized data will be stored ( in app's private storage ) with this filename.
    private val SERIALIZED_DATA_FILENAME = "image_data"

    // Shared Pref key to check if the data was stored.
    private val SHARED_PREF_IS_DATA_STORED_KEY = "is_data_stored"

    private lateinit var previewView : PreviewView
    private lateinit var frameAnalyser  : FrameAnalyser
    private lateinit var faceNetModel: FaceNetModel
    private lateinit var fileReader : FileReader
    private lateinit var cameraProviderFuture : ListenableFuture<ProcessCameraProvider>
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var splashscreen: CardView

    // <----------------------- User controls --------------------------->

    // Use the device's GPU to perform faster computations.
    // Refer https://www.tensorflow.org/lite/performance/gpu
    private val useGpu = false

    // Use XNNPack to accelerate inference.
    // Refer https://blog.tensorflow.org/2020/07/accelerating-tensorflow-lite-xnnpack-integration.html
    private val useXNNPack = true

    // Use the model configs in Models.kt
    private val modelInfo = Models.FACENET

    // <---------------------------------------------------------------->


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var k = 0
        // Remove the status bar to have a full screen experience
        // See this answer on SO -> https://stackoverflow.com/a/68152688/10878733
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.decorView.windowInsetsController!!
                .hide( WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        }
        else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        }
        setContentView(R.layout.activity_main)

        splashscreen = findViewById(R.id.splashscreen)

        // Implementation of CameraX preview
        previewView = findViewById( R.id.preview_view )
        // Necessary to keep the Overlay above the PreviewView so that the boxes are visible.
        val boundingBoxOverlay = findViewById<BoundingBoxOverlay>( R.id.bbox_overlay )
        boundingBoxOverlay.setWillNotDraw( false )
        boundingBoxOverlay.setZOrderOnTop( true )

        faceNetModel = FaceNetModel( this , modelInfo , useGpu , useXNNPack )
        frameAnalyser = FrameAnalyser( this , boundingBoxOverlay , faceNetModel )
        fileReader = FileReader( faceNetModel )


        // We'll only require the CAMERA permission from the user.
        // For scoped storage, particularly for accessing documents, we won't require WRITE_EXTERNAL_STORAGE or
        // READ_EXTERNAL_STORAGE permissions. See https://developer.android.com/training/data-storage
        if ( ActivityCompat.checkSelfPermission( this , Manifest.permission.CAMERA ) != PackageManager.PERMISSION_GRANTED ) {
            requestCameraPermission()
        }
        else {
            startCameraPreview()
        }

        sharedPreferences = getSharedPreferences( getString( R.string.app_name ) , Context.MODE_PRIVATE )
        isSerializedDataStored = sharedPreferences.getBoolean( SHARED_PREF_IS_DATA_STORED_KEY , false )
        if ( !isSerializedDataStored ) {
            showSelectDirectoryDialog()
        }
        else {
            val alertDialog = AlertDialog.Builder( this ).apply {
                setTitle( "Serialized Data")
                setMessage( "Existing trained data was found on the device. Would you like to load it or would you like to reload it from the server (data charges may apply)?" )
                setCancelable( false )
                setNegativeButton( "LOAD") { dialog, which ->
                    dialog.dismiss()
                    Executors.newSingleThreadScheduledExecutor().schedule({
                        val animate = TranslateAnimation(0F,
                            -2000.0f, 0F, 0F)
                        animate.duration = 500
                        animate.fillAfter = true
                        splashscreen.startAnimation(animate) }, 2, TimeUnit.SECONDS)
                    frameAnalyser.faceList = loadSerializedImageData()
                }
                setPositiveButton( "RELOAD") { dialog, which ->
                    dialog.dismiss()
                    Executors.newSingleThreadScheduledExecutor().schedule({
                        val animate = TranslateAnimation(0F,
                            -2000.0f, 0F, 0F)
                        animate.duration = 500
                        animate.fillAfter = true
                        splashscreen.startAnimation(animate) }, 2, TimeUnit.SECONDS)
                    showSelectDirectoryDialog()
                }
                create()
            }
            alertDialog.show()
        }

    }

    // ---------------------------------------------- //

    // Attach the camera stream to the PreviewView.
    private fun startCameraPreview() {
        cameraProviderFuture = ProcessCameraProvider.getInstance( this )
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider) },
            ContextCompat.getMainExecutor(this) )
    }

    private fun bindPreview(cameraProvider : ProcessCameraProvider) {
        val preview : Preview = Preview.Builder().build()
        val cameraSelector : CameraSelector = CameraSelector.Builder()
            .requireLensFacing( CameraSelector.LENS_FACING_BACK )
            .build()
        preview.setSurfaceProvider( previewView.surfaceProvider )
        val imageFrameAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size( 480, 640 ) )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        imageFrameAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), frameAnalyser )
        cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview , imageFrameAnalysis  )
    }

    // We let the system handle the requestCode. This doesn't require onRequestPermissionsResult and
    // hence makes the code cleaner.
    // See the official docs -> https://developer.android.com/training/permissions/requesting#request-permission
    private fun requestCameraPermission() {
        cameraPermissionLauncher.launch( Manifest.permission.CAMERA )
    }

    private val cameraPermissionLauncher = registerForActivityResult( ActivityResultContracts.RequestPermission() ) {
        isGranted ->
        if ( isGranted ) {
            startCameraPreview()
        }
        else {
            val alertDialog = AlertDialog.Builder( this ).apply {
                setTitle( "Camera Permission")
                setMessage( "The app couldn't function without the camera permission." )
                setCancelable( false )
                setPositiveButton( "ALLOW" ) { dialog, which ->
                    dialog.dismiss()
                    requestCameraPermission()
                }
                setNegativeButton( "CLOSE" ) { dialog, which ->
                    dialog.dismiss()
                    finish()
                }
                create()
            }
            alertDialog.show()
        }

    }


    // ---------------------------------------------- //

    private lateinit var dbReference: DatabaseReference
    // Open File chooser to choose the images directory.
    private fun showSelectDirectoryDialog() {
        val images: MutableList<Pair<String,Bitmap>> = ArrayList()
        dbReference = FirebaseDatabase.getInstance().getReference("1MPU39ZefbUAoYX-k_ethP-0CKpqaFRETiOHwdooFo_0/missing")
        val missingPersonList: MutableList<MissingInfo?> = ArrayList()
        dbReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (noteSnapshot in dataSnapshot.children) {
                    val note: MissingInfo? = noteSnapshot.getValue(MissingInfo::class.java)
                    missingPersonList.add(note)
                    if (note != null) {
                        val urls = note.images!!.split(",")
                        for(url in urls) {
                            images.add(Pair(note.name.toString(), getBitmap(url)))
                        }
                    }
                }
                fileReader.run(images as ArrayList<Pair<String, Bitmap>>, fileReaderCallback)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.d("LOG", databaseError.message)
            }
        })
    }

    private fun getBitmap(imageFileUri: String): Bitmap {
        val latch = CountDownLatch(1)

        var value : Bitmap? = null
            val uiThread: Thread = object : HandlerThread("UIHandler") {
                override fun run() {
                    try {
                        value = Picasso.get().load(imageFileUri).get()
                    } catch (e: Exception) {
                        Log.d("Tag", imageFileUri + "")
                        return
                    }
                    latch.countDown() // Release await() in the test thread.
                }
            }
            uiThread.start()
            latch.await()
        return value!!
    }

    // ---------------------------------------------- //

    private val fileReaderCallback = object : FileReader.ProcessCallback {
        override fun onProcessCompleted(data: ArrayList<Pair<String, FloatArray>>, numImagesWithNoFaces: Int) {
            frameAnalyser.faceList = data
            saveSerializedImageData( data )
        }
    }


    private fun saveSerializedImageData(data : ArrayList<Pair<String,FloatArray>> ) {
        val serializedDataFile = File( filesDir , SERIALIZED_DATA_FILENAME )
        ObjectOutputStream( FileOutputStream( serializedDataFile )  ).apply {
            writeObject( data )
            flush()
            close()
        }
        sharedPreferences.edit().putBoolean( SHARED_PREF_IS_DATA_STORED_KEY , true ).apply()
    }


    private fun loadSerializedImageData() : ArrayList<Pair<String,FloatArray>> {
        val serializedDataFile = File( filesDir , SERIALIZED_DATA_FILENAME )
        val objectInputStream = ObjectInputStream( FileInputStream( serializedDataFile ) )
        val data = objectInputStream.readObject() as ArrayList<Pair<String,FloatArray>>
        objectInputStream.close()
        return data
    }


}
