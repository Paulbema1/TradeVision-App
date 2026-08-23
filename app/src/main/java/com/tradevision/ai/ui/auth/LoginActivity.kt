package com.tradevision.ai.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tradevision.ai.data.model.AuthRequest
import com.tradevision.ai.data.network.ApiClient
import com.tradevision.ai.data.network.SessionManager
import com.tradevision.ai.databinding.ActivityLoginBinding
import com.tradevision.ai.ui.main.MainActivity
import kotlinx.coroutines.launch
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        if (sessionManager.isLoggedIn()) {
            startMainActivity()
            return
        }

        binding.btnLogin.setOnClickListener { performAuth(isLogin = true) }
        binding.btnRegister.setOnClickListener { performAuth(isLogin = false) }
    }

    private fun performAuth(isLogin: Boolean) {
        val username = binding.etUsername.text?.toString()?.trim() ?: ""
        val password = binding.etPassword.text?.toString()?.trim() ?: ""

        if (username.length < 3) {
            Toast.makeText(this, "Le pseudo doit faire au moins 3 caractères", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Le mot de passe doit faire au moins 6 caractères", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false
        binding.btnRegister.isEnabled = false

        lifecycleScope.launch {
            try {
                val api = ApiClient.getApiService(this@LoginActivity)
                val request = AuthRequest(username, password)
                val response = if (isLogin) api.login(request) else api.register(request)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    sessionManager.saveToken(body.accessToken)
                    sessionManager.saveRole(body.role)
                    sessionManager.saveUsername(body.username)

                    val actionName = if (isLogin) "Connecté" else "Compte créé"
                    Toast.makeText(
                        this@LoginActivity,
                        "$actionName avec succès ! Rôle: ${body.role}",
                        Toast.LENGTH_SHORT
                    ).show()

                    startMainActivity()
                } else {
                    val rawError = response.errorBody()?.string() ?: ""
                    var errorDetail = "Erreur ${response.code()}"
                    try {
                        val json = JSONObject(rawError)
                        if (json.has("detail")) {
                            errorDetail = json.getString("detail")
                        }
                    } catch (e: Exception) {
                        if (rawError.isNotEmpty()) errorDetail = rawError
                    }
                    Toast.makeText(this@LoginActivity, errorDetail, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@LoginActivity,
                    "Erreur réseau : ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true
                binding.btnRegister.isEnabled = true
            }
        }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
