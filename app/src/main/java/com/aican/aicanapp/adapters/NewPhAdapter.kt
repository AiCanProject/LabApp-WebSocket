package com.aican.aicanapp.adapters

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aican.aicanapp.Dashboard
import com.aican.aicanapp.R
import com.aican.aicanapp.dataClasses.PhDevice
import com.aican.aicanapp.databinding.PhItemBinding
import com.aican.aicanapp.interfaces.DashboardListsOptionsClickListener
import com.aican.aicanapp.ph.PhActivity

class NewPhAdapter(
    val context: Context, val arrayList: ArrayList<PhDevice>,
    val optionsClickListener: DashboardListsOptionsClickListener
) :
    RecyclerView.Adapter<NewPhAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var binding = PhItemBinding.bind(itemView)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(R.layout.ph_item, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = arrayList[position]

        holder.binding.customDeviceName.text = "pH Meter " + item.getId()

        holder.itemView.setOnClickListener {
            val intent = Intent(context, PhActivity::class.java)
            intent.putExtra(Dashboard.KEY_DEVICE_ID, item.id)
            context.startActivity(intent)
        }


    }

    fun refreshPh( phVal: Float, deviceID: String) {
        Log.e("NotifyingS", "DNB : $deviceID")
        for (i in arrayList.indices) {
            Log.e("NotifyingS", "DB : $deviceID")
            val phDevice: PhDevice = arrayList.get(i)
            if (phDevice.id == deviceID) {
                phDevice.ph = phVal
                val finalI: Int = i
                Log.e("NotifyingS", "D : $deviceID")
                Handler(Looper.getMainLooper()).post {
                    notifyItemChanged(
                        finalI
                    )
                }
                break // Exit the loop after finding the matching device
            }
        }

    }

    fun refreshTemp(tempVal: Int, deviceID: String) {
        for (i in arrayList.indices) {
            Log.e("NotifyingS", "DB : $deviceID")
            val phDevice: PhDevice = arrayList.get(i)
            if (phDevice.id == deviceID) {
                phDevice.temp = tempVal
                val finalI: Int = i
                Log.e("NotifyingS", "D : $deviceID")
                Handler(Looper.getMainLooper()).post {
                    notifyItemChanged(
                        finalI
                    )
                }
                break // Exit the loop after finding the matching device
            }
        }
    }

    fun refreshMv(mvVal: Float, deviceID: String) {
        for (i in arrayList.indices) {
            Log.e("NotifyingS", "DB : $deviceID")
            val phDevice: PhDevice = arrayList.get(i)
            if (phDevice.id == deviceID) {
                phDevice.ec = mvVal
                val finalI: Int = i
                Log.e("NotifyingS", "D : $deviceID")
                Handler(Looper.getMainLooper()).post {
                    notifyItemChanged(
                        finalI
                    )
                }
                break // Exit the loop after finding the matching device
            }
        }
    }

}