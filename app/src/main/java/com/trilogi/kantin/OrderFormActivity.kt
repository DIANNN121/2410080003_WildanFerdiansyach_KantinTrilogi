package com.trilogi.kantin

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trilogi.kantin.data.MenuEntity
import com.trilogi.kantin.data.OrderEntity
import com.trilogi.kantin.data.OrderStatus
import com.trilogi.kantin.ui.AppCard
import com.trilogi.kantin.ui.FoodImage
import com.trilogi.kantin.ui.KantinTheme
import com.trilogi.kantin.ui.ObserveEvent
import com.trilogi.kantin.util.AppConfig
import com.trilogi.kantin.util.Extras
import com.trilogi.kantin.util.Validators
import com.trilogi.kantin.util.formatRupiah

class OrderFormActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Terima data dari layar sebelumnya + validasi (Defensive Programming)
        val nama = intent.getStringExtra(Extras.NAMA).orEmpty().trim()
        val nim = intent.getStringExtra(Extras.NIM).orEmpty().trim()
        val menuId = intent.getIntExtra(Extras.MENU_ID, -1)
        val orderId = intent.getIntExtra(Extras.ORDER_ID, -1)

        if (nama.isEmpty() || Validators.validateNim(nim) != null || (menuId <= 0 && orderId <= 0)) {
            Toast.makeText(this, "Data pesanan tidak valid.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContent {
            KantinTheme {
                OrderFormScreen(
                    nama = nama,
                    nim = nim,
                    menuId = menuId,
                    orderId = orderId,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderFormScreen(
    nama: String,
    nim: String,
    menuId: Int,
    orderId: Int,
    onBack: () -> Unit,
    vm: KantinViewModel = viewModel()
) {
    val isEdit = orderId > 0

    var menu by remember { mutableStateOf<MenuEntity?>(null) }
    var order by remember { mutableStateOf<OrderEntity?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // State input: bertahan saat rotasi layar
    var jumlahText by rememberSaveable { mutableStateOf("1") }
    var catatan by rememberSaveable { mutableStateOf("") }
    var initialized by rememberSaveable { mutableStateOf(false) }

    val saving by vm.saving.collectAsState()

    // Muat data menu / pesanan dari Room (di background thread)
    LaunchedEffect(menuId, orderId) {
        loading = true
        if (isEdit) {
            val o = vm.getOrder(orderId)
            if (o == null || o.nim != nim) {
                errorMessage = "Pesanan tidak ditemukan."
            } else if (o.status != OrderStatus.MENUNGGU) {
                errorMessage = "Pesanan sudah diproses petugas, sehingga tidak dapat diubah."
            } else {
                order = o
                if (!initialized) {
                    jumlahText = o.jumlah.toString()
                    catatan = o.catatan
                    initialized = true
                }
            }
        } else {
            val m = vm.getMenu(menuId)
            if (m == null) errorMessage = "Menu tidak ditemukan." else menu = m
        }
        loading = false
    }

    ObserveEvent(vm) { onBack() }

    val itemName: String? = if (isEdit) order?.menuNama else menu?.nama
    val itemImage: String? = if (isEdit) order?.imageKey else menu?.imageKey
    val itemPrice: Int? = if (isEdit) order?.hargaSatuan else menu?.harga
    val itemDesc: String = if (isEdit) "" else menu?.deskripsi.orEmpty()

    // Reactive UI: validasi & total dihitung ulang setiap input berubah
    val jumlahError = Validators.validateJumlah(jumlahText)
    val catatanError = Validators.validateCatatan(catatan)
    val qty = jumlahText.trim().toIntOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEdit) "Ubah Pesanan" else "Buat Pesanan",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Kembali") }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (itemName != null && itemImage != null && itemPrice != null) {
                AppCard {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        FoodImage(imageKey = itemImage, modifier = Modifier.size(96.dp))
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        ) {
                            Text(
                                text = itemName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (itemDesc.isNotEmpty()) {
                                Text(itemDesc, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                text = formatRupiah(itemPrice) + " / porsi",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                AppCard {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Pemesan: $nama ($nim)",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val current = jumlahText.trim().toIntOrNull() ?: 1
                                    jumlahText = (current - 1).coerceAtLeast(1).toString()
                                },
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .size(48.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) { Text("-") }

                            OutlinedTextField(
                                value = jumlahText,
                                onValueChange = { value ->
                                    jumlahText = value.filter { it in '0'..'9' }.take(3)
                                },
                                modifier = Modifier.weight(1f),
                                label = { Text("Jumlah porsi") },
                                singleLine = true,
                                isError = jumlahError != null,
                                supportingText = { Text(jumlahError ?: "Maksimal ${AppConfig.MAX_QTY} porsi") },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                )
                            )

                            OutlinedButton(
                                onClick = {
                                    val current = jumlahText.trim().toIntOrNull() ?: 0
                                    jumlahText = (current + 1).coerceAtMost(AppConfig.MAX_QTY).toString()
                                },
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .size(48.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) { Text("+") }
                        }

                        OutlinedTextField(
                            value = catatan,
                            onValueChange = { catatan = it.take(AppConfig.MAX_NOTE) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Catatan (opsional)") },
                            isError = catatanError != null,
                            supportingText = {
                                Text(catatanError ?: "${catatan.length}/${AppConfig.MAX_NOTE} - contoh: tidak pedas")
                            },
                            minLines = 2,
                            maxLines = 3,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                        )
                    }
                }

                val total = if (jumlahError == null && qty != null) itemPrice * qty else 0
                AppCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total bayar", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = formatRupiah(total),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Button(
                    onClick = {
                        if (jumlahError == null && catatanError == null && qty != null) {
                            if (isEdit) {
                                vm.updateOrder(orderId, nim, qty, catatan.trim())
                            } else {
                                vm.createOrder(
                                    OrderEntity(
                                        nim = nim,
                                        namaMahasiswa = nama,
                                        menuId = menuId,
                                        menuNama = itemName,
                                        imageKey = itemImage,
                                        hargaSatuan = itemPrice,
                                        jumlah = qty,
                                        catatan = catatan.trim(),
                                        status = OrderStatus.MENUNGGU,
                                        dibuatPada = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    },
                    enabled = !saving && jumlahError == null && catatanError == null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        when {
                            saving -> "Menyimpan..."
                            isEdit -> "Simpan Perubahan"
                            else -> "Pesan Sekarang"
                        }
                    )
                }
                Text(
                    text = "Setelah dipesan, petugas kantin akan memproses pesananmu. " +
                        "Pantau status di tab Pesanan Saya, lalu ambil di kantin saat statusnya Siap Diambil.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else if (loading) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Text(
                    text = errorMessage ?: "Data tidak dapat dimuat.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(onClick = onBack) { Text("Kembali") }
            }
        }
    }
}
