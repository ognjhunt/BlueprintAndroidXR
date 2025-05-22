package com.example.blueprintvision.ui

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.example.blueprintvision.ui.theme.BlueprintVisionTheme
import com.google.firebase.auth.FirebaseAuth
import com.mixpanel.android.mpmetrics.MixpanelAPI

/**
 * Activity that displays the privacy policy using a WebView.
 */
class PrivacyPolicyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            BlueprintVisionTheme {
                PrivacyPolicyScreen(
                    onClose = { finish() }
                )
            }
        }
    }
}

@Composable
fun PrivacyPolicyScreen(onClose: () -> Unit) {
    // The URL pointing to the privacy policy that will be displayed within the web view
    val privacyPolicyURL = "https://www.freeprivacypolicy.com/live/c6db5a65-4e92-41ae-b465-44fe5c5f282e"
    
    // Track event when the view appears
    LaunchedEffect(key1 = Unit) {
        val properties = HashMap<String, Any>()
        properties["blueprint_id"] = getSharedPreferences("blueprintPrefs", 0).getString("SelectedBlueprintID", "") ?: ""
        properties["session_id"] = getSharedPreferences("blueprintPrefs", 0).getString("SessionID", "") ?: ""
        properties["sign_up_variant"] = getSharedPreferences("blueprintPrefs", 0).getString("sign_up_variant", "") ?: ""
        properties["user_id"] = FirebaseAuth.getInstance().currentUser?.uid ?: 
                               getSharedPreferences("blueprintPrefs", 0).getString("temporaryUserID", "") ?: ""
        properties["date"] = System.currentTimeMillis()
        
        MixpanelAPI.getInstance(onClose as ComponentActivity, "MIXPANEL_TOKEN").track("privacy_policy_view_entered", properties)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Black
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // WebView to display the privacy policy
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = true
                        loadUrl(privacyPolicyURL)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
