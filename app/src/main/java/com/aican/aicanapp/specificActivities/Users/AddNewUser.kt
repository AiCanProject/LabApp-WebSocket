package com.aican.aicanapp.specificActivities.Users

import android.R
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.aican.aicanapp.SettingActivity
import com.aican.aicanapp.databinding.ActivityAddNewUserBinding
import com.aican.aicanapp.roomDatabase.daoObjects.UserDao
import com.aican.aicanapp.roomDatabase.database.AppDatabase
import com.aican.aicanapp.roomDatabase.entities.UserEntity
import com.kyanogen.signatureview.SignatureView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar

class AddNewUser : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private lateinit var userDao: UserDao
    lateinit var binding: ActivityAddNewUserBinding
    val r = arrayOf("Operator", "Supervisor")

    var ROLE = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddNewUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val role = ArrayAdapter(this, R.layout.simple_spinner_item, r)
        role.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.selectRole.adapter = role
        binding.selectRole.onItemSelectedListener = this


        binding.addSignature.setOnClickListener {
            showSignatureDialog()
        }

        userDao =
            Room.databaseBuilder(applicationContext, AppDatabase::class.java, "aican-database")
                .build().userDao()

        // Add user functionality
        binding.assignRole.setOnClickListener {

            val uid = binding.assignedId.text.toString()
            val username = binding.assignedName.text.toString()
            val password = binding.assignedPwd.text.toString()
            val userType = ROLE
            if (isEmailValid(uid) && isPassCodeValid(password) && isValidRole(userType.toString()) &&
                isValidName(username)
            ) {
                lifecycleScope.launch(Dispatchers.IO) {

                    val existingUser = userDao.getUserById(uid)
                    if (existingUser != null) {
                        runOnUiThread {
                            showToast("User with ID ${uid} already exists!")
                        }
                    } else {

                        val user = UserEntity(
                            uid,
                            username,
                            password,
                            userType,
                            getPresentDate().toString(),
                            getExpiryDate().toString(),
                            getPresentDate().toString(),
                            true
                        )


                        userDao.insertUser(user)
                        runOnUiThread {

                            showToast("User inserted successfully!")
                        }
                    }


                }
            }
        }

    }

    fun showToast(message: String) {
        Toast.makeText(this@AddNewUser, message, Toast.LENGTH_SHORT).show()
    }

    private fun showSignatureDialog() {
        val dialog = Dialog(this@AddNewUser)
        dialog.setContentView(com.aican.aicanapp.R.layout.signature_layout)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(true)
        dialog.setCancelable(true)
        val signatureView: SignatureView =
            dialog.findViewById(com.aican.aicanapp.R.id.signature_view)
        dialog.findViewById<View>(com.aican.aicanapp.R.id.signClear)
            .setOnClickListener { signatureView.clearCanvas() }
        dialog.findViewById<View>(com.aican.aicanapp.R.id.signSave)
            .setOnClickListener { //                dialog.dismiss();
                val signBitmap: Bitmap? = signatureView.getSignatureBitmap()
                if (signBitmap != null) {
                    binding.signature.setImageBitmap(signBitmap)
                    saveImage(signBitmap)
                    dialog.dismiss()
                }
            }
        dialog.show()
    }

    private fun getSignImage(): Bitmap? {
        val sh = getSharedPreferences("signature", MODE_PRIVATE)
        val photo = sh.getString("signature_data", "")
        var bitmap: Bitmap? = null
        if (!photo.equals("", ignoreCase = true)) {
            val b = Base64.decode(photo, Base64.DEFAULT)
            bitmap = BitmapFactory.decodeByteArray(b, 0, b.size)
        }
        return bitmap
    }

    private fun saveImage(realImage: Bitmap?) {
        val baos = ByteArrayOutputStream()
        realImage!!.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val b = baos.toByteArray()
        val encodedImage = Base64.encodeToString(b, Base64.DEFAULT)
        val shre = getSharedPreferences("signature", MODE_PRIVATE)
        val edit = shre.edit()
        edit.putString("signature_data", encodedImage)
        edit.commit()
    }

    var PICK_IMAGE = 1

    private fun isValidName(passcode: String): Boolean {
        val validName = passcode
        if (validName.isEmpty()) {
            Toast.makeText(this, "Enter name!", Toast.LENGTH_SHORT).show()
        }
        return true
    }

    private fun isPassCodeValid(passcode: String): Boolean {
        val validName = passcode
        if (validName.isEmpty()) {
            Toast.makeText(this, "Enter Passcode!", Toast.LENGTH_SHORT).show()
        }
        return true
    }

    private fun isValidRole(role: String): Boolean {
        val validName = role
        if (validName.isEmpty()) {
            Toast.makeText(this, "Choose role!", Toast.LENGTH_SHORT).show()
        }
        return true
    }

    private fun isEmailValid(name: String): Boolean {
        val validName = name
        if (validName.isEmpty()) {
            Toast.makeText(this, "Enter Email Address!", Toast.LENGTH_SHORT).show()
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SettingActivity.MY_CAMERA_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show()
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(cameraIntent, SettingActivity.CAMERA_REQUEST)
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show()
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SettingActivity.CAMERA_REQUEST && resultCode == RESULT_OK) {
            val photo = data!!.extras!!["data"] as Bitmap?
            saveImage(photo)
            binding.signature.setImageBitmap(photo)
            binding.signature.visibility = View.VISIBLE
            //            addSign.setText("Ok!");
//            addSign.setEnabled(false);
        }
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            if (data != null) {
                val picUri = data.data //<- get Uri here from data intent
                if (picUri != null) {
//                    Bitmap photo = (Bitmap) data.getExtras().get("data");
                    var photo: Bitmap? = null
                    try {
                        photo = MediaStore.Images.Media.getBitmap(
                            this.contentResolver,
                            picUri
                        )
                        saveImage(photo)
                        binding.signature.setImageBitmap(photo)
                        binding.signature.visibility = View.VISIBLE
                        //                        selectCompanyLogo.setText("Ok!");
                    } catch (e: FileNotFoundException) {
                        throw RuntimeException(e)
                    } catch (e: IOException) {
                        throw RuntimeException(e)
                    }
                }
            }
        }
    }


    private fun getExpiryDate(): String? {
        val date = Calendar.getInstance().time
        val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd")
        val presentDate = dateFormat.format(date)
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd")
        try {
            cal.time = sdf.parse(presentDate)
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        // use add() method to add the days to the given date
        cal.add(Calendar.DAY_OF_MONTH, 90)
        return sdf.format(cal.time)
    }

    private fun getPresentDate(): String? {
        val date = Calendar.getInstance().time
        val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd")
        return dateFormat.format(date)
    }


    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        ROLE = p0?.getItemAtPosition(p2).toString()

    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        ROLE = p0?.selectedItem.toString()

    }
}