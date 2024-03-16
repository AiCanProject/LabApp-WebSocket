package com.aican.aicanapp.ph.phFragment

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.aican.aicanapp.data.DatabaseHelper
import com.aican.aicanapp.databinding.FragmentPhBinding
import com.aican.aicanapp.dialogs.UserAuthDialog
import com.aican.aicanapp.ph.PhActivity
import com.aican.aicanapp.roomDatabase.daoObjects.UserActionDao
import com.aican.aicanapp.roomDatabase.daoObjects.UserDao
import com.aican.aicanapp.roomDatabase.database.AppDatabase
import com.aican.aicanapp.roomDatabase.entities.UserActionEntity
import com.aican.aicanapp.utils.AlarmConstants
import com.aican.aicanapp.utils.SharedPref
import com.aican.aicanapp.utils.Source
import com.aican.aicanapp.viewModels.SharedViewModel
import com.aican.aicanapp.websocket.WebSocketManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern


class PhFragment : Fragment() {


    lateinit var binding: FragmentPhBinding
    lateinit var jsonData: JSONObject
    private val sharedViewModel: SharedViewModel by activityViewModels()
    lateinit var databaseHelper: DatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPhBinding.inflate(inflater, container, false);
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        jsonData = JSONObject()
        databaseHelper = DatabaseHelper(requireContext())


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

    private lateinit var userDao: UserDao
    lateinit var userActionDao: UserActionDao
    var tempToggleSharedPref: String? = null
    override fun onResume() {
        super.onResume()


         tempToggleSharedPref =
            SharedPref.getSavedData(requireContext(), "setTempToggle" + PhActivity.DEVICE_ID)

        getPreviousData()

        webSocketInit()
        turnAtcSwitch()

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
//                                ", entered ph main fragment", "", "", "", ""
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



    private fun turnAtcSwitch() {
        binding.switchAtc.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { v: CompoundButton?, isChecked: Boolean ->
            if (binding.switchAtc.isChecked()) {

                addUserAction(
                    "username: " + Source.userName + ", Role: " + Source.userRole +
                            ", ATC toggle on", "", "", "", ""
                )
                try {
                    jsonData = JSONObject()
                    jsonData.put("ATC", "1")
                    jsonData.put("ATC_AT", binding.atcValue.getText().toString())
                    jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                    WebSocketManager.sendMessage(jsonData.toString())
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                val togglePref = requireContext().getSharedPreferences(
                    "togglePref",
                    Context.MODE_PRIVATE
                )
                val editT = togglePref.edit()
                editT.putInt("toggleValue", 1)
                editT.commit()
                binding.setATC.setVisibility(View.VISIBLE)
//                WebSocketManager.sendMessage(jsonData.toString())

            } else {
                val date =
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(Date())
                val time =
                    SimpleDateFormat("HH:mm", Locale.getDefault())
                        .format(Date())
                try {
                    jsonData = JSONObject()
                    jsonData.put("ATC", "0")
                    jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                    WebSocketManager.sendMessage(jsonData.toString())

                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                addUserAction(
                    "username: " + Source.userName + ", Role: " + Source.userRole +
                            ", ATC toggle off", "", "", "", ""
                )
                val togglePref = requireContext().getSharedPreferences(
                    "togglePref",
                    Context.MODE_PRIVATE
                )
                val editT = togglePref.edit()
                editT.putInt("toggleValue", 0)
                editT.commit()
                binding.setATC.visibility = View.INVISIBLE
            }
        })
    }

    private fun getPreviousData() {

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

        val batteryVal = SharedPref.getSavedData(requireContext(), "battery" + PhActivity.DEVICE_ID)
        if (batteryVal != null) {
            if (batteryVal != "") {
                binding.batteryPercent.text = "$batteryVal %"
            }
        }

        val slopeVal = SharedPref.getSavedData(requireContext(), "SLOPE_" + PhActivity.DEVICE_ID)

        if (slopeVal != null) {
            binding.slopeVal.text = "$slopeVal %"

        }

        val offsetVal =
            SharedPref.getSavedData(requireContext(), "OFFSET_" + PhActivity.DEVICE_ID)
        if (offsetVal != null) {
            binding.offsetVal.text = offsetVal
        }

        val ecValue = SharedPref.getSavedData(requireContext(), "ecValue" + PhActivity.DEVICE_ID)
        if (ecValue != null) {
            binding.tvEcCurr.text = ecValue
        }

        val tempVal = SharedPref.getSavedData(requireContext(), "tempValue" + PhActivity.DEVICE_ID)
        if (tempVal != null) {
            binding.tvTempCurr.text = "$tempVal °C"
        }

        val toggleVals =
            SharedPref.getSavedData(requireContext(), "toggleValue" + PhActivity.DEVICE_ID)
        if (toggleVals != null) {
            var toggleVal = 0
            if (validateNumber(toggleVals)) {
                toggleVal = toggleVals.toInt()
                if (toggleVal == 1) {
                    binding.switchAtc.isChecked = true
                } else if (toggleVal == 0) {
                    binding.switchAtc.isChecked = false
                }
            }
        }

    }


    public fun webSocketInit() {


        if (Source.SOCKET_CONNECTED) {
            sharedViewModel.openConnectionLiveData.value = ""

        } else {
            sharedViewModel.closeConnectionLiveData.value = ""

        }

        WebSocketManager.setCloseListener { _, s, b ->
            sharedViewModel.closeConnectionLiveData.value = s + ""

        }
        WebSocketManager.setOpenListener {
            sharedViewModel.openConnectionLiveData.value = ""
        }

        WebSocketManager.setMessageListener { message ->
            requireActivity().runOnUiThread {
                Log.e("WebSocketMessageAican", message)

                updateMessage(message)
                try {
                    jsonData = JSONObject(message)
                    Log.d("JSONReceived:PHFFragment", "onMessage: $message")
                    if (jsonData.has("PH_VAL") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                        val `val`: String = jsonData.getString("PH_VAL")
                        binding.tvPhCurr.setText(`val`)
                        var floatVal = 0.0f
                        if (jsonData.getString("PH_VAL") != "nan" && PhFragment.validateNumber(
                                jsonData.getString("PH_VAL")
                            )
                        ) {
                            floatVal = `val`.toFloat()
                        }
                        SharedPref.saveData(
                            requireContext(),
                            "phValue" + PhActivity.DEVICE_ID,
                            floatVal.toString()
                        )

                        binding.phView.moveTo(floatVal)
                        AlarmConstants.PH = floatVal
                    }
                    if (jsonData.has("ATC") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                        if (jsonData.getString("ATC") == "1") {
                            binding.switchAtc.setChecked(true)

                            SharedPref.saveData(
                                requireContext(),
                                "toggleValue" + PhActivity.DEVICE_ID,
                                "1"
                            )

                        } else {
                            binding.switchAtc.setChecked(false)
                            SharedPref.saveData(
                                requireContext(),
                                "toggleValue" + PhActivity.DEVICE_ID,
                                "0"
                            )

                        }
                    }
                    if (jsonData.has("ATC_AT") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                        val `val`: String = jsonData.getString("ATC_AT")
                        SharedPref.saveData(
                            requireContext(),
                            "atcValue" + PhActivity.DEVICE_ID,
                            `val`
                        )
                        binding.atcValue.setText(`val`)
                    }
                    if (jsonData.has("TEMP_VAL") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                        var tempval = 0.0f
                        var temp = "0.0"
                        if (jsonData.getString("TEMP_VAL") != "nan" && PhFragment.validateNumber(
                                jsonData.getString("TEMP_VAL")
                            )
                        ) {
                            tempval = jsonData.getString("TEMP_VAL").toFloat()
                            temp = Math.round(tempval).toString()
                            Log.e("NullCheck", "" + tempToggleSharedPref)

                            if (tempToggleSharedPref != null) {
                                if (tempToggleSharedPref == "true") {
                                    binding.tvTempCurr.setText("$temp°C")
                                    SharedPref.saveData(
                                        requireContext(),
                                        "tempValue" + PhActivity.DEVICE_ID,
                                        temp
                                    )

                                    if (temp.toInt() <= -127) {
                                        binding.tvTempCurr.setText("NA")

                                        binding.switchAtc.setEnabled(false)
                                    } else {
                                        binding.switchAtc.setEnabled(true)
                                    }
                                }
                            }else{
                                binding.tvTempCurr.setText("$temp°C")
                                SharedPref.saveData(
                                    requireContext(),
                                    "tempValue" + PhActivity.DEVICE_ID,
                                    temp
                                )

                                if (temp.toInt() <= -127) {
                                    binding.tvTempCurr.setText("NA")

                                    binding.switchAtc.setEnabled(false)
                                } else {
                                    binding.switchAtc.setEnabled(true)
                                }
                            }
                        }

                    }
                    if (jsonData.has("BATTERY") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                        val battery: String = jsonData.getString("BATTERY")
                        binding.batteryPercent.setText("$battery %")
                        SharedPref.saveData(
                            requireContext(),
                            "battery" + PhActivity.DEVICE_ID,
                            battery
                        )

                    }
                    if (jsonData.has("SLOPE") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                        val slope: String = jsonData.getString("SLOPE")
                        binding.slopeVal.setText("$slope %")

                        SharedPref.saveData(
                            requireContext(), "SLOPE_" + PhActivity.DEVICE_ID, slope
                        )


                    }
                    if (jsonData.has("OFFSET") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                        val offSet: String = jsonData.getString("OFFSET")
                        //                        String offsetForm = String.format(Locale.UK, "%.2f", offSet);
                        binding.offsetVal.setText(offSet)
                        SharedPref.saveData(
                            requireContext(),
                            "offsetVal" + PhActivity.DEVICE_ID,
                            offSet
                        )

                    }
                    if (jsonData.has("EC_VAL") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                        val ec: String = jsonData.getString("EC_VAL")
                        //                        String ecForm = String.format(Locale.UK, "%.1f", ec);
                        binding.tvEcCurr.setText(ec)

                        SharedPref.saveData(requireContext(), "ecValue" + PhActivity.DEVICE_ID, ec)

                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }


            }

        }

        WebSocketManager.setErrorListener {
            val activity: Activity? = requireActivity()
            activity?.runOnUiThread {
                updateError(it.message.toString())
                Log.e("WebSocketErrorAican", it.message + "")
            }


        }

    }

    private fun updateMessage(message: String) {
        sharedViewModel.messageLiveData.value = message
    }

    private fun updateError(error: String) {
        sharedViewModel.errorLiveData.value = error
    }

    companion object {
        fun validateNumber(text: String?): Boolean {
            val pattern = Pattern.compile("^-?\\d+(\\.\\d+)?$")
            val matcher = pattern.matcher(text)
            return matcher.matches()
        }
    }

}