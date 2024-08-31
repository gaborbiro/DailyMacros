package dev.gaborbiro.nutrition.core.navigation


interface NavigationDispatcher {

    fun navigateTo(command: NavigationCommand)

    fun closeIfOnTop(feature: String)

    companion object {
        val DUMMY_IMPLEMENTATION = object : NavigationDispatcher {
            override fun navigateTo(command: NavigationCommand) {}
            override fun closeIfOnTop(feature: String) {}
        }
    }
}