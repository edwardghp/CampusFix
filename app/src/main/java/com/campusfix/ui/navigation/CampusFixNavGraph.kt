package com.campusfix.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.campusfix.feature.auth.LoginScreen
import com.campusfix.feature.auth.RegisterScreen
import com.campusfix.feature.home.HomeScreen
import com.campusfix.feature.profile.ProfileScreen
import com.campusfix.feature.report.QrScanScreen
import com.campusfix.feature.report.ReportScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun CampusFixNavGraph() {
    val navController = rememberNavController()
    // Si ya hay sesion activa (HU01: sesion persistente), iniciamos en Home
    val start = if (FirebaseAuth.getInstance().currentUser != null) Routes.HOME else Routes.LOGIN

    NavHost(navController = navController, startDestination = start) {

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onGoToRegister = { navController.navigate(Routes.REGISTER) },
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegistered = {
                    navController.navigate(Routes.PROFILE) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                onSaved = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.PROFILE) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onReportFault = { navController.navigate(Routes.QR_SCAN) },
                onEditProfile = { navController.navigate(Routes.PROFILE) },
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0)
                    }
                },
            )
        }

        composable(Routes.QR_SCAN) {
            QrScanScreen(
                onAulaDetected = { aulaId ->
                    navController.navigate(Routes.report(aulaId)) {
                        popUpTo(Routes.QR_SCAN) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.REPORT,
            arguments = listOf(navArgument("aulaId") { type = NavType.StringType }),
        ) {
            ReportScreen(
                onTicketSent = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
