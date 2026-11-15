package com.trilogi.kantin.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Tabel "pesanan": pesanan mahasiswa. Nama menu & harga disalin agar riwayat tetap utuh. */
@Entity(tableName = "pesanan")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nim: String,
    @ColumnInfo(name = "nama_mahasiswa") val namaMahasiswa: String,
    @ColumnInfo(name = "menu_id") val menuId: Int,
    @ColumnInfo(name = "menu_nama") val menuNama: String,
    @ColumnInfo(name = "image_key") val imageKey: String,
    @ColumnInfo(name = "harga_satuan") val hargaSatuan: Int,
    val jumlah: Int,
    val catatan: String,
    val status: String,
    @ColumnInfo(name = "dibuat_pada") val dibuatPada: Long
) {
    fun total(): Int = hargaSatuan * jumlah
}

/** Alur status pesanan: MENUNGGU -> DIPROSES -> SIAP -> SELESAI. */
object OrderStatus {
    const val MENUNGGU = "MENUNGGU"
    const val DIPROSES = "DIPROSES"
    const val SIAP = "SIAP"
    const val SELESAI = "SELESAI"

    fun label(status: String): String = when (status) {
        MENUNGGU -> "Menunggu Konfirmasi"
        DIPROSES -> "Sedang Diproses"
        SIAP -> "Siap Diambil"
        SELESAI -> "Selesai"
        else -> "Tidak diketahui"
    }

    /** Posisi status pada stepper (0..3). */
    fun step(status: String): Int = when (status) {
        MENUNGGU -> 0
        DIPROSES -> 1
        SIAP -> 2
        SELESAI -> 3
        else -> 0
    }

    /** Status berikutnya yang boleh diset petugas; null jika sudah bukan wewenang petugas. */
    fun next(status: String): String? = when (status) {
        MENUNGGU -> DIPROSES
        DIPROSES -> SIAP
        else -> null
    }
}
