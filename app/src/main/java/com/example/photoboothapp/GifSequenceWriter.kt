package com.example.photoboothapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class GifSequenceWriter(outputFile: File?, private val frameDuration: Int) {
    private val outputStream = FileOutputStream(outputFile)
    private val frames = ArrayList<Bitmap>()

    fun addFrame(jpegFile: File) {
        // Decode the JPEG file into a Bitmap
        val bitmap = BitmapFactory.decodeFile(jpegFile.absolutePath)
        // Define the desired resolution, e.g., 1280x960 for 4:3
        val desiredWidth = 2560
        val desiredHeight = (desiredWidth * 3) / 4 // Maintain 4:3 aspect ratio

        // Resize the bitmap if it's not the desired size
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, desiredWidth, desiredHeight, true)
        frames.add(resizedBitmap)
    }

    @Throws(IOException::class)
    fun close() {
        if (frames.isEmpty()) return

        val firstFrame = frames[0]
        val canvas = Canvas(firstFrame)

        for (frame in frames) {
            canvas.drawBitmap(frame, 0f, 0f, null)
        }

        firstFrame.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.close()
    }
}
