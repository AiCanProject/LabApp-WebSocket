package com.aican.aicanapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.aican.aicanapp.adapters.ARNumListAdapter
import com.aican.aicanapp.adapters.BatchListAdapter
import com.aican.aicanapp.adapters.ProductListAdapter
import com.aican.aicanapp.databinding.ActivityAddProductBatchArListBinding
import com.aican.aicanapp.roomDatabase.daoObjects.ARNumDao
import com.aican.aicanapp.roomDatabase.daoObjects.BatchListDao
import com.aican.aicanapp.roomDatabase.daoObjects.ProductsListDao
import com.aican.aicanapp.roomDatabase.database.AppDatabase
import com.aican.aicanapp.roomDatabase.entities.ARNumEntity
import com.aican.aicanapp.roomDatabase.entities.BatchEntity
import com.aican.aicanapp.roomDatabase.entities.ProductEntity
import com.aican.aicanapp.viewModels.ARNumViewModel
import com.aican.aicanapp.viewModels.ARNumViewModelFactory
import com.aican.aicanapp.viewModels.BatchViewModel
import com.aican.aicanapp.viewModels.BatchViewModelFactory
import com.aican.aicanapp.viewModels.ProductViewModel
import com.aican.aicanapp.viewModels.ProductViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AddProductBatchArList : AppCompatActivity() {

    lateinit var binding: ActivityAddProductBatchArListBinding
    lateinit var productsListDao: ProductsListDao
    lateinit var arNumListDao: ARNumDao
    lateinit var batchListDao: BatchListDao
    lateinit var productListAdapter: ProductListAdapter
    lateinit var arNumListAdapter: ARNumListAdapter
    lateinit var batchListAdapter: BatchListAdapter
    private lateinit var productViewModel: ProductViewModel
    private lateinit var arNumViewModel: ARNumViewModel
    lateinit var batchViewModel: BatchViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddProductBatchArListBinding.inflate(layoutInflater)
        setContentView(binding.root)


        productsListDao = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "aican-database"
        ).build().productsDao()

        batchListDao = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "aican-database"
        ).build().batchDao()

        arNumListDao = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "aican-database"
        ).build().arNumDao()

        val viewModelFactory = ProductViewModelFactory(productsListDao)
        productViewModel =
            ViewModelProvider(this, viewModelFactory)[ProductViewModel::class.java]

        val viewModelFactoryBatch = BatchViewModelFactory(batchListDao)
        batchViewModel =
            ViewModelProvider(this, viewModelFactoryBatch)[BatchViewModel::class.java]

        val viewModelFactoryARNum = ARNumViewModelFactory(arNumListDao)
        arNumViewModel =
            ViewModelProvider(this, viewModelFactoryARNum)[ARNumViewModel::class.java]


        observeProductList()
        observeARList()
        observeBatchList()

        binding.addProductBtn.setOnClickListener {
            val product = binding.editTextProduct.text.toString()
            if (product != "") {
                val productEntity = ProductEntity(productName = product)
                lifecycleScope.launch(Dispatchers.IO) {

                    productViewModel.insertProduct(productEntity)
                }
                binding.editTextProduct.text!!.clear()
            } else {
                Toast.makeText(this@AddProductBatchArList, "Enter any product", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        binding.addBatchBtn.setOnClickListener {
            val product = binding.editTextBatch.text.toString()
            if (product != "") {
                val productEntity = BatchEntity(batchName = product)
                lifecycleScope.launch(Dispatchers.IO) {

                    batchViewModel.insertProduct(productEntity)
                }
                binding.editTextBatch.text!!.clear()
            } else {
                Toast.makeText(
                    this@AddProductBatchArList,
                    "Enter any batch number",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }

        binding.addARNumBtn.setOnClickListener {
            val product = binding.editTextARNum.text.toString()
            if (product != "") {
                val productEntity = ARNumEntity(arNum = product)
                lifecycleScope.launch(Dispatchers.IO) {

                    arNumViewModel.insertProduct(productEntity)
                }
                binding.editTextARNum.text!!.clear()
            } else {
                Toast.makeText(
                    this@AddProductBatchArList,
                    "Enter any AR number",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }


    }

    private fun observeBatchList() {
        lifecycleScope.launch(Dispatchers.Main) {

            batchViewModel.productListLiveData.observe(this@AddProductBatchArList) { productList ->
                batchListAdapter =
                    BatchListAdapter(this@AddProductBatchArList, productList, batchListDao)
                runOnUiThread {
//                    Toast.makeText(this@SettingPage, "" + productList.size, Toast.LENGTH_SHORT)
//                        .show()
                    binding.batchRecView.adapter = batchListAdapter
                    batchListAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun observeARList() {
        lifecycleScope.launch(Dispatchers.Main) {

            arNumViewModel.productListLiveData.observe(this@AddProductBatchArList) { productList ->
                arNumListAdapter =
                    ARNumListAdapter(this@AddProductBatchArList, productList, arNumListDao)
                runOnUiThread {
//                    Toast.makeText(this@SettingPage, "" + productList.size, Toast.LENGTH_SHORT)
//                        .show()
                    binding.aRNumRecView.adapter = arNumListAdapter
                    arNumListAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun observeProductList() {
        lifecycleScope.launch(Dispatchers.Main) {

            productViewModel.productListLiveData.observe(this@AddProductBatchArList) { productList ->
                productListAdapter =
                    ProductListAdapter(this@AddProductBatchArList, productList, productsListDao)
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