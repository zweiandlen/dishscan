package com.dishrecognition.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dishrecognition.ui.add.AddDishScreen
import com.dishrecognition.ui.camera.CameraScreen
import com.dishrecognition.ui.dishlist.DishListScreen
import com.dishrecognition.ui.nutrition.NutritionScreen

data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Camera, Icons.Default.CameraAlt, "识别"),
    BottomNavItem(Screen.DishList, Icons.Default.List, "菜品"),
    BottomNavItem(Screen.AddDish, Icons.Default.Add, "添加"),
    BottomNavItem(Screen.Nutrition, Icons.Default.Restaurant, "营养")
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true,
                        onClick = {
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Camera.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Camera.route) {
                CameraScreen()
            }
            composable(Screen.DishList.route) {
                DishListScreen()
            }
            composable(Screen.AddDish.route) {
                AddDishScreen(
                    onDishAdded = {
                        navController.navigate(Screen.DishList.route) {
                            popUpTo(Screen.Camera.route)
                        }
                    }
                )
            }
            composable(Screen.Nutrition.route) {
                NutritionScreen()
            }
        }
    }
}
