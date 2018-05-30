package dying.slowly;

import android.app.Activity
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import org.opencv.core.Mat
import slowly.dying.R

import org.opencv.android.*

import android.view.*
import java.util.*
import android.content.Intent
import android.net.Uri
import java.io.File
import android.graphics.Bitmap
import android.widget.ImageView
import org.opencv.core.Core
import org.opencv.imgproc.Imgproc
import android.graphics.Point
import android.os.Handler
import android.widget.Switch
import android.widget.TextView
import dying.slowly.generated.CoreLibInterface
import org.opencv.core.CvType
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import java.io.FileOutputStream
import org.opencv.core.Point as cvPoint

import java.io.IOException

class MainActivity : Activity(), CameraBridgeViewBase.CvCameraViewListener2 {
    private val TAG = "MainActivity"

    private var mOpenCvCameraView: BetterView? = null
    private lateinit var mSwitch: Switch
    private var mCurrentFrame: Mat? = null

    private lateinit var mFileName: String
    // private lateinit var currentFrame: Mat
    val VALIDATE_SCAN_REQUEST = 1
    val FRAME_PROCESS_PERIOD = 5 // in milis
    private var m100milisCounter: Int = 0

    companion object {
        // init block executes before first object is created
        init {
            System.loadLibrary("software_scanner")
            System.loadLibrary("opencv_java3") // comment this when using OpenCV Manager
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams
        //        .FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main)
        mOpenCvCameraView = findViewById(R.id.HelloVisionView)
        mOpenCvCameraView!!.onPictureTakenCallback = {
            displayImage()
        }


        //mOpenCvCameraView!!.setMaxFrameSize(720, 480)
        mOpenCvCameraView!!.visibility = SurfaceView.VISIBLE
        mOpenCvCameraView!!.setCvCameraViewListener(this)
        mOpenCvCameraView!!.enableView()
        //this@OpenCVCameraView.activity.setRequestedOrientation(ActivityInfo
        //        .SCREEN_ORIENTATION_LANDSCAPE);

        val button = findViewById<Button>(R.id.main_button) as Button
        button.setOnClickListener {
            onPictureTaken()
            Log.d(TAG, "Round button clicked")
        }

        mSwitch = findViewById<Switch>(R.id.switch1)
        mSwitch.rotation = (-90).toFloat()



        val handler = Handler()
        val delay = 100 //milliseconds
        handler.postDelayed(object : Runnable {
            override fun run() {
                m100milisCounter++
                Log.d(TAG,"100minil past, now:" + m100milisCounter.toString())
                handler.postDelayed(this, delay.toLong())
            }
        }, delay.toLong())
    }


    private fun onPictureTaken() {
        //val imageToDisplay = currentFrame.clone()
        mOpenCvCameraView!!.setBestResolution()
        //var res = mOpenCvCameraView!!.getResolutionList()

        val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
        val currentDateAndTime = sdf.format(Date())

/*        mFileName = Environment.getExternalStoragePublicDirectory(Environment
                .DIRECTORY_PICTURES).path + "/sample_picture_" + currentDateAndTime + ".jpg"*/

/*        mFileName = Environment.getExternalStorageDirectory().path + "/sample_picture_" +
        currentDateAndTime + ".jpg"*/

        val photo = File(Environment.getExternalStorageDirectory(), "photo.jpg")
        if (photo.exists()) {
            photo.delete()
        }
        mFileName = photo.path
        mOpenCvCameraView!!.takePicture(photo.path)
        //galleryAddPic()

/*        Toast.makeText(mOpenCvCameraView!!.context, mFileName + " saved", Toast
                .LENGTH_SHORT)
                .show()*/
    }

    private fun displayImage() {
        val intent = Intent(this, DisplayIntend::class.java)
        intent.putExtra("filePath", mFileName)
        startActivityForResult(intent, VALIDATE_SCAN_REQUEST)

        // TODO handle results
    }

    private fun galleryAddPic() {
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        val f = File(mFileName)
        val contentUri = Uri.fromFile(f)
        mediaScanIntent.data = contentUri
        mOpenCvCameraView!!.context.sendBroadcast(mediaScanIntent)
    }

    override fun onResume() {
        super.onResume()
        mCurrentFrame = Mat()
    }

    override fun onDestroy() {
        super.onDestroy()
        mOpenCvCameraView!!.disableView()
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        mCurrentFrame = Mat(height, width, CvType.CV_8UC4)
    }

    override fun onCameraViewStopped() {
        mCurrentFrame!!.release()
    }


    override fun onCameraFrame(frame: CameraBridgeViewBase.CvCameraViewFrame): Mat? {
        //mCurrentFrame.release()
        mCurrentFrame = frame.rgba()
        //CoreLibInterface.cvtGray(mCurrentFrame!!.nativeObjAddr)
        if (mSwitch.isChecked) {
            if (m100milisCounter >= FRAME_PROCESS_PERIOD)
            {
                if (!mCurrentFrame!!.empty()) {
                    CoreLibInterface.drawContour(mCurrentFrame!!.nativeObjAddr)
                }
            }
        }


        return mCurrentFrame
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == VALIDATE_SCAN_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                val isScanAccepted = data.extras.getBoolean("isScanAccepted")

                // delete temporary file
                val photo = File(mFileName)
                if (photo.exists()) {
                    photo.delete()
                }
                galleryAddPic()
                Log.i(TAG, "Scan accepted: " + isScanAccepted.toString())
            }
        }
    }
}


class DisplayIntend : Activity() {
    private lateinit var scan_view: ImageView
    private lateinit var closeButton: Button
    private lateinit var saveButton: Button
    private lateinit var originalImage: Mat
    private lateinit var sampledImage: Mat
    private lateinit var scannedImage: Mat
    private lateinit var rgbImage: Mat

    private lateinit var mScannedBitMap: Bitmap
    private val TAG = "DisplayIntend"

    companion object {
        // init block executes before first object is created
        init {
            System.loadLibrary("software_scanner")
            System.loadLibrary("opencv_java3") // comment this when using OpenCV Manager
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.scanned_image)
        scan_view = findViewById(R.id.scanned_image_view)
        closeButton = findViewById(R.id.close_button)
        saveButton = findViewById(R.id.save_button)

        closeButton.setOnClickListener { onCloseButton() }
        saveButton.setOnClickListener { onSaveButton() }

        val fileName = intent.getStringExtra("filePath")
        rgbImage = loadImage(fileName)
        scannedImage = scanImage(rgbImage)
        displayImage(scannedImage)

    }

    private fun onCloseButton() {
        releaseAll()
        val intent = Intent()
        intent.putExtra("isScanAccepted", false)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun onSaveButton() {
        releaseAll()
        // Create valid name for scan pic and save it in Public storage
        val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
        val currentDateAndTime = sdf.format(Date())
        val fileName = Environment.getExternalStoragePublicDirectory(Environment
                .DIRECTORY_PICTURES).path + "/scan_picture_" + currentDateAndTime + ".png"

        Log.d(TAG, "save button clicked")

        // Write the image in a file (in jpeg format) // need bytes
        var fos: FileOutputStream
        try {
            fos = FileOutputStream(fileName)
            mScannedBitMap.compress(Bitmap.CompressFormat.PNG, 100, fos);

            fos.close()
            Log.d(TAG, "should save")
            Log.d(TAG, "SAVE AT: " + fileName)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "Exception in photoCallback", e)
        }

        val intent = Intent()
        intent.putExtra("isScanAccepted", true)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun displayImage(image: Mat) {
        scaleImage(image)
        // create a bitMap
        mScannedBitMap = Bitmap.createBitmap(sampledImage.cols(), sampledImage.rows(), Bitmap.Config.RGB_565)
        // convert to bitmap:
        Utils.matToBitmap(sampledImage, mScannedBitMap)

        // find the imageview and draw it!
        scan_view.setImageBitmap(mScannedBitMap)
    }

    private fun loadImage(path: String) : Mat {
        originalImage = Imgcodecs.imread(path)
        val rgbImage = Mat()

        Imgproc.cvtColor(originalImage, rgbImage, Imgproc.COLOR_BGR2RGB)
        return rgbImage
    }

    private fun  scaleImage(rgbImage: Mat){
        val display = windowManager.defaultDisplay
        //This is "android graphics Point" class
        val size = Point()
        display.getSize(size)

        val width = size.x
        val height = size.y
        sampledImage = Mat()

        // TODO do it in native
        val downSampleRatio = calculateSubSampleSize(rgbImage, width, height)

        Imgproc.resize(rgbImage, sampledImage, Size(), downSampleRatio, downSampleRatio, Imgproc.INTER_AREA)

        sampledImage = sampledImage.t()
        //flip on the y-axis
        Core.flip(sampledImage, sampledImage, 1)

    }

    private fun scanImage(sampledImage: Mat): Mat {
        scannedImage = Mat()
        CoreLibInterface.softwareScanner(sampledImage.nativeObjAddr, scannedImage.nativeObjAddr)
        return scannedImage

    }

    private fun calculateSubSampleSize(srcImage: Mat, reqWidth: Int, reqHeight: Int): Double {
        // Raw height and width of image
        val height = srcImage.height()
        val width = srcImage.width()
        var inSampleSize = 1.0

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of requested height and width to the raw
            //height and width
            val heightRatio = reqHeight.toDouble() / height.toDouble()
            val widthRatio = reqWidth.toDouble() / width.toDouble()

            // Choose the smallest ratio as inSampleSize value, this will
            //guarantee final image with both dimensions larger than or
            //equal to the requested height and width.
            return if (heightRatio < widthRatio) heightRatio else widthRatio
        }
        return inSampleSize
    }

    private fun releaseAll() {
        originalImage.release()
        sampledImage.release()
        scannedImage.release()
        rgbImage.release()
    }
}
