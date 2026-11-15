package com.trilogi.kantin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trilogi.kantin.data.AppDatabase
import com.trilogi.kantin.data.MenuEntity
import com.trilogi.kantin.data.OrderEntity
import com.trilogi.kantin.data.OrderStatus
import com.trilogi.kantin.util.formatQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Pesan satu kali (toast) hasil operasi database. */
data class UiEvent(val success: Boolean, val message: String)

/**
 * ViewModel bertahan saat rotasi layar (Activity Recreation).
 * Semua operasi database dijalankan di Dispatchers.IO (aturan emas Sesi 6).
 */
class KantinViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val menuDao = database.menuDao()
    private val orderDao = database.orderDao()

    val menuList: Flow<List<MenuEntity>> = menuDao.observeAvailable()
    val allOrders: Flow<List<OrderEntity>> = orderDao.observeAll()

    private val _event = MutableStateFlow<UiEvent?>(null)
    val event: StateFlow<UiEvent?> = _event

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving

    fun consumeEvent() {
        _event.value = null
    }

    fun ordersFor(nim: String): Flow<List<OrderEntity>> = orderDao.observeByNim(nim)

    suspend fun getMenu(id: Int): MenuEntity? = withContext(Dispatchers.IO) { menuDao.getById(id) }

    suspend fun getOrder(id: Int): OrderEntity? = withContext(Dispatchers.IO) { orderDao.getById(id) }

    // ---------- CREATE ----------
    fun createOrder(order: OrderEntity) {
        if (_saving.value) return
        _saving.value = true
        viewModelScope.launch {
            try {
                val id = withContext(Dispatchers.IO) { orderDao.insert(order) }
                _event.value = UiEvent(
                    true,
                    "Pesanan ${formatQueue(id.toInt())} berhasil dibuat. Pantau statusnya di tab Pesanan Saya."
                )
            } catch (e: Exception) {
                _event.value = UiEvent(false, "Gagal menyimpan pesanan. Silakan coba lagi.")
            } finally {
                _saving.value = false
            }
        }
    }

    // ---------- UPDATE ----------
    fun updateOrder(id: Int, nim: String, jumlah: Int, catatan: String) {
        if (_saving.value) return
        _saving.value = true
        viewModelScope.launch {
            try {
                val rows = withContext(Dispatchers.IO) {
                    orderDao.updateIfPending(id, nim, jumlah, catatan)
                }
                _event.value = if (rows > 0) {
                    UiEvent(true, "Pesanan berhasil diperbarui.")
                } else {
                    UiEvent(false, "Pesanan sudah diproses petugas dan tidak dapat diubah lagi.")
                }
            } catch (e: Exception) {
                _event.value = UiEvent(false, "Gagal memperbarui pesanan. Silakan coba lagi.")
            } finally {
                _saving.value = false
            }
        }
    }

    // ---------- DELETE ----------
    fun deleteOrder(id: Int, nim: String) {
        viewModelScope.launch {
            try {
                val rows = withContext(Dispatchers.IO) { orderDao.deleteIfAllowed(id, nim) }
                _event.value = if (rows > 0) {
                    UiEvent(true, "Pesanan berhasil dihapus.")
                } else {
                    UiEvent(false, "Pesanan sedang diproses, tidak dapat dibatalkan.")
                }
            } catch (e: Exception) {
                _event.value = UiEvent(false, "Gagal menghapus pesanan.")
            }
        }
    }

    // ---------- UPDATE STATUS: mahasiswa ----------
    fun confirmPickup(id: Int, nim: String) {
        viewModelScope.launch {
            try {
                val rows = withContext(Dispatchers.IO) { orderDao.confirmPickup(id, nim) }
                _event.value = if (rows > 0) {
                    UiEvent(true, "Terima kasih! Pesanan ditandai sudah diambil.")
                } else {
                    UiEvent(false, "Pesanan belum siap diambil.")
                }
            } catch (e: Exception) {
                _event.value = UiEvent(false, "Gagal memperbarui status pesanan.")
            }
        }
    }

    // ---------- UPDATE STATUS: petugas ----------
    fun advanceStatus(order: OrderEntity) {
        val next = OrderStatus.next(order.status) ?: return
        viewModelScope.launch {
            try {
                val rows = withContext(Dispatchers.IO) {
                    orderDao.advanceStatus(order.id, order.status, next)
                }
                _event.value = if (rows > 0) {
                    UiEvent(true, "Status diubah menjadi: ${OrderStatus.label(next)}")
                } else {
                    UiEvent(false, "Status pesanan sudah berubah. Daftar diperbarui.")
                }
            } catch (e: Exception) {
                _event.value = UiEvent(false, "Gagal mengubah status pesanan.")
            }
        }
    }
}
