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
import com.aican.aicanapp.R
import com.aican.aicanapp.databinding.FragmentPhAlarmFragmentBinding
import com.aican.aicanapp.ph.PhActivity
import com.aican.aicanapp.utils.AlarmConstants
import com.aican.aicanapp.viewModels.SharedViewModel
import com.aican.aicanapp.websocket.WebSocketManager
import com.google.firebase.database.DatabaseReference
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
            AlarmConstants.isServiceAvailable = false
            stopAlarm.isEnabled = false
            alarm.isEnabled = true
        }

        webSocketInit()


    }

    private fun webSocketInit() {
        WebSocketManager.setMessageListener { message ->
            requireActivity().runOnUiThread {
                updateMessage(message)

                try {
                    jsonData = JSONObject(message)
                    Log.d("JSONReceived:PHFragment", "onMessage: " + message)
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