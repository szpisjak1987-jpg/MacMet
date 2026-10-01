package com.example.macmet.ui.radar

import android.annotation.SuppressLint
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WeatherRadarSection(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Csapadék Radar (Windy)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp) // Kicsit magasabb, hogy a Windy UI jobban elférjen
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 2.dp,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            // Közvetlen Windy Embed URL, így elkerüljük a HTML CORS problémákat
            val windyUrl = remember(latitude, longitude) {
                "https://embed.windy.com/embed2.html?lat=$latitude&lon=$longitude&zoom=8&level=surface&overlay=radar&menu=&message=&marker=true&calendar=now&pressure=&type=map&location=coordinates&detail=&metricWind=km%2Fh&metricTemp=%C2%B0C&radarRange=-1"
            }

            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        // Hardveres gyorsítás bekapcsolása a WebGL-hez (Windy motorja)
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        webViewClient = WebViewClient()
                        webChromeClient = WebChromeClient()
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                        }
                        loadUrl(windyUrl)
                    }
                },
                update = { webView ->
                    // Nem töltjük újra folyamatosan, csak ha szükséges
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
