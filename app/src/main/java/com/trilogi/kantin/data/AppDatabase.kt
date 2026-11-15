package com.trilogi.kantin.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [MenuEntity::class, OrderEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun menuDao(): MenuDao
    abstract fun orderDao(): OrderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kantin_db"
                )
                    .addCallback(SeedCallback)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class SeedItem(
            val nama: String,
            val deskripsi: String,
            val harga: Int,
            val kategori: String,
            val imageKey: String
        )

        private val seedMenu = listOf(
            SeedItem("Nasi Goreng Spesial", "Nasi goreng ayam dengan telur mata sapi dan kerupuk", 15000, "Makanan", "nasi"),
            SeedItem("Ayam Geprek", "Ayam crispy diulek sambal bawang pedas, nasi putih", 17000, "Makanan", "ayam"),
            SeedItem("Mie Ayam Bakso", "Mie kenyal, ayam cincang, bakso, dan kuah gurih", 14000, "Makanan", "mangkuk"),
            SeedItem("Soto Ayam", "Soto kuah kuning dengan suwiran ayam dan telur", 13000, "Makanan", "mangkuk"),
            SeedItem("Es Teh Manis", "Teh dingin segar dengan gula asli", 5000, "Minuman", "minuman"),
            SeedItem("Jus Jeruk", "Jeruk peras segar tanpa pengawet", 8000, "Minuman", "minuman"),
            SeedItem("Kopi Susu", "Kopi susu dingin gula aren", 10000, "Minuman", "minuman"),
            SeedItem("Pisang Goreng", "Pisang goreng tepung renyah, 4 potong", 7000, "Snack", "snack"),
            SeedItem("Batagor", "Batagor ikan dengan saus kacang", 12000, "Snack", "snack")
        )

        /** Mengisi data menu awal sekali saja, tepat saat database pertama kali dibuat. */
        private object SeedCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                seedMenu.forEach { item ->
                    db.execSQL(
                        "INSERT INTO menu (nama, deskripsi, harga, kategori, image_key, tersedia) " +
                            "VALUES (?, ?, ?, ?, ?, 1)",
                        arrayOf<Any>(item.nama, item.deskripsi, item.harga, item.kategori, item.imageKey)
                    )
                }
            }
        }
    }
}
