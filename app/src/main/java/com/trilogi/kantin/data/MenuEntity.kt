package com.trilogi.kantin.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Tabel "menu": daftar makanan/minuman yang dijual kantin. */
@Entity(tableName = "menu")
data class MenuEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nama: String,
    val deskripsi: String,
    val harga: Int,
    val kategori: String,
    @ColumnInfo(name = "image_key") val imageKey: String,
    val tersedia: Boolean = true
)
