package com.tradevision.ai.data.network

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("tradevision_session", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit().putString("jwt_token", token).apply()
    }

    fun getToken(): String? {
        return prefs.getString("jwt_token", null)
    }

    fun saveRole(role: String) {
        prefs.edit().putString("user_role", role).apply()
    }

    fun getRole(): String {
        return prefs.getString("user_role", "USER") ?: "USER"
    }

    fun saveUsername(username: String) {
        prefs.edit().putString("username", username).apply()
    }

    fun getUsername(): String {
        return prefs.getString("username", "") ?: ""
    }

    fun saveMainTf(tf: String) {
        prefs.edit().putString("main_tf", tf).apply()
    }

    fun getMainTf(): String {
        return prefs.getString("main_tf", "1h") ?: "1h"
    }

    fun saveConfirmTf(tf: String) {
        prefs.edit().putString("confirm_tf", tf).apply()
    }

    fun getConfirmTf(): String {
        return prefs.getString("confirm_tf", "4h") ?: "4h"
    }

    fun isLoggedIn(): Boolean {
        return !getToken().isNullOrEmpty()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
