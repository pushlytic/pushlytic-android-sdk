package com.pushlytic.example

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pushlytic.sdk.Pushlytic

@Composable
fun DetailView(viewModel: MainViewModel) {
    LaunchedEffect(Unit) {
        // Track view lifecycle events
        // Example event:
        // {
        //     "name": "detail_view_opened",
        //     "metadata": {}
        // }
        Pushlytic.sendCustomEvent(
            name = "detail_view_opened",
            metadata = emptyMap()
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = "Detail View",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        Button(
            onClick = {
                // Track user interactions with custom events
                // Example event:
                // {
                //     "name": "detail_action_performed",
                //     "metadata": {
                //         "action": "button_tap"
                //     }
                // }
                Pushlytic.sendCustomEvent(
                    name = "detail_action_performed",
                    metadata = mapOf("action" to "button_tap")
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Perform Action")
        }

        Button(
            onClick = { updatePartialMetadata() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Update Some Metadata")
        }

        Button(
            onClick = {
                // Clears all metadata associated with the current connection
                Pushlytic.clearMetadata()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Clear All Metadata")
        }
    }
}

// Updates specific metadata fields without affecting other existing metadata
//
// Pushlytic.setMetadata performs an upsert operation:
// - Existing fields are updated with new values
// - New fields are added
// - Unmentioned fields remain unchanged
//
// Note: To completely reset metadata before setting new values,
// first call Pushlytic.clearMetadata() followed by setMetadata()
//
// Example metadata update:
// {
//     "last_name": "Smith",
//     "os_version": "17.0",
//     "device_info": {
//         "screen_size": "6.1 inch",
//         "brightness": 0.8
//     },
//     "new_top_level": "value"
// }
private fun updatePartialMetadata() {
    val updatedMetadata = mapOf(
        "last_name" to "Smith", // Updates existing field
        "os_version" to "17.0", // Updates existing field
        "device_info" to mapOf( // Adds new nested structure
            "screen_size" to "6.1 inch",
            "brightness" to 0.8
        ),
        "new_top_level" to "value" // Adds new field
    )

    // To completely replace metadata instead of upserting:
    // Pushlytic.clearMetadata()
    // Pushlytic.setMetadata(updatedMetadata)

    // Performs upsert - updates existing fields and adds new ones
    Pushlytic.setMetadata(updatedMetadata)
}