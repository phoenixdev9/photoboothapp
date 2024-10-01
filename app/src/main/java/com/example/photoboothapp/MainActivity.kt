package com.example.photoboothapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen(
                onScreenTap = { navigateToPhotoList() },
                onCheckboxCheckedChange = { isChecked ->
                    saveCheckboxState(isChecked) // Save checkbox state
                }
            )
        }
    }

    private fun navigateToPhotoList() {
        Log.d("MainActivity", "Navigating to PhotoListActivity")
        val intent = Intent(this, MyCameraActivity::class.java)
        startActivity(intent)
    }

    // Save checkbox state to SharedPreferences
    private fun saveCheckboxState(isChecked: Boolean) {
        val sharedPref = getSharedPreferences("PhotoboothPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("isPrintEnabled", isChecked)
            apply()
        }
    }
}


@Composable
fun MainScreen(onScreenTap: () -> Unit, onCheckboxCheckedChange: (Boolean) -> Unit) {
    var isChecked by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onScreenTap() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val kanitFontFamily = FontFamily(
                Font(R.font.kanitblack, FontWeight.Black),
                Font(R.font.kanitregular, FontWeight.Normal)
            )

            // Touch to start text
            Text(
                text = "TOUCHER\nPOUR\nCOMMENCER",
                fontFamily = kanitFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 36.sp,
                textAlign = TextAlign.Center,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Touch to start secondary text
            Text(
                text = "TOUCH TO START",
                fontFamily = kanitFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(150.dp))

            // Beaupassage image
            Image(
                painter = painterResource(id = R.drawable.smalllog),
                contentDescription = "Background Image",
                modifier = Modifier.fillMaxWidth(0.45f)
            )

            Spacer(modifier = Modifier.height(50.dp))

            // Checkbox at the bottom
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = {
                        isChecked = it
                        onCheckboxCheckedChange(it) // Notify parent about change
                    }
                )
                Text(text = "Enable Printing")
            }
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun PreviewMainScreen() {
//    MainScreen(onScreenTap = {})

