package com.trilogi.kantin.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuDao {
    @Query("SELECT * FROM menu WHERE tersedia = 1 ORDER BY kategori, nama")
    fun observeAvailable(): Flow<List<MenuEntity>>

    @Query("SELECT * FROM menu WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): MenuEntity?
}

@Dao
interface OrderDao {
    // CREATE
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: OrderEntity): Long

    // READ
    @Query("SELECT * FROM pesanan WHERE nim = :nim ORDER BY dibuat_pada DESC")
    fun observeByNim(nim: String): Flow<List<OrderEntity>>

    @Query(
        "SELECT * FROM pesanan " +
            "ORDER BY CASE status WHEN 'SELESAI' THEN 1 ELSE 0 END, dibuat_pada ASC"
    )
    fun observeAll(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM pesanan WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): OrderEntity?

    // UPDATE (hanya boleh selama masih MENUNGGU, supaya aman dari tabrakan dengan petugas)
    @Query(
        "UPDATE pesanan SET jumlah = :jumlah, catatan = :catatan " +
            "WHERE id = :id AND nim = :nim AND status = 'MENUNGGU'"
    )
    suspend fun updateIfPending(id: Int, nim: String, jumlah: Int, catatan: String): Int

    // UPDATE status oleh petugas (hanya jika status saat ini masih sesuai)
    @Query("UPDATE pesanan SET status = :next WHERE id = :id AND status = :current")
    suspend fun advanceStatus(id: Int, current: String, next: String): Int

    // UPDATE status oleh mahasiswa: konfirmasi sudah mengambil pesanan
    @Query("UPDATE pesanan SET status = 'SELESAI' WHERE id = :id AND nim = :nim AND status = 'SIAP'")
    suspend fun confirmPickup(id: Int, nim: String): Int

    // DELETE (batalkan pesanan yang belum diproses, atau hapus riwayat yang sudah selesai)
    @Query(
        "DELETE FROM pesanan " +
            "WHERE id = :id AND nim = :nim AND status IN ('MENUNGGU', 'SELESAI')"
    )
    suspend fun deleteIfAllowed(id: Int, nim: String): Int
}
