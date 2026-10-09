package com.jovinyap.productchallenge

import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Our Material theme is light even when the phone uses dark mode.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT))
        // ViewModelProvider retains the instance on rotation, avoiding another catalogue request.
        val factory = viewModelFactory {
            initializer { ProductListViewModel((application as ProductApplication).productRepository) }
        }
        val viewModel = ViewModelProvider(this, factory)[ProductListViewModel::class.java]
        setContent {
            MaterialTheme { ProductListRoute(viewModel) }
        }
    }
}
