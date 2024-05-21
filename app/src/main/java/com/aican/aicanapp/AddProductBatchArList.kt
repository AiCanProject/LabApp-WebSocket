package com.aican.aicanapp

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.aican.aicanapp.adapters.ARNumListAdapter
import com.aican.aicanapp.adapters.BatchListAdapter
import com.aican.aicanapp.adapters.ProductListAdapter
import com.aican.aicanapp.adapters.UnknownListAdapter1
import com.aican.aicanapp.adapters.UnknownListAdapter2
import com.aican.aicanapp.databinding.ActivityAddProductBatchArListBinding
import com.aican.aicanapp.roomDatabase.daoObjects.ARNumDao
import com.aican.aicanapp.roomDatabase.daoObjects.BatchListDao
import com.aican.aicanapp.roomDatabase.daoObjects.ProductsListDao
import com.aican.aicanapp.roomDatabase.daoObjects.UnknownListDao1
import com.aican.aicanapp.roomDatabase.daoObjects.UnknownListDao2
import com.aican.aicanapp.roomDatabase.database.AppDatabase
import com.aican.aicanapp.roomDatabase.entities.ARNumEntity
import com.aican.aicanapp.roomDatabase.entities.BatchEntity
import com.aican.aicanapp.roomDatabase.entities.ProductEntity
import com.aican.aicanapp.roomDatabase.entities.UnknownEntity1
import com.aican.aicanapp.roomDatabase.entities.UnknownEntity2
import com.aican.aicanapp.utils.SharedPref
import com.aican.aicanapp.viewModels.ARNumViewModel
import com.aican.aicanapp.viewModels.ARNumViewModelFactory
import com.aican.aicanapp.viewModels.BatchViewModel
import com.aican.aicanapp.viewModels.BatchViewModelFactory
import com.aican.aicanapp.viewModels.ProductViewModel
import com.aican.aicanapp.viewModels.ProductViewModelFactory
import com.aican.aicanapp.viewModels.UnknownListViewModel1
import com.aican.aicanapp.viewModels.UnknownListViewModel2
import com.aican.aicanapp.viewModels.UnknownListViewModelFactory1
import com.aican.aicanapp.viewModels.UnknownListViewModelFactory2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AddProductBatchArList : AppCompatActivity() {

    lateinit var binding: ActivityAddProductBatchArListBinding
    lateinit var productsListDao: ProductsListDao
    lateinit var unknownListDao1: UnknownListDao1
    lateinit var unknownListDao2: UnknownListDao2
    lateinit var arNumListDao: ARNumDao
    lateinit var batchListDao: BatchListDao

    lateinit var productListAdapter: ProductListAdapter
    lateinit var unknownListAdapter1: UnknownListAdapter1
    lateinit var unknownListAdapter2: UnknownListAdapter2

    lateinit var arNumListAdapter: ARNumListAdapter
    lateinit var batchListAdapter: BatchListAdapter
    private lateinit var productViewModel: ProductViewModel
    private lateinit var arNumViewModel: ARNumViewModel
    lateinit var batchViewModel: BatchViewModel

    lateinit var unknownListViewModel1: UnknownListViewModel1
    lateinit var unknownListViewModel2: UnknownListViewModel2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddProductBatchArListBinding.inflate(layoutInflater)
        setContentView(binding.root)



        binding.knownHeading1.text = SharedPref.getSavedData(this@AddProductBatchArList, "known1")
        binding.knownHeading2.text = SharedPref.getSavedData(this@AddProductBatchArList, "known2")
        binding.knownHeading3.text = SharedPref.getSavedData(this@AddProductBatchArList, "known3")


        binding.unknownHeading1.text = SharedPref.getSavedData(this@AddProductBatchArList, "unknownHeading1")

        binding.unknownHeading2.text = SharedPref.getSavedData(this@AddProductBatchArList, "unknownHeading2")



        unknownListDao1 = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "aican-database"
        ).build().unknown1Dao()

        unknownListDao2 = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "aican-database"
        ).build().unknown2Dao()

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

        val viewModelFactory1 = UnknownListViewModelFactory1(unknownListDao1)
        unknownListViewModel1 =
            ViewModelProvider(this, viewModelFactory1)[UnknownListViewModel1::class.java]

        val viewModelFactory2 = UnknownListViewModelFactory2(unknownListDao2)
        unknownListViewModel2 =
            ViewModelProvider(this, viewModelFactory2)[UnknownListViewModel2::class.java]

        val viewModelFactory = ProductViewModelFactory(productsListDao)
        productViewModel =
            ViewModelProvider(this, viewModelFactory)[ProductViewModel::class.java]

        val viewModelFactoryBatch = BatchViewModelFactory(batchListDao)
        batchViewModel =
            ViewModelProvider(this, viewModelFactoryBatch)[BatchViewModel::class.java]

        val viewModelFactoryARNum = ARNumViewModelFactory(arNumListDao)
        arNumViewModel =
            ViewModelProvider(this, viewModelFactoryARNum)[ARNumViewModel::class.java]



        observeUnknownList1()
        observeUnknownList2()
        observeProductList()
        observeARList()
        observeBatchList()

        binding.addUnknown1.setOnClickListener {
            val product = binding.editTextUnknown1.text.toString()
            if (product != "") {
                val productEntity = UnknownEntity1(productName = product)
                lifecycleScope.launch(Dispatchers.IO) {

                    unknownListViewModel1.insertProduct(productEntity)
                }
                binding.editTextUnknown1.text!!.clear()
            } else {
                Toast.makeText(this@AddProductBatchArList, "Enter any text", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        binding.addUnknown2.setOnClickListener {
            val product = binding.editTextUnKnown2.text.toString()
            if (product != "") {
                val productEntity = UnknownEntity2(productName = product)
                lifecycleScope.launch(Dispatchers.IO) {

                    unknownListViewModel2.insertProduct(productEntity)
                }
                binding.editTextUnKnown2.text!!.clear()
            } else {
                Toast.makeText(this@AddProductBatchArList, "Enter any text", Toast.LENGTH_SHORT)
                    .show()
            }
        }

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


        binding.knownHeading1.text = SharedPref.getSavedData(this@AddProductBatchArList, "known1")


        binding.knownHeading2.text = SharedPref.getSavedData(this@AddProductBatchArList, "known2")

        binding.knownHeading3.text = SharedPref.getSavedData(this@AddProductBatchArList, "known3")


        binding.unknownHeading1.text = SharedPref.getSavedData(this@AddProductBatchArList, "unknownHeading1")

        binding.unknownHeading2.text = SharedPref.getSavedData(this@AddProductBatchArList, "unknownHeading2")

        binding.unknownHeading1.setOnClickListener {
            showEditDialog("unknownHeading1")
        }

        binding.unknownHeading2.setOnClickListener {
            showEditDialog("unknownHeading2")
        }

        binding.knownHeading1.setOnClickListener {
            showEditDialog("known1")
        }

        binding.knownHeading2.setOnClickListener {
            showEditDialog("known2")
        }

        binding.knownHeading3.setOnClickListener {
            showEditDialog("known3")
        }



    }
    private fun showEditDialog(key: String) {
        val dialog = Dialog(this@AddProductBatchArList)
        dialog.setContentView(R.layout.dialog_heading_edit_text)
        val editText = dialog.findViewById<EditText>(R.id.editText)
        val buttonSave = dialog.findViewById<Button>(R.id.buttonSave)

        buttonSave.setOnClickListener {
            val newText = editText.text.toString()
            SharedPref.saveData(this@AddProductBatchArList, key, newText)
            if (key == "unknownHeading1") {
                binding.unknownHeading1.text = newText
            } else if (key == "unknownHeading2") {
                binding.unknownHeading2.text = newText
            } else if (key == "known1") {
                binding.knownHeading1.text = newText
            } else if (key == "known2") {
                binding.knownHeading2.text = newText
            } else if (key == "known3") {
                binding.knownHeading3.text = newText
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun observeUnknownList1() {
        lifecycleScope.launch(Dispatchers.Main) {

            unknownListViewModel1.productListLiveData.observe(this@AddProductBatchArList) { productList ->
                unknownListAdapter1 =
                    UnknownListAdapter1(this@AddProductBatchArList, productList, unknownListDao1)
                runOnUiThread {
//                    Toast.makeText(this@SettingPage, "" + productList.size, Toast.LENGTH_SHORT)
//                        .show()
                    binding.unknown1RecView.adapter = unknownListAdapter1
                    unknownListAdapter1.notifyDataSetChanged()
                }
            }
        }
    }

    private fun observeUnknownList2() {
        lifecycleScope.launch(Dispatchers.Main) {

            unknownListViewModel2.productListLiveData.observe(this@AddProductBatchArList) { productList ->
                unknownListAdapter2 =
                    UnknownListAdapter2(this@AddProductBatchArList, productList, unknownListDao2)
                runOnUiThread {
//                    Toast.makeText(this@SettingPage, "" + productList.size, Toast.LENGTH_SHORT)
//                        .show()
                    binding.unknown2RecView.adapter = unknownListAdapter2
                    unknownListAdapter2.notifyDataSetChanged()
                }
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