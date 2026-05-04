package com.bienhieu.chamcong.ui.navigation

/**
 * Type-safe navigation route definitions.
 * Prevents typo bugs from raw strings like "attendance", "employees".
 */
sealed class Route(val route: String) {
    data object Attendance : Route("attendance")
    data object Employees : Route("employees")
    data object FaceRegistration : Route("face_registration?employeeId={employeeId}") {
        fun createRoute(employeeId: String? = null): String {
            return if (employeeId != null) {
                "face_registration?employeeId=$employeeId"
            } else {
                "face_registration"
            }
        }
    }
}