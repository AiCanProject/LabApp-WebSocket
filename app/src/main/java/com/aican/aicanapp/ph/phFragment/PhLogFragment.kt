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
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.aican.aicanapp.R
import com.aican.aicanapp.adapters.LogAdapter
import com.aican.aicanapp.adapters.PDF_CSV_Adapter
import com.aican.aicanapp.data.DatabaseHelper
import com.aican.aicanapp.dataClasses.phData
import com.aican.aicanapp.databinding.FragmentPhLogBinding
import com.aican.aicanapp.ph.Export
import com.aican.aicanapp.ph.PhActivity
import com.aican.aicanapp.ph.PhLogGraph
import com.aican.aicanapp.ph.phAnim.PhView
import com.aican.aicanapp.roomDatabase.daoObjects.AllLogsDataDao
import com.aican.aicanapp.roomDatabase.daoObjects.ProductsListDao
import com.aican.aicanapp.roomDatabase.daoObjects.UserActionDao
import com.aican.aicanapp.roomDatabase.daoObjects.UserDao
import com.aican.aicanapp.roomDatabase.database.AppDatabase
import com.aican.aicanapp.roomDatabase.entities.AllLogsEntity
import com.aican.aicanapp.roomDatabase.entities.UserActionEntity
import com.aican.aicanapp.utils.AlarmConstants
import com.aican.aicanapp.utils.Constants
import com.aican.aicanapp.utils.SharedKeys
import com.aican.aicanapp.utils.SharedPref
import com.aican.aicanapp.utils.Source
import com.aican.aicanapp.viewModels.ProductViewModel
import com.aican.aicanapp.viewModels.ProductViewModelFactory
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
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
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


class PhLogFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    lateinit var binding: FragmentPhLogBinding
    lateinit var graphView: GraphView

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
    private lateinit var plAdapter: PDF_CSV_Adapter
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
        databaseHelper = DatabaseHelper(requireContext())

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
        graphView = binding.graph

        phDataModelList = ArrayList()
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

        graphView.viewport.isScalable = true

        graphView.viewport.isScrollable = true

        graphView.viewport.setScalableY(true)

        graphView.viewport.setScrollableY(true)

        graphView.viewport.isXAxisBoundsManual = true
        graphView.viewport.setMinX(-2.0)
        graphView.viewport.setMaxX(20.0)

        graphView.viewport.isYAxisBoundsManual = true
        graphView.viewport.setMinY(-800.0)
        graphView.viewport.setMaxY(800.0)


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

        submitBtn.setOnClickListener { saveDetails() }

        exportBtn.isEnabled = true
        logBtn.isEnabled = true
        printBtn.isEnabled = true
        binding.printGraph.isEnabled = true

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
            val roleSuper = Source.userName
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
        logBtn.setOnClickListener {
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




        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())


        val path =
            ContextWrapper(requireContext()).externalMediaDirs[0].toString() + File.separator + "/LabApp/Currentlog"
        val root = File(path)

        if (checkPermission()) {
            Toast.makeText(
                requireContext().applicationContext, "Permission Granted", Toast.LENGTH_SHORT
            ).show()
        } else {
            requestPermission()
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

        switchInterval.setOnCheckedChangeListener { compoundButton, b ->
            if (switchInterval.isChecked) {
//                Constants.logIntervalActive = true
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
//                        Toast.makeText(
//                            requireContext(), "C " + Constants.timeInSec, Toast.LENGTH_SHORT
//                        ).show()

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
                                Constants.logIntervalActive = true
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

                                    if (Constants.timeInSec != 0) {
                                        val f = Constants.timeInSec.toFloat() / 60000
                                        enterTime.setText("" + f)
                                        if (handler != null) handler!!.removeCallbacks(runnable)
                                        takeLog()
                                        handler()
                                    }

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

        clearBtn.setOnClickListener {
//            series.resetData(arrayOf())

            // Redraw the graph
            graphView.invalidate()

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
        binding.printCSV.visibility = View.VISIBLE
        binding.print.visibility = View.VISIBLE

        if (Source.EXPORT_CSV) {
            binding.printCSV.visibility = View.VISIBLE
        } else {
            binding.printCSV.visibility = View.GONE
        }

        if (Source.EXPORT_PDF) {
            binding.print.visibility = View.VISIBLE
        } else {
            binding.print.visibility = View.GONE

        }

        // Set click listener for the printBtn
        printBtn.setOnClickListener {
            try {

                Source.showLoading(
                    requireActivity(), false, false, "Generating pdf...",
                    false
                )
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        addUserAction(
                            "username: " + Source.userName + ", Role: " + Source.userRole +
                                    ", print log report pdf", "", "", "", ""
                        )
                        generatePDF()

                        launch(Dispatchers.Main) {
                            val startsWith = "CurrentData"
                            val path =
                                ContextWrapper(requireContext()).externalMediaDirs[0].toString() + File.separator + "/LabApp/Currentlog"
                            val root = File(path)
                            val filesAndFolders = root.listFiles()

                            if (filesAndFolders == null || filesAndFolders.isEmpty()) {
                                Toast.makeText(
                                    requireContext(),
                                    "No Files Found",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            // Call the function to show PDF files after generating
                            showPdfFiles()
                            Source.cancelLoading()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        launch(Dispatchers.Main) {
                            Source.cancelLoading()
                            Toast.makeText(
                                requireActivity(),
                                "Failed to generate PDF",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }


//                exportLogCsv()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }


        }


        binding.printCSV.setOnClickListener {
            try {

                Source.showLoading(
                    requireActivity(), false, false, "Generating csv...",
                    false
                )

                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        addUserAction(
                            "username: " + Source.userName + ", Role: " + Source.userRole +
                                    ", print log report csv", "", "", "", ""
                        )

                        exportLogCsv()
                        launch(Dispatchers.Main) {
                            val startsWith = "CurrentData"
                            val path =
                                ContextWrapper(requireContext()).externalMediaDirs[0].toString() + File.separator + "/LabApp/Currentlog"
                            val root = File(path)
                            val filesAndFolders = root.listFiles()

                            if (filesAndFolders == null || filesAndFolders.isEmpty()) {
                                Toast.makeText(
                                    requireContext(),
                                    "No Files Found",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            // Call the function to show PDF files after generating
                            showPdfFiles()
                            Source.cancelLoading()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        launch(Dispatchers.Main) {
                            Source.cancelLoading()
                            Toast.makeText(
                                requireActivity(),
                                "Failed to generate PDF",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
//                generatePDF()


            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }


        }

        binding.printGraph.setOnClickListener {
            printGraph()
        }


    }

    private fun printGraph() {
        val db = databaseHelper.writableDatabase
        val curCSV: Cursor
        curCSV = if (Constants.OFFLINE_MODE) {
            db.rawQuery("SELECT * FROM PrintLogUserdetails", null)
        } else {
            db.rawQuery("SELECT * FROM PrintLogUserdetails", null)
        }
        PhLogGraph.phDataArrayList.clear()
        while (curCSV.moveToNext()) {
            val date = curCSV.getString(curCSV.getColumnIndex("date"))
            val time = curCSV.getString(curCSV.getColumnIndex("time"))
            val pH = curCSV.getString(curCSV.getColumnIndex("ph"))

            if (PhFragment.validateNumber(pH)) {
                PhLogGraph.phDataArrayList.add(pH.toFloat())
            }
        }

//        val inet = Intent(requireContext(), PhLogGraph::class.java)
//        startActivity(inet)


        val seriesData = ArrayList<DataPoint>()
        seriesData.add(DataPoint(0.0, 0.0))
        var i = 1
        for (data in PhLogGraph.phDataArrayList) {
            seriesData.add(DataPoint(i.toDouble(), data.toDouble()))
            i++
        }


        val series = LineGraphSeries<DataPoint>(
            seriesData.toTypedArray()
        )
        graphView.addSeries(series)
        series.isDrawDataPoints = true
        series.setAnimated(true)

        generateGraphPDF(graphView)

    }

    private fun generateGraphPDF(graphView: GraphView) {
        // Create a new PDF document
        var company_name = ""

        val companyname = SharedPref.getSavedData(requireContext(), "COMPANY_NAME")
        if (companyname != null) {
            company_name = "Company: $companyname"

        } else {
            company_name = "Company: N/A"

        }

        val user_name = "Username: " + Source.userName
        val device_id = "DeviceID: " + PhActivity.DEVICE_ID
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
                "Offset: " + "0"
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
                "Slope: " + "0"
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
                "Temperature: " + "0"
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

            } else {
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

            document.add(
                Paragraph(
                    """$reportDate  |  $reportTime
                                 $offset  |  $battery
                                 $slope  |  $tempe"""
                )
            )


            document.add(Paragraph(""))
            document.add(Paragraph("Ph Log Graph"))


            val bitmap =
                Bitmap.createBitmap(graphView.width, graphView.height, Bitmap.Config.ARGB_8888)
            val graphCanvas = Canvas(bitmap)
            graphView.draw(graphCanvas)

            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()

            val imageData = ImageDataFactory.create(byteArray)

            val image = Image(imageData).setHeight(80f).setWidth(80f)
            document.add(image)


            val db = databaseHelper.writableDatabase
            var calibCSV: Cursor? = null



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
            table1.addCell("Product")


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
        } catch (e: Exception) {
//            Toast.makeText(
//                requireContext(), "Error : " + e.message, Toast.LENGTH_SHORT
//            ).show()
        }
        document.close()

    }


    private fun showPdfFiles() {
        val pathPDF =
            ContextWrapper(requireContext()).externalMediaDirs[0].toString() + File.separator + "/LabApp/Currentlog/"
        val rootPDF = File(pathPDF)
        fileNotWrite(rootPDF)
        val filesAndFoldersPDF = rootPDF.listFiles() ?: return

        // Sort the files based on last modified timestamp in descending order
        val sortedFiles = filesAndFoldersPDF.sortedByDescending { it.lastModified() }

        // Find the first PDF file
        val pdfFile =
            sortedFiles.firstOrNull { it.name.endsWith(".pdf") || it.name.endsWith(".csv") }

        // Set up RecyclerView with adapter
        plAdapter = PDF_CSV_Adapter(requireContext(), sortedFiles.toTypedArray(), "PhLog")
        binding.recyclerViewCSVLog.adapter = plAdapter
        plAdapter.notifyDataSetChanged()
        binding.recyclerViewCSVLog.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun showPdfFiles1() {
        val pathPDF =
            ContextWrapper(requireContext()).externalMediaDirs[0].toString() + File.separator + "/LabApp/Currentlog/"
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

        plAdapter = PDF_CSV_Adapter(
            requireContext().applicationContext, reverseFileArray(filesAndFoldersPDF), "PhLog"
        )
        binding.recyclerViewCSVLog.adapter = plAdapter
        plAdapter.notifyDataSetChanged()
        binding.recyclerViewCSVLog.layoutManager =
            LinearLayoutManager(requireContext().applicationContext)
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


    public fun webSocketConnection() {


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

                        if (tempToggleSharedPref != null) {
                            if (tempToggleSharedPref == "true") {

                                temp = if (temp1.toInt() <= -127) {

                                    "NA"
                                } else {
                                    temp1
                                }

                                SharedPref.saveData(
                                    requireContext(), "tempValue" + PhActivity.DEVICE_ID, temp
                                )
                            }
                        } else {
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
            binding.printGraph.isEnabled = true
            logBtn.isEnabled = true
        } else if (AutoLog == 1) {
            exportBtn.isEnabled = false
            printBtn.isEnabled = false
            binding.printGraph.isEnabled = false
            logBtn.isEnabled = false
            switchHold.isChecked = true
            switchInterval.isChecked = false
            switchBtnClick.isChecked = false
        } else if (AutoLog == 2) {
            exportBtn.isEnabled = false
            printBtn.isEnabled = false
            binding.printGraph.isEnabled = false
            isAlertShow = false
            logBtn.isEnabled = false
            switchHold.isChecked = false
            switchInterval.isChecked = true
            switchBtnClick.isChecked = false
        } else if (AutoLog == 3) {
            exportBtn.isEnabled = false
            printBtn.isEnabled = false
            binding.printGraph.isEnabled = false
            switchHold.isChecked = false
            switchInterval.isChecked = false
            logBtn.isEnabled = false
            switchBtnClick.isChecked = true
        } else {
            exportBtn.isEnabled = true
            logBtn.isEnabled = true
            printBtn.isEnabled = true
            binding.printGraph.isEnabled = true
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

//        Toast.makeText(requireContext(), "Printing...", Toast.LENGTH_LONG).show()

        var company_name = ""

        val companyname = SharedPref.getSavedData(requireContext(), "COMPANY_NAME")
        if (companyname != null) {
            company_name = "Company: $companyname"

        } else {
            company_name = "Company: N/A"

        }

        val user_name = "Username: " + Source.userName
        val device_id = "DeviceID: " + PhActivity.DEVICE_ID
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
                "Offset: " + "0"
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
                "Slope: " + "0"
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
                "Temperature: " + "0"
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

            } else {
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

            document.add(
                Paragraph(
                    """$reportDate  |  $reportTime
                                 $offset  |  $battery
                                 $slope  |  $tempe"""
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
//            calibCSV = db.rawQuery("SELECT * FROM CalibOfflineData", null);
                if (Source.calibMode == 0) {
                    calibCSV = db.rawQuery("SELECT * FROM CalibOfflineDataFive", null)
                }
                if (Source.calibMode == 1) {
//                    calibCSV = db.rawQuery("SELECT * FROM CalibOfflineDataThree", null)
                    calibCSV = db.rawQuery("SELECT * FROM CalibOfflineDataFive", null)
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
            table1.addCell("Product")
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
        } catch (e: Exception) {
//            Toast.makeText(
//                requireContext(), "Error : " + e.message, Toast.LENGTH_SHORT
//            ).show()
        }
        document.close()
//        Toast.makeText(context, "Pdf generated", Toast.LENGTH_SHORT).show()
    }


    private fun exportLogCsv() {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
            val currentDateandTime = sdf.format(Date())
            val tempPath =
                File(requireContext().externalMediaDirs[0], "/LabApp/Currentlog")
            tempPath.mkdirs()

            val filePath =
                File(tempPath, "CL_$currentDateandTime-${tempPath.listFiles()?.size ?: 0}.csv")
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
                writer.writeNext(arrayOf("Username: ${Source.userName}"))
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
            writer.writeNext(arrayOf("Log Table", "", "", "", "", "", ""))
            writer.writeNext(arrayOf("", "", "", "", "", "", "", "", "", "", ""))
            writer.writeNext(
                arrayOf(
                    "Date",
                    "Time",
                    "pH",
                    "Temperature",
                    "Batch No",
                    "AR No",
                    "Product"
                )
            )

            val cursor = db.rawQuery("SELECT * FROM PrintLogUserdetails", null)
            while (cursor.moveToNext()) {
                val date = cursor.getString(cursor.getColumnIndex("date"))
                val time = cursor.getString(cursor.getColumnIndex("time"))
                val pH = cursor.getString(cursor.getColumnIndex("ph"))
                val temp = cursor.getString(cursor.getColumnIndex("temperature"))
                val batchNum = cursor.getString(cursor.getColumnIndex("batchnum"))
                val arNum = cursor.getString(cursor.getColumnIndex("arnum"))
                val product = cursor.getString(cursor.getColumnIndex("compound"))

                writer.writeNext(arrayOf(date, time, pH, temp, batchNum, arNum, product))
            }

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
//            databaseHelper.insert_action_data(
//                time,
//                date,
//                "Log pressed : " + Source.logUserName,
//                ph,
//                temp,
//                mv,
//                compound_name,
//                PhActivity.DEVICE_ID
//            )
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

    fun reverseFileArray(fileArray: Array<File>): Array<File>? {
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

    private fun observeProductList() {
        lifecycleScope.launch(Dispatchers.Main) {
            productViewModel.productListLiveData.observe(this@PhLogFragment) { productList ->
                val spinnerAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    productList.map { it.productName }
                )
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.productSpinner.adapter = spinnerAdapter


                binding.productSpinner.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            if (position >= 0 && position < productList.size) {
                                val selectedProduct = productList[position]
                                binding.compoundName.setText(selectedProduct.productName)
                                compound_name = selectedProduct.productName
                            }
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {
                            binding.compoundName.setText("Select product")
                            compound_name = "Select product"

                        }
                    }


            }


        }


    }


    lateinit var userDao: UserDao
    lateinit var userActionDao: UserActionDao
    lateinit var allLogsDataDao: AllLogsDataDao
    var tempToggleSharedPref: String? = null
    lateinit var productsListDao: ProductsListDao
    private lateinit var productViewModel: ProductViewModel

    override fun onResume() {
        super.onResume()

        webSocketConnection()

        tempToggleSharedPref =
            SharedPref.getSavedData(requireContext(), "setTempToggle" + PhActivity.DEVICE_ID)

        setPreviousData()

        userDao = Room.databaseBuilder(
            requireContext().applicationContext, AppDatabase::class.java, "aican-database"
        ).build().userDao()

        productsListDao = Room.databaseBuilder(
            requireContext().applicationContext,
            AppDatabase::class.java,
            "aican-database"
        ).build().productsDao()

        allLogsDataDao = Room.databaseBuilder(
            requireContext().applicationContext, AppDatabase::class.java, "aican-database"
        ).build().allLogsDao()

        userActionDao = Room.databaseBuilder(
            requireContext().applicationContext, AppDatabase::class.java, "aican-database"
        ).build().userActionDao()

        val viewModelFactory = ProductViewModelFactory(productsListDao)
        productViewModel =
            ViewModelProvider(this, viewModelFactory)[ProductViewModel::class.java]

        observeProductList()


//        if (Source.cfr_mode) {
//            val userAuthDialog = UserAuthDialog(requireContext(), userDao)
//            userAuthDialog.showLoginDialog { isValidCredentials ->
//                if (isValidCredentials) {
//                    addUserAction(
//                        "username: " + Source.userName + ", Role: " + Source.userRole + ", entered ph log fragment",
//                        "",
//                        "",
//                        "",
//                        ""
//                    )
//                } else {
//                    requireActivity().runOnUiThread {
//                        Toast.makeText(requireContext(), "Invalid credentials", Toast.LENGTH_SHORT)
//                            .show()
//                    }
//                }
//            }
//        }

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