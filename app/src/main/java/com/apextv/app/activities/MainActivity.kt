package com.apextv.app.activities

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import com.apextv.app.BuildConfig
import com.apextv.app.R
import com.apextv.app.databinding.ActivityMainBinding
import com.apextv.app.fragments.MainFragment
import com.apextv.app.activities.VodActivity
import com.apextv.app.activities.AccountActivity
import com.apextv.app.services.ApiService
import com.apextv.app.utils.AutoUpdater
import com.apextv.app.utils.DeviceUtils
import com.apextv.app.utils.Prefs
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var mainFragment: MainFragment? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var isMobile = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiService.appContext = applicationContext
        isMobile = !DeviceUtils.isTV(this)

        if (isMobile) {
            setContentView(R.layout.activity_main_mobile)
            mainFragment = MainFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.mainContainer, mainFragment!!).commit()
            setupMobileNav()
            checkUpdate()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mainFragment = MainFragment()
        mainFragment!!.onChannelsLoaded = {
            binding.shimmerLayout.stopShimmer()
            binding.shimmerLayout.visibility = View.GONE
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.mainContainer, mainFragment!!).commit()
        binding.shimmerLayout.startShimmer()
        binding.tvUserEmail.text = "👤 " + Prefs.getEmail(this)
        val subEnd = Prefs.getSubEnd(this)
        if (subEnd.isNotEmpty()) binding.tvVencimiento.text = subEnd

        setupSidebar()
        setupNavigation()
        checkUpdate()
    }

    private fun setupMobileNav() {
        findViewById<View>(R.id.navInicio).setOnClickListener {
            mainFragment?.filterCategory(null)
        }
        findViewById<View>(R.id.navPeliculas).setOnClickListener {
            val i2 = Intent(this, VodActivity::class.java)
            i2.putExtra(VodActivity.EXTRA_TYPE, VodActivity.TYPE_MOVIES)
            startActivity(i2)
        }
        findViewById<View>(R.id.navSeries).setOnClickListener {
            val i2 = Intent(this, VodActivity::class.java)
            i2.putExtra(VodActivity.EXTRA_TYPE, VodActivity.TYPE_SERIES)
            startActivity(i2)
        }
        findViewById<View>(R.id.navBuscar).setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        findViewById<View>(R.id.navMas).setOnClickListener { showMoreMenu() }
    }

    private fun showMoreMenu() {
        val role = Prefs.getRole(this)
        val isAdmin = role in listOf("admin", "distribuidor", "super_reseller", "reseller")
        val options = if (isAdmin)
            arrayOf("👤 Mi Cuenta", "🔒 Adultos", "⭐ Favoritos", "🗑️ Borrar Caché", "⚙️ Panel Admin", "🚪 Cerrar Sesión")
        else
            arrayOf("👤 Mi Cuenta", "🔒 Adultos", "⭐ Favoritos", "🗑️ Borrar Caché", "🚪 Cerrar Sesión")

        android.app.AlertDialog.Builder(this)
            .setTitle("Más opciones")
            .setItems(options) { _, which ->
                if (isAdmin) {
                    when (which) {
                        0 -> startActivity(Intent(this, AccountActivity::class.java))
                        1 -> showPinDialog { mainFragment?.filterCategory("ADULTOS") }
                        2 -> mainFragment?.loadFavorites()
                        3 -> { cacheDir.listFiles()?.forEach { it.deleteRecursively() }
                               Toast.makeText(this, "Caché borrado", Toast.LENGTH_SHORT).show() }
                        4 -> startActivity(Intent(this, AdminActivity::class.java))
                        5 -> { Prefs.logout(this)
                               startActivity(Intent(this, LoginActivity::class.java))
                               finishAffinity() }
                    }
                } else {
                    when (which) {
                        0 -> startActivity(Intent(this, AccountActivity::class.java))
                        1 -> showPinDialog { mainFragment?.filterCategory("ADULTOS") }
                        2 -> mainFragment?.loadFavorites()
                        3 -> { cacheDir.listFiles()?.forEach { it.deleteRecursively() }
                               Toast.makeText(this, "Caché borrado", Toast.LENGTH_SHORT).show() }
                        4 -> { Prefs.logout(this)
                               startActivity(Intent(this, LoginActivity::class.java))
                               finishAffinity() }
                    }
                }
            }.show()
    }

    private fun setupSidebar() {
        binding.btnTv.setOnClickListener { mainFragment?.filterCategory(null) }
        binding.btnPeliculas.setOnClickListener {
            val i = Intent(this, VodActivity::class.java)
            i.putExtra(VodActivity.EXTRA_TYPE, VodActivity.TYPE_MOVIES)
            startActivity(i)
        }
        binding.btnSeries.setOnClickListener {
            val i = Intent(this, VodActivity::class.java)
            i.putExtra(VodActivity.EXTRA_TYPE, VodActivity.TYPE_SERIES)
            startActivity(i)
        }
        binding.btnAdultos.setOnClickListener { showPinDialog { mainFragment?.filterCategory("ADULTOS") } }
        binding.btnBuscar.setOnClickListener { startActivity(Intent(this, SearchActivity::class.java)) }
        binding.btnFavoritos.setOnClickListener { mainFragment?.loadFavorites() }
        binding.btnClearCache.setOnClickListener {
            cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            Toast.makeText(this, "Caché borrado", Toast.LENGTH_SHORT).show()
        }
        binding.btnLogout.setOnClickListener {
            Prefs.logout(this)
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
        binding.btnMiCuenta.setOnClickListener {
            startActivity(Intent(this, AccountActivity::class.java))
        }
    }

    private fun setupNavigation() {
        val items = listOf(binding.btnTv, binding.btnPeliculas, binding.btnSeries,
            binding.btnAdultos, binding.btnBuscar, binding.btnFavoritos,
            binding.btnClearCache, binding.btnLogout)

        items.forEach { btn ->
            btn.setOnFocusChangeListener { v, focused ->
                v.setBackgroundColor(if (focused) getColor(R.color.surface2) else android.graphics.Color.TRANSPARENT)
                val tv = (v as? android.view.ViewGroup)?.getChildAt(1) as? android.widget.TextView
                val iv = (v as? android.view.ViewGroup)?.getChildAt(0) as? android.widget.ImageView
                tv?.setTextColor(if (focused) getColor(R.color.primary) else getColor(R.color.text_primary))
                iv?.setColorFilter(if (focused) getColor(R.color.primary) else getColor(R.color.text_secondary))
                if (focused) v.animate().translationX(4f).setDuration(100).start()
                else v.animate().translationX(0f).setDuration(100).start()
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                android.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle("Salir").setMessage("¿Querés salir de ApexTV?")
                    .setPositiveButton("Salir") { _, _ -> finish() }
                    .setNegativeButton("Cancelar", null).show()
            }
        })
    }

    private fun showPinDialog(onSuccess: () -> Unit) {
        val dp = resources.displayMetrics.density
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding((24*dp).toInt(), (24*dp).toInt(), (24*dp).toInt(), (8*dp).toInt())
        }
        val input = android.widget.EditText(this).apply {
            hint = "Ingresá el PIN"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            gravity = android.view.Gravity.CENTER
            textSize = 24f
            setTextColor(android.graphics.Color.WHITE)
            setHintTextColor(android.graphics.Color.GRAY)
        }
        val tvError = android.widget.TextView(this).apply {
            text = ""; textSize = 12f
            setTextColor(android.graphics.Color.RED)
            gravity = android.view.Gravity.CENTER
        }
        layout.addView(input); layout.addView(tvError)
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("🔒 Control Parental")
            .setView(layout)
            .setPositiveButton("Confirmar", null)
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val pin = input.text.toString()
                if (pin.length < 4) { tvError.text = "El PIN debe tener al menos 4 dígitos"; return@setOnClickListener }
                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled = false
                scope.launch {
                    try {
                        val res = withContext(Dispatchers.IO) { ApiService.verifyParentalPin(pin) }
                        if (res.optBoolean("success")) {
                            dialog.dismiss(); onSuccess()
                        } else {
                            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled = true
                            tvError.text = res.optString("error", "PIN incorrecto")
                            input.text.clear()
                        }
                    } catch (_: Exception) {
                        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled = true
                        tvError.text = "Error de conexión"
                    }
                }
            }
        }
        dialog.show(); input.requestFocus()
    }

    private fun checkUpdate() {
        scope.launch {
            try {
                val ver = withContext(Dispatchers.IO) { ApiService.getVersion() }
                if (ver != null) AutoUpdater.check(this@MainActivity, BuildConfig.VERSION_NAME, ver)
            } catch (_: Exception) {}
        }
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}
