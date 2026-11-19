package com.hereapp.messenger

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hereapp.messenger.core.auth.FirebaseAuthRepository
import com.hereapp.messenger.core.messaging.UnprovisionedMessagingRepository
import com.hereapp.messenger.core.notifications.MessageNotificationChannels
import com.hereapp.messenger.core.preferences.AppPreferencesRepository
import com.hereapp.messenger.ui.AppViewModel
import com.hereapp.messenger.ui.HereAppRoot
import com.hereapp.messenger.ui.auth.AuthViewModel
import com.hereapp.messenger.ui.messaging.MessagingViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        MessageNotificationChannels.ensureChannelsCreated(applicationContext)
        val preferencesRepository = AppPreferencesRepository(applicationContext)
        val authRepository = FirebaseAuthRepository(applicationContext)
        val messagingRepository = UnprovisionedMessagingRepository()
        setContent {
            val appViewModel: AppViewModel = viewModel(
                factory = AppViewModel.Factory(preferencesRepository)
            )
            val authViewModel: AuthViewModel = viewModel(
                factory = AuthViewModel.Factory(authRepository)
            )
            val messagingViewModel: MessagingViewModel = viewModel(
                factory = MessagingViewModel.Factory(messagingRepository)
            )
            HereAppRoot(
                viewModel = appViewModel,
                authViewModel = authViewModel,
                messagingViewModel = messagingViewModel
            )
        }
    }
}
