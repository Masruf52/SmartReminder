package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Calendar : Screen("calendar")
    object ChooseAddMethod : Screen("choose_add_method")
    object AddEditReminder : Screen("add_edit_reminder?id={id}") {
        fun createRoute(id: Long = -1L) = "add_edit_reminder?id=$id"
    }
    object UploadReminder : Screen("upload_reminder")
    object OcrReview : Screen("ocr_review")
    object AlarmFullScreen : Screen("alarm_full_screen?id={id}") {
        fun createRoute(id: Long) = "alarm_full_screen?id=$id"
    }
    object Statistics : Screen("statistics")
    object Settings : Screen("settings")
}
