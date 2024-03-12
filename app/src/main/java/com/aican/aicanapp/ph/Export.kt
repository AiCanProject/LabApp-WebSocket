package com.aican.aicanapp.ph

import android.Manifest
import android.Manifest.permission
import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.aican.aicanapp.R
import com.aican.aicanapp.adapters.PDF_CSV_Adapter
import com.aican.aicanapp.adapters.UserDataAdapter
import com.aican.aicanapp.data.DatabaseHelper
import com.aican.aicanapp.databinding.ActivityExportBinding
import com.aican.aicanapp.roomDatabase.daoObjects.AllLogsDataDao
import com.aican.aicanapp.roomDatabase.daoObjects.UserActionDao
import com.aican.aicanapp.roomDatabase.database.AppDatabase
import com.aican.aicanapp.roomDatabase.entities.AllLogsEntity
import com.aican.aicanapp.utils.Constants
import com.aican.aicanapp.utils.SharedKeys
import com.aican.aicanapp.utils.SharedPref
import com.aican.aicanapp.utils.Source
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.common.base.Splitter
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.opencsv.CSVWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class Export : AppCompatActivity() {


    companion object {
        const val MY_CAMERA_PERMISSION_CODE: Int = 100
        const val CAMERA_REQUEST: Int = 1888
        val permissions: Array<String> = arrayOf(Manifest.permission.CAMERA)
        const val PERMISSION_REQUEST_CODE: Int = 200
        var PICK_IMAGE = 1

    }

    lateinit var calib_stat: String

    lateinit var selectCompanyLogo: Button
    lateinit var companyLogo: ImageView
    lateinit var mDateBtn: Button
    lateinit var exportUserData: Button
    lateinit var exportPDFAllLogDataBtn: Button
    lateinit var convertToXls: Button
    lateinit var arNumBtn: ImageButton
    lateinit var batchNumBtn: ImageButton
    lateinit var compoundBtn: ImageButton
    lateinit var tvStartDate: TextView
    lateinit var tvEndDate: TextView
    lateinit var tvUserLog: TextView
    lateinit var deviceId: TextView
    lateinit var dateA: TextView
    var deviceID: String = ""
    var user: String = ""
    var roleExport: String = ""
    var reportDate: String = ""
    var reportTime: String = ""
    var startDateString: String = ""
    var endDateString: String = ""
    var startTimeString: String = ""
    var endTimeString: String = ""
    var arNumString: String? = null
    var compoundName: String? = null
    var batchNumString: String? = null
    lateinit var offset: String
    lateinit var battery: String
    lateinit var slope: String

    var startHour: Int = 0
    var startMinute: kotlin.Int = 0
    var endHour: kotlin.Int = 0
    var endMinute: kotlin.Int = 0


    lateinit var temp: String
    lateinit var companyName: String
    lateinit var nullEntry: String
    lateinit var printAllCalibData: Button
    lateinit var fAdapter: PDF_CSV_Adapter
    lateinit var uAdapter: UserDataAdapter
    lateinit var companyNameEditText: EditText
    lateinit var arNumEditText: EditText
    lateinit var batchNumEditText: EditText
    lateinit var compoundNameEditText: EditText
    lateinit var databaseHelper: DatabaseHelper
    lateinit var month: String
    lateinit var year: String
    lateinit var binding: ActivityExportBinding
    lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recyclerView = findViewById<RecyclerView>(R.id.recyclerViewCSV)
        val userRecyclerView = findViewById<RecyclerView>(R.id.recyclerViewUserData)
//        TextView noFilesText = findViewById(R.id.nofiles_textview);
        //        TextView noFilesText = findViewById(R.id.nofiles_textview);
        selectCompanyLogo = findViewById<Button>(R.id.selectCompanyLogo)
        selectCompanyLogo.setOnClickListener { v: View? -> showOptionDialog() }
        companyLogo = findViewById<ImageView>(R.id.companyLogo)
        deviceId = findViewById<TextView>(R.id.DeviceId)
        dateA = findViewById<TextView>(R.id.dateA)
        exportPDFAllLogDataBtn = findViewById<Button>(R.id.exportCSV)
        printAllCalibData = findViewById(R.id.printAllCalibData)
        mDateBtn = findViewById<Button>(R.id.materialDateBtn)
        arNumEditText = findViewById<EditText>(R.id.ar_num_sort)
        exportUserData = findViewById<Button>(R.id.exportUserData)
        batchNumEditText = findViewById<EditText>(R.id.batch_num_sort)
        arNumBtn = findViewById<ImageButton>(R.id.ar_text_button)
        batchNumBtn = findViewById<ImageButton>(R.id.batch_text_button)
        compoundBtn = findViewById<ImageButton>(R.id.compound_text_button)
        compoundNameEditText = findViewById<EditText>(R.id.compound_num_sort)
        convertToXls = findViewById<Button>(R.id.convertToXls)
        tvUserLog = findViewById<TextView>(R.id.tvUserLog)


        deviceID = PhActivity.DEVICE_ID!!

        binding.DeviceId.text = deviceID

        companyNameEditText = findViewById<EditText>(R.id.companyName)

        companyNameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                companyName = charSequence.toString()

                SharedPref.saveData(this@Export, "COMPANY_NAME", companyName)
            }

            override fun afterTextChanged(editable: Editable) {}
        })
        databaseHelper = DatabaseHelper(this)
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        databaseHelper.insert_action_data(
            time,
            date,
            "Exported : " + Source.logUserName,
            "",
            "",
            "",
            "",
            PhActivity.DEVICE_ID
        )

        nullEntry = " "

        val res = databaseHelper._userActivity_data
        if (res != null) {
            if (res.moveToFirst()) {
                year = res.getString(1).substring(0, 4)
                month = res.getString(1).substring(5, 7)
                dateA.text = "Data available from " + res.getString(1)
            }
        } else {
            dateA.text = "No data available"
            month = "01"
            year = Calendar.YEAR.toString()
        }

        convertToXls.visibility = View.INVISIBLE

        if (!Source.cfr_mode) {
            exportUserData.visibility = View.GONE
            userRecyclerView.visibility = View.GONE
            tvUserLog.visibility = View.GONE

        }
        mDateBtn.setOnClickListener {
            val calendar1 = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

            // now set the starting bound from current month to
            // previous MARCH
            calendar1[Calendar.MONTH] = when (month) {
                "01" -> Calendar.JANUARY
                "02" -> Calendar.FEBRUARY
                "03" -> Calendar.MARCH
                "04" -> Calendar.APRIL
                "05" -> Calendar.MAY
                "06" -> Calendar.JUNE
                "07" -> Calendar.JULY
                "08" -> Calendar.AUGUST
                "09" -> Calendar.SEPTEMBER
                "10" -> Calendar.OCTOBER
                "11" -> Calendar.NOVEMBER
                "12" -> Calendar.DECEMBER
                else -> Calendar.JANUARY
            }

            val start = calendar1.timeInMillis

            // now set the ending bound from current month to
            // DECEMBER
            calendar1[Calendar.MONTH] = Calendar.DECEMBER
            val end = calendar1.timeInMillis

            val calendarConstraintBuilder = CalendarConstraints.Builder()
            calendarConstraintBuilder.setStart(start)
            calendarConstraintBuilder.setEnd(end)

            val datePicker =
                MaterialDatePicker.Builder.dateRangePicker()
                    .setSelection(
                        Pair(
                            MaterialDatePicker.thisMonthInUtcMilliseconds(),
                            MaterialDatePicker.todayInUtcMilliseconds()
                        )
                    )
                    .setTitleText("Select dates")
                    .setCalendarConstraints(calendarConstraintBuilder.build())
                    .build()
            datePicker.show(supportFragmentManager, "date")

            datePicker.addOnPositiveButtonClickListener { selection ->
                val startDate = selection.first
                val endDate = selection.second
                startDateString = DateFormat.format("yyyy-MM-dd", Date(startDate)).toString()
                endDateString = DateFormat.format("yyyy-MM-dd", Date(endDate)).toString()

                binding.dateText.text =
                    "From: $startDateString to: $endDateString"

                val date1 = "Start: $startDateString End: $endDateString"
                Toast.makeText(this, date1, Toast.LENGTH_SHORT).show()

            }
        }

        binding.materialTimeBtn.setOnClickListener {

            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setTitleText("Select Start Time")
                .setHour(12)
                .setMinute(10)
                .build()
            timePicker.show(supportFragmentManager, "time")

            timePicker.addOnPositiveButtonClickListener {
                startHour = timePicker.hour
                startMinute = timePicker.minute

                val calendar = Calendar.getInstance()
                calendar[Calendar.HOUR_OF_DAY] = startHour
                calendar[Calendar.MINUTE] = startMinute

                startTimeString = DateFormat.format("HH:mm", calendar).toString()
                binding.timeText.text =
                    "From: $startDateString [ $startTimeString ] to: $endDateString [ $endTimeString ]"

                val timePicker2 = MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setTitleText("Select End Time")
                    .setHour(12)
                    .setMinute(10)
                    .build()
                timePicker2.show(supportFragmentManager, "time")

                timePicker2.addOnPositiveButtonClickListener { dialog2 ->
                    endHour = timePicker2.hour
                    endMinute = timePicker2.minute

                    val calendar2 = Calendar.getInstance()
                    calendar2[Calendar.HOUR_OF_DAY] = endHour
                    calendar2[Calendar.MINUTE] = endMinute

                    endTimeString = DateFormat.format("HH:mm", calendar2).toString()

                    binding.timeText.text =
                        "From: $startTimeString to: $endTimeString"

                }
            }
        }

        binding.dateText.setOnClickListener {
            binding.dateText.text = "Not selected"
            startDateString = ""
            endDateString = ""
        }

        binding.timeText.setOnClickListener {
            binding.timeText.text = "Not selected"
            startTimeString = ""
            endTimeString = ""
        }

        arNumBtn.setOnClickListener { arNumString = arNumEditText.text.toString() }

        batchNumBtn.setOnClickListener { batchNumString = batchNumEditText.text.toString() }

        compoundBtn.setOnClickListener { compoundName = compoundNameEditText.text.toString() }


        val pathPDF =
            ContextWrapper(this@Export).externalMediaDirs[0].toString() + File.separator + "/LabApp/Sensordata/"
        val rootPDF = File(pathPDF)
        fileNotWrite(rootPDF)
        val filesAndFoldersPDF = rootPDF.listFiles()


        fAdapter = PDF_CSV_Adapter(
            applicationContext,
            reverseFileArray(filesAndFoldersPDF ?: arrayOfNulls<File>(0)),
            "PhExport"
        )
        recyclerView.adapter = fAdapter
        fAdapter.notifyDataSetChanged()
        recyclerView.layoutManager = LinearLayoutManager(applicationContext)


        val path11 =
            ContextWrapper(this@Export).externalMediaDirs[0].toString() + File.separator + "/LabApp/Useractivity"
        val root11 = File(path11)

        val pathPDF11 =
            ContextWrapper(this@Export).externalMediaDirs[0].toString() + File.separator + "/LabApp/Useractivity/"
        val rootPDF11 = File(pathPDF11)
        fileNotWrite(root11)
        val filesAndFoldersPDF11 = rootPDF11.listFiles()


        uAdapter = UserDataAdapter(this@Export, reverseFileArray(filesAndFoldersPDF11))
        userRecyclerView.adapter = uAdapter
        uAdapter.notifyDataSetChanged()
        userRecyclerView.layoutManager = LinearLayoutManager(applicationContext)





        exportUserData.setOnClickListener {
            companyName = companyNameEditText.text.toString()

            try {
                generateUserActivityPDF()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }

            //                exportUserData();
            val path =
                ContextWrapper(this@Export).externalMediaDirs[0].toString() + File.separator + "/LabApp/Useractivity"
            val root = File(path)
            val pathPDF =
                ContextWrapper(this@Export).externalMediaDirs[0].toString() + File.separator + "/LabApp/Useractivity/"
            val rootPDF = File(pathPDF)
            fileNotWrite(root)
            val filesAndFoldersPDF = rootPDF.listFiles()
            uAdapter = UserDataAdapter(this@Export, reverseFileArray(filesAndFoldersPDF))
            userRecyclerView.adapter = uAdapter
            uAdapter.notifyDataSetChanged()
            userRecyclerView.layoutManager = LinearLayoutManager(applicationContext)
        }

        if (checkPermission()) {
            Toast.makeText(applicationContext, "Permission Granted", Toast.LENGTH_SHORT).show()
        } else {
            requestPermission()
        }

        if (Constants.OFFLINE_MODE) {
            val company_name = SharedPref.getSavedData(this@Export, "COMPANY_NAME")
            if (company_name != null) {
                companyNameEditText.setText(company_name)
            } else {
                companyNameEditText.setText("N/A")

            }
        }

        val comLo = getCompanyLogo()
        if (comLo != null) {
            companyLogo.setImageBitmap(comLo)
        }



        exportPDFAllLogDataBtn.setOnClickListener {
            companyName = companyNameEditText.text.toString()
            if (!companyName.isEmpty()) {
                if (Constants.OFFLINE_MODE) {
                    val company_name = getSharedPreferences("COMPANY_NAME", MODE_PRIVATE)
                    val editT = company_name.edit()
                    editT.putString("COMPANY_NAME", companyName)
                    editT.commit()
                }
            }
            try {
                generateAllLogPDFs()
//                generateAllLogCSV()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
            updateRecyclerView()
            convertToXls.visibility = View.INVISIBLE
        }

        binding.printSensorCSV.setOnClickListener {
            companyName = companyNameEditText.text.toString()
            if (!companyName.isEmpty()) {
                if (Constants.OFFLINE_MODE) {
                    val company_name = getSharedPreferences("COMPANY_NAME", MODE_PRIVATE)
                    val editT = company_name.edit()
                    editT.putString("COMPANY_NAME", companyName)
                    editT.commit()
                }
            }
            try {
//                generateAllLogPDFs()
                generateAllLogCSV()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
            updateRecyclerView()
            convertToXls.visibility = View.INVISIBLE
        }
        binding.printAllCalibData.visibility = View.VISIBLE
        exportPDFAllLogDataBtn.visibility = View.VISIBLE
        binding.printAllCalibDataCSV.visibility = View.VISIBLE
        binding.printSensorCSV.visibility = View.VISIBLE

        if (Source.EXPORT_CSV) {
            binding.printAllCalibDataCSV.visibility = View.VISIBLE
            binding.printSensorCSV.visibility = View.VISIBLE
        } else {
            binding.printAllCalibDataCSV.visibility = View.GONE
            binding.printSensorCSV.visibility = View.GONE
        }

        if (Source.EXPORT_PDF) {
            binding.printAllCalibData.visibility = View.VISIBLE
            exportPDFAllLogDataBtn.visibility = View.VISIBLE
        } else {
            binding.printAllCalibData.visibility = View.GONE
            exportPDFAllLogDataBtn.visibility = View.GONE
        }

        printAllCalibData.setOnClickListener { v: View? ->
            try {
                printAllCalibPDF()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
            updateRecyclerView()
            convertToXls.visibility = View.INVISIBLE
        }

        binding.printAllCalibDataCSV.setOnClickListener { v: View? ->
            try {
                printAllCalibCSV()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
            updateRecyclerView()
            convertToXls.visibility = View.INVISIBLE
        }
    }

    private fun updateRecyclerView() {
        val pathPDF =
            ContextWrapper(this@Export).externalMediaDirs[0].toString() + File.separator + "/LabApp/Sensordata/"
        val rootPDF = File(pathPDF)
        fileNotWrite(rootPDF)
        val filesAndFoldersPDF = rootPDF.listFiles() ?: return

        // Sort the files based on last modified timestamp in descending order
        val sortedFiles = filesAndFoldersPDF.sortedByDescending { it.lastModified() }

        // Find the first PDF file
        val pdfFile =
            sortedFiles.firstOrNull { it.name.endsWith(".pdf") || it.name.endsWith(".csv") }

        // Set up RecyclerView with adapter
        fAdapter = PDF_CSV_Adapter(applicationContext, sortedFiles.toTypedArray(), "PhExport")

        recyclerView.adapter = fAdapter
        fAdapter.notifyDataSetChanged()
        recyclerView.layoutManager = LinearLayoutManager(this@Export)
    }

    fun printAllCalibCSV() {
        Toast.makeText(this@Export, "Printing...", Toast.LENGTH_LONG).show()


        val exportDir = File(ContextWrapper(this@Export).externalMediaDirs[0], "/LabApp/Sensordata")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
        val currentDateandTime = sdf.format(Date())
        val tempPath =
            ContextWrapper(this@Export).externalMediaDirs[0].toString() + File.separator + "/LabApp/Sensordata"
        val tempRoot = File(tempPath)
        fileNotWrite(tempRoot)
        val tempFilesAndFolders = tempRoot.listFiles()
        val file = File(
            ContextWrapper(this@Export).externalMediaDirs[0],
            "/LabApp/Sensordata/AllCalibData_$currentDateandTime-${(tempFilesAndFolders?.size ?: 0) - 1}.csv"
        )

        try {
            val writer = CSVWriter(FileWriter(file))
            val headers = arrayOf(
                "Company Name",
                "Report Generated By",
                "Device ID",
                "Last Calibrated By",
                "Date",
                "Time",
                "Offset",
                "Temperature",
                "Battery",
                "Slope"
            )
            writer.writeNext(headers)


            var company_name1 = ""
            val companyname = SharedPref.getSavedData(this@Export, "COMPANY_NAME")
            company_name1 =
                if (!companyname.isNullOrEmpty()) "Company: $companyname" else "Company: N/A"

            val newOffset = if (SharedPref.getSavedData(
                    this@Export, "OFFSET_" + PhActivity.DEVICE_ID
                ) != null && SharedPref.getSavedData(
                    this@Export, "OFFSET_" + PhActivity.DEVICE_ID
                ) != ""
            ) {
                val data =
                    SharedPref.getSavedData(this@Export, "OFFSET_" + PhActivity.DEVICE_ID)
                "Offset: $data"
            } else {
                "Offset: " + "0"
            }

            val newSlope = if (SharedPref.getSavedData(
                    this@Export, "SLOPE_" + PhActivity.DEVICE_ID
                ) != null && SharedPref.getSavedData(
                    this@Export, "SLOPE_" + PhActivity.DEVICE_ID
                ) != ""
            ) {
                val data =
                    SharedPref.getSavedData(this@Export, "SLOPE_" + PhActivity.DEVICE_ID)
                "Slope: $data"
            } else {
                "Slope: " + "0"
            }

            val tempData =
                SharedPref.getSavedData(this@Export, "tempValue" + PhActivity.DEVICE_ID)

            val newTemp = if (SharedPref.getSavedData(
                    this@Export, "tempValue" + PhActivity.DEVICE_ID
                ) != null && SharedPref.getSavedData(
                    this@Export, "tempValue" + PhActivity.DEVICE_ID
                ) != ""
            ) {

                "Temperature: $tempData"
            } else {
                "Temperature: " + "0"
            }

            val batteryVal =
                SharedPref.getSavedData(this@Export, "battery" + PhActivity.DEVICE_ID)
            val newBattery = if (batteryVal != null && batteryVal != "") {
                "Battery: $batteryVal %"
            } else {
                "Battery: 0 %"

            }


            writer.writeNext(arrayOf(company_name1))
            if (Source.cfr_mode) {
                writer.writeNext(arrayOf("Username: ${Source.logUserName}"))
            }
            writer.writeNext(arrayOf("Device ID: ${PhActivity.DEVICE_ID}"))
            writer.writeNext(
                arrayOf(
                    "Date: ${Source.getPresentDate()}",
                    "Time: ${Source.getCurrentTime()}"
                )
            )
            writer.writeNext(arrayOf(newOffset, newSlope, newTemp, newBattery))
            writer.writeNext(arrayOf("", "", "", "", "", "", "", "", "", "", ""))
            writer.writeNext(arrayOf("", "", "", "", "", "", "", "", "", "")) // Blank row
            writer.writeNext(arrayOf("", "", "", "", "", "", "", "", "", "")) // Blank row
            writer.writeNext(arrayOf("", "", "", "", "", "", "", "", "", "")) // Blank row
            writer.writeNext(arrayOf("", "", "", "", "", "", "", "", "", "")) // Blank row

            writer.writeNext(arrayOf("All Calibration's Table"))
            val calibHeaders =
                arrayOf("pH", "pH After Calib", "Slope", "mV", "Date & Time", "Temperature")
            writer.writeNext(calibHeaders)
            var rowCounter = 0
            val db = databaseHelper.writableDatabase
            val calibCSV = db.rawQuery("SELECT * FROM CalibAllDataOffline", null)



            while (calibCSV.moveToNext()) {
                val ph = calibCSV.getString(calibCSV.getColumnIndex("PH")) ?: "--"
                val mv = calibCSV.getString(calibCSV.getColumnIndex("MV")) ?: "--"
                val date = calibCSV.getString(calibCSV.getColumnIndex("DT")) ?: "--"
                val slope = calibCSV.getString(calibCSV.getColumnIndex("SLOPE")) ?: "--"
                val pHAC = calibCSV.getString(calibCSV.getColumnIndex("pHAC")) ?: "--"
                val temperature1 =
                    calibCSV.getString(calibCSV.getColumnIndex("temperature")) ?: "--"
                if (ph == "calibration-ended") {
                    // Add the headers for the next set of rows
                    writer.writeNext(arrayOf("", "", "", "", "", "", "", "", "", "")) // Blank row
                    writer.writeNext(calibHeaders)
                } else {
                    val row = arrayOf(ph, pHAC, slope, mv, date, temperature1)
                    writer.writeNext(row)
                }


            }

            writer.close()
            Toast.makeText(this@Export, "CSV file exported", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(
                this@Export,
                "Error exporting CSV file: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            e.printStackTrace()
        }
    }

    @Throws(FileNotFoundException::class)
    private fun printAllCalibPDF() {
        Toast.makeText(this@Export, "Printing...", Toast.LENGTH_LONG).show()

        val company_name = "Company: $companyName"
        val user_name = "Report generated by: " + Source.logUserName
        val device_id = "DeviceID: $deviceID"
        val calib_by = "Last calibrated by: " + Source.calib_completed_by
        reportDate = "Date: " + SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        reportTime = "Time: " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val shp = getSharedPreferences("Extras", MODE_PRIVATE)
        offset = "Offset: " + shp.getString("offset", "")
        if (Constants.OFFLINE_DATA) {
            offset = if (SharedPref.getSavedData(
                    this@Export,
                    "OFFSET_" + PhActivity.DEVICE_ID
                ) != null && SharedPref.getSavedData(
                    this@Export,
                    "OFFSET_" + PhActivity.DEVICE_ID
                ) !== ""
            ) {
                val data = SharedPref.getSavedData(this@Export, "OFFSET_" + PhActivity.DEVICE_ID)
                "Offset: $data"
            } else {
                "Offset: " + "null"
            }
        } else {
        }
        temp = "Temperature: " + shp.getString("temp", "")
        battery = "Battery: " + shp.getString("battery", "")


        val leftDesignationString =
            SharedPref.getSavedData(this@Export, SharedKeys.LEFT_DESIGNATION_KEY)
        val rightDesignationString =
            SharedPref.getSavedData(
                this@Export, SharedKeys.RIGHT_DESIGNATION_KEY
            )

        if (leftDesignationString != null && leftDesignationString != "") {

        } else {
            SharedPref.saveData(
                this@Export,
                SharedKeys.LEFT_DESIGNATION_KEY,
                "Operator Sign"
            )
        }

        if (rightDesignationString != null && rightDesignationString != "") {
        } else {
            SharedPref.saveData(
                this@Export,
                SharedKeys.RIGHT_DESIGNATION_KEY,
                "Supervisor Sign"
            )
        }




        if (Constants.OFFLINE_DATA) {
            val slopeData = SharedPref.getSavedData(this@Export, "SLOPE_" + PhActivity.DEVICE_ID)

            slope = if (SharedPref.getSavedData(
                    this@Export,
                    "SLOPE_" + PhActivity.DEVICE_ID
                ) != null && SharedPref.getSavedData(
                    this@Export,
                    "SLOPE_" + PhActivity.DEVICE_ID
                ) !== ""
            ) {
                "Slope: $slopeData"
            } else {
                "Slope: " + "null"
            }
            val tempData = SharedPref.getSavedData(this@Export, "tempValue" + PhActivity.DEVICE_ID)

            temp = if (SharedPref.getSavedData(
                    this@Export,
                    "tempValue" + PhActivity.DEVICE_ID
                ) != null && SharedPref.getSavedData(
                    this@Export,
                    "tempValue" + PhActivity.DEVICE_ID
                ) !== ""
            ) {
                "Temperature: $tempData"
            } else {
                "Temperature: " + "null"
            }
            val batteryVal = SharedPref.getSavedData(this@Export, "battery" + PhActivity.DEVICE_ID)
            if (batteryVal != null) {
                if (batteryVal != "") {
                    battery = "Battery: $batteryVal %"
//                    binding.batteryPercent.text = "$batteryVal %"
                } else {
                    battery = "Battery: 0 %"

                }
            }

        } else {
            slope = "Slope: " + shp.getString("slope", "")
        }
        val exportDir =
            File(ContextWrapper(this@Export).externalMediaDirs[0].toString() + File.separator + "/LabApp/Sensordata")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
        val currentDateandTime = sdf.format(Date())
        val tempPath =
            ContextWrapper(this@Export).externalMediaDirs[0].toString() + File.separator + "/LabApp/Sensordata"
        val tempRoot = File(tempPath)
        fileNotWrite(tempRoot)
        val tempFilesAndFolders = tempRoot.listFiles()
        val file = File(
            ContextWrapper(this@Export).externalMediaDirs[0].toString() + File.separator + "/LabApp/Sensordata/AllCalibData_" + currentDateandTime + "_" + ((tempFilesAndFolders?.size
                ?: 0) - 1) + ".pdf"
        )
        val outputStream: OutputStream = FileOutputStream(file)
        val writer = PdfWriter(file)
        val pdfDocument = PdfDocument(writer)
        val document = Document(pdfDocument)
        val imgBit = getCompanyLogo()
        if (imgBit != null) {

            val byteArrayOutputStream = ByteArrayOutputStream()
            imgBit.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()

// Create ImageData from byte array
            val imageData = ImageDataFactory.create(byteArray)

// Create an Image element
            val image = Image(imageData).setHeight(80f).setWidth(80f)
            document.add(image)

        } else {
//                Toast.makeText(requireContext(), "Null", Toast.LENGTH_SHORT).show()
        }

//        Text text = new Text(company_name);
//        Text text1 = new Text(user_name);
//        Text text2 = new Text(device_id);
//
//
//
//        document.add(new Paragraph(text).add(text1).add(text2));
        if (Source.cfr_mode) {

            document.add(
                Paragraph(
                    """
            $company_name
            $calib_by
            $user_name
            $device_id
            """.trimIndent()
                )
            )
        } else {
            document.add(
                Paragraph(
                    """
            $company_name
            $device_id
            """.trimIndent()
                )
            )
        }
        document.add(Paragraph(""))
        document.add(
            Paragraph(
                """$reportDate  |  $reportTime
                        $offset  |  $battery
                        $slope  |  $temp"""
            )
        )
        document.add(Paragraph(""))
        document.add(Paragraph("Calibration Table"))
        val db = databaseHelper.writableDatabase
        var calibCSV: Cursor? = null
        calibCSV = db.rawQuery("SELECT * FROM CalibAllDataOffline", null)
//            calibCSV = if (startDateString != null) {
//                db.rawQuery(
//                    "SELECT * FROM CalibAllDataOffline WHERE (DATE(date) BETWEEN '$startDateString' AND '$endDateString') AND (time BETWEEN '$startTimeString' AND '$endTimeString')",
//                    null
//                )
//                //            curCSV = db.rawQuery("SELECT * FROM LogUserdetails WHERE (DATE(date) BETWEEN '" + startDateString + "' AND '" + endDateString + "')", null);
//            } else {
//                db.rawQuery("SELECT * FROM CalibAllDataOffline", null)
//            }

        if (startDateString != "" || endDateString != "" || startTimeString != "" || endTimeString != "") {


            if (startDateString != "" && endDateString != "" && startTimeString != "" && endTimeString != "") {
                calibCSV = db.rawQuery(
                    "SELECT * FROM CalibAllDataOffline WHERE (DATE(date) BETWEEN '$startDateString' AND '$endDateString') AND (time BETWEEN '$startTimeString' AND '$endTimeString')",
                    null
                )
            } else if (startDateString != "" && endDateString != "") {
                calibCSV = db.rawQuery(
                    "SELECT * FROM CalibAllDataOffline WHERE (DATE(date) BETWEEN '$startDateString' AND '$endDateString')",
                    null
                )
            } else {
                calibCSV = db.rawQuery("SELECT * FROM CalibAllDataOffline", null)

            }


        } else {
            calibCSV = db.rawQuery("SELECT * FROM CalibAllDataOffline", null)

        }

        val so = 1
        val columnWidth = floatArrayOf(200f, 210f, 190f, 170f, 340f, 170f)
        var table = Table(columnWidth)
        table.addCell("pH")
        table.addCell("pH Aft Calib")
        table.addCell("Slope")
        table.addCell("mV")
        table.addCell("Date & Time")
        table.addCell("Temperature")
        var rowCounter = 0 // To keep track of the number of rows processed
        runOnUiThread {
            Toast.makeText(
                this@Export,
                "" + calibCSV.count + ", " + calibCSV.columnCount,
                Toast.LENGTH_SHORT
            ).show()
        }
        while (calibCSV.moveToNext()) {
            val ph = calibCSV.getString(calibCSV.getColumnIndex("PH")) ?: ""
            if (ph == "calibration-ended") {
                // Start a new table
                if (!table.isEmpty) {
                    document.add(table)
                }
                document.add(Paragraph("Calibration: completed"))
                if (leftDesignationString != null && leftDesignationString != "" &&
                    rightDesignationString != null && rightDesignationString != ""
                ) {
                    document.add(Paragraph("$leftDesignationString                                                                                      $rightDesignationString"))
                } else {
                    document.add(Paragraph("Operator Sign                                                                                      Supervisor Sign"))
                }
                document.add(Paragraph(""))
                document.add(Paragraph(""))
                document.add(Paragraph(""))
                document.add(Paragraph(""))
                document.add(Paragraph(""))
                document.add(Paragraph(""))

                // Create a new table for the next set of data
                table = Table(columnWidth)
                table.addCell("pH")
                table.addCell("pH Aft Calib")
                table.addCell("Slope")
                table.addCell("mV")
                table.addCell("Date & Time")
                table.addCell("Temperature")
            } else {
                // Add data to the current table
                val mv = calibCSV.getString(calibCSV.getColumnIndex("MV")) ?: ""
                val date = calibCSV.getString(calibCSV.getColumnIndex("DT")) ?: ""
                val slope = calibCSV.getString(calibCSV.getColumnIndex("SLOPE")) ?: ""
                val pHAC = calibCSV.getString(calibCSV.getColumnIndex("pHAC")) ?: ""
                val temperature1 = calibCSV.getString(calibCSV.getColumnIndex("temperature")) ?: ""
                table.addCell(ph)
                table.addCell(pHAC)
                table.addCell(slope)
                table.addCell(mv)
                table.addCell(date)
                table.addCell(temperature1)
            }
        }

// Add the last table if there are remaining rows
        if (!table.isEmpty) {
            document.add(table)
        }
        if (Constants.OFFLINE_DATA) {
            if (SharedPref.getSavedData(
                    this@Export, "CALIB_STAT" + PhActivity.DEVICE_ID
                ) != null && SharedPref.getSavedData(
                    this@Export, "CALIB_STAT" + PhActivity.DEVICE_ID
                ) !== ""
            ) {
                val calibSTat = SharedPref.getSavedData(
                    this@Export, "CALIB_STAT" + PhActivity.DEVICE_ID
                )
                //                document.add(new Paragraph("Calibration : " + calibSTat));
            } else {
//                document.add(new Paragraph("Calibration : " + calib_stat));
            }
        } else {
            document.add(Paragraph("Calibration : $calib_stat"))
        }

//        document.add(new Paragraph("Operator Sign                                                                                      Supervisor Sign"));
        val imgBit1 = getSignImage()

        if (imgBit1 != null) {

            val byteArrayOutputStream = ByteArrayOutputStream()
            imgBit1.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()

// Create ImageData from byte array
            val imageData = ImageDataFactory.create(byteArray)

// Create an Image element
            val image = Image(imageData).setHeight(80f).setWidth(80f)
            document.add(image)

        } else {
//                Toast.makeText(requireContext(), "Null", Toast.LENGTH_SHORT).show()
        }
        document.close()
        Toast.makeText(this, "Pdf generated", Toast.LENGTH_SHORT).show()
    }

    private fun showOptionDialog() {
        val dialog = Dialog(this@Export)
        dialog.setContentView(R.layout.img_options_dialog)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(true)
        dialog.findViewById<View>(R.id.gallery).setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE)
            dialog.dismiss()
        }
        dialog.findViewById<View>(R.id.camera).setOnClickListener {
            if (checkPermissions()) {
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(cameraIntent, CAMERA_REQUEST)
                dialog.dismiss()
            } else {
                Toast.makeText(this@Export, "Camera permission required", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            val photo = data!!.extras!!["data"] as Bitmap?
            saveImage(photo)
            companyLogo.setImageBitmap(photo)
            companyLogo.visibility = View.VISIBLE
            //            selectCompanyLogo.setText("Ok!");
//            selectCompanyLogo.setEnabled(false);
        }
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            if (data != null) {
                val picUri = data.data //<- get Uri here from data intent
                if (picUri != null) {
//                    Bitmap photo = (Bitmap) data.getExtras().get("data");
                    var photo: Bitmap? = null
                    try {
                        photo = MediaStore.Images.Media.getBitmap(
                            this.contentResolver,
                            picUri
                        )
                        saveImage(photo)
                        companyLogo.setImageBitmap(photo)
                        companyLogo.visibility = View.VISIBLE
                        //                        selectCompanyLogo.setText("Ok!");
                    } catch (e: FileNotFoundException) {
                        throw RuntimeException(e)
                    } catch (e: IOException) {
                        throw RuntimeException(e)
                    }
                }
            }
        }
    }

    private fun saveImage(realImage: Bitmap?) {
        val baos = ByteArrayOutputStream()
        realImage!!.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val b = baos.toByteArray()
        val encodedImage = Base64.encodeToString(b, Base64.DEFAULT)
        val shre = getSharedPreferences("logo", MODE_PRIVATE)
        val edit = shre.edit()
        edit.putString("logo_data", encodedImage)
        edit.commit()
    }

    fun getCompanyLogo(): Bitmap? {
        val sh = this@Export.getSharedPreferences("logo", Context.MODE_PRIVATE)
        val photo = sh.getString("logo_data", "")
        var bitmap: Bitmap? = null
        if (!photo.equals("", ignoreCase = true)) {
            val b: ByteArray = Base64.decode(photo, Base64.DEFAULT)
            bitmap = BitmapFactory.decodeByteArray(b, 0, b.size)
        }
        return bitmap
    }

    private fun getSignImage(): Bitmap? {
        val sh = getSharedPreferences("signature", MODE_PRIVATE)
        val photo = sh.getString("signature_data", "")
        var bitmap: Bitmap? = null
        if (!photo.equals("", ignoreCase = true)) {
            val b = Base64.decode(photo, Base64.DEFAULT)
            bitmap = BitmapFactory.decodeByteArray(b, 0, b.size)
        }
        return bitmap
    }

    fun reverseFileArray(fileArray: Array<File>?): Array<File>? {
        if (fileArray != null) {
            for (i in 0 until fileArray.size / 2) {
                val a = fileArray[i]
                fileArray[i] = fileArray[fileArray.size - i - 1]
                fileArray[fileArray.size - i - 1] = a
            }
        }
        return fileArray
    }

    private fun stringSplitter(str: String): String? {
        var newText = ""
        val strings = Splitter.fixedLength(8).split(str)
        for (temp in strings) {
            newText = "$newText $temp"
        }
        return newText.trim { it <= ' ' }
    }

    fun getPath(uri: Uri?): String {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = managedQuery(uri, projection, null, null, null)
        val column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor.moveToFirst()
        return cursor.getString(column_index)
    }

    lateinit var allLogsDataDao: AllLogsDataDao
    lateinit var userActionDao: UserActionDao

    override fun onResume() {
        super.onResume()
        allLogsDataDao = Room.databaseBuilder(
            this@Export.applicationContext, AppDatabase::class.java, "aican-database"
        ).build().allLogsDao()

        userActionDao = Room.databaseBuilder(
            this@Export.applicationContext, AppDatabase::class.java, "aican-database"
        ).build().userActionDao()

    }

    fun generateAllLogCSV() {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
            val currentDateandTime = sdf.format(Date())
            val tempPath =
                File(this@Export.externalMediaDirs[0], "/LabApp/Sensordata")
            tempPath.mkdirs()

            val filePath =
                File(tempPath, "DSL_$currentDateandTime-${tempPath.listFiles()?.size ?: 0}.csv")
            val writer = CSVWriter(FileWriter(filePath))

            val db = databaseHelper.writableDatabase

            var company_name1 = ""
            val companyname = SharedPref.getSavedData(this@Export, "COMPANY_NAME")
            company_name1 =
                if (!companyname.isNullOrEmpty()) "Company: $companyname" else "Company: N/A"

            val newOffset = if (SharedPref.getSavedData(
                    this@Export, "OFFSET_" + PhActivity.DEVICE_ID
                ) != null && SharedPref.getSavedData(
                    this@Export, "OFFSET_" + PhActivity.DEVICE_ID
                ) != ""
            ) {
                val data =
                    SharedPref.getSavedData(this@Export, "OFFSET_" + PhActivity.DEVICE_ID)
                "Offset: $data"
            } else {
                "Offset: " + "0"
            }

            val newSlope = if (SharedPref.getSavedData(
                    this@Export, "SLOPE_" + PhActivity.DEVICE_ID
                ) != null && SharedPref.getSavedData(
                    this@Export, "SLOPE_" + PhActivity.DEVICE_ID
                ) != ""
            ) {
                val data =
                    SharedPref.getSavedData(this@Export, "SLOPE_" + PhActivity.DEVICE_ID)
                "Slope: $data"
            } else {
                "Slope: " + "0"
            }

            val tempData =
                SharedPref.getSavedData(this@Export, "tempValue" + PhActivity.DEVICE_ID)

            val newTemp = if (SharedPref.getSavedData(
                    this@Export, "tempValue" + PhActivity.DEVICE_ID
                ) != null && SharedPref.getSavedData(
                    this@Export, "tempValue" + PhActivity.DEVICE_ID
                ) != ""
            ) {

                "Temperature: $tempData"
            } else {
                "Temperature: " + "0"
            }

            val batteryVal =
                SharedPref.getSavedData(this@Export, "battery" + PhActivity.DEVICE_ID)
            val newBattery = if (batteryVal != null && batteryVal != "") {
                "Battery: $batteryVal %"
            } else {
                "Battery: 0 %"

            }


            writer.writeNext(arrayOf(company_name1))
            if (Source.cfr_mode) {
                writer.writeNext(arrayOf("Username: ${Source.logUserName}"))
            }
            writer.writeNext(arrayOf("Device ID: ${PhActivity.DEVICE_ID}"))
            writer.writeNext(
                arrayOf(
                    "Date: ${Source.getPresentDate()}",
                    "Time: ${Source.getCurrentTime()}"
                )
            )
            writer.writeNext(arrayOf(newOffset, newSlope, newTemp, newBattery))
            writer.writeNext(arrayOf("", "", "", "", "", "", "", "", "", "", ""))
            writer.writeNext(arrayOf("", "", "", "", "", "", "", "", "", "", ""))
            writer.writeNext(arrayOf("", "", "", "", "", "", "", "", "", "", ""))

            writer.writeNext(arrayOf("Calibration Table", "", "", "", "", "", "", "", ""))
            writer.writeNext(arrayOf("", "", "", "", "", "", "", "", "", "", ""))

            writer.writeNext(
                arrayOf(
                    "pH",
                    "pH After Cal",
                    "Slope",
                    "mV",
                    "Date & Time",
                    "Temperature"
                )
            )

            var calibCSV: Cursor? = null
            if (Constants.OFFLINE_MODE) {
                calibCSV = if (Source.calibMode == 0) {
                    db.rawQuery("SELECT * FROM CalibOfflineDataFive", null)
                } else {
                    db.rawQuery("SELECT * FROM CalibOfflineDataFive", null)
                }
            } else {
                calibCSV = db.rawQuery("SELECT * FROM CalibData", null)
            }

            while (calibCSV != null && calibCSV.moveToNext()) {
                val ph = calibCSV.getString(calibCSV.getColumnIndex("PH")) ?: "--"
                val pHAC = calibCSV.getString(calibCSV.getColumnIndex("pHAC")) ?: "--"
                val slope = calibCSV.getString(calibCSV.getColumnIndex("SLOPE")) ?: "--"
                val mv = calibCSV.getString(calibCSV.getColumnIndex("MV")) ?: "--"
                val date = calibCSV.getString(calibCSV.getColumnIndex("DT")) ?: "--"
                val temperature1 =
                    calibCSV.getString(calibCSV.getColumnIndex("temperature")) ?: "--"

                writer.writeNext(arrayOf(ph, pHAC, slope, mv, date, temperature1))
            }

            writer.writeNext(arrayOf("", "", "", "", "", "", "", "", "", "", ""))
            writer.writeNext(arrayOf("", "", "", "", "", "", "", "", "", "", ""))
            writer.writeNext(arrayOf("", "", "", "", "", "", "", "", "", "", ""))
            writer.writeNext(arrayOf("", "", "", "", "", "", "", "", "", "", ""))
            writer.writeNext(arrayOf("All Log Data", "", "", "", "", "", "", "", "", "", ""))
            writer.writeNext(arrayOf("", "", "", "", "", "", "", "", "", "", ""))



            GlobalScope.launch(Dispatchers.Main) {
//                val allLogsArrayList = withContext(Dispatchers.IO) {
//                    allLogsDataDao.getAllLogs()
//                }

                var allLogsArrayList: List<AllLogsEntity>? = null

                if (startDateString != "" && endDateString != "" && startTimeString != "" && endTimeString != "") {
                    if (arNumEditText.text.toString()
                            .isNotEmpty() || batchNumEditText.text.toString()
                            .isNotEmpty()
                        || compoundNameEditText.text.toString().isNotEmpty()
                    ) {

                        if (arNumEditText.text.toString()
                                .isNotEmpty() && batchNumEditText.text.toString()
                                .isNotEmpty() && compoundNameEditText.text.toString().isNotEmpty()
                        ) {
                            allLogsArrayList = withContext(Dispatchers.IO) {
                                allLogsDataDao.getLogByBAC_DNT(
                                    startDateString,
                                    endDateString,
                                    startTimeString,
                                    endTimeString,
                                    arNumEditText.text.toString(),
                                    batchNumEditText.text.toString(),
                                    compoundNameEditText.text.toString()
                                )
                            }
                        } else if (arNumEditText.text.toString()
                                .isNotEmpty() && batchNumEditText.text.toString().isNotEmpty()
                        ) {
                            allLogsArrayList = withContext(Dispatchers.IO) {
                                allLogsDataDao.getLogByArnumAndBatchnum_DNT(
                                    startDateString, endDateString, startTimeString, endTimeString,
                                    arNumEditText.text.toString(),
                                    batchNumEditText.text.toString()
                                )
                            }
                        } else if (arNumEditText.text.toString()
                                .isNotEmpty() && compoundNameEditText.text.toString().isNotEmpty()
                        ) {
                            allLogsArrayList = withContext(Dispatchers.IO) {

                                allLogsDataDao.getLogByArnumAndProduct_DNT(
                                    startDateString, endDateString, startTimeString, endTimeString,
                                    arNumEditText.text.toString(),
                                    compoundNameEditText.text.toString()
                                )
                            }
                        } else if (batchNumEditText.text.toString()
                                .isNotEmpty() && compoundNameEditText.text.toString().isNotEmpty()
                        ) {
                            allLogsArrayList = withContext(Dispatchers.IO) {

                                allLogsDataDao.getLogByBatchnumAndProduct_DNT(
                                    startDateString, endDateString, startTimeString, endTimeString,
                                    batchNumEditText.text.toString(),
                                    compoundNameEditText.text.toString()
                                )
                            }
                        } else if (arNumEditText.text.toString().isNotEmpty()) {
                            allLogsArrayList = withContext(Dispatchers.IO) {
                                allLogsDataDao.getLogByArnum_DNT(
                                    startDateString, endDateString, startTimeString, endTimeString,
                                    arNumEditText.text.toString(),
                                )
                            }
                        } else if (batchNumEditText.text.toString().isNotEmpty()) {
                            allLogsArrayList = withContext(Dispatchers.IO) {
                                allLogsDataDao.getLogByBatchnum_DNT(
                                    startDateString, endDateString, startTimeString, endTimeString,
                                    batchNumEditText.text.toString(),
                                )
                            }
                        } else if (compoundNameEditText.text.toString().isNotEmpty()) {
                            allLogsArrayList = withContext(Dispatchers.IO) {
                                allLogsDataDao.getLogByProduct_DNT(
                                    startDateString, endDateString, startTimeString, endTimeString,
                                    compoundNameEditText.text.toString(),
                                )
                            }
                        }


                    } else {
                        allLogsArrayList = withContext(Dispatchers.IO) {
                            allLogsDataDao.getAllLogBy_DNT(
                                startDateString, endDateString, startTimeString, endTimeString,
                            )
                        }
                    }
                } else if (startDateString != "" && endDateString != "") {
                    if (arNumEditText.text.toString()
                            .isNotEmpty() || batchNumEditText.text.toString()
                            .isNotEmpty()
                        || compoundNameEditText.text.toString().isNotEmpty()
                    ) {

                        if (arNumEditText.text.toString()
                                .isNotEmpty() && batchNumEditText.text.toString()
                                .isNotEmpty() && compoundNameEditText.text.toString().isNotEmpty()
                        ) {
                            allLogsArrayList = withContext(Dispatchers.IO) {
                                allLogsDataDao.getLogByBAC_DNT(
                                    startDateString,
                                    endDateString,
                                    startTimeString,
                                    endTimeString,
                                    arNumEditText.text.toString(),
                                    batchNumEditText.text.toString(),
                                    compoundNameEditText.text.toString()
                                )
                            }
                        } else if (arNumEditText.text.toString()
                                .isNotEmpty() && batchNumEditText.text.toString().isNotEmpty()
                        ) {
                            allLogsArrayList = withContext(Dispatchers.IO) {
                                allLogsDataDao.getLogByArnumAndBatchnum_Date(
                                    startDateString, endDateString,
                                    arNumEditText.text.toString(),
                                    batchNumEditText.text.toString()
                                )
                            }
                        } else if (arNumEditText.text.toString()
                                .isNotEmpty() && compoundNameEditText.text.toString().isNotEmpty()
                        ) {
                            allLogsArrayList = withContext(Dispatchers.IO) {

                                allLogsDataDao.getLogByArnumAndProduct_Date(
                                    startDateString, endDateString,
                                    arNumEditText.text.toString(),
                                    compoundNameEditText.text.toString()
                                )
                            }
                        } else if (batchNumEditText.text.toString()
                                .isNotEmpty() && compoundNameEditText.text.toString().isNotEmpty()
                        ) {
                            allLogsArrayList = withContext(Dispatchers.IO) {

                                allLogsDataDao.getLogByBatchnumAndProduct_Date(
                                    startDateString, endDateString,
                                    batchNumEditText.text.toString(),
                                    compoundNameEditText.text.toString()
                                )
                            }
                        } else if (arNumEditText.text.toString().isNotEmpty()) {
                            allLogsArrayList = withContext(Dispatchers.IO) {
                                allLogsDataDao.getLogByArnum_Date(
                                    startDateString, endDateString,
                                    arNumEditText.text.toString(),
                                )
                            }
                        } else if (batchNumEditText.text.toString().isNotEmpty()) {
                            allLogsArrayList = withContext(Dispatchers.IO) {
                                allLogsDataDao.getLogByBatchnum_Date(
                                    startDateString, endDateString,
                                    batchNumEditText.text.toString(),
                                )
                            }
                        } else if (compoundNameEditText.text.toString().isNotEmpty()) {
                            allLogsArrayList = withContext(Dispatchers.IO) {
                                allLogsDataDao.getLogByProduct_Date(
                                    startDateString, endDateString,
                                    compoundNameEditText.text.toString(),
                                )
                            }
                        }


                    } else {
                        Log.e("NothingNoDate", "Ok")
                        allLogsArrayList = withContext(Dispatchers.IO) {
                            allLogsDataDao.getAllLogBy_Date(
                                startDateString, endDateString
                            )
                        }
                    }
                } else {
                    if (arNumEditText.text.toString()
                            .isNotEmpty() || batchNumEditText.text.toString()
                            .isNotEmpty()
                        || compoundNameEditText.text.toString().isNotEmpty()
                    ) {

                        if (arNumEditText.text.toString()
                                .isNotEmpty() && batchNumEditText.text.toString()
                                .isNotEmpty() && compoundNameEditText.text.toString().isNotEmpty()
                        ) {
                            allLogsArrayList = withContext(Dispatchers.IO) {
                                allLogsDataDao.getLogByBAC(
                                    arNumEditText.text.toString(),
                                    batchNumEditText.text.toString(),
                                    compoundNameEditText.text.toString()
                                )
                            }
                        } else if (arNumEditText.text.toString()
                                .isNotEmpty() && batchNumEditText.text.toString().isNotEmpty()
                        ) {
                            allLogsArrayList = withContext(Dispatchers.IO) {
                                allLogsDataDao.getLogByArnumAndBatchnum(
                                    arNumEditText.text.toString(),
                                    batchNumEditText.text.toString()
                                )
                            }
                        } else if (arNumEditText.text.toString()
                                .isNotEmpty() && compoundNameEditText.text.toString().isNotEmpty()
                        ) {
                            allLogsArrayList = withContext(Dispatchers.IO) {

                                allLogsDataDao.getLogByArnumAndProduct(
                                    arNumEditText.text.toString(),
                                    compoundNameEditText.text.toString()
                                )
                            }
                        } else if (batchNumEditText.text.toString()
                                .isNotEmpty() && compoundNameEditText.text.toString().isNotEmpty()
                        ) {
                            allLogsArrayList = withContext(Dispatchers.IO) {

                                allLogsDataDao.getLogByBatchnumAndProduct(
                                    batchNumEditText.text.toString(),
                                    compoundNameEditText.text.toString()
                                )
                            }
                        } else if (arNumEditText.text.toString().isNotEmpty()) {
                            allLogsArrayList = withContext(Dispatchers.IO) {
                                allLogsDataDao.getLogByArnum(
                                    arNumEditText.text.toString(),
                                )
                            }
                        } else if (batchNumEditText.text.toString().isNotEmpty()) {
                            allLogsArrayList = withContext(Dispatchers.IO) {
                                allLogsDataDao.getLogByBatchnum(
                                    batchNumEditText.text.toString(),
                                )
                            }
                        } else if (compoundNameEditText.text.toString().isNotEmpty()) {
                            allLogsArrayList = withContext(Dispatchers.IO) {
                                allLogsDataDao.getLogByProduct(
                                    compoundNameEditText.text.toString(),
                                )
                            }
                        }


                    } else {
                        allLogsArrayList = withContext(Dispatchers.IO) {
                            allLogsDataDao.getAllLogs()
                        }
                    }
                }


                if (allLogsArrayList != null) {
                    Toast.makeText(this@Export, "" + allLogsArrayList.size, Toast.LENGTH_SHORT)
                        .show()

                    val headers = arrayOf(
                        "Date",
                        "Time",
                        "pH",
                        "Temperature",
                        "Batch No",
                        "AR No",
                        "Product"
                    )
                    writer.writeNext(headers)

                    for (logs in allLogsArrayList) {
                        val date = logs.date
                        val time = logs.time
                        val pH = logs.ph ?: "--"
                        val temp = logs.temperature ?: "--"
                        val batchnum = logs.batchnum ?: "--"
                        val arnum = logs.arnum ?: "--"
                        val comp = logs.compound ?: "--"

                        val row = arrayOf(date, time, pH, temp, batchnum, arnum, comp)
                        writer.writeNext(row)
                    }

                    Toast.makeText(this@Export, "CSV file exported", Toast.LENGTH_SHORT).show()


                }
                writer.close()
            }

        } catch (e: IOException) {
            Toast.makeText(
                this@Export,
                "Error exporting CSV file: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            e.printStackTrace()

        }
    }

    @Throws(FileNotFoundException::class)
    fun generateAllLogPDFs() {

        Toast.makeText(this@Export, "Printing...", Toast.LENGTH_LONG).show()


        val device_id = "DeviceID: ${PhActivity.DEVICE_ID}"
        companyName = "" + companyNameEditText.text.toString()
        reportDate = "Date: " + SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        reportTime = "Time: " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val shp = getSharedPreferences("Extras", MODE_PRIVATE)
        offset = "Offset: " + shp.getString("offset", "")
        if (Constants.OFFLINE_DATA) {
            val offsetData = SharedPref.getSavedData(this@Export, "OFFSET_" + PhActivity.DEVICE_ID)

            if (SharedPref.getSavedData(
                    this@Export,
                    "OFFSET_" + PhActivity.DEVICE_ID
                ) != null && SharedPref.getSavedData(
                    this@Export,
                    "OFFSET_" + PhActivity.DEVICE_ID
                ) !== ""
            ) {
                offset = "Offset: $offsetData"
            } else {
                offset = "Offset: " + "null"
            }
        } else {
        }
        temp = "Temperature: " + shp.getString("temp", "")
        battery = "Battery: " + shp.getString("battery", "")
        if (Constants.OFFLINE_DATA) {
            val slopeData = SharedPref.getSavedData(this@Export, "SLOPE_" + PhActivity.DEVICE_ID)

            if (SharedPref.getSavedData(
                    this@Export,
                    "SLOPE_" + PhActivity.DEVICE_ID
                ) != null && SharedPref.getSavedData(
                    this@Export,
                    "SLOPE_" + PhActivity.DEVICE_ID
                ) !== ""
            ) {
                slope = "Slope: $slopeData"
            } else {
                slope = "Slope: " + "null"
            }
            val tempData = SharedPref.getSavedData(this@Export, "tempValue" + PhActivity.DEVICE_ID)

            if (SharedPref.getSavedData(
                    this@Export,
                    "tempValue" + PhActivity.DEVICE_ID
                ) != null && SharedPref.getSavedData(
                    this@Export,
                    "tempValue" + PhActivity.DEVICE_ID
                ) !== ""
            ) {
                temp = "Temperature: $tempData"
            } else {
                temp = "Temperature: " + "null"
            }
            val batteryVal = SharedPref.getSavedData(this@Export, "battery" + PhActivity.DEVICE_ID)
            if (batteryVal != null) {
                if (batteryVal != "") {
                    battery = "Battery: $batteryVal %"
//                    binding.batteryPercent.text = "$batteryVal %"
                } else {
                    battery = "Battery: 0 %"

                }
            }

        } else {
            slope = "Slope: " + shp.getString("slope", "")
        }
        roleExport = "Made By: " + Source.logUserName
        val exportDir =
            File(ContextWrapper(this@Export).externalMediaDirs[0].toString() + File.separator + "/LabApp/Sensordata")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
        val currentDateandTime = sdf.format(Date())
        val tempPath =
            ContextWrapper(this@Export).externalMediaDirs[0].toString() + File.separator + "/LabApp/Sensordata"
        val tempRoot = File(tempPath)
        fileNotWrite(tempRoot)
        val tempFilesAndFolders = tempRoot.listFiles()
        val file = File(
            ContextWrapper(this@Export).externalMediaDirs[0].toString() + File.separator + "/LabApp/Sensordata/DSL_" + currentDateandTime + "_" + ((tempFilesAndFolders?.size
                ?: 0) - 1) + ".pdf"
        )
        val outputStream: OutputStream = FileOutputStream(file)
        val writer = PdfWriter(file)
        val pdfDocument = PdfDocument(writer)
        val document = Document(pdfDocument)

//        float[] columnWidth12 = {150, 400, 400};
//        Table table12 = new Table(columnWidth12);
        val imgBit = getCompanyLogo()
        if (imgBit != null) {

            val byteArrayOutputStream = ByteArrayOutputStream()
            imgBit.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()

// Create ImageData from byte array
            val imageData = ImageDataFactory.create(byteArray)

// Create an Image element
            val image = Image(imageData).setHeight(80f).setWidth(80f)
            document.add(image)

        } else {
//                Toast.makeText(requireContext(), "Null", Toast.LENGTH_SHORT).show()
        }
        //
//        table12.addCell(new Paragraph(companyName));
//        table12.setBorder(Border.NO_BORDER);
//
//        document.add(table12);
        if (Constants.OFFLINE_MODE) {
//            document.add(new Paragraph("Offline Mode"));
        }
        if (Source.cfr_mode) {

            document.add(Paragraph(companyName + "\n" + roleExport + "\n" + device_id))
        } else {
            document.add(Paragraph(companyName + "\n" + device_id))

        }
        document.add(Paragraph(""))
        document.add(
            Paragraph(
                reportDate
                        + "  |  " + reportTime + "\n" +
                        offset + "  |  " + battery + "\n" + slope + "  |  " + temp
            )
        )
        document.add(Paragraph(""))
        document.add(Paragraph("Calibration Table"))
        val columnWidth = floatArrayOf(200f, 210f, 190f, 170f, 340f, 170f)
        val table = Table(columnWidth)
        table.addCell("pH")
        table.addCell("pH Aft Calib")
        table.addCell("Slope")
        table.addCell("mV")
        table.addCell("Date & Time")
        table.addCell("Temperature")
        val db = databaseHelper.writableDatabase
        var calibCSV: Cursor? = null
        if (Constants.OFFLINE_MODE) {
//            calibCSV = db.rawQuery("SELECT * FROM CalibOfflineData", null);
            if (Source.calibMode === 0) {
                calibCSV = db.rawQuery("SELECT * FROM CalibOfflineDataFive", null)
            }
            if (Source.calibMode === 1) {
                calibCSV = db.rawQuery("SELECT * FROM CalibOfflineDataThree", null)
            }
        } else {
            calibCSV = db.rawQuery("SELECT * FROM CalibData", null)
        }
        while (calibCSV != null && calibCSV.moveToNext()) {
            val ph = calibCSV.getString(calibCSV.getColumnIndex("PH"))
            val mv = calibCSV.getString(calibCSV.getColumnIndex("MV"))
            val date = calibCSV.getString(calibCSV.getColumnIndex("DT"))
            val slope = calibCSV.getString(calibCSV.getColumnIndex("SLOPE"))
            val pHAC = calibCSV.getString(calibCSV.getColumnIndex("pHAC"))
            val temperature1 = calibCSV.getString(calibCSV.getColumnIndex("temperature"))
            table.addCell(ph)
            table.addCell(pHAC + "")
            table.addCell(slope + "")
            table.addCell(mv)
            table.addCell(date)
            table.addCell(temperature1)
        }
        document.add(table)
        document.add(Paragraph(""))
        document.add(Paragraph("Log Table"))
        val columnWidth1 = floatArrayOf(210f, 120f, 170f, 150f, 350f, 350f, 250f)
        val table1 = Table(columnWidth1)
        table1.addCell("Date")
        table1.addCell("Time")
        table1.addCell("pH")
        table1.addCell("Temp")
        table1.addCell("Batch No")
        table1.addCell("AR No")
        table1.addCell("Product")

        GlobalScope.launch(Dispatchers.Main) {
            var allLogsArrayList: List<AllLogsEntity>? = null

            if (startDateString != "" && endDateString != "" && startTimeString != "" && endTimeString != "") {
                if (arNumEditText.text.toString().isNotEmpty() || batchNumEditText.text.toString()
                        .isNotEmpty()
                    || compoundNameEditText.text.toString().isNotEmpty()
                ) {

                    if (arNumEditText.text.toString()
                            .isNotEmpty() && batchNumEditText.text.toString()
                            .isNotEmpty() && compoundNameEditText.text.toString().isNotEmpty()
                    ) {
                        allLogsArrayList = withContext(Dispatchers.IO) {
                            allLogsDataDao.getLogByBAC_DNT(
                                startDateString,
                                endDateString,
                                startTimeString,
                                endTimeString,
                                arNumEditText.text.toString(),
                                batchNumEditText.text.toString(),
                                compoundNameEditText.text.toString()
                            )
                        }
                    } else if (arNumEditText.text.toString()
                            .isNotEmpty() && batchNumEditText.text.toString().isNotEmpty()
                    ) {
                        allLogsArrayList = withContext(Dispatchers.IO) {
                            allLogsDataDao.getLogByArnumAndBatchnum_DNT(
                                startDateString, endDateString, startTimeString, endTimeString,
                                arNumEditText.text.toString(),
                                batchNumEditText.text.toString()
                            )
                        }
                    } else if (arNumEditText.text.toString()
                            .isNotEmpty() && compoundNameEditText.text.toString().isNotEmpty()
                    ) {
                        allLogsArrayList = withContext(Dispatchers.IO) {

                            allLogsDataDao.getLogByArnumAndProduct_DNT(
                                startDateString, endDateString, startTimeString, endTimeString,
                                arNumEditText.text.toString(),
                                compoundNameEditText.text.toString()
                            )
                        }
                    } else if (batchNumEditText.text.toString()
                            .isNotEmpty() && compoundNameEditText.text.toString().isNotEmpty()
                    ) {
                        allLogsArrayList = withContext(Dispatchers.IO) {

                            allLogsDataDao.getLogByBatchnumAndProduct_DNT(
                                startDateString, endDateString, startTimeString, endTimeString,
                                batchNumEditText.text.toString(),
                                compoundNameEditText.text.toString()
                            )
                        }
                    } else if (arNumEditText.text.toString().isNotEmpty()) {
                        allLogsArrayList = withContext(Dispatchers.IO) {
                            allLogsDataDao.getLogByArnum_DNT(
                                startDateString, endDateString, startTimeString, endTimeString,
                                arNumEditText.text.toString(),
                            )
                        }
                    } else if (batchNumEditText.text.toString().isNotEmpty()) {
                        allLogsArrayList = withContext(Dispatchers.IO) {
                            allLogsDataDao.getLogByBatchnum_DNT(
                                startDateString, endDateString, startTimeString, endTimeString,
                                batchNumEditText.text.toString(),
                            )
                        }
                    } else if (compoundNameEditText.text.toString().isNotEmpty()) {
                        allLogsArrayList = withContext(Dispatchers.IO) {
                            allLogsDataDao.getLogByProduct_DNT(
                                startDateString, endDateString, startTimeString, endTimeString,
                                compoundNameEditText.text.toString(),
                            )
                        }
                    }


                } else {
                    allLogsArrayList = withContext(Dispatchers.IO) {
                        allLogsDataDao.getAllLogBy_DNT(
                            startDateString, endDateString, startTimeString, endTimeString,
                        )
                    }
                }
            } else if (startDateString != "" && endDateString != "") {
                if (arNumEditText.text.toString().isNotEmpty() || batchNumEditText.text.toString()
                        .isNotEmpty()
                    || compoundNameEditText.text.toString().isNotEmpty()
                ) {

                    if (arNumEditText.text.toString()
                            .isNotEmpty() && batchNumEditText.text.toString()
                            .isNotEmpty() && compoundNameEditText.text.toString().isNotEmpty()
                    ) {
                        allLogsArrayList = withContext(Dispatchers.IO) {
                            allLogsDataDao.getLogByBAC_DNT(
                                startDateString,
                                endDateString,
                                startTimeString,
                                endTimeString,
                                arNumEditText.text.toString(),
                                batchNumEditText.text.toString(),
                                compoundNameEditText.text.toString()
                            )
                        }
                    } else if (arNumEditText.text.toString()
                            .isNotEmpty() && batchNumEditText.text.toString().isNotEmpty()
                    ) {
                        allLogsArrayList = withContext(Dispatchers.IO) {
                            allLogsDataDao.getLogByArnumAndBatchnum_Date(
                                startDateString, endDateString,
                                arNumEditText.text.toString(),
                                batchNumEditText.text.toString()
                            )
                        }
                    } else if (arNumEditText.text.toString()
                            .isNotEmpty() && compoundNameEditText.text.toString().isNotEmpty()
                    ) {
                        allLogsArrayList = withContext(Dispatchers.IO) {

                            allLogsDataDao.getLogByArnumAndProduct_Date(
                                startDateString, endDateString,
                                arNumEditText.text.toString(),
                                compoundNameEditText.text.toString()
                            )
                        }
                    } else if (batchNumEditText.text.toString()
                            .isNotEmpty() && compoundNameEditText.text.toString().isNotEmpty()
                    ) {
                        allLogsArrayList = withContext(Dispatchers.IO) {

                            allLogsDataDao.getLogByBatchnumAndProduct_Date(
                                startDateString, endDateString,
                                batchNumEditText.text.toString(),
                                compoundNameEditText.text.toString()
                            )
                        }
                    } else if (arNumEditText.text.toString().isNotEmpty()) {
                        allLogsArrayList = withContext(Dispatchers.IO) {
                            allLogsDataDao.getLogByArnum_Date(
                                startDateString, endDateString,
                                arNumEditText.text.toString(),
                            )
                        }
                    } else if (batchNumEditText.text.toString().isNotEmpty()) {
                        allLogsArrayList = withContext(Dispatchers.IO) {
                            allLogsDataDao.getLogByBatchnum_Date(
                                startDateString, endDateString,
                                batchNumEditText.text.toString(),
                            )
                        }
                    } else if (compoundNameEditText.text.toString().isNotEmpty()) {
                        allLogsArrayList = withContext(Dispatchers.IO) {
                            allLogsDataDao.getLogByProduct_Date(
                                startDateString, endDateString,
                                compoundNameEditText.text.toString(),
                            )
                        }
                    }


                } else {
                    Log.e("NothingNoDate", "Ok")
                    allLogsArrayList = withContext(Dispatchers.IO) {
                        allLogsDataDao.getAllLogBy_Date(
                            startDateString, endDateString
                        )
                    }
                }
            } else {
                if (arNumEditText.text.toString().isNotEmpty() || batchNumEditText.text.toString()
                        .isNotEmpty()
                    || compoundNameEditText.text.toString().isNotEmpty()
                ) {

                    if (arNumEditText.text.toString()
                            .isNotEmpty() && batchNumEditText.text.toString()
                            .isNotEmpty() && compoundNameEditText.text.toString().isNotEmpty()
                    ) {
                        allLogsArrayList = withContext(Dispatchers.IO) {
                            allLogsDataDao.getLogByBAC(
                                arNumEditText.text.toString(),
                                batchNumEditText.text.toString(),
                                compoundNameEditText.text.toString()
                            )
                        }
                    } else if (arNumEditText.text.toString()
                            .isNotEmpty() && batchNumEditText.text.toString().isNotEmpty()
                    ) {
                        allLogsArrayList = withContext(Dispatchers.IO) {
                            allLogsDataDao.getLogByArnumAndBatchnum(
                                arNumEditText.text.toString(),
                                batchNumEditText.text.toString()
                            )
                        }
                    } else if (arNumEditText.text.toString()
                            .isNotEmpty() && compoundNameEditText.text.toString().isNotEmpty()
                    ) {
                        allLogsArrayList = withContext(Dispatchers.IO) {

                            allLogsDataDao.getLogByArnumAndProduct(
                                arNumEditText.text.toString(),
                                compoundNameEditText.text.toString()
                            )
                        }
                    } else if (batchNumEditText.text.toString()
                            .isNotEmpty() && compoundNameEditText.text.toString().isNotEmpty()
                    ) {
                        allLogsArrayList = withContext(Dispatchers.IO) {

                            allLogsDataDao.getLogByBatchnumAndProduct(
                                batchNumEditText.text.toString(),
                                compoundNameEditText.text.toString()
                            )
                        }
                    } else if (arNumEditText.text.toString().isNotEmpty()) {
                        allLogsArrayList = withContext(Dispatchers.IO) {
                            allLogsDataDao.getLogByArnum(
                                arNumEditText.text.toString(),
                            )
                        }
                    } else if (batchNumEditText.text.toString().isNotEmpty()) {
                        allLogsArrayList = withContext(Dispatchers.IO) {
                            allLogsDataDao.getLogByBatchnum(
                                batchNumEditText.text.toString(),
                            )
                        }
                    } else if (compoundNameEditText.text.toString().isNotEmpty()) {
                        allLogsArrayList = withContext(Dispatchers.IO) {
                            allLogsDataDao.getLogByProduct(
                                compoundNameEditText.text.toString(),
                            )
                        }
                    }


                } else {
                    allLogsArrayList = withContext(Dispatchers.IO) {
                        allLogsDataDao.getAllLogs()
                    }
                }
            }




            if (allLogsArrayList != null) {
                Toast.makeText(this@Export, "" + allLogsArrayList.size, Toast.LENGTH_SHORT).show()
                for (logs in allLogsArrayList) {
                    val date = logs.date
                    val time = logs.time
                    val device = logs.deviceID
                    val pH = logs.ph
                    val temp = logs.temperature
                    var batchnum = logs.batchnum
                    var arnum = logs.arnum
                    var comp = logs.compound
                    table1.addCell(date)
                    table1.addCell(time)
                    table1.addCell(pH ?: "--")
                    table1.addCell(temp ?: "--")
                    if (batchnum == null) {
                        batchnum = "--"
                    }
                    table1.addCell(
                        if (batchnum != null && batchnum.length >= 8) stringSplitter(
                            batchnum
                        ) else batchnum
                    )
                    if (arnum == null) {
                        arnum = "--"
                    }
                    table1.addCell(if (arnum != null && arnum.length >= 8) stringSplitter(arnum) else arnum)
                    if (comp == null) {
                        comp = "--"
                    }
                    table1.addCell(if (comp != null && comp.length >= 8) stringSplitter(comp) else comp)


                }

                document.add(table1)

                document.add(Paragraph(""))


                val leftDesignationString =
                    SharedPref.getSavedData(this@Export, SharedKeys.LEFT_DESIGNATION_KEY)
                val rightDesignationString =
                    SharedPref.getSavedData(
                        this@Export, SharedKeys.RIGHT_DESIGNATION_KEY
                    )

                if (leftDesignationString != null && leftDesignationString != "") {

                } else {
                    SharedPref.saveData(
                        this@Export,
                        SharedKeys.LEFT_DESIGNATION_KEY,
                        "Operator Sign"
                    )
                }

                if (rightDesignationString != null && rightDesignationString != "") {
                } else {
                    SharedPref.saveData(
                        this@Export,
                        SharedKeys.RIGHT_DESIGNATION_KEY,
                        "Supervisor Sign"
                    )
                }

                if (leftDesignationString != null && leftDesignationString != "" &&
                    rightDesignationString != null && rightDesignationString != ""
                ) {
                    document.add(Paragraph("$leftDesignationString                                                                                      $rightDesignationString"))

                } else {
                    document.add(Paragraph("Operator Sign                                                                                      Supervisor Sign"))

                }

                val imgBit1 = getSignImage()

                if (imgBit1 != null) {

                    val byteArrayOutputStream = ByteArrayOutputStream()
                    imgBit1.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                    val byteArray = byteArrayOutputStream.toByteArray()

                    val imageData = ImageDataFactory.create(byteArray)
                    val image = Image(imageData).setHeight(80f).setWidth(80f)
                    document.add(image)

                } else {
//                Toast.makeText(requireContext(), "Null", Toast.LENGTH_SHORT).show()
                }
                document.close()
                Toast.makeText(this@Export, "Pdf generated", Toast.LENGTH_SHORT).show()

            } else {
                document.add(Paragraph(""))


                val leftDesignationString =
                    SharedPref.getSavedData(this@Export, SharedKeys.LEFT_DESIGNATION_KEY)
                val rightDesignationString =
                    SharedPref.getSavedData(
                        this@Export, SharedKeys.RIGHT_DESIGNATION_KEY
                    )

                if (leftDesignationString != null && leftDesignationString != "") {

                } else {
                    SharedPref.saveData(
                        this@Export,
                        SharedKeys.LEFT_DESIGNATION_KEY,
                        "Operator Sign"
                    )
                }

                if (rightDesignationString != null && rightDesignationString != "") {
                } else {
                    SharedPref.saveData(
                        this@Export,
                        SharedKeys.RIGHT_DESIGNATION_KEY,
                        "Supervisor Sign"
                    )
                }

                if (leftDesignationString != null && leftDesignationString != "" &&
                    rightDesignationString != null && rightDesignationString != ""
                ) {
                    document.add(Paragraph("$leftDesignationString                                                                                      $rightDesignationString"))

                } else {
                    document.add(Paragraph("Operator Sign                                                                                      Supervisor Sign"))

                }

                val imgBit1 = getSignImage()

                if (imgBit1 != null) {

                    val byteArrayOutputStream = ByteArrayOutputStream()
                    imgBit1.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                    val byteArray = byteArrayOutputStream.toByteArray()

                    val imageData = ImageDataFactory.create(byteArray)
                    val image = Image(imageData).setHeight(80f).setWidth(80f)
                    document.add(image)

                } else {
//                Toast.makeText(requireContext(), "Null", Toast.LENGTH_SHORT).show()
                }
                document.close()
                Toast.makeText(this@Export, "Pdf generated", Toast.LENGTH_SHORT).show()
            }
        }


//        while (curCSV.moveToNext()) {
//            val date = curCSV.getString(curCSV.getColumnIndex("date"))
//            val time = curCSV.getString(curCSV.getColumnIndex("time"))
//            val device = curCSV.getString(curCSV.getColumnIndex("deviceID"))
//            val pH = curCSV.getString(curCSV.getColumnIndex("ph"))
//            val temp = curCSV.getString(curCSV.getColumnIndex("temperature"))
//            var batchnum = curCSV.getString(curCSV.getColumnIndex("batchnum"))
//            var arnum = curCSV.getString(curCSV.getColumnIndex("arnum"))
//            var comp = curCSV.getString(curCSV.getColumnIndex("compound"))
//            table1.addCell(date)
//            table1.addCell(time)
//            table1.addCell(pH ?: "--")
//            table1.addCell(temp ?: "--")
//            if (batchnum == null) {
//                batchnum = "--"
//            }
//            table1.addCell(if (batchnum != null && batchnum.length >= 8) stringSplitter(batchnum) else batchnum)
//            if (arnum == null) {
//                arnum = "--"
//            }
//            table1.addCell(if (arnum != null && arnum.length >= 8) stringSplitter(arnum) else arnum)
//            if (comp == null) {
//                comp = "--"
//            }
//            table1.addCell(if (comp != null && comp.length >= 8) stringSplitter(comp) else comp)
//        }

    }

    fun getImageUri(inContext: Context, inImage: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path =
            MediaStore.Images.Media.insertImage(inContext.contentResolver, inImage, "Title", null)
        return Uri.parse(path)
    }

    private fun checkPermissions(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }
        var allGranted = true
        for (permission in permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false
            }
        }
        if (!allGranted) requestPermissions(permissions, CAMERA_REQUEST)
        return allGranted
    }


    private fun fileNotWrite(file: File) {
        file.setWritable(false)
        if (file.canWrite()) {
            Log.d("csv", "Nhi kaam kar rha")
        } else {
            Log.d("csvnw", "Party Bhaiiiii")
        }
    }


    @Throws(FileNotFoundException::class)
    fun generateUserActivityPDF() {
        Toast.makeText(this@Export, "Printing...", Toast.LENGTH_LONG).show()

        if (SharedPref.getSavedData(
                this@Export,
                "COMPANY_NAME"
            ) != null && SharedPref.getSavedData(
                this@Export, "COMPANY_NAME"
            ) != "N/A"
        ) {
            companyName = SharedPref.getSavedData(this@Export, "COMPANY_NAME");
        } else {
            companyName = "N/A";
        }
        val company_name = "Company: $companyName"
        val user_name = "Supervisor: " + Source.logUserName
        val device_id = "DeviceID: $deviceID"
        reportDate = "Date: " + SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        reportTime = "Time: " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val shp = getSharedPreferences("Extras", MODE_PRIVATE)
        offset = "Offset: " + shp.getString("offset", "")
        if (Constants.OFFLINE_DATA) {
            val offsetData = SharedPref.getSavedData(this@Export, "OFFSET_" + PhActivity.DEVICE_ID)

            offset = if (SharedPref.getSavedData(
                    this@Export,
                    "OFFSET_" + PhActivity.DEVICE_ID
                ) != null && SharedPref.getSavedData(
                    this@Export,
                    "OFFSET_" + PhActivity.DEVICE_ID
                ) !== ""
            ) {
                "Offset: $offsetData"
            } else {
                "Offset: " + "null"
            }
        } else {
        }
        var tempe = "Temperature: " + shp.getString("temp", "")
        battery = "Battery: " + shp.getString("battery", "")
        if (Constants.OFFLINE_DATA) {
            val slopeData = SharedPref.getSavedData(this@Export, "SLOPE_" + PhActivity.DEVICE_ID)

            slope = if (SharedPref.getSavedData(
                    this@Export,
                    "SLOPE_" + PhActivity.DEVICE_ID
                ) != null && SharedPref.getSavedData(
                    this@Export,
                    "SLOPE_" + PhActivity.DEVICE_ID
                ) !== ""
            ) {
                "Slope: $slopeData"
            } else {
                "Slope: " + "null"
            }
            val tempData = SharedPref.getSavedData(this@Export, "tempValue" + PhActivity.DEVICE_ID)

            tempe = if (SharedPref.getSavedData(
                    this@Export,
                    "tempValue" + PhActivity.DEVICE_ID
                ) != null && SharedPref.getSavedData(
                    this@Export,
                    "tempValue" + PhActivity.DEVICE_ID
                ) !== ""
            ) {
                "Temperature: $tempData"
            } else {
                "Temperature: " + "null"
            }
            val batteryVal = SharedPref.getSavedData(this@Export, "battery" + PhActivity.DEVICE_ID)
            if (batteryVal != null) {
                if (batteryVal != "") {
                    battery = "Battery: $batteryVal %"
//                    binding.batteryPercent.text = "$batteryVal %"
                } else {
                    battery = "Battery: 0 %"

                }
            }

        } else {
            slope = "Slope: " + shp.getString("slope", "")
        }
        val exportDir =
            File(ContextWrapper(this@Export).externalMediaDirs[0].toString() + File.separator + "/LabApp/Useractivity")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
        val currentDateandTime = sdf.format(Date())
        val tempPath =
            ContextWrapper(this@Export).externalMediaDirs[0].toString() + File.separator + "/LabApp/Useractivity"
        val tempRoot = File(tempPath)
        fileNotWrite(tempRoot)
        val tempFilesAndFolders = tempRoot.listFiles()
        val file =
            File(ContextWrapper(this@Export).externalMediaDirs[0].toString() + File.separator + "/LabApp/Useractivity/UA_" + currentDateandTime + "_" + (tempFilesAndFolders.size - 1) + ".pdf")
        val outputStream: OutputStream = FileOutputStream(file)
        val writer = PdfWriter(file)
        val pdfDocument = PdfDocument(writer)
        val document = Document(pdfDocument)
        val imgBit = getCompanyLogo()
        if (imgBit != null) {

            val byteArrayOutputStream = ByteArrayOutputStream()
            imgBit.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()

// Create ImageData from byte array
            val imageData = ImageDataFactory.create(byteArray)

// Create an Image element
            val image = Image(imageData).setHeight(80f).setWidth(80f)
            document.add(image)

        } else {
//                Toast.makeText(requireContext(), "Null", Toast.LENGTH_SHORT).show()
        }
        document.add(
            Paragraph(
                """
            $company_name
            $user_name
            $device_id
            """.trimIndent()
            )
        )
        document.add(Paragraph(""))
        document.add(
            Paragraph(
                """$reportDate  |  $reportTime
$offset  |  $battery
$slope  |  $tempe"""
            )
        )
        document.add(Paragraph(""))
        val db = databaseHelper.writableDatabase
        val shp2 = getSharedPreferences("RolePref", MODE_PRIVATE)
        //            roleExport = "Supervisor: " + shp2.getString("roleSuper", "");
        roleExport = "Supervisor: " + Source.logUserName
        var userCSV = db.rawQuery("SELECT * FROM UserActiondetails", null)
        if (startDateString != null) {
            userCSV = db.rawQuery(
                "SELECT * FROM UserActiondetails WHERE (DATE(date) BETWEEN '$startDateString' AND '$endDateString') AND (time BETWEEN '$startTimeString' AND '$endTimeString')",
                null
            )
            //            userCSV = db.rawQuery("SELECT * FROM UserActiondetails WHERE (DATE(date) BETWEEN '" + startDateString + "' AND '" + endDateString + "')", null);
        }
        document.add(Paragraph("User Activity Data Table"))
        val columnWidth1 = floatArrayOf(300f, 350f, 150f, 150f, 170f, 200f)
        val table1 = Table(columnWidth1)
        table1.addCell("Date & Time")
        table1.addCell("Activity")
        table1.addCell("pH")
        table1.addCell("Temp")
        table1.addCell("mV")
        table1.addCell("Device ID")



        GlobalScope.launch(Dispatchers.Main) {
            val allUserActionsArrayList = withContext(Dispatchers.IO) {
                userActionDao.getAllUsersActions()
            }
            for (userA in allUserActionsArrayList) {
                val Time = userA.time
                var Date = userA.date
                val activity = userA.userAction
                val Ph = userA.ph
                val Temp = userA.temperature
                val Mv = userA.mv
                val device = userA.deviceID
                Date = "$Date $Time"
                //            String record2 = Date + "," + Activity + "," + Ph + "," + Temp + "," + Mv + "," + device;
                table1.addCell(Date + "")
                table1.addCell(activity + "")
                table1.addCell(Ph + "")
                table1.addCell(Temp + "")
                table1.addCell(Mv + "")
                table1.addCell(device + "")
            }
            document.add(table1)
            val leftDesignationString =
                SharedPref.getSavedData(this@Export, SharedKeys.LEFT_DESIGNATION_KEY)
            val rightDesignationString =
                SharedPref.getSavedData(
                    this@Export, SharedKeys.RIGHT_DESIGNATION_KEY
                )

            if (leftDesignationString != null && leftDesignationString != "") {

            } else {
                SharedPref.saveData(
                    this@Export,
                    SharedKeys.LEFT_DESIGNATION_KEY,
                    "Operator Sign"
                )
            }

            if (rightDesignationString != null && rightDesignationString != "") {
            } else {
                SharedPref.saveData(
                    this@Export,
                    SharedKeys.RIGHT_DESIGNATION_KEY,
                    "Supervisor Sign"
                )
            }

            if (leftDesignationString != null && leftDesignationString != "" &&
                rightDesignationString != null && rightDesignationString != ""
            ) {
                document.add(Paragraph("$leftDesignationString                                                                                      $rightDesignationString"))

            } else {
                document.add(Paragraph("Operator Sign                                                                                      Supervisor Sign"))

            }

            val imgBit1 = getSignImage()

            if (imgBit1 != null) {

                val byteArrayOutputStream = ByteArrayOutputStream()
                imgBit1.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()

// Create ImageData from byte array
                val imageData = ImageDataFactory.create(byteArray)

// Create an Image element
                val image = Image(imageData).setHeight(80f).setWidth(80f)
                document.add(image)

            } else {
//                Toast.makeText(requireContext(), "Null", Toast.LENGTH_SHORT).show()
            }
            document.close()
            Toast.makeText(this@Export, "Pdf generated", Toast.LENGTH_SHORT).show()
        }

    }

    private fun checkPermission(): Boolean {
        val permission1 =
            ContextCompat.checkSelfPermission(applicationContext, permission.WRITE_EXTERNAL_STORAGE)
        val permission2 =
            ContextCompat.checkSelfPermission(applicationContext, permission.READ_EXTERNAL_STORAGE)
        return permission1 == PackageManager.PERMISSION_GRANTED && permission2 == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(permission.WRITE_EXTERNAL_STORAGE, permission.READ_EXTERNAL_STORAGE),
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST) {
            var allGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false
                }
            }
            if (!allGranted) {
                Toast.makeText(this, "Permission needed for app to work.", Toast.LENGTH_SHORT)
                    .show()
            } else {
            }
        }
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.size > 0) {
                val writeStorage = grantResults[0] == PackageManager.PERMISSION_GRANTED
                val readStorage = grantResults[1] == PackageManager.PERMISSION_GRANTED
                if (writeStorage && readStorage) {
                    Toast.makeText(applicationContext, "Permission Granted..", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Toast.makeText(applicationContext, "Permission Denined.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }


}