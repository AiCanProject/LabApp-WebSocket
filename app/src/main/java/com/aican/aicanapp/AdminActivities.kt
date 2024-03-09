package com.aican.aicanapp

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.aican.aicanapp.adapters.ProductListAdapter
import com.aican.aicanapp.databinding.ActivitiesAdminPageBinding
import com.aican.aicanapp.ph.PhActivity
import com.aican.aicanapp.ph.phFragment.PhFragment
import com.aican.aicanapp.roomDatabase.daoObjects.ProductsListDao
import com.aican.aicanapp.roomDatabase.database.AppDatabase
import com.aican.aicanapp.roomDatabase.entities.ProductEntity
import com.aican.aicanapp.utils.SharedPref
import com.aican.aicanapp.viewModels.ProductViewModel
import com.aican.aicanapp.viewModels.ProductViewModelFactory
import com.aican.aicanapp.websocket.WebSocketManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject

class AdminActivities : AppCompatActivity() {

    lateinit var binding: ActivitiesAdminPageBinding
    lateinit var productsListDao: ProductsListDao
    lateinit var productListLiveData: LiveData<List<ProductEntity>>
    lateinit var productListAdapter: ProductListAdapter
    private lateinit var productViewModel: ProductViewModel
    lateinit var jsonData: JSONObject

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitiesAdminPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                Toast.makeText(this@AdminActivities, "Enter any product", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this@AdminActivities, "Check at least one", Toast.LENGTH_SHORT)
                    .show()
            }
        }


    }

    private fun temperatureToggle() {

        val tempVal =
            SharedPref.getSavedData(this@AdminActivities, "tempValue" + PhActivity.DEVICE_ID)

        if (tempVal != null) {
            binding.setManualTempEdit.setText(tempVal.toString())
        }


        binding.setManualTempBtn.setOnClickListener {
            if (binding.setManualTempEdit.text.toString() != "") {
                if (PhFragment.validateNumber(binding.setManualTempEdit.text.toString())) {
                    SharedPref.saveData(
                        this@AdminActivities,
                        "tempValue" + PhActivity.DEVICE_ID,
                        binding.setManualTempEdit.text.toString()
                    )
                }
            }
        }


        val tempToggleSharedPref =
            SharedPref.getSavedData(this@AdminActivities, "setTempToggle" + PhActivity.DEVICE_ID)

        if (tempToggleSharedPref != null) {
            if (tempToggleSharedPref == "") {
//                Toast.makeText(this, "Not Null", Toast.LENGTH_SHORT).show()
                binding.tempratureStateToggle.text = "Auto"
                binding.tempratureStateToggle.isChecked = true
                binding.setTempLayout.visibility = View.GONE
                SharedPref.saveData(
                    this@AdminActivities,
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
                this@AdminActivities,
                "setTempToggle" + PhActivity.DEVICE_ID,
                "true"
            )

        }

        binding.tempratureStateToggle.setOnClickListener {
            if (binding.tempratureStateToggle.isChecked) {
                SharedPref.saveData(
                    this@AdminActivities,
                    "setTempToggle" + PhActivity.DEVICE_ID,
                    "true"
                )
                binding.tempratureStateToggle.text = "Auto"
                binding.setTempLayout.visibility = View.GONE

            } else {
                SharedPref.saveData(
                    this@AdminActivities,
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

            productViewModel.productListLiveData.observe(this@AdminActivities) { productList ->
                productListAdapter = ProductListAdapter(this@AdminActivities, productList, productsListDao)
                runOnUiThread {
//                    Toast.makeText(this@SettingPage, "" + productList.size, Toast.LENGTH_SHORT)
//                        .show()
                    binding.productRecView.adapter = productListAdapter
                    productListAdapter.notifyDataSetChanged()
                }
            }
        }
    }

}