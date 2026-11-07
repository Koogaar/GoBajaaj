package com.example.somalitraffic.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.somalitraffic.data.model.UserRole
import com.example.somalitraffic.ui.SomaliTrafficViewModel
import com.example.somalitraffic.ui.components.AutomaticLocationChecker
import com.example.somalitraffic.ui.screens.*

@Composable
fun SomaliTrafficNavHost(
    viewModel: SomaliTrafficViewModel,
    navController: NavHostController = rememberNavController()
) {
    // Automatically checks and alerts driver to enable Location / GPS if disabled
    AutomaticLocationChecker()
    val currentUser by viewModel.currentUser.collectAsState()
    val subscription by viewModel.subscription.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    // ---- 24h Driver Pass lockdown: ONLY Drivers are locked out once the free
    // 24-hour window (or their paid period) expires until a new pass is approved.
    // Passengers (macaamiisha) and Admins NEVER get locked by Driver Pass.
    // Drivers with a PENDING subscription have already submitted payment and are
    // waiting for admin approval — they must NOT be locked out during that wait.
    val role = currentUser?.role
    val isDriver = role == UserRole.DRIVER
    val subscriptionActive = viewModel.isSubscriptionActive(subscription)
    val subscriptionPending = subscription?.status?.lowercase() == "pending"
    val loggedIn = currentUser != null
    // Don't lock if: not a driver, not logged in, subscription is active, OR subscription is pending approval
    val locked = loggedIn && isDriver && !subscriptionActive && !subscriptionPending
    val topRoute = navBackStackEntry?.destination?.route
    val lockExempt = topRoute == null || topRoute == NavRoutes.SPLASH || topRoute == NavRoutes.ONBOARDING ||
        topRoute == NavRoutes.LOGIN || topRoute == NavRoutes.FORGOT_PASSWORD || topRoute == NavRoutes.SIGN_UP ||
        topRoute == NavRoutes.REGISTRATION_FORM || topRoute == NavRoutes.DRIVER_PASS ||
        topRoute == NavRoutes.ADMIN_DASHBOARD || topRoute == NavRoutes.REPORTER_DASHBOARD
    val showLock = locked && !lockExempt

    val safeNavigate: (String) -> Unit = { route ->
        navController.navigate(route)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = NavRoutes.SPLASH
        ) {
        // Screen 1: Splash
        composable(NavRoutes.SPLASH) {
            SplashScreen(
                viewModel = viewModel,
                onNavigateNext = { route ->
                    navController.navigate(route) {
                        popUpTo(NavRoutes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // Screen 2: Onboarding
        composable(NavRoutes.ONBOARDING) {
            OnboardingScreen(
                viewModel = viewModel,
                onFinishOnboarding = {
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        // Screen 3: Unified Single Login
        composable(NavRoutes.LOGIN) {
            LoginScreen(
                viewModel = viewModel,
                onNavigateToSignUp = { navController.navigate(NavRoutes.SIGN_UP) },
                onNavigateToForgotPassword = { navController.navigate(NavRoutes.FORGOT_PASSWORD) },
                onLoginSuccess = { role ->
                    val destination = when (role) {
                        UserRole.REPORTER -> NavRoutes.REPORTER_DASHBOARD
                        UserRole.SUPER_ADMIN,
                        UserRole.USER_MANAGER,
                        UserRole.SUBSCRIPTION_MANAGER,
                        UserRole.TRAFFIC_MANAGER,
                        UserRole.REPORTER_MANAGER,
                        UserRole.SUPPORT_AGENT -> NavRoutes.ADMIN_DASHBOARD
                        UserRole.DRIVER -> if (viewModel.isSubscriptionActive()) NavRoutes.HOME else NavRoutes.DRIVER_PASS
                        UserRole.PASSENGER -> NavRoutes.RIDE
                        else -> NavRoutes.RIDE
                    }
                    navController.navigate(destination) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        // Screen 3a: Dedicated Forgot Password Screen
        composable(NavRoutes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onResetSuccess = { role ->
                    val destination = when (role) {
                        UserRole.REPORTER -> NavRoutes.REPORTER_DASHBOARD
                        UserRole.SUPER_ADMIN,
                        UserRole.USER_MANAGER,
                        UserRole.SUBSCRIPTION_MANAGER,
                        UserRole.TRAFFIC_MANAGER,
                        UserRole.REPORTER_MANAGER,
                        UserRole.SUPPORT_AGENT -> NavRoutes.ADMIN_DASHBOARD
                        UserRole.DRIVER -> if (viewModel.isSubscriptionActive()) NavRoutes.HOME else NavRoutes.DRIVER_PASS
                        UserRole.PASSENGER -> NavRoutes.RIDE
                        else -> NavRoutes.RIDE
                    }
                    navController.navigate(destination) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        // Screen 3b: Role Selection
        composable(NavRoutes.SIGN_UP) {
            RoleSelectionScreen(
                onBackClick = { navController.popBackStack() },
                onRoleSelected = { role ->
                    navController.navigate(NavRoutes.registrationForm(role))
                }
            )
        }

        // Screen 4: Sign Up (Registration Form)
        composable(NavRoutes.REGISTRATION_FORM) { backStackEntry ->
            val roleArg = backStackEntry.arguments?.getString("role") ?: "driver"
            val isDriver = roleArg == "driver" || roleArg == "true"
            
            SignUpScreen(
                viewModel = viewModel,
                initialRole = roleArg,
                initialIsDriver = isDriver,
                onBackClick = { navController.popBackStack() },
                onSignUpSuccess = {
                    val user = viewModel.currentUser.value
                    val currentRole = user?.role ?: if (isDriver) UserRole.DRIVER else UserRole.PASSENGER
                    when (currentRole) {
                        UserRole.DRIVER -> {
                            navController.navigate(NavRoutes.DRIVER_PASS) {
                                popUpTo(NavRoutes.REGISTRATION_FORM) { inclusive = true }
                            }
                        }
                        UserRole.COURIER -> {
                            navController.navigate(NavRoutes.HOME) {
                                popUpTo(NavRoutes.REGISTRATION_FORM) { inclusive = true }
                            }
                        }
                        else -> {
                            navController.navigate(NavRoutes.RIDE) {
                                popUpTo(NavRoutes.REGISTRATION_FORM) { inclusive = true }
                            }
                        }
                    }
                }
            )
        }

        // Screen 5: Driver Pass ($0.25/maalinle)
        composable(NavRoutes.DRIVER_PASS) {
            DriverPassScreen(
                viewModel = viewModel,
                onPaymentSuccess = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.DRIVER_PASS) { inclusive = true }
                    }
                },
                onBackClick = {
                    if (!navController.popBackStack()) {
                        navController.navigate(NavRoutes.HOME) {
                            popUpTo(NavRoutes.DRIVER_PASS) { inclusive = true }
                        }
                    }
                }
            )
        }

        // Screen 6: Home Dashboard
        composable(NavRoutes.HOME) {
            HomeDashboardScreen(
                viewModel = viewModel,
                onNavigate = { route -> safeNavigate(route) }
            )
        }

        // Screen 7: Live Traffic Map
        composable(NavRoutes.LIVE_TRAFFIC) {
            LiveTrafficMapScreen(
                viewModel = viewModel,
                onNavigate = { route -> safeNavigate(route) },
                onIntersectionClick = { id ->
                    safeNavigate(NavRoutes.trafficDetails(id))
                }
            )
        }

        // Screen 7b: GoBajaaj Ride (Uber-like)
        composable(NavRoutes.RIDE) {
            BajajRideScreen(
                viewModel = viewModel,
                onNavigate = { route -> safeNavigate(route) }
            )
        }

        // Screen 8: Traffic Details
        composable(NavRoutes.TRAFFIC_DETAILS) { backStackEntry ->
            val intersectionId = backStackEntry.arguments?.getString("intersectionId") ?: "int-1"
            TrafficDetailsScreen(
                intersectionId = intersectionId,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onNavigateToAlternativeRoutes = { navController.navigate(NavRoutes.ALTERNATIVE_ROUTES) }
            )
        }

        // Screen 9: Alternative Routes
        composable(NavRoutes.ALTERNATIVE_ROUTES) {
            AlternativeRouteScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Screen 10: Traffic Alerts
        composable(NavRoutes.TRAFFIC_ALERTS) {
            TrafficAlertsScreen(
                viewModel = viewModel,
                onNavigate = { route -> safeNavigate(route) }
            )
        }

        // Screen 11: Traffic History
        composable(NavRoutes.TRAFFIC_HISTORY) {
            TrafficHistoryScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Screen 11a: Driver Finances (Buugga Dhaqaalaha)
        composable(NavRoutes.DRIVER_FINANCES) {
            DriverFinancesScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Screen 12: Profile & Settings
        composable(NavRoutes.PROFILE_SETTINGS) {
            ProfileSettingsScreen(
                viewModel = viewModel,
                onNavigate = { route -> safeNavigate(route) },
                onLogout = {
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Screen 19: Reporter Dashboard
        composable(NavRoutes.REPORTER_DASHBOARD) {
            ReporterDashboardScreen(
                viewModel = viewModel,
                onLogout = {
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Screen 20: Admin Dashboard
        composable(NavRoutes.ADMIN_DASHBOARD) {
            AdminDashboardScreen(
                viewModel = viewModel,
                onLogout = {
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Support Center Screen
        composable(NavRoutes.SUPPORT_CENTER) {
            SupportCenterScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }

    if (showLock) {
        DriverPassLockScreen(
            viewModel = viewModel,
            onPayNow = {
                try {
                    navController.navigate(NavRoutes.DRIVER_PASS) {
                        popUpTo(NavRoutes.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SomaliTrafficNavHost", "Navigate to DRIVER_PASS error", e)
                }
            },
            onCheckAgain = {
                viewModel.syncCurrentSubscription()
            },
            onLogout = {
                viewModel.logout {
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        )
    }
}
}
