package com.aican.aicanapp.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.aican.aicanapp.EditUserDatabase
import com.aican.aicanapp.R
import com.aican.aicanapp.interfaces.UserDeleteListener
import com.aican.aicanapp.roomDatabase.daoObjects.UserDao
import com.aican.aicanapp.roomDatabase.entities.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class NewUserAdapter(
    val context: Context,
    private val userList: List<UserEntity>,
    val userDao: UserDao,
    val userDeleteListener: UserDeleteListener
) :
    RecyclerView.Adapter<NewUserAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.users_table_item, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val currentUser = userList[position]
        holder.bind(currentUser)

        holder.editBtn.setOnClickListener {
            val intent = Intent(context, EditUserDatabase::class.java)
            intent.putExtra("username", userList.get(position).name)
            intent.putExtra("userrole", userList.get(position).role)
            intent.putExtra("passcode", userList.get(position).password)
            intent.putExtra("uid", userList.get(position).id)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.getApplicationContext().startActivity(intent)
        }

        holder.deleteBtn.setOnClickListener {
            deleteUserById(currentUser, position)
        }

    }

    private fun deleteUserById(user: UserEntity, position: Int) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                // Retrieve the user from the database (optional)
                val userToDelete = userDao.getUserById(user.id)

                // Delete the user by ID
                if (userToDelete != null) {
                    userDao.deleteUser(user)

                    (context as Activity).runOnUiThread {
                        userDeleteListener.deleted()
                    }

                    // Optionally, perform any UI updates or notify the user
                    showToast("User deleted successfully", position)
                } else {
                    showToast("User not found", position)
                }
            } catch (e: Exception) {
                showToast("Failed to delete user: ${e.message}", position)
            }
        }
    }

    private fun showToast(message: String, position: Int) {
        (context as Activity).runOnUiThread {
            notifyItemRemoved(position)

            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        public val userNumberTextView: TextView = itemView.findViewById(R.id.userNumber)
        public val userNameTextView: TextView = itemView.findViewById(R.id.user_name)
        public val userRoleTextView: TextView = itemView.findViewById(R.id.user_role)
        public val dateCreatedTextView: TextView = itemView.findViewById(R.id.dateCreated)
        public val expiryDateTextView: TextView = itemView.findViewById(R.id.expiry_date)
        public val editBtn: ImageView = itemView.findViewById(R.id.editBtn)
        public val deleteBtn: ImageView = itemView.findViewById(R.id.deleteBtn)

        fun bind(user: UserEntity) {
            userNumberTextView.text = "User ${adapterPosition + 1}"
            userNameTextView.text = user.name
            userRoleTextView.text = user.role
            dateCreatedTextView.text = user.dateOfCreation.toString() // Format as needed
            expiryDateTextView.text = user.dateOfExpiry.toString() // Format as needed
        }
    }
}
