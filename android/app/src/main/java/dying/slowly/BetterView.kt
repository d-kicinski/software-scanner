package dying.slowly

import java.io.File
import java.io.FileOutputStream

import org.opencv.android.JavaCameraView

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.Camera
import android.hardware.Camera.PictureCallback
import android.hardware.Camera.Size
import android.os.Environment
import android.util.AttributeSet
import android.util.Log

//     android:theme="@android:style/Theme.NoTitleBar.Fullscreen"

class BetterView(context: Context, attrs: AttributeSet) : JavaCameraView(context, attrs), PictureCallback {
    private var mPictureFileName: String? = null
    public lateinit var onPictureTakenCallback: () -> Unit

    val effectList: List<String>
        get() = mCamera.parameters.supportedColorEffects

    val isEffectSupported: Boolean
        get() = mCamera.parameters.colorEffect != null

    var effect: String
        get() = mCamera.parameters.colorEffect
        set(effect) {
            val params = mCamera.parameters
            params.colorEffect = effect
            mCamera.parameters = params
        }

    val resolutionList: List<Size>
        get() = mCamera.parameters.supportedPreviewSizes

    var resolution: Size
        get() = mCamera.parameters.previewSize
        set(resolution) {
            disconnectCamera()
            mMaxHeight = resolution.height
            mMaxWidth = resolution.width
            connectCamera(width, height)
        }

    fun setBestResolution() {
        val param: Camera.Parameters
        param = mCamera.parameters

        var bestSize: Camera.Size? = null
        val sizeList = param.supportedPictureSizes
        bestSize = sizeList[0]
        for (i in 1 until sizeList.size) {
            if (sizeList[i].width * sizeList[i].height > bestSize!!.width * bestSize.height) {
                bestSize = sizeList[i]
            }
        }

        //param.setPreviewSize(sizeList[11]);
        param.setPictureSize(bestSize!!.width, bestSize.height)
        mCamera.parameters = param
    }

    fun takePicture(fileName: String) {
        Log.i(TAG, "Taking picture")
        this.mPictureFileName = fileName
        // Postview and jpeg are sent in the same buffers if the queue is not empty when performing a capture.
        // Clear up buffers to avoid mCamera.takePicture to be stuck because of a memory issue
        mCamera.setPreviewCallback(null)

        // PictureCallback is implemented by the current class
        mCamera.takePicture(null, null, this)
    }

    override fun onPictureTaken(data: ByteArray, camera: Camera) {
        Log.i(TAG, "Saving a bitmap to file")

        /*
        val fileName = "myImage"//no .png or .jpg needed
        val photo = File(Environment.getExternalStorageDirectory(), "photo.jpg")
        if (photo.exists()) {
            photo.delete()
        }*/

        // Write the image in a file (in jpeg format)
        try {
            val fos = FileOutputStream(mPictureFileName!!)
            fos.write(data)
            fos.close()

        } catch (e: java.io.IOException) {
            Log.e("PictureDemo", "Exception in photoCallback", e)
        }

        // The camera preview was automatically stopped. Start it again.
        mCamera.startPreview()
        mCamera.setPreviewCallback(this)
        onPictureTakenCallback()
    }

    companion object {

        private val TAG = "Sample::Tutorial3View"
    }
}
