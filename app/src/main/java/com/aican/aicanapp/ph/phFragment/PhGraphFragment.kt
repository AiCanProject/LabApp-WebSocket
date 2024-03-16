package com.aican.aicanapp.ph.phFragment

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.aican.aicanapp.R
import com.aican.aicanapp.data.DatabaseHelper
import com.aican.aicanapp.databinding.FragmentPhGraphBinding
import com.aican.aicanapp.dialogs.UserAuthDialog
import com.aican.aicanapp.ph.PhActivity
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
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

class PhGraphFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    lateinit var binding: FragmentPhGraphBinding
    private lateinit var jsonData: JSONObject
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var lineChart: LineChart
    private lateinit var deviceRef: DatabaseReference
    private lateinit var ph: String
    private lateinit var refresh: Button
    private lateinit var btnGraphCancel: Button
    private var lineDataSet: LineDataSet = LineDataSet(null, null)
    private var iLineDataSets: ArrayList<ILineDataSet> = ArrayList()
    private lateinit var lineData: LineData
    private lateinit var tvGraphTemp: TextView
    private lateinit var tvGraphPH: TextView
    private lateinit var graphInterval: Spinner
    private lateinit var countDownTimer: CountDownTimer
    var intervalSelected: String = "5 sec"
    private var loadGraph: Boolean = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPhGraphBinding.inflate(inflater, container, false);
        return binding.root;
    }

    private val sharedViewModel: SharedViewModel by activityViewModels()

    private fun updateMessage(message: String) {
        sharedViewModel.messageLiveData.value = message
    }

    private fun updateError(error: String) {
        sharedViewModel.errorLiveData.value = error
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lineChart = view.findViewById<LineChart>(R.id.graph)
        refresh = view.findViewById<Button>(R.id.btnGraphRefresh)
        btnGraphCancel = view.findViewById<Button>(R.id.btnGraphCancel)
        tvGraphTemp = view.findViewById<TextView>(R.id.tvGraphTemp)
        tvGraphPH = view.findViewById<TextView>(R.id.tvGraphPH)
        graphInterval = view.findViewById<Spinner>(R.id.graphInterval)
        btnGraphCancel.isEnabled = false
        spinnerAction()
        jsonData = JSONObject()

        tvGraphPH.text = "0"

        Constants.OFFLINE_MODE = true
        Constants.OFFLINE_DATA = true

        lineDataSet.label = "Data"
        spinnerSelected()
        refresh.setOnClickListener {
            if (Constants.OFFLINE_MODE) {
                if (intervalSelected == "5 sec") {

                    graphShowOffline(5, 5)
                }
                if (intervalSelected == "10 sec") {
                    graphShowOffline(10, 10)
                }
                if (intervalSelected == "30 sec") {
                    graphShowOffline(30, 30)
                }
                if (intervalSelected == "1 min") {
                    graphShowOffline(30, 60)
                }
                if (intervalSelected == "3 min") {
                    graphShowOffline(40, 180)
                }
                if (intervalSelected == "5 min") {
                    graphShowOffline(50, 300)
                }
                if (intervalSelected == "10 min") {
                    graphShowOffline(60, 600)
                }
            }
        }

        btnGraphCancel.setOnClickListener {
            countDownTimer.cancel()
            btnGraphCancel.isEnabled = false
            refresh.isEnabled = true
        }

//        webSocketInit()
        setPreviousData()
    }

    fun addUserAction(action: String, ph: String, temp: String, mv: String, compound: String) {
        lifecycleScope.launch(Dispatchers.IO) {

            userActionDao.insertUserAction(
                UserActionEntity(
                    0, Source.getCurrentTime(), Source.getPresentDate(),
                    action, ph, temp, mv, compound, PhActivity.DEVICE_ID.toString()
                )
            )
        }
    }

    lateinit var userDao: UserDao
    lateinit var userActionDao: UserActionDao
    var tempToggleSharedPref: String? = null

    override fun onResume() {
        super.onResume()

        tempToggleSharedPref =
            SharedPref.getSavedData(requireContext(), "setTempToggle" + PhActivity.DEVICE_ID)

        setPreviousData()
        webSocketInit()

        userDao = Room.databaseBuilder(
            requireContext().applicationContext,
            AppDatabase::class.java,
            "aican-database"
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
//                                ", entered ph graph fragment", "", "", "", ""
//                    )
//                } else {
//                    requireActivity().runOnUiThread {
//                        Toast.makeText(requireContext(), "Invalid credentials", Toast.LENGTH_SHORT)
//                            .show()
//                    }
//                }
//            }
//        }
    }


    private fun setPreviousData() {
        val phVal = SharedPref.getSavedData(requireContext(), "phValue" + PhActivity.DEVICE_ID)

        if (phVal != null) {
            var floatVal = 0.0f
            if (PhFragment.validateNumber(phVal)) {
                floatVal = phVal.toFloat()
                binding.tvGraphPH.text = floatVal.toString()
                ec_val_offline = floatVal
                AlarmConstants.PH = floatVal
            }
        }

        val tempVal = SharedPref.getSavedData(requireContext(), "tempValue" + PhActivity.DEVICE_ID)
        if (tempVal != null) {
//            temp = tempVal
            binding.tvGraphTemp.text = "$tempVal °C"
        }

    }

    public fun webSocketInit() {

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
                    updateMessage(message.toString())
                    jsonData = JSONObject(message.toString())
                    Log.d("JSONReceived:PHGraphFragment", "onMessage: $message")
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
                        var ph = jsonData.getString("PH_VAL").toFloat()
                        val phForm = String.format(Locale.UK, "%.2f", ph)
                        ec_val_offline = ph
                        tvGraphPH.text = phForm
                        AlarmConstants.PH = ph

                        if (jsonData.getString("PH_VAL") != "nan" && PhFragment.validateNumber(
                                jsonData.getString("PH_VAL")
                            )
                        ) {
                            ph = jsonData.getString("PH_VAL").toFloat()
                        }
                        tvGraphPH.text = ph.toString()
                        SharedPref.saveData(
                            requireContext(), "phValue" + PhActivity.DEVICE_ID, ph.toString()
                        )
                        AlarmConstants.PH = ph
                    }
                    if (jsonData.has("TEMP_VAL") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                        var temp = jsonData.getString("TEMP_VAL")
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
                                tvGraphTemp.text = "$temp°C"

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
                            tvGraphTemp.text = "$temp°C"
                        }
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
    }


    fun spinnerSelected() {
        graphInterval.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                if (position == 0) {
                    intervalSelected = "5 sec"
                    addUserAction(
                        "username: " + Source.userName + ", Role: " + Source.userRole +
                                ", changed graph interval to " + intervalSelected, "", "", "", ""
                    )
                }
                if (position == 1) {
                    intervalSelected = "10 sec"
                    addUserAction(
                        "username: " + Source.userName + ", Role: " + Source.userRole +
                                ", changed graph interval to " + intervalSelected, "", "", "", ""
                    )
                }
                if (position == 2) {
                    intervalSelected = "30 sec"
                    addUserAction(
                        "username: " + Source.userName + ", Role: " + Source.userRole +
                                ", changed graph interval to " + intervalSelected, "", "", "", ""
                    )
                }
                if (position == 3) {
                    intervalSelected = "1 min"
                    addUserAction(
                        "username: " + Source.userName + ", Role: " + Source.userRole +
                                ", changed graph interval to " + intervalSelected, "", "", "", ""
                    )
                }
                if (position == 4) {
                    intervalSelected = "3 min"
                    addUserAction(
                        "username: " + Source.userName + ", Role: " + Source.userRole +
                                ", changed graph interval to " + intervalSelected, "", "", "", ""
                    )
                }
                if (position == 5) {
                    intervalSelected = "5 min"
                    addUserAction(
                        "username: " + Source.userName + ", Role: " + Source.userRole +
                                ", changed graph interval to " + intervalSelected, "", "", "", ""
                    )
                }
                if (position == 6) {
                    intervalSelected = "10 min"
                    addUserAction(
                        "username: " + Source.userName + ", Role: " + Source.userRole +
                                ", changed graph interval to " + intervalSelected, "", "", "", ""
                    )
                }

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                intervalSelected = "5 sec"
                addUserAction(
                    "username: " + Source.userName + ", Role: " + Source.userRole +
                            ", interval selected to " + intervalSelected, "", "", "", ""
                )
            }
        }
    }

    private fun spinnerAction() {
        // 30, 1 min , 3 min , 5 , 10

        // Spinner Drop down elements
        val categories: MutableList<String> = java.util.ArrayList()
        categories.add("5 sec")
        categories.add("10 sec")
        categories.add("30 sec")
        categories.add("1 min")
        categories.add("3 min")
        categories.add("5 min")
        categories.add("10 min")

        // Creating adapter for spinner
        val dataAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)

        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // attaching data adapter to spinner
        graphInterval.adapter = dataAdapter
    }

    var ec_val = 0f
    var ec_val_offline = -100f

    var start = false

    fun graphShowOffline(totalMin: Long, interval: Long) {
        addUserAction(
            "username: " + Source.userName + ", Role: " + Source.userRole +
                    ", started plotting graph, with interval" + intervalSelected, "", "", "", ""
        )
        lineChart.clear()
        lineChart.invalidate()
        val information = java.util.ArrayList<Entry>()
        var ecValue: Float
        val times = intArrayOf(0)
        btnGraphCancel.setOnClickListener {
            countDownTimer.cancel()
            btnGraphCancel.isEnabled = false
            refresh.isEnabled = true
        }
        //        String valURL = "https://labdevices-8c9a5-default-rtdb.asia-southeast1.firebasedatabase.app/PHMETER/" + PhActivity.DEVICE_ID + "/Data/.json";
        countDownTimer = object : CountDownTimer(totalMin * 60 * 1000, interval * 1000) {
            override fun onTick(millisUntilFinished: Long) {
                var millisUntilFinished = millisUntilFinished
                millisUntilFinished /= 1000
                val min = millisUntilFinished.toInt() / 60
                val sec = millisUntilFinished.toInt() % 60
                start = true
                refresh.isEnabled = false
                btnGraphCancel.isEnabled = true
                //                Toast.makeText(getContext(), "" + getConductivity, Toast.LENGTH_SHORT).show();
                if (ec_val_offline != -100f) {
                    information.add(
                        Entry(
                            times[0].toFloat(), ec_val_offline
                        )
                    )
                    times[0] = (times[0] + interval).toInt()
                    activity!!.runOnUiThread { showChart(information) }
                } else {
                    activity!!.runOnUiThread {
                        lineChart.clear()
                        lineChart.invalidate()
                        countDownTimer.cancel()
                    }
                }
            }

            override fun onFinish() {
                refresh.isEnabled = true
                btnGraphCancel.isEnabled = false
            }
        }
        countDownTimer.start()
    }

    fun showChart(dataVal: java.util.ArrayList<Entry>) {
        lineDataSet.values = dataVal
        iLineDataSets.add(lineDataSet)
        lineData = LineData(iLineDataSets)
        lineChart.clear()
        lineChart.data = lineData
        lineChart.invalidate()
    }
}