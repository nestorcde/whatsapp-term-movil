package com.whatsappbulk.sender

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.Modifier
import androidx.activity.compose.BackHandler
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.whatsappbulk.sender.ui.screens.splash.SplashScreen
import com.whatsappbulk.sender.ui.screens.vpn.VpnConnectionScreen
import com.whatsappbulk.sender.ui.screens.vpn.VpnInstallScreen
import com.whatsappbulk.sender.ui.screens.campaigns.CampaignDetailsScreen
import com.whatsappbulk.sender.ui.screens.campaigns.CampaignListScreen
import com.whatsappbulk.sender.ui.screens.login.LoginScreen
import com.whatsappbulk.sender.ui.screens.login.LoginViewModel
import com.whatsappbulk.sender.ui.screens.sending.SendingScreen
import com.whatsappbulk.sender.ui.screens.whatsapp.WhatsAppSessionScreen
import com.whatsappbulk.sender.ui.theme.WhatsAppBulkSenderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WhatsAppBulkSenderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // Double back to exit when no backstack
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()
                    val backPressedOnce = remember { mutableStateOf(false) }
                    BackHandler(enabled = navController.previousBackStackEntry == null) {
                        if (backPressedOnce.value) {
                            finish()
                        } else {
                            backPressedOnce.value = true
                            Toast.makeText(context, context.getString(R.string.press_back_again_to_exit), Toast.LENGTH_SHORT).show()
                            scope.launch {
                                delay(2000)
                                backPressedOnce.value = false
                            }
                        }
                    }

                    NavHost(navController = navController, startDestination = "splash") {
                        composable("splash") {
                            SplashScreen(
                                onNavigateToLogin = {
                                    navController.navigate("login") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                },
                                onNavigateToVpnConnection = {
                                    navController.navigate("vpn_connection")
                                },
                                onNavigateToVpnInstall = {
                                    navController.navigate("vpn_install")
                                }
                            )
                        }
                        composable("login") {
                            val loginViewModel: LoginViewModel = hiltViewModel()
                            val uiState by loginViewModel.uiState.collectAsStateWithLifecycle()
                            LoginScreen(
                                uiState = uiState,
                                onUsernameChange = loginViewModel::onUsernameChange,
                                onPasswordChange = loginViewModel::onPasswordChange,
                                onLoginClick = loginViewModel::login,
                                onNavigateToWhatsApp = { navController.navigate("whatsapp") }
                            )
                        }
                        composable("vpn_connection") {
                            VpnConnectionScreen(
                                onNavigateToLogin = {
                                    navController.navigate("login") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                },
                                onRequireInstall = {
                                    navController.navigate("vpn_install")
                                }
                            )
                        }
                        composable("vpn_install") {
                            VpnInstallScreen(
                                onInstallAndNavigate = {
                                    navController.navigate("vpn_connection") {
                                        popUpTo("vpn_install") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("whatsapp") {
                            WhatsAppSessionScreen(onOpenCampaigns = { navController.navigate("campaignList") })
                        }
                        composable("campaignList") {
                            CampaignListScreen(
                                onCampaignClick = { campaign ->
                                    navController.navigate("campaignDetails/${campaign.id}")
                                },
                                onBackClick = { navController.popBackStack() },
                                onRequireVpn = { navController.navigate("vpn_connection") }
                            )
                        }
                        composable(
                            route = "campaignDetails/{campaignId}",
                            arguments = listOf(navArgument("campaignId") { type = NavType.IntType })
                        ) {
                            CampaignDetailsScreen(
                                onBack = { navController.popBackStack() },
                                onStartSend = { campaignId, quantity ->
                                    navController.navigate("sending/${campaignId}?quantity=${quantity}")
                                },
                                onRequireVpn = { navController.navigate("vpn_connection") }
                            )
                        }
                        composable(
                            route = "sending/{campaignId}?quantity={quantity}",
                            arguments = listOf(
                                navArgument("campaignId") { type = NavType.IntType },
                                navArgument("quantity") { type = NavType.IntType; defaultValue = 0 }
                            )
                        ) {
                            SendingScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
