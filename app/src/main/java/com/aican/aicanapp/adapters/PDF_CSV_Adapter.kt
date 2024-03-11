package com.aican.aicanapp.adapters

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.aican.aicanapp.CSVWebViewActivity
import com.aican.aicanapp.PDFViewer
import com.aican.aicanapp.R
import com.aican.aicanapp.databinding.CustomFileItemsBinding
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

class PDF_CSV_Adapter(
    val context: Context,
    private val files: Array<File>?,
    private val activity: String
) : RecyclerView.Adapter<PDF_CSV_Adapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var binding = CustomFileItemsBinding.bind(itemView)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(R.layout.custom_file_items, parent, false)
        )
    }

    override fun getItemCount(): Int = files!!.size


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val selectedFile = files!![position]
        holder.binding.fileNameTextView.text = selectedFile.name
        holder.binding.iconView.setImageResource(R.drawable.ic_baseline_insert_drive_file_24)

        holder.itemView.setOnClickListener {
            val path = when (activity) {
                "PhExport" -> ContextWrapper(context).externalMediaDirs[0].absolutePath + File.separator + "LabApp/Sensordata/" + selectedFile.name
                "EcExport" -> ContextWrapper(context).externalMediaDirs[0].absolutePath + File.separator + "LabApp/EcSensordata/" + selectedFile.name
                "PhCalib" -> ContextWrapper(context).externalMediaDirs[0].absolutePath + File.separator + "LabApp/CalibrationData/" + selectedFile.name
                "PhLog" -> ContextWrapper(context).externalMediaDirs[0].absolutePath + File.separator + "LabApp/Currentlog/" + selectedFile.name
                else -> ""
            }
            if (selectedFile.name.endsWith(".csv")){

                val csvContent = readCsvFileContent(path)

                // Convert CSV data to HTML
                val htmlContent = convertCsvToHtml(csvContent)

                // Start CSVWebViewActivity to display the HTML content
                val intent = Intent(context, CSVWebViewActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra("HtmlContent", htmlContent)
                }

                context.applicationContext.startActivity(intent)
//
//                val file = File(context.getExternalFilesDir(null), path)
//                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
//
//                val intent = Intent(Intent.ACTION_VIEW)
//                intent.setDataAndType(uri, "text/csv")
//                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//
//                try {
//                    context.startActivity(intent)
//                } catch (e: ActivityNotFoundException) {
//                    // Handle case where no suitable viewer app is installed
//                    Toast.makeText(context, "No app found to open CSV files", Toast.LENGTH_SHORT).show()
//                }
            }else {
                val intent = Intent(context.applicationContext, PDFViewer::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra("Path", path)
                }
                context.applicationContext.startActivity(intent)
            }
        }

        holder.itemView.setOnLongClickListener {
            val popupMenu = PopupMenu(context, it)
            popupMenu.menu.add("DELETE")
            popupMenu.menu.add("SHARE")

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.title) {
                    "DELETE" -> {
                        val deleted = selectedFile.delete()
                        if (deleted) {
                            Toast.makeText(
                                context.applicationContext,
                                "DELETED ",
                                Toast.LENGTH_SHORT
                            ).show()
                            files.toMutableList().remove(selectedFile)
                            notifyItemRemoved(position)
                        }
                    }

                    "SHARE" -> shareFile(selectedFile)
                }
                true
            }

            popupMenu.show()
            true
        }

        holder.binding.shareBtn.visibility = View.VISIBLE
        holder.binding.shareBtn.setOnClickListener { shareFile(selectedFile) }
    }

    fun readCsvFileContent(filePath: String): String {
        val file = File(filePath)
        val stringBuilder = StringBuilder()

        try {
            val bufferedReader = BufferedReader(FileReader(file))
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                stringBuilder.append(line).append("\n")
            }
            bufferedReader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return stringBuilder.toString()
    }
    fun convertCsvToHtml(csvData: String): String {
        val rows = csvData.split("\n")
        val sb = StringBuilder()

        sb.append("<table>")
        for (row in rows) {
            sb.append("<tr>")
            val columns = row.split(",")
            for (column in columns) {
                sb.append("<td>").append(column).append("</td>")
            }
            sb.append("</tr>")
        }
        sb.append("</table>")

        return sb.toString()
    }

    private fun shareFile(file: File) {
        val fileUri = FileProvider.getUriForFile(context, context.packageName + ".provider", file)
        val intentShare = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_STREAM, fileUri)
        }
        val chooserIntent = Intent.createChooser(intentShare, "Share file").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error sharing file", Toast.LENGTH_SHORT).show()
        }

    }
}

