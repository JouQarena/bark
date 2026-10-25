package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.data.repository.SafirRepository
import com.example.ui.MainApp

class MainActivity : ComponentActivity() {
    private lateinit var repository: SafirRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        repository = SafirRepository(this)
        val initialInviteUrl = intent?.dataString

        setContent {
            MainApp(
                repository = repository,
                initialInviteLink = initialInviteUrl
            )
        }
    }
}
