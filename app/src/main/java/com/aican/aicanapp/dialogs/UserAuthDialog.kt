package com.aican.aicanapp.dialogs

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatButton
import com.aican.aicanapp.R
import com.aican.aicanapp.roomDatabase.daoObjects.UserDao
import com.aican.aicanapp.utils.Source
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar

class UserAuthDialog(private val context: Context, private val userDao: UserDao) {

    fun showLoginDialog(onLoginListener: (isValidCredentials: Boolean) -> Unit) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_login, null)
        val alertDialogBuilder = AlertDialog.Builder(context)
            .setView(dialogView)


        val alertDialog = alertDialogBuilder.show()

        alertDialog.setCancelable(false)
        alertDialog.setCanceledOnTouchOutside(false)

        dialogView.findViewById<LinearLayout>(R.id.cancelLay).setOnClickListener {
            (context as Activity).finish()
        }

        dialogView.findViewById<AppCompatButton>(R.id.authenticateRole).setOnClickListener {
            val userid = dialogView.findViewById<TextInputEditText>(R.id.userId).text.toString()
            val password = dialogView.findViewById<TextInputEditText>(R.id.userPwd).text.toString()


            GlobalScope.launch {
                val user = withContext(Dispatchers.IO) {
                    userDao.getUserById(userid)
                }

                val isValidCredentials =
                    user != null && user.password == password && user.isActive &&
                            user.dateOfExpiry > getPresentDate().toString()

                withContext(Dispatchers.Main) {
                    onLoginListener(isValidCredentials)

                    if (isValidCredentials) {
                        Source.userId = userid
                        Source.userName = user!!.name
                        Source.userRole = user.role
                        alertDialog.dismiss()
                    }
                }
            }

        }
    }

    private fun getPresentDate(): String? {
        val date = Calendar.getInstance().time
        val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd")
        return dateFormat.format(date)
    }
}