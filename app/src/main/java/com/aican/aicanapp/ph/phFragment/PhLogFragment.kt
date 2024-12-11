package com.aican.aicanapp.ph.phFragment

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.aican.aicanapp.R
import com.aican.aicanapp.adapters.LogAdapter
import com.aican.aicanapp.adapters.PrintLogAdapter
import com.aican.aicanapp.data.DatabaseHelper
import com.aican.aicanapp.dataClasses.phData
import com.aican.aicanapp.databinding.FragmentPhLogBinding
import com.aican.aicanapp.dialogs.UserAuthDialog
import com.aican.aicanapp.ph.Export
import com.aican.aicanapp.ph.PhActivity
import com.aican.aicanapp.ph.phAnim.PhView
import com.aican.aicanapp.roomDatabase.daoObjects.AllLogsDataDao
import com.aican.aicanapp.roomDatabase.daoObjects.UserActionDao
import com.aican.aicanapp.roomDatabase.daoObjects.UserDao
import com.aican.aicanapp.roomDatabase.database.AppDatabase
import com.aican.aicanapp.roomDatabase.entities.AllLogsEntity
import com.aican.aicanapp.roomDatabase.entities.UserActionEntity
import com.aican.aicanapp.utils.AlarmConstants
import com.aican.aicanapp.utils.Constants
import com.aican.aicanapp.utils.SharedPref
import com.aican.aicanapp.utils.Source
import com.aican.aicanapp.utils.Source.deviceID
import com.aican.aicanapp.viewModels.SharedViewModel
import com.aican.aicanapp.websocket.WebSocketManager
import com.google.common.base.Splitter
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


class PhLogFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    lateinit var binding: FragmentPhLogBinding

    companion object {
        private val PERMISSION_REQUEST_CODE = 200

        var LOG_INTERVAL: Float = 0F
    }

    private var handler1: Handler? = null
    private lateinit var runnable1: Runnable
    private lateinit var jsonData: JSONObject

    var autoLogggg: Int = 0

    private lateinit var phView: PhView
    private lateinit var tvPhCurr: TextView
    private lateinit var tvPhNext: TextView
    private var ph: String = "0.0"
    private var temp: String = "0.0"
    private var mv: String = "0.0"
    private var date: String = ""
    private var time: String = ""
    private var batchnum: String = ""
    private var arnum: String = ""
    private var compound_name: String = ""
    private var ph_fetched: String = ""
    private var m_fetched: String = ""
    private var currentDate_fetched: String = ""
    private var currentTime_fetched: String = ""
    private var batchnum_fetched: String = ""
    private var arnum_fetched: String = ""
    private var compound_name_fetched: String = ""
    private var ph1: String = "0.0"
    private var mv1: String = "0.0"
    private var ph2: String = "0.0"
    private var mv2: String = "0.0"
    private var ph3: String = "0.0"
    private var mv3: String = "0.0"
    private var ph4: String = "0.0"
    private var mv4: String = "0.0"
    private var ph5: String = "0.0"
    private var mv5: String = "0.0"
    private var dt1: String = ""
    private var dt2: String = ""
    private var dt3: String = ""
    private var dt4: String = ""
    private var dt5: String = ""
    private lateinit var mode: String
    private lateinit var reportDate: String
    private lateinit var reportTime: String
    private lateinit var phDataModelList: ArrayList<phData>
    private lateinit var adapter: LogAdapter
    private lateinit var offset: String
    private lateinit var battery: String
    private lateinit var slope: String
    private lateinit var tempe: String
    private lateinit var temperature: String
    private lateinit var roleExport: String
    private lateinit var nullEntry: String
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var logBtn: Button
    private lateinit var exportBtn: Button
    private lateinit var printBtn: Button
    private lateinit var clearBtn: Button
    private lateinit var submitBtn: Button
    private lateinit var enterBtn: ImageButton
    private lateinit var batchBtn: ImageButton
    private lateinit var arBtn: ImageButton
    private lateinit var plAdapter: PrintLogAdapter
    private lateinit var compound_name_txt: EditText
    private lateinit var batch_number: EditText
    private lateinit var ar_number: EditText
    private lateinit var enterTime: EditText
    private lateinit var TABLE_NAME: String
    private lateinit var recyclerView: RecyclerView
    private var handler: Handler? = null
    private lateinit var runnable: Runnable
    private lateinit var switchHold: SwitchCompat
    private lateinit var switchInterval: SwitchCompat
    private lateinit var switchBtnClick: SwitchCompat
    private lateinit var autoLog: CardView
    private lateinit var autoLogWarn: TextView
    private lateinit var timer_cloud_layout: CardView
    private lateinit var saveTimer: ImageButton
    private lateinit var log_interval_text: TextView

    private var timerInSec: Int = 0
    private var isTimer: Boolean = false
    private var companyName: String = ""

    private var holdFlag: Int = 0
    var isAlertShow = true
    private val sharedViewModel: SharedViewModel by activityViewModels()

    private fun updateMessage(message: String) {
        sharedViewModel.messageLiveData.value = message
    }

    private fun updateError(error: String) {
        sharedViewModel.errorLiveData.value = error
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPhLogBinding.inflate(inflater, container, false);
        return binding.root;
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Constants.OFFLINE_MODE = true
        Constants.OFFLINE_DATA = true

        phView = view.findViewById(R.id.phView)
        tvPhCurr = view.findViewById(R.id.tvPhCurr)
        tvPhNext = view.findViewById(R.id.tvPhNext)
        autoLog = view.findViewById(R.id.autoLog)
        log_interval_text = view.findViewById(R.id.log_interval_text)

        logBtn = view.findViewById(R.id.logBtn)
        exportBtn = view.findViewById(R.id.export)
        enterBtn = view.findViewById(R.id.enter_text)
        printBtn = view.findViewById(R.id.print)
        compound_name_txt = view.findViewById(R.id.compound_name)
        batch_number = view.findViewById(R.id.batch_number)
        ar_number = view.findViewById(R.id.ar_number)
        batchBtn = view.findViewById(R.id.batch_text)
        arBtn = view.findViewById(R.id.ar_text)
        switchHold = view.findViewById(R.id.switchHold)
        switchInterval = view.findViewById(R.id.switchInterval)
        switchBtnClick = view.findViewById(R.id.switchBtnClick)
        clearBtn = view.findViewById(R.id.clear)
        submitBtn = view.findViewById(R.id.submit)
        enterTime = view.findViewById(R.id.EnterTime)
        timer_cloud_layout = view.findViewById(R.id.timer_cloud_layout)
        recyclerView = view.findViewById(R.id.recyclerViewLog)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)
        saveTimer = view.findViewById(R.id.sumbit_timer) as ImageButton
        val csvRecyclerView: RecyclerView = view.findViewById(R.id.recyclerViewCSVLog)
        csvRecyclerView.setHasFixedSize(true)
        csvRecyclerView.layoutManager = LinearLayoutManager(context)

        phDataModelList = ArrayList()
        databaseHelper = DatabaseHelper(context)
        adapter = LogAdapter(context, getSQLList())
        adapter.notifyItemInserted(0)
        recyclerView.adapter = adapter
        recyclerView.setHasFixedSize(true)
        val linearLayoutManager = LinearLayoutManager(requireContext())
        linearLayoutManager.reverseLayout = true
        linearLayoutManager.stackFromEnd = true
        recyclerView.layoutManager = linearLayoutManager
        nullEntry = " "
        jsonData = JSONObject()


        if (Constants.OFFLINE_DATA) {

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

        }

        if (checkPermission()) {
//            Toast.makeText(requireContext(), "Permission Granted", Toast.LENGTH_SHORT).show();
        } else {
            requestPermission();
        }

        submitBtn.setOnClickListener { view1: View? -> saveDetails() }

        exportBtn.isEnabled = true
        printBtn.isEnabled = true

        exportBtn.setOnClickListener {
            Source.status_export = true
            val time: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            addUserAction(
                "username: " + Source.userName + ", Role: " + Source.userRole + ", Moved to export activity",
                ph,
                temp,
                mv,
                compound_name
            )
            val sh = requireContext().getSharedPreferences("RolePref", MODE_PRIVATE)
            val roleE = sh.edit()
            val roleSuper = Source.logUserName
            roleE.putString("roleSuper", roleSuper)
            roleE.commit()
            deleteAll()
            databaseHelper.insertCalibData(ph1, mv1, dt1)
            databaseHelper.insertCalibData(ph2, mv2, dt2)
            databaseHelper.insertCalibData(ph3, mv3, dt3)
            databaseHelper.insertCalibData(ph4, mv4, dt4)
            databaseHelper.insertCalibData(ph5, mv5, dt5)
            if (Source.subscription == "cfr") {
                //                    DialogMain dialogMain = new DialogMain();
                //                    dialogMain.setCancelable(false);
                //                    Source.userTrack = "PhLogFragment logged in by ";
//                dialogMain.show(activity!!.supportFragmentManager, "example dialog")
            } else {
                val intent = Intent(requireContext(), Export::class.java)
                startActivity(intent)
            }
        }

        /**
         * Getting a log of pH, temp, the time and date of that respective moment, and the name of the compound
         */
        /**
         * Getting a log of pH, temp, the time and date of that respective moment, and the name of the compound
         */
        logBtn.setOnClickListener { v: View? ->
            if (Constants.OFFLINE_MODE) {
                date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                if (ph == null || temp == null || mv == null) {
//                    Toast.makeText(getContext(), "Fetching Data", Toast.LENGTH_SHORT).show();
                }
                ph = binding.tvPhCurr.text.toString()

                addUserAction(
                    "username: " + Source.userName + ", Role: " + Source.userRole + ", Log pressed",
                    ph,
                    temp,
                    mv,
                    compound_name
                )
                databaseHelper.print_insert_log_data(
                    date, time, ph, temp, batchnum, arnum, compound_name, PhActivity.DEVICE_ID
                )
//                databaseHelper.insert_log_data(
//                    date, time, ph, temp, batchnum, arnum, compound_name, PhActivity.DEVICE_ID
//                )

                addLogData(
                    ph, temp, batchnum, arnum, compound_name
                )

                adapter = LogAdapter(context, getList())
                recyclerView.adapter = adapter
            }

        }
        //        File exportDir = new File(requireContext().getExternalFilesDir(null).getAbsolutePath() + "/LabApp/Currentlog");
        //        File exportDir = new File(requireContext().getExternalFilesDir(null).getAbsolutePath() + "/LabApp/Currentlog");
        val exportDir =
            File(ContextWrapper(requireContext()).externalMediaDirs[0].toString() + "/LabApp/Currentlog")

        Log.e("FileNameErrorExportDir", exportDir.getPath())

        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }


//        printBtn.setOnClickListener(View.OnClickListener { //                printBtn.setB
//            try {
//                generatePDF()
//            } catch (e: FileNotFoundException) {
//                e.printStackTrace()
//            }
//            //                exportSensorCsv();
//
//            // calibration
//            // reset button
//            // voltage recieve and
//            val startsWith = "CurrentData"
//            //                String path = requireContext().getExternalFilesDir(null).getAbsolutePath() + File.separator + "/LabApp/Currentlog";
//            val path =
//                ContextWrapper(requireContext()).externalMediaDirs[0].toString() + File.separator + "/LabApp/Currentlog"
//            val root = File(path)
//            val filesAndFolders = root.listFiles()
//            Log.e("FileNameErrorDirRoot", root.path)
//            if (filesAndFolders == null || filesAndFolders.size == 0) {
//                Toast.makeText(requireContext(), "No Files Found", Toast.LENGTH_SHORT).show()
//                return@OnClickListener
//            } else {
//                for (i in filesAndFolders.indices) {
//                    filesAndFolders[i].name.startsWith(startsWith)
//                }
//            }
//
//
//            //                try {
//            //                    Workbook workbook = new Workbook(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "/LabApp/Currentlog/CurrentData.xlsx");
//            //                    PdfSaveOptions options = new PdfSaveOptions();
//            //                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault());
//            //                    String currentDateandTime = sdf.format(new Date());
//            //                    options.setCompliance(PdfCompliance.PDF_A_1_B);
//            //
//            ////                    File Pdfdir = new File(Environment.getExternalStorageDirectory()+"/LabApp/Currentlog/LogPdf");
//            ////                    if (!Pdfdir.exists()) {
//            ////                        if (!Pdfdir.mkdirs()) {
//            ////                            Log.d("App", "failed to create directory");
//            ////                        }
//            ////                    }
//            //                    String tempPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "/LabApp/Currentlog";
//            //                    File tempRoot = new File(tempPath);
//            //                    fileNotWrite(tempRoot);
//            //                    File[] tempFilesAndFolders = tempRoot.listFiles();
//            //                    workbook.save(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "/LabApp/Currentlog/CL_" + currentDateandTime + "_" + (tempFilesAndFolders.length - 1) + ".pdf", options);
//            //
//            //                    String path1 = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "/LabApp/Currentlog";
//            //                    File root1 = new File(path1);
//            //                    fileNotWrite(root1);
//            //                    File[] filesAndFolders1 = root1.listFiles();
//            //
//            //                    if (filesAndFolders1 == null || filesAndFolders1.length == 0) {
//            //
//            //                        return;
//            //                    } else {
//            //                        for (int i = 0; i < filesAndFolders1.length; i++) {
//            //                            if (filesAndFolders1[i].getName().endsWith(".csv") || filesAndFolders1[i].getName().endsWith(".xlsx")) {
//            //                                filesAndFolders1[i].delete();
//            //                            }
//            //                        }
//            //                    }
//            //
//            //                } catch (Exception e) {
//            //                    e.printStackTrace();
//            //                }
//
//
//            //                String pathPDF = requireContext().getExternalFilesDir(null).getPath() + File.separator + "/LabApp/Currentlog/";
//            val pathPDF =
//                ContextWrapper(requireContext()).externalMediaDirs[0].toString() + File.separator + "/LabApp/Currentlog/"
//            val rootPDF = File(pathPDF)
//            fileNotWrite(root)
//            val filesAndFoldersPDF = rootPDF.listFiles()
//            val filesAndFoldersNewPDF = arrayOfNulls<File>(1)
//            if (filesAndFoldersPDF == null || filesAndFoldersPDF.size == 0) {
//                return@OnClickListener
//            } else {
//                for (i in filesAndFoldersPDF.indices) {
//                    if (filesAndFoldersPDF[i].name.endsWith(".pdf")) {
//                        filesAndFoldersNewPDF[0] = filesAndFoldersPDF[i]
//                    }
//                }
//            }
//            plAdapter = PrintLogAdapter(
//                requireContext().applicationContext, reverseFileArray(filesAndFoldersPDF)
//            )
//            csvRecyclerView.adapter = plAdapter
//            plAdapter.notifyDataSetChanged()
//            csvRecyclerView.layoutManager = LinearLayoutManager(requireContext().applicationContext)
//        })


        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

//        String startsWith = "CurrentData";

//        String startsWith = "CurrentData";
        val path =
            ContextWrapper(requireContext()).externalMediaDirs[0].toString() + File.separator + "/LabApp/Currentlog"
        val root = File(path)
        val filesAndFolders = root.listFiles()

        plAdapter = PrintLogAdapter(requireContext().applicationContext, filesAndFolders)
        csvRecyclerView.adapter = plAdapter
        plAdapter.notifyDataSetChanged()
        csvRecyclerView.layoutManager = LinearLayoutManager(requireContext().applicationContext)
        if (checkPermission()) {
            Toast.makeText(
                requireContext().applicationContext, "Permission Granted", Toast.LENGTH_SHORT
            ).show()
        } else {
            requestPermission()
        }


        switchInterval.setOnCheckedChangeListener { compoundButton, b ->
            if (switchInterval.isChecked) {
                Constants.logIntervalActive = true
                autoLogggg = 2
                updateAutoLog()
                if (Constants.OFFLINE_MODE) {
                    try {
                        switchBtnClick.isChecked = false
                        switchHold.isChecked = false
                        jsonData = JSONObject()
                        enterTime.isEnabled = false
                        jsonData.put("AUTOLOG", "2")
                        jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                        WebSocketManager.sendMessage(jsonData.toString())
                        addUserAction(
                            "username: " + Source.userName + ", Role: " + Source.userRole + ", AUTOLOG = 2",
                            ph,
                            temp,
                            mv,
                            compound_name
                        )
                        Toast.makeText(
                            requireContext(), "C " + Constants.timeInSec, Toast.LENGTH_SHORT
                        ).show()
                        if (Constants.timeInSec == 0) {
                        } else {
                            val f = Constants.timeInSec.toFloat() / 60000
                            enterTime.setText("" + f)
                            if (handler != null) handler!!.removeCallbacks(runnable)
                            takeLog()
                            handler()
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            } else {
                enterTime.isEnabled = true
                Constants.logIntervalActive = false
                autoLogggg = 0
                updateAutoLog()
                if (Constants.OFFLINE_MODE) {
                    try {
                        isTimer = false
                        //                            Constants.timeInSec = 0;
                        jsonData = JSONObject()
                        jsonData.put("AUTOLOG", "0")
                        jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                        WebSocketManager.sendMessage(jsonData.toString())

                        addUserAction(
                            "username: " + Source.userName + ", Role: " + Source.userRole + ", AUTOLOG = 0",
                            ph,
                            temp,
                            mv,
                            compound_name
                        )
                        isAlertShow = true
                        if (handler != null) {
                            handler!!.removeCallbacks(runnable)
                            if (handler1 != null) {
                                handler1!!.removeCallbacks(runnable1)
                            }
                        }
                        if (!switchHold.isChecked && !switchBtnClick.isChecked) {
                            jsonData = JSONObject()
                            jsonData.put("AUTOLOG", "0")
                            jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                            WebSocketManager.sendMessage(jsonData.toString())
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
        }

        if (SharedPref.getSavedData(
                requireContext(), "LOG_INTERVAL_A"
            ) != null && SharedPref.getSavedData(requireContext(), "LOG_INTERVAL_A") !== ""
        ) {
            val d = SharedPref.getSavedData(requireContext(), "LOG_INTERVAL_A").toDouble() * 60000
            Toast.makeText(
                requireContext(),
                "" + SharedPref.getSavedData(requireContext(), "LOG_INTERVAL_A"),
                Toast.LENGTH_SHORT
            ).show()
            enterTime.setText(SharedPref.getSavedData(requireContext(), "LOG_INTERVAL_A"))
            Constants.timeInSec = d.toInt()
        } else {
            SharedPref.saveData(requireContext(), "LOG_INTERVAL_A", "0.1")
            Constants.timeInSec = 5000
        }

        saveTimer.setOnClickListener {
            if (!enterTime.text.toString().isEmpty()) {
                try {
                    val enteredValue = enterTime.text.toString().toDouble()
                    SharedPref.saveData(requireContext(), "LOG_INTERVAL_A", enteredValue.toString())
                    val db1 = enteredValue * 60000
                    Constants.timeInSec = db1.toInt()
                    Toast.makeText(
                        requireContext(), "A " + Constants.timeInSec, Toast.LENGTH_SHORT
                    ).show()
                    log_interval_text.text = (Constants.timeInSec / 60000).toString() + ""
                    if (switchInterval.isChecked) {
                        if (Constants.OFFLINE_MODE) {
                            if (!Constants.logIntervalActive) {
                                try {
                                    val d = enterTime.text.toString().toDouble() * 60000
                                    Constants.timeInSec = d.toInt()
                                    val a = Constants.timeInSec.toDouble() / 60000
                                    Log.d("TimerVal", "" + a)
                                    SharedPref.saveData(
                                        requireContext(),
                                        "LOG_INTERVAL_A",
                                        enterTime.text.toString()
                                    )

                                    //                            deviceRef.child("Data").child("LOG_INTERVAL").setValue(a);
                                    jsonData = JSONObject()
                                    jsonData.put("LOG_INTERVAL", a.toString())
                                    jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                                    WebSocketManager.sendMessage(jsonData.toString())
                                    LOG_INTERVAL = enterTime.text.toString().toFloat() * 60
                                    log_interval_text.text = java.lang.String.valueOf(LOG_INTERVAL)

                                    //                    startTimer();

                                    //                            takeLog();
                                    //                            handler();
                                } catch (e: JSONException) {
                                    e.printStackTrace()
                                }
                            } else {
                                Toast.makeText(
                                    requireContext(), "Already running", Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            val d = enterTime.text.toString().toDouble() * 60000
                            Constants.timeInSec = d.toInt()
                            val a = Constants.timeInSec.toDouble() / 60000
                            Log.d("TimerVal", "" + a)
                            LOG_INTERVAL = enterTime.text.toString().toFloat() * 60
                            log_interval_text.text = java.lang.String.valueOf(LOG_INTERVAL)

                            //                    startTimer();
                            takeLog()
                            handler()
                        }
                    }
                } catch (e: NumberFormatException) {
                    // Show an error message for invalid input
                    Toast.makeText(
                        requireContext(), "Please enter a valid number", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }



        switchBtnClick.setOnCheckedChangeListener { compoundButton, b ->
            if (switchBtnClick.isChecked) {
                autoLogggg = 3
                updateAutoLog()
                if (Constants.OFFLINE_MODE) {
                    try {
                        jsonData = JSONObject()
                        jsonData.put("AUTOLOG", "3")
                        jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                        WebSocketManager.sendMessage(jsonData.toString())
                        if (switchInterval.isChecked) {
                            isTimer = false
                            handler!!.removeCallbacks(runnable)
                            if (handler1 != null) {
                                handler1!!.removeCallbacks(runnable1)
                            }
                            switchInterval.isChecked = false
                        }
                        switchHold.isChecked = false
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            } else {
                autoLogggg = 0
                updateAutoLog()
                //                    if (!switchInterval.isChecked() && !switchHold.isChecked()) {
                if (Constants.OFFLINE_MODE) {
                    try {
                        jsonData = JSONObject()
                        jsonData.put("AUTOLOG", "0")
                        jsonData.put("LOG", "0")
                        jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                        WebSocketManager.sendMessage(jsonData.toString())
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
        }

        switchHold.setOnCheckedChangeListener { compoundButton, b ->
            if (switchHold.isChecked) {
                autoLogggg = 1
                updateAutoLog()
                if (Constants.OFFLINE_MODE) {
                    try {
                        jsonData = JSONObject()
                        //                            jsonData.put("HOLD", "0");
                        jsonData.put("AUTOLOG", "1")
                        jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                        WebSocketManager.sendMessage(jsonData.toString())
                        if (switchInterval.isChecked) {
                            isTimer = false
                            handler!!.removeCallbacks(runnable)
                            switchInterval.isChecked = false
                        }
                        switchBtnClick.isChecked = false
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            } else {
                holdFlag = 0
                if (!switchInterval.isChecked && !switchBtnClick.isChecked) {
                    autoLogggg = 0
                    updateAutoLog()
                    if (Constants.OFFLINE_MODE) {
                        try {
                            jsonData = JSONObject()
                            jsonData.put("AUTOLOG", "0")
                            jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                            WebSocketManager.sendMessage(jsonData.toString())
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        clearBtn.setOnClickListener {
            phDataModelList.clear()
            val ar = ArrayList<phData>()
            adapter = LogAdapter(context, ar)
            recyclerView.adapter = adapter
            val db = databaseHelper.writableDatabase
            val curCSV: Cursor? = db.rawQuery("SELECT * FROM PrintLogUserdetails", null)
            if (curCSV != null && curCSV.getCount() > 0) {
                deleteAllLogs()
            } else {
//                    Toast.makeText(requireContext(), "Database is empty, please insert values", Toast.LENGTH_SHORT).show();
            }
        }

        LOG_INTERVAL = if (enterTime.text.toString() == "") {
            (0 * 60).toFloat()
        } else {
            enterTime.text.toString().toFloat() * 60
        }
        log_interval_text.text = java.lang.String.valueOf(LOG_INTERVAL)

        updateAutoLog()

//        webSocketConnection()

//        setPreviousData()
        showPdfFiles()

        // Set click listener for the printBtn
        printBtn.setOnClickListener {
            try {
                generatePDF()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }

            val startsWith = "CurrentData"
            val path = ContextWrapper(requireContext()).externalMediaDirs[0].toString() + File.separator + "/LabApp/Currentlog"
            val root = File(path)
            val filesAndFolders = root.listFiles()

            if (filesAndFolders == null || filesAndFolders.isEmpty()) {
                Toast.makeText(requireContext(), "No Files Found", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Call the function to show PDF files after generating
            showPdfFiles()
        }
    }

    private fun showPdfFiles() {
        val pathPDF = ContextWrapper(requireContext()).externalMediaDirs[0].toString() + File.separator + "/LabApp/Currentlog/"
        val rootPDF = File(pathPDF)
        fileNotWrite(rootPDF)
        val filesAndFoldersPDF = rootPDF.listFiles() ?: return

        val filesAndFoldersNewPDF = arrayOfNulls<File>(1)
        for (i in filesAndFoldersPDF.indices) {
            if (filesAndFoldersPDF[i].name.endsWith(".pdf")) {
                filesAndFoldersNewPDF[0] = filesAndFoldersPDF[i]
                break
            }
        }

        plAdapter = PrintLogAdapter(
            requireContext().applicationContext, reverseFileArray(filesAndFoldersPDF)
        )
        binding.recyclerViewCSVLog.adapter = plAdapter
        plAdapter.notifyDataSetChanged()
        binding.recyclerViewCSVLog.layoutManager = LinearLayoutManager(requireContext().applicationContext)
    }

    private fun setPreviousData() {
        val phVal = SharedPref.getSavedData(requireContext(), "phValue" + PhActivity.DEVICE_ID)

        if (phVal != null) {
            var floatVal = 0.0f
            if (PhFragment.validateNumber(phVal)) {
                floatVal = phVal.toFloat()
                binding.tvPhCurr.text = floatVal.toString()
                binding.phView.moveTo(floatVal)
                ph = floatVal.toString()
                AlarmConstants.PH = floatVal
            }
        }

        val tempVal = SharedPref.getSavedData(requireContext(), "tempValue" + PhActivity.DEVICE_ID)
        if (tempVal != null) {
            temp = tempVal
//            binding.tvTempCurr.text = "$tempVal °C"
        }


        val slopeVal = SharedPref.getSavedData(requireContext(), "SLOPE_" + PhActivity.DEVICE_ID)

        if (slopeVal != null) {
            slope = slopeVal
//            binding.finalSlope.text2t = "$slopeVal %"

        }

        val ecValue = SharedPref.getSavedData(requireContext(), "ecValue" + PhActivity.DEVICE_ID)
        if (ecValue != null) {
//            binding.tvEcCurr.text = ecValue
        }


    }


    private fun webSocketConnection() {




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


            requireActivity().runOnUiThread {
                try {
                    updateMessage(message)

                    jsonData = JSONObject(message)
                    Log.d("JSONReceived:PHFragment", "onMessage: " + message)

                    if (jsonData.has("BATTERY") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                        val battery: String = jsonData.getString("BATTERY")
//                        binding.batteryPercent.setText("$battery %")
                        SharedPref.saveData(
                            requireContext(),
                            "battery" + PhActivity.DEVICE_ID,
                            battery
                        )

                    }

                    if (jsonData.has("PH_VAL") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                        var phk = 0.0f
                        if (jsonData.getString("PH_VAL") != "nan" && PhFragment.validateNumber(
                                jsonData.getString("PH_VAL")
                            )
                        ) {
                            phk = jsonData.getString("PH_VAL").toFloat()
                        }
                        tvPhCurr.text = phk.toString()
                        phView.moveTo(phk)
                        SharedPref.saveData(
                            requireContext(), "phValue" + PhActivity.DEVICE_ID, phk.toString()
                        )
                        ph = phk.toString()
                        AlarmConstants.PH = phk
                    }
                    if (jsonData.has("TEMP_VAL") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                        var tempval = 0.0f
                        if (jsonData.getString("TEMP_VAL") != "nan" && PhFragment.validateNumber(
                                jsonData.getString("TEMP_VAL")
                            )
                        ) {
                            tempval = jsonData.getString("TEMP_VAL").toFloat()

                        }
                        val temp1 = Math.round(tempval).toString()

                        if (tempToggleSharedPref != null){
                            if (tempToggleSharedPref == "true"){

                                temp = if (temp1.toInt() <= -127) {

                                    "NA"
                                } else {
                                    temp1
                                }

                        SharedPref.saveData(
                            requireContext(), "tempValue" + PhActivity.DEVICE_ID, temp
                        )
                            }
                        }else{
                            temp = if (temp1.toInt() <= -127) {

                                "NA"
                            } else {
                                temp1
                            }

                            SharedPref.saveData(
                                requireContext(), "tempValue" + PhActivity.DEVICE_ID, temp
                            )
                        }
                    }
                    if (jsonData.has("LOG") && jsonData.getString("LOG") == "1" && jsonData.getString(
                            "DEVICE_ID"
                        ) == PhActivity.DEVICE_ID
                    ) {
                        if (switchBtnClick.isChecked) {
                            date = SimpleDateFormat(
                                "yyyy-MM-dd", Locale.getDefault()
                            ).format(Date())
                            time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                            //                        fetch_logs();
                            if (ph == null || temp == null || mv == null) {
//                                Toast.makeText(getContext(), "Fetching Data", Toast.LENGTH_SHORT).show();
                            }
                            ph = binding.tvPhCurr.text.toString()

                            //                        } else {
//                            databaseHelper.print_insert_log_data(date, time, ph, temp, batchnum, arnum, compound_name, PhActivity.DEVICE_ID);
//                            databaseHelper.insert_log_data(date, time, ph, temp, batchnum, arnum, compound_name, PhActivity.DEVICE_ID);
//                            databaseHelper.insert_action_data(time, date, "Log pressed : " + Source.logUserName, ph, temp, mv, compound_name, PhActivity.DEVICE_ID);
//                        }
//                        if (Constants.OFFLINE_MODE) {
//                            databaseHelper.print_insert_log_data(date, time, ph, temp, batchnum, arnum, compound_name, PhActivity.DEVICE_ID);
//                            databaseHelper.insert_log_data(date, time, ph, temp, batchnum, arnum, compound_name, PhActivity.DEVICE_ID);
//                        }
                            takeLog()
                        }
                        //                        deviceRef.child("Data").child("LOG").setValue(0);
                    }
                    if (jsonData.has("HOLD") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                        if (switchHold.isChecked) {
                            if (jsonData.getString("HOLD") == "0") {
                                holdFlag = 0
                            }
                        }
                        if (switchHold.isChecked) {
                            if (jsonData.getString("HOLD") == "1") {
                                holdFlag++
                                date = SimpleDateFormat(
                                    "yyyy-MM-dd", Locale.getDefault()
                                ).format(Date())
                                time = SimpleDateFormat(
                                    "HH:mm", Locale.getDefault()
                                ).format(Date())
                                //                            fetch_logs();
//                                Toast.makeText(getContext(), "HOLD " + jsonData.getString("HOLD"), Toast.LENGTH_SHORT).show();
                                jsonData = JSONObject()
                                jsonData.put("HOLD", 0.toString())
                                jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                                WebSocketManager.sendMessage(jsonData.toString())
                                ph = binding.tvPhCurr.text.toString()

//                                deviceRef.child("Data").child("HOLD").setValue(0);
                                if (holdFlag == 1) {
                                    if (ph == null || temp == null || mv == null) {
//                                    Toast.makeText(getContext(), "Fetching Data", Toast.LENGTH_SHORT).show();
                                    }
                                    //                                } else {
//                                    databaseHelper.print_insert_log_data(date, time, ph, temp, batchnum, arnum, compound_name, PhActivity.DEVICE_ID);
//                                    databaseHelper.insert_log_data(date, time, ph, temp, batchnum, arnum, compound_name, PhActivity.DEVICE_ID);
//                                    databaseHelper.insert_action_data(time, date, "Log pressed : " + Source.userName, ph, temp, mv, compound_name, PhActivity.DEVICE_ID);
//                                }
                                    adapter = LogAdapter(context, getList())
                                    recyclerView.adapter = adapter
                                    if (Constants.OFFLINE_MODE) {
                                        databaseHelper.print_insert_log_data(
                                            date,
                                            time,
                                            ph,
                                            temp,
                                            batchnum,
                                            arnum,
                                            compound_name,
                                            PhActivity.DEVICE_ID
                                        )
//                                        databaseHelper.insert_log_data(
//                                            date,
//                                            time,
//                                            ph,
//                                            temp,
//                                            batchnum,
//                                            arnum,
//                                            compound_name,
//                                            PhActivity.DEVICE_ID
//                                        )

                                        addLogData(
                                            ph, temp, batchnum, arnum, compound_name
                                        )

                                    }
                                }
                            }
                        }
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

        }
    }


    private fun updateAutoLog() {
        val AutoLog = autoLogggg
        Source.auto_log = AutoLog
        if (AutoLog == 0) {
            exportBtn.isEnabled = true
            printBtn.isEnabled = true
        } else if (AutoLog == 1) {
            exportBtn.isEnabled = false
            printBtn.isEnabled = false
            switchHold.isChecked = true
            switchInterval.isChecked = false
            switchBtnClick.isChecked = false
        } else if (AutoLog == 2) {
            exportBtn.isEnabled = false
            printBtn.isEnabled = false
            isAlertShow = false
            switchHold.isChecked = false
            switchInterval.isChecked = true
            switchBtnClick.isChecked = false
        } else if (AutoLog == 3) {
            exportBtn.isEnabled = false
            printBtn.isEnabled = false
            switchHold.isChecked = false
            switchInterval.isChecked = false
            switchBtnClick.isChecked = true
        } else {
            exportBtn.isEnabled = true
            printBtn.isEnabled = true
        }
    }

    private fun autoLogs() {
        autoLog.visibility = View.GONE

    }

    private fun startTimer() {
        LOG_INTERVAL = enterTime.text.toString().toFloat() * 60
        log_interval_text.text = java.lang.String.valueOf(LOG_INTERVAL)
        handler1 = Handler()
        runnable1 = object : Runnable {
            override fun run() {
                if (!Constants.logIntervalActive) {
                    LOG_INTERVAL = 0F
                    log_interval_text.text = java.lang.String.valueOf(LOG_INTERVAL)
                    handler1!!.removeCallbacks(this)
                }
                Log.d("Runnable", "Handler is working")
                if (LOG_INTERVAL === 0.toFloat()) { // just remove call backs
                    log_interval_text.text = java.lang.String.valueOf(LOG_INTERVAL)
                    handler1!!.removeCallbacks(this)
                    Log.d("Runnable", "ok")
                } else { // post again
                    --LOG_INTERVAL
                    log_interval_text.text = java.lang.String.valueOf(LOG_INTERVAL)
                    handler1!!.postDelayed(this, 1000)
                }
            }
        }
        runnable1.run()
    }

    fun getCompanyLogo(): Bitmap? {
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

        Toast.makeText(requireContext(), "Printing...", Toast.LENGTH_LONG).show()

        var company_name = ""

        val companyname = SharedPref.getSavedData(requireContext(), "COMPANY_NAME")
        if (companyname != null) {
            company_name = "Company: $companyname"

        } else {
            company_name = "Company: N/A"

        }

        val user_name = "Username: " + Source.logUserName
//        val device_id = "DeviceID: " + PhActivity.DEVICE_ID
        val device_id = "Device Name: "+ SharedPref.getSavedData(requireActivity(), PhActivity.DEVICE_ID)
        reportDate = "Date: " + SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        reportTime = "Time: " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val shp = requireContext().getSharedPreferences("Extras", MODE_PRIVATE)
        offset = "Offset: " + shp.getString("offset", "")
        if (Constants.OFFLINE_DATA) {
            offset = if (SharedPref.getSavedData(
                    requireContext(), "OFFSET_" + PhActivity.DEVICE_ID
                ) != null && SharedPref.getSavedData(
                    requireContext(), "OFFSET_" + PhActivity.DEVICE_ID
                ) !== ""
            ) {
                val data =
                    SharedPref.getSavedData(requireContext(), "OFFSET_" + PhActivity.DEVICE_ID)
                "Offset: $data"
            } else {
                "Offset: " + "null"
            }
        } else {
        }
        tempe = "Temperature: " + shp.getString("temp", "")
        battery = "Battery: " + shp.getString("battery", "")
        if (Constants.OFFLINE_DATA) {
            slope = if (SharedPref.getSavedData(
                    requireContext(), "SLOPE_" + PhActivity.DEVICE_ID
                ) != null && SharedPref.getSavedData(
                    requireContext(), "SLOPE_" + PhActivity.DEVICE_ID
                ) !== ""
            ) {
                val data =
                    SharedPref.getSavedData(requireContext(), "SLOPE_" + PhActivity.DEVICE_ID)
                "Slope: $data"
            } else {
                "Slope: " + "null"
            }
            val tempData =
                SharedPref.getSavedData(requireContext(), "tempValue" + PhActivity.DEVICE_ID)
            tempe = if (SharedPref.getSavedData(
                    requireContext(), "tempValue" + PhActivity.DEVICE_ID
                ) != null && SharedPref.getSavedData(
                    requireContext(), "tempValue" + PhActivity.DEVICE_ID
                ) !== ""
            ) {

                "Temperature: $tempData"
            } else {
                "Temperature: " + "null"
            }

            val batteryVal =
                SharedPref.getSavedData(requireContext(), "battery" + PhActivity.DEVICE_ID)
            if (batteryVal != null) {
                if (batteryVal != "") {
                    battery = "Battery: $batteryVal %"
                }else{
                    battery = "Battery: 0 %"

                }
            }

        } else {
            slope = "Slope: " + shp.getString("slope", "")
        }

//        File exportDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "/LabApp/Currentlog");
//        if (!exportDir.exists()) {
//            exportDir.mkdirs();
//        }
        val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
        val currentDateandTime = sdf.format(Date())
        //        String tempPath = requireContext().getExternalFilesDir(null).getAbsolutePath() + "/LabApp/Currentlog";
        val tempPath =
            ContextWrapper(requireContext()).externalMediaDirs[0].toString() + "/LabApp/Currentlog"
        val tempRoot = File(tempPath)
        fileNotWrite(tempRoot)
        val tempFilesAndFolders = tempRoot.listFiles()

//        Toast.makeText(requireContext(), "" + tempFilesAndFolders.length, Toast.LENGTH_SHORT).show();

//        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator
//                + "/LabApp/Currentlog/CL_" + currentDateandTime + "_" + ((tempFilesAndFolders != null ? tempFilesAndFolders.length : 0) - 1)
//                + ".pdf");
        val filePath = ("" //                requireContext().getExternalFilesDir(null)
                + "/LabApp/Currentlog/CL_" + currentDateandTime + "_" + ((tempFilesAndFolders?.size
            ?: 0) - 1) + ".pdf")


//        File file = new File(requireContext().getExternalFilesDir(null).getAbsolutePath(), filePath);
        val file = File(ContextWrapper(requireContext()).externalMediaDirs[0], filePath)
        Log.e("FileNameError", file.path)
        Log.e("FileNameError", file.absolutePath)
        val outputStream: OutputStream = FileOutputStream(file)
        val writer = PdfWriter(file)
        val pdfDocument = PdfDocument(writer)
        val document = Document(pdfDocument)
        try {
            val imgBit = getCompanyLogo()
//            if (imgBit != null) {
//                val uri: Uri? = getImageUri(requireContext(), imgBit)
//                try {
//                    val add: String? = getPath(uri)
//                    val imageData = ImageDataFactory.create(add)
//                    val image: Image = Image(imageData).setHeight(80f).setWidth(80f)
//                    //                table12.addCell(new Cell(2, 1).add(image));
//                    // Adding image to the document
//                    document.add(image)
//                } catch (e: MalformedURLException) {
//                    e.printStackTrace()
//                }
//            }
            //

            if (imgBit != null) {

                val byteArrayOutputStream = ByteArrayOutputStream()
                imgBit.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()

// Create ImageData from byte array
                val imageData = ImageDataFactory.create(byteArray)

// Create an Image element
                val image = Image(imageData).setHeight(80f).setWidth(80f)
                document.add(image)

            }else{
//                Toast.makeText(requireContext(), "Null", Toast.LENGTH_SHORT).show()
            }
            ///
            if (Constants.OFFLINE_MODE) {
//                document.add(new Paragraph("Offline Mode"));
            }
            if (Source.cfr_mode) {

                document.add(
                    Paragraph(
                        """
                $company_name
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

//            document.add(
//                Paragraph(
//                    """$reportDate  |  $reportTime
//                                 $offset  |  $battery
//                                 $slope  |  $tempe"""
//                )
//            )

            document.add(
                Paragraph(
                    """$reportDate  |  $reportTime
                                 $offset 
                                 $slope  """
                )
            )

            val db = databaseHelper.writableDatabase
            var calibCSV: Cursor? = null
            if (Constants.OFFLINE_MODE || Constants.OFFLINE_DATA) {
                calibCSV = when (Source.calibMode) {
                    0 -> db.rawQuery("SELECT * FROM CalibOfflineDataFive", null)
                    1 -> db.rawQuery("SELECT * FROM CalibOfflineDataThree", null)
                    else -> null
                }
            } else {
                calibCSV = db.rawQuery("SELECT * FROM CalibData", null)
            }

            var tempSum = 0.0
            var tempCount = 0
            while (calibCSV?.moveToNext() == true) {
                val rawTempValue = calibCSV.getString(calibCSV.getColumnIndex("temperature"))
                val tempValue = rawTempValue?.replace("°C", "", ignoreCase = true)?.trim()?.toDoubleOrNull()
                if (tempValue != null) {
                    tempSum += tempValue
                    tempCount++
                }
            }

            calibCSV?.close()

            val averageTemp = if (tempCount > 0) tempSum / tempCount else null
            val averageTempText = averageTemp?.let { "Average Temperature: %.2f".format(it) } ?: "Average Temperature: null"
            document.add(Paragraph("$averageTempText"))


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

            if (Constants.OFFLINE_MODE) {
//            calibCSV = db.rawQuery("SELECT * FROM CalibOfflineData", null);
//            calibCSV = db.rawQuery("SELECT * FROM CalibOfflineData", null);
                if (Source.calibMode == 0) {
                    calibCSV = db.rawQuery("SELECT * FROM CalibOfflineDataFive", null)
                }
                if (Source.calibMode == 1) {
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
                table.addCell(ph ?: "--")
                table.addCell(pHAC ?: "--")
                table.addCell(slope ?: "--")
                table.addCell(mv ?: "--")
                table.addCell(date ?: "--")
                table.addCell(temperature1 ?: "--")
            }
            document.add(table)
            document.add(Paragraph(""))
            document.add(Paragraph("Log Table"))
            val columnWidth1 = floatArrayOf(240f, 120f, 150f, 150f, 270f, 270f, 270f)
            val table1 = Table(columnWidth1)
            table1.addCell("Date")
            table1.addCell("Time")
            table1.addCell("pH")
            table1.addCell("Temp")
            table1.addCell("Batch No")
            table1.addCell("AR No")
            table1.addCell("Compound")
            val curCSV: Cursor
            curCSV = if (Constants.OFFLINE_MODE) {
                db.rawQuery("SELECT * FROM PrintLogUserdetails", null)
            } else {
                db.rawQuery("SELECT * FROM PrintLogUserdetails", null)
            }
            while (curCSV.moveToNext()) {
                val date = curCSV.getString(curCSV.getColumnIndex("date"))
                val time = curCSV.getString(curCSV.getColumnIndex("time"))
                val pH = curCSV.getString(curCSV.getColumnIndex("ph"))
                val temp = curCSV.getString(curCSV.getColumnIndex("temperature"))
                val batchnum = curCSV.getString(curCSV.getColumnIndex("batchnum"))
                val arnum = curCSV.getString(curCSV.getColumnIndex("arnum"))
                val comp = curCSV.getString(curCSV.getColumnIndex("compound"))
                var newBatchNum: String? = "--"
                if (batchnum != null && batchnum.length >= 8) {
                    newBatchNum = stringSplitter(batchnum)
                } else {
                    newBatchNum = batchnum
                }
                var newArum: String? = "--"
                if (arnum != null && arnum.length >= 8) {
                    newArum = stringSplitter(arnum)
                } else {
                    newArum = arnum
                }
                var newComp: String? = "--"
                if (comp != null && comp.length >= 8) {
                    newComp = stringSplitter(comp)
                } else {
                    newComp = comp
                }
                table1.addCell(date ?: "--")
                table1.addCell(time ?: "--")
                table1.addCell(pH ?: "--")
                table1.addCell(temp ?: "--")
                table1.addCell(newBatchNum ?: "--")
                table1.addCell(newArum ?: "--")
                table1.addCell(newComp ?: "--")
            }
            document.add(table1)
            document.add(Paragraph("Operator Sign                                                                                      Supervisor Sign"))
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

            }else{
//                Toast.makeText(requireContext(), "Null", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(), "Error : " + e.message, Toast.LENGTH_SHORT
            ).show()
        }
        document.close()
        Toast.makeText(context, "Pdf generated", Toast.LENGTH_SHORT).show()
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
        val sh = requireContext().getSharedPreferences("signature", MODE_PRIVATE)
        val photo = sh.getString("signature_data", "")
        var bitmap: Bitmap? = null
        if (!photo.equals("", ignoreCase = true)) {
            val b = Base64.decode(photo, Base64.DEFAULT)
            bitmap = BitmapFactory.decodeByteArray(b, 0, b.size)
        }
        return bitmap
    }

    private fun stringSplitter(str: String): String? {
        var newText = ""
        val strings = Splitter.fixedLength(8).split(str)
        for (temp in strings) {
            newText = "$newText $temp"
        }
        return newText.trim { it <= ' ' }
    }

    private fun saveDetails() {
        if (Constants.OFFLINE_MODE) {
            if (!compound_name_txt.text.toString().isEmpty()) {
                compound_name = compound_name_txt.text.toString()
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

                addUserAction(
                    "username: " + Source.userName + ", Role: " + Source.userRole + ", Compound name changed to " + compound_name,
                    ph,
                    temp,
                    mv,
                    compound_name
                )
            } else {
                compound_name = "NA"

                addUserAction(
                    "username: " + Source.userName + ", Role: " + Source.userRole + ", Compound name changed to " + compound_name,
                    ph,
                    temp,
                    mv,
                    compound_name
                )
            }
            if (!batch_number.text.toString().isEmpty()) {
                batchnum = batch_number.text.toString()
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

                addUserAction(
                    "username: " + Source.userName + ", Role: " + Source.userRole + ", Batchnum changed to " + batchnum,
                    ph,
                    temp,
                    mv,
                    compound_name
                )
            } else {
                batchnum = "NA"
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

                addUserAction(
                    "username: " + Source.userName + ", Role: " + Source.userRole + ", Batchnum changed to " + batchnum,
                    ph,
                    temp,
                    mv,
                    compound_name
                )
            }
            if (!ar_number.text.toString().isEmpty()) {
                arnum = ar_number.text.toString()
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

                addUserAction(
                    "username: " + Source.userName + ", Role: " + Source.userRole + ", AR_NUMBER changed to " + arnum,
                    ph,
                    temp,
                    mv,
                    compound_name
                )
            } else {
                arnum = "NA"
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                addUserAction(
                    "username: " + Source.userName + ", Role: " + Source.userRole + ", AR_NUMBER changed to " + arnum,
                    ph,
                    temp,
                    mv,
                    compound_name
                )
            }
        }
    }

    override fun onDestroy() {
        deleteAllLogs()
        deleteAllLogsOffline()
        super.onDestroy()
    }

    override fun onPause() {
        Log.d("Timer", "onPause: ")
        if (handler != null) handler!!.removeCallbacks(runnable)
        super.onPause()
    }

    fun handler() {
        Log.d("Timer", "doInBackground: in while " + Constants.timeInSec)


//        Toast.makeText(getContext(), "Background service running ", Toast.LENGTH_SHORT).show();
//        Toast.makeText(getContext(), Constants.timeInSec + "", Toast.LENGTH_SHORT).show();
        if (handler1 != null) {
            handler1!!.removeCallbacks(runnable1)
        }
        if (handler != null) {
            handler!!.removeCallbacks(runnable)
        }
        startTimer()
        handler = Handler()
        runnable = object : Runnable {
            override fun run() {
                if (!Constants.logIntervalActive) {
                    handler!!.removeCallbacks(this)
                }
                Log.d("Timer", "doInBackground: in handler")
                takeLog()
                handler()
            }
        }
        handler!!.postDelayed(runnable, Constants.timeInSec.toLong())
        Log.d("Timer", "doInBackground: out handler")
    }

    fun takeLog() {
        date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        ph = binding.tvPhCurr.text.toString()
        if (Constants.OFFLINE_MODE) {
            databaseHelper.print_insert_log_data(
                date, time, ph, temp, batchnum, arnum, compound_name, PhActivity.DEVICE_ID
            )
//            databaseHelper.insert_log_data(
//                date, time, ph, temp, batchnum, arnum, compound_name, PhActivity.DEVICE_ID
//            )
            addLogData(
                ph, temp, batchnum, arnum, compound_name
            )
            databaseHelper.insert_action_data(
                time,
                date,
                "Log pressed : " + Source.logUserName,
                ph,
                temp,
                mv,
                compound_name,
                PhActivity.DEVICE_ID
            )
        }
        adapter = LogAdapter(context, getList())
        recyclerView.adapter = adapter
    }

    private fun fileNotWrite(file: File) {
        file.setWritable(false)
        if (file.canWrite()) {
            Log.d("csv", "Not Working")
        } else {
            Log.d("csvnw", "Working")
        }
    }


    private fun getList(): List<phData?>? {
        phDataModelList.add(0, phData(ph, temp, date, time, batchnum, arnum, compound_name))
        return phDataModelList
    }

    private fun deleteAll() {
        val db = databaseHelper.writableDatabase
        db.execSQL("DELETE FROM Calibdetails")
        db.close()
    }


    private fun deleteAllLogs() {
        val db = databaseHelper.writableDatabase
        db.execSQL("DELETE FROM PrintLogUserdetails")
        db.close()
    }

    private fun deleteAllLogsOffline() {
        val db = databaseHelper.writableDatabase
        db.execSQL("DELETE FROM PrintLogUserdetailsOffline")
        db.close()
    }

    private fun getSQLList(): ArrayList<phData>? {
        val res = databaseHelper._log
        if (res.count == 0) {
            Toast.makeText(context, "No entry", Toast.LENGTH_SHORT).show()
        }
        while (res.moveToNext()) {
            date = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date())
            time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            currentDate_fetched = res.getString(0)
            currentTime_fetched = res.getString(1)
            ph_fetched = res.getString(2)
            m_fetched = res.getString(3)
            batchnum_fetched = res.getString(4)
            arnum_fetched = res.getString(5)
            compound_name_fetched = res.getString(6)
            if (date == currentDate_fetched && time == currentTime_fetched) {
                phDataModelList.add(
                    0, phData(
                        ph_fetched,
                        m_fetched,
                        currentDate_fetched,
                        currentTime_fetched,
                        batchnum_fetched,
                        arnum_fetched,
                        compound_name_fetched
                    )
                )
            }
        }
        return phDataModelList
    }


    /**
     * checking of permissions.
     *
     * @return
     */
    private fun checkPermission(): Boolean {
        val permission1 =
            ContextCompat.checkSelfPermission(requireContext(), WRITE_EXTERNAL_STORAGE)
        val permission2 = ContextCompat.checkSelfPermission(requireContext(), READ_EXTERNAL_STORAGE)
        return permission1 == PackageManager.PERMISSION_GRANTED && permission2 == PackageManager.PERMISSION_GRANTED
    }

    /**
     * requesting permissions if not provided.
     */
    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            (requireContext() as Activity), arrayOf(
                WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE
            ), PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty()) {
                val writeStorage = grantResults[0] == PackageManager.PERMISSION_GRANTED
                val readStorage = grantResults[1] == PackageManager.PERMISSION_GRANTED
                if (writeStorage && readStorage) {
                    Toast.makeText(requireContext(), "Permission Granted..", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Toast.makeText(requireContext(), "Permission Denined.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
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

    fun addUserAction(action: String, ph: String, temp: String, mv: String, compound: String) {
        lifecycleScope.launch(Dispatchers.IO) {

            userActionDao.insertUserAction(
                UserActionEntity(
                    0,
                    Source.getCurrentTime(),
                    Source.getPresentDate(),
                    action,
                    ph,
                    temp,
                    mv,
                    compound,
                    PhActivity.DEVICE_ID.toString()
                )
            )
        }
    }

    fun addLogData(
        ph: String,
        temperature: String,
        batchnum: String,
        arnum: String,
        compound: String,
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            allLogsDataDao.insertLogData(
                AllLogsEntity(
                    0,
                    Source.getPresentDate(),
                    Source.getCurrentTime(),
                    ph,
                    temperature,
                    batchnum,
                    arnum,
                    compound,
                    PhActivity.DEVICE_ID.toString()
                )
            )
        }
    }

    lateinit var userDao: UserDao
    lateinit var userActionDao: UserActionDao
    lateinit var allLogsDataDao: AllLogsDataDao
    var tempToggleSharedPref: String? = null

    override fun onResume() {
        super.onResume()

        webSocketConnection()

        tempToggleSharedPref =
            SharedPref.getSavedData(requireContext(), "setTempToggle" + PhActivity.DEVICE_ID)

        setPreviousData()

        userDao = Room.databaseBuilder(
            requireContext().applicationContext, AppDatabase::class.java, "aican-database"
        ).build().userDao()

        allLogsDataDao = Room.databaseBuilder(
            requireContext().applicationContext, AppDatabase::class.java, "aican-database"
        ).build().allLogsDao()

        userActionDao = Room.databaseBuilder(
            requireContext().applicationContext, AppDatabase::class.java, "aican-database"
        ).build().userActionDao()

        if (Source.cfr_mode) {
            val userAuthDialog = UserAuthDialog(requireContext(), userDao)
            userAuthDialog.showLoginDialog { isValidCredentials ->
                if (isValidCredentials) {
                    addUserAction(
                        "username: " + Source.userName + ", Role: " + Source.userRole + ", entered ph log fragment",
                        "",
                        "",
                        "",
                        ""
                    )
                } else {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Invalid credentials", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }

        Source.activeFragment = 2
        if (handler != null && runnable != null) {
            handler!!.removeCallbacks(runnable)
        }
        if (handler1 != null && runnable1 != null) {
            handler1!!.removeCallbacks(runnable1)
        }
    }

    override fun onStart() {
        super.onStart()

    }


}