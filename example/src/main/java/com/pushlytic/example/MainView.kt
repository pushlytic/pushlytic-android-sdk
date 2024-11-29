package com.pushlytic.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pushlytic.sdk.Pushlytic

@Composable
fun MainView(viewModel: MainViewModel, paddingValues: PaddingValues) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "main",
        modifier = Modifier.padding(paddingValues)
    ) {
        composable("main") {
            MainContent(viewModel, navController)
        }
        composable("detail") {
            DetailView(viewModel)
        }
    }
}

@Composable
private fun MainContent(
    viewModel: MainViewModel,
    navController: NavController,
) {
    val connectionStatus by viewModel.connectionStatus.observeAsState("Disconnected")
    val userInfo by viewModel.userInfo.observeAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchEarlyLifecycleData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Connection Status: $connectionStatus",
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(16.dp)
        )

        userInfo?.let { user ->
            Text(
                text = "Welcome, ${user.firstName} ${user.lastName}!",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        Button(
            onClick = {
                // Track button interaction
                // Example event:
                // {
                //     "name": "main_button_tapped",
                //     "metadata": {
                //         "screen": "main"
                //     }
                // }
                Pushlytic.sendCustomEvent(
                    name = "main_button_tapped",
                    metadata = mapOf("screen" to "main")
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Send Custom Event")
        }

        Button(
            onClick = { navController.navigate("detail") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Go to Detail View")
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "SDK Status",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Connection: $connectionStatus")
                userInfo?.let { user ->
                    Text("User ID: ${user.userId}")
                    Text("Name: ${user.firstName} ${user.lastName}")
                    Text("Email: ${user.email}")
                    Text("Premium: ${if (user.premiumStatus) "Yes" else "No"}")
                }
            }
        }
    }
}