package com.aican.aicanapp.ph.phFragment

import android.Manifest.permission
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.aican.aicanapp.ProbeScanner
import com.aican.aicanapp.R
import com.aican.aicanapp.adapters.CalibFileAdapter
import com.aican.aicanapp.adapters.PDF_CSV_Adapter
import com.aican.aicanapp.data.DatabaseHelper
import com.aican.aicanapp.dataClasses.BufferData
import com.aican.aicanapp.dataClasses.CalibDatClass
import com.aican.aicanapp.databinding.FragmentPhCalibNewBinding
import com.aican.aicanapp.dialogs.EditPhBufferDialog
import com.aican.aicanapp.dialogs.UserAuthDialog
import com.aican.aicanapp.ph.PHCalibGraph
import com.aican.aicanapp.ph.PhActivity
import com.aican.aicanapp.ph.PhMvTable
import com.aican.aicanapp.ph.phAnim.PhView
import com.aican.aicanapp.roomDatabase.daoObjects.UserActionDao
import com.aican.aicanapp.roomDatabase.daoObjects.UserDao
import com.aican.aicanapp.roomDatabase.database.AppDatabase
import com.aican.aicanapp.roomDatabase.entities.UserActionEntity
import com.aican.aicanapp.utils.AlarmConstants
import com.aican.aicanapp.utils.Constants
import com.aican.aicanapp.utils.SharedKeys
import com.aican.aicanapp.utils.SharedPref
import com.aican.aicanapp.utils.Source
import com.aican.aicanapp.viewModels.SharedViewModel
import com.aican.aicanapp.websocket.WebSocketManager
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
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class PhCalibFragmentNew : Fragment() {

    companion object {
        var PH_MODE = "both"
        private var line = 0
        var wrong_5 = false

        private var LOG_INTERVAL = 0f
        private var LOG_INTERVAL_3 = 0f

        var ph_mode_selected = 5
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPhCalibNewBinding.inflate(inflater, container, false)
        return binding.root;
    }

    private lateinit var fragmentContext: Context
    lateinit var mode: String
    var currentBuf = 0
    private val sharedViewModel: SharedViewModel by activityViewModels()


    override fun onAttach(context: Context) {
        super.onAttach(context)
        fragmentContext = context
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeAllViews(view)
        jsonData = JSONObject()
        databaseHelper = DatabaseHelper(requireContext())


        Constants.OFFLINE_DATA = true
        Constants.OFFLINE_MODE = true

        /////
        val exportDir =
            File(ContextWrapper(requireContext()).externalMediaDirs[0].toString() + File.separator + "/LabApp/CalibrationData")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }


        phGraphOnClick()

        calibrateButtons()

        phMvTable.setOnClickListener {
            addUserAction(
                "username: " + Source.userName + ", Role: " + Source.userRole +
                        ", clicked on PhMvTable button", "", "", "", ""
            )
            val intent = Intent(fragmentContext, PhMvTable::class.java)
            startActivity(intent)
        }



        showCalibPDFs()

        binding.printCalibData.visibility = View.VISIBLE
        binding.printCSV.visibility = View.VISIBLE

        if (Source.EXPORT_CSV) {
            binding.printCSV.visibility = View.VISIBLE

        } else {
            binding.printCSV.visibility = View.GONE

        }

        if (Source.EXPORT_PDF) {
            binding.printCalibData.visibility = View.VISIBLE

        } else {
            binding.printCalibData.visibility = View.GONE

        }

        // Set click listener for the button
        printCalibData.setOnClickListener {
            try {
                Source.showLoading(requireActivity(), false, false, "Generating pdf...",
                    false)

                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        addUserAction(
                            "username: " + Source.userName + ", Role: " + Source.userRole +
                                    ", print calib report pdf", "", "", "", ""
                        )

                        generatePDF()
                        launch(Dispatchers.Main) {
                            showCalibPDFs()

                            Source.cancelLoading()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        launch(Dispatchers.Main) {
                            Source.cancelLoading()
                            Toast.makeText(fragmentContext, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
//                generatePDF()
//                printCalibCSV()


            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
            // Call the function to show calibration PDFs after generating
        }


        binding.printCSV.setOnClickListener {
            try {
                Source.showLoading(requireActivity(), false, false, "Generating csv...",
                    false)

                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        addUserAction(
                            "username: " + Source.userName + ", Role: " + Source.userRole +
                                    ", print calib report csv", "", "", "", ""
                        )
                        printCalibCSV()

                        launch(Dispatchers.Main) {
                            showCalibPDFs()

                            Source.cancelLoading()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        launch(Dispatchers.Main) {
                            Source.cancelLoading()
                            Toast.makeText(fragmentContext, "Failed to generate csv", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
//                generatePDF()



            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
            // Call the function to show calibration PDFs after generating
        }

        /////


    }

    private fun showCalibPDFs() {
        val pathPDF =
            ContextWrapper(requireContext()).externalMediaDirs[0].toString() + File.separator + "/LabApp/CalibrationData/"
        val rootPDF = File(pathPDF)
        fileNotWrite(rootPDF)
        val filesAndFoldersPDF = rootPDF.listFiles() ?: return

        // Sort the files based on last modified timestamp in descending order
        val sortedFiles = filesAndFoldersPDF.sortedByDescending { it.lastModified() }

        // Find the first PDF file
        val pdfFile =
            sortedFiles.firstOrNull { it.name.endsWith(".pdf") || it.name.endsWith(".csv") }

        // Set up RecyclerView with adapter
        calibFileAdapter = PDF_CSV_Adapter(requireContext(), sortedFiles.toTypedArray(), "PhCalib")
        calibRecyclerView.adapter = calibFileAdapter
        calibFileAdapter.notifyDataSetChanged()
        calibRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun showCalibPDFs1() {
        val path =
            ContextWrapper(requireContext()).externalMediaDirs[0].toString() + File.separator + "/LabApp/CalibrationData"
        val root = File(path)
        val filesAndFolders = root.listFiles()
        if (filesAndFolders == null || filesAndFolders.isEmpty()) {
            Toast.makeText(requireContext(), "No Files Found", Toast.LENGTH_SHORT).show()
            return
        }

        val pathPDF =
            ContextWrapper(requireContext()).externalMediaDirs[0].toString() + File.separator + "/LabApp/CalibrationData/"
        val rootPDF = File(pathPDF)
        fileNotWrite(root)
        val filesAndFoldersPDF = rootPDF.listFiles()
        calibFileAdapter = PDF_CSV_Adapter(
            requireContext().applicationContext, reverseFileArray(filesAndFoldersPDF), "PhCalib"
        )
        calibRecyclerView.adapter = calibFileAdapter
        calibFileAdapter.notifyDataSetChanged()
        calibRecyclerView.layoutManager = LinearLayoutManager(requireContext().applicationContext)
    }

    private fun updateMessage(message: String) {
        sharedViewModel.messageLiveData.value = message
    }

    private fun updateError(error: String) {
        sharedViewModel.errorLiveData.value = error
    }

    public fun websocketData() {


        WebSocketManager.setCloseListener { i, s, b ->
            sharedViewModel.closeConnectionLiveData.value = s + ""

        }
        WebSocketManager.setOpenListener {
            sharedViewModel.openConnectionLiveData.value = ""
        }

        WebSocketManager.setErrorListener { error ->
            requireActivity().runOnUiThread {

                updateError(error.toString())
            }
        }
        WebSocketManager.setMessageListener { message ->
            connectedWebsocket = true


            requireActivity().runOnUiThread {
                try {
                    updateMessage(message)
                    jsonData = JSONObject(message)
                    Log.d("JSONReceived:PHFragment", "onMessage: $message")

                    if (jsonData.has("BATTERY") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                        val battery: String = jsonData.getString("BATTERY")
//                        binding.batteryPercent.setText("$battery %")
                        SharedPref.saveData(
                            requireContext(),
                            "battery" + PhActivity.DEVICE_ID,
                            battery
                        )

                    }

                    if (jsonData.has("SLOPE") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                        if (jsonData.getString("SLOPE") != "nan" && PhFragment.validateNumber(
                                jsonData.getString("SLOPE")
                            )
                        ) {
                            val finalSlopes = jsonData.getString("SLOPE")
//                            Toast.makeText(requireContext(), "" + finalSlopes, Toast.LENGTH_SHORT)
//                                .show()
                            SharedPref.saveData(
                                requireContext(), "SLOPE_" + PhActivity.DEVICE_ID, finalSlopes
                            )
                            finalSlope.text = finalSlopes
                        }
                    }
                    if (jsonData.has("OFFSET") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                        if (jsonData.getString("OFFSET") != "nan" && PhFragment.validateNumber(
                                jsonData.getString("OFFSET")
                            )
                        ) {
                            val finalSlopes = jsonData.getString("OFFSET")
//                            Toast.makeText(requireContext(), "" + finalSlopes, Toast.LENGTH_SHORT)
//                                .show()
                            SharedPref.saveData(
                                requireContext(), "OFFSET_" + PhActivity.DEVICE_ID, finalSlopes
                            )
//                            finalSlope.text = finalSlopes
                        }
                    }
                    if (spin.selectedItemPosition == 0) {
                        if (jsonData.has("PH_VAL") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            var ph = 0.0f
                            if (jsonData.getString("PH_VAL") != "nan" && PhFragment.validateNumber(
                                    jsonData.getString("PH_VAL")
                                )
                            ) {
                                ph = jsonData.getString("PH_VAL").toFloat()
                            }
                            val phForm = String.format(Locale.UK, "%.2f", ph)
                            SharedPref.saveData(
                                requireContext(),
                                "phValue" + PhActivity.DEVICE_ID,
                                phForm.toString()
                            )
                            tvPhCurr.text = phForm
                            phView.moveTo(ph)
                            AlarmConstants.PH = ph
                        }
                        if (jsonData.has("TEMP_VAL") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            var tempVal = 0.0f
                            if (jsonData.getString("TEMP_VAL") != "nan" && PhFragment.validateNumber(
                                    jsonData.getString("TEMP_VAL")
                                )
                            ) {
                                tempVal = jsonData.getString("TEMP_VAL").toFloat()
                                val tempForm = String.format(Locale.UK, "%.1f", tempVal)
                                Log.e("NullCheck", "" + tempToggleSharedPref)

                                requireActivity().runOnUiThread {
//                                    Toast.makeText(requireContext(), "" + tempForm, Toast.LENGTH_SHORT).show()
                                }

                                if (tempToggleSharedPref != null) {
                                    if (tempToggleSharedPref == "true") {
                                        tvTempCurr.text = "$tempForm°C"
                                        SharedPref.saveData(
                                            requireContext(),
                                            "tempValue" + PhActivity.DEVICE_ID,
                                            tempForm
                                        )
                                        if (tempVal <= -127.0) {
                                            tvTempCurr.text = "NA"
                                            SharedPref.saveData(
                                                requireContext(),
                                                "tempValue" + PhActivity.DEVICE_ID,
                                                "NA"
                                            )
                                        }
                                    }
                                } else {
                                    tvTempCurr.text = "$tempForm°C"
                                    SharedPref.saveData(
                                        requireContext(),
                                        "tempValue" + PhActivity.DEVICE_ID,
                                        tempForm
                                    )
                                    if (tempVal <= -127.0) {
                                        tvTempCurr.text = "NA"
                                        SharedPref.saveData(
                                            requireContext(),
                                            "tempValue" + PhActivity.DEVICE_ID,
                                            "NA"
                                        )
                                    }
                                }
                            } else {
                                tvTempCurr.text = "nan"
//                                SharedPref.saveData(
//                                    requireContext(), "tempValue" + PhActivity.DEVICE_ID, "nan"
//                                )
                            }
                        }
                        if (jsonData.has("EC_VAL") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("EC_VAL")
                            tvEcCurr.text = `val`
                        }
                        if (jsonData.has("MV_1") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("MV_1")
                            var ecForm = "0"
                            ecForm = if (`val` == "nan" && !PhFragment.validateNumber(`val`)) {
                                "nan"
                            } else {
                                String.format(
                                    Locale.UK, "%.2f", `val`.toFloat()
                                )
                            }
                            mv1.text = ecForm
                            mV1 = mv1.text.toString()
                            Log.d("test1", mV1)
                            val sharedPreferences = fragmentContext.getSharedPreferences(
                                "CalibPrefs", Context.MODE_PRIVATE
                            )
                            val myEdit = sharedPreferences.edit()
                            myEdit.putString("MV1", mV1)
                            myEdit.commit()
                        }
                        if (jsonData.has("MV_2") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("MV_2")
                            var v = `val`
                            if (`val` != "nan" && PhFragment.validateNumber(`val`)) {
                                v = String.format(
                                    Locale.UK, "%.2f", `val`.toFloat()
                                )
                            }
                            mv2.text = v
                            mV2 = mv2.text.toString()
                            Log.d("test2", mV2)
                            val sharedPreferences = fragmentContext.getSharedPreferences(
                                "CalibPrefs", Context.MODE_PRIVATE
                            )
                            val myEdit = sharedPreferences.edit()
                            myEdit.putString("MV2", mV2)
                            myEdit.commit()
                        }
                        if (jsonData.has("MV_3") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("MV_3")
                            var v = `val`
                            if (`val` != "nan" && PhFragment.validateNumber(`val`)) {
                                v = String.format(
                                    Locale.UK, "%.2f", `val`.toFloat()
                                )
                            }
                            mv3.text = v
                            mV3 = mv3.text.toString()
                            Log.d("test3", mV3)
                            val sharedPreferences = fragmentContext.getSharedPreferences(
                                "CalibPrefs", Context.MODE_PRIVATE
                            )
                            val myEdit = sharedPreferences.edit()
                            myEdit.putString("MV3", mV3)
                            myEdit.commit()
                        }
                        if (jsonData.has("MV_4") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("MV_4")
                            var v = `val`
                            if (`val` != "nan" && PhFragment.validateNumber(`val`)) {
                                v = String.format(
                                    Locale.UK, "%.2f", `val`.toFloat()
                                )
                            }
                            mv4.text = v
                            mV4 = mv4.text.toString()
                            Log.d("test4", mV4)
                            val sharedPreferences = fragmentContext.getSharedPreferences(
                                "CalibPrefs", Context.MODE_PRIVATE
                            )
                            val myEdit = sharedPreferences.edit()
                            myEdit.putString("MV4", mV4)
                            myEdit.commit()
                        }
                        if (jsonData.has("MV_5") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("MV_5")
                            var v = `val`
                            if (`val` != "nan" && PhFragment.validateNumber(`val`)) {
                                v = String.format(
                                    Locale.UK, "%.2f", `val`.toFloat()
                                )
                            }
                            mv5.text = v
                            mV5 = mv5.text.toString()
                            Log.d("test5", mV5)
                            val sharedPreferences = fragmentContext.getSharedPreferences(
                                "CalibPrefs", Context.MODE_PRIVATE
                            )
                            val myEdit = sharedPreferences.edit()
                            myEdit.putString("MV5", mV5)
                            myEdit.commit()
                        }
                        if (jsonData.has("POST_VAL_1") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("POST_VAL_1")
                            var v = `val`
                            if (`val` != "nan" && PhFragment.validateNumber(`val`)) {
                                v = String.format(
                                    Locale.UK, "%.2f", `val`.toFloat()
                                )
                            }
                            phAfterCalib1.text = v
                            pHAC1 = phAfterCalib1.text.toString()
                            val calibDatClass = CalibDatClass(
                                1,
                                ph1.text.toString(),
                                mv1.text.toString(),
                                slope1.text.toString(),
                                dt1.text.toString(),
                                bufferD1.text.toString(),
                                phAfterCalib1.text.toString(),
                                tvTempCurr.text.toString(),
                                if (dt1.text.toString().length >= 15) dt1.text.toString()
                                    .substring(0, 10) else "--",
                                if (dt1.text.toString().length >= 15) dt1.text.toString()
                                    .substring(11, 16) else "--"
                            )
                            databaseHelper.updateClbOffDataFive(calibDatClass)
                            val sharedPreferences = fragmentContext.getSharedPreferences(
                                "CalibPrefs", Context.MODE_PRIVATE
                            )
                            val myEdit = sharedPreferences.edit()
                            myEdit.putString("pHAC1", pHAC1)
                            myEdit.commit()
                        }
                        if (jsonData.has("POST_VAL_2") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("POST_VAL_2")
                            var v = `val`
                            if (`val` != "nan" && PhFragment.validateNumber(`val`)) {
                                v = String.format(
                                    Locale.UK, "%.2f", `val`.toFloat()
                                )
                            }
                            phAfterCalib2.text = v
                            pHAC2 = phAfterCalib2.text.toString()
                            val calibDatClass = CalibDatClass(
                                2,
                                ph2.text.toString(),
                                mv2.text.toString(),
                                slope2.text.toString(),
                                dt2.text.toString(),
                                bufferD2.text.toString(),
                                phAfterCalib2.text.toString(),
                                tvTempCurr.text.toString(),
                                if (dt2.text.toString().length >= 15) dt2.text.toString()
                                    .substring(0, 10) else "--",
                                if (dt2.text.toString().length >= 15) dt2.text.toString()
                                    .substring(11, 16) else "--"
                            )
                            databaseHelper.updateClbOffDataFive(calibDatClass)
                            val sharedPreferences = fragmentContext.getSharedPreferences(
                                "CalibPrefs", Context.MODE_PRIVATE
                            )
                            val myEdit = sharedPreferences.edit()
                            myEdit.putString("pHAC2", pHAC2)
                            myEdit.commit()
                        }
                        if (jsonData.has("SLOPE_1") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("SLOPE_1")
                            var v = "--"
                            if (jsonData.getString("SLOPE_1") != "nan" && PhFragment.validateNumber(
                                    jsonData.getString("SLOPE_1")
                                )
                            ) {
                                v = String.format(
                                    Locale.UK, "%.2f", `val`.toFloat()
                                )
                            }
                            slope2.text = v
                            val calibDatClass = CalibDatClass(
                                2,
                                ph2.text.toString(),
                                mv2.text.toString(),
                                slope2.text.toString(),
                                dt2.text.toString(),
                                bufferD2.text.toString(),
                                phAfterCalib2.text.toString(),
                                tvTempCurr.text.toString(),
                                if (dt2.text.toString().length >= 15) dt2.text.toString()
                                    .substring(0, 10) else "--",
                                if (dt2.text.toString().length >= 15) dt2.text.toString()
                                    .substring(11, 16) else "--"
                            )
                            databaseHelper.updateClbOffDataFive(calibDatClass)
                        }
                        if (jsonData.has("SLOPE_2") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("SLOPE_2")
                            var v = "--"
                            if (jsonData.getString("SLOPE_2") != "nan" && PhFragment.validateNumber(
                                    jsonData.getString("SLOPE_2")
                                )
                            ) {
                                v = String.format(
                                    Locale.UK, "%.2f", `val`.toFloat()
                                )
                            }
                            slope3.text = v
                            val calibDatClass = CalibDatClass(
                                3,
                                ph3.text.toString(),
                                mv3.text.toString(),
                                slope3.text.toString(),
                                dt3.text.toString(),
                                bufferD3.text.toString(),
                                phAfterCalib3.text.toString(),
                                tvTempCurr.text.toString(),
                                if (dt3.text.toString().length >= 15) dt3.text.toString()
                                    .substring(0, 10) else "--",
                                if (dt3.text.toString().length >= 15) dt3.text.toString()
                                    .substring(11, 16) else "--"
                            )
                            databaseHelper.updateClbOffDataFive(calibDatClass)
                        }
                        if (jsonData.has("POST_VAL_3") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("POST_VAL_3")
                            var v = "--"
                            if (jsonData.getString("POST_VAL_3") != "nan" && PhFragment.validateNumber(
                                    jsonData.getString("POST_VAL_3")
                                )
                            ) {
                                v = String.format(
                                    Locale.UK, "%.2f", `val`.toFloat()
                                )
                            }
                            phAfterCalib3.text = v
                            pHAC3 = phAfterCalib3.text.toString()
                            val calibDatClass = CalibDatClass(
                                3,
                                ph3.text.toString(),
                                mv3.text.toString(),
                                slope3.text.toString(),
                                dt3.text.toString(),
                                bufferD3.text.toString(),
                                phAfterCalib3.text.toString(),
                                tvTempCurr.text.toString(),
                                if (dt3.text.toString().length >= 15) dt3.text.toString()
                                    .substring(0, 10) else "--",
                                if (dt3.text.toString().length >= 15) dt3.text.toString()
                                    .substring(11, 16) else "--"
                            )
                            databaseHelper.updateClbOffDataFive(calibDatClass)
                            val sharedPreferences = fragmentContext.getSharedPreferences(
                                "CalibPrefs", Context.MODE_PRIVATE
                            )
                            val myEdit = sharedPreferences.edit()
                            myEdit.putString("pHAC3", pHAC3)
                            myEdit.commit()
                        }
                        if (jsonData.has("SLOPE_3") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("SLOPE_3")
                            var v = "--"
                            if (jsonData.getString("SLOPE_3") != "nan" && PhFragment.validateNumber(
                                    jsonData.getString("SLOPE_3")
                                )
                            ) {
                                v = String.format(
                                    Locale.UK, "%.2f", `val`.toFloat()
                                )
                            }
                            slope4.text = v
                            val calibDatClass = CalibDatClass(
                                4,
                                ph4.text.toString(),
                                mv4.text.toString(),
                                slope4.text.toString(),
                                dt4.text.toString(),
                                bufferD4.text.toString(),
                                phAfterCalib4.text.toString(),
                                tvTempCurr.text.toString(),
                                if (dt4.text.toString().length >= 15) dt4.text.toString()
                                    .substring(0, 10) else "--",
                                if (dt4.text.toString().length >= 15) dt4.text.toString()
                                    .substring(11, 16) else "--"
                            )
                            databaseHelper.updateClbOffDataFive(calibDatClass)
                        }
                        if (jsonData.has("POST_VAL_4") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("POST_VAL_4")
                            var v = "--"
                            if (jsonData.getString("POST_VAL_4") != "nan" && PhFragment.validateNumber(
                                    jsonData.getString("POST_VAL_4")
                                )
                            ) {
                                v = String.format(
                                    Locale.UK, "%.2f", `val`.toFloat()
                                )
                            }
                            phAfterCalib4.text = v
                            pHAC4 = phAfterCalib4.text.toString()
                            val calibDatClass = CalibDatClass(
                                4,
                                ph4.text.toString(),
                                mv4.text.toString(),
                                slope4.text.toString(),
                                dt4.text.toString(),
                                bufferD4.text.toString(),
                                phAfterCalib4.text.toString(),
                                tvTempCurr.text.toString(),
                                if (dt4.text.toString().length >= 15) dt4.text.toString()
                                    .substring(0, 10) else "--",
                                if (dt4.text.toString().length >= 15) dt4.text.toString()
                                    .substring(11, 16) else "--"
                            )
                            databaseHelper.updateClbOffDataFive(calibDatClass)
                            val sharedPreferences = fragmentContext.getSharedPreferences(
                                "CalibPrefs", Context.MODE_PRIVATE
                            )
                            val myEdit = sharedPreferences.edit()
                            myEdit.putString("pHAC4", pHAC4)
                            myEdit.commit()
                        }
                        if (jsonData.has("SLOPE_4") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("SLOPE_4")
                            var v = "--"
                            if (jsonData.getString("SLOPE_4") != "nan" && PhFragment.validateNumber(
                                    jsonData.getString("SLOPE_4")
                                )
                            ) {
                                v = String.format(
                                    Locale.UK, "%.2f", `val`.toFloat()
                                )
                            }
                            slope5.text = v
                            val calibDatClass = CalibDatClass(
                                5,
                                ph5.text.toString(),
                                mv5.text.toString(),
                                slope5.text.toString(),
                                dt5.text.toString(),
                                bufferD5.text.toString(),
                                phAfterCalib5.text.toString(),
                                tvTempCurr.text.toString(),
                                if (dt5.text.toString().length >= 15) dt5.text.toString()
                                    .substring(0, 10) else "--",
                                if (dt5.text.toString().length >= 15) dt5.text.toString()
                                    .substring(11, 16) else "--"
                            )
                            databaseHelper.updateClbOffDataFive(calibDatClass)
                        }
                        if (jsonData.has("POST_VAL_5") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("POST_VAL_5")
                            var v = `val`
                            if (`val` != "nan" && PhFragment.validateNumber(`val`)) {
                                v = String.format(
                                    Locale.UK, "%.2f", `val`.toFloat()
                                )
                            }
                            phAfterCalib5.text = v
                            pHAC5 = phAfterCalib5.text.toString()
                            val calibDatClass = CalibDatClass(
                                5,
                                ph5.text.toString(),
                                mv5.text.toString(),
                                slope5.text.toString(),
                                dt5.text.toString(),
                                bufferD5.text.toString(),
                                phAfterCalib5.text.toString(),
                                tvTempCurr.text.toString(),
                                if (dt5.text.toString().length >= 15) dt5.text.toString()
                                    .substring(0, 10) else "--",
                                if (dt5.text.toString().length >= 15) dt5.text.toString()
                                    .substring(11, 16) else "--"
                            )
                            databaseHelper.updateClbOffDataFive(calibDatClass)
                            val sharedPreferences = fragmentContext.getSharedPreferences(
                                "CalibPrefs", Context.MODE_PRIVATE
                            )
                            val myEdit = sharedPreferences.edit()
                            myEdit.putString("pHAC5", pHAC5)
                            myEdit.commit()
                        }
                        if (jsonData.has("DT_1") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("DT_1")
                            dt1.text = `val`
                            DT1 = dt1.text.toString()
                            val sharedPreferences = fragmentContext.getSharedPreferences(
                                "CalibPrefs", Context.MODE_PRIVATE
                            )
                            val myEdit = sharedPreferences.edit()
                            myEdit.putString("DT1", DT1)
                            myEdit.commit()
                        }
                        if (jsonData.has("DT_2") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("DT_2")
                            dt2.text = `val`
                            DT2 = dt2.text.toString()
                            val sharedPreferences = fragmentContext.getSharedPreferences(
                                "CalibPrefs", Context.MODE_PRIVATE
                            )
                            val myEdit = sharedPreferences.edit()
                            myEdit.putString("DT2", DT2)
                            myEdit.commit()
                        }
                        if (jsonData.has("DT_3") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("DT_3")
                            dt3.text = `val`
                            DT3 = dt3.text.toString()
                            val sharedPreferences = fragmentContext.getSharedPreferences(
                                "CalibPrefs", Context.MODE_PRIVATE
                            )
                            val myEdit = sharedPreferences.edit()
                            myEdit.putString("DT3", DT3)
                            myEdit.commit()
                        }
                        if (jsonData.has("DT_4") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("DT_4")
                            dt4.text = `val`
                            DT4 = dt4.text.toString()
                            val sharedPreferences = fragmentContext.getSharedPreferences(
                                "CalibPrefs", Context.MODE_PRIVATE
                            )
                            val myEdit = sharedPreferences.edit()
                            myEdit.putString("DT4", DT4)
                            myEdit.commit()
                        }
                        if (jsonData.has("DT_5") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("DT_5")
                            dt5.text = `val`
                            DT5 = dt5.text.toString()
                            val sharedPreferences = fragmentContext.getSharedPreferences(
                                "CalibPrefs", Context.MODE_PRIVATE
                            )
                            val myEdit = sharedPreferences.edit()
                            myEdit.putString("DT5", DT5)
                            myEdit.commit()
                        }
                        if (jsonData.has("B_1") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("B_1")
                            ph1.text = `val`
                            PH1 = ph1.text.toString()
                            val sharedPreferences = fragmentContext.getSharedPreferences(
                                "CalibPrefs", Context.MODE_PRIVATE
                            )
                            val myEdit = sharedPreferences.edit()
                            myEdit.putString("PH1", PH1)
                            myEdit.commit()
                        }
                        if (jsonData.has("B_2") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("B_2")
                            ph2.text = `val`
                            PH2 = ph2.text.toString()
                            val sharedPreferences = fragmentContext.getSharedPreferences(
                                "CalibPrefs", Context.MODE_PRIVATE
                            )
                            val myEdit = sharedPreferences.edit()
                            myEdit.putString("PH2", PH2)
                            myEdit.commit()
                        }
                        if (jsonData.has("B_3") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("B_3")
                            ph3.text = `val`
                            PH3 = ph3.text.toString()
                            val sharedPreferences = fragmentContext.getSharedPreferences(
                                "CalibPrefs", Context.MODE_PRIVATE
                            )
                            val myEdit = sharedPreferences.edit()
                            myEdit.putString("PH3", PH3)
                            myEdit.commit()
                        }
                        if (jsonData.has("B_4") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("B_4")
                            ph4.text = `val`
                            PH4 = ph4.text.toString()
                            val sharedPreferences = fragmentContext.getSharedPreferences(
                                "CalibPrefs", Context.MODE_PRIVATE
                            )
                            val myEdit = sharedPreferences.edit()
                            myEdit.putString("PH4", PH4)
                            myEdit.commit()
                        }
                        if (jsonData.has("B_5") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("B_5")
                            ph5.text = `val`
                            PH5 = ph5.text.toString()
                            val sharedPreferences = fragmentContext.getSharedPreferences(
                                "CalibPrefs", Context.MODE_PRIVATE
                            )
                            val myEdit = sharedPreferences.edit()
                            myEdit.putString("PH5", PH5)
                            myEdit.commit()
                        }
                        if (jsonData.has("CAL") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("CAL")
                            val ec = `val`.toInt()
                            Log.d("ECVal", "onDataChange: $ec")
                            //                            stateChangeModeFive();
                            val sharedPreferences = fragmentContext.getSharedPreferences(
                                "CalibPrefs", Context.MODE_PRIVATE
                            )
                            val myEdit = sharedPreferences.edit()
                            if (jsonData.getString("CAL") == "11" && jsonData.has("POST_VAL_1")) {
                                val d = jsonData.getString("POST_VAL_1")
                                phAfterCalib1.text = d
                                val calibDatClass = CalibDatClass(
                                    1,
                                    ph1.text.toString(),
                                    mv1.text.toString(),
                                    slope1.text.toString(),
                                    dt1.text.toString(),
                                    bufferD1.text.toString(),
                                    phAfterCalib1.text.toString(),
                                    tvTempCurr.text.toString(),
                                    if (dt1.text.toString().length >= 15) dt1.text.toString()
                                        .substring(0, 10) else "--",
                                    if (dt1.text.toString().length >= 15) dt1.text.toString()
                                        .substring(11, 16) else "--"
                                )
                                databaseHelper.updateClbOffDataFive(calibDatClass)
                                SharedPref.saveData(
                                    requireContext(), "tem1", tvTempCurr.text.toString()
                                )
                                SharedPref.saveData(requireContext(), "pHAC1", d)
                                //                                deviceRef.child("Data").child("CALIBRATION_STAT").setValue("incomplete");
                                temp1.text = tvTempCurr.text
                            } else if (jsonData.getString("CAL") == "21" && jsonData.has("POST_VAL_2")) {
                                val d = jsonData.getString("POST_VAL_2")
                                phAfterCalib2.text = d
                                val calibDatClass = CalibDatClass(
                                    2,
                                    ph2.text.toString(),
                                    mv2.text.toString(),
                                    slope2.text.toString(),
                                    dt2.text.toString(),
                                    bufferD2.text.toString(),
                                    phAfterCalib2.text.toString(),
                                    tvTempCurr.text.toString(),
                                    if (dt2.text.toString().length >= 15) dt2.text.toString()
                                        .substring(0, 10) else "--",
                                    if (dt2.text.toString().length >= 15) dt2.text.toString()
                                        .substring(11, 16) else "--"
                                )
                                databaseHelper.updateClbOffDataFive(calibDatClass)
                                SharedPref.saveData(
                                    requireContext(), "tem2", tvTempCurr.text.toString()
                                )
                                SharedPref.saveData(requireContext(), "pHAC2", d)

                                temp2.text = tvTempCurr.text
                            } else if (jsonData.getString("CAL") == "31" && jsonData.has("POST_VAL_3")) {
                                val d = jsonData.getString("POST_VAL_3")
                                phAfterCalib3.text = d
                                val calibDatClass = CalibDatClass(
                                    3,
                                    ph3.text.toString(),
                                    mv3.text.toString(),
                                    slope3.text.toString(),
                                    dt3.text.toString(),
                                    bufferD3.text.toString(),
                                    phAfterCalib3.text.toString(),
                                    tvTempCurr.text.toString(),
                                    if (dt3.text.toString().length >= 15) dt3.text.toString()
                                        .substring(0, 10) else "--",
                                    if (dt3.text.toString().length >= 15) dt3.text.toString()
                                        .substring(11, 16) else "--"
                                )
                                databaseHelper.updateClbOffDataFive(calibDatClass)

                                SharedPref.saveData(
                                    requireContext(), "tem3", tvTempCurr.text.toString()
                                )
                                SharedPref.saveData(requireContext(), "pHAC3", d)

                                temp3.text = tvTempCurr.text
                            } else if (jsonData.getString("CAL") == "41" && jsonData.has("POST_VAL_4")) {
                                val d = jsonData.getString("POST_VAL_4")
                                phAfterCalib4.text = d.toString()
                                val calibDatClass = CalibDatClass(
                                    4,
                                    ph4.text.toString(),
                                    mv4.text.toString(),
                                    slope4.text.toString(),
                                    dt4.text.toString(),
                                    bufferD4.text.toString(),
                                    phAfterCalib4.text.toString(),
                                    tvTempCurr.text.toString(),
                                    if (dt4.text.toString().length >= 15) dt4.text.toString()
                                        .substring(0, 10) else "--",
                                    if (dt4.text.toString().length >= 15) dt4.text.toString()
                                        .substring(11, 16) else "--"
                                )
                                databaseHelper.updateClbOffDataFive(calibDatClass)

                                SharedPref.saveData(
                                    requireContext(), "tem4", tvTempCurr.text.toString()
                                )
                                SharedPref.saveData(requireContext(), "pHAC4", d.toString())


                                temp4.text = tvTempCurr.text
                            } else if (jsonData.getString("CAL") == "51" && jsonData.has("POST_VAL_5")) {
                                val d = jsonData.getString("POST_VAL_5")
                                phAfterCalib5.text = d.toString()
                                val calibDatClass = CalibDatClass(
                                    5,
                                    ph5.text.toString(),
                                    mv5.text.toString(),
                                    slope5.text.toString(),
                                    dt5.text.toString(),
                                    bufferD5.text.toString(),
                                    phAfterCalib5.text.toString(),
                                    tvTempCurr.text.toString(),
                                    if (dt5.text.toString().length >= 15) dt5.text.toString()
                                        .substring(0, 10) else "--",
                                    if (dt5.text.toString().length >= 15) dt5.text.toString()
                                        .substring(11, 16) else "--"
                                )
                                databaseHelper.updateClbOffDataFive(calibDatClass)


                                SharedPref.saveData(
                                    requireContext(), "tem5", tvTempCurr.text.toString()
                                )
                                SharedPref.saveData(requireContext(), "pHAC5", d.toString())

                                temp5.text = tvTempCurr.text
                                //                                deviceRef.child("Data").child("CALIBRATION_STAT").setValue("ok");
//                                calibData()
                            }
                        }
                    } else if (spin.selectedItemPosition == 1) {
                    }
//                    if (Constants.OFFLINE_MODE) {
//                        offlineDataFeeding();
//                    }
                    if (jsonData.has("FAULT") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                        val `val` = jsonData.getString("FAULT")
                        val fault = `val`.toInt()
                        if (fault != null) if (fault == 1) {
                            showAlertDialogButtonClicked()
                        }
                    }

//                    progressDialog.dismiss();
//                    calibrateBtn.setEnabled(true);
//
//                    if (Constants.OFFLINE_MODE){
//                        calibrateBtn.setOnClickListener(v -> {
//                            calibrateFivePointOffline(webSocket);
//                        });
//                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

        }
    }


    private fun calibrateButtons() {


        resetCalibFive.setOnClickListener {
            Source.calibratingNow = false
            //                Intent i = new Intent(requireContext(), PhActivity.class);
            //                i.putExtra("refreshCalib", "y");
            //                i.putExtra(Dashboard.KEY_DEVICE_ID, PhActivity.DEVICE_ID);
            //                startActivity(i);
            //                getActivity().finish();
            jsonData = JSONObject()
            try {
                jsonData.put("CAL", "0")
                jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)

                WebSocketManager.sendMessage(jsonData.toString())
            } catch (e: JSONException) {
                throw java.lang.RuntimeException(e)
            }
            if (ph_mode_selected == 5) {
                line = 0
                log1.setBackgroundColor(Color.GRAY)
                log2.setBackgroundColor(Color.WHITE)
                log3.setBackgroundColor(Color.WHITE)
                log4.setBackgroundColor(Color.WHITE)
                log5.setBackgroundColor(Color.WHITE)
                currentBuf = 0

            } else {
                line = 1
                log2.setBackgroundColor(Color.GRAY)
                log3.setBackgroundColor(Color.WHITE)
                log4.setBackgroundColor(Color.WHITE)
                currentBuf = 1

            }


            //                resetCalibration.resetCalibration();
            calibrateBtn.isEnabled = true
            isCalibrating = false
            phGraph.isEnabled = true
            phMvTable.isEnabled = true
            printCalibData.isEnabled = true
            calibSpinner.isEnabled = true
            spin.isEnabled = true
            Source.calibratingNow = false
            if (timer5 != null) {
                timer5!!.cancel()
            }


            //
            tvTimer.visibility = View.INVISIBLE
        }
        connectedWebsocket = true
        calibrateBtn.setOnClickListener { v: View? ->
            if (Constants.OFFLINE_MODE && Constants.OFFLINE_DATA) {
                if (connectedWebsocket) {

                    calibrateFivePointOffline()
                } else {
                    Toast.makeText(
                        fragmentContext,
                        "Websocket connection is not established yet",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else if (Constants.OFFLINE_DATA) {
                Toast.makeText(
                    fragmentContext,
                    "You can't calibrate you are in offline mode and device is not connect",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
            }
        }


    }

    var deviceID = ""
    var companyName = ""
    var nullEntry = ""
    var reportDate = ""
    var reportTime = ""
    var offset = ""
    var battery = ""
    var slope = ""
    var temp = ""
    var calib_stat = "incomplete"

    private fun getCompanyLogo(): Bitmap? {
        val sh = requireActivity().getSharedPreferences("logo", Context.MODE_PRIVATE)
        val photo = sh.getString("logo_data", "")
        var bitmap: Bitmap? = null
        if (!photo.equals("", ignoreCase = true)) {
            val b: ByteArray = Base64.decode(photo, Base64.DEFAULT)
            bitmap = BitmapFactory.decodeByteArray(b, 0, b.size)
        }
        return bitmap
    }

    private fun printCalibCSV() {
        try {

            val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
            val currentDateandTime = sdf.format(Date())
            val tempPath =
                File(requireContext().externalMediaDirs[0], "/LabApp/CalibrationData")
            tempPath.mkdirs()

            val filePath =
                File(tempPath, "CD_$currentDateandTime-${tempPath.listFiles()?.size ?: 0}.csv")
            val writer = CSVWriter(FileWriter(filePath))

            val db = databaseHelper.writableDatabase

            var company_name1 = ""
            val companyname = SharedPref.getSavedData(requireContext(), "COMPANY_NAME")
            company_name1 =
                if (!companyname.isNullOrEmpty()) "Company: $companyname" else "Company: N/A"

            val newOffset = if (SharedPref.getSavedData(
                    requireContext(), "OFFSET_" + PhActivity.DEVICE_ID
                ) != null && SharedPref.getSavedData(
                    requireContext(), "OFFSET_" + PhActivity.DEVICE_ID
                ) != ""
            ) {
                val data =
                    SharedPref.getSavedData(requireContext(), "OFFSET_" + PhActivity.DEVICE_ID)
                "Offset: $data"
            } else {
                "Offset: " + "0"
            }

            val newSlope = if (SharedPref.getSavedData(
                    requireContext(), "SLOPE_" + PhActivity.DEVICE_ID
                ) != null && SharedPref.getSavedData(
                    requireContext(), "SLOPE_" + PhActivity.DEVICE_ID
                ) != ""
            ) {
                val data =
                    SharedPref.getSavedData(requireContext(), "SLOPE_" + PhActivity.DEVICE_ID)
                "Slope: $data"
            } else {
                "Slope: " + "0"
            }

            val tempData =
                SharedPref.getSavedData(requireContext(), "tempValue" + PhActivity.DEVICE_ID)

            val newTemp = if (SharedPref.getSavedData(
                    requireContext(), "tempValue" + PhActivity.DEVICE_ID
                ) != null && SharedPref.getSavedData(
                    requireContext(), "tempValue" + PhActivity.DEVICE_ID
                ) != ""
            ) {

                "Temperature: $tempData"
            } else {
                "Temperature: " + "0"
            }

            val batteryVal =
                SharedPref.getSavedData(requireContext(), "battery" + PhActivity.DEVICE_ID)
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


            writer.close()

//            Toast.makeText(requireContext(), "CSV file exported", Toast.LENGTH_SHORT).show()


        } catch (e: IOException) {
//            Toast.makeText(
//                requireContext(),
//                "Error exporting CSV file: ${e.message}",
//                Toast.LENGTH_SHORT
//            ).show()
            e.printStackTrace()
        }
    }

    @Throws(FileNotFoundException::class)
    private fun generatePDF() {

//        Toast.makeText(requireContext(), "Printing...", Toast.LENGTH_LONG).show()

        if (SharedPref.getSavedData(
                getContext(), "COMPANY_NAME"
            ) != null && SharedPref.getSavedData(
                getContext(), "COMPANY_NAME"
            ) != "N/A"
        ) {
            companyName = SharedPref.getSavedData(getContext(), "COMPANY_NAME");
        } else {
            companyName = "N/A";
        }
        var company_name = ""
        company_name = "Company: $companyName"
        var user_name = ""
        if (Source.cfr_mode) {

            user_name = "Report generated by: " + Source.userName
        }
        val device_id = "DeviceID: ${PhActivity.DEVICE_ID}"
        val calib_by = "Calibrated by: " + Source.calib_completed_by
        reportDate = "Date: " + SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        reportTime = "Time: " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val shp = fragmentContext.getSharedPreferences("Extras", Context.MODE_PRIVATE)
        offset = "Offset: " + shp.getString("offset", "")
        if (Constants.OFFLINE_DATA) {
            if (SharedPref.getSavedData(
                    requireContext(), "OFFSET_" + PhActivity.DEVICE_ID
                ) != null && SharedPref.getSavedData(
                    requireContext(), "OFFSET_" + PhActivity.DEVICE_ID
                ) !== ""
            ) {
                val data =
                    SharedPref.getSavedData(requireContext(), "OFFSET_" + PhActivity.DEVICE_ID)
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
                    requireContext(), "SLOPE_" + PhActivity.DEVICE_ID
                ) != null && SharedPref.getSavedData(
                    requireContext(), "SLOPE_" + PhActivity.DEVICE_ID
                ) !== ""
            ) {
                val data =
                    SharedPref.getSavedData(requireContext(), "SLOPE_" + PhActivity.DEVICE_ID)
                slope = "Slope: $data"
            } else {
                slope = "Slope: " + "null"
            }
            if (SharedPref.getSavedData(
                    requireContext(), "tempValue" + PhActivity.DEVICE_ID
                ) != null && SharedPref.getSavedData(
                    requireContext(), "tempValue" + PhActivity.DEVICE_ID
                ) !== ""
            ) {
                val data =
                    SharedPref.getSavedData(requireContext(), "tempValue" + PhActivity.DEVICE_ID)
                temp = "Temperature: $data"
            } else {
                temp = "Temperature: " + "null"
            }

            val batteryVal =
                SharedPref.getSavedData(requireContext(), "battery" + PhActivity.DEVICE_ID)
            if (batteryVal != null) {
                if (batteryVal != "") {
                    battery = "Battery: $batteryVal %"
                } else {
                    battery = "Battery: 0 %"
                }
            }


//            "TEMP_VAL_"+PhActivity.DEVICE_ID
        } else {
            slope = "Slope: " + shp.getString("slope", "")
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
        val currentDateandTime = sdf.format(Date())
        val tempPath =
            ContextWrapper(requireContext()).externalMediaDirs[0].toString() + File.separator + "/LabApp/CalibrationData"
        val tempRoot = File(tempPath)
        fileNotWrite(tempRoot)
        val tempFilesAndFolders = tempRoot.listFiles()
        val fileName =
            ContextWrapper(requireContext()).externalMediaDirs[0].toString() + File.separator + "/LabApp/CalibrationData/CD_" + currentDateandTime + "_" + ((tempFilesAndFolders?.size
                ?: 0) - 1) + ".pdf"
        val file = File(fileName)
        val outputStream: OutputStream = FileOutputStream(file)
        val writer = PdfWriter(file)
        val pdfDocument = PdfDocument(writer)
        val document = Document(pdfDocument)


//        Text text = new Text(company_name);
//        Text text1 = new Text(user_name);
//        Text text2 = new Text(device_id);
//
//
//
        if (Constants.OFFLINE_MODE || Constants.OFFLINE_DATA) {
//            document.add(Paragraph("Offline Mode"))
        }
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
                reportDate + "  |  " + reportTime + "\n" + offset + "  |  " + battery + "\n" + slope + "  |  " + temp
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
        if (Constants.OFFLINE_MODE || Constants.OFFLINE_DATA) {
//            calibCSV = db.rawQuery("SELECT * FROM CalibOfflineData", null);
            if (spin.selectedItemPosition == 0 || PhCalibFragmentNew.ph_mode_selected == 5) {
                calibCSV = db.rawQuery("SELECT * FROM CalibOfflineDataFive", null)
            }
            if (spin.selectedItemPosition == 1 || ph_mode_selected == 3) {
                calibCSV = db.rawQuery("SELECT * FROM CalibOfflineDataFive", null)
            }
        } else {
            calibCSV = db.rawQuery("SELECT * FROM CalibData", null)
        }
        var jk = 1
        while (calibCSV != null && calibCSV.moveToNext()) {
            val ph = calibCSV.getString(calibCSV.getColumnIndex("PH"))
            val mv = calibCSV.getString(calibCSV.getColumnIndex("MV"))
            val date = calibCSV.getString(calibCSV.getColumnIndex("DT"))
            val slope = calibCSV.getString(calibCSV.getColumnIndex("SLOPE"))
            val pHAC = calibCSV.getString(calibCSV.getColumnIndex("pHAC"))
            val temperature1 = calibCSV.getString(calibCSV.getColumnIndex("temperature"))
            if (ph_mode_selected == 3) {
                if (jk in 2..4) {
                    table.addCell(ph)
                    table.addCell(pHAC + "")
                    table.addCell(slope + "")
                    table.addCell(mv)
                    table.addCell(date)
                    table.addCell(temperature1)
                }
            } else {
                table.addCell(ph)
                table.addCell(pHAC + "")
                table.addCell(slope + "")
                table.addCell(mv)
                table.addCell(date)
                table.addCell(temperature1)
            }
            jk++

        }
        document.add(table)
        if (Constants.OFFLINE_DATA) {
            if (SharedPref.getSavedData(
                    requireContext(), "CALIB_STAT" + PhActivity.DEVICE_ID
                ) != null && SharedPref.getSavedData(
                    requireContext(), "CALIB_STAT" + PhActivity.DEVICE_ID
                ) !== ""
            ) {
                val calibSTat = SharedPref.getSavedData(
                    requireContext(), "CALIB_STAT" + PhActivity.DEVICE_ID
                )
                document.add(Paragraph("Calibration : $calibSTat"))
            } else {
//                document.add(new Paragraph("Calibration : " + calib_stat));
            }
        } else {

        }
        val leftDesignationString =
            SharedPref.getSavedData(requireContext(), SharedKeys.LEFT_DESIGNATION_KEY)
        val rightDesignationString =
            SharedPref.getSavedData(requireContext(), SharedKeys.RIGHT_DESIGNATION_KEY)

        if (leftDesignationString != null && leftDesignationString != "") {

        } else {
            SharedPref.saveData(
                requireContext(),
                SharedKeys.LEFT_DESIGNATION_KEY,
                "Operator Sign"
            )
        }

        if (rightDesignationString != null && rightDesignationString != "") {
        } else {
            SharedPref.saveData(
                requireContext(),
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
        val imgBit1: Bitmap? = getSignImage()

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
//        Toast.makeText(fragmentContext, "Pdf generated", Toast.LENGTH_SHORT).show()
    }


    private fun getSignImage(): Bitmap? {
        val sh = fragmentContext.getSharedPreferences("signature", Context.MODE_PRIVATE)
        val photo = sh.getString("signature_data", "")
        var bitmap: Bitmap? = null
        if (!photo.isNullOrEmpty()) {
            val b = Base64.decode(photo, Base64.DEFAULT)
            bitmap = BitmapFactory.decodeByteArray(b, 0, b.size)
        }
        return bitmap
    }


    var isCalibrating = false
    var timer5: CountDownTimer? = null
    val handler55 = Handler()
    lateinit var runnable55: Runnable

    var bufferList = ArrayList<BufferData>()
    var bufferListThree = ArrayList<BufferData>()

    var calValues = intArrayOf(10, 20, 30, 40, 50)
    var calValuesThree = intArrayOf(20, 30, 40)

    private fun calibrateFivePointOffline() {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        calibrateBtn.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(), R.color.black
            )
        )
        calibrateBtn.isEnabled = false
        tvTimer.visibility = View.VISIBLE
        isCalibrating = true

        addUserAction(
            "username: " + Source.userName + ", Role: " + Source.userRole +
                    ", started calibration mode 5, of buffer " + currentBuf, "", "", "", ""
        )

        timer5 = object : CountDownTimer(5000, 1000) {
            //45000
            //        timer5 = new CountDownTimer(5000, 1000) { //45000
            override fun onTick(millisUntilFinished: Long) {
                var millisUntilFinished = millisUntilFinished
                calibrateBtn.isEnabled = false
                millisUntilFinished /= 1000
                val min = millisUntilFinished.toInt() / 60
                val sec = millisUntilFinished.toInt() % 60
                val time = String.format(Locale.UK, "%02d:%02d", min, sec)
                tvTimer.text = time
                Log.e("lineN", line.toString() + "")
                Source.calibratingNow = true
                phGraph.isEnabled = false
                phMvTable.isEnabled = false
                printCalibData.isEnabled = false
                calibSpinner.isEnabled = false
                spin.isEnabled = false
                if (line == -1) {
                    log1.setBackgroundColor(Color.WHITE)
                    log2.setBackgroundColor(Color.WHITE)
                    log3.setBackgroundColor(Color.WHITE)
                    log4.setBackgroundColor(Color.WHITE)
                    log5.setBackgroundColor(Color.WHITE)
                }
                if (line == 0) {
                    log1.setBackgroundColor(Color.GRAY)
                    log2.setBackgroundColor(Color.WHITE)
                    log3.setBackgroundColor(Color.WHITE)
                    log4.setBackgroundColor(Color.WHITE)
                    log5.setBackgroundColor(Color.WHITE)
                    jsonData = JSONObject()
                    try {
                        jsonData.put("B_1", ph1.text.toString())
                        jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                        Log.e("ThisIsNotAnError", jsonData.getString("B_1"))
                        WebSocketManager.sendMessage(jsonData.toString())
                    } catch (e: JSONException) {
                        throw java.lang.RuntimeException(e)
                    }
                    val mv1Text = binding.mv1.text.toString()
                    if (PhFragment.validateNumber(binding.mv1.text.toString()) && PhFragment.validateNumber(
                            mMaxMV1
                        )
                        && PhFragment.validateNumber(mMinMV1)
                    ) {
                        if (mv1Text.toFloat() <= mMaxMV1.toFloat() && mv1Text.toFloat() >= mMinMV1.toFloat()) {
                            wrong_5 = false
                            // Toast.makeText(fragmentContext, "In Range", Toast.LENGTH_SHORT).show()
                        } else {
                            wrong_5 = true
                            timer5!!.cancel()
                            // handler33.removeCallbacks(this)
                            // wrong_3 = false
                            calibrateBtn.isEnabled = true
                            showAlertDialogButtonClicked()
                            Toast.makeText(fragmentContext, "Out of Range", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }


                }
                if (line == 1) {
                    log1.setBackgroundColor(Color.WHITE)
                    log2.setBackgroundColor(Color.GRAY)
                    log3.setBackgroundColor(Color.WHITE)
                    log4.setBackgroundColor(Color.WHITE)
                    log5.setBackgroundColor(Color.WHITE)
                    jsonData = JSONObject()
                    try {
                        jsonData.put("B_2", ph2.text.toString())
                        jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                        Log.e("ThisIsNotAnError", jsonData.getString("B_2"))
                        WebSocketManager.sendMessage(jsonData.toString())
                    } catch (e: JSONException) {
                        throw java.lang.RuntimeException(e)
                    }

                    val mv2Text = binding.mv2.text.toString()
                    if (PhFragment.validateNumber(binding.mv2.text.toString()) && PhFragment.validateNumber(
                            mMaxMV2
                        )
                        && PhFragment.validateNumber(mMinMV2)
                    ) {
                        if (mv2Text.toFloat() <= mMaxMV2.toFloat() && mv2Text.toFloat() >= mMinMV2.toFloat()) {
                            wrong_5 = false
                            // Toast.makeText(fragmentContext, "In Range", Toast.LENGTH_SHORT).show()
                        } else {
                            wrong_5 = true
                            timer5!!.cancel()
                            // handler33.removeCallbacks(this)
                            // wrong_3 = false
                            calibrateBtn.isEnabled = true
                            showAlertDialogButtonClicked()
                            Toast.makeText(fragmentContext, "Out of Range", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }


                }
                if (line == 2) {
                    log1.setBackgroundColor(Color.WHITE)
                    log2.setBackgroundColor(Color.WHITE)
                    log3.setBackgroundColor(Color.GRAY)
                    log4.setBackgroundColor(Color.WHITE)
                    log5.setBackgroundColor(Color.WHITE)
                    jsonData = JSONObject()
                    try {
                        jsonData.put("B_3", ph3.text.toString())
                        jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                        Log.e("ThisIsNotAnError", jsonData.getString("B_3"))
                        WebSocketManager.sendMessage(jsonData.toString())
                    } catch (e: JSONException) {
                        throw java.lang.RuntimeException(e)
                    }

                    val mv3Text = binding.mv3.text.toString()
                    if (PhFragment.validateNumber(binding.mv3.text.toString()) && PhFragment.validateNumber(
                            mMaxMV3
                        )
                        && PhFragment.validateNumber(mMinMV3)
                    ) {
                        if (mv3Text.toFloat() <= mMaxMV3.toFloat() && mv3Text.toFloat() >= mMinMV3.toFloat()) {
                            wrong_5 = false
                            // Toast.makeText(fragmentContext, "In Range", Toast.LENGTH_SHORT).show()
                        } else {
                            wrong_5 = true
                            timer5!!.cancel()
                            // handler33.removeCallbacks(this)
                            // wrong_3 = false
                            calibrateBtn.isEnabled = true
                            showAlertDialogButtonClicked()
                            Toast.makeText(fragmentContext, "Out of Range", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }


                }
                if (line == 3) {
                    log1.setBackgroundColor(Color.WHITE)
                    log2.setBackgroundColor(Color.WHITE)
                    log3.setBackgroundColor(Color.WHITE)
                    log4.setBackgroundColor(Color.GRAY)
                    log5.setBackgroundColor(Color.WHITE)
                    jsonData = JSONObject()
                    try {
                        jsonData.put("B_4", ph4.text.toString())
                        jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                        Log.e("ThisIsNotAnError", jsonData.getString("B_4"))
                        WebSocketManager.sendMessage(jsonData.toString())
                    } catch (e: JSONException) {
                        throw java.lang.RuntimeException(e)
                    }

                    val mv4Text = binding.mv4.text.toString()
                    if (PhFragment.validateNumber(binding.mv4.text.toString()) && PhFragment.validateNumber(
                            mMaxMV4
                        )
                        && PhFragment.validateNumber(mMinMV4)
                    ) {
                        if (mv4Text.toFloat() <= mMaxMV4.toFloat() && mv4Text.toFloat() >= mMinMV4.toFloat()) {
                            wrong_5 = false
                            // Toast.makeText(fragmentContext, "In Range", Toast.LENGTH_SHORT).show()
                        } else {
                            wrong_5 = true
                            timer5!!.cancel()
                            // handler33.removeCallbacks(this)
                            // wrong_3 = false
                            calibrateBtn.isEnabled = true
                            showAlertDialogButtonClicked()
                            Toast.makeText(fragmentContext, "Out of Range", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }


                }
                if (line == 4) {
                    log1.setBackgroundColor(Color.WHITE)
                    log2.setBackgroundColor(Color.WHITE)
                    log3.setBackgroundColor(Color.WHITE)
                    log4.setBackgroundColor(Color.WHITE)
                    log5.setBackgroundColor(Color.GRAY)
                    jsonData = JSONObject()
                    try {
                        jsonData.put("B_5", ph5.text.toString())
                        jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                        Log.e("ThisIsNotAnError", jsonData.getString("B_5"))
                        WebSocketManager.sendMessage(jsonData.toString())
                    } catch (e: JSONException) {
                        throw java.lang.RuntimeException(e)
                    }

                    val mv5Text = binding.mv5.text.toString()
                    if (PhFragment.validateNumber(binding.mv5.text.toString()) && PhFragment.validateNumber(
                            mMaxMV5
                        )
                        && PhFragment.validateNumber(mMinMV5)
                    ) {
                        if (mv5Text.toFloat() <= mMaxMV5.toFloat() && mv5Text.toFloat() >= mMinMV5.toFloat()) {
                            wrong_5 = false
                            // Toast.makeText(fragmentContext, "In Range", Toast.LENGTH_SHORT).show()
                        } else {
                            wrong_5 = true
                            timer5!!.cancel()
                            // handler33.removeCallbacks(this)
                            // wrong_3 = false
                            calibrateBtn.isEnabled = true
                            showAlertDialogButtonClicked()
                            Toast.makeText(fragmentContext, "Out of Range", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }


                }
                if (line > 4) {
                    log1.setBackgroundColor(Color.WHITE)
                    log2.setBackgroundColor(Color.WHITE)
                    log3.setBackgroundColor(Color.WHITE)
                    log4.setBackgroundColor(Color.WHITE)
                    log5.setBackgroundColor(Color.WHITE)
                    PhCalibFragmentNew.wrong_5 = false
                }
                PhCalibFragmentNew.wrong_5 = false
            }

            override fun onFinish() {
                runnable55 = object : Runnable {
                    override fun run() {
                        try {
                            Source.calibratingNow = false
                            phGraph.isEnabled = true
                            phMvTable.isEnabled = true
                            printCalibData.isEnabled = true
                            calibSpinner.isEnabled = true
                            spin.isEnabled = true
                            if (ph_mode_selected == 5) {
                                if (!PhCalibFragmentNew.wrong_5) {
                                    PhCalibFragmentNew.wrong_5 = false
                                    line = currentBuf + 1
                                    if (currentBuf == 4) {
                                        val date123 =
                                            SimpleDateFormat(
                                                "yyyy-MM-dd",
                                                Locale.getDefault()
                                            ).format(
                                                Date()
                                            )
                                        val time123 =
                                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(
                                                Date()
                                            )

                                        dt5.text = "$date123 $time123"
                                        jsonData = JSONObject()
                                        val object1 = JSONObject()
                                        jsonData.put("DT_5", "$date123 $time123")
                                        jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                                        WebSocketManager.sendMessage(jsonData.toString())
                                        SharedPref.saveData(
                                            requireContext(),
                                            "CALIB_STAT" + PhActivity.DEVICE_ID,
                                            "completed"
                                        )

//                                    deviceRef.child("UI").child("PH").child("PH_CAL").child("DT_5").setValue(date123 + " " + time123);
                                        calibrateBtn.isEnabled = false
                                        Source.calib_completed_by = Source.logUserName
                                        calibrateBtn.text = "DONE"
                                        startTimer()
                                    }


                                    if (currentBuf == 0) {
                                        val date123 =
                                            SimpleDateFormat(
                                                "yyyy-MM-dd",
                                                Locale.getDefault()
                                            ).format(
                                                Date()
                                            )
                                        val time123 =
                                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(
                                                Date()
                                            )
                                        dt1.text = "$date123 $time123"
                                        jsonData = JSONObject()
                                        val object1 = JSONObject()
                                        jsonData.put("DT_1", "$date123 $time123")
                                        jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                                        WebSocketManager.sendMessage(jsonData.toString())
                                        SharedPref.saveData(
                                            requireContext(),
                                            "CALIB_STAT" + PhActivity.DEVICE_ID,
                                            "incomplete"
                                        )
                                        //                                    deviceRef.child("UI").child("PH").child("PH_CAL").child("DT_1").setValue(date123 + " " + time123);
                                        log1.setBackgroundColor(Color.WHITE)
                                        log2.setBackgroundColor(Color.GRAY)
                                        log3.setBackgroundColor(Color.WHITE)
                                        log4.setBackgroundColor(Color.WHITE)
                                        log5.setBackgroundColor(Color.WHITE)
                                    }
                                    if (currentBuf == 1) {
                                        val date123 =
                                            SimpleDateFormat(
                                                "yyyy-MM-dd",
                                                Locale.getDefault()
                                            ).format(
                                                Date()
                                            )
                                        val time123 =
                                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(
                                                Date()
                                            )
                                        dt2.text = "$date123 $time123"
                                        jsonData = JSONObject()
                                        val object1 = JSONObject()
                                        jsonData.put("DT_2", "$date123 $time123")
                                        jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                                        WebSocketManager.sendMessage(jsonData.toString())
                                        SharedPref.saveData(
                                            requireContext(),
                                            "CALIB_STAT" + PhActivity.DEVICE_ID,
                                            "incomplete"
                                        )
                                        //                                    deviceRef.child("UI").child("PH").child("PH_CAL").child("DT_2").setValue(date123 + " " + time123);
                                        log1.setBackgroundColor(Color.WHITE)
                                        log2.setBackgroundColor(Color.WHITE)
                                        log3.setBackgroundColor(Color.GRAY)
                                        log4.setBackgroundColor(Color.WHITE)
                                        log5.setBackgroundColor(Color.WHITE)
                                    }
                                    if (currentBuf == 2) {
                                        val date123 =
                                            SimpleDateFormat(
                                                "yyyy-MM-dd",
                                                Locale.getDefault()
                                            ).format(
                                                Date()
                                            )
                                        val time123 =
                                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(
                                                Date()
                                            )
                                        dt3.text = "$date123 $time123"
                                        jsonData = JSONObject()
                                        val object1 = JSONObject()
                                        jsonData.put("DT_3", "$date123 $time123")
                                        jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                                        WebSocketManager.sendMessage(jsonData.toString())

//                                    deviceRef.child("UI").child("PH").child("PH_CAL").child("DT_3").setValue(date123 + " " + time123);
                                        SharedPref.saveData(
                                            requireContext(),
                                            "CALIB_STAT" + PhActivity.DEVICE_ID,
                                            "incomplete"
                                        )
                                        log1.setBackgroundColor(Color.WHITE)
                                        log2.setBackgroundColor(Color.WHITE)
                                        log3.setBackgroundColor(Color.WHITE)
                                        log4.setBackgroundColor(Color.GRAY)
                                        log5.setBackgroundColor(Color.WHITE)
                                    }
                                    if (currentBuf == 3) {
                                        val date123 =
                                            SimpleDateFormat(
                                                "yyyy-MM-dd",
                                                Locale.getDefault()
                                            ).format(
                                                Date()
                                            )
                                        val time123 =
                                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(
                                                Date()
                                            )
                                        dt4.text = "$date123 $time123"
                                        jsonData = JSONObject()
                                        val object1 = JSONObject()
                                        jsonData.put("DT_4", "$date123 $time123")
                                        jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                                        WebSocketManager.sendMessage(jsonData.toString())
                                        if (ph_mode_selected == 5) {
                                            SharedPref.saveData(
                                                requireContext(),
                                                "CALIB_STAT" + PhActivity.DEVICE_ID,
                                                "incomplete"
                                            )
                                        } else {
                                            SharedPref.saveData(
                                                requireContext(),
                                                "CALIB_STAT" + PhActivity.DEVICE_ID,
                                                "incomplete"
                                            )
                                        }
                                        //                                    deviceRef.child("UI").child("PH").child("PH_CAL").child("DT_4").setValue(date123 + " " + time123);
                                        log1.setBackgroundColor(Color.WHITE)
                                        log2.setBackgroundColor(Color.WHITE)
                                        log3.setBackgroundColor(Color.WHITE)
                                        log4.setBackgroundColor(Color.WHITE)
                                        log5.setBackgroundColor(Color.GRAY)
                                    }
                                    calibrateBtn.isEnabled = true
                                    tvTimer.visibility = View.INVISIBLE
                                    val currentTime = SimpleDateFormat(
                                        "yyyy-MM-dd HH:mm", Locale.getDefault()
                                    ).format(
                                        Date()
                                    )
                                    bufferList.add(BufferData(null, null, currentTime))
                                    //                        bufferListThree.add(new BufferData(null, null, currentTime));
                                    jsonData = JSONObject()
                                    val object0 = JSONObject()
                                    jsonData.put("CAL", (calValues.get(currentBuf) + 1).toString())
                                    jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                                    WebSocketManager.sendMessage(jsonData.toString())
                                    //                                deviceRef.child("UI").child("PH").child("PH_CAL").child("CAL").setValue(calValues[currentBuf] + 1);
                                    Log.e("cValue", currentBuf.toString() + "")


//                                int b = currentBuf < 0 ? 4 : currentBuf;
                                    val b = currentBuf
                                    Log.e("cValue2", currentBuf.toString() + "")
                                    Log.e("bValue", b.toString() + "")

//                                deviceRef.child("UI").child("PH").child("PH_CAL").child(postCoeffLabels[b]).get().addOnSuccessListener(dataSnapshot2 -> {
//                                    Float postCoeff = dataSnapshot2.getValue(Float.class);
                                    val sharedPreferences = fragmentContext.getSharedPreferences(
                                        "CalibPrefs", Context.MODE_PRIVATE
                                    )
                                    val myEdit = sharedPreferences.edit()
                                    if (b == 0) {
                                        myEdit.putString("tem1", tvTempCurr.text.toString())
                                        myEdit.commit()
                                        jsonData = JSONObject()
                                        jsonData.put("CALIBRATION_STAT", "incomplete")
                                        jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                                        WebSocketManager.sendMessage(jsonData.toString())
                                        //                                    deviceRef.child("Data").child("CALIBRATION_STAT").setValue("incomplete");
                                        val calibDatClass = CalibDatClass(
                                            1,
                                            ph1.text.toString(),
                                            mv1.text.toString(),
                                            slope1.text.toString(),
                                            dt1.text.toString(),
                                            bufferD1.text.toString(),
                                            phAfterCalib1.text.toString(),
                                            tvTempCurr.text.toString(),
                                            if (dt1.text.toString().length >= 15) dt1.text.toString()
                                                .substring(0, 10) else "--",
                                            if (dt1.text.toString().length >= 15) dt1.text.toString()
                                                .substring(11, 16) else "--"
                                        )
                                        databaseHelper.updateClbOffDataFive(calibDatClass)
                                        temp1.text = tvTempCurr.text
                                    } else if (b == 1) {
                                        myEdit.putString("tem2", tvTempCurr.text.toString())
                                        myEdit.commit()
                                        val calibDatClass = CalibDatClass(
                                            2,
                                            ph2.text.toString(),
                                            mv2.text.toString(),
                                            slope2.text.toString(),
                                            dt2.text.toString(),
                                            bufferD2.text.toString(),
                                            phAfterCalib2.text.toString(),
                                            tvTempCurr.text.toString(),
                                            if (dt2.text.toString().length >= 15) dt2.text.toString()
                                                .substring(0, 10) else "--",
                                            if (dt2.text.toString().length >= 15) dt2.text.toString()
                                                .substring(11, 16) else "--"
                                        )
                                        databaseHelper.updateClbOffDataFive(calibDatClass)
                                        temp2.text = tvTempCurr.text
                                    } else if (b == 2) {
                                        myEdit.putString("tem3", tvTempCurr.text.toString())
                                        myEdit.commit()
                                        val calibDatClass = CalibDatClass(
                                            3,
                                            ph3.text.toString(),
                                            mv3.text.toString(),
                                            slope3.text.toString(),
                                            dt3.text.toString(),
                                            bufferD3.text.toString(),
                                            phAfterCalib3.text.toString(),
                                            tvTempCurr.text.toString(),
                                            if (dt3.text.toString().length >= 15) dt3.text.toString()
                                                .substring(0, 10) else "--",
                                            if (dt3.text.toString().length >= 15) dt3.text.toString()
                                                .substring(11, 16) else "--"
                                        )
                                        databaseHelper.updateClbOffDataFive(calibDatClass)
                                        temp3.text = tvTempCurr.text
                                    } else if (b == 3) {
                                        myEdit.putString("tem4", tvTempCurr.text.toString())
                                        myEdit.commit()
                                        val calibDatClass = CalibDatClass(
                                            4,
                                            ph4.text.toString(),
                                            mv4.text.toString(),
                                            slope4.text.toString(),
                                            dt4.text.toString(),
                                            bufferD4.text.toString(),
                                            phAfterCalib4.text.toString(),
                                            tvTempCurr.text.toString(),
                                            if (dt4.text.toString().length >= 15) dt4.text.toString()
                                                .substring(0, 10) else "--",
                                            if (dt4.text.toString().length >= 15) dt4.text.toString()
                                                .substring(11, 16) else "--"
                                        )
                                        databaseHelper.updateClbOffDataFive(calibDatClass)
                                        temp4.text = tvTempCurr.text
                                    } else if (b == 4) {
                                        myEdit.putString("tem5", tvTempCurr.text.toString())
                                        myEdit.commit()
                                        temp5.text = tvTempCurr.text
                                        jsonData = JSONObject()
                                        jsonData.put("CALIBRATION_STAT", "ok")
                                        jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                                        WebSocketManager.sendMessage(jsonData.toString())
                                        //                                    deviceRef.child("Data").child("CALIBRATION_STAT").setValue("ok");
//                                    calibData()
                                        val calibDatClass = CalibDatClass(
                                            5,
                                            ph5.text.toString(),
                                            mv5.text.toString(),
                                            slope5.text.toString(),
                                            dt5.text.toString(),
                                            bufferD5.text.toString(),
                                            phAfterCalib5.text.toString(),
                                            tvTempCurr.text.toString(),
                                            if (dt5.text.toString().length >= 15) dt5.text.toString()
                                                .substring(0, 10) else "--",
                                            if (dt5.text.toString().length >= 15) dt5.text.toString()
                                                .substring(11, 16) else "--"
                                        )
                                        databaseHelper.updateClbOffDataFive(calibDatClass)
                                        databaseHelper.insertCalibrationOfflineAllData(
                                            ph1.text.toString(),
                                            mv1.text.toString(),
                                            slope1.text.toString(),
                                            dt1.text.toString(),
                                            bufferD1.text.toString(),
                                            phAfterCalib1.text.toString(),
                                            tvTempCurr.text.toString(),
                                            if (dt1.text.toString().length >= 15) dt1.text.toString()
                                                .substring(0, 10) else "--",
                                            if (dt1.text.toString().length >= 15) dt1.text.toString()
                                                .substring(11, 16) else "--"
                                        )
                                        databaseHelper.insertCalibrationOfflineAllData(
                                            ph2.text.toString(),
                                            mv2.text.toString(),
                                            slope2.text.toString(),
                                            dt2.text.toString(),
                                            bufferD2.text.toString(),
                                            phAfterCalib2.text.toString(),
                                            tvTempCurr.text.toString(),
                                            if (dt2.text.toString().length >= 15) dt2.text.toString()
                                                .substring(0, 10) else "--",
                                            if (dt2.text.toString().length >= 15) dt2.text.toString()
                                                .substring(11, 16) else "--"
                                        )
                                        databaseHelper.insertCalibrationOfflineAllData(
                                            ph3.text.toString(),
                                            mv3.text.toString(),
                                            slope3.text.toString(),
                                            dt3.text.toString(),
                                            bufferD3.text.toString(),
                                            phAfterCalib3.text.toString(),
                                            tvTempCurr.text.toString(),
                                            if (dt3.text.toString().length >= 15) dt3.text.toString()
                                                .substring(0, 10) else "--",
                                            if (dt3.text.toString().length >= 15) dt3.text.toString()
                                                .substring(11, 16) else "--"
                                        )
                                        databaseHelper.insertCalibrationOfflineAllData(
                                            ph4.text.toString(),
                                            mv4.text.toString(),
                                            slope4.text.toString(),
                                            dt4.text.toString(),
                                            bufferD4.text.toString(),
                                            phAfterCalib4.text.toString(),
                                            tvTempCurr.text.toString(),
                                            if (dt4.text.toString().length >= 15) dt4.text.toString()
                                                .substring(0, 10) else "--",
                                            if (dt4.text.toString().length >= 15) dt4.text.toString()
                                                .substring(11, 16) else "--"
                                        )
                                        databaseHelper.insertCalibrationOfflineAllData(
                                            ph5.text.toString(),
                                            mv5.text.toString(),
                                            slope5.text.toString(),
                                            dt5.text.toString(),
                                            bufferD5.text.toString(),
                                            phAfterCalib5.text.toString(),
                                            tvTempCurr.text.toString(),
                                            if (dt5.text.toString().length >= 15) dt5.text.toString()
                                                .substring(0, 10) else "--",
                                            if (dt5.text.toString().length >= 15) dt5.text.toString()
                                                .substring(11, 16) else "--"
                                        )
                                    }
                                    currentBuf += 1
//                                calibData()
                                    deleteAllOfflineCalibData()


                                    databaseHelper.insertCalibrationOfflineData(
                                        ph1.text.toString(),
                                        mv1.text.toString(),
                                        slope1.text.toString(),
                                        dt1.text.toString(),
                                        bufferD1.text.toString(),
                                        phAfterCalib1.text.toString(),
                                        tvTempCurr.text.toString(),
                                        if (dt1.text.toString().length >= 15) dt1.text.toString()
                                            .substring(0, 10) else "--",
                                        if (dt1.text.toString().length >= 15) dt1.text.toString()
                                            .substring(11, 16) else "--"
                                    )
                                    databaseHelper.insertCalibrationOfflineData(
                                        ph2.text.toString(),
                                        mv2.text.toString(),
                                        slope2.text.toString(),
                                        dt2.text.toString(),
                                        bufferD2.text.toString(),
                                        phAfterCalib2.text.toString(),
                                        tvTempCurr.text.toString(),
                                        if (dt2.text.toString().length >= 15) dt2.text.toString()
                                            .substring(0, 10) else "--",
                                        if (dt2.text.toString().length >= 15) dt2.text.toString()
                                            .substring(11, 16) else "--"
                                    )
                                    databaseHelper.insertCalibrationOfflineData(
                                        ph3.text.toString(),
                                        mv3.text.toString(),
                                        slope3.text.toString(),
                                        dt3.text.toString(),
                                        bufferD3.text.toString(),
                                        phAfterCalib3.text.toString(),
                                        tvTempCurr.text.toString(),
                                        if (dt3.text.toString().length >= 15) dt3.text.toString()
                                            .substring(0, 10) else "--",
                                        if (dt3.text.toString().length >= 15) dt3.text.toString()
                                            .substring(11, 16) else "--"
                                    )
                                    databaseHelper.insertCalibrationOfflineData(
                                        ph4.text.toString(),
                                        mv4.text.toString(),
                                        slope4.text.toString(),
                                        dt4.text.toString(),
                                        bufferD4.text.toString(),
                                        phAfterCalib4.text.toString(),
                                        tvTempCurr.text.toString(),
                                        if (dt4.text.toString().length >= 15) dt4.text.toString()
                                            .substring(0, 10) else "--",
                                        if (dt4.text.toString().length >= 15) dt4.text.toString()
                                            .substring(11, 16) else "--"
                                    )
                                    databaseHelper.insertCalibrationOfflineData(
                                        ph5.text.toString(),
                                        mv5.text.toString(),
                                        slope5.text.toString(),
                                        dt5.text.toString(),
                                        bufferD5.text.toString(),
                                        phAfterCalib5.text.toString(),
                                        tvTempCurr.text.toString(),
                                        if (dt5.text.toString().length >= 15) dt5.text.toString()
                                            .substring(0, 10) else "--",
                                        if (dt5.text.toString().length >= 15) dt5.text.toString()
                                            .substring(11, 16) else "--"
                                    )


                                } else {
//                            --line_3;
//                            --currentBufThree;
                                    timer5!!.cancel()
                                    handler55.removeCallbacks(this)
                                    PhCalibFragmentNew.wrong_5 = false
                                    calibrateBtn.isEnabled = true
                                    showAlertDialogButtonClicked()
                                }
                            }
                            if (ph_mode_selected == 3) {
                                if (!PhCalibFragmentNew.wrong_5) {
                                    PhCalibFragmentNew.wrong_5 = false
                                    line = currentBuf + 1
                                    if (currentBuf == 3) {
                                        val date123 =
                                            SimpleDateFormat(
                                                "yyyy-MM-dd",
                                                Locale.getDefault()
                                            ).format(
                                                Date()
                                            )
                                        val time123 =
                                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(
                                                Date()
                                            )

                                        dt4.text = "$date123 $time123"
                                        jsonData = JSONObject()
                                        val object1 = JSONObject()
                                        jsonData.put("DT_4", "$date123 $time123")
                                        jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                                        WebSocketManager.sendMessage(jsonData.toString())
                                        SharedPref.saveData(
                                            requireContext(),
                                            "CALIB_STAT" + PhActivity.DEVICE_ID,
                                            "completed"
                                        )

//                                    deviceRef.child("UI").child("PH").child("PH_CAL").child("DT_5").setValue(date123 + " " + time123);
                                        calibrateBtn.isEnabled = false
                                        Source.calib_completed_by = Source.logUserName
                                        calibrateBtn.text = "DONE"
                                        startTimer()
                                    }


                                    if (currentBuf == 1) {
                                        val date123 =
                                            SimpleDateFormat(
                                                "yyyy-MM-dd",
                                                Locale.getDefault()
                                            ).format(
                                                Date()
                                            )
                                        val time123 =
                                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(
                                                Date()
                                            )
                                        dt2.text = "$date123 $time123"
                                        jsonData = JSONObject()
                                        val object1 = JSONObject()
                                        jsonData.put("DT_2", "$date123 $time123")
                                        jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                                        WebSocketManager.sendMessage(jsonData.toString())
                                        SharedPref.saveData(
                                            requireContext(),
                                            "CALIB_STAT" + PhActivity.DEVICE_ID,
                                            "incomplete"
                                        )
                                        //                                    deviceRef.child("UI").child("PH").child("PH_CAL").child("DT_2").setValue(date123 + " " + time123);
                                        log1.setBackgroundColor(Color.WHITE)
                                        log2.setBackgroundColor(Color.WHITE)
                                        log3.setBackgroundColor(Color.GRAY)
                                        log4.setBackgroundColor(Color.WHITE)
                                        log5.setBackgroundColor(Color.WHITE)
                                    }
                                    if (currentBuf == 2) {
                                        val date123 =
                                            SimpleDateFormat(
                                                "yyyy-MM-dd",
                                                Locale.getDefault()
                                            ).format(
                                                Date()
                                            )
                                        val time123 =
                                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(
                                                Date()
                                            )
                                        dt3.text = "$date123 $time123"
                                        jsonData = JSONObject()
                                        val object1 = JSONObject()
                                        jsonData.put("DT_3", "$date123 $time123")
                                        jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                                        WebSocketManager.sendMessage(jsonData.toString())

//                                    deviceRef.child("UI").child("PH").child("PH_CAL").child("DT_3").setValue(date123 + " " + time123);
                                        SharedPref.saveData(
                                            requireContext(),
                                            "CALIB_STAT" + PhActivity.DEVICE_ID,
                                            "incomplete"
                                        )
                                        log1.setBackgroundColor(Color.WHITE)
                                        log2.setBackgroundColor(Color.WHITE)
                                        log3.setBackgroundColor(Color.WHITE)
                                        log4.setBackgroundColor(Color.GRAY)
                                        log5.setBackgroundColor(Color.WHITE)
                                    }
                                    if (currentBuf == 3) {
                                        val date123 =
                                            SimpleDateFormat(
                                                "yyyy-MM-dd",
                                                Locale.getDefault()
                                            ).format(
                                                Date()
                                            )
                                        val time123 =
                                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(
                                                Date()
                                            )
                                        dt4.text = "$date123 $time123"
                                        jsonData = JSONObject()
                                        val object1 = JSONObject()
                                        jsonData.put("DT_4", "$date123 $time123")
                                        jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                                        WebSocketManager.sendMessage(jsonData.toString())
                                        SharedPref.saveData(
                                            requireContext(),
                                            "CALIB_STAT" + PhActivity.DEVICE_ID,
                                            "incomplete"
                                        )
                                        //                                    deviceRef.child("UI").child("PH").child("PH_CAL").child("DT_4").setValue(date123 + " " + time123);
                                        log1.setBackgroundColor(Color.WHITE)
                                        log2.setBackgroundColor(Color.WHITE)
                                        log3.setBackgroundColor(Color.WHITE)
                                        log4.setBackgroundColor(Color.WHITE)
                                        log5.setBackgroundColor(Color.GRAY)
                                    }

                                    calibrateBtn.isEnabled = true
                                    tvTimer.visibility = View.INVISIBLE
                                    val currentTime = SimpleDateFormat(
                                        "yyyy-MM-dd HH:mm", Locale.getDefault()
                                    ).format(
                                        Date()
                                    )
                                    bufferList.add(BufferData(null, null, currentTime))
                                    //                        bufferListThree.add(new BufferData(null, null, currentTime));
                                    jsonData = JSONObject()
                                    val object0 = JSONObject()
                                    jsonData.put("CAL", (calValues.get(currentBuf) + 1).toString())
                                    jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                                    WebSocketManager.sendMessage(jsonData.toString())
                                    //                                deviceRef.child("UI").child("PH").child("PH_CAL").child("CAL").setValue(calValues[currentBuf] + 1);
                                    Log.e("cValue", currentBuf.toString() + "")


//                                int b = currentBuf < 0 ? 4 : currentBuf;
                                    val b = currentBuf
                                    Log.e("cValue2", currentBuf.toString() + "")
                                    Log.e("bValue", b.toString() + "")

//                                deviceRef.child("UI").child("PH").child("PH_CAL").child(postCoeffLabels[b]).get().addOnSuccessListener(dataSnapshot2 -> {
//                                    Float postCoeff = dataSnapshot2.getValue(Float.class);
                                    val sharedPreferences = fragmentContext.getSharedPreferences(
                                        "CalibPrefs", Context.MODE_PRIVATE
                                    )
                                    val myEdit = sharedPreferences.edit()

                                    if (b == 1) {
                                        myEdit.putString("tem2", tvTempCurr.text.toString())
                                        myEdit.commit()
                                        val calibDatClass = CalibDatClass(
                                            2,
                                            ph2.text.toString(),
                                            mv2.text.toString(),
                                            slope2.text.toString(),
                                            dt2.text.toString(),
                                            bufferD2.text.toString(),
                                            phAfterCalib2.text.toString(),
                                            tvTempCurr.text.toString(),
                                            if (dt2.text.toString().length >= 15) dt2.text.toString()
                                                .substring(0, 10) else "--",
                                            if (dt2.text.toString().length >= 15) dt2.text.toString()
                                                .substring(11, 16) else "--"
                                        )
                                        databaseHelper.updateClbOffDataFive(calibDatClass)
                                        temp2.text = tvTempCurr.text
                                    } else if (b == 2) {
                                        myEdit.putString("tem3", tvTempCurr.text.toString())
                                        myEdit.commit()
                                        val calibDatClass = CalibDatClass(
                                            3,
                                            ph3.text.toString(),
                                            mv3.text.toString(),
                                            slope3.text.toString(),
                                            dt3.text.toString(),
                                            bufferD3.text.toString(),
                                            phAfterCalib3.text.toString(),
                                            tvTempCurr.text.toString(),
                                            if (dt3.text.toString().length >= 15) dt3.text.toString()
                                                .substring(0, 10) else "--",
                                            if (dt3.text.toString().length >= 15) dt3.text.toString()
                                                .substring(11, 16) else "--"
                                        )
                                        databaseHelper.updateClbOffDataFive(calibDatClass)
                                        temp3.text = tvTempCurr.text
                                    } else if (b == 3) {
                                        myEdit.putString("tem4", tvTempCurr.text.toString())
                                        myEdit.commit()
                                        val calibDatClass = CalibDatClass(
                                            4,
                                            ph4.text.toString(),
                                            mv4.text.toString(),
                                            slope4.text.toString(),
                                            dt4.text.toString(),
                                            bufferD4.text.toString(),
                                            phAfterCalib4.text.toString(),
                                            tvTempCurr.text.toString(),
                                            if (dt4.text.toString().length >= 15) dt4.text.toString()
                                                .substring(0, 10) else "--",
                                            if (dt4.text.toString().length >= 15) dt4.text.toString()
                                                .substring(11, 16) else "--"
                                        )
                                        databaseHelper.updateClbOffDataFive(calibDatClass)

                                        temp4.text = tvTempCurr.text
                                        jsonData = JSONObject()
                                        jsonData.put("CALIBRATION_STAT", "ok")
                                        jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                                        WebSocketManager.sendMessage(jsonData.toString())
                                    }

                                    currentBuf += 1
//                                calibData()
                                    deleteAllOfflineCalibData()


                                    databaseHelper.insertCalibrationOfflineData(
                                        ph1.text.toString(),
                                        mv1.text.toString(),
                                        slope1.text.toString(),
                                        dt1.text.toString(),
                                        bufferD1.text.toString(),
                                        phAfterCalib1.text.toString(),
                                        tvTempCurr.text.toString(),
                                        if (dt1.text.toString().length >= 15) dt1.text.toString()
                                            .substring(0, 10) else "--",
                                        if (dt1.text.toString().length >= 15) dt1.text.toString()
                                            .substring(11, 16) else "--"
                                    )
                                    databaseHelper.insertCalibrationOfflineData(
                                        ph2.text.toString(),
                                        mv2.text.toString(),
                                        slope2.text.toString(),
                                        dt2.text.toString(),
                                        bufferD2.text.toString(),
                                        phAfterCalib2.text.toString(),
                                        tvTempCurr.text.toString(),
                                        if (dt2.text.toString().length >= 15) dt2.text.toString()
                                            .substring(0, 10) else "--",
                                        if (dt2.text.toString().length >= 15) dt2.text.toString()
                                            .substring(11, 16) else "--"
                                    )
                                    databaseHelper.insertCalibrationOfflineData(
                                        ph3.text.toString(),
                                        mv3.text.toString(),
                                        slope3.text.toString(),
                                        dt3.text.toString(),
                                        bufferD3.text.toString(),
                                        phAfterCalib3.text.toString(),
                                        tvTempCurr.text.toString(),
                                        if (dt3.text.toString().length >= 15) dt3.text.toString()
                                            .substring(0, 10) else "--",
                                        if (dt3.text.toString().length >= 15) dt3.text.toString()
                                            .substring(11, 16) else "--"
                                    )
                                    databaseHelper.insertCalibrationOfflineData(
                                        ph4.text.toString(),
                                        mv4.text.toString(),
                                        slope4.text.toString(),
                                        dt4.text.toString(),
                                        bufferD4.text.toString(),
                                        phAfterCalib4.text.toString(),
                                        tvTempCurr.text.toString(),
                                        if (dt4.text.toString().length >= 15) dt4.text.toString()
                                            .substring(0, 10) else "--",
                                        if (dt4.text.toString().length >= 15) dt4.text.toString()
                                            .substring(11, 16) else "--"
                                    )
                                    databaseHelper.insertCalibrationOfflineData(
                                        ph5.text.toString(),
                                        mv5.text.toString(),
                                        slope5.text.toString(),
                                        dt5.text.toString(),
                                        bufferD5.text.toString(),
                                        phAfterCalib5.text.toString(),
                                        tvTempCurr.text.toString(),
                                        if (dt5.text.toString().length >= 15) dt5.text.toString()
                                            .substring(0, 10) else "--",
                                        if (dt5.text.toString().length >= 15) dt5.text.toString()
                                            .substring(11, 16) else "--"
                                    )


                                } else {
//                            --line_3;
//                            --currentBufThree;
                                    timer5!!.cancel()
                                    handler55.removeCallbacks(this)
                                    PhCalibFragmentNew.wrong_5 = false
                                    calibrateBtn.isEnabled = true
                                    showAlertDialogButtonClicked()
                                }
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }
                }
                runnable55.run()
            }
        }
        try {
            jsonData = JSONObject()
            jsonData.put("CAL", calValues.get(currentBuf).toString())
            jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
            WebSocketManager.sendMessage(jsonData.toString())
            timer5!!.start()
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    lateinit var handler1: Handler
    private lateinit var runnable1: Runnable

    private fun startTimer() {
        calibrateBtn.isEnabled = false
        printCalibData.isEnabled = false
        binding.printCSV.isEnabled = false
        calibrateBtn.text = "Saving data.."

        PhCalibFragmentNew.LOG_INTERVAL = 6f
        tvTimer.setText(PhCalibFragmentNew.LOG_INTERVAL.toString())
        handler1 = Handler()
        runnable1 = object : Runnable {
            override fun run() {
                Log.d("Runnable", "Handler is working")
                calibrateBtn.isEnabled = false
                if (PhCalibFragmentNew.LOG_INTERVAL == 0f) { // just remove call backs
                    requireActivity().runOnUiThread {
//                        Toast.makeText(requireContext(), "" + phAfterCalib5.text.toString(), Toast.LENGTH_SHORT).show()
                    }
                    tvTimer.setText(PhCalibFragmentNew.LOG_INTERVAL.toString())
                    handler1.removeCallbacks(this)
                    calibrateBtn.isEnabled = true
                    printCalibData.isEnabled = true
                    binding.printCSV.isEnabled = true
                    calibrateBtn.text = "Start"
                    if (ph_mode_selected == 5) {
                        currentBuf = 0
                        line = 0
                        log1.setBackgroundColor(Color.GRAY)
                        log2.setBackgroundColor(Color.WHITE)
                        log3.setBackgroundColor(Color.WHITE)
                        log4.setBackgroundColor(Color.WHITE)
                        log5.setBackgroundColor(Color.WHITE)
                    } else {
                        SharedPref.saveData(
                            requireContext(),
                            "CALIB_STAT" + PhActivity.DEVICE_ID,
                            "completed"
                        )
                        currentBuf = 1
                        line = 1
                        log2.setBackgroundColor(Color.GRAY)
                        log3.setBackgroundColor(Color.WHITE)
                        log4.setBackgroundColor(Color.WHITE)
                    }
                    if (ph_mode_selected == 5) {
                        databaseHelper.insertCalibrationAllDataOffline(
                            ph1.text.toString(),
                            mv1.text.toString(),
                            slope1.text.toString(),
                            dt1.text.toString(),
                            bufferD1.text.toString(),
                            phAfterCalib1.text.toString(),
                            tvTempCurr.text.toString(),
                            if (dt1.text.toString().length >= 15) dt1.text.toString()
                                .substring(0, 10) else "--",
                            if (dt1.text.toString().length >= 15) dt1.text.toString()
                                .substring(11, 16) else "--"
                        )
                        databaseHelper.insertCalibrationAllDataOffline(
                            ph2.text.toString(),
                            mv2.text.toString(),
                            slope2.text.toString(),
                            dt2.text.toString(),
                            bufferD2.text.toString(),
                            phAfterCalib2.text.toString(),
                            tvTempCurr.text.toString(),
                            if (dt2.text.toString().length >= 15) dt2.text.toString()
                                .substring(0, 10) else "--",
                            if (dt2.text.toString().length >= 15) dt2.text.toString()
                                .substring(11, 16) else "--"
                        )
                        databaseHelper.insertCalibrationAllDataOffline(
                            ph3.text.toString(),
                            mv3.text.toString(),
                            slope3.text.toString(),
                            dt3.text.toString(),
                            bufferD3.text.toString(),
                            phAfterCalib3.text.toString(),
                            tvTempCurr.text.toString(),
                            if (dt3.text.toString().length >= 15) dt3.text.toString()
                                .substring(0, 10) else "--",
                            if (dt3.text.toString().length >= 15) dt3.text.toString()
                                .substring(11, 16) else "--"
                        )
                        databaseHelper.insertCalibrationAllDataOffline(
                            ph4.text.toString(),
                            mv4.text.toString(),
                            slope4.text.toString(),
                            dt4.text.toString(),
                            bufferD4.text.toString(),
                            phAfterCalib4.text.toString(),
                            tvTempCurr.text.toString(),
                            if (dt4.text.toString().length >= 15) dt4.text.toString()
                                .substring(0, 10) else "--",
                            if (dt4.text.toString().length >= 15) dt4.text.toString()
                                .substring(11, 16) else "--"
                        )
                        databaseHelper.insertCalibrationAllDataOffline(
                            ph5.text.toString(),
                            mv5.text.toString(),
                            slope5.text.toString(),
                            dt5.text.toString(),
                            bufferD5.text.toString(),
                            phAfterCalib5.text.toString(),
                            tvTempCurr.text.toString(),
                            if (dt5.text.toString().length >= 15) dt5.text.toString()
                                .substring(0, 10) else "--",
                            if (dt5.text.toString().length >= 15) dt5.text.toString()
                                .substring(11, 16) else "--"
                        )
                        databaseHelper.insertCalibrationAllDataOffline(
                            "calibration-ended", "-", "-", "-", "-", "-", "-",
                            if (dt5.text.toString().length >= 15) dt5.text.toString()
                                .substring(0, 10) else "--",
                            if (dt5.text.toString().length >= 15) dt5.text.toString()
                                .substring(11, 16) else "--"
                        )
                    } else {
                        databaseHelper.insertCalibrationAllDataOffline(
                            ph2.text.toString(),
                            mv2.text.toString(),
                            slope2.text.toString(),
                            dt2.text.toString(),
                            bufferD2.text.toString(),
                            phAfterCalib2.text.toString(),
                            tvTempCurr.text.toString(),
                            if (dt2.text.toString().length >= 15) dt2.text.toString()
                                .substring(0, 10) else "--",
                            if (dt2.text.toString().length >= 15) dt2.text.toString()
                                .substring(11, 16) else "--"
                        )
                        databaseHelper.insertCalibrationAllDataOffline(
                            ph3.text.toString(),
                            mv3.text.toString(),
                            slope3.text.toString(),
                            dt3.text.toString(),
                            bufferD3.text.toString(),
                            phAfterCalib3.text.toString(),
                            tvTempCurr.text.toString(),
                            if (dt3.text.toString().length >= 15) dt3.text.toString()
                                .substring(0, 10) else "--",
                            if (dt3.text.toString().length >= 15) dt3.text.toString()
                                .substring(11, 16) else "--"
                        )
                        databaseHelper.insertCalibrationAllDataOffline(
                            ph4.text.toString(),
                            mv4.text.toString(),
                            slope4.text.toString(),
                            dt4.text.toString(),
                            bufferD4.text.toString(),
                            phAfterCalib4.text.toString(),
                            tvTempCurr.text.toString(),
                            if (dt4.text.toString().length >= 15) dt4.text.toString()
                                .substring(0, 10) else "--",
                            if (dt4.text.toString().length >= 15) dt4.text.toString()
                                .substring(11, 16) else "--"
                        )
                        databaseHelper.insertCalibrationAllDataOffline(
                            "calibration-ended", "-", "-", "-", "-", "-", "-",
                            if (dt4.text.toString().length >= 15) dt4.text.toString()
                                .substring(0, 10) else "--",
                            if (dt4.text.toString().length >= 15) dt4.text.toString()
                                .substring(11, 16) else "--"
                        )
                    }

//                    deviceRef.child("UI").child("PH").child("PH_CAL").child("CAL").setValue(0)
                    tvTimer.text = "00:45"
                    Log.d("Runnable", "ok")
                } else { // post again
                    --PhCalibFragmentNew.LOG_INTERVAL
                    tvTimer.text =
                        "00:0" + PhCalibFragmentNew.LOG_INTERVAL.toString().substring(0, 1)
                    handler1.postDelayed(this, 1000)
                }
            }
        }
        runnable1.run()
    }


    fun showAlertDialogButtonClicked() {

        // Create an alert builder
        val builder = AlertDialog.Builder(requireContext())
        // set the custom layout
        val customLayout: View = layoutInflater.inflate(
            R.layout.fault_dialog, null
        )
        builder.setView(customLayout)

        // add a button
        builder.setPositiveButton(
            "Continue Calibration"
        ) { dialog, which ->
            Source.calibratingNow = false
            phMvTable.isEnabled = true
        }
        builder.setNeutralButton(
            "Restart"
        ) { dialog, which ->

            Source.calibratingNow = false
            //                Intent i = new Intent(requireContext(), PhActivity.class);
            //                i.putExtra("refreshCalib", "y");
            //                i.putExtra(Dashboard.KEY_DEVICE_ID, PhActivity.DEVICE_ID);
            //                startActivity(i);
            //                getActivity().finish();
            jsonData = JSONObject()
            try {
                jsonData.put("CAL", "0")
                jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)

                WebSocketManager.sendMessage(jsonData.toString())
            } catch (e: JSONException) {
                throw java.lang.RuntimeException(e)
            }
            line = 0
            currentBuf = 0


            //                resetCalibration.resetCalibration();
            calibrateBtn.isEnabled = true
            isCalibrating = false
            phGraph.isEnabled = true
            phMvTable.isEnabled = true
            printCalibData.isEnabled = true
            calibSpinner.isEnabled = true
            spin.isEnabled = true
            Source.calibratingNow = false
            if (timer5 != null) {
                timer5!!.cancel()
            }
            log1.setBackgroundColor(Color.GRAY)
            log2.setBackgroundColor(Color.WHITE)
            log3.setBackgroundColor(Color.WHITE)
            log4.setBackgroundColor(Color.WHITE)
            log5.setBackgroundColor(Color.WHITE)


            //
            tvTimer.visibility = View.INVISIBLE


//            Source.calibratingNow = false
//            val i = Intent(requireContext(), PhActivity::class.java)
//            i.putExtra("refreshCalib", "y")
//            i.putExtra(Dashboard.KEY_DEVICE_ID, PhActivity.DEVICE_ID)
//            startActivity(i)
//            requireActivity().finish()

            //                        Fragment frg = null;
            //                        frg = requireActivity().getSupportFragmentManager().findFragmentById(R.layout.fragment_ph_calib_new);
            //                        final FragmentTransaction ft = requireActivity().getSupportFragmentManager().beginTransaction();
            //                        if (frg != null) {
            //                            ft.detach(frg);
            //                        }
            //                        ft.attach(frg);
            //                        ft.commit();
        }

        // create and show
        // the alert dialog
        val dialog = builder.create()
        dialog.show()
    }


    fun deleteAllCalibData() {
        val db = databaseHelper.writableDatabase
        db.execSQL("DELETE FROM CalibData")
        db.close()
    }


    fun deleteAllOfflineCalibData() {
        val db = databaseHelper.writableDatabase
        db.execSQL("DELETE FROM CalibOfflineData")
        db.close()
    }

    private fun phGraphOnClick() {
        phGraph.setOnClickListener { v: View? ->
            if (PH1 != "" || PH2 != "" || PH3 != "" || PH4 != "" || PH5 != "" || MV1 != "" || MV2 != "" || MV3 != "" || MV4 != "" || MV5 != "") {
                val i = Intent(fragmentContext, PHCalibGraph::class.java)
                i.putExtra("PH1", PH1)
                i.putExtra("PH2", PH2)
                i.putExtra("PH3", PH3)
                i.putExtra("PH4", PH4)
                i.putExtra("PH5", PH5)
                i.putExtra("MV1", MV1)
                i.putExtra("MV2", MV2)
                i.putExtra("MV3", MV3)
                i.putExtra("MV4", MV4)
                i.putExtra("MV5", MV5)
                startActivity(i)
            } else {
                Toast.makeText(
                    fragmentContext,
                    "Not allow to move further because some values are null, and null values cannot plot the graph",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    }

    private fun setPreviousData() {
        val phVal = SharedPref.getSavedData(requireContext(), "phValue" + PhActivity.DEVICE_ID)

        if (phVal != null) {
            var floatVal = 0.0f
            if (PhFragment.validateNumber(phVal)) {
                floatVal = phVal.toFloat()
                binding.tvPhCurr.text = floatVal.toString()
                binding.phView.moveTo(floatVal)
                AlarmConstants.PH = floatVal
            }
        }

        val tempVal = SharedPref.getSavedData(requireContext(), "tempValue" + PhActivity.DEVICE_ID)
        if (tempVal != null) {
            binding.tvTempCurr.text = "$tempVal °C"
        }


        val slopeVal = SharedPref.getSavedData(requireContext(), "SLOPE_" + PhActivity.DEVICE_ID)

        if (slopeVal != null) {
            binding.finalSlope.text = "$slopeVal %"

        }

        val ecValue = SharedPref.getSavedData(requireContext(), "ecValue" + PhActivity.DEVICE_ID)
        if (ecValue != null) {
            binding.tvEcCurr.text = ecValue
        }


    }

    fun addUserAction(action: String, ph: String, temp: String, mv: String, compound: String) {
        if (Source.cfr_mode) {
            lifecycleScope.launch(Dispatchers.IO) {

                userActionDao.insertUserAction(
                    UserActionEntity(
                        0, Source.getCurrentTime(), Source.getPresentDate(),
                        action, ph, temp, mv, compound, PhActivity.DEVICE_ID.toString()
                    )
                )
            }
        }
    }

    fun updateMinMaxMVs() {

        val mMinMV1 =
            SharedPref.getSavedData(requireContext(), SharedKeys.minMV1 + PhActivity.DEVICE_ID)
        if (mMinMV1 != null) {
            this.mMinMV1 = mMinMV1
        }
        val mMinMV2 =
            SharedPref.getSavedData(requireContext(), SharedKeys.minMV2 + PhActivity.DEVICE_ID)
        if (mMinMV2 != null) {
            this.mMinMV2 = mMinMV2
        }

        val mMinMV3 =
            SharedPref.getSavedData(requireContext(), SharedKeys.minMV3 + PhActivity.DEVICE_ID)
        if (mMinMV3 != null) {
            this.mMinMV3 = mMinMV3
        }

        val mMinMV4 =
            SharedPref.getSavedData(requireContext(), SharedKeys.minMV4 + PhActivity.DEVICE_ID)
        if (mMinMV4 != null) {
            this.mMinMV4 = mMinMV4
        }

        val mMinMV5 =
            SharedPref.getSavedData(requireContext(), SharedKeys.minMV5 + PhActivity.DEVICE_ID)
        if (mMinMV5 != null) {
            this.mMinMV5 = mMinMV5
        }
        val mMaxMV1 =
            SharedPref.getSavedData(requireContext(), SharedKeys.maxMV1 + PhActivity.DEVICE_ID)
        if (mMaxMV1 != null) {
            this.mMaxMV1 = mMaxMV1
        }

        val mMaxMV2 =
            SharedPref.getSavedData(requireContext(), SharedKeys.maxMV2 + PhActivity.DEVICE_ID)
        if (mMaxMV2 != null) {
            this.mMaxMV2 = mMaxMV2
        }

        val mMaxMV3 =
            SharedPref.getSavedData(requireContext(), SharedKeys.maxMV3 + PhActivity.DEVICE_ID)
        if (mMaxMV3 != null) {
            this.mMaxMV3 = mMaxMV3
        }

        val mMaxMV4 =
            SharedPref.getSavedData(requireContext(), SharedKeys.maxMV4 + PhActivity.DEVICE_ID)
        if (mMaxMV4 != null) {
            this.mMaxMV4 = mMaxMV4
        }

        val mMaxMV5 =
            SharedPref.getSavedData(requireContext(), SharedKeys.maxMV5 + PhActivity.DEVICE_ID)
        if (mMaxMV5 != null) {
            this.mMaxMV5 = mMaxMV5
        }
    }

    var mMinMV1: String = ""
    var mMinMV2: String = ""
    var mMinMV3: String = ""
    var mMinMV4: String = ""
    var mMinMV5: String = ""

    var mMaxMV1: String = ""
    var mMaxMV2: String = ""
    var mMaxMV3: String = ""
    var mMaxMV4: String = ""
    var mMaxMV5: String = ""


    lateinit var userDao: UserDao
    lateinit var userActionDao: UserActionDao
    var tempToggleSharedPref: String? = null

    override fun onResume() {
        super.onResume()

        binding.tvTimer.text = "Disconnected"

        WebSocketManager.setCloseListener { i, s, b ->
            requireActivity().runOnUiThread {
                binding.tvTimer.text = "Disconnected"

                Toast.makeText(requireContext(), "Closed", Toast.LENGTH_SHORT).show()
            }
        }

        updateMinMaxMVs()
        tempToggleSharedPref =
            SharedPref.getSavedData(requireContext(), "setTempToggle" + PhActivity.DEVICE_ID)

        setPreviousData()

        websocketData()


        userDao = Room.databaseBuilder(
            requireContext().applicationContext, AppDatabase::class.java, "aican-database"
        ).build().userDao()

        userActionDao = Room.databaseBuilder(
            requireContext().applicationContext,
            AppDatabase::class.java,
            "aican-database"
        ).build().userActionDao()

//        if (Source.cfr_mode) {
//            val userAuthDialog = UserAuthDialog(requireContext(), userDao)
//            userAuthDialog.showLoginDialog { isValidCredentials ->
//                if (isValidCredentials) {
//                    addUserAction(
//                        "username: " + Source.userName + ", Role: " + Source.userRole +
//                                ", entered ph calib fragment", "", "", "", ""
//                    )
//                } else {
//                    requireActivity().runOnUiThread {
//                        Toast.makeText(requireContext(), "Invalid credentials", Toast.LENGTH_SHORT)
//                            .show()
//                    }
//                }
//            }
//        }

        offlineDataFeeding()


    }

    override fun onStart() {
//        initiateSocketConnection();
        val spinselect = arrayOf("5", "3")

        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, spinselect)

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spin.adapter = adapter


        if (Constants.OFFLINE_DATA) {
            PH_MODE = "both"
        }


        when (PH_MODE) {
            "both" -> {
                calibSpinner.visibility = View.VISIBLE
                spin.setSelection(0)
            }

            "5" -> {
                calibSpinner.visibility = View.INVISIBLE
                modeText.text = "Mode : 5 Point"
                spin.setSelection(0)
            }

            "3" -> {
                calibSpinner.visibility = View.INVISIBLE
                modeText.text = "Mode : 3 Point"
                spin.setSelection(1)
            }
        }


        Source.calibMode = 0
        if (Constants.OFFLINE_MODE) {
            try {
                jsonData = JSONObject()
                jsonData.put("CAL_MODE", 5.toString())
                jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                WebSocketManager.sendMessage(jsonData.toString())
            } catch (e: JSONException) {
                throw java.lang.RuntimeException(e)
            }
        }
        try {

            spin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, view: View?, position: Int, id: Long
                ) {

                    view ?: return

                    when (position) {
                        0 -> {

                            PhCalibFragmentNew.ph_mode_selected = 5
                            Log.d("SpinnerSelection", "Selected: $ph_mode_selected")

                            mode = "5"
                            Source.calibMode = 0

                            currentBuf = 0
                            PhCalibFragmentNew.line = 0
                            if (Constants.OFFLINE_MODE) {
                                try {
                                    jsonData = JSONObject()
                                    jsonData.put("CAL_MODE", 5.toString())
                                    jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                                    WebSocketManager.sendMessage(jsonData.toString())
                                } catch (e: JSONException) {
                                    throw java.lang.RuntimeException(e)
                                }
                            }

                            log1.setBackgroundColor(Color.GRAY)
                            log2.setBackgroundColor(Color.WHITE)
                            log3.setBackgroundColor(Color.WHITE)
                            log4.setBackgroundColor(Color.WHITE)
                            log5.setBackgroundColor(Color.WHITE)

                            log1.visibility = View.VISIBLE
                            log5.visibility = View.VISIBLE

                            addUserAction(
                                "username: " + Source.userName + ", Role: " + Source.userRole +
                                        ", switched ph calib mode to 5", "", "", "", ""
                            )

                        }

                        1 -> {
                            PhCalibFragmentNew.ph_mode_selected = 3
                            Log.d("SpinnerSelection", "Selected: $ph_mode_selected")

                            mode = "3"
                            Source.calibMode = 1

                            currentBuf = 1
                            PhCalibFragmentNew.line = 1
                            if (Constants.OFFLINE_MODE) {
                                try {
                                    jsonData = JSONObject()
                                    jsonData.put("CAL_MODE", 3.toString())
                                    jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                                    WebSocketManager.sendMessage(jsonData.toString())
                                } catch (e: JSONException) {
                                    throw java.lang.RuntimeException(e)
                                }
                            }
                            log1.visibility = View.GONE
                            log5.visibility = View.GONE

                            log2.setBackgroundColor(Color.GRAY)
                            log3.setBackgroundColor(Color.WHITE)
                            log4.setBackgroundColor(Color.WHITE)
                            addUserAction(
                                "username: " + Source.userName + ", Role: " + Source.userRole +
                                        ", switched ph calib mode to 3", "", "", "", ""
                            )

                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    Toast.makeText(
                        requireContext(), "Select a mode of Calibration", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: Exception) {
            Log.e("ErrorSpinner", "" + e.message)
//            Toast.makeText(requireContext(), "E: " + e.message, Toast.LENGTH_SHORT).show()
        }
        super.onStart()
    }


    private fun offlineDataFeeding() {
        val db = databaseHelper.writableDatabase
        val calibCSV5: Cursor = db.rawQuery("SELECT * FROM CalibOfflineDataFive", null)
        var index = 0
        if (calibCSV5.count == 0) {
            databaseHelper.insertCalibrationOfflineDataFive(
                1,
                binding.ph1.text.toString(),
                mv1.text.toString(),
                slope1.text.toString(),
                dt1.text.toString(),
                bufferD1.text.toString(),
                phAfterCalib1.text.toString(),
                tvTempCurr.text.toString(),
                if (dt1.text.toString().length >= 15) dt1.text.toString()
                    .substring(0, 10) else "--",
                if (dt1.text.toString().length >= 15) dt1.text.toString()
                    .substring(11, 16) else "--"
            )
            databaseHelper.insertCalibrationOfflineDataFive(
                2,
                ph2.text.toString(),
                mv2.text.toString(),
                slope2.text.toString(),
                dt2.text.toString(),
                bufferD2.text.toString(),
                phAfterCalib2.text.toString(),
                tvTempCurr.text.toString(),
                if (dt2.text.toString().length >= 15) dt2.text.toString()
                    .substring(0, 10) else "--",
                if (dt2.text.toString().length >= 15) dt2.text.toString()
                    .substring(11, 16) else "--"
            )
            databaseHelper.insertCalibrationOfflineDataFive(
                3,
                ph3.text.toString(),
                mv3.text.toString(),
                slope3.text.toString(),
                dt3.text.toString(),
                bufferD3.text.toString(),
                phAfterCalib3.text.toString(),
                tvTempCurr.text.toString(),
                if (dt3.text.toString().length >= 15) dt3.text.toString()
                    .substring(0, 10) else "--",
                if (dt3.text.toString().length >= 15) dt3.text.toString()
                    .substring(11, 16) else "--"
            )
            databaseHelper.insertCalibrationOfflineDataFive(
                4,
                ph4.text.toString(),
                mv4.text.toString(),
                slope4.text.toString(),
                dt4.text.toString(),
                bufferD4.text.toString(),
                phAfterCalib4.text.toString(),
                tvTempCurr.text.toString(),
                if (dt4.text.toString().length >= 15) dt4.text.toString()
                    .substring(0, 10) else "--",
                if (dt4.text.toString().length >= 15) dt4.text.toString()
                    .substring(11, 16) else "--"
            )
            databaseHelper.insertCalibrationOfflineDataFive(
                5,
                ph5.text.toString(),
                mv5.text.toString(),
                slope5.text.toString(),
                dt5.text.toString(),
                bufferD5.text.toString(),
                phAfterCalib5.text.toString(),
                tvTempCurr.text.toString(),
                if (dt5.text.toString().length >= 15) dt5.text.toString()
                    .substring(0, 10) else "--",
                if (dt5.text.toString().length >= 15) dt5.text.toString()
                    .substring(11, 16) else "--"
            )
        }
        var index5 = 0
        while (calibCSV5.moveToNext()) {
            val ph = calibCSV5.getString(calibCSV5.getColumnIndex("PH"))
            val mv = calibCSV5.getString(calibCSV5.getColumnIndex("MV"))
            val date = calibCSV5.getString(calibCSV5.getColumnIndex("DT"))
            val slope = calibCSV5.getString(calibCSV5.getColumnIndex("SLOPE"))
            val pHAC = calibCSV5.getString(calibCSV5.getColumnIndex("pHAC"))
            val temperature1 = calibCSV5.getString(calibCSV5.getColumnIndex("temperature"))
            Log.d("Cursor Data", "PH: " + calibCSV5.getString(calibCSV5.getColumnIndex("PH")))
            if (index5 == 0) {
                PH1 = ph
                MV1 = mv
                SLOPE1 = slope
                DT1 = date
                pHAC1 = pHAC
                t1 = temperature1
                ph1.text = ph
                mv1.text = mv
                slope1.text = slope
                dt1.text = date
                phAfterCalib1.text = pHAC
                temp1.text = temperature1
            }
            if (index5 == 1) {
                PH2 = ph
                MV2 = mv
                SLOPE2 = slope
                DT2 = date
                pHAC2 = pHAC
                t2 = temperature1
                ph2.text = ph
                mv2.text = mv
                slope2.text = slope
                dt2.text = date
                phAfterCalib2.text = pHAC
                temp2.text = temperature1
            }
            if (index5 == 2) {
                PH3 = ph
                MV3 = mv
                SLOPE3 = slope
                DT3 = date
                pHAC3 = pHAC
                t3 = temperature1
                ph3.text = ph
                mv3.text = mv
                slope3.text = slope
                dt3.text = date
                phAfterCalib3.text = pHAC
                temp3.text = temperature1
            }
            if (index5 == 3) {
                PH4 = ph
                MV4 = mv
                SLOPE4 = slope
                DT4 = date
                pHAC4 = pHAC
                t4 = temperature1
                ph4.text = ph
                mv4.text = mv
                slope4.text = slope
                dt4.text = date
                phAfterCalib4.text = pHAC
                temp4.text = temperature1
            }
            if (index5 == 4) {
                PH5 = ph
                MV5 = mv
                SLOPE5 = slope
                DT5 = date
                pHAC5 = pHAC
                t5 = temperature1
                ph5.text = ph
                mv5.text = mv
                slope5.text = slope
                dt5.text = date
                phAfterCalib5.text = pHAC
                temp5.text = temperature1
            }
            index5++
        }

    }


    fun fileNotWrite(file: File) {
        file.setWritable(false)
        if (file.canWrite()) {
            Log.d("csv", "Nhi kaam kar rha")
        } else {
            Log.d("csvnw", "Party Bhaiiiii")
        }
    }


    fun reverseFileArray(fileArray: Array<File>): Array<File>? {
        for (i in 0 until fileArray.size / 2) {
            val a = fileArray[i]
            fileArray[i] = fileArray[fileArray.size - i - 1]
            fileArray[fileArray.size - i - 1] = a
        }
        return if (fileArray.isNotEmpty()) fileArray else null
    }


    private fun onClick(v: View) {
        when (v.id) {
            R.id.phEdit1 -> {
                val dialog = EditPhBufferDialog { ph ->
                    updateBufferValue(ph)
                    if (Constants.OFFLINE_MODE && Constants.OFFLINE_DATA) {
                        try {
                            jsonData = JSONObject()
                            jsonData.put("B_1", java.lang.String.valueOf(ph))
                            jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                            Log.e("ThisIsNotAnError", jsonData.getString("B_1"))
                            WebSocketManager.sendMessage(jsonData.toString())
                            ph1.setText(java.lang.String.valueOf(ph))
                            val calibDatClass = CalibDatClass(
                                1,
                                ph1.text.toString(),
                                mv1.text.toString(),
                                slope1.text.toString(),
                                dt1.text.toString(),
                                bufferD1.text.toString(),
                                phAfterCalib1.text.toString(),
                                tvTempCurr.text.toString(),
                                if (dt1.text.toString().length >= 15) dt1.text.toString()
                                    .substring(0, 10) else "--",
                                if (dt1.text.toString().length >= 15) dt1.text.toString()
                                    .substring(11, 16) else "--"
                            )
                            databaseHelper.updateClbOffDataFive(calibDatClass)
                            addUserAction(
                                "username: " + Source.userName + ", Role: " + Source.userRole +
                                        ", changed ph_buffer_1 value to " + ph + ", in calibmode 5",
                                "",
                                "",
                                "",
                                ""
                            )

                        } catch (e: JSONException) {
                            throw RuntimeException(e)
                        }
                    } else {
                        if (Constants.OFFLINE_DATA) {
                            Toast.makeText(fragmentContext, "You can't edit", Toast.LENGTH_SHORT)
                                .show()
                        } else {

                        }
                    }
                }
                dialog.show(parentFragmentManager, null)
            }

            R.id.phEdit2 -> {
                val dialog1 = EditPhBufferDialog { ph ->
                    updateBufferValue(ph)
                    if (Constants.OFFLINE_MODE && Constants.OFFLINE_DATA) {
                        try {
                            jsonData = JSONObject()
                            jsonData.put("B_2", java.lang.String.valueOf(ph))
                            jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                            WebSocketManager.sendMessage(jsonData.toString())
                            ph2.setText(java.lang.String.valueOf(ph))
                            val calibDatClass = CalibDatClass(
                                2,
                                ph2.text.toString(),
                                mv2.text.toString(),
                                slope2.text.toString(),
                                dt2.text.toString(),
                                bufferD2.text.toString(),
                                phAfterCalib2.text.toString(),
                                tvTempCurr.text.toString(),
                                if (dt2.text.toString().length >= 15) dt2.text.toString()
                                    .substring(0, 10) else "--",
                                if (dt2.text.toString().length >= 15) dt2.text.toString()
                                    .substring(11, 16) else "--"
                            )
                            databaseHelper.updateClbOffDataFive(calibDatClass)

                            addUserAction(
                                "username: " + Source.userName + ", Role: " + Source.userRole +
                                        ", changed ph_buffer_2 value to " + ph + ", in calibmode 5",
                                "",
                                "",
                                "",
                                ""
                            )
                        } catch (e: JSONException) {
                            throw RuntimeException(e)
                        }
                    } else {
                        if (Constants.OFFLINE_DATA) {
                            Toast.makeText(fragmentContext, "You can't edit", Toast.LENGTH_SHORT)
                                .show()
                        } else {

                        }
                    }
                }
                dialog1.show(parentFragmentManager, null)
            }

            R.id.phEdit3 -> {
                val dialog2 = EditPhBufferDialog { ph ->
                    updateBufferValue(ph)
                    if (Constants.OFFLINE_MODE && Constants.OFFLINE_DATA) {
                        try {
                            jsonData = JSONObject()
                            jsonData.put("B_3", java.lang.String.valueOf(ph))
                            jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                            WebSocketManager.sendMessage(jsonData.toString())
                            ph3.setText(java.lang.String.valueOf(ph))
                            val calibDatClass = CalibDatClass(
                                3,
                                ph3.text.toString(),
                                mv3.text.toString(),
                                slope3.text.toString(),
                                dt3.text.toString(),
                                bufferD3.text.toString(),
                                phAfterCalib3.text.toString(),
                                tvTempCurr.text.toString(),
                                if (dt3.text.toString().length >= 15) dt3.text.toString()
                                    .substring(0, 10) else "--",
                                if (dt3.text.toString().length >= 15) dt3.text.toString()
                                    .substring(11, 16) else "--"
                            )
                            databaseHelper.updateClbOffDataFive(calibDatClass)

                            addUserAction(
                                "username: " + Source.userName + ", Role: " + Source.userRole +
                                        ", changed ph_buffer_3 value to " + ph + ", in calibmode 5",
                                "",
                                "",
                                "",
                                ""
                            )
                        } catch (e: JSONException) {
                            throw RuntimeException(e)
                        }
                    } else {
                        if (Constants.OFFLINE_DATA) {
                            Toast.makeText(fragmentContext, "You can't edit", Toast.LENGTH_SHORT)
                                .show()
                        } else {

                        }
                    }
                }
                dialog2.show(parentFragmentManager, null)
            }

            R.id.phEdit4 -> {
                val dialog3 = EditPhBufferDialog { ph ->
                    updateBufferValue(ph)
                    if (Constants.OFFLINE_MODE && Constants.OFFLINE_DATA) {
                        try {
                            jsonData = JSONObject()
                            jsonData.put("B_4", java.lang.String.valueOf(ph))
                            jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                            WebSocketManager.sendMessage(jsonData.toString())
                            ph4.setText(java.lang.String.valueOf(ph))
                            val calibDatClass = CalibDatClass(
                                4,
                                ph4.text.toString(),
                                mv4.text.toString(),
                                slope4.text.toString(),
                                dt4.text.toString(),
                                bufferD4.text.toString(),
                                phAfterCalib4.text.toString(),
                                tvTempCurr.text.toString(),
                                if (dt4.text.toString().length >= 15) dt4.text.toString()
                                    .substring(0, 10) else "--",
                                if (dt4.text.toString().length >= 15) dt4.text.toString()
                                    .substring(11, 16) else "--"
                            )
                            databaseHelper.updateClbOffDataFive(calibDatClass)

                            addUserAction(
                                "username: " + Source.userName + ", Role: " + Source.userRole +
                                        ", changed ph_buffer_4 value to " + ph + ", in calibmode 5",
                                "",
                                "",
                                "",
                                ""
                            )
                        } catch (e: JSONException) {
                            throw RuntimeException(e)
                        }
                    } else {
                        if (Constants.OFFLINE_DATA) {
                            Toast.makeText(fragmentContext, "You can't edit", Toast.LENGTH_SHORT)
                                .show()
                        } else {

                        }
                    }
                }
                dialog3.show(parentFragmentManager, null)
            }

            R.id.phEdit5 -> {
                val dialog5 = EditPhBufferDialog { ph ->
                    updateBufferValue(ph)
                    if (Constants.OFFLINE_MODE && Constants.OFFLINE_DATA) {
                        try {
                            jsonData = JSONObject()
                            jsonData.put("B_5", java.lang.String.valueOf(ph))
                            jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                            WebSocketManager.sendMessage(jsonData.toString())
                            ph5.setText(java.lang.String.valueOf(ph))
                            val calibDatClass = CalibDatClass(
                                5,
                                ph5.text.toString(),
                                mv5.text.toString(),
                                slope5.text.toString(),
                                dt5.text.toString(),
                                bufferD5.text.toString(),
                                phAfterCalib5.text.toString(),
                                tvTempCurr.text.toString(),
                                if (dt5.text.toString().length >= 15) dt5.text.toString()
                                    .substring(0, 10) else "--",
                                if (dt5.text.toString().length >= 15) dt5.text.toString()
                                    .substring(11, 16) else "--"
                            )
                            databaseHelper.updateClbOffDataFive(calibDatClass)

                            addUserAction(
                                "username: " + Source.userName + ", Role: " + Source.userRole +
                                        ", changed ph_buffer_5 value to " + ph + ", in calibmode 5",
                                "",
                                "",
                                "",
                                ""
                            )
                        } catch (e: JSONException) {
                            throw RuntimeException(e)
                        }
                    } else {
                        if (Constants.OFFLINE_DATA) {
                            Toast.makeText(fragmentContext, "You can't edit", Toast.LENGTH_SHORT)
                                .show()
                        } else {

                        }
                    }
                }
                dialog5.show(parentFragmentManager, null)
            }

            R.id.qr1 -> openQRActivity("qr1")
            R.id.qr2 -> openQRActivity("qr2")
            R.id.qr3 -> openQRActivity("qr3")
            R.id.qr4 -> openQRActivity("qr4")
            R.id.qr5 -> openQRActivity("qr5")

            else -> {}
        }
    }

    private fun openQRActivity(view: String) {
        val intent = Intent(fragmentContext, ProbeScanner::class.java)
        intent.putExtra("activity", "PhCalibFragment")
        intent.putExtra("view", view)
        startActivity(intent)
    }

    private fun updateBufferValue(value: Float) {
        val newValue = value.toString()
    }

    private fun checkPermission(): Boolean {
        val permission1 =
            ContextCompat.checkSelfPermission(fragmentContext, permission.WRITE_EXTERNAL_STORAGE)
        val permission2 =
            ContextCompat.checkSelfPermission(fragmentContext, permission.READ_EXTERNAL_STORAGE)
        return permission1 == PackageManager.PERMISSION_GRANTED && permission2 == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            (requireContext() as Activity),
            arrayOf(permission.WRITE_EXTERNAL_STORAGE, permission.READ_EXTERNAL_STORAGE),
            PERMISSION_REQUEST_CODE
        )
    }


    private val PERMISSION_REQUEST_CODE = 200

    private fun initializeAllViews(view: View) {
        fivePointCalib = view.findViewById<CardView>(R.id.fivePointCalib)
        finalSlope = view.findViewById<TextView>(R.id.finalSlope)
        phMvTable = view.findViewById<Button>(R.id.phMvTable)
        phGraph = view.findViewById<Button>(R.id.phGraph)
        printCalibData = view.findViewById<Button>(R.id.printCalibData)
        printAllCalibData = view.findViewById<Button>(R.id.printAllCalibData)
        calibrateBtn = view.findViewById<Button>(R.id.startBtn)
        modeText = view.findViewById<TextView>(R.id.modeText)
        tvTimer = view.findViewById<TextView>(R.id.tvTimer)
        calibSpinner = view.findViewById<LinearLayout>(R.id.calibSpinner)
        spin = view.findViewById<Spinner>(R.id.calibMode)
        resetCalibFive = view.findViewById<Button>(R.id.resetCalibFive)
        syncOfflineData = view.findViewById<Button>(R.id.syncOfflineData)
        fivePointCalibStart = view.findViewById(R.id.fivePointCalibStart)
        log1 = view.findViewById<LinearLayout>(R.id.log1)
        log2 = view.findViewById<LinearLayout>(R.id.log2)
        log3 = view.findViewById<LinearLayout>(R.id.log3)
        log4 = view.findViewById<LinearLayout>(R.id.log4)
        log5 = view.findViewById<LinearLayout>(R.id.log5)
        log5point = view.findViewById<LinearLayout>(R.id.log5Point)
        ph1 = view.findViewById<TextView>(R.id.ph1)
        ph2 = view.findViewById<TextView>(R.id.ph2)
        ph3 = view.findViewById<TextView>(R.id.ph3)
        ph4 = view.findViewById<TextView>(R.id.ph4)
        ph5 = view.findViewById<TextView>(R.id.ph5)
        phAfterCalib1 = view.findViewById<TextView>(R.id.phAfterCalib1)
        phAfterCalib2 = view.findViewById<TextView>(R.id.phAfterCalib2)
        phAfterCalib3 = view.findViewById<TextView>(R.id.phAfterCalib3)
        phAfterCalib4 = view.findViewById<TextView>(R.id.phAfterCalib4)
        phAfterCalib5 = view.findViewById<TextView>(R.id.phAfterCalib5)
        mv1 = view.findViewById<TextView>(R.id.mv1)
        mv2 = view.findViewById<TextView>(R.id.mv2)
        mv3 = view.findViewById<TextView>(R.id.mv3)
        mv4 = view.findViewById<TextView>(R.id.mv4)
        mv5 = view.findViewById<TextView>(R.id.mv5)
        temp1 = view.findViewById<TextView>(R.id.temp1)
        temp2 = view.findViewById<TextView>(R.id.temp2)
        temp3 = view.findViewById<TextView>(R.id.temp3)
        temp4 = view.findViewById<TextView>(R.id.temp4)
        temp5 = view.findViewById<TextView>(R.id.temp5)
        qr1 = view.findViewById<TextView>(R.id.qr1)
        qr2 = view.findViewById<TextView>(R.id.qr2)
        qr3 = view.findViewById<TextView>(R.id.qr3)
        qr4 = view.findViewById<TextView>(R.id.qr4)
        qr5 = view.findViewById<TextView>(R.id.qr5)
        bufferD1 = view.findViewById<TextView>(R.id.bufferD1)
        bufferD2 = view.findViewById<TextView>(R.id.bufferD2)
        bufferD3 = view.findViewById<TextView>(R.id.bufferD3)
        bufferD4 = view.findViewById<TextView>(R.id.bufferD4)
        bufferD5 = view.findViewById<TextView>(R.id.bufferD5)
        slope1 = view.findViewById<TextView>(R.id.slope1)
        slope2 = view.findViewById<TextView>(R.id.slope2)
        slope3 = view.findViewById<TextView>(R.id.slope3)
        slope4 = view.findViewById<TextView>(R.id.slope4)
        slope5 = view.findViewById<TextView>(R.id.slope5)
        bufferD1.setSelected(true)
        bufferD2.setSelected(true)
        bufferD3.setSelected(true)
        bufferD4.setSelected(true)
        bufferD5.setSelected(true)
        phEdit1 = view.findViewById<TextView>(R.id.phEdit1)
        phEdit2 = view.findViewById<TextView>(R.id.phEdit2)
        phEdit3 = view.findViewById<TextView>(R.id.phEdit3)
        phEdit4 = view.findViewById<TextView>(R.id.phEdit4)
        phEdit5 = view.findViewById<TextView>(R.id.phEdit5)
        dt1 = view.findViewById<TextView>(R.id.dt1)
        dt2 = view.findViewById<TextView>(R.id.dt2)
        dt3 = view.findViewById<TextView>(R.id.dt3)
        dt4 = view.findViewById<TextView>(R.id.dt4)
        dt5 = view.findViewById<TextView>(R.id.dt5)

        calibRecyclerView = view.findViewById<RecyclerView>(R.id.rvCalibFileView)

        tvTempCurr = view.findViewById<TextView>(R.id.tvTempCurr)
        tvPhCurr = view.findViewById<TextView>(R.id.tvPhCurr)
        phView = view.findViewById(R.id.phView)
        tvEcCurr = view.findViewById<TextView>(R.id.tvEcCurr)
        phEdit1.setOnClickListener(View.OnClickListener { v: View? ->
            this.onClick(
                v!!
            )
        })
        phEdit2.setOnClickListener(View.OnClickListener { v: View? ->
            this.onClick(
                v!!
            )
        })
        phEdit3.setOnClickListener(View.OnClickListener { v: View? ->
            this.onClick(
                v!!
            )
        })
        phEdit4.setOnClickListener(View.OnClickListener { v: View? ->
            this.onClick(
                v!!
            )
        })
        phEdit5.setOnClickListener(View.OnClickListener { v: View? ->
            this.onClick(
                v!!
            )
        })
        qr1.setOnClickListener(View.OnClickListener { v: View? ->
            this.onClick(
                v!!
            )
        })
        qr2.setOnClickListener(View.OnClickListener { v: View? ->
            this.onClick(
                v!!
            )
        })
        qr3.setOnClickListener(View.OnClickListener { v: View? ->
            this.onClick(
                v!!
            )
        })
        qr4.setOnClickListener(View.OnClickListener { v: View? ->
            this.onClick(
                v!!
            )
        })
        qr5.setOnClickListener(View.OnClickListener { v: View? ->
            this.onClick(
                v!!
            )
        })

        if (Constants.OFFLINE_MODE || Constants.OFFLINE_DATA) {
            syncOfflineData.setVisibility(View.GONE)
        } else {
            syncOfflineData.setVisibility(View.VISIBLE)
        }
        syncOfflineData.setVisibility(View.GONE)

//        syncOfflineData.setOnClickListener(View.OnClickListener { v: View? -> syncOfflineWithOnline() })
    }


    private lateinit var fivePointCalibStart: LinearLayout
    private var connectedWebsocket = false
    private lateinit var log1: LinearLayout
    private lateinit var log2: LinearLayout
    private lateinit var log3: LinearLayout
    private lateinit var log4: LinearLayout
    private lateinit var log5: LinearLayout

    private lateinit var log5point: LinearLayout
    private lateinit var calibSpinner: LinearLayout
    private lateinit var spin: Spinner
    private lateinit var tvTimer: TextView
    private lateinit var tvTempCurr: TextView
    private lateinit var tvPhCurr: TextView
    private lateinit var modeText: TextView
    private lateinit var finalSlope: TextView
    private lateinit var calibrateBtn: Button
    private lateinit var printCalibData: Button
    private lateinit var phMvTable: Button
    private lateinit var phGraph: Button
    private lateinit var printAllCalibData: Button
    private lateinit var ph1: TextView
    private lateinit var ph2: TextView
    private lateinit var ph3: TextView
    private lateinit var ph4: TextView
    private lateinit var ph5: TextView
    private lateinit var phAfterCalib1: TextView
    private lateinit var phAfterCalib2: TextView
    private lateinit var phAfterCalib3: TextView
    private lateinit var phAfterCalib4: TextView
    private lateinit var phAfterCalib5: TextView
    private lateinit var slope1: TextView
    private lateinit var slope2: TextView
    private lateinit var slope3: TextView
    private lateinit var slope4: TextView
    private lateinit var slope5: TextView
    private lateinit var temp1: TextView
    private lateinit var temp2: TextView
    private lateinit var temp3: TextView
    private lateinit var temp4: TextView
    private lateinit var temp5: TextView
    private lateinit var mv1: TextView
    private lateinit var mv2: TextView
    private lateinit var mv3: TextView

    lateinit var binding: FragmentPhCalibNewBinding

    private lateinit var calibRecyclerView: RecyclerView
    private lateinit var calibFileAdapter: PDF_CSV_Adapter

    private var minMV1: Float = 0f
    private var minMV2: Float = 0f
    private var minMV3: Float = 0f
    private var minMV4: Float = 0f
    private var minMV5: Float = 0f

    private var maxMV1: Float = 0f
    private var maxMV2: Float = 0f
    private var maxMV3: Float = 0f
    private var maxMV4: Float = 0f
    private var maxMV5: Float = 0f

    private var minMV1_3: Float = 0f
    private var minMV2_3: Float = 0f
    private var minMV3_3: Float = 0f

    private var maxMV1_3: Float = 0f
    private var maxMV2_3: Float = 0f
    private var maxMV3_3: Float = 0f

    private lateinit var MV1: String
    private lateinit var MV2: String
    private lateinit var MV3: String
    private lateinit var MV4: String
    private lateinit var MV5: String

    private lateinit var PH1: String
    private lateinit var PH2: String
    private lateinit var PH3: String
    private lateinit var PH4: String
    private lateinit var PH5: String

    private lateinit var DT1: String
    private lateinit var DT2: String
    private lateinit var DT3: String
    private lateinit var DT4: String
    private lateinit var DT5: String

    private lateinit var BFD1: String
    private lateinit var BFD2: String
    private lateinit var BFD3: String
    private lateinit var BFD4: String
    private lateinit var BFD5: String

    private lateinit var t1: String
    private lateinit var t2: String
    private lateinit var t3: String
    private lateinit var t4: String
    private lateinit var t5: String

    private lateinit var pHAC1: String
    private lateinit var pHAC2: String
    private lateinit var pHAC3: String
    private lateinit var pHAC4: String
    private lateinit var pHAC5: String

    private lateinit var mV1: String
    private lateinit var mV2: String
    private lateinit var mV3: String
    private lateinit var mV4: String
    private lateinit var mV5: String

    private lateinit var SLOPE1: String
    private lateinit var SLOPE2: String
    private lateinit var SLOPE3: String
    private lateinit var SLOPE4: String
    private lateinit var SLOPE5: String


    private lateinit var fivePointCalib: CardView


    private val bufferLabels = arrayOf("B_1", "B_2", "B_3", "B_4", "B_5")
    private val bufferLabelsThree = arrayOf("B_2", "B_3", "B_4")
    private val coeffLabels = arrayOf("VAL_1", "VAL_2", "VAL_3", "VAL_4", "VAL_5")
    private val postCoeffLabels =
        arrayOf("POST_VAL_1", "POST_VAL_2", "POST_VAL_3", "POST_VAL_4", "POST_VAL_5")
    private val postCoeffLabelsThree = arrayOf("POST_VAL_2", "POST_VAL_3", "POST_VAL_4")
    private val coeffLabelsThree = arrayOf("VAL_2", "VAL_3", "VAL_4")

    lateinit var resetCalibFive: Button
    lateinit var syncOfflineData: Button


    private lateinit var mv4: TextView
    private lateinit var mv5: TextView

    private lateinit var dt1: TextView
    private lateinit var dt2: TextView
    private lateinit var dt3: TextView
    private lateinit var dt4: TextView
    private lateinit var dt5: TextView

    private lateinit var bufferD1: TextView
    private lateinit var bufferD2: TextView
    private lateinit var bufferD3: TextView
    private lateinit var bufferD4: TextView
    private lateinit var bufferD5: TextView

    private lateinit var phEdit1: TextView
    private lateinit var phEdit2: TextView
    private lateinit var phEdit3: TextView
    private lateinit var phEdit4: TextView
    private lateinit var phEdit5: TextView

    private lateinit var qr1: TextView
    private lateinit var qr2: TextView
    private lateinit var qr3: TextView
    private lateinit var qr4: TextView
    private lateinit var qr5: TextView


    lateinit var phView: PhView
    lateinit var tvEcCurr: TextView

    lateinit var jsonData: JSONObject
    lateinit var databaseHelper: DatabaseHelper

}