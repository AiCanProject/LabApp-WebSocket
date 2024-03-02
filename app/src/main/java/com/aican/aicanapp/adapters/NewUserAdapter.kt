package com.aican.aicanapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aican.aicanapp.R
import com.aican.aicanapp.roomDatabase.entities.UserEntity

class NewUserAdapter(private val userList: List<UserEntity>) :
    RecyclerView.Adapter<NewUserAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.users_table_item, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val currentUser = userList[position]
        holder.bind(currentUser)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userNumberTextView: TextView = itemView.findViewById(R.id.userNumber)
        private val userNameTextView: TextView = itemView.findViewById(R.id.user_name)
        private val userRoleTextView: TextView = itemView.findViewById(R.id.user_role)
        private val dateCreatedTextView: TextView = itemView.findViewById(R.id.dateCreated)
        private val expiryDateTextView: TextView = itemView.findViewById(R.id.expiry_date)

        fun bind(user: UserEntity) {
            userNumberTextView.text = "User ${adapterPosition + 1}"
            userNameTextView.text = user.name
            userRoleTextView.text = user.role
            dateCreatedTextView.text = user.dateOfCreation.toString() // Format as needed
            expiryDateTextView.text = user.dateOfExpiry.toString() // Format as needed
        }
    }
}
