package com.apextv.app.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.apextv.app.databinding.ActivityLoginBinding
import com.apextv.app.services.ApiService
import com.apextv.app.utils.Prefs
import kotlinx.coroutines.*

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnLogin.setOnClickListener { doLogin() }
    }

    private fun doLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        if (email.isEmpty() || password.isEmpty()) {
            showError("Completá todos los campos")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Ingresá un email válido")
            return
        }
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false
        binding.tvError.visibility = View.GONE
        scope.launch {
            val token = withContext(Dispatchers.IO) {
                try { ApiService.login(email, password) } catch (_: Exception) { null }
            }
            if (isDestroyed) return@launch
            binding.progressBar.visibility = View.GONE
            binding.btnLogin.isEnabled = true
            if (token != null) {
                ApiService.token = token
                Prefs.saveToken(this@LoginActivity, token)
                Prefs.saveEmail(this@LoginActivity, email)
                Prefs.saveSubEnd(this@LoginActivity, ApiService.subEnd)
                Prefs.saveRole(this@LoginActivity, ApiService.userRole)
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            } else {
                showError(when {
                    ApiService.loginError.contains("vencida", ignoreCase = true) -> "Tu suscripción ha vencido"
                    ApiService.loginError.contains("bloqueado", ignoreCase = true) -> "Tu cuenta ha sido bloqueada"
                    else -> "Credenciales incorrectas"
                })
            }
        }
    }

    private fun showError(msg: String) {
        binding.tvError.text = msg
        binding.tvError.visibility = View.VISIBLE
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}
