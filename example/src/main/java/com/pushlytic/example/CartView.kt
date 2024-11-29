package com.pushlytic.example

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pushlytic.sdk.Pushlytic

@Composable
fun CartView(viewModel: MainViewModel, paddingValues: PaddingValues) {
    var cartItems by remember { mutableStateOf(listOf("Item 1", "Item 2", "Item 3")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
    ) {
        Text(
            text = "Cart",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        cartItems.forEach { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item)
                    IconButton(
                        onClick = {
                            cartItems = cartItems.filter { it != item }
                            // Track cart item removal with remaining item count via Pushlytic
                            // Example event:
                            // {
                            //     "name": "cart_item_removed",
                            //     "metadata": {
                            //         "remaining_items": 2
                            //     }
                            // }
                            Pushlytic.sendCustomEvent(
                                name = "cart_item_removed",
                                metadata = mapOf("remaining_items" to cartItems.size)
                            )
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove item")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                // Track checkout initiation with total items in cart via Pushlytic
                // Example event:
                // {
                //     "name": "checkout_initiated",
                //     "metadata": {
                //         "items_count": 3
                //     }
                // }
                Pushlytic.sendCustomEvent(
                    name = "checkout_initiated",
                    metadata = mapOf("items_count" to cartItems.size)
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Checkout")
        }
    }
}