package com.aican.aicanapp.adapters

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aican.aicanapp.R
import com.aican.aicanapp.databinding.CustomProductsBinding
import com.aican.aicanapp.roomDatabase.daoObjects.ARNumDao
import com.aican.aicanapp.roomDatabase.entities.ARNumEntity
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ARNumListAdapter(
    val context: Context,
    val arrayList: List<ARNumEntity>,
    val arNumListDao: ARNumDao,
) :
    RecyclerView.Adapter<ARNumListAdapter.ViewHolder>() {
    open class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var binding = CustomProductsBinding.bind(itemView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ARNumListAdapter.ViewHolder(
            LayoutInflater.from(context).inflate(R.layout.custom_products, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = arrayList[position]
        holder.binding.productName.text = item.arNum

        holder.binding.editIcon.setOnClickListener {
            updateDialog(item)
        }

        holder.binding.deleteIcon.setOnClickListener {
            showDeleteAlertDialog(item)
        }

    }

    fun showDeleteAlertDialog(product: ARNumEntity) {
        val alertDialogBuilder = AlertDialog.Builder(context)
        alertDialogBuilder.setTitle("Delete Product")
        alertDialogBuilder.setMessage("Are you sure you want to delete this product?")

        alertDialogBuilder.setPositiveButton("Delete") { dialog, _ ->
            CoroutineScope(Dispatchers.IO).launch {

                arNumListDao.deleteARNum(product)
                (context as Activity).runOnUiThread {
                    dialog.dismiss()

                }
            }
        }

        alertDialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    private fun updateDialog(item: ARNumEntity) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_edit_name)

        val btnSave = dialog.findViewById<Button>(R.id.btnSave)
        val titleTextDialog = dialog.findViewById<TextView>(R.id.titleTextDialog)
        val etName = dialog.findViewById<TextInputEditText>(R.id.etName)
        titleTextDialog.text = "Rename"
        etName.setText(item.arNum)

        btnSave.setOnClickListener {
            val newName = etName.text.toString()

            if (newName.isEmpty()) {
                etName.error = "Enter any product"
            } else {

                CoroutineScope(Dispatchers.IO).launch {
                    arNumListDao.updateARNum(ARNumEntity(item.id, newName))
                    (context as Activity).runOnUiThread {
                        dialog.dismiss()

                    }
                }
            }


        }

        dialog.show()

    }
}