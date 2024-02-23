package com.aican.aicanapp

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aican.aicanapp.adapters.UserAdapter
import com.aican.aicanapp.adapters.UserDatabaseAdapter
import com.aican.aicanapp.data.DatabaseHelper
import com.aican.aicanapp.dataClasses.userDatabase.UserDatabaseModel
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserDatabase : AppCompatActivity() {

    private val userDatabaseModelList: ArrayList<UserDatabaseModel> = ArrayList<UserDatabaseModel>()
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var printBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_database)

        databaseHelper = DatabaseHelper(this)
        printBtn = findViewById<Button>(R.id.printBtn)

        val recyclerView = findViewById<RecyclerView>(R.id.user_database_recycler_view)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = UserDatabaseAdapter(this, getList())
        recyclerView.adapter = adapter

        printBtn.setOnClickListener(View.OnClickListener { view: View? ->
//            exportCsv()
//            try {
//                val workbook = Workbook(
//                    Environment.getExternalStorageDirectory()
//                        .absolutePath + File.separator + "/LabApp/UserData/UserData.xlsx"
//                )
//                val options = PdfSaveOptions()
//                val sdf =
//                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
//                val currentDateandTime = sdf.format(Date())
//                options.setCompliance(PdfCompliance.PDF_A_1_B)
//                workbook.save(
//                    Environment.getExternalStorageDirectory()
//                        .absolutePath + File.separator + "/LabApp/UserData/UserData" + currentDateandTime + ".pdf",
//                    options
//                )
//                val path1 = Environment.getExternalStorageDirectory()
//                    .absolutePath + File.separator + "/LabApp/UserData"
//                val root1 = File(path1)
//                fileNotWrite(root1)
//                val filesAndFolders1 = root1.listFiles()
//                if (filesAndFolders1 == null || filesAndFolders1.size == 0) {
//                    return@setOnClickListener
//                } else {
//                    for (i in filesAndFolders1.indices) {
//                        if (filesAndFolders1[i].name
//                                .endsWith(".csv") || filesAndFolders1[i].name
//                                .endsWith(".xlsx")
//                        ) {
//                            filesAndFolders1[i].delete()
//                        }
//                    }
//                }
//                val pathPDF = Environment.getExternalStorageDirectory()
//                    .absolutePath + File.separator + "/LabApp/UserData/"
//                val rootPDF = File(pathPDF)
//                fileNotWrite(rootPDF)
//                val filesAndFoldersPDF = rootPDF.listFiles()
//                val filesAndFoldersNewPDF =
//                    arrayOfNulls<File>(1)
//                if (filesAndFoldersPDF == null || filesAndFoldersPDF.size == 0) {
//                    return@setOnClickListener
//                } else {
//                    for (i in filesAndFoldersPDF.indices) {
//                        if (filesAndFoldersPDF[i].name.endsWith(".pdf")) {
//                            filesAndFoldersNewPDF[0] = filesAndFoldersPDF[i]
//                        }
//                    }
//                }
//                val pdfRecyclerView = findViewById<RecyclerView>(R.id.userDataPDF)
//                val plAdapter = UserAdapter(this, filesAndFoldersPDF)
//                pdfRecyclerView.adapter = plAdapter
//                plAdapter.notifyDataSetChanged()
//                pdfRecyclerView.layoutManager = LinearLayoutManager(this)
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
        })


    }

    fun fileNotWrite(file: File) {
        file.setWritable(false)
        if (file.canWrite()) {
            Log.d("csv", "Not Working")
        } else {
            Log.d("csvnw", "Working")
        }
    }

    private fun getList(): List<UserDatabaseModel?>? {
        val res = databaseHelper!!._data
        if (res.count == 0) {
            Toast.makeText(this@UserDatabase, "No entry", Toast.LENGTH_SHORT).show()
        }
        while (res.moveToNext()) {
            userDatabaseModelList.add(
                UserDatabaseModel(
                    res.getString(0),
                    res.getString(3),
                    res.getString(2),
                    res.getString(1),
                    res.getString(4),
                    res.getString(5)
                )
            )
        }
        return userDatabaseModelList
    }

    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this@UserDatabase, Dashboard::class.java))
        finish()
    }

//    fun exportCsv() {
//        //We use the Download directory for saving our .csv file.
//        val exportDir =
//            File(Environment.getExternalStorageDirectory().absolutePath + File.separator + "/LabApp/UserData")
//        if (!exportDir.exists()) {
//            exportDir.mkdirs()
//        }
//        val file: File
//        var printWriter: PrintWriter? = null
//        try {
//            file = File(exportDir, "UserData.csv")
//            file.createNewFile()
//            printWriter = PrintWriter(FileWriter(file), true)
//            val nullEntry = " "
//            val db = databaseHelper!!.writableDatabase
//            val curCSV = db.rawQuery("SELECT * FROM UserDataDetails", null)
//            printWriter.println("$nullEntry,$nullEntry,$nullEntry,$nullEntry")
//            printWriter.println("$nullEntry,$nullEntry,$nullEntry,$nullEntry")
//            printWriter.println("UserData Table,$nullEntry,$nullEntry,$nullEntry,$nullEntry,$nullEntry,$nullEntry")
//            printWriter.println("UserName, UserRole, DateCreated, ExpiryDate")
//            printWriter.println("$nullEntry,$nullEntry,$nullEntry,$nullEntry")
//            while (curCSV.moveToNext()) {
//                val userName = curCSV.getString(curCSV.getColumnIndex("Username"))
//                val userRole = curCSV.getString(curCSV.getColumnIndex("Role"))
//                val dateCreated = curCSV.getString(curCSV.getColumnIndex("dateCreated"))
//                var expiryDate = ""
//                expiryDate = if (userRole == "Admin") {
//                    "No expiry"
//                } else {
//                    curCSV.getString(curCSV.getColumnIndex("expiryDate"))
//                }
//                val record = "$userName,$userRole,$dateCreated,$expiryDate"
//                printWriter.println(record)
//            }
//            printWriter.println("$nullEntry,$nullEntry")
//            printWriter.println("$nullEntry,$nullEntry")
//            printWriter.println("$nullEntry,$nullEntry")
//            printWriter.println("$nullEntry,$nullEntry")
//            printWriter.println("Operator Sign,$nullEntry,$nullEntry,$nullEntry,Supervisor Sign,$nullEntry,$nullEntry")
//            curCSV.close()
//            db.close()
//            val loadOptions = LoadOptions(FileFormatType.CSV)
//            val inputFile =
//                Environment.getExternalStorageDirectory().absolutePath + File.separator + "/LabApp/UserData/"
//            val workbook = Workbook(inputFile + "UserData.csv", loadOptions)
//            val worksheet: Worksheet = workbook.getWorksheets().get(0)
//            worksheet.getCells().setColumnWidth(0, 10.5)
//            worksheet.getCells().setColumnWidth(2, 10.5)
//            workbook.save(
//                Environment.getExternalStorageDirectory().absolutePath + File.separator + "/LabApp/UserData/UserData.xlsx",
//                SaveFormat.XLSX
//            )
//        } catch (e: java.lang.Exception) {
//            Log.d("csvexception", e.toString())
//        }
//    }
}