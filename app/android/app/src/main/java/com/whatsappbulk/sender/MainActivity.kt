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
                    NavHost(navController = navController, startDestination = "login") {
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
                        composable("whatsapp") {
                            WhatsAppSessionScreen(onOpenCampaigns = { navController.navigate("campaignList") })
                        }
                        composable("campaignList") {
                            CampaignListScreen(
                                onCampaignClick = { campaign ->
                                    navController.navigate("campaignDetails/${campaign.id}")
                                },
                                onBackClick = { navController.popBackStack() }
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
                                }
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
