package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.example.ui.DashboardScreen
import com.example.ui.JournalViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {
    private lateinit var viewModel: JournalViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        viewModel = ViewModelProvider(this)[JournalViewModel::class.java]
        
        enableEdgeToEdge()
        
        setContent {
            val settingsState by viewModel.settingsState.collectAsState(initial = null)
            val isDarkMode = settingsState?.isDarkMode == true
            MyApplicationTheme(darkTheme = isDarkMode) {
                DashboardScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Intercept application resume cycles and lock if security configurations exist
        if (::viewModel.isInitialized) {
            val settings = viewModel.settingsState.value
            if (settings != null && (settings.isBiometricEnabled || !settings.backupPinCode.isNullOrEmpty())) {
                viewModel.lock()
            }
        }
    }
}
