package com.example.ikondeitirici

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ikondeitirici.ui.theme.IkonDeğiştiriciTheme

class ShortcutProxyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val targetPackage = intent.getStringExtra("target_package")
        
        if (targetPackage == null) {
            finish()
            return
        }

        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val hasPassword = prefs.contains("app_password")

        if (!hasPassword) {
            launchTarget(targetPackage)
            return
        }

        setContent {
            IkonDeğiştiriciTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    PasswordScreen(onSuccess = {
                        launchTarget(targetPackage)
                    })
                }
            }
        }
    }

    private fun launchTarget(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            Toast.makeText(this, "Uygulama bulunamadı!", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
