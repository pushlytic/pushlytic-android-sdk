package com.pushlytic.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    val viewModel: MainViewModel = viewModel()
                    RootView(viewModel)
                }
            }
        }
    }
}

@Composable
fun RootView(viewModel: MainViewModel) {
    val isStreamOpen by viewModel.isStreamOpen.observeAsState(false)
    val showingMessage by viewModel.showingMessage.observeAsState(false)
    val currentMessage by viewModel.currentMessage.observeAsState()
    val navController = rememberNavController()

    LaunchedEffect(isStreamOpen) {
        if (!isStreamOpen) {
            navController.navigate("open_stream") {
                popUpTo(0) { inclusive = true }
            }
        } else {
            navController.navigate("main_tabs") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = if (isStreamOpen) "main_tabs" else "open_stream"
        ) {
            composable("open_stream") {
                OpenStreamView(viewModel)
            }
            composable("main_tabs") {
                MainTabView(viewModel)
            }
        }
    }

    if (showingMessage && currentMessage != null) {
        MessageDialog(
            message = currentMessage!!,
            onDismiss = { viewModel.dismissMessage() }
        )
    }
}

@Composable
fun OpenStreamView(viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to Pushlytic",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { viewModel.openStream() }
        ) {
            Text("Open Stream")
        }
    }
}

@Composable
fun MessageDialog(message: MessageData, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Message") },
        text = {
            Column {
                Text("Name: ${message.marketing.name}")
                Text("Email: ${message.marketing.email}")
                Text("Message: ${message.marketing.message}")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}