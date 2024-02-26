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
import com.aican.aicanapp.R
import com.aican.aicanapp.adapters.FileAdapter
import com.aican.aicanapp.adapters.UserDataAdapter
import com.aican.aicanapp.data.DatabaseHelper
import com.aican.aicanapp.databinding.ActivityExportBinding
import com.aican.aicanapp.utils.Constants
import com.aican.aicanapp.utils.SharedPref
import com.aican.aicanapp.utils.Source
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.common.base.Splitter
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.MalformedURLException
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
    lateinit var exportCSV: Button
    lateinit var convertToXls: Button
    lateinit var arNumBtn: ImageButton
    lateinit var batchNumBtn: ImageButton
    lateinit var compoundBtn: ImageButton
    lateinit var tvStartDate: TextView
    lateinit var tvEndDate: TextView
    lateinit var tvUserLog: TextView
    lateinit var deviceId: TextView
    lateinit var dateA: TextView
    lateinit var deviceID: String
    lateinit var user: String
    lateinit var roleExport: String
    lateinit var reportDate: String
    lateinit var reportTime: String
    lateinit var startDateString: String
    lateinit var endDateString: String
    lateinit var startTimeString: String
    lateinit var endTimeString: String
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
    lateinit var fAdapter: FileAdapter
    lateinit var uAdapter: UserDataAdapter
    lateinit var companyNameEditText: EditText
    lateinit var arNumEditText: EditText
    lateinit var batchNumEditText: EditText
    lateinit var compoundNameEditText: EditText
    lateinit var databaseHelper: DatabaseHelper
    lateinit var deviceRef: DatabaseReference
    lateinit var month: String
    lateinit var year: String
    lateinit var binding: ActivityExportBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewCSV)
        val userRecyclerView = findViewById<RecyclerView>(R.id.recyclerViewUserData)
//        TextView noFilesText = findViewById(R.id.nofiles_textview);
        //        TextView noFilesText = findViewById(R.id.nofiles_textview);
        selectCompanyLogo = findViewById<Button>(R.id.selectCompanyLogo)
        selectCompanyLogo.setOnClickListener { v: View? -> showOptionDialog() }
        companyLogo = findViewById<ImageView>(R.id.companyLogo)
        deviceId = findViewById<TextView>(R.id.DeviceId)
        dateA = findViewById<TextView>(R.id.dateA)
        exportCSV = findViewById<Button>(R.id.exportCSV)
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
        deviceRef =
            FirebaseDatabase.getInstance(FirebaseApp.getInstance(PhActivity.DEVICE_ID!!)).reference.child(
                "PHMETER"
            ).child(
                PhActivity.DEVICE_ID!!
            )

        companyNameEditText = findViewById<EditText>(R.id.companyName)

        companyNameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                companyName = charSequence.toString()
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

        if (Source.subscription != null ) {
          if(  Source.subscription.equals("nonCfr") ){
                exportUserData.visibility = View.GONE
                userRecyclerView.visibility = View.GONE
                tvUserLog.visibility = View.GONE
            }
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
                val date1 = "Start: $startDateString End: $endDateString"
                Toast.makeText(this, date1, Toast.LENGTH_SHORT).show()

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
                    }
                }
            }
        }

        arNumBtn.setOnClickListener { arNumString = arNumEditText.text.toString() }

        batchNumBtn.setOnClickListener { batchNumString = batchNumEditText.text.toString() }

        compoundBtn.setOnClickListener { compoundName = compoundNameEditText.text.toString() }

        exportCSV.setOnClickListener {
            companyName = companyNameEditText.text.toString()
            if (!companyName.isEmpty()) {
                if (Constants.OFFLINE_MODE) {
                    val company_name =
                        getSharedPreferences("COMPANY_NAME", MODE_PRIVATE)
                    val editT = company_name.edit()
                    editT.putString("COMPANY_NAME", companyName)
                    editT.commit()
                } else {
                    deviceRef.child("UI").child("PH").child("PH_CAL").child("COMPANY_NAME")
                        .setValue(companyName)
                }
            }
            try {
                generatePDF1()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
            val pathPDF =
                ContextWrapper(this@Export).externalMediaDirs[0].toString() + File.separator + "/LabApp/Sensordata/"
            val rootPDF = File(pathPDF)
            fileNotWrite(rootPDF)
            val filesAndFoldersPDF = rootPDF.listFiles()
            fAdapter =
                FileAdapter(applicationContext, reverseFileArray(filesAndFoldersPDF), "PhExport")
            recyclerView.adapter = fAdapter
            fAdapter.notifyDataSetChanged()
            recyclerView.layoutManager = LinearLayoutManager(applicationContext)
            convertToXls.visibility = View.INVISIBLE
        }

        val pathPDF =
            ContextWrapper(this@Export).externalMediaDirs[0].toString() + File.separator + "/LabApp/Sensordata/"
        val rootPDF = File(pathPDF)
        fileNotWrite(rootPDF)
        val filesAndFoldersPDF = rootPDF.listFiles()


        fAdapter = FileAdapter(
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
            if (!companyName.isEmpty()) {
                deviceRef.child("UI").child("PH").child("PH_CAL").child("COMPANY_NAME")
                    .setValue(companyName)
            }
            try {
                generatePDF2()
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
            val company_name = getSharedPreferences("COMPANY_NAME", MODE_PRIVATE)
            companyNameEditText.setText(company_name.getString("COMPANY_NAME", "N/A"))
        }

        val comLo = getCompanyLogo()
        if (comLo != null) {
            companyLogo.setImageBitmap(comLo)
        }


        printAllCalibData.setOnClickListener { v: View? ->
            try {
                generateAllPDF()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
//                exportCalibData();
            val path = ContextWrapper(this@Export).externalMediaDirs[0]
                .toString() + File.separator + "/LabApp/CalibrationData"
            val root = File(path)
            val filesAndFolders = root.listFiles()
            if (filesAndFolders == null || filesAndFolders.size == 0) {
                return@setOnClickListener
            } else {
                for (i in filesAndFolders.indices) {
                    filesAndFolders[i].name.endsWith(".pdf")
                }
            }
            val pathPDF1 = ContextWrapper(this@Export).externalMediaDirs[0]
                .toString() + File.separator + "/LabApp/Sensordata/"
            val rootPDF1 = File(pathPDF1)
            fileNotWrite(root)
            val filesAndFoldersPDF1 = rootPDF1.listFiles()
            fAdapter = FileAdapter(
                applicationContext,
                reverseFileArray(filesAndFoldersPDF1),
                "PhExport"
            )
            recyclerView.adapter = fAdapter
            fAdapter.notifyDataSetChanged()
            recyclerView.layoutManager = LinearLayoutManager(applicationContext)
            convertToXls.visibility = View.INVISIBLE
        }

    }


    @Throws(FileNotFoundException::class)
    private fun generateAllPDF() {
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
        if (Constants.OFFLINE_DATA) {
            slope = if (SharedPref.getSavedData(
                    this@Export,
                    "SLOPE_" + PhActivity.DEVICE_ID
                ) != null && SharedPref.getSavedData(
                    this@Export,
                    "SLOPE_" + PhActivity.DEVICE_ID
                ) !== ""
            ) {
                val data = SharedPref.getSavedData(this@Export, "SLOPE_" + PhActivity.DEVICE_ID)
                "Slope: $data"
            } else {
                "Slope: " + "null"
            }
            temp = if (SharedPref.getSavedData(
                    this@Export,
                    "tempValue" + PhActivity.DEVICE_ID
                ) != null && SharedPref.getSavedData(
                    this@Export,
                    "tempValue" + PhActivity.DEVICE_ID
                ) !== ""
            ) {
                val data = SharedPref.getSavedData(this@Export, "tempValue" + PhActivity.DEVICE_ID)
                "Temperature: $data"
            } else {
                "Temperature: " + "null"
            }
            val batteryVal = SharedPref.getSavedData(this@Export, "battery" + PhActivity.DEVICE_ID)
            if (batteryVal != null) {
                if (batteryVal != "") {
                    battery = "$batteryVal %"
//                    binding.batteryPercent.text = "$batteryVal %"
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
            val uri: Uri = getImageUri(this@Export, imgBit)
            try {
                val add: String = getPath(uri)
                val imageData = ImageDataFactory.create(add)
                val image = Image(imageData).setHeight(80f).setWidth(80f)
                //                table12.addCell(new Cell(2, 1).add(image));
                // Adding image to the document
                document.add(image)
            } catch (e: MalformedURLException) {
                e.printStackTrace()
            }
        }

//        Text text = new Text(company_name);
//        Text text1 = new Text(user_name);
//        Text text2 = new Text(device_id);
//
//
//
//        document.add(new Paragraph(text).add(text1).add(text2));
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
        if (Constants.OFFLINE_DATA) {
            calibCSV = db.rawQuery("SELECT * FROM CalibAllDataOffline", null)
            calibCSV = if (startDateString != null) {
                db.rawQuery(
                    "SELECT * FROM CalibAllDataOffline WHERE (DATE(date) BETWEEN '$startDateString' AND '$endDateString') AND (time BETWEEN '$startTimeString' AND '$endTimeString')",
                    null
                )
                //            curCSV = db.rawQuery("SELECT * FROM LogUserdetails WHERE (DATE(date) BETWEEN '" + startDateString + "' AND '" + endDateString + "')", null);
            } else {
                db.rawQuery("SELECT * FROM CalibAllDataOffline", null)
            }
        } else {
            calibCSV = db.rawQuery("SELECT * FROM CalibAllData", null)
            calibCSV = if (startDateString != null) {
                db.rawQuery(
                    "SELECT * FROM CalibAllData WHERE (DATE(date) BETWEEN '$startDateString' AND '$endDateString') AND (time BETWEEN '$startTimeString' AND '$endTimeString')",
                    null
                )
                //            curCSV = db.rawQuery("SELECT * FROM LogUserdetails WHERE (DATE(date) BETWEEN '" + startDateString + "' AND '" + endDateString + "')", null);
            } else {
                db.rawQuery("SELECT * FROM CalibAllData", null)
            }
        }
        val so = 1
        val columnWidth = floatArrayOf(200f, 210f, 190f, 170f, 340f, 170f, 210f)
        var table = Table(columnWidth)
        table.addCell("pH")
        table.addCell("pH Aft Calib")
        table.addCell("Slope")
        table.addCell("mV")
        table.addCell("Date & Time")
        table.addCell("Temperature")
        table.addCell("Calibrated by")
        var rowCounter = 0 // To keep track of the number of rows processed
        while (calibCSV.moveToNext()) {
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
            table.addCell(if (Source.calib_completed_by == null) "Unknown" else Source.calib_completed_by)
            rowCounter++
            if (rowCounter % 5 == 0) {
                // Add the table to the document every 5 rows
                document.add(table)
                document.add(Paragraph("Calibration : " + "completed"))
                document.add(Paragraph("Operator Sign                                                                                      Supervisor Sign"))
                document.add(Paragraph(""))
                document.add(Paragraph(""))
                document.add(Paragraph(""))
                document.add(Paragraph(""))
                document.add(Paragraph(""))
                document.add(Paragraph(""))

                // Create a new table for the next set of 5 rows
                table = Table(columnWidth)
                table.addCell("pH")
                table.addCell("pH Aft Calib")
                table.addCell("Slope")
                table.addCell("mV")
                table.addCell("Date & Time")
                table.addCell("Temperature")
                table.addCell("Calibrated by")
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
            val uri1: Uri = getImageUri(this, imgBit1)
            try {
                val add: String = getPath(uri1)
                val imageData1 = ImageDataFactory.create(add)
                val image1 = Image(imageData1).setHeight(80f).setWidth(80f)
                //                table12.addCell(new Cell(2, 1).add(image));
                // Adding image to the document
                document.add(image1)
            } catch (e: MalformedURLException) {
                e.printStackTrace()
            }
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

    fun reverseFileArray(fileArray: Array<File?>?): Array<File?>? {
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

    @Throws(FileNotFoundException::class)
    fun generatePDF1() {
        val device_id = "DeviceID: $deviceID"
        companyName = "" + companyNameEditText.text.toString()
        reportDate = "Date: " + SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        reportTime = "Time: " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val shp = getSharedPreferences("Extras", MODE_PRIVATE)
        offset = "Offset: " + shp.getString("offset", "")
        if (Constants.OFFLINE_DATA) {
            if (SharedPref.getSavedData(
                    this@Export,
                    "OFFSET_" + PhActivity.DEVICE_ID
                ) != null && SharedPref.getSavedData(
                    this@Export,
                    "OFFSET_" + PhActivity.DEVICE_ID
                ) !== ""
            ) {
                val data = SharedPref.getSavedData(this@Export, "OFFSET_" + PhActivity.DEVICE_ID)
                offset = "Offset: $data"
            } else {
                offset = "Offset: " + "null"
            }
        } else {
        }
        temp = "Temperature: " + shp.getString("temp", "")
        battery = "Battery: " + shp.getString("battery", "")
        if (Constants.OFFLINE_DATA) {
            if (SharedPref.getSavedData(
                    this@Export,
                    "SLOPE_" + PhActivity.DEVICE_ID
                ) != null && SharedPref.getSavedData(
                    this@Export,
                    "SLOPE_" + PhActivity.DEVICE_ID
                ) !== ""
            ) {
                val data = SharedPref.getSavedData(this@Export, "SLOPE_" + PhActivity.DEVICE_ID)
                slope = "Slope: $data"
            } else {
                slope = "Slope: " + "null"
            }
            if (SharedPref.getSavedData(
                    this@Export,
                    "tempValue" + PhActivity.DEVICE_ID
                ) != null && SharedPref.getSavedData(
                    this@Export,
                    "tempValue" + PhActivity.DEVICE_ID
                ) !== ""
            ) {
                val data = SharedPref.getSavedData(this@Export, "tempValue" + PhActivity.DEVICE_ID)
                temp = "Temperature: $data"
            } else {
                temp = "Temperature: " + "null"
            }
            val batteryVal = SharedPref.getSavedData(this@Export, "battery" + PhActivity.DEVICE_ID)
            if (batteryVal != null) {
                if (batteryVal != "") {
                    battery  = "$batteryVal %"
//                    binding.batteryPercent.text = "$batteryVal %"
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
            val uri = getImageUri(this@Export, imgBit)
            try {
                val add = getPath(uri)
                val imageData = ImageDataFactory.create(add)
                val image = Image(imageData).setHeight(80f).setWidth(80f)
                //                table12.addCell(new Cell(2, 1).add(image));
                // Adding image to the document
                document.add(image)
            } catch (e: MalformedURLException) {
                e.printStackTrace()
            }
        }
        //
//        table12.addCell(new Paragraph(companyName));
//        table12.setBorder(Border.NO_BORDER);
//
//        document.add(table12);
        if (Constants.OFFLINE_MODE) {
//            document.add(new Paragraph("Offline Mode"));
        }
        document.add(Paragraph(companyName + "\n" + roleExport + "\n" + device_id))
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
        table1.addCell("Compound")
        var curCSV: Cursor
        if (Constants.OFFLINE_MODE) {
            curCSV = db.rawQuery(
                "SELECT * FROM LogUserdetails WHERE (DATE(date) BETWEEN '$startDateString' AND '$endDateString') AND (time BETWEEN '$startTimeString' AND '$endTimeString') AND (arnum = '$compoundName') AND (batchnum = '$batchNumString') AND (compound = '$arNumString')",
                null
            )
        } else {
            curCSV = db.rawQuery(
                "SELECT * FROM LogUserdetails WHERE (DATE(date) BETWEEN '$startDateString' AND '$endDateString') AND (time BETWEEN '$startTimeString' AND '$endTimeString') AND (arnum = '$compoundName') AND (batchnum = '$batchNumString') AND (compound = '$arNumString')",
                null
            )
            //            Cursor curCSV = db.rawQuery("SELECT * FROM LogUserdetails WHERE (DATE(date) BETWEEN '" + startDateString + "' AND '" + endDateString + "') AND (time BETWEEN '" + startTimeString + "' AND '" + endTimeString + "')')", null);
        }
        if (arNumEditText.text.toString().isEmpty()) {
            arNumString = null
        }
        if (compoundNameEditText.text.toString().isEmpty()) {
            compoundName = null
        }
        if (batchNumEditText.text.toString().isEmpty()) {
            batchNumString = null
        }

        //Setting sql query according to filer
        if ((startDateString != null) && (compoundName != null) && (batchNumString != null) && (arNumString != null)) {
            curCSV = db.rawQuery(
                "SELECT * FROM LogUserdetails WHERE (DATE(date) BETWEEN '$startDateString' AND '$endDateString') AND (time BETWEEN '$startTimeString' AND '$endTimeString') AND (arnum = '$compoundName') AND (batchnum = '$batchNumString') AND (compound = '$arNumString')",
                null
            )
        } else if ((startDateString != null) && (compoundName != null) && (batchNumString != null)) {
            curCSV = db.rawQuery(
                "SELECT * FROM LogUserdetails WHERE (DATE(date) BETWEEN '$startDateString' AND '$endDateString') AND (time BETWEEN '$startTimeString' AND '$endTimeString') AND (arnum = '$compoundName') AND (batchnum = '$batchNumString')",
                null
            )
        } else if ((startDateString != null) && (compoundName != null) && (arNumString != null)) {
            curCSV = db.rawQuery(
                "SELECT * FROM LogUserdetails WHERE (DATE(date) BETWEEN '$startDateString' AND '$endDateString') AND (time BETWEEN '$startTimeString' AND '$endTimeString') AND (arnum = '$compoundName') AND (batchnum = '$batchNumString') AND (compound = '$arNumString')",
                null
            )
        } else if ((startDateString != null) && (batchNumString != null) && (arNumString != null)) {
            curCSV = db.rawQuery(
                "SELECT * FROM LogUserdetails WHERE (DATE(date) BETWEEN '$startDateString' AND '$endDateString') AND (time BETWEEN '$startTimeString' AND '$endTimeString') AND (batchnum = '$batchNumString') AND (compound = '$arNumString')",
                null
            )
        } else if ((compoundName != null) && (batchNumString != null) && (arNumString != null)) {
            curCSV = db.rawQuery(
                "SELECT * FROM LogUserdetails WHERE  (arnum = '$compoundName') AND (batchnum = '$batchNumString') AND (compound = '$arNumString')",
                null
            )
        } else if (startDateString != null && compoundName != null) {
            curCSV = db.rawQuery(
                "SELECT * FROM LogUserdetails WHERE  (time BETWEEN '$startTimeString' AND '$endTimeString') AND (arnum = '$compoundName')",
                null
            )
        } else if (startDateString != null && batchNumString != null) {
            curCSV = db.rawQuery(
                "SELECT * FROM LogUserdetails WHERE (DATE(date) BETWEEN '$startDateString' AND '$endDateString') AND (time BETWEEN '$startTimeString' AND '$endTimeString') AND (batchnum = '$batchNumString')",
                null
            )
        } else if (startDateString != null && arNumString != null) {
            curCSV = db.rawQuery(
                "SELECT * FROM LogUserdetails WHERE (DATE(date) BETWEEN '$startDateString' AND '$endDateString') AND (time BETWEEN '$startTimeString' AND '$endTimeString') AND (compound = '$arNumString')",
                null
            )
        } else if (compoundName != null && batchNumString != null) {
            curCSV = db.rawQuery(
                "SELECT * FROM LogUserdetails WHERE  (arnum = '$compoundName') AND (batchnum = '$batchNumString')",
                null
            )
        } else if (compoundName != null && arNumString != null) {
            curCSV = db.rawQuery(
                "SELECT * FROM LogUserdetails WHERE  (arnum = '$compoundName') AND (compound = '$arNumString')",
                null
            )
        } else if (batchNumString != null && arNumString != null) {
            curCSV = db.rawQuery(
                "SELECT * FROM LogUserdetails WHERE  (batchnum = '$batchNumString') AND (compound = '$arNumString')",
                null
            )
        } else if (startDateString != null) {
            curCSV = db.rawQuery(
                "SELECT * FROM LogUserdetails WHERE (DATE(date) BETWEEN '$startDateString' AND '$endDateString') AND (time BETWEEN '$startTimeString' AND '$endTimeString')",
                null
            )
            //            curCSV = db.rawQuery("SELECT * FROM LogUserdetails WHERE (DATE(date) BETWEEN '" + startDateString + "' AND '" + endDateString + "')", null);
        } else if (compoundName != null) {
            curCSV =
                db.rawQuery("SELECT * FROM LogUserdetails WHERE  (arnum = '$compoundName')", null)
        } else if (batchNumString != null) {
            curCSV = db.rawQuery(
                "SELECT * FROM LogUserdetails WHERE  (batchnum = '$batchNumString') ",
                null
            )
        } else if (arNumString != null) {
            curCSV =
                db.rawQuery("SELECT * FROM LogUserdetails WHERE  (compound = '$arNumString')", null)
        } else {
            curCSV = db.rawQuery("SELECT * FROM LogUserdetails", null)
        }
        while (curCSV.moveToNext()) {
            val date = curCSV.getString(curCSV.getColumnIndex("date"))
            val time = curCSV.getString(curCSV.getColumnIndex("time"))
            val device = curCSV.getString(curCSV.getColumnIndex("deviceID"))
            val pH = curCSV.getString(curCSV.getColumnIndex("ph"))
            val temp = curCSV.getString(curCSV.getColumnIndex("temperature"))
            var batchnum = curCSV.getString(curCSV.getColumnIndex("batchnum"))
            var arnum = curCSV.getString(curCSV.getColumnIndex("arnum"))
            var comp = curCSV.getString(curCSV.getColumnIndex("compound"))
            table1.addCell(date)
            table1.addCell(time)
            table1.addCell(pH ?: "--")
            table1.addCell(temp ?: "--")
            if (batchnum == null) {
                batchnum = "--"
            }
            table1.addCell(if (batchnum != null && batchnum.length >= 8) stringSplitter(batchnum) else batchnum)
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
        document.add(Paragraph("Operator Sign                                                                                          Supervisor Sign"))
        val imgBit1 = getSignImage()
        if (imgBit1 != null) {
            val uri1 = getImageUri(this@Export, imgBit1)
            try {
                val add = getPath(uri1)
                val imageData1 = ImageDataFactory.create(add)
                val image1 = Image(imageData1).setHeight(80f).setWidth(80f)
                //                table12.addCell(new Cell(2, 1).add(image));
                // Adding image to the document
                document.add(image1)
            } catch (e: MalformedURLException) {
                e.printStackTrace()
            }
        }
        document.close()
        Toast.makeText(this@Export, "Pdf generated", Toast.LENGTH_SHORT).show()
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


    fun fileNotWrite(file: File) {
        file.setWritable(false)
        if (file.canWrite()) {
            Log.d("csv", "Nhi kaam kar rha")
        } else {
            Log.d("csvnw", "Party Bhaiiiii")
        }
    }

    @Throws(FileNotFoundException::class)
    fun generatePDF2() {
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
        var tempe = "Temperature: " + shp.getString("temp", "")
        battery = "Battery: " + shp.getString("battery", "")
        if (Constants.OFFLINE_DATA) {
            slope = if (SharedPref.getSavedData(
                    this@Export,
                    "SLOPE_" + PhActivity.DEVICE_ID
                ) != null && SharedPref.getSavedData(
                    this@Export,
                    "SLOPE_" + PhActivity.DEVICE_ID
                ) !== ""
            ) {
                val data = SharedPref.getSavedData(this@Export, "SLOPE_" + PhActivity.DEVICE_ID)
                "Slope: $data"
            } else {
                "Slope: " + "null"
            }
            tempe = if (SharedPref.getSavedData(
                    this@Export,
                    "tempValue" + PhActivity.DEVICE_ID
                ) != null && SharedPref.getSavedData(
                    this@Export,
                    "tempValue" + PhActivity.DEVICE_ID
                ) !== ""
            ) {
                val data = SharedPref.getSavedData(this@Export, "tempValue" + PhActivity.DEVICE_ID)
                "Temperature: $data"
            } else {
                "Temperature: " + "null"
            }
            val batteryVal = SharedPref.getSavedData(this@Export, "battery" + PhActivity.DEVICE_ID)
            if (batteryVal != null) {
                if (batteryVal != "") {
                    battery = "$batteryVal %"
//                    binding.batteryPercent.text = "$batteryVal %"
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
            val uri = getImageUri(this@Export, imgBit)
            try {
                val add = getPath(uri)
                val imageData = ImageDataFactory.create(add)
                val image = Image(imageData).setHeight(80f).setWidth(80f)
                //                table12.addCell(new Cell(2, 1).add(image));
                // Adding image to the document
                document.add(image)
            } catch (e: MalformedURLException) {
                e.printStackTrace()
            }
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
        while (userCSV.moveToNext()) {
            val Time = userCSV.getString(userCSV.getColumnIndex("time"))
            var Date = userCSV.getString(userCSV.getColumnIndex("date"))
            val activity = userCSV.getString(userCSV.getColumnIndex("useraction"))
            val Ph = userCSV.getString(userCSV.getColumnIndex("ph"))
            val Temp = userCSV.getString(userCSV.getColumnIndex("temperature"))
            val Mv = userCSV.getString(userCSV.getColumnIndex("mv"))
            val device = userCSV.getString(userCSV.getColumnIndex("deviceID"))
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
        document.add(Paragraph("Operator Sign                                                                                      Supervisor Sign"))
        val imgBit1 = getSignImage()
        if (imgBit1 != null) {
            val uri1 = getImageUri(this@Export, imgBit1)
            try {
                val add = getPath(uri1)
                val imageData1 = ImageDataFactory.create(add)
                val image1 = Image(imageData1).setHeight(80f).setWidth(80f)
                //                table12.addCell(new Cell(2, 1).add(image));
                // Adding image to the document
                document.add(image1)
            } catch (e: MalformedURLException) {
                e.printStackTrace()
            }
        }
        document.close()
        Toast.makeText(this@Export, "Pdf generated", Toast.LENGTH_SHORT).show()
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