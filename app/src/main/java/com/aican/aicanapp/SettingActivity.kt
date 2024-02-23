package com.aican.aicanapp

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
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aican.aicanapp.Authentication.AdminLoginActivity
import com.aican.aicanapp.FirebaseAccounts.PrimaryAccount
import com.aican.aicanapp.data.DatabaseHelper
import com.aican.aicanapp.ph.PhActivity
import com.aican.aicanapp.utils.Source
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.kyanogen.signatureview.SignatureView
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SettingActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {


    lateinit var databaseHelper: DatabaseHelper
    lateinit var name: EditText
    lateinit var passcode: EditText
    lateinit var userId: EditText
    lateinit var generate: Button
    lateinit var addSign: Button
    lateinit var spinner: Spinner
    lateinit var imageView: ImageView
    lateinit var user_database: ImageView
    val r = arrayOf("Operator", "Supervisor")
    lateinit var Role: String

    lateinit var mDatabase: DatabaseReference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        userId = findViewById<EditText>(R.id.assignedId)
        name = findViewById<EditText>(R.id.assignedName)
        passcode = findViewById<EditText>(R.id.assignedPwd)
        generate = findViewById<Button>(R.id.assignRole)
        spinner = findViewById<Spinner>(R.id.selectRole)
        addSign = findViewById<Button>(R.id.addSignature)
        imageView = findViewById<ImageView>(R.id.signature)
        user_database = findViewById<ImageView>(R.id.btnUserDatabase)

        addSign.setOnClickListener { //                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            //                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            //                showOptionDialog();
            showSignatureDialog()
        }

        val role = ArrayAdapter(this, android.R.layout.simple_spinner_item, r)
        role.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = role
        spinner.setOnItemSelectedListener(this)
        mDatabase = FirebaseDatabase.getInstance(PrimaryAccount.getInstance(this)).reference

        databaseHelper = DatabaseHelper(this)

        user_database.setOnClickListener {
            val intent = Intent(applicationContext, AdminLoginActivity::class.java)
            intent.putExtra("checkBtn", "checkDatabase")
            startActivity(intent)
        }

        generate.setOnClickListener { view: View? ->
            if (isEmailValid() && isPassCodeValid()) {
                val date =
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(Date())
                val time =
                    SimpleDateFormat("HH:mm", Locale.getDefault())
                        .format(Date())
                Source.userRole = Role
                Source.userId = userId.text.toString()
                Source.userName = name.text.toString()
                Source.userPasscode = passcode.text.toString()
                Source.expiryDate = getExpiryDate()
                Source.dateCreated = getPresentDate()
                Log.d("expiryDate", "onCreate: " + Source.expiryDate)
                databaseHelper.insert_data(
                    Source.userName,
                    Source.userRole,
                    Source.userId,
                    Source.userPasscode,
                    Source.expiryDate,
                    Source.dateCreated
                )
                databaseHelper.insertUserData(
                    Source.userName,
                    Source.userId,
                    Source.userRole,
                    Source.expiryDate,
                    Source.dateCreated
                )
                val details =
                    """
                        $Role
                        ${name.text}
                        
                        """.trimIndent() + passcode.text
                        .toString() + "\n" + userId.text.toString()
                databaseHelper.insert_action_data(
                    time,
                    date,
                    "Name: " + Source.userName + ", UID: " + Source.userId + " Role: " + Source.userRole + " user added",
                    "",
                    "",
                    "",
                    "",
                    PhActivity.DEVICE_ID
                )
                var fos: FileOutputStream? = null
                Toast.makeText(applicationContext, "Role Assigned", Toast.LENGTH_SHORT).show()
                try {
                    fos = openFileOutput(
                        FILE_NAME,
                        MODE_PRIVATE
                    )
                    fos.write(details.toByteArray())
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    if (fos != null) {
                        try {
                            fos.close()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Role Not Assigned", Toast.LENGTH_SHORT).show()
            }
        }

        val comLo: Bitmap = getSignImage()!!
        if (comLo != null) {
            imageView.setImageBitmap(comLo)
        }


    }

    private fun showSignatureDialog() {
        val dialog = Dialog(this@SettingActivity)
        dialog.setContentView(R.layout.signature_layout)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(true)
        dialog.setCancelable(true)
        val signatureView: SignatureView = dialog.findViewById(R.id.signature_view)
        dialog.findViewById<View>(R.id.signClear).setOnClickListener { signatureView.clearCanvas() }
        dialog.findViewById<View>(R.id.signSave)
            .setOnClickListener { //                dialog.dismiss();
                val signBitmap: Bitmap = signatureView.getSignatureBitmap()
                if (signBitmap != null) {
                    imageView.setImageBitmap(signBitmap)
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

    private fun showOptionDialog() {
        val dialog = Dialog(this@SettingActivity)
        dialog.setContentView(R.layout.img_options_dialog)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(true)
        dialog.findViewById<View>(R.id.gallery).setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE)
            dialog.dismiss()
        }
        dialog.findViewById<View>(R.id.camera).setOnClickListener {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, CAMERA_REQUEST)
            dialog.dismiss()
        }
        dialog.show()
    }


    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this@SettingActivity, Dashboard::class.java))
        finish()
    }

    override fun onItemSelected(adapterView: AdapterView<*>, view: View?, i: Int, l: Long) {
        Role = adapterView.getItemAtPosition(i).toString()
    }

    override fun onNothingSelected(adapterView: AdapterView<*>?) {
        Role = spinner.selectedItem.toString()
    }

    private fun isPassCodeValid(): Boolean {
        val validName = passcode.text.toString()
        if (validName.isEmpty()) {
            Toast.makeText(this, "Enter Passcode!", Toast.LENGTH_SHORT).show()
        }
        return true
    }

    private fun isEmailValid(): Boolean {
        val validName = name.text.toString()
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
        if (requestCode == MY_CAMERA_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show()
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(cameraIntent, CAMERA_REQUEST)
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            val photo = data!!.extras!!["data"] as Bitmap?
            saveImage(photo)
            imageView.setImageBitmap(photo)
            imageView.visibility = View.VISIBLE
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
                        imageView.setImageBitmap(photo)
                        imageView.visibility = View.VISIBLE
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

    companion object {
        const val FILE_NAMEE = "user_calibrate.txt"
        const val FILE_NAME = "user_info.txt"
        const val MY_CAMERA_PERMISSION_CODE = 100
        const val CAMERA_REQUEST = 1888
    }
}