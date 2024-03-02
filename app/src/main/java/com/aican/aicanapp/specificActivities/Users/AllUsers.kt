package com.aican.aicanapp.specificActivities.Users

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.aican.aicanapp.adapters.NewUserAdapter
import com.aican.aicanapp.databinding.ActivityAllUsersBinding
import com.aican.aicanapp.roomDatabase.daoObjects.UserDao
import com.aican.aicanapp.roomDatabase.database.AppDatabase
import com.aican.aicanapp.roomDatabase.entities.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class AllUsers : AppCompatActivity() {

    private val userList = mutableListOf<UserEntity>()
    lateinit var binding: ActivityAllUsersBinding
    private lateinit var userDao: UserDao
    lateinit var userAdapter: NewUserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userDao =
            Room.databaseBuilder(applicationContext, AppDatabase::class.java, "aican-database")
                .build().userDao()

        userList.add(
            UserEntity(
                "v",
                "Vishal Anand",
                "v",
                "Supervisor",
                Date().date.toString(),
                Date().date.toString(),
                Date().date.toString(),
                true
            )
        )


    }

    private fun loadUsers() {
        GlobalScope.launch(Dispatchers.Main) {
            // Perform database operation to get users in the IO context
            val users = withContext(Dispatchers.IO) {
                userDao.getAllUsers()
            }

            if (users!!.isNotEmpty()) {
                binding.noUsersText.visibility = View.GONE
                binding.userDatabaseRecyclerView.visibility = View.VISIBLE
            }




            userAdapter = NewUserAdapter(users!!)

//        binding.userDatabaseRecyclerView.layoutManager = LinearLayoutManager(this)
            binding.userDatabaseRecyclerView.adapter = userAdapter

            binding.addUsersbtn.setOnClickListener {
                startActivity(Intent(this@AllUsers, AddNewUser::class.java))
            }
        }
    }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch(Dispatchers.IO) {

            loadUsers()
        }

    }
}