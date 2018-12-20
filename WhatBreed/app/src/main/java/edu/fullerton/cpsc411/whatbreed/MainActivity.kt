package edu.fullerton.cpsc411.whatbreed

import android.app.Activity
import android.app.PendingIntent.getActivity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView

import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.activity_main.*
import org.tensorflow.lite.Interpreter;
import java.nio.channels.FileChannel.MapMode.READ_ONLY
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.util.Log
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.ArrayList


class MainActivity : AppCompatActivity() {

    val PICK_IMAGE = 100
    lateinit var imageUri: Uri
    lateinit var myImageView: ImageView
    lateinit var myTextView: TextView

    private val MODEL_PATH = "dog_optimized_graph.tflite"//"output.tflite"
    private val LABEL_PATH = "retrained_labels.txt"//"retrained_labels.txt"
    /** An instance of the driver class to run model inference with Tensorflow Lite.  */
    lateinit var tflite: Interpreter

    /** Labels corresponding to the output of the vision model.  */
    lateinit var labelList: List<String>

    /** an array to hold the inference results, to be fed into tensorflow lite as outputs. */
    private lateinit var labelProbArray: Array<FloatArray>

    /** A ByteBuffer to hold image data, to be feed into Tensorflow Lite as inputs.  */
    private lateinit var bitmapForImage: Bitmap

    /** A ByteBuffer to hold image data, to be feed into Tensorflow Lite as inputs.  */
    private lateinit var imgData: ByteBuffer

    /** Dimensions of inputs.  */
    private val DIM_BATCH_SIZE = 1

    private val DIM_PIXEL_SIZE = 3

    internal var DIM_IMG_SIZE_X = 224
    internal var DIM_IMG_SIZE_Y = 224

    private val IMAGE_MEAN = 128
    private val IMAGE_STD = 128.0f

    /* Preallocated buffers for storing image data in. */
    private var intValues = IntArray(DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y)



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            openGallery()
        }

        runModelCalc.setOnClickListener {view ->
            calcModel(this@MainActivity)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openGallery(){
        myImageView = dog_picture!!
        myTextView = textView2!!
        if(displayPrediction.visibility == View.VISIBLE){
            runModelCalc.visibility = View.VISIBLE
            displayPrediction.visibility = View.GONE
            textView2.visibility = View.VISIBLE
        }
        val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI )
        startActivityForResult(gallery, PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK && requestCode == PICK_IMAGE){
            imageUri = data!!.getData()
            val picturePath = getPath(this.getApplicationContext(), imageUri)
            bitmapForImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri)
            textView2.text = picturePath
            myImageView.setImageURI(imageUri)
        }
    }

    private fun getPath(context: Context, uri: Uri): String {
        var result: String? = null
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = context.getContentResolver().query(uri, proj, null, null, null)
        if (cursor != null) {
            if (cursor!!.moveToFirst()) {
                val column_index = cursor!!.getColumnIndexOrThrow(proj[0])
                result = cursor!!.getString(column_index)
            }
            cursor!!.close()
        }
        if (result == null) {
            result = "Not found"
        }
        return result
    }

    private fun calcModel(activity: Activity){

        tflite = Interpreter(loadModelFile(activity))
        labelList = loadLabelList(activity)
        labelProbArray = Array(1) { FloatArray(labelList.size) }
        imgData =
                ByteBuffer.allocateDirect(
                        4 * DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE)
        imgData.order(ByteOrder.nativeOrder())
        labelProbArray = Array(1) { FloatArray(labelList.size) }

        val bitmap = dog_picture.drawable as Drawable
        val newBitMap = (bitmap as BitmapDrawable).bitmap
        val newNewBitmap = Bitmap.createScaledBitmap(newBitMap, 224, 224, false)

        convertBitmapToByteBuffer(newNewBitmap)

        val startTime = SystemClock.uptimeMillis()
        tflite.run(imgData, labelProbArray)
        val endTime = SystemClock.uptimeMillis()

        var textToShow: String = getTopKLabels()
        textToShow = java.lang.Long.toString(endTime - startTime) + "ms" + textToShow
        displayPrediction.setText(textToShow)
        runModelCalc.visibility = View.GONE
        displayPrediction.visibility = View.VISIBLE
        textView2.visibility = View.GONE

        tflite.close()
    }

    /** Memory-map the model file in Assets.  */
    @Throws(IOException::class)
    private fun loadModelFile(activity: Activity): MappedByteBuffer {
        val fileDescriptor = activity.assets.openFd(MODEL_PATH) as AssetFileDescriptor
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.getChannel()
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /** Reads label list from Assets.  */
    @Throws(IOException::class)
    private fun loadLabelList(activity: Activity): List<String> {
        val reader = BufferedReader(InputStreamReader(activity.assets.open(LABEL_PATH)))
        var line: String
        var labelList = reader.readLines()
        return labelList
    }

    /**converts bitmap to bytebuffer.............NOT DONE HERE YETTTT**/
    private fun convertBitmapToByteBuffer(bitmap: Bitmap) {
        if (imgData == null) {
            return
        }
        imgData.rewind()
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        // Convert the image to floating point.
        var pixel = 0
        val startTime = SystemClock.uptimeMillis()
        for (i in 0 until DIM_IMG_SIZE_X) {
            for (j in 0 until DIM_IMG_SIZE_Y) {
                val `val` = intValues[pixel++]
                imgData.putFloat(((`val` shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                imgData.putFloat(((`val` shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                imgData.putFloat(((`val` and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
            }
        }
        val endTime = SystemClock.uptimeMillis()
        Log.d("time", "Timecost to put values into ByteBuffer: " + java.lang.Long.toString(endTime - startTime))
    }

    private fun getTopKLabels(): String{
        var first: Float
        var second: Float
        var third: Float
        var fIndex: Int
        var sIndex: Int
        var tIndex: Int

        third = java.lang.Float.MIN_VALUE
        second = third
        first = second
        tIndex = Integer.MIN_VALUE
        sIndex = tIndex
        fIndex = sIndex
        var textToShow = ""

        for (i in labelList.indices) {

            if (labelProbArray[0][i] > first) {
                tIndex = sIndex
                sIndex = fIndex
                fIndex = i
                third = second
                second = first
                first = labelProbArray[0][i]
            } else if (labelProbArray[0][i] > second) {
                tIndex = sIndex
                sIndex = i
                third = second
                second = labelProbArray[0][i]
            } else if (labelProbArray[0][i] > third)
                tIndex = i/* If arr[i] is in between first and
            second then update second  */
            third = labelProbArray[0][i]
        }


        textToShow = String.format("\n%s: %4.2f", labelList[tIndex], third) + textToShow
        textToShow = String.format("\n%s: %4.2f", labelList[sIndex], second) + textToShow
        textToShow = String.format("\n%s: %4.2f", labelList[fIndex], first) + textToShow

        return textToShow
    }

}
