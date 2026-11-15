package com.trilogi.kantin.util

import android.content.Context

data class Session(val nama: String, val nim: String)

/** Menyimpan sesi login mahasiswa (SharedPreferences, sesuai materi Sesi 6). */
class SessionManager(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("kantin_session", Context.MODE_PRIVATE)

    fun save(nama: String, nim: String) {
        prefs.edit().putString(KEY_NAMA, nama).putString(KEY_NIM, nim).apply()
    }

    fun get(): Session? {
        val nama = prefs.getString(KEY_NAMA, null)
        val nim = prefs.getString(KEY_NIM, null)
        return if (nama.isNullOrBlank() || nim.isNullOrBlank()) null else Session(nama, nim)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_NAMA = "nama"
        const val KEY_NIM = "nim"
    }
}
