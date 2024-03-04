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
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.provider.MediaStore
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
import com.aican.aicanapp.Dashboard
import com.aican.aicanapp.ProbeScanner
import com.aican.aicanapp.R
import com.aican.aicanapp.adapters.CalibFileAdapter
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
import com.aican.aicanapp.utils.SharedPref
import com.aican.aicanapp.utils.Source
import com.aican.aicanapp.viewModels.SharedViewModel
import com.aican.aicanapp.websocket.WebSocketManager
import com.google.firebase.database.DatabaseReference
import com.itextpdf.io.image.ImageData
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.MalformedURLException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class PhCalibFragmentNew : Fragment() {

    companion object {
        var PH_MODE = "both"
        private var line = 0
        private var line_3 = 0
        var wrong_5 = false

        private var LOG_INTERVAL = 0f
        private var LOG_INTERVAL_3 = 0f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
    var currentBufThree = 0
    private val sharedViewModel: SharedViewModel by activityViewModels()

    // Within your fragment's lifecycle methods or when the context is available
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

        setPreviousData()

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


        printCalibData.setOnClickListener { v: View? ->
            try {
                generatePDF()

                addUserAction(
                    "username: " + Source.userName + ", Role: " + Source.userRole +
                            ", print calib report ", "", "", "", ""
                )

            } catch (e: FileNotFoundException) {
//                Toast.makeText(requireContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace()
            }
//                exportCalibData();
            val path =
                ContextWrapper(requireContext()).externalMediaDirs[0].toString() + File.separator + "/LabApp/CalibrationData"
            val root = File(path)
            val filesAndFolders = root.listFiles()
            if (filesAndFolders == null || filesAndFolders.size == 0) {
                Toast.makeText(requireContext(), "No Files Found", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else {
                for (i in filesAndFolders.indices) {
                    filesAndFolders[i].name.endsWith(".pdf")
                }
            }
            val pathPDF =
                ContextWrapper(requireContext()).externalMediaDirs[0].toString() + File.separator + "/LabApp/CalibrationData/"
            val rootPDF = File(pathPDF)
            fileNotWrite(root)
            val filesAndFoldersPDF = rootPDF.listFiles()
            calibFileAdapter = CalibFileAdapter(
                requireContext().applicationContext, reverseFileArray(filesAndFoldersPDF)
            )
            calibRecyclerView.adapter = calibFileAdapter
            calibFileAdapter.notifyDataSetChanged()
            calibRecyclerView.layoutManager =
                LinearLayoutManager(requireContext().applicationContext)
        }

        /////

        websocketData()


    }

    private fun updateMessage(message: String) {
        sharedViewModel.messageLiveData.value = message
    }

    private fun updateError(error: String) {
        sharedViewModel.errorLiveData.value = error
    }

    private fun websocketData() {

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
                    if (jsonData.has("SLOPE") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                        if (jsonData.getString("SLOPE") != "nan" && PhFragment.validateNumber(
                                jsonData.getString("SLOPE")
                            )
                        ) {
                            val finalSlopes = jsonData.getString("SLOPE")
                            Toast.makeText(requireContext(), "" + finalSlopes, Toast.LENGTH_SHORT)
                                .show()
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
                            Toast.makeText(requireContext(), "" + finalSlopes, Toast.LENGTH_SHORT)
                                .show()
                            SharedPref.saveData(
                                requireContext(), "OFFSET_" + PhActivity.DEVICE_ID, finalSlopes
                            )
                            finalSlope.text = finalSlopes
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
                                tvTempCurr.text = "$tempForm°C"
                                SharedPref.saveData(
                                    requireContext(), "tempValueu" + PhActivity.DEVICE_ID, tempForm
                                )
                                if (tempVal <= -127.0) {
                                    tvTempCurr.text = "NA"
                                    SharedPref.saveData(
                                        requireContext(), "tempValue" + PhActivity.DEVICE_ID, "NA"
                                    )
                                }
                            } else {
                                tvTempCurr.text = "nan"
                                SharedPref.saveData(
                                    requireContext(), "tempValue" + PhActivity.DEVICE_ID, "nan"
                                )
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
                        if (jsonData.has("POST_VAL_2") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("POST_VAL_2")
                            var v = `val`
                            if (`val` != "nan" && PhFragment.validateNumber(`val`)) {
                                v = String.format(
                                    Locale.UK, "%.2f", `val`.toFloat()
                                )
                            }
                            phAfterCalib1_3.text = v
                            pHAC1_3 = phAfterCalib1_3.text.toString()
                            val calibDatClass1 = CalibDatClass(
                                1,
                                ph1_3.text.toString(),
                                mv1_3.text.toString(),
                                slope1_3.text.toString(),
                                dt1_3.text.toString(),
                                bufferD1_3.text.toString(),
                                phAfterCalib1_3.text.toString(),
                                tvTempCurr.text.toString(),
                                if (dt1_3.text.toString().length >= 15) dt1_3.text.toString()
                                    .substring(0, 10) else "--",
                                if (dt1_3.text.toString().length >= 15) dt1_3.text.toString()
                                    .substring(11, 16) else "--"
                            )
                            databaseHelper.updateClbOffDataThree(calibDatClass1)

                            SharedPref.saveData(requireContext(), "pHAC1_3", pHAC1_3)


                        }
                        if (jsonData.has("POST_VAL_3") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("POST_VAL_3")
                            var v = `val`
                            if (`val` != "nan" && PhFragment.validateNumber(`val`)) {
                                v = String.format(
                                    Locale.UK, "%.2f", `val`.toFloat()
                                )
                            }
                            phAfterCalib2_3.text = v
                            pHAC2_3 = phAfterCalib2_3.text.toString()
                            val calibDatClass2 = CalibDatClass(
                                2,
                                ph2_3.text.toString(),
                                mv2_3.text.toString(),
                                slope2_3.text.toString(),
                                dt2_3.text.toString(),
                                bufferD2_3.text.toString(),
                                phAfterCalib2_3.text.toString(),
                                tvTempCurr.text.toString(),
                                if (dt2_3.text.toString().length >= 15) dt2_3.text.toString()
                                    .substring(0, 10) else "--",
                                if (dt2_3.text.toString().length >= 15) dt2_3.text.toString()
                                    .substring(11, 16) else "--"
                            )
                            databaseHelper.updateClbOffDataThree(calibDatClass2)

                            SharedPref.saveData(requireContext(), "pHAC2_3", pHAC2_3)

                        }
                        if (jsonData.has("POST_VAL_4") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("POST_VAL_4")
                            var v = `val`
                            if (`val` != "nan" && PhFragment.validateNumber(`val`)) {
                                v = String.format(
                                    Locale.UK, "%.2f", `val`.toFloat()
                                )
                            }
                            phAfterCalib3_3.text = v
                            pHAC3_3 = phAfterCalib3_3.text.toString()
                            val calibDatClass3 = CalibDatClass(
                                3,
                                ph3_3.text.toString(),
                                mv3_3.text.toString(),
                                slope3_3.text.toString(),
                                dt3_3.text.toString(),
                                bufferD3_3.text.toString(),
                                phAfterCalib3_3.text.toString(),
                                tvTempCurr.text.toString(),
                                if (dt3_3.text.toString().length >= 15) dt3_3.text.toString()
                                    .substring(0, 10) else "--",
                                if (dt3_3.text.toString().length >= 15) dt3_3.text.toString()
                                    .substring(11, 16) else "--"
                            )
                            databaseHelper.updateClbOffDataThree(calibDatClass3)

                            SharedPref.saveData(requireContext(), "pHAC3_3", pHAC3_3)


                        }
                        if (jsonData.has("SLOPE_1") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("SLOPE_1")
                            var v = `val`
                            if (`val` != "nan" && PhFragment.validateNumber(`val`)) {
                                v = String.format(
                                    Locale.UK, "%.2f", `val`.toFloat()
                                )
                            }
                            slope2_3.text = v
                            val calibDatClass2 = CalibDatClass(
                                2,
                                ph2_3.text.toString(),
                                mv2_3.text.toString(),
                                slope2_3.text.toString(),
                                dt2_3.text.toString(),
                                bufferD2_3.text.toString(),
                                phAfterCalib2_3.text.toString(),
                                tvTempCurr.text.toString(),
                                if (dt2_3.text.toString().length >= 15) dt2_3.text.toString()
                                    .substring(0, 10) else "--",
                                if (dt2_3.text.toString().length >= 15) dt2_3.text.toString()
                                    .substring(11, 16) else "--"
                            )
                            databaseHelper.updateClbOffDataThree(calibDatClass2)
                        }
                        if (jsonData.has("SLOPE_2") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("SLOPE_2")
                            var v = `val`
                            if (`val` != "nan" && PhFragment.validateNumber(`val`)) {
                                v = String.format(
                                    Locale.UK, "%.2f", `val`.toFloat()
                                )
                            }
                            slope3_3.text = v
                            pHAC3_3 = phAfterCalib3_3.text.toString()
                            val calibDatClass3 = CalibDatClass(
                                3,
                                ph3_3.text.toString(),
                                mv3_3.text.toString(),
                                slope3_3.text.toString(),
                                dt3_3.text.toString(),
                                bufferD3_3.text.toString(),
                                phAfterCalib3_3.text.toString(),
                                tvTempCurr.text.toString(),
                                if (dt3_3.text.toString().length >= 15) dt3_3.text.toString()
                                    .substring(0, 10) else "--",
                                if (dt3_3.text.toString().length >= 15) dt3_3.text.toString()
                                    .substring(11, 16) else "--"
                            )
                            databaseHelper.updateClbOffDataThree(calibDatClass3)
                        }
                        if (jsonData.has("PH_VAL") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("PH_VAL")
                            var v = `val`
                            if (`val` != "nan" && PhFragment.validateNumber(`val`)) {
                                v = String.format(
                                    Locale.UK, "%.2f", `val`.toFloat()
                                )
                                phView.moveTo(v.toFloat())
                            }
                            tvPhCurr.text = v
                        }
                        if (jsonData.has("TEMP_VAL") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val ph = jsonData.getString("TEMP_VAL").toFloat()
                            val tempForm = String.format(Locale.UK, "%.1f", ph)
                            tvTempCurr.text = "$tempForm°C"
                            if (ph <= -127.0) {
                                tvTempCurr.text = "NA"
                            }
                        }
                        if (jsonData.has("EC_VAL") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val ph = jsonData.getString("EC_VAL")
                            SharedPref.saveData(
                                requireContext(), "ecValue" + PhActivity.DEVICE_ID, ph
                            )
                            tvEcCurr.text = ph
                        }
                        if (jsonData.has("MV_2") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("MV_2")
                            var e = `val`
                            if (`val` != "nan" && PhFragment.validateNumber(`val`)) {
                                e = String.format(
                                    Locale.UK, "%.2f", `val`.toFloat()
                                )
                            }
                            mv1_3.text = e
                            mV1_3 = mv1_3.text.toString()

//                            SharedPref.saveData(requireContext(),)


                            val sharedPreferences = fragmentContext.getSharedPreferences(
                                "CalibPrefs", Context.MODE_PRIVATE
                            )
                            val myEdit = sharedPreferences.edit()
                            myEdit.putString("MV1_3", mV1_3)
                            myEdit.commit()
                        }
                        if (jsonData.has("MV_3") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("MV_3")
                            var e = `val`
                            if (`val` != "nan" && PhFragment.validateNumber(`val`)) {
                                e = String.format(
                                    Locale.UK, "%.2f", `val`.toFloat()
                                )
                            }
                            mv2_3.text = e
                            mV2_3 = mv2_3.text.toString()
                            val sharedPreferences = fragmentContext.getSharedPreferences(
                                "CalibPrefs", Context.MODE_PRIVATE
                            )
                            val myEdit = sharedPreferences.edit()
                            myEdit.putString("MV2_3", mV2_3)
                            myEdit.commit()
                        }
                        if (jsonData.has("MV_4") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("MV_4")
                            var e = `val`
                            if (`val` != "nan" && PhFragment.validateNumber(`val`)) {
                                e = String.format(
                                    Locale.UK, "%.2f", `val`.toFloat()
                                )
                            }
                            mv3_3.text = e
                            mV3_3 = mv3_3.text.toString()
                            val sharedPreferences = fragmentContext.getSharedPreferences(
                                "CalibPrefs", Context.MODE_PRIVATE
                            )
                            val myEdit = sharedPreferences.edit()
                            myEdit.putString("MV3_3", mV3_3)
                            myEdit.commit()
                        }
                        if (jsonData.has("DT_2") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("DT_2")
                            dt1_3.text = `val`
                            DT1_3 = dt1_3.text.toString()
                            val sharedPreferences = fragmentContext.getSharedPreferences(
                                "CalibPrefs", Context.MODE_PRIVATE
                            )
                            val myEdit = sharedPreferences.edit()
                            myEdit.putString("DT1_3", DT1_3)
                            myEdit.commit()
                        }
                        if (jsonData.has("DT_3") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("DT_3")
                            dt2_3.text = `val`
                            DT2_3 = dt2_3.text.toString()
                            val sharedPreferences = fragmentContext.getSharedPreferences(
                                "CalibPrefs", Context.MODE_PRIVATE
                            )
                            val myEdit = sharedPreferences.edit()
                            myEdit.putString("DT2_3", DT2_3)
                            myEdit.commit()
                        }
                        if (jsonData.has("DT_4") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("DT_4")
                            dt3_3.text = `val`
                            DT3_3 = dt3_3.text.toString()
                            val sharedPreferences = fragmentContext.getSharedPreferences(
                                "CalibPrefs", Context.MODE_PRIVATE
                            )
                            val myEdit = sharedPreferences.edit()
                            myEdit.putString("DT3_3", DT3_3)
                            myEdit.commit()
                        }
                        if (jsonData.has("B_2") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("B_2")
                            ph1_3.text = `val`
                            PH1_3 = ph1_3.text.toString()
                            val sharedPreferences = fragmentContext.getSharedPreferences(
                                "CalibPrefs", Context.MODE_PRIVATE
                            )
                            val myEdit = sharedPreferences.edit()
                            myEdit.putString("PH1_3", PH1_3)
                            myEdit.commit()
                        }
                        if (jsonData.has("B_3") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("B_3")
                            ph2_3.text = `val`
                            PH2_3 = ph2_3.text.toString()
                            val sharedPreferences = fragmentContext.getSharedPreferences(
                                "CalibPrefs", Context.MODE_PRIVATE
                            )
                            val myEdit = sharedPreferences.edit()
                            myEdit.putString("PH2_3", PH2_3)
                            myEdit.commit()
                        }
                        if (jsonData.has("B_4") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                            val `val` = jsonData.getString("B_4")
                            ph3_3.text = `val`
                            PH3_3 = ph3_3.text.toString()
                            val sharedPreferences = fragmentContext.getSharedPreferences(
                                "CalibPrefs", Context.MODE_PRIVATE
                            )
                            val myEdit = sharedPreferences.edit()
                            myEdit.putString("PH3_3", PH3_3)
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
                            if (jsonData.getString("CAL") == "21" && jsonData.has("POST_VAL_2")) {
                                val d = jsonData.getString("POST_VAL_2")
                                phAfterCalib1_3.text = d
                                val calibDatClass1 = CalibDatClass(
                                    1,
                                    ph1_3.text.toString(),
                                    mv1_3.text.toString(),
                                    slope1_3.text.toString(),
                                    dt1_3.text.toString(),
                                    bufferD1_3.text.toString(),
                                    phAfterCalib1_3.text.toString(),
                                    tvTempCurr.text.toString(),
                                    if (dt1_3.text.toString().length >= 15) dt1_3.text.toString()
                                        .substring(0, 10) else "--",
                                    if (dt1_3.text.toString().length >= 15) dt1_3.text.toString()
                                        .substring(11, 16) else "--"
                                )
                                databaseHelper.updateClbOffDataThree(calibDatClass1)
                                myEdit.putString("tem1_3", tvTempCurr.text.toString())
                                myEdit.putString("pHAC1_3", d)
                                myEdit.commit()
                                temp1_3.text = tvTempCurr.text
                            } else if (jsonData.getString("CAL") == "31" && jsonData.has("POST_VAL_3")) {
                                val d = jsonData.getString("POST_VAL_3")
                                phAfterCalib2_3.text = d
                                val calibDatClass2 = CalibDatClass(
                                    2,
                                    ph2_3.text.toString(),
                                    mv2_3.text.toString(),
                                    slope2_3.text.toString(),
                                    dt2_3.text.toString(),
                                    bufferD2_3.text.toString(),
                                    phAfterCalib2_3.text.toString(),
                                    tvTempCurr.text.toString(),
                                    if (dt2_3.text.toString().length >= 15) dt2_3.text.toString()
                                        .substring(0, 10) else "--",
                                    if (dt2_3.text.toString().length >= 15) dt2_3.text.toString()
                                        .substring(11, 16) else "--"
                                )
                                databaseHelper.updateClbOffDataThree(calibDatClass2)
                                myEdit.putString("tem2_3", tvTempCurr.text.toString())
                                myEdit.putString("pHAC2_3", d)
                                myEdit.commit()
                                temp2_3.text = tvTempCurr.text
                            } else if (jsonData.getString("CAL") == "41" && jsonData.has("POST_VAL_4")) {
                                val d = jsonData.getString("POST_VAL_4")
                                phAfterCalib3_3.text = d.toString()
                                val calibDatClass3 = CalibDatClass(
                                    3,
                                    ph3_3.text.toString(),
                                    mv3_3.text.toString(),
                                    slope3_3.text.toString(),
                                    dt3_3.text.toString(),
                                    bufferD3_3.text.toString(),
                                    phAfterCalib3_3.text.toString(),
                                    tvTempCurr.text.toString(),
                                    if (dt3_3.text.toString().length >= 15) dt3_3.text.toString()
                                        .substring(0, 10) else "--",
                                    if (dt3_3.text.toString().length >= 15) dt3_3.text.toString()
                                        .substring(11, 16) else "--"
                                )
                                databaseHelper.updateClbOffDataThree(calibDatClass3)
                                myEdit.putString("tem3_3", tvTempCurr.text.toString())
                                myEdit.putString("pHAC3_3", d.toString())
                                myEdit.commit()
                                temp3_3.text = tvTempCurr.text
//                                calibData3()
                            }
                        }
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


        resetCalibThree.setOnClickListener {
            jsonData = JSONObject()
            try {
                jsonData.put("CAL", "0")
                jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)

                WebSocketManager.sendMessage(jsonData.toString())
            } catch (e: JSONException) {
                throw java.lang.RuntimeException(e)
            }
            line_3 = 0
            currentBufThree = 0


            //                resetCalibration.resetCalibration();
            calibrateBtnThree.isEnabled = true
            isCalibrating = false
            phGraph.isEnabled = true
            phMvTable.isEnabled = true
            printCalibData.isEnabled = true
            calibSpinner.isEnabled = true
            spin.isEnabled = true
            Source.calibratingNow = false
            if (timer3 != null) {
                timer3!!.cancel()
            }
            log1_3.setBackgroundColor(Color.GRAY)
            log2_3.setBackgroundColor(Color.WHITE)
            log3_3.setBackgroundColor(Color.WHITE)


            //
            tvTimerThree.visibility = View.INVISIBLE
        }

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
        }

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

        calibrateBtnThree.setOnClickListener { v: View? ->
            if (Constants.OFFLINE_MODE && Constants.OFFLINE_DATA) {
                if (connectedWebsocket) {
                    calibrateThreePointOffline()
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
                    "You can't calibrate you are inoffline mode and device is not connect",
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

    @Throws(FileNotFoundException::class)
    private fun generatePDF() {

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
        val device_id = "DeviceID: $deviceID"
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
            val uri: Uri? = getImageUri(requireContext(), imgBit)
            try {
                val add: String? = getPath(uri)
                val imageData = ImageDataFactory.create(add)
                val image: Image = Image(imageData).setHeight(80f).setWidth(80f)
                //                table12.addCell(new Cell(2, 1).add(image));
                // Adding image to the document
                document.add(image)
            } catch (e: MalformedURLException) {
                e.printStackTrace()
            }
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
            if (spin.selectedItemPosition == 0) {
                calibCSV = db.rawQuery("SELECT * FROM CalibOfflineDataFive", null)
            }
            if (spin.selectedItemPosition == 1) {
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
            if (spin.selectedItemPosition == 0) {
                document.add(Paragraph("Calibration : $calib_stat"))
            }
        }
        document.add(Paragraph("Operator Sign                                                                                      Supervisor Sign"))
        val imgBit1: Bitmap? = getSignImage()
        if (imgBit1 != null) {
            val uri1: Uri = getImageUri(fragmentContext, imgBit1)!!
            try {
                val add: String = getPath(uri1)!!
                val imageData1: ImageData = ImageDataFactory.create(add)
                val image1: Image = Image(imageData1).setHeight(80f).setWidth(80f)
                //                table12.addCell(new Cell(2, 1).add(image));
                // Adding image to the document
                document.add(image1)
            } catch (e: MalformedURLException) {
                e.printStackTrace()
            }
        }
        document.close()
        Toast.makeText(fragmentContext, "Pdf generated", Toast.LENGTH_SHORT).show()
    }

    fun getPath(uri: Uri?): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = requireActivity().managedQuery(uri, projection, null, null, null)
        val column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor.moveToFirst()
        return cursor.getString(column_index)
    }


    fun getImageUri(inContext: Context, inImage: Bitmap): Uri? {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path =
            MediaStore.Images.Media.insertImage(inContext.contentResolver, inImage, "Title", null)
        return Uri.parse(path)
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
                requireContext(), R.color.colorPrimaryAlpha
            )
        )
        calibrateBtn.isEnabled = false
        tvTimer.visibility = View.VISIBLE
        isCalibrating = true

        addUserAction(
            "username: " + Source.userName + ", Role: " + Source.userRole +
                    ", started calibration mode 5, of buffer " + currentBuf, "", "", "", ""
        )

        timer5 = object : CountDownTimer(50000, 1000) {
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
                            if (!PhCalibFragmentNew.wrong_5) {
                                PhCalibFragmentNew.wrong_5 = false
                                line = currentBuf + 1
                                if (currentBuf == 4) {
                                    val date123 =
                                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
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
                                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
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
                                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
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
                                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
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
                                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
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
        PhCalibFragmentNew.LOG_INTERVAL = 5f
        tvTimer.setText(PhCalibFragmentNew.LOG_INTERVAL.toString())
        handler1 = Handler()
        runnable1 = object : Runnable {
            override fun run() {
                Log.d("Runnable", "Handler is working")
                calibrateBtn.isEnabled = false
                if (PhCalibFragmentNew.LOG_INTERVAL == 0f) { // just remove call backs
                    tvTimer.setText(PhCalibFragmentNew.LOG_INTERVAL.toString())
                    handler1.removeCallbacks(this)
                    calibrateBtn.isEnabled = true
                    calibrateBtn.text = "Start"
                    currentBuf = 0
                    line = 0
                    log1.setBackgroundColor(Color.GRAY)
                    log2.setBackgroundColor(Color.WHITE)
                    log3.setBackgroundColor(Color.WHITE)
                    log4.setBackgroundColor(Color.WHITE)
                    log5.setBackgroundColor(Color.WHITE)
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
                        "-", "-", "-", "-", "-", "-", "-", "-", "-"
                    )
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
            val i = Intent(requireContext(), PhActivity::class.java)
            i.putExtra("refreshCalib", "y")
            i.putExtra(Dashboard.KEY_DEVICE_ID, PhActivity.DEVICE_ID)
            startActivity(i)
            requireActivity().finish()

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

    private var line_3 = 0
    var wrong_3 = false
    var timer3: CountDownTimer? = null
    val handler33 = Handler()
    lateinit var runnable33: Runnable

    private fun calibrateThreePointOffline() {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        calibrateBtnThree.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(), R.color.colorPrimaryAlpha
            )
        )
        calibrateBtnThree.isEnabled = false
        tvTimerThree.visibility = View.VISIBLE
        isCalibrating = true

        addUserAction(
            "username: " + Source.userName + ", Role: " + Source.userRole +
                    ", started calibration mode 3, of buffer " + currentBufThree, "", "", "", ""
        )

//        startTimer();
        timer3 = object : CountDownTimer(45000, 1000) {
            //45000
            override fun onTick(millisUntilFinished: Long) {
                var millisUntilFinished = millisUntilFinished
                calibrateBtnThree.isEnabled = false
                phGraph.isEnabled = false
                phMvTable.isEnabled = false
                printCalibData.isEnabled = false
                calibSpinner.isEnabled = false
                spin.isEnabled = false
                millisUntilFinished /= 1000
                val min = millisUntilFinished.toInt() / 60
                val sec = millisUntilFinished.toInt() % 60
                val time = String.format(Locale.UK, "%02d:%02d", min, sec)
                tvTimerThree.text = time
                Log.e("lineNThree", line.toString() + "")
                if (!Source.calibratingNow) {
                    timer3!!.cancel()
                    //                    handler33.removeCallbacks(this);
                    wrong_3 = false
                    calibrateBtnThree.isEnabled = true
                    calibrateBtnThree.isEnabled = true
                    phGraph.isEnabled = true
                    phMvTable.isEnabled = true
                    printCalibData.isEnabled = true
                    calibSpinner.isEnabled = true
                    spin.isEnabled = true
                }
                Source.calibratingNow = true
                if (line_3 == -1) {
                    log1_3.setBackgroundColor(Color.WHITE)
                    log2_3.setBackgroundColor(Color.WHITE)
                    log3_3.setBackgroundColor(Color.WHITE)
                    wrong_3 = false
                }
                if (line_3 == 0) {
                    log1_3.setBackgroundColor(Color.GRAY)
                    log2_3.setBackgroundColor(Color.WHITE)
                    log3_3.setBackgroundColor(Color.WHITE)
                    jsonData = JSONObject()
                    try {
                        jsonData.put("B_2", ph1_3.text.toString())
                        jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                        Log.e("ThisIsNotAnError", jsonData.getString("B_2"))
                        WebSocketManager.sendMessage(jsonData.toString())
                    } catch (e: JSONException) {
                        throw java.lang.RuntimeException(e)
                    }

                }
                if (line_3 == 1) {
                    log1_3.setBackgroundColor(Color.WHITE)
                    log2_3.setBackgroundColor(Color.GRAY)
                    log3_3.setBackgroundColor(Color.WHITE)
                    jsonData = JSONObject()
                    try {
                        jsonData.put("B_3", ph2_3.text.toString())
                        jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                        Log.e("ThisIsNotAnError", jsonData.getString("B_3"))
                        WebSocketManager.sendMessage(jsonData.toString())
                    } catch (e: JSONException) {
                        throw java.lang.RuntimeException(e)
                    }

                }
                if (line_3 == 2) {
                    log1_3.setBackgroundColor(Color.WHITE)
                    log2_3.setBackgroundColor(Color.WHITE)
                    log3_3.setBackgroundColor(Color.GRAY)
                    jsonData = JSONObject()
                    try {
                        jsonData.put("B_4", ph3_3.text.toString())
                        jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                        Log.e("ThisIsNotAnError", jsonData.getString("B_4"))
                        WebSocketManager.sendMessage(jsonData.toString())
                    } catch (e: JSONException) {
                        throw java.lang.RuntimeException(e)
                    }


                }
                if (line_3 > 2) {
                    log1_3.setBackgroundColor(Color.WHITE)
                    log2_3.setBackgroundColor(Color.WHITE)
                    log3_3.setBackgroundColor(Color.WHITE)
                    wrong_3 = false
                }
                wrong_3 = false
            }

            override fun onFinish() {
                runnable33 = object : Runnable {
                    override fun run() {
                        try {
                            Source.calibratingNow = false
                            phGraph.isEnabled = true
                            phMvTable.isEnabled = true
                            printCalibData.isEnabled = true
                            calibSpinner.isEnabled = true
                            spin.isEnabled = true
                            if (!wrong_3) {
                                wrong_3 = false
                                line_3 = currentBufThree + 1
                                if (currentBufThree == 2) {
                                    val date123 =
                                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                                            Date()
                                        )
                                    val time123 =
                                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(
                                            Date()
                                        )
                                    dt3_3.text = "$date123 $time123"
                                    jsonData = JSONObject()
                                    val `object` = JSONObject()
                                    jsonData.put("DT_4", "$date123 $time123")
                                    jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                                    WebSocketManager.sendMessage(jsonData.toString())
                                    //                                    deviceRef.child("UI").child("PH").child("PH_CAL").child("DT_4").setValue(date123 + " " + time123);
                                    calibrateBtnThree.isEnabled = false
                                    Source.calib_completed_by = Source.logUserName
                                    calibrateBtnThree.text = "DONE"
                                    startTimer3()
                                }
                                if (currentBufThree == 0) {
                                    val date123 =
                                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                                            Date()
                                        )
                                    val time123 =
                                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(
                                            Date()
                                        )
                                    dt1_3.text = "$date123 $time123"
                                    jsonData = JSONObject()
                                    val `object` = JSONObject()
                                    jsonData.put("DT_2", "$date123 $time123")
                                    jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                                    WebSocketManager.sendMessage(jsonData.toString())
                                    //                                    deviceRef.child("UI").child("PH").child("PH_CAL").child("DT_2").setValue(date123 + " " + time123);
                                    log1_3.setBackgroundColor(Color.WHITE)
                                    log2_3.setBackgroundColor(Color.GRAY)
                                    log3_3.setBackgroundColor(Color.WHITE)
                                }
                                if (currentBufThree == 1) {
                                    val date123 =
                                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                                            Date()
                                        )
                                    val time123 =
                                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(
                                            Date()
                                        )
                                    dt2_3.text = "$date123 $time123"
                                    jsonData = JSONObject()
                                    jsonData.put("DT_3", "$date123 $time123")
                                    jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                                    WebSocketManager.sendMessage(jsonData.toString())
                                    //                                    deviceRef.child("UI").child("PH").child("PH_CAL").child("DT_3").setValue(date123 + " " + time123);
                                    log1_3.setBackgroundColor(Color.WHITE)
                                    log2_3.setBackgroundColor(Color.WHITE)
                                    log3_3.setBackgroundColor(Color.GRAY)
                                }
                                calibrateBtnThree.isEnabled = true
                                tvTimerThree.visibility = View.INVISIBLE
                                val currentTime = SimpleDateFormat(
                                    "yyyy-MM-dd HH:mm", Locale.getDefault()
                                ).format(
                                    Date()
                                )
                                bufferListThree.add(BufferData(null, null, currentTime))
                                //                        bufferListThree.add(new BufferData(null, null, currentTime));
                                jsonData = JSONObject()
                                jsonData.put(
                                    "CAL", (calValuesThree[currentBufThree] + 1).toString()
                                )
                                jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                                WebSocketManager.sendMessage(jsonData.toString())

//                                deviceRef.child("UI").child("PH").child("PH_CAL").child("CAL").setValue(calValuesThree[currentBufThree] + 1);
                                Log.e("cValue", currentBufThree.toString() + "")

//                                int b = currentBuf < 0 ? 4 : currentBuf;
                                val b = currentBufThree
                                Log.e("cValue2", currentBufThree.toString() + "")
                                Log.e("bValue", b.toString() + "")
                                val sharedPreferences = fragmentContext.getSharedPreferences(
                                    "CalibPrefs", Context.MODE_PRIVATE
                                )
                                val myEdit = sharedPreferences.edit()
                                if (b == 0) {
                                    myEdit.putString("tem1_3", tvTempCurr.text.toString())
                                    myEdit.commit()
                                    jsonData = JSONObject()
                                    jsonData.put("CALIBRATION_STAT", "incomplete")
                                    jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                                    WebSocketManager.sendMessage(jsonData.toString())
                                    //                                    deviceRef.child("Data").child("CALIBRATION_STAT").setValue("incomplete");
//                                    calibData3();

//                                    CalibDatClass calibDatClass1 = new CalibDatClass(1, PH1_3, MV1_3, SLOPE1_3, DT1_3, BFD1_3, pHAC1_3, t1_3, DT1_3.length() >= 15 ? DT1_3.substring(0, 10) : "--", DT1_3.length() >= 15 ? DT1_3.substring(11, 16) : "--");
                                    val calibDatClass1 = CalibDatClass(
                                        1,
                                        ph1_3.text.toString(),
                                        mv1_3.text.toString(),
                                        slope1_3.text.toString(),
                                        dt1_3.text.toString(),
                                        bufferD1_3.text.toString(),
                                        phAfterCalib1_3.text.toString(),

                                        tvTempCurr.text.toString(),
                                        if (dt1_3.text.toString().length >= 15) dt1_3.text.toString()
                                            .substring(0, 10) else "--",
                                        if (dt1_3.text.toString().length >= 15) dt1_3.text.toString()
                                            .substring(11, 16) else "--"
                                    )
                                    databaseHelper.updateClbOffDataThree(calibDatClass1)
                                    temp1_3.text = tvTempCurr.text
                                } else if (b == 1) {
                                    myEdit.putString("tem2_3", tvTempCurr.text.toString())
                                    myEdit.commit()
                                    //                                    calibData3();

//                                    CalibDatClass calibDatClass2 = new CalibDatClass(2, PH2_3, MV2_3, SLOPE2_3, DT2_3, BFD2_3, pHAC2_3, t2_3, DT2_3.length() >= 15 ? DT2_3.substring(0, 10) : "--", DT2_3.length() >= 15 ? DT2_3.substring(11, 16) : "--");
                                    val calibDatClass2 = CalibDatClass(
                                        2,
                                        ph2_3.text.toString(),
                                        mv2_3.text.toString(),
                                        slope2_3.text.toString(),
                                        dt2_3.text.toString(),
                                        bufferD2_3.text.toString(),
                                        phAfterCalib2_3.text.toString(),
                                        tvTempCurr.text.toString(),
                                        if (dt2_3.text.toString().length >= 15) dt2_3.text.toString()
                                            .substring(0, 10) else "--",
                                        if (dt2_3.text.toString().length >= 15) dt2_3.text.toString()
                                            .substring(11, 16) else "--"
                                    )
                                    databaseHelper.updateClbOffDataThree(calibDatClass2)
                                    temp2_3.text = tvTempCurr.text
                                } else if (b == 2) {
                                    myEdit.putString("tem3_3", tvTempCurr.text.toString())
                                    myEdit.commit()
                                    temp3_3.text = tvTempCurr.text
                                    jsonData = JSONObject()
                                    jsonData.put("CALIBRATION_STAT", "ok")
                                    jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                                    WebSocketManager.sendMessage(jsonData.toString())
                                    //                                    deviceRef.child("Data").child("CALIBRATION_STAT").setValue("ok");
//                                    calibData3()
                                    //                                    CalibDatClass calibDatClass3 = new CalibDatClass(3, PH3_3, MV3_3, SLOPE3_3, DT3_3, BFD3_3, pHAC3_3, t3_3, DT3_3.length() >= 15 ? DT3_3.substring(0, 10) : "--", DT3_3.length() >= 15 ? DT3_3.substring(11, 16) : "--");
                                    val calibDatClass3 = CalibDatClass(
                                        3,
                                        ph3_3.text.toString(),
                                        mv3_3.text.toString(),
                                        slope3_3.text.toString(),
                                        dt3_3.text.toString(),
                                        bufferD3_3.text.toString(),
                                        phAfterCalib3_3.text.toString(),
                                        tvTempCurr.text.toString(),
                                        if (dt3_3.text.toString().length >= 15) dt3_3.text.toString()
                                            .substring(0, 10) else "--",
                                        if (dt3_3.text.toString().length >= 15) dt3_3.text.toString()
                                            .substring(11, 16) else "--"
                                    )
                                    databaseHelper.updateClbOffDataThree(calibDatClass3)

                                    databaseHelper.insertCalibrationOfflineAllData(
                                        ph1_3.text.toString(),
                                        mv1_3.text.toString(),
                                        slope1_3.text.toString(),
                                        dt1_3.text.toString(),
                                        bufferD1_3.text.toString(),
                                        phAfterCalib1_3.text.toString(),
                                        tvTempCurr.text.toString(),
                                        if (dt1_3.text.toString().length >= 15) dt1_3.text.toString()
                                            .substring(0, 10) else "--",
                                        if (dt1_3.text.toString().length >= 15) dt1_3.text.toString()
                                            .substring(11, 16) else "--"
                                    )
                                    databaseHelper.insertCalibrationOfflineAllData(
                                        ph2_3.text.toString(),
                                        mv2_3.text.toString(),
                                        slope2_3.text.toString(),
                                        dt2_3.text.toString(),
                                        bufferD2_3.text.toString(),
                                        phAfterCalib2_3.text.toString(),
                                        tvTempCurr.text.toString(),
                                        if (dt2_3.text.toString().length >= 15) dt2_3.text.toString()
                                            .substring(0, 10) else "--",
                                        if (dt2_3.text.toString().length >= 15) dt2_3.text.toString()
                                            .substring(11, 16) else "--"
                                    )
                                    databaseHelper.insertCalibrationOfflineAllData(
                                        ph3_3.text.toString(),
                                        mv3_3.text.toString(),
                                        slope3_3.text.toString(),
                                        dt3_3.text.toString(),
                                        bufferD3_3.text.toString(),
                                        phAfterCalib3_3.text.toString(),
                                        tvTempCurr.text.toString(),
                                        if (dt3_3.text.toString().length >= 15) dt3_3.text.toString()
                                            .substring(0, 10) else "--",
                                        if (dt3_3.text.toString().length >= 15) dt3_3.text.toString()
                                            .substring(11, 16) else "--"
                                    )


                                }
                                currentBufThree += 1
//                                calibData3()
                                deleteAllOfflineCalibData()
                                databaseHelper.insertCalibrationOfflineData(
                                    ph1_3.text.toString(),
                                    mv1_3.text.toString(),
                                    slope1_3.text.toString(),
                                    dt1_3.text.toString(),
                                    bufferD1_3.text.toString(),
                                    phAfterCalib1_3.text.toString(),
                                    tvTempCurr.text.toString(),
                                    if (dt1_3.text.toString().length >= 15) dt1_3.text.toString()
                                        .substring(0, 10) else "--",
                                    if (dt1_3.text.toString().length >= 15) dt1_3.text.toString()
                                        .substring(11, 16) else "--"
                                )
                                databaseHelper.insertCalibrationOfflineData(
                                    ph2_3.text.toString(),
                                    mv2_3.text.toString(),
                                    slope2_3.text.toString(),
                                    dt2_3.text.toString(),
                                    bufferD2_3.text.toString(),
                                    phAfterCalib2_3.text.toString(),
                                    tvTempCurr.text.toString(),
                                    if (dt2_3.text.toString().length >= 15) dt2_3.text.toString()
                                        .substring(0, 10) else "--",
                                    if (dt2_3.text.toString().length >= 15) dt2_3.text.toString()
                                        .substring(11, 16) else "--"
                                )
                                databaseHelper.insertCalibrationOfflineData(
                                    ph3_3.text.toString(),
                                    mv3_3.text.toString(),
                                    slope3_3.text.toString(),
                                    dt3_3.text.toString(),
                                    bufferD3_3.text.toString(),
                                    phAfterCalib3_3.text.toString(),
                                    tvTempCurr.text.toString(),
                                    if (dt3_3.text.toString().length >= 15) dt3_3.text.toString()
                                        .substring(0, 10) else "--",
                                    if (dt3_3.text.toString().length >= 15) dt3_3.text.toString()
                                        .substring(11, 16) else "--"
                                )
                            } else {
//                            --line_3;
//                            --currentBufThree;
                                timer3!!.cancel()
                                handler33.removeCallbacks(this)
                                wrong_3 = false
                                calibrateBtnThree.isEnabled = true
                                showAlertDialogButtonClicked()
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }
                }
                runnable33.run()
            }
        }
        try {
            jsonData = JSONObject()
            jsonData.put("CAL", calValuesThree[currentBufThree].toString())
            jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
            WebSocketManager.sendMessage(jsonData.toString())
            timer3!!.start()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
//        if (!wrong_3) {
//        deviceRef.child("UI").child("PH").child("PH_CAL").child("CAL").setValue(calValuesThree[currentBufThree]).addOnSuccessListener(t -> {
//            timer3.start();
//        });
    }


    lateinit var handler2: Handler
    lateinit var runnable2: Runnable

    private fun startTimer3() {
        calibrateBtnThree.isEnabled = false
        LOG_INTERVAL_3 = 5f
        tvTimerThree.text = LOG_INTERVAL_3.toString()
        handler2 = Handler()
        runnable2 = object : Runnable {
            override fun run() {
                Log.d("Runnable", "Handler is working")
                calibrateBtnThree.isEnabled = false
                if (LOG_INTERVAL_3 == 0f) { // just remove call backs
                    tvTimerThree.text = LOG_INTERVAL_3.toString()
                    handler2.removeCallbacks(this)
                    calibrateBtnThree.isEnabled = true
                    calibrateBtnThree.text = "Start"
                    currentBufThree = 0
                    PhCalibFragmentNew.line_3 = 0
                    log1_3.setBackgroundColor(Color.GRAY)
                    log2_3.setBackgroundColor(Color.WHITE)
                    log3_3.setBackgroundColor(Color.WHITE)
                    tvTimerThree.text = "00:45"
                    Log.d("Runnable", "ok")
                } else { // post again
                    --LOG_INTERVAL_3
                    tvTimerThree.text = "00:0" + LOG_INTERVAL_3.toString().substring(0, 1)
                    handler2.postDelayed(this, 1000)
                }
            }
        }
        runnable2.run()
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

    lateinit var userDao: UserDao
    lateinit var userActionDao: UserActionDao

    override fun onResume() {
        super.onResume()

        userDao = Room.databaseBuilder(
            requireContext().applicationContext, AppDatabase::class.java, "aican-database"
        ).build().userDao()

        userActionDao = Room.databaseBuilder(
            requireContext().applicationContext,
            AppDatabase::class.java,
            "aican-database"
        ).build().userActionDao()

        if (Source.cfr_mode) {
            val userAuthDialog = UserAuthDialog(requireContext(), userDao)
            userAuthDialog.showLoginDialog { isValidCredentials ->
                if (isValidCredentials) {
                    addUserAction(
                        "username: " + Source.userName + ", Role: " + Source.userRole +
                                ", entered ph calib fragment", "", "", "", ""
                    )
                } else {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Invalid credentials", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }

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
        threePointCalib.visibility = View.GONE


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
                    Log.d("SpinnerSelection", "Parent: $parent")
                    Log.d("SpinnerSelection", "View: $view")
                    Log.d("SpinnerSelection", "Position: $position")
                    Log.d("SpinnerSelection", "Id: $id")
                    view ?: return

                    when (position) {
                        0 -> {
                            mode = "5"
                            Source.calibMode = 0
                            fivePointCalib.visibility = View.VISIBLE
                            threePointCalib.visibility = View.GONE
                            threePointCalibStart.visibility = View.GONE
                            fivePointCalibStart.visibility = View.VISIBLE
                            currentBuf = 0
                            currentBufThree = 0
                            PhCalibFragmentNew.line = 0
                            PhCalibFragmentNew.line_3 = 0
                            if (Constants.OFFLINE_MODE) {
                                try {
                                    jsonData = JSONObject()
                                    jsonData.put("CAL_MODE", 5.toString())
                                    jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                                    WebSocketManager.sendMessage(jsonData.toString())
                                } catch (e: JSONException) {
                                    throw java.lang.RuntimeException(e)
                                }
                            } else {
//                            deviceRef.child("UI").child("PH").child("PH_CAL").child("CAL")
//                                .setValue(0)
                            }
                            log1_3.setBackgroundColor(Color.GRAY)
                            log2_3.setBackgroundColor(Color.WHITE)
                            log3_3.setBackgroundColor(Color.WHITE)
                            log1.setBackgroundColor(Color.GRAY)
                            log2.setBackgroundColor(Color.WHITE)
                            log3.setBackgroundColor(Color.WHITE)
                            log4.setBackgroundColor(Color.WHITE)
                            log5.setBackgroundColor(Color.WHITE)
                            addUserAction(
                                "username: " + Source.userName + ", Role: " + Source.userRole +
                                        ", switched ph calib mode to 5", "", "", "", ""
                            )

                        }

                        1 -> {
                            mode = "3"
                            Source.calibMode = 1
                            fivePointCalib.visibility = View.GONE
                            fivePointCalibStart.visibility = View.GONE
                            threePointCalib.visibility = View.VISIBLE
                            threePointCalibStart.visibility = View.VISIBLE
                            //                        currentBufThree = 0;
                            currentBuf = 0
                            currentBufThree = 0
                            PhCalibFragmentNew.line = 0
                            PhCalibFragmentNew.line_3 = 0
                            if (Constants.OFFLINE_MODE) {
                                try {
                                    jsonData = JSONObject()
                                    jsonData.put("CAL_MODE", 3.toString())
                                    jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                                    WebSocketManager.sendMessage(jsonData.toString())
                                } catch (e: JSONException) {
                                    throw java.lang.RuntimeException(e)
                                }
                            } else {
                            }
                            log1_3.setBackgroundColor(Color.GRAY)
                            log2_3.setBackgroundColor(Color.WHITE)
                            log3_3.setBackgroundColor(Color.WHITE)
                            log1.setBackgroundColor(Color.GRAY)
                            log2.setBackgroundColor(Color.WHITE)
                            log3.setBackgroundColor(Color.WHITE)
                            log4.setBackgroundColor(Color.WHITE)
                            log5.setBackgroundColor(Color.WHITE)
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
        val calibCSV3: Cursor = db.rawQuery("SELECT * FROM CalibOfflineDataThree", null)
        val calibCSV5: Cursor = db.rawQuery("SELECT * FROM CalibOfflineDataFive", null)
        var index = 0
        if (calibCSV3.count == 0) {
            databaseHelper.insertCalibrationOfflineDataThree(
                1,
                ph1_3.text.toString(),
                mv1_3.text.toString(),
                slope1_3.text.toString(),
                dt1_3.text.toString(),
                bufferD1_3.text.toString(),
                phAfterCalib1_3.text.toString(),
                tvTempCurr.text.toString(),
                if (dt1_3.text.toString().length >= 15) dt1_3.text.toString()
                    .substring(0, 10) else "--",
                if (dt1_3.text.toString().length >= 15) dt1_3.text.toString()
                    .substring(11, 16) else "--"
            )
            databaseHelper.insertCalibrationOfflineDataThree(
                2,
                ph2_3.text.toString(),
                mv2_3.text.toString(),
                slope2_3.text.toString(),
                dt2_3.text.toString(),
                bufferD2_3.text.toString(),
                phAfterCalib2_3.text.toString(),
                tvTempCurr.text.toString(),
                if (dt2_3.text.toString().length >= 15) dt2_3.text.toString()
                    .substring(0, 10) else "--",
                if (dt2_3.text.toString().length >= 15) dt2_3.text.toString()
                    .substring(11, 16) else "--"
            )
            databaseHelper.insertCalibrationOfflineDataThree(
                3,
                ph3_3.text.toString(),
                mv3_3.text.toString(),
                slope3_3.text.toString(),
                dt3_3.text.toString(),
                bufferD3_3.text.toString(),
                phAfterCalib3_3.text.toString(),
                tvTempCurr.text.toString(),
                if (dt3_3.text.toString().length >= 15) dt3_3.text.toString()
                    .substring(0, 10) else "--",
                if (dt3_3.text.toString().length >= 15) dt3_3.text.toString()
                    .substring(11, 16) else "--"
            )
        }
        if (calibCSV5.count == 0) {
            databaseHelper.insertCalibrationOfflineDataFive(
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
        while (calibCSV3.moveToNext()) {
            val ph = calibCSV3.getString(calibCSV3.getColumnIndex("PH"))
            val mv = calibCSV3.getString(calibCSV3.getColumnIndex("MV"))
            val date = calibCSV3.getString(calibCSV3.getColumnIndex("DT"))
            val slope = calibCSV3.getString(calibCSV3.getColumnIndex("SLOPE"))
            val pHAC = calibCSV3.getString(calibCSV3.getColumnIndex("pHAC"))
            val temperature1 = calibCSV3.getString(calibCSV3.getColumnIndex("temperature"))
            if (index == 0) {
                ph1_3.text = ph
                mv1_3.text = mv
                slope1_3.text = slope
                dt1_3.text = date
                phAfterCalib1_3.text = pHAC
                temp1_3.text = temperature1
                PH1_3 = ph
                MV1_3 = mv
                SLOPE1_3 = slope
                DT1_3 = date
                pHAC1_3 = pHAC
                t1_3 = temperature1
            }
            if (index == 1) {
                ph2_3.text = ph
                mv2_3.text = mv
                slope2_3.text = slope
                dt2_3.text = date
                phAfterCalib2_3.text = pHAC
                temp2_3.text = temperature1
                PH2_3 = ph
                MV2_3 = mv
                SLOPE2_3 = slope
                DT2_3 = date
                pHAC2_3 = pHAC
                t2_3 = temperature1
            }
            if (index == 2) {
                ph3_3.text = ph
                mv3_3.text = mv
                slope3_3.text = slope
                dt3_3.text = date
                phAfterCalib3_3.text = pHAC
                temp3_3.text = temperature1
                PH3_3 = ph
                MV3_3 = mv
                SLOPE3_3 = slope
                DT3_3 = date
                pHAC3_3 = pHAC
                t3_3 = temperature1
            }
            index++
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


    fun reverseFileArray(fileArray: Array<File?>): Array<File?>? {
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

            R.id.phEdit1_3 -> {
                val dialog_3 = EditPhBufferDialog { ph ->
                    updateBufferValue(ph)
                    println("1")
                    if (Constants.OFFLINE_MODE && Constants.OFFLINE_DATA) {
                        try {
                            jsonData = JSONObject()
                            jsonData.put("B_2", java.lang.String.valueOf(ph))
                            jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                            WebSocketManager.sendMessage(jsonData.toString())
                            ph1_3.setText(java.lang.String.valueOf(ph))
                            val calibDatClass1 = CalibDatClass(
                                1,
                                ph1_3.text.toString(),
                                mv1_3.text.toString(),
                                slope1_3.text.toString(),
                                dt1_3.text.toString(),
                                bufferD1_3.text.toString(),
                                phAfterCalib1_3.text.toString(),
                                tvTempCurr.text.toString(),
                                if (dt1_3.text.toString().length >= 15) dt1_3.text.toString()
                                    .substring(0, 10) else "--",
                                if (dt1_3.text.toString().length >= 15) dt1_3.text.toString()
                                    .substring(11, 16) else "--"
                            )
                            databaseHelper.updateClbOffDataThree(calibDatClass1)

                            addUserAction(
                                "username: " + Source.userName + ", Role: " + Source.userRole +
                                        ", changed ph_buffer_1 value to " + ph + ", in calibmode 3",
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
                dialog_3.show(parentFragmentManager, null)
            }

            R.id.phEdit2_3 -> {
                val dialog1_3 = EditPhBufferDialog { ph ->
                    updateBufferValue(ph)
                    println("2")
                    if (Constants.OFFLINE_MODE && Constants.OFFLINE_DATA) {
                        try {
                            jsonData = JSONObject()
                            jsonData.put("B_3", java.lang.String.valueOf(ph))
                            jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                            WebSocketManager.sendMessage(jsonData.toString())
                            ph2_3.setText(java.lang.String.valueOf(ph))
                            val calibDatClass2 = CalibDatClass(
                                2,
                                ph2_3.text.toString(),
                                mv2_3.text.toString(),
                                slope2_3.text.toString(),
                                dt2_3.text.toString(),
                                bufferD2_3.text.toString(),
                                phAfterCalib2_3.text.toString(),
                                tvTempCurr.text.toString(),
                                if (dt2_3.text.toString().length >= 15) dt2_3.text.toString()
                                    .substring(0, 10) else "--",
                                if (dt2_3.text.toString().length >= 15) dt2_3.text.toString()
                                    .substring(11, 16) else "--"
                            )
                            databaseHelper.updateClbOffDataThree(calibDatClass2)

                            addUserAction(
                                "username: " + Source.userName + ", Role: " + Source.userRole +
                                        ", changed ph_buffer_2 value to " + ph + ", in calibmode 3",
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
                dialog1_3.show(parentFragmentManager, null)
            }

            R.id.phEdit3_3 -> {
                val dialog2_3 = EditPhBufferDialog { ph ->
                    updateBufferValue(ph)
                    println("3")
                    if (Constants.OFFLINE_MODE && Constants.OFFLINE_DATA) {
                        try {
                            jsonData = JSONObject()
                            jsonData.put("B_4", java.lang.String.valueOf(ph))
                            jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                            WebSocketManager.sendMessage(jsonData.toString())
                            ph3_3.setText(java.lang.String.valueOf(ph))
                            val calibDatClass3 = CalibDatClass(
                                3,
                                ph3_3.text.toString(),
                                mv3_3.text.toString(),
                                slope3_3.text.toString(),
                                dt3_3.text.toString(),
                                bufferD3_3.text.toString(),
                                phAfterCalib3_3.text.toString(),
                                tvTempCurr.text.toString(),
                                if (dt3_3.text.toString().length >= 15) dt3_3.text.toString()
                                    .substring(0, 10) else "--",
                                if (dt3_3.text.toString().length >= 15) dt3_3.text.toString()
                                    .substring(11, 16) else "--"
                            )
                            databaseHelper.updateClbOffDataThree(calibDatClass3)

                            addUserAction(
                                "username: " + Source.userName + ", Role: " + Source.userRole +
                                        ", changed ph_buffer_3 value to " + ph + ", in calibmode 3",
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
                dialog2_3.show(parentFragmentManager, null)
            }

            R.id.qr1 -> openQRActivity("qr1")
            R.id.qr2 -> openQRActivity("qr2")
            R.id.qr3 -> openQRActivity("qr3")
            R.id.qr4 -> openQRActivity("qr4")
            R.id.qr5 -> openQRActivity("qr5")
            R.id.qr1_3 -> openQRActivity("qr2")
            R.id.qr2_3 -> openQRActivity("qr3")
            R.id.qr3_3 -> openQRActivity("qr4")
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
        threePointCalib = view.findViewById<CardView>(R.id.threePointCalib)
        phMvTable = view.findViewById<Button>(R.id.phMvTable)
        phGraph = view.findViewById<Button>(R.id.phGraph)
        printCalibData = view.findViewById<Button>(R.id.printCalibData)
        printAllCalibData = view.findViewById<Button>(R.id.printAllCalibData)
        calibrateBtn = view.findViewById<Button>(R.id.startBtn)
        calibrateBtnThree = view.findViewById<Button>(R.id.startBtnThree)
        modeText = view.findViewById<TextView>(R.id.modeText)
        tvTimer = view.findViewById<TextView>(R.id.tvTimer)
        tvTimerThree = view.findViewById<TextView>(R.id.tvTimerThree)
        calibSpinner = view.findViewById<LinearLayout>(R.id.calibSpinner)
        spin = view.findViewById<Spinner>(R.id.calibMode)
        log1_3 = view.findViewById<LinearLayout>(R.id.log1_3)
        log2_3 = view.findViewById<LinearLayout>(R.id.log2_3)
        log3_3 = view.findViewById<LinearLayout>(R.id.log3_3)
        resetCalibFive = view.findViewById<Button>(R.id.resetCalibFive)
        resetCalibThree = view.findViewById<Button>(R.id.resetCalibThree)
        syncOfflineData = view.findViewById<Button>(R.id.syncOfflineData)
        fivePointCalibStart = view.findViewById(R.id.fivePointCalibStart)
        threePointCalibStart = view.findViewById<LinearLayout>(R.id.threePointCalibStart)
        log1 = view.findViewById<LinearLayout>(R.id.log1)
        log2 = view.findViewById<LinearLayout>(R.id.log2)
        log3 = view.findViewById<LinearLayout>(R.id.log3)
        log4 = view.findViewById<LinearLayout>(R.id.log4)
        log5 = view.findViewById<LinearLayout>(R.id.log5)
        log3point = view.findViewById<LinearLayout>(R.id.log3point)
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
        ph1_3 = view.findViewById<TextView>(R.id.ph1_3)
        ph2_3 = view.findViewById<TextView>(R.id.ph2_3)
        ph3_3 = view.findViewById<TextView>(R.id.ph3_3)
        phAfterCalib1_3 = view.findViewById<TextView>(R.id.phAfterCalib1_3)
        phAfterCalib2_3 = view.findViewById<TextView>(R.id.phAfterCalib2_3)
        phAfterCalib3_3 = view.findViewById<TextView>(R.id.phAfterCalib3_3)
        mv1_3 = view.findViewById<TextView>(R.id.mv1_3)
        mv2_3 = view.findViewById<TextView>(R.id.mv2_3)
        mv3_3 = view.findViewById<TextView>(R.id.mv3_3)
        temp1_3 = view.findViewById<TextView>(R.id.temp1_3)
        temp2_3 = view.findViewById<TextView>(R.id.temp2_3)
        temp3_3 = view.findViewById<TextView>(R.id.temp3_3)
        qr1_3 = view.findViewById<TextView>(R.id.qr1_3)
        qr2_3 = view.findViewById<TextView>(R.id.qr2_3)
        qr3_3 = view.findViewById<TextView>(R.id.qr3_3)
        bufferD1_3 = view.findViewById<TextView>(R.id.bufferD1_3)
        bufferD2_3 = view.findViewById<TextView>(R.id.bufferD2_3)
        bufferD3_3 = view.findViewById<TextView>(R.id.bufferD3_3)
        slope1_3 = view.findViewById<TextView>(R.id.slope1_3)
        slope2_3 = view.findViewById<TextView>(R.id.slope2_3)
        slope3_3 = view.findViewById<TextView>(R.id.slope3_3)
        bufferD1_3.setSelected(true)
        bufferD2_3.setSelected(true)
        bufferD3_3.setSelected(true)
        calibRecyclerView = view.findViewById<RecyclerView>(R.id.rvCalibFileView)
        phEdit1_3 = view.findViewById<TextView>(R.id.phEdit1_3)
        phEdit2_3 = view.findViewById<TextView>(R.id.phEdit2_3)
        phEdit3_3 = view.findViewById<TextView>(R.id.phEdit3_3)
        dt1_3 = view.findViewById<TextView>(R.id.dt1_3)
        dt2_3 = view.findViewById<TextView>(R.id.dt2_3)
        dt3_3 = view.findViewById<TextView>(R.id.dt3_3)
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
        phEdit1_3.setOnClickListener(View.OnClickListener { v: View? ->
            this.onClick(
                v!!
            )
        })
        phEdit2_3.setOnClickListener(View.OnClickListener { v: View? ->
            this.onClick(
                v!!
            )
        })
        phEdit3_3.setOnClickListener(View.OnClickListener { v: View? ->
            this.onClick(
                v!!
            )
        })
        qr1_3.setOnClickListener(View.OnClickListener { v: View? ->
            this.onClick(
                v!!
            )
        })
        qr2_3.setOnClickListener(View.OnClickListener { v: View? ->
            this.onClick(
                v!!
            )
        })
        qr3_3.setOnClickListener(View.OnClickListener { v: View? ->
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
    private lateinit var threePointCalibStart: LinearLayout
    private var connectedWebsocket = false
    private lateinit var log1: LinearLayout
    private lateinit var log2: LinearLayout
    private lateinit var log3: LinearLayout
    private lateinit var log4: LinearLayout
    private lateinit var log5: LinearLayout
    private lateinit var log1_3: LinearLayout
    private lateinit var log2_3: LinearLayout
    private lateinit var log3_3: LinearLayout
    private lateinit var log3point: LinearLayout
    private lateinit var log5point: LinearLayout
    private lateinit var calibSpinner: LinearLayout
    private lateinit var spin: Spinner
    private lateinit var tvTimer: TextView
    private lateinit var tvTimerThree: TextView
    private lateinit var tvTempCurr: TextView
    private lateinit var tvPhCurr: TextView
    private lateinit var modeText: TextView
    private lateinit var finalSlope: TextView
    private lateinit var calibrateBtn: Button
    private lateinit var calibrateBtnThree: Button
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
    private lateinit var calibFileAdapter: CalibFileAdapter

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

    private lateinit var MV1_3: String
    private lateinit var MV2_3: String
    private lateinit var MV3_3: String

    private lateinit var PH1_3: String
    private lateinit var PH2_3: String
    private lateinit var PH3_3: String

    private lateinit var DT1_3: String
    private lateinit var DT2_3: String
    private lateinit var DT3_3: String

    private lateinit var BFD1_3: String
    private lateinit var BFD2_3: String
    private lateinit var BFD3_3: String

    private lateinit var t1_3: String
    private lateinit var t2_3: String
    private lateinit var t3_3: String

    private lateinit var pHAC1_3: String
    private lateinit var pHAC2_3: String
    private lateinit var pHAC3_3: String

    private lateinit var mV1_3: String
    private lateinit var mV2_3: String
    private lateinit var mV3_3: String

    private lateinit var SLOPE1_3: String
    private lateinit var SLOPE2_3: String
    private lateinit var SLOPE3_3: String

    private lateinit var fivePointCalib: CardView
    private lateinit var threePointCalib: CardView


    private val bufferLabels = arrayOf("B_1", "B_2", "B_3", "B_4", "B_5")
    private val bufferLabelsThree = arrayOf("B_2", "B_3", "B_4")
    private val coeffLabels = arrayOf("VAL_1", "VAL_2", "VAL_3", "VAL_4", "VAL_5")
    private val postCoeffLabels =
        arrayOf("POST_VAL_1", "POST_VAL_2", "POST_VAL_3", "POST_VAL_4", "POST_VAL_5")
    private val postCoeffLabelsThree = arrayOf("POST_VAL_2", "POST_VAL_3", "POST_VAL_4")
    private val coeffLabelsThree = arrayOf("VAL_2", "VAL_3", "VAL_4")

    lateinit var resetCalibFive: Button
    lateinit var resetCalibThree: Button
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

    private lateinit var ph1_3: TextView
    private lateinit var ph2_3: TextView
    private lateinit var ph3_3: TextView

    private lateinit var phAfterCalib1_3: TextView
    private lateinit var phAfterCalib2_3: TextView
    private lateinit var phAfterCalib3_3: TextView

    private lateinit var slope1_3: TextView
    private lateinit var slope2_3: TextView
    private lateinit var slope3_3: TextView

    private lateinit var temp1_3: TextView
    private lateinit var temp2_3: TextView
    private lateinit var temp3_3: TextView

    private lateinit var mv1_3: TextView
    private lateinit var mv2_3: TextView
    private lateinit var mv3_3: TextView

    private lateinit var dt1_3: TextView
    private lateinit var dt2_3: TextView
    private lateinit var dt3_3: TextView

    private lateinit var bufferD1_3: TextView
    private lateinit var bufferD2_3: TextView
    private lateinit var bufferD3_3: TextView

    private lateinit var phEdit1_3: TextView
    private lateinit var phEdit2_3: TextView
    private lateinit var phEdit3_3: TextView

    private lateinit var qr1_3: TextView
    private lateinit var qr2_3: TextView
    private lateinit var qr3_3: TextView
    lateinit var phView: PhView
    lateinit var tvEcCurr: TextView

    lateinit var jsonData: JSONObject
    lateinit var databaseHelper: DatabaseHelper

}