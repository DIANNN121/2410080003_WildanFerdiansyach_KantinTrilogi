package com.trilogi.kantin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trilogi.kantin.data.MenuEntity
import com.trilogi.kantin.data.OrderEntity
import com.trilogi.kantin.data.OrderStatus
import com.trilogi.kantin.ui.AppCard
import com.trilogi.kantin.ui.FoodImage
import com.trilogi.kantin.ui.KantinTheme
import com.trilogi.kantin.ui.ObserveEvent
import com.trilogi.kantin.ui.StatusBadge
import com.trilogi.kantin.ui.StatusStepper
import com.trilogi.kantin.util.Extras
import com.trilogi.kantin.util.SessionManager
import com.trilogi.kantin.util.Validators
import com.trilogi.kantin.util.formatDateTime
import com.trilogi.kantin.util.formatQueue
import com.trilogi.kantin.util.formatRupiah

class HomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Defensive programming: data Intent bisa saja kosong/tidak valid.
        val nama = intent.getStringExtra(Extras.NAMA).orEmpty().trim()
        val nim = intent.getStringExtra(Extras.NIM).orEmpty().trim()
        if (nama.isEmpty() || Validators.validateNim(nim) != null) {
            Toast.makeText(this, "Sesi tidak valid. Silakan login ulang.", Toast.LENGTH_LONG).show()
            logout()
            return
        }

        setContent {
            KantinTheme {
                HomeScreen(
                    nama = nama,
                    nim = nim,
                    onOrderMenu = { menuId -> openOrderForm(nama, nim, menuId, -1) },
                    onEditOrder = { orderId -> openOrderForm(nama, nim, -1, orderId) },
                    onLogout = { logout() }
                )
            }
        }
    }

    /** Explicit Intent ke OrderFormActivity beserta data yang dikirim. */
    private fun openOrderForm(nama: String, nim: String, menuId: Int, orderId: Int) {
        val intent = Intent(this, OrderFormActivity::class.java).apply {
            putExtra(Extras.NAMA, nama)
            putExtra(Extras.NIM, nim)
            putExtra(Extras.MENU_ID, menuId)
            putExtra(Extras.ORDER_ID, orderId)
        }
        startActivity(intent)
    }

    private fun logout() {
        SessionManager(this).clear()
        val intent = Intent(this, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
    }
}

private val CATEGORIES = listOf("Semua", "Makanan", "Minuman", "Snack")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    nama: String,
    nim: String,
    onOrderMenu: (Int) -> Unit,
    onEditOrder: (Int) -> Unit,
    onLogout: () -> Unit,
    vm: KantinViewModel = viewModel()
) {
    val menuList by vm.menuList.collectAsState(initial = emptyList())
    val ordersFlow = remember(nim) { vm.ordersFor(nim) }
    val orders by ordersFlow.collectAsState(initial = emptyList())

    var tab by rememberSaveable { mutableStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("Semua") }
    var deleteId by rememberSaveable { mutableStateOf(-1) }

    ObserveEvent(vm)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Kantin Trilogi", fontWeight = FontWeight.Bold)
                        Text(
                            text = "$nama - $nim",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onLogout) { Text("Keluar") }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Menu") })
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    text = { Text("Pesanan Saya (${orders.size})") }
                )
            }

            if (tab == 0) {
                MenuTab(
                    menuList = menuList,
                    query = query,
                    onQueryChange = { query = it.take(30) },
                    category = category,
                    onCategoryChange = { category = it },
                    onOrder = onOrderMenu
                )
            } else {
                OrdersTab(
                    orders = orders,
                    onEdit = onEditOrder,
                    onCancel = { deleteId = it },
                    onPickup = { vm.confirmPickup(it, nim) },
                    onGoMenu = { tab = 0 }
                )
            }
        }
    }

    // Dialog konfirmasi sebelum hapus (Defensive UX, materi Sesi 7)
    val target = orders.firstOrNull { it.id == deleteId }
    if (target != null) {
        AlertDialog(
            onDismissRequest = { deleteId = -1 },
            title = {
                Text(if (target.status == OrderStatus.SELESAI) "Hapus Riwayat" else "Batalkan Pesanan")
            },
            text = {
                Text(
                    "Yakin ingin menghapus pesanan ${target.menuNama} " +
                        "(${formatQueue(target.id)})? Tindakan ini tidak dapat dibatalkan."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteOrder(target.id, nim)
                    deleteId = -1
                }) { Text("Ya, Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { deleteId = -1 }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun MenuTab(
    menuList: List<MenuEntity>,
    query: String,
    onQueryChange: (String) -> Unit,
    category: String,
    onCategoryChange: (String) -> Unit,
    onOrder: (Int) -> Unit
) {
    // Reactive UI: daftar langsung terfilter setiap kali teks/kategori berubah
    val filtered = menuList.filter { item ->
        (category == "Semua" || item.kategori == category) &&
            item.nama.contains(query.trim(), ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Cari menu") },
                singleLine = true
            )
        }
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CATEGORIES.forEach { c ->
                    if (c == category) {
                        Button(onClick = { onCategoryChange(c) }) { Text(c) }
                    } else {
                        OutlinedButton(onClick = { onCategoryChange(c) }) { Text(c) }
                    }
                }
            }
        }
        if (filtered.isEmpty()) {
            item {
                Text(
                    text = if (menuList.isEmpty()) "Memuat menu..." else "Menu tidak ditemukan.",
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        items(filtered, key = { it.id }) { menu ->
            MenuCard(menu = menu, onOrder = { onOrder(menu.id) })
        }
    }
}

@Composable
private fun MenuCard(menu: MenuEntity, onOrder: () -> Unit) {
    AppCard {
        Row(modifier = Modifier.padding(12.dp)) {
            FoodImage(imageKey = menu.imageKey, modifier = Modifier.size(88.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = menu.nama,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = menu.deskripsi,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatRupiah(menu.harga),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Button(onClick = onOrder) { Text("Pesan") }
                }
            }
        }
    }
}

@Composable
private fun OrdersTab(
    orders: List<OrderEntity>,
    onEdit: (Int) -> Unit,
    onCancel: (Int) -> Unit,
    onPickup: (Int) -> Unit,
    onGoMenu: () -> Unit
) {
    if (orders.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_logo_kantin),
                contentDescription = null,
                modifier = Modifier.size(96.dp).clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Belum ada pesanan", style = MaterialTheme.typography.titleMedium)
            Text("Pilih menu favoritmu lalu tekan tombol Pesan.")
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onGoMenu) { Text("Lihat Menu") }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(orders, key = { it.id }) { order ->
            OrderCard(
                order = order,
                onEdit = { onEdit(order.id) },
                onCancel = { onCancel(order.id) },
                onPickup = { onPickup(order.id) }
            )
        }
    }
}

@Composable
private fun OrderCard(
    order: OrderEntity,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
    onPickup: () -> Unit
) {
    val info = when (order.status) {
        OrderStatus.MENUNGGU -> "Pesanan menunggu dikonfirmasi petugas kantin."
        OrderStatus.DIPROSES -> "Pesanan sedang dibuat. Mohon tunggu sebentar."
        OrderStatus.SIAP -> "Pesanan sudah jadi! Silakan ambil di kantin."
        else -> "Pesanan sudah diambil. Terima kasih!"
    }

    AppCard {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FoodImage(imageKey = order.imageKey, modifier = Modifier.size(56.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = "Antrian ${formatQueue(order.id)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("${order.menuNama} x${order.jumlah}")
                    Text(
                        text = formatRupiah(order.total()),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                StatusBadge(status = order.status)
            }

            StatusStepper(status = order.status)

            Text(info, style = MaterialTheme.typography.bodyMedium)
            if (order.catatan.isNotBlank()) {
                Text("Catatan: ${order.catatan}", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = "Dipesan: ${formatDateTime(order.dibuatPada)}",
                style = MaterialTheme.typography.labelSmall
            )

            when (order.status) {
                OrderStatus.MENUNGGU -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onEdit) { Text("Ubah") }
                    OutlinedButton(onClick = onCancel) { Text("Batalkan") }
                }
                OrderStatus.SIAP -> Button(onClick = onPickup, modifier = Modifier.fillMaxWidth()) {
                    Text("Sudah Saya Ambil")
                }
                OrderStatus.SELESAI -> TextButton(onClick = onCancel) { Text("Hapus Riwayat") }
                else -> Unit
            }
        }
    }
}
