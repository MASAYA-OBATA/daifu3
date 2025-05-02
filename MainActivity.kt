package com.example.daifu3.viewmodel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.daifu3.ui.GameScreen
import com.example.daifu3.viewmodel.GameViewModel
import com.example.daifu3.ui.theme.Daifu3Theme

class MainActivity : ComponentActivity() {
    private val viewModel = GameViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Daifu3Theme {
                GameScreen(viewModel = viewModel)
            }
        }
    }
}
