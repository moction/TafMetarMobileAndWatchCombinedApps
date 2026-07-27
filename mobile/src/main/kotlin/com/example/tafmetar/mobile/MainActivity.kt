package com.example.tafmetar.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.tafmetar.mobile.ui.MainScreen
import com.example.tafmetar.mobile.ui.TafMetarTheme
import com.example.tafmetar.mobile.work.WorkScheduler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Planifie (ou confirme) le rafraîchissement périodique en arrière-plan.
        WorkScheduler.schedulePeriodicRefresh(applicationContext)

        setContent {
            TafMetarTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen()
                }
            }
        }
    }
}
