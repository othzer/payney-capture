package com.otzrlabs.payney.capture.ui.pair

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.otzrlabs.payney.capture.ui.theme.PayNeyColors

@Composable
fun PairScreen(
    onPaired: () -> Unit,
    viewModel: PairViewModel = viewModel(),
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "PayNey Capture",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Pair this device with your PayNey account to start capturing bank SMS, UPI notifications, and receipts.",
                style = MaterialTheme.typography.bodyMedium,
                color = PayNeyColors.OnSurfaceMuted,
                textAlign = TextAlign.Center,
            )

            OutlinedTextField(
                value = viewModel.code,
                onValueChange = viewModel::onCodeChange,
                label = { Text("Pairing code") },
                singleLine = true,
                enabled = !viewModel.isLoading,
                isError = viewModel.errorMessage != null,
                modifier = Modifier.fillMaxWidth(),
            )

            viewModel.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }

            Button(
                onClick = { viewModel.pair(onPaired) },
                enabled = !viewModel.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Pair")
                }
            }
        }
    }
}
