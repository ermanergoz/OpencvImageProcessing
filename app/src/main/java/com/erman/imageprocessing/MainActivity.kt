package com.erman.imageprocessing

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.core.Core.bitwise_not
import org.opencv.core.Core.inRange
import org.opencv.imgproc.Imgproc.*
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private val imageBitmap by lazy { (ContextCompat.getDrawable(this, R.drawable.cisim) as BitmapDrawable).bitmap }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        applyGrayScale()
    }

    private fun getMidpoint(point1: Point, point2: Point): Point {
        return Point((point1.x + point2.x) / 2, (point1.y + point2.y) / 2)
    }

    private fun getLineLength(point1: Point, point2: Point): Double {
        return sqrt(((point1.x - point2.x) * (point1.x - point2.x)) + ((point1.y - point2.y) * (point1.y - point2.y)))
    }

    private fun applyGrayScale() {
        val frame = Mat()
        val mask1 = Mat()
        val mask2 = Mat()
        Utils.bitmapToMat(imageBitmap, frame)
        val hsv = Mat()
        cvtColor(frame, hsv, COLOR_RGB2HSV)

        inRange(hsv, Scalar(0.0, 156.0, 163.0), Scalar(179.0, 255.0, 255.0), mask1) //red
        inRange(hsv, Scalar(30.0, 140.0, 0.0), Scalar(130.0, 255.0, 255.0), mask2) //blue

        adaptiveThreshold(mask2, mask2, 255.0, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 15, 40.0)
        bitwise_not(mask2, mask2)

        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        findContours(mask2, contours, hierarchy, RETR_LIST, CHAIN_APPROX_NONE);

        for (i in 0 until contours.size) {
            val dst = MatOfPoint2f()
            contours[i].convertTo(dst, CvType.CV_32F)
            val minRect: RotatedRect = minAreaRect(dst)
            val rectPoints = arrayOfNulls<Point>(4)
            minRect.points(rectPoints)

            for (j in 0..3) {
                val sideLength = rectPoints[j]?.let { rectPoints[(j + 1) % 4]?.let { it1 -> getLineLength(it, it1) } }
                if (sideLength != null && sideLength > 100) {
                    line(frame, rectPoints[j], rectPoints[(j + 1) % 4], Scalar(255.0, 255.0, 0.0), 2);
                    putText(frame, sideLength.toString(), rectPoints[(j + 1) % 4]?.let { rectPoints[j]?.let { it1 -> getMidpoint(it1, it) } }, Core.FONT_HERSHEY_SIMPLEX, 1.0, Scalar(0.0, 0.0, 0.0), 4)
                    Log.e("Side length", sideLength.toString())
                }
            }
        }
        val bitmap = Bitmap.createBitmap(frame.cols(), frame.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(frame, bitmap)
        image.setImageBitmap(bitmap)
    }
}
