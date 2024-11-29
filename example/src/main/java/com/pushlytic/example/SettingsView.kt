package com.pushlytic.example

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pushlytic.sdk.Pushlytic

@Composable
fun SettingsView(viewModel: MainViewModel, paddingValues: PaddingValues) {
    var notifications by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Preferences",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Enable Notifications")
                    Switch(
                        checked = notifications,
                        onCheckedChange = { isChecked ->
                            notifications = isChecked
                            // Track notification preference changes
                            // Example event:
                            // {
                            //     "name": "notifications_toggled",
                            //     "metadata": {
                            //         "enabled": true/false
                            //     }
                            // }
                            Pushlytic.sendCustomEvent(
                                name = "notifications_toggled",
                                metadata = mapOf("enabled" to isChecked)
                            )
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.closeStream() },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Close Stream")
        }
    }
}