package com.example.macmet.ui.radar

import android.annotation.SuppressLint
import android.view.View
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
            text = "Csapadék Radar (RainViewer)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 2.dp,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            val htmlContent = remember(latitude, longitude) {
                """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                    <style>
                        body, html, #map { height: 100%; width: 100%; margin: 0; padding: 0; background: #1a1c1e; }
                        .leaflet-container { background: #1a1c1e; }
                    </style>
                </head>
                <body>
                    <div id="map"></div>
                    <script>
                        var map = L.map('map', { zoomControl: true, attributionControl: false }).setView([$latitude, $longitude], 8);
                        
                        // CartoDB Dark Matter tile layer for a sleek dark weather map look
                        L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
                            maxZoom: 19,
                            subdomains: 'abcd'
                        }).addTo(map);

                        // Location marker
                        L.circleMarker([$latitude, $longitude], {
                            radius: 7,
                            fillColor: "#388e3c",
                            color: "#ffffff",
                            weight: 2,
                            opacity: 1,
                            fillOpacity: 0.9
                        }).addTo(map);

                        // Fetch RainViewer Radar Overlay
                        fetch('https://api.rainviewer.com/public/weather-maps.json')
                            .then(function(res) { return res.json(); })
                            .then(function(data) {
                                if (data && data.radar && data.radar.past && data.radar.past.length > 0) {
                                    var latest = data.radar.past[data.radar.past.length - 1];
                                    var tileUrl = data.host + latest.path + '/256/{z}/{x}/{y}/2/1_1.png';
                                    L.tileLayer(tileUrl, {
                                        opacity: 0.75,
                                        zIndex: 100
                                    }).addTo(map);
                                }
                            })
                            .catch(function(err) {
                                console.error('Radar load error:', err);
                            });
                    </script>
                </body>
                </html>
                """.trimIndent()
            }

            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        webViewClient = WebViewClient()
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        }
                        loadDataWithBaseURL("https://api.rainviewer.com", htmlContent, "text/html", "UTF-8", null)
                    }
                },
                update = { webView ->
                    webView.loadDataWithBaseURL("https://api.rainviewer.com", htmlContent, "text/html", "UTF-8", null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
