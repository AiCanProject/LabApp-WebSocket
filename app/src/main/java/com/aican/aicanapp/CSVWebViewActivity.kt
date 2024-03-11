package com.aican.aicanapp



import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity

class CSVWebViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_csvweb_view)

        val webView = findViewById<WebView>(R.id.webView)

        // Enable JavaScript (optional)
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true

        // Load CSV data into WebView
        var csvData = "CSV data in HTML format" // Replace this with your CSV data converted to HTML
        csvData = intent.getStringExtra("HtmlContent").toString()
        webView.loadDataWithBaseURL(null, csvData, "text/html", "UTF-8", null)

        // Optional: Handle download requests (e.g., for links within the CSV data)
        webView.setDownloadListener { url, _, _, _, _ ->
            // Handle download requests here
        }
    }
}
