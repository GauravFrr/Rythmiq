package com.premium.spotifyclone.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home_tab")
    object HotAndNew : Screen("hot_and_new_tab")
    object Charts : Screen("charts_tab")
    object Library : Screen("library_tab")
    object Search : Screen("search_screen")
}
