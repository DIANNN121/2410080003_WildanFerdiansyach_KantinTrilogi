package com.trilogi.kantin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trilogi.kantin.data.OrderEntity
import com.trilogi.kantin.data.OrderStatus
import com.trilogi.kantin.ui.AppCard
import com.trilogi.kantin.ui.FoodImage
import com.trilogi.kantin.ui.KantinTheme
import com.trilogi.kantin.ui.ObserveEvent
import com.trilogi.kantin.ui.StatusBadge
import com.trilogi.kantin.ui.StatusStepper
import com.trilogi.kantin.util.formatDateTime
import com.trilogi.kantin.util.formatQueue
import com.trilogi.kantin.util.formatRupiah

class StaffActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KantinTheme {
                StaffScreen(onExit = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffScreen(onExit: () -> Unit, vm: KantinViewModel = viewModel()) {
    val orders by vm.allOrders.collectAsState(initial = emptyList())
    var showAll by rememberSaveable { mutableStateOf(false) }

    ObserveEvent(vm)

    val waiting = orders.count { it.status == OrderStatus.MENUNGGU }
    val cooking = orders.count { it.status == OrderStatus.DIPROSES }
    val ready = orders.count { it.status == OrderStatus.SIAP }
    val visible = if (showAll) orders else orders.filter { it.status != OrderStatus.SELESAI }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel Petugas Kantin", fontWeight = FontWeight.Bold) },
                actions = { TextButton(onClick = onExit) { Text("Keluar") } }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryBox("Menunggu", waiting, Modifier.weight(1f))
                    SummaryBox("Diproses", cooking, Modifier.weight(1f))
                    SummaryBox("Siap", ready, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!showAll) {
                        Button(onClick = { showAll = false }) { Text("Pesanan Aktif") }
                        OutlinedButton(onClick = { showAll = true }) { Text("Semua Riwayat") }
                    } else {
                        OutlinedButton(onClick = { showAll = false }) { Text("Pesanan Aktif") }
                        Button(onClick = { showAll = true }) { Text("Semua Riwayat") }
                    }
                }
            }
            if (visible.isEmpty()) {
                item {
                    Text(
                        text = "Belum ada pesanan masuk.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
            items(visible, key = { it.id }) { order ->
                StaffOrderCard(order = order, onAdvance = { vm.advanceStatus(order) })
            }
        }
    }
}

@Composable
private fun SummaryBox(label: String, count: Int, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun StaffOrderCard(order: OrderEntity, onAdvance: () -> Unit) {
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
                    Text("${order.namaMahasiswa} (${order.nim})", style = MaterialTheme.typography.bodySmall)
                    Text("${order.menuNama} x${order.jumlah} - ${formatRupiah(order.total())}")
                }
                StatusBadge(status = order.status)
            }
            if (order.catatan.isNotBlank()) {
                Text("Catatan: ${order.catatan}", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = "Dipesan: ${formatDateTime(order.dibuatPada)}",
                style = MaterialTheme.typography.labelSmall
            )
            StatusStepper(status = order.status)

            when (order.status) {
                OrderStatus.MENUNGGU -> Button(onClick = onAdvance, modifier = Modifier.fillMaxWidth()) {
                    Text("Terima & Mulai Proses")
                }
                OrderStatus.DIPROSES -> Button(onClick = onAdvance, modifier = Modifier.fillMaxWidth()) {
                    Text("Tandai Siap Diambil")
                }
                OrderStatus.SIAP -> Text(
                    text = "Menunggu mahasiswa mengambil pesanan.",
                    style = MaterialTheme.typography.bodyMedium
                )
                else -> Text("Pesanan selesai.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
