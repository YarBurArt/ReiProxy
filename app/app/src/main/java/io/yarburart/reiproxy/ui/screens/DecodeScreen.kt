package io.yarburart.reiproxy.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DecodeScreen(modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        // Clear cache on load to avoid cache miss
                        clearCache(true)
                    }
                }
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                loadUrl("https://gchq.github.io/CyberChef/")
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
