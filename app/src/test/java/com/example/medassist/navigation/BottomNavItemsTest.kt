package com.example.medassist.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class BottomNavItemsTest {

    @Test
    fun tabsFollowRequestedOrderAndExcludeSteps() {
        assertEquals(
            listOf(
                Screen.Home.route,
                Screen.Vault.route,
                Screen.Assistant.route,
                Screen.Analyzer.route,
                Screen.Profile.route,
            ),
            bottomNavItems.map { it.screen.route },
        )
    }
}
