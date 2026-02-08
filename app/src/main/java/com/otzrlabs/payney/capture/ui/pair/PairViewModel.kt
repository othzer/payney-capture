package com.otzrlabs.payney.capture.ui.pair

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otzrlabs.payney.capture.data.TokenStore
import com.otzrlabs.payney.capture.data.network.ApiClient
import com.otzrlabs.payney.capture.data.network.PairDeviceRequest
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class PairViewModel : ViewModel() {

    var code by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun onCodeChange(value: String) {
        code = value
        errorMessage = null
    }

    fun pair(onSuccess: () -> Unit) {
        val trimmedCode = code.trim()
        if (trimmedCode.isEmpty()) {
            errorMessage = "Enter the pairing code shown on the PayNey web app"
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = ApiClient.apiService.pairDevice(PairDeviceRequest(trimmedCode))
                TokenStore.saveToken(response.token)
                onSuccess()
            } catch (e: HttpException) {
                // The backend answers 404 for an unknown code and 410 for one
                // that's already been used or has expired.
                errorMessage = when (e.code()) {
                    400, 401, 404, 410 -> "Invalid or expired pairing code"
                    else -> "Something went wrong (HTTP ${e.code()}). Please try again."
                }
            } catch (e: IOException) {
                errorMessage = "Couldn't reach PayNey. Check your connection and try again."
            } finally {
                isLoading = false
            }
        }
    }
}
