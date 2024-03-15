package com.aican.aicanapp

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.aican.aicanapp.adapters.ProductListAdapter
import com.aican.aicanapp.databinding.ActivityAdminSettingsBinding
import com.aican.aicanapp.ph.PhActivity
import com.aican.aicanapp.ph.phFragment.PhFragment
import com.aican.aicanapp.roomDatabase.daoObjects.ProductsListDao
import com.aican.aicanapp.roomDatabase.database.AppDatabase
import com.aican.aicanapp.roomDatabase.entities.ProductEntity
import com.aican.aicanapp.specificActivities.Users.AddNewUser
import com.aican.aicanapp.specificActivities.Users.AllUsers
import com.aican.aicanapp.utils.SharedKeys
import com.aican.aicanapp.utils.SharedPref
import com.aican.aicanapp.utils.Source
import com.aican.aicanapp.viewModels.ProductViewModel
import com.aican.aicanapp.viewModels.ProductViewModelFactory
import com.aican.aicanapp.websocket.WebSocketManager
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject

class AdminSettings : AppCompatActivity() {

    lateinit var binding: ActivityAdminSettingsBinding
    lateinit var productsListDao: ProductsListDao
    lateinit var productListAdapter: ProductListAdapter
    private lateinit var productViewModel: ProductViewModel
    lateinit var jsonData: JSONObject

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.adminSettings.setOnClickListener {
            val intnt = Intent(this@AdminSettings, AdminActivities::class.java)
            startActivity(intnt)

        }

        if (Source.cfr_mode){
            binding.addUsersBtn.visibility = View.VISIBLE
            binding.allUsersBtn.visibility = View.VISIBLE
        }else{
            binding.addUsersBtn.visibility = View.GONE
            binding.allUsersBtn.visibility = View.GONE
        }

        binding.addUsersBtn.setOnClickListener {
            val intnt = Intent(this@AdminSettings, AddNewUser::class.java)
            startActivity(intnt)
        }

        binding.allUsersBtn.setOnClickListener {
            val intnt = Intent(this@AdminSettings, AllUsers::class.java)
            startActivity(intnt)
        }




        fetchExportConditions()

        fetchDesignations()

        jsonData = JSONObject()

        productsListDao = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "aican-database"
        ).build().productsDao()

        val viewModelFactory = ProductViewModelFactory(productsListDao)
        productViewModel =
            ViewModelProvider(this, viewModelFactory)[ProductViewModel::class.java]

        observeProductList()

        binding.addProductBtn.setOnClickListener {
            val product = binding.editTextProduct.text.toString()
            if (product != "") {
                val productEntity = ProductEntity(productName = product)
                lifecycleScope.launch(Dispatchers.IO) {

                    productViewModel.insertProduct(productEntity)
                }
                binding.editTextProduct.text!!.clear()
            } else {
                Toast.makeText(this@AdminSettings, "Enter any product", Toast.LENGTH_SHORT).show()
            }
        }

        temperatureToggle()

        setThermistor()


    }


    private fun setThermistor() {
        binding.setNTC.setOnClickListener {
            binding.setPTC.isChecked = !binding.setNTC.isChecked
        }
        binding.setPTC.setOnClickListener {
            binding.setNTC.isChecked = !binding.setPTC.isChecked
        }
        binding.setNTC.isChecked = true

        binding.setThermistor.setOnClickListener {
            if (binding.setNTC.isChecked) {
                jsonData = JSONObject()
                try {
                    jsonData.put("THERM_VAL", "0")
                    jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                    WebSocketManager.sendMessage(jsonData.toString())
                } catch (e: JSONException) {
                    throw RuntimeException(e)
                }
            } else if (binding.setPTC.isChecked) {
                jsonData = JSONObject()
                try {
                    jsonData.put("THERM_VAL", "1")
                    jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                    WebSocketManager.sendMessage(jsonData.toString())
                } catch (e: JSONException) {
                    throw RuntimeException(e)
                }
            } else {
                Toast.makeText(this@AdminSettings, "Check at least one", Toast.LENGTH_SHORT)
                    .show()
            }
        }


    }

    private fun temperatureToggle() {

        val tempVal =
            SharedPref.getSavedData(this@AdminSettings, "tempValue" + PhActivity.DEVICE_ID)

        if (tempVal != null) {
            binding.setManualTempEdit.setText(tempVal.toString())
        }


        binding.setManualTempBtn.setOnClickListener {
            if (binding.setManualTempEdit.text.toString() != "") {
                if (PhFragment.validateNumber(binding.setManualTempEdit.text.toString())) {
                    SharedPref.saveData(
                        this@AdminSettings,
                        "tempValue" + PhActivity.DEVICE_ID,
                        binding.setManualTempEdit.text.toString()
                    )
                }
            }
        }


        val tempToggleSharedPref =
            SharedPref.getSavedData(this@AdminSettings, "setTempToggle" + PhActivity.DEVICE_ID)

        if (tempToggleSharedPref != null) {
            if (tempToggleSharedPref == "") {
//                Toast.makeText(this, "Not Null", Toast.LENGTH_SHORT).show()
                binding.tempratureStateToggle.text = "Auto"
                binding.tempratureStateToggle.isChecked = true
                binding.setTempLayout.visibility = View.GONE
                SharedPref.saveData(
                    this@AdminSettings,
                    "setTempToggle" + PhActivity.DEVICE_ID,
                    "true"
                )

            } else {
                if (tempToggleSharedPref == "true") {
                    binding.tempratureStateToggle.text = "Auto"
                    binding.tempratureStateToggle.isChecked = true
                    binding.setTempLayout.visibility = View.GONE

                } else {
                    binding.tempratureStateToggle.text = "Manual"
                    binding.tempratureStateToggle.isChecked = false
                    binding.setTempLayout.visibility = View.VISIBLE
                }
            }
        } else {
            Toast.makeText(this, "Null", Toast.LENGTH_SHORT).show()
            binding.tempratureStateToggle.text = "Auto"
            binding.tempratureStateToggle.isChecked = true
            binding.setTempLayout.visibility = View.GONE
            SharedPref.saveData(
                this@AdminSettings,
                "setTempToggle" + PhActivity.DEVICE_ID,
                "true"
            )

        }

        binding.tempratureStateToggle.setOnClickListener {
            if (binding.tempratureStateToggle.isChecked) {
                SharedPref.saveData(
                    this@AdminSettings,
                    "setTempToggle" + PhActivity.DEVICE_ID,
                    "true"
                )
                binding.tempratureStateToggle.text = "Auto"
                binding.setTempLayout.visibility = View.GONE

            } else {
                SharedPref.saveData(
                    this@AdminSettings,
                    "setTempToggle" + PhActivity.DEVICE_ID,
                    "false"
                )
                binding.tempratureStateToggle.text = "Manual"
                binding.setTempLayout.visibility = View.VISIBLE

            }
        }
    }

    private fun observeProductList() {
        lifecycleScope.launch(Dispatchers.Main) {

            productViewModel.productListLiveData.observe(this@AdminSettings) { productList ->
                productListAdapter = ProductListAdapter(this@AdminSettings, productList, productsListDao)
                runOnUiThread {
//                    Toast.makeText(this@SettingPage, "" + productList.size, Toast.LENGTH_SHORT)
//                        .show()
                    binding.productRecView.adapter = productListAdapter
                    productListAdapter.notifyDataSetChanged()
                }
            }
        }
    }


    private fun fetchDesignations() {
        val leftDesignationString = SharedPref.getSavedData(this@AdminSettings, SharedKeys.LEFT_DESIGNATION_KEY)
        val rightDesignationString = SharedPref.getSavedData(this@AdminSettings, SharedKeys.RIGHT_DESIGNATION_KEY)


        if (leftDesignationString != null && leftDesignationString != ""){
            binding.leftDesignation.setText(leftDesignationString)
        }else{
            binding.leftDesignation.setText("Operator Sign")
            SharedPref.saveData(this@AdminSettings, SharedKeys.LEFT_DESIGNATION_KEY, "Operator Sign")
        }

        if (rightDesignationString != null && rightDesignationString != ""){
            binding.rightDesignation.setText(rightDesignationString)
        }else{
            binding.rightDesignation.setText("Supervisor Sign")
            SharedPref.saveData(this@AdminSettings, SharedKeys.RIGHT_DESIGNATION_KEY, "Supervisor Sign")
        }

        binding.leftDesignation.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                SharedPref.saveData(this@AdminSettings, SharedKeys.LEFT_DESIGNATION_KEY, p0.toString())

            }

            override fun afterTextChanged(p0: Editable?) {
            }

        })

        binding.rightDesignation.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                SharedPref.saveData(this@AdminSettings, SharedKeys.RIGHT_DESIGNATION_KEY, p0.toString())

            }

            override fun afterTextChanged(p0: Editable?) {
            }

        })

    }

    private fun fetchExportConditions() {

        val exportCsvEnabled = SharedPref.getSavedData(this@AdminSettings, "EXPORT_CSV")
        val exportPdfEnabled = SharedPref.getSavedData(this@AdminSettings, "EXPORT_PDF")

        if (exportCsvEnabled != null && exportCsvEnabled != "") {
            binding.exportCsvSwitch.isChecked = exportCsvEnabled == "true"
            Source.EXPORT_CSV = true
        }else{
            binding.exportCsvSwitch.isChecked = false
            SharedPref.saveData(this@AdminSettings,"EXPORT_CSV", "false")
            Source.EXPORT_CSV = false
        }

        if (exportPdfEnabled != null && exportPdfEnabled != "") {
            binding.exportPdfSwitch.isChecked = exportPdfEnabled == "true"
            Source.EXPORT_PDF = true
        }else{
            binding.exportPdfSwitch.isChecked = true
            Source.EXPORT_PDF = false
            SharedPref.saveData(this@AdminSettings,"EXPORT_PDF", "true")
        }

        binding.exportCsvSwitch.setOnClickListener {
            if (binding.exportCsvSwitch.isChecked){
                Source.EXPORT_CSV = true
                SharedPref.saveData(this@AdminSettings,"EXPORT_CSV", "true")
            }else{
                Source.EXPORT_CSV = false
                SharedPref.saveData(this@AdminSettings,"EXPORT_CSV", "false")
            }
        }

        binding.exportPdfSwitch.setOnClickListener {
            if (binding.exportPdfSwitch.isChecked){
                Source.EXPORT_PDF = true
                SharedPref.saveData(this@AdminSettings,"EXPORT_PDF", "true")
            }else{
                Source.EXPORT_PDF = false
                SharedPref.saveData(this@AdminSettings,"EXPORT_PDF", "false")
            }
        }

    }


}