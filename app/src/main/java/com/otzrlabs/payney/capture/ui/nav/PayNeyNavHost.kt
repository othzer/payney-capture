package com.otzrlabs.payney.capture.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.otzrlabs.payney.capture.data.TokenStore
import com.otzrlabs.payney.capture.ui.pair.PairScreen
import com.otzrlabs.payney.capture.ui.receipt.ScanReceiptScreen
import com.otzrlabs.payney.capture.ui.status.StatusScreen

@Composable
fun PayNeyNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val startDestination = if (TokenStore.hasToken()) PayNeyDestinations.STATUS else PayNeyDestinations.PAIR

    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        composable(PayNeyDestinations.PAIR) {
            PairScreen(
                onPaired = {
                    navController.navigate(PayNeyDestinations.STATUS) {
                        popUpTo(PayNeyDestinations.PAIR) { inclusive = true }
                    }
                },
            )
        }
        composable(PayNeyDestinations.STATUS) {
            StatusScreen(
                onUnpaired = {
                    navController.navigate(PayNeyDestinations.PAIR) {
                        popUpTo(PayNeyDestinations.STATUS) { inclusive = true }
                    }
                },
                onScanReceipt = {
                    navController.navigate(PayNeyDestinations.SCAN_RECEIPT)
                },
            )
        }
        composable(PayNeyDestinations.SCAN_RECEIPT) {
            ScanReceiptScreen(
                onDone = { navController.popBackStack() },
            )
        }
    }
}
