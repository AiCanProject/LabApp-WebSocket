package com.aican.aicanapp.ph.phFragment

import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.aican.aicanapp.R
import com.aican.aicanapp.databinding.FragmentPhAlarmFragmentBinding
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
import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale


class PhAlarmFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    lateinit var binding: FragmentPhAlarmFragmentBinding
    private lateinit var radioGroup: RadioGroup
    private lateinit var deviceRef: DatabaseReference
    private lateinit var alarm: Button
    private lateinit var stopAlarm: Button
    private lateinit var ringtone: Ringtone
    private lateinit var phForm: String
    private lateinit var phValue: EditText
    private lateinit var maxPhValue: EditText
    private lateinit var radioButton: RadioButton
    private lateinit var serviceIntent: Intent
    private var ph: Float = 0f
    private lateinit var jsonData: JSONObject

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPhAlarmFragmentBinding.inflate(inflater, container, false);
        return binding.root;
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        radioGroup = view.findViewById<RadioGroup>(R.id.groupradio)
        alarm = view.findViewById<Button>(R.id.startAlarm)
        stopAlarm = view.findViewById<Button>(R.id.stopAlarm)
        phValue = view.findViewById<EditText>(R.id.etPhValue)
        maxPhValue = view.findViewById<EditText>(R.id.maxPhValue)


        stopAlarm.isEnabled = false

        if (AlarmConstants.ringtone == null) AlarmConstants.ringtone = RingtoneManager.getRingtone(
            requireContext().applicationContext,
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        )

        if (AlarmConstants.ringtone.isPlaying) {
            stopAlarm.isEnabled = true
            alarm.isEnabled = false
        }

        if (AlarmConstants.minPh != null && AlarmConstants.maxPh != null) {
            phValue.setText("" + AlarmConstants.minPh)
            maxPhValue.setText("" + AlarmConstants.maxPh)
        }
        alarm.setOnClickListener {
            val minPH = phValue.text.toString()
            val maxPH = maxPhValue.text.toString()
            if (phValue.text.toString().isEmpty() || maxPhValue.text.toString().isEmpty()) {
                Toast.makeText(context, "Please enter all values", Toast.LENGTH_SHORT).show()
            } else {
                Log.d("Alarm", "Start alarm clicked")
                addUserAction(
                    "username: " + Source.userName + ", Role: " + Source.userRole +
                            ", started alarm", "", "", "", ""
                )
                stopAlarm.isEnabled = true
                AlarmConstants.maxPh = minPH.toFloat()
                AlarmConstants.minPh = maxPH.toFloat()
                stopAlarm.isEnabled = true
                alarm.isEnabled = false
                AlarmConstants.isServiceAvailable = true
                alarmBackgroundService()
                    .execute(minPH, maxPH)
            }
        }

        stopAlarm.setOnClickListener {
            if (AlarmConstants.ringtone.isPlaying || AlarmConstants.ringtone != null) {
                AlarmConstants.ringtone.stop()
            }
            addUserAction(
                "username: " + Source.userName + ", Role: " + Source.userRole +
                        ", stopped alarm", "", "", "", ""
            )
            AlarmConstants.isServiceAvailable = false
            stopAlarm.isEnabled = false
            alarm.isEnabled = true
        }

        webSocketInit()


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

    override fun onResume() {
        super.onResume()

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


        if (Source.cfr_mode) {
            val userAuthDialog = UserAuthDialog(requireContext(), userDao)
            userAuthDialog.showLoginDialog { isValidCredentials ->
                if (isValidCredentials) {
                    addUserAction(
                        "username: " + Source.userName + ", Role: " + Source.userRole +
                                ", entered ph alarm fragment", "", "", "", ""
                    )
                } else {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Invalid credentials", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }
    }

    private fun webSocketInit() {
        WebSocketManager.setMessageListener { message ->
            requireActivity().runOnUiThread {
                updateMessage(message)

                try {
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
                        val ph = jsonData.getString("PH_VAL").toFloat()
                        val phForm =
                            String.format(Locale.UK, "%.2f", ph)
                        AlarmConstants.PH = ph
                    }
                    if (jsonData.has("TEMP_VAL") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                        val temp = jsonData.getString("TEMP_VAL")
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
        WebSocketManager.setErrorListener { error ->
            requireActivity().runOnUiThread {
                updateError(error.toString())
            }
        }
        WebSocketManager.setCloseListener { i, s, b ->
            sharedViewModel.closeConnectionLiveData.value = s + ""

        }
        WebSocketManager.setOpenListener {
            sharedViewModel.openConnectionLiveData.value = ""
        }
    }

    private val sharedViewModel: SharedViewModel by activityViewModels()

    private fun updateMessage(message: String) {
        sharedViewModel.messageLiveData.value = message
    }


    private fun updateError(error: String) {
        sharedViewModel.errorLiveData.value = error
    }

    class alarmBackgroundService :
        AsyncTask<String?, String?, String?>() {
        override fun doInBackground(vararg p0: String?): String? {
            while (AlarmConstants.isServiceAvailable) {
//                if(AlarmConstants.ringtone.isPlaying()) break;
                val minPH = p0[0]?.toFloat()
                val maxPH = p0[1]?.toFloat()
                Log.d("AlarmFragment", "AlarmFragment: doInBackground in if " + AlarmConstants.PH)
                if (AlarmConstants.PH < minPH!! || AlarmConstants.PH > maxPH!!) {
                    if (!AlarmConstants.ringtone.isPlaying && AlarmConstants.ringtone != null) AlarmConstants.ringtone.play()
                } else if (AlarmConstants.ringtone.isPlaying) {
                    AlarmConstants.ringtone.stop()
                }
            }
            Log.d("AlarmFragment", "AlarmFragment: Service stopped " + AlarmConstants.PH)
            return null
        }
    }


}