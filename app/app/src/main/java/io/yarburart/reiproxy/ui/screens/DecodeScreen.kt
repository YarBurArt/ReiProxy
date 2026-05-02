package io.yarburart.reiproxy.ui.screens

import android.annotation.SuppressLint
import android.os.Build
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DecodeScreen(modifier: Modifier = Modifier) {
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var fallbackInput by remember { mutableStateOf("") }
    var fallbackOutput by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        // WebView for CyberChef
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?,
                        ) {
                            hasError = true
                            isLoading = false
                        }
                    }
                    webChromeClient = WebChromeClient()
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.javaScriptCanOpenWindowsAutomatically = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                    settings.loadsImagesAutomatically = true
                    settings.setSupportMultipleWindows(true)
                    
                    // Force Desktop Mode
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        settings.mixedContentMode =
                            android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }

                    loadUrl("https://gchq.github.io/CyberChef/")
                }
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text("Loading CyberChef...", modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}
