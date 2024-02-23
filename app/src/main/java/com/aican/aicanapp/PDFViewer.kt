package com.aican.aicanapp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.github.barteksc.pdfviewer.PDFView
import java.io.File

class PDFViewer : AppCompatActivity() {

    private val TAG = "PDFViewerActivity"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdfviewer)

        //getting path from previous activity

        //getting path from previous activity
        val intent = intent
        val path = intent.getStringExtra("Path")
        Log.d(TAG, "onCreate: path -> $path")

        //Getting file from the path

        //Getting file from the path
        val file = File(path)

        val pdfView: PDFView = findViewById(R.id.pdfViewer)

        pdfView.fromFile(file).load()

    }
}