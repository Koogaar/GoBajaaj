package com.example

import android.os.Bundle
import java.io.File
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.somalitraffic.ui.SomaliTrafficViewModel
import com.example.somalitraffic.ui.navigation.SomaliTrafficNavHost
import com.example.ui.theme.GoBajaajTheme

class MainActivity : ComponentActivity() {
    private val viewModel: SomaliTrafficViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GoBajaajTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SomaliTrafficNavHost(viewModel = viewModel)
                }
            }
        }
    }
}
