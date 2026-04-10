package com.dishrecognition.ui.navigation

sealed class Screen(val route: String) {
    data object Camera : Screen("camera")
    data object DishList : Screen("dish_list")
    data object AddDish : Screen("add_dish")
    data object Nutrition : Screen("nutrition")
}
