package com.example.photoboothapp

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class PhotoListActivity : ComponentActivity() {

    private val bitmaps: MutableList<Bitmap> = mutableListOf()
    private lateinit var gifImageView: ImageView
    private lateinit var animationDrawable: AnimationDrawable
    private lateinit var retakeButton: ImageButton
    private lateinit var shareButton: ImageButton
    private lateinit var printButton: ImageButton
    private lateinit var emailButton: ImageButton
    private lateinit var tiktokButton: ImageButton
    private lateinit var facebookButton: ImageButton
    private lateinit var instagramButton: ImageButton
    private lateinit var emailPopup: RelativeLayout
    private lateinit var sharePopup: RelativeLayout
    private var isButtonPressed = false
    private var lastPressedButton: ImageButton? = null
    private var isSharePopupVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("PhotoListActivity", "Activity Created")
        setContentView(R.layout.photocontrolactivity)

        gifImageView = findViewById(R.id.gifImageView)

        // Load bitmaps from intent
        loadBitmapsFromIntent()

        if (bitmaps.size == 4) {
            createAndStartAnimation()
        } else {
            Log.e("PhotoListActivity", "Failed to load 4 images")
        }

        // Initialize buttons
        retakeButton = findViewById(R.id.buttonRetake)
        shareButton = findViewById(R.id.buttonShare)
        printButton = findViewById(R.id.buttonPrint)
        emailPopup = findViewById(R.id.emailPopup)
        sharePopup = findViewById(R.id.sharePopup)
        emailButton = findViewById(R.id.buttonEmail)
        instagramButton = findViewById(R.id.buttonInstagram)
        tiktokButton = findViewById(R.id.buttonTikTok)
        facebookButton = findViewById(R.id.buttonFacebook)

        checkPrintEnabled()

        // Set click listeners
        setupButtonListeners()
    }

    private fun checkPrintEnabled() {
        val sharedPref = getSharedPreferences("PhotoboothPrefs", Context.MODE_PRIVATE)
        val isPrintEnabled = sharedPref.getBoolean("isPrintEnabled", false)
        printButton.isEnabled = isPrintEnabled

        if (isPrintEnabled) {
            // Set the button to the normal image when enabled
            printButton.setBackgroundResource(R.drawable.print) // Replace with your enabled image
        } else {
            // Set the button to the disabled image when disabled
            printButton.setBackgroundResource(R.drawable.print_disable) // Replace with your disabled image
        }
    }


    private fun animateButton(button: ImageButton) {
        // Create scale animations for X and Y axes
        val scaleX = ObjectAnimator.ofFloat(button, "scaleX", 1f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 1.2f, 1f)

        // Optional: Add a slight rotation or alpha animation if desired
        val rotate = ObjectAnimator.ofFloat(button, "rotation", 0f, 15f, 0f)
        val alpha = ObjectAnimator.ofFloat(button, "alpha", 1f, 0.8f, 1f)

        // Combine animations into an AnimatorSet to play them together
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY, rotate, alpha)
        animatorSet.duration = 300 // Duration in milliseconds
        animatorSet.start()
    }


    private fun setupButtonListeners() {
        retakeButton.setOnClickListener {
            toggleSharePopup()
            animateButton(retakeButton)
            handleButtonPress(it as ImageButton) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        printButton.setOnClickListener {
            if (printButton.isEnabled) {
                animateButton(printButton)
                printImages()
            } else {
                Log.d("PhotoListActivity", "Print button is disabled")
            }
        }

        shareButton.setOnClickListener {
            // Perform the button click action (e.g., toggle the share popup)
            toggleSharePopup()
            animateButton(shareButton)
        }

        emailButton.setOnClickListener {
            animateButton(emailButton)
            toggleSharePopup()
            showEmailDialog()
        }



        instagramButton.setOnClickListener {
            toggleSharePopup()
            animateButton(instagramButton)

            shareImagesToInstagram()
        }

        tiktokButton.setOnClickListener {

            toggleSharePopup()
            animateButton(tiktokButton)
            shareImagesToTiktok()
        }

        facebookButton.setOnClickListener {

            toggleSharePopup()
            animateButton(facebookButton)
            shareImagesToFacebook()
        }
    }

    private fun toggleSharePopup() {
        sharePopup.visibility = if (isSharePopupVisible) {
            View.GONE
        } else {
            View.VISIBLE
        }
        isSharePopupVisible = !isSharePopupVisible
    }

    private fun loadBitmapsFromIntent() {
        for (i in 0 until 4) {
            val imageUriString = intent.getStringExtra("image_uri_$i")
            if (imageUriString != null) {
                val imageUri = Uri.parse(imageUriString)
                try {
                    val inputStream = contentResolver.openInputStream(imageUri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        bitmaps.add(bitmap)
                    } else {
                        Log.e("PhotoListActivity", "Failed to decode bitmap at index $i")
                    }
                    inputStream?.close()
                } catch (e: Exception) {
                    Log.e("PhotoListActivity", "Error loading bitmap at index $i", e)
                }
            } else {
                Log.e("PhotoListActivity", "Image URI is null at index $i")
            }
        }
    }

    private fun createAndStartAnimation() {
        animationDrawable = AnimationDrawable()
        animationDrawable.isOneShot = false // Loop indefinitely

        // Add frames to the animation
        for (bitmap in bitmaps) {
            val drawable = BitmapDrawable(resources, bitmap)
            animationDrawable.addFrame(drawable, 1000) // 1000ms = 1 second per frame
        }

        // Set the AnimationDrawable as the source for the ImageView
        gifImageView.setImageDrawable(animationDrawable)

        // Start the animation
        gifImageView.post { animationDrawable.start() }
    }

    private fun handleButtonPress(button: ImageButton, action: () -> Unit) {
        if (isButtonPressed && lastPressedButton == button) {
            resetButtons()
            isButtonPressed = false
            lastPressedButton = null
        } else {
            disableAllButtonsExcept(button)
            isButtonPressed = true
            lastPressedButton = button
            action()
        }
    }

    private fun disableAllButtonsExcept(button: ImageButton) {
        retakeButton.isEnabled = button == retakeButton
        shareButton.isEnabled = button == shareButton
        printButton.isEnabled = button == printButton
    }

    private fun resetButtons() {
        retakeButton.isEnabled = true
        shareButton.isEnabled = true
        printButton.isEnabled = true
        emailPopup.visibility = View.GONE
        sharePopup.visibility = View.GONE
    }

    private fun showEmailDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.custom_popup_email, null)
        val dialog = AlertDialog.Builder(this, R.style.TransparentDialog)
            .setView(dialogView)
            .create()

        val closeButton = dialogView.findViewById<ImageButton>(R.id.closeButton)
        val emailEditText = dialogView.findViewById<EditText>(R.id.emailEditText)

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        emailEditText.setOnEditorActionListener { _, _, _ ->
            val email = emailEditText.text.toString()
            if (email.isNotEmpty()) {
                sendEmailWithImages(email)
                dialog.dismiss()
            } else {
                emailEditText.error = "Please enter an email address"
            }
            true
        }

        dialog.show()
    }

    private fun sendEmailWithImages(email: String) {
        val imageUris = ArrayList<Uri>()

        try {
            for (bitmap in bitmaps) {
                val file = File(externalCacheDir, "image_${System.currentTimeMillis()}.png")
                val outputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.close()

                val imageUri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.provider",
                    file
                )
                imageUris.add(imageUri)
            }

            val emailIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                putExtra(Intent.EXTRA_SUBJECT, "Your Photos from PhotoBooth")
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris)
            }

            startActivity(Intent.createChooser(emailIntent, "Send email using..."))

        } catch (e: IOException) {
            Log.e("PhotoListActivity", "Error preparing email attachments", e)
        }
    }

    private fun prepareImagesForSharing(): List<Uri> {
        val imageUris = mutableListOf<Uri>()
        for (bitmap in bitmaps) {
            val file = saveBitmapToFile(bitmap)
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                file
            )
            imageUris.add(uri)
        }
        return imageUris
    }

    private fun shareImagesToInstagram() {
        val imageUris = prepareImagesForSharing()
        if (imageUris.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(imageUris))
                setPackage("com.instagram.android")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share images to Instagram"))
        }
    }

    private fun shareImagesToTiktok() {
        val imageUris = prepareImagesForSharing()
        if (imageUris.isNotEmpty()) {
            val intent = Intent("com.instagram.share.ADD_TO_STORY").apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, imageUris[0]) // Share the first image
                putExtra("interactive_asset_uri", imageUris[0])
                putExtra("top_background_color", "#000000")
                putExtra("bottom_background_color", "#000000")
                // Try both package names
                setPackage("com.zhiliaoapp.musically")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            // Check if TikTok is installed
            if (isPackageInstalled(this, "com.zhiliaoapp.musically") || isPackageInstalled(
                    this,
                    "com.ss.android.ugc.trill"
                )
            ) {
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("TikTokShare", "Error sharing to TikTok", e)
                    fallbackToGeneralShare(imageUris)
                }
            } else {
                Log.e("TikTokShare", "TikTok app is not installed.")
                fallbackToGeneralShare(imageUris)
            }
        } else {
            Log.e("TikTokShare", "No images to share")
            Toast.makeText(this, "No images available to share.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fallbackToGeneralShare(imageUris: List<Uri>) {
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(imageUris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share images")

        // Ensure TikTok appears in the chooser
        val tiktokIntent = intent.setPackage("com.zhiliaoapp.musically")
        val resInfoList =
            packageManager.queryIntentActivities(tiktokIntent, PackageManager.MATCH_DEFAULT_ONLY)
        for (resolveInfo in resInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            chooser.putExtra(
                Intent.EXTRA_INITIAL_INTENTS,
                arrayOf(Intent(intent).setPackage(packageName))
            )
        }

        startActivity(chooser)
    }

    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            // If the first package name fails, try the alternative
            if (packageName == "com.zhiliaoapp.musically") {
                try {
                    context.packageManager.getPackageInfo("com.ss.android.ugc.trill", 0)
                    true
                } catch (e: PackageManager.NameNotFoundException) {
                    false
                }
            } else {
                false
            }
        }
    }

    private fun shareImagesToFacebook() {
        val imageUris = prepareImagesForSharing()
        if (imageUris.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(imageUris))
                setPackage("com.facebook.katana")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share images to Facebook"))
        }
    }

    private fun saveBitmapToFile(bitmap: Bitmap): File {
        val file = File(externalCacheDir, "image_${System.currentTimeMillis()}.png")
        try {
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.close()
        } catch (e: IOException) {
            Log.e("PhotoListActivity", "Error saving bitmap to file", e)
        }
        return file
    }

    private fun printImages() {
        val printManager = getSystemService(PRINT_SERVICE) as PrintManager
        val jobName = getString(R.string.app_name) + " Document"

        printManager.print(jobName, object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?,
            ) {
                if (cancellationSignal!!.isCanceled) {
                    callback!!.onLayoutCancelled()
                    return
                }

                val pdi = PrintDocumentInfo.Builder(jobName)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()
                callback!!.onLayoutFinished(pdi, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?,
            ) {
                val a5PrintAttributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A5)
                    .setResolution(PrintAttributes.Resolution("default", "default", 300, 300))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()

                val pdfDocument = PrintedPdfDocument(this@PhotoListActivity, a5PrintAttributes)

                try {
                    val page = pdfDocument.startPage(0)
                    val canvas = page.canvas
                    val pageWidth = canvas.width
                    val pageHeight = canvas.height

                    // Load the template image
                    val templateBitmap = BitmapFactory.decodeResource(resources, R.drawable.template2)
                    canvas.drawBitmap(templateBitmap, 0f, 0f, null) // Draw the template onto the canvas

                    // Calculate positions and sizes for the four images on the template
                    val padding = 20 // adjust as needed for spacing
                    val templateImageWidth = templateBitmap.width / 2
                    val templateImageHeight = templateBitmap.height / 2

                    val positions = listOf(
                        Pair(padding, padding), // Top-left
                        Pair(templateImageWidth + padding, padding), // Top-right
                        Pair(padding, templateImageHeight + padding), // Bottom-left
                        Pair(templateImageWidth + padding, templateImageHeight + padding) // Bottom-right
                    )

                    // Scale and place the four images on the template
                    for (i in bitmaps.indices) {
                        val scaledBitmap = Bitmap.createScaledBitmap(
                            bitmaps[i],
                            templateImageWidth - padding,
                            templateImageHeight - padding,
                            true
                        )
                        canvas.drawBitmap(scaledBitmap, positions[i].first.toFloat(), positions[i].second.toFloat(), null)
                    }

                    pdfDocument.finishPage(page)

                    // Write the document to the destination
                    val outputStream = FileOutputStream(destination!!.fileDescriptor)
                    pdfDocument.writeTo(outputStream)
                    callback!!.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: IOException) {
                    Log.e("PhotoListActivity", "Error writing document", e)
                    callback!!.onWriteFailed(e.toString())
                } finally {
                    pdfDocument.close()
                }
            }

        }, null)
    }

}