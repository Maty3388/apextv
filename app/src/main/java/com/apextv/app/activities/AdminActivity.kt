package com.apextv.app.activities

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.apextv.app.R
import com.apextv.app.services.ApiService
import com.apextv.app.utils.Prefs
import kotlinx.coroutines.*
import org.json.JSONObject

class AdminActivity : AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var currentTab = "users"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        setupTabs()
        loadStats()
        loadUsers()

        findViewById<View>(R.id.btnBack)?.setOnClickListener { finish() }
    }

    private fun setupTabs() {
        findViewById<View>(R.id.tabUsers)?.setOnClickListener { switchTab("users") }
        findViewById<View>(R.id.tabCredits)?.setOnClickListener { switchTab("credits") }
        findViewById<View>(R.id.tabStats)?.setOnClickListener { switchTab("stats") }
        switchTab("users")
    }

    private fun switchTab(tab: String) {
        currentTab = tab
        listOf("users", "credits", "stats").forEach { t ->
            val btn = when(t) {
                "users" -> findViewById<TextView>(R.id.tabUsers)
                "credits" -> findViewById<TextView>(R.id.tabCredits)
                else -> findViewById<TextView>(R.id.tabStats)
            }
            val panel = when(t) {
                "users" -> findViewById<View>(R.id.panelUsers)
                "credits" -> findViewById<View>(R.id.panelCredits)
                else -> findViewById<View>(R.id.panelStats)
            }
            val active = t == tab
            btn?.setTextColor(if (active) getColor(R.color.background) else getColor(R.color.text_secondary))
            btn?.setBackgroundColor(if (active) getColor(R.color.primary) else getColor(R.color.surface2))
            panel?.visibility = if (active) View.VISIBLE else View.GONE
        }
        when(tab) {
            "users" -> loadUsers()
            "credits" -> loadCredits()
            "stats" -> loadStats()
        }
    }

    private fun loadStats() {
        scope.launch {
            try {
                val stats = withContext(Dispatchers.IO) { ApiService.getAdminStats() }
                findViewById<TextView>(R.id.tvStatTotal)?.text = "Total: ${stats.optInt("total")}"
                findViewById<TextView>(R.id.tvStatActive)?.text = "Activos: ${stats.optInt("active")}"
                findViewById<TextView>(R.id.tvStatDemos)?.text = "Demos: ${stats.optInt("demos")}"
                findViewById<TextView>(R.id.tvStatClients)?.text = "Clientes: ${stats.optInt("clients")}"
            } catch (_: Exception) {}
        }
    }

    private fun loadCredits() {
        scope.launch {
            try {
                val data = withContext(Dispatchers.IO) { ApiService.getAdminCredits() }
                findViewById<TextView>(R.id.tvMyCredits)?.text = "Mis créditos: ${data.optInt("credits")}"
                findViewById<TextView>(R.id.tvMyRole)?.text = "Rol: ${data.optString("role")}"
            } catch (_: Exception) {}
        }
    }

    private fun loadUsers() {
        scope.launch {
            try {
                val users = withContext(Dispatchers.IO) { ApiService.getAdminUsers() }
                val rv = findViewById<RecyclerView>(R.id.rvUsers)
                rv?.layoutManager = LinearLayoutManager(this@AdminActivity)
                rv?.adapter = UserAdapter(users,
                    onEdit = { user -> showEditDialog(user) },
                    onDelete = { user -> confirmDelete(user) },
                    onRenew = { user -> showRenewDialog(user) }
                )
            } catch (_: Exception) {}
        }
    }

    fun showCreateUserDialog() {
        val dp = resources.displayMetrics.density
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16*dp).toInt(), (16*dp).toInt(), (16*dp).toInt(), (8*dp).toInt())
        }

        val etEmail = EditText(this).apply { hint = "Email"; textSize = 14f; setTextColor(0xFFE8E8E8.toInt()); setHintTextColor(0xFF888899.toInt()) }
        val etPass = EditText(this).apply { hint = "Contraseña"; textSize = 14f; setTextColor(0xFFE8E8E8.toInt()); setHintTextColor(0xFF888899.toInt()); inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD }
        val etSubEnd = EditText(this).apply { hint = "Vencimiento (YYYY-MM-DD)"; textSize = 14f; setTextColor(0xFFE8E8E8.toInt()); setHintTextColor(0xFF888899.toInt()) }
        val etNotes = EditText(this).apply { hint = "Notas (opcional)"; textSize = 14f; setTextColor(0xFFE8E8E8.toInt()); setHintTextColor(0xFF888899.toInt()) }

        val roles = arrayOf("client", "demo", "reseller", "super_reseller", "distribuidor")
        val spinnerRole = Spinner(this).apply {
            adapter = ArrayAdapter(this@AdminActivity, android.R.layout.simple_spinner_dropdown_item, roles)
        }

        layout.addView(TextView(this).apply { text = "Tipo de cuenta"; setTextColor(0xFFC9A84C.toInt()); textSize = 12f })
        layout.addView(spinnerRole)
        layout.addView(etEmail)
        layout.addView(etPass)
        layout.addView(etSubEnd)
        layout.addView(etNotes)

        val tvError = TextView(this).apply { text = ""; setTextColor(0xFFFF4444.toInt()); textSize = 12f }
        layout.addView(tvError)

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("➕ Crear usuario")
            .setView(layout)
            .setPositiveButton("Crear", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(0xFF0F0F1A.toInt()))
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val email = etEmail.text.toString().trim()
                val pass = etPass.text.toString().trim()
                val role = roles[spinnerRole.selectedItemPosition]
                val subEnd = etSubEnd.text.toString().trim()
                val notes = etNotes.text.toString().trim()

                if (email.isEmpty() || pass.isEmpty()) { tvError.text = "Email y contraseña requeridos"; return@setOnClickListener }

                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled = false
                scope.launch {
                    try {
                        val res = withContext(Dispatchers.IO) {
                            ApiService.createAdminUser(email, pass, role, subEnd, notes)
                        }
                        if (res.optBoolean("success")) {
                            dialog.dismiss()
                            Toast.makeText(this@AdminActivity, "Usuario creado ✓", Toast.LENGTH_SHORT).show()
                            loadUsers()
                        } else {
                            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled = true
                            tvError.text = res.optString("error", "Error al crear")
                        }
                    } catch (_: Exception) {
                        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled = true
                        tvError.text = "Error de conexión"
                    }
                }
            }
        }
        dialog.show()
    }

    private fun showEditDialog(user: JSONObject) {
        val dp = resources.displayMetrics.density
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16*dp).toInt(), (16*dp).toInt(), (16*dp).toInt(), (8*dp).toInt())
        }
        val etSubEnd = EditText(this).apply {
            hint = "Vencimiento (YYYY-MM-DD)"
            setText(user.optString("subscription_end").take(10))
            textSize = 14f; setTextColor(0xFFE8E8E8.toInt()); setHintTextColor(0xFF888899.toInt())
        }
        val etNotes = EditText(this).apply {
            hint = "Notas"; setText(user.optString("notes"))
            textSize = 14f; setTextColor(0xFFE8E8E8.toInt()); setHintTextColor(0xFF888899.toInt())
        }
        val etPass = EditText(this).apply {
            hint = "Nueva contraseña (opcional)"
            textSize = 14f; setTextColor(0xFFE8E8E8.toInt()); setHintTextColor(0xFF888899.toInt())
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val switchBlocked = Switch(this).apply {
            text = "Bloqueado"; isChecked = user.optBoolean("blocked"); setTextColor(0xFFE8E8E8.toInt())
        }
        layout.addView(etSubEnd); layout.addView(etNotes); layout.addView(etPass); layout.addView(switchBlocked)

        android.app.AlertDialog.Builder(this)
            .setTitle("✏️ Editar: ${user.optString("email")}")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                scope.launch {
                    try {
                        val body = JSONObject()
                        body.put("subscription_end", etSubEnd.text.toString().trim())
                        body.put("notes", etNotes.text.toString().trim())
                        body.put("blocked", switchBlocked.isChecked)
                        if (etPass.text.isNotEmpty()) body.put("password", etPass.text.toString())
                        val res = withContext(Dispatchers.IO) { ApiService.editAdminUser(user.optString("_id"), body) }
                        if (res.optBoolean("success")) {
                            Toast.makeText(this@AdminActivity, "Guardado ✓", Toast.LENGTH_SHORT).show()
                            loadUsers()
                        }
                    } catch (_: Exception) {}
                }
            }
            .setNegativeButton("Cancelar", null)
            .create().also {
                it.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(0xFF0F0F1A.toInt()))
            }.show()
    }

    private fun showRenewDialog(user: JSONObject) {
        val options = arrayOf("30 días", "60 días", "90 días", "180 días", "365 días")
        val days = arrayOf(30, 60, 90, 180, 365)
        android.app.AlertDialog.Builder(this)
            .setTitle("🔄 Renovar: ${user.optString("email")}")
            .setItems(options) { _, which ->
                scope.launch {
                    try {
                        val res = withContext(Dispatchers.IO) { ApiService.renewAdminUser(user.optString("_id"), days[which]) }
                        if (res.optBoolean("success")) {
                            Toast.makeText(this@AdminActivity, "Renovado por ${days[which]} días ✓", Toast.LENGTH_SHORT).show()
                            loadUsers()
                        } else {
                            Toast.makeText(this@AdminActivity, res.optString("error"), Toast.LENGTH_SHORT).show()
                        }
                    } catch (_: Exception) {}
                }
            }.show()
    }

    private fun confirmDelete(user: JSONObject) {
        android.app.AlertDialog.Builder(this)
            .setTitle("🗑️ Eliminar usuario")
            .setMessage("¿Eliminar ${user.optString("email")}? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                scope.launch {
                    try {
                        val res = withContext(Dispatchers.IO) { ApiService.deleteAdminUser(user.optString("_id")) }
                        if (res.optBoolean("success")) {
                            Toast.makeText(this@AdminActivity, "Eliminado ✓", Toast.LENGTH_SHORT).show()
                            loadUsers()
                        }
                    } catch (_: Exception) {}
                }
            }
            .setNegativeButton("Cancelar", null).show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) { finish(); return true }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}

class UserAdapter(
    private val users: List<JSONObject>,
    private val onEdit: (JSONObject) -> Unit,
    private val onDelete: (JSONObject) -> Unit,
    private val onRenew: (JSONObject) -> Unit
) : RecyclerView.Adapter<UserAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvEmail: TextView = view.findViewById(R.id.tvUserEmail)
        val tvRole: TextView = view.findViewById(R.id.tvUserRole)
        val tvSubEnd: TextView = view.findViewById(R.id.tvUserSubEnd)
        val tvStatus: TextView = view.findViewById(R.id.tvUserStatus)
        val btnEdit: View = view.findViewById(R.id.btnEditUser)
        val btnDelete: View = view.findViewById(R.id.btnDeleteUser)
        val btnRenew: View = view.findViewById(R.id.btnRenewUser)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
        val v = android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val user = users[position]
        holder.tvEmail.text = user.optString("email")
        holder.tvRole.text = user.optString("role").uppercase()
        holder.tvSubEnd.text = user.optString("subscription_end").take(10).ifEmpty { "Sin fecha" }
        val blocked = user.optBoolean("blocked")
        holder.tvStatus.text = if (blocked) "🔴 Bloqueado" else "🟢 Activo"
        holder.tvStatus.setTextColor(if (blocked) 0xFFFF4444.toInt() else 0xFF44FF44.toInt())
        holder.btnEdit.setOnClickListener { onEdit(user) }
        holder.btnDelete.setOnClickListener { onDelete(user) }
        holder.btnRenew.setOnClickListener { onRenew(user) }
    }

    override fun getItemCount() = users.size
}
