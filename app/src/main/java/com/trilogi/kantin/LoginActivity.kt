package com.trilogi.kantin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trilogi.kantin.ui.AppCard
import com.trilogi.kantin.ui.KantinTheme
import com.trilogi.kantin.util.AppConfig
import com.trilogi.kantin.util.Extras
import com.trilogi.kantin.util.SessionManager
import com.trilogi.kantin.util.Validators

class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Jika sudah pernah login (dan datanya masih valid), langsung ke beranda.
        val session = SessionManager(this)
        val saved = session.get()
        if (saved != null) {
            if (Validators.validateNama(saved.nama) == null && Validators.validateNim(saved.nim) == null) {
                openHome(saved.nama, saved.nim)
                return
            }
            session.clear()
        }

        setContent {
            KantinTheme {
                LoginScreen(
                    onLogin = { nama, nim ->
                        session.save(nama, nim)
                        openHome(nama, nim)
                    },
                    onStaffLogin = {
                        startActivity(Intent(this@LoginActivity, StaffActivity::class.java))
                    }
                )
            }
        }
    }

    /** Explicit Intent + kirim data (nama & NIM) ke layar berikutnya. */
    private fun openHome(nama: String, nim: String) {
        val intent = Intent(this, HomeActivity::class.java).apply {
            putExtra(Extras.NAMA, nama)
            putExtra(Extras.NIM, nim)
        }
        startActivity(intent)
        finish()
    }
}

@Composable
fun LoginScreen(onLogin: (String, String) -> Unit, onStaffLogin: () -> Unit) {
    // rememberSaveable: input tidak hilang saat layar diputar (Ephemeral State Loss)
    var nama by rememberSaveable { mutableStateOf("") }
    var nim by rememberSaveable { mutableStateOf("") }
    var namaTouched by rememberSaveable { mutableStateOf(false) }
    var nimTouched by rememberSaveable { mutableStateOf(false) }
    var showStaffDialog by rememberSaveable { mutableStateOf(false) }

    // Reactive UI: error dihitung ulang setiap teks berubah, hilang otomatis saat sudah benar
    val namaError = Validators.validateNama(nama)
    val nimError = Validators.validateNim(nim)
    val namaShown = if (namaTouched) namaError else null
    val nimShown = if (nimTouched) nimError else null

    fun submit() {
        namaTouched = true
        nimTouched = true
        if (namaError == null && nimError == null) {
            onLogin(nama.trim().replace(Regex("\\s+"), " "), nim.trim())
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_logo_kantin),
                    contentDescription = "Logo Kantin Trilogi",
                    modifier = Modifier.size(104.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Kantin Trilogi",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Pesan online, tinggal ambil di kantin",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            AppCard {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Login Mahasiswa",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = nama,
                        onValueChange = { value ->
                            nama = value.take(40)
                            namaTouched = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nama Lengkap") },
                        singleLine = true,
                        isError = namaShown != null,
                        supportingText = { Text(namaShown ?: "Contoh: Budi Santoso") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    OutlinedTextField(
                        value = nim,
                        onValueChange = { value ->
                            nim = value.filter { it in '0'..'9' }.take(10)
                            nimTouched = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("NIM") },
                        singleLine = true,
                        isError = nimShown != null,
                        supportingText = { Text(nimShown ?: "8-10 digit angka (${nim.length}/10)") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit() })
                    )
                    Button(
                        onClick = { submit() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Masuk")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = { showStaffDialog = true }) {
                Text("Masuk sebagai Petugas Kantin")
            }
        }
    }

    if (showStaffDialog) {
        StaffPinDialog(
            onDismiss = { showStaffDialog = false },
            onSuccess = {
                showStaffDialog = false
                onStaffLogin()
            }
        )
    }
}

@Composable
fun StaffPinDialog(onDismiss: () -> Unit, onSuccess: () -> Unit) {
    var pin by rememberSaveable { mutableStateOf("") }
    var showError by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PIN Petugas Kantin") },
        text = {
            Column {
                Text("Masukkan PIN petugas (demo: ${AppConfig.STAFF_PIN}).")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { value ->
                        pin = value.filter { it in '0'..'9' }.take(6)
                        showError = false
                    },
                    label = { Text("PIN") },
                    singleLine = true,
                    isError = showError,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    supportingText = { Text(if (showError) "PIN salah" else "") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (pin == AppConfig.STAFF_PIN) onSuccess() else showError = true
            }) {
                Text("Masuk")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
