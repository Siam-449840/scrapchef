package com.scrapchef.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.scrapchef.android.ui.components.PantryBottomBar
import com.scrapchef.android.ui.screens.CountryLanguageScreen
import com.scrapchef.android.ui.screens.DietaryPreferencesScreen
import com.scrapchef.android.ui.screens.FavoritesScreen
import com.scrapchef.android.ui.screens.GeneratingScreen
import com.scrapchef.android.ui.screens.IngredientInputScreen
import com.scrapchef.android.ui.screens.KitchenModeScreen
import com.scrapchef.android.ui.screens.LandingScreen
import com.scrapchef.android.ui.screens.OnboardingScreen
import com.scrapchef.android.ui.screens.PantryAddScreen
import com.scrapchef.android.ui.screens.PantryScreen
import com.scrapchef.android.ui.screens.ProfileScreen

import com.scrapchef.android.ui.screens.SplashScreen
import com.scrapchef.android.ui.theme.ScrapChefTheme
import com.scrapchef.android.ui.theme.ScrapChefTransitions
import com.scrapchef.android.ui.viewmodel.PantryViewModel
import com.scrapchef.android.ui.viewmodel.RecipeViewModel
import com.scrapchef.android.ui.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScrapChefTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ScrapChefNavHost()
                }
            }
        }
    }
}

@Composable
fun ScrapChefNavHost() {
    val navController = rememberNavController()
    val viewModel: RecipeViewModel = viewModel()
    val pantryViewModel: PantryViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    // Only collect what's needed at the NavHost level: the favorites count for the bottom bar badge.
    // Individual screens collect their own uiState to avoid full-graph recomposition.
    val favoritesList by viewModel.parsedFavoritesFlow.collectAsState()
    val favoritesCount by remember { derivedStateOf { favoritesList.size } }
    
    val settingsUiState by settingsViewModel.uiState.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute by remember { derivedStateOf { navBackStackEntry?.destination?.route ?: "landing" } }

    val tabRoutes = remember { listOf("landing", "favorites", "pantry", "profile") }

    val bottomBarComposable: @Composable () -> Unit = {
        if (currentRoute in tabRoutes) {
            PantryBottomBar(
                currentRoute = currentRoute,
                favoritesCount = favoritesCount,
                onTabSelected = { targetRoute ->
                    navController.navigate(targetRoute) {
                        popUpTo("landing") {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }

    NavHost(
        navController = navController,
        startDestination = "splash",
        // Default transitions for all routes
        enterTransition = { ScrapChefTransitions.pushEnter },
        exitTransition = { ScrapChefTransitions.pushExit },
        popEnterTransition = { ScrapChefTransitions.popEnter },
        popExitTransition = { ScrapChefTransitions.popExit }
    ) {
        // Splash — no transition (it has its own animation)
        composable(
            route = "splash",
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None }
        ) {
            SplashScreen(
                onSplashFinished = {
                    val nextRoute = if (settingsUiState.isOnboardingCompleted) "landing" else "onboarding"
                    navController.navigate(nextRoute) {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        // Onboarding
        composable(
            route = "onboarding",
            enterTransition = { ScrapChefTransitions.pushEnter },
            exitTransition = { ScrapChefTransitions.pushExit }
        ) {
            OnboardingScreen(
                viewModel = settingsViewModel,
                onFinishOnboarding = {
                    navController.navigate("landing") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        // Tab destinations — crossfade instead of slide
        composable(
            route = "landing",
            enterTransition = { ScrapChefTransitions.tabEnter },
            exitTransition = { ScrapChefTransitions.tabExit }
        ) {
            val uiState by viewModel.uiState.collectAsState()
            LandingScreen(
                viewModel = viewModel,
                uiState = uiState,
                onNavigateToIngredientInput = {
                    navController.navigate("ingredient_input")
                },
                bottomBar = bottomBarComposable
            )
        }

        // Push destination — slide + fade
        composable(route = "ingredient_input") {
            val uiState by viewModel.uiState.collectAsState()
            IngredientInputScreen(
                viewModel = viewModel,
                uiState = uiState,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToRecipe = {
                    navController.navigate("generating")
                }
            )
        }

        // Generating / Recipe Detail — per §10.6, streaming assembles into the detail screen
        composable(route = "generating") {
            GeneratingScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onStartCooking = {
                    navController.navigate("kitchen_mode")
                },
                onNewRecipe = {
                    viewModel.resetSelections()
                    navController.navigate("landing") {
                        popUpTo("landing") { inclusive = true }
                    }
                }
            )
        }


        // Kitchen Mode Push
        composable(route = "kitchen_mode") {
            val uiState by viewModel.uiState.collectAsState()
            KitchenModeScreen(
                viewModel = viewModel,
                uiState = uiState,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Tab destination — crossfade
        composable(
            route = "favorites",
            enterTransition = { ScrapChefTransitions.tabEnter },
            exitTransition = { ScrapChefTransitions.tabExit }
        ) {
            FavoritesScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSelectFavorite = {
                    // Navigate to generating screen to view the favorite
                    navController.navigate("generating")
                },
                bottomBar = bottomBarComposable
            )
        }

        // Tab destination — crossfade
        composable(
            route = "pantry",
            enterTransition = { ScrapChefTransitions.tabEnter },
            exitTransition = { ScrapChefTransitions.tabExit }
        ) {
            PantryScreen(
                viewModel = pantryViewModel,
                onNavigateToAdd = { navController.navigate("pantry/add") },
                bottomBar = bottomBarComposable
            )
        }

        // Push destination
        composable(route = "pantry/add") {
            PantryAddScreen(
                viewModel = pantryViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Tab destination — crossfade
        composable(
            route = "profile",
            enterTransition = { ScrapChefTransitions.tabEnter },
            exitTransition = { ScrapChefTransitions.tabExit }
        ) {
            ProfileScreen(
                viewModel = settingsViewModel,
                onNavigateToDietaryPrefs = { navController.navigate("profile/dietary") },
                onNavigateToCountryLang = { navController.navigate("profile/country_lang") },
                bottomBar = bottomBarComposable
            )
        }

        // Push destination
        composable(route = "profile/dietary") {
            DietaryPreferencesScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Push destination
        composable(route = "profile/country_lang") {
            CountryLanguageScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
