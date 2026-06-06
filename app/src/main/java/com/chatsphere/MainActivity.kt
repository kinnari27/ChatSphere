package com.chatsphere

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.chatsphere.presentation.navigation.ChatSphereNavHost
import com.chatsphere.presentation.theme.ChatSphereTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatSphereTheme {
                Surface {
                    ChatSphereNavHost()
                }
            }
        }
    }
}
