package com.trilogi.kantin.util

/** Validasi sisi klien. Setiap fungsi mengembalikan pesan error, atau null jika input valid. */
object Validators {
    private val NAMA_REGEX = Regex("^[A-Za-z .'-]+$")

    fun validateNama(raw: String): String? {
        val s = raw.trim()
        return when {
            s.isEmpty() -> "Nama wajib diisi"
            s.length < 3 -> "Nama minimal 3 karakter"
            !NAMA_REGEX.matches(s) -> "Nama hanya boleh huruf, spasi, titik, apostrof, atau strip"
            else -> null
        }
    }

    fun validateNim(raw: String): String? {
        val s = raw.trim()
        return when {
            s.isEmpty() -> "NIM wajib diisi"
            !s.all { it in '0'..'9' } -> "NIM hanya boleh berisi angka"
            s.length < 8 || s.length > 10 -> "NIM harus 8-10 digit"
            else -> null
        }
    }

    fun validateJumlah(raw: String): String? {
        val s = raw.trim()
        if (s.isEmpty()) return "Jumlah wajib diisi"
        val n = s.toIntOrNull() ?: return "Jumlah harus berupa angka"
        return when {
            n < 1 -> "Jumlah minimal 1 porsi"
            n > AppConfig.MAX_QTY -> "Jumlah maksimal ${AppConfig.MAX_QTY} porsi"
            else -> null
        }
    }

    fun validateCatatan(raw: String): String? =
        if (raw.length > AppConfig.MAX_NOTE) "Catatan maksimal ${AppConfig.MAX_NOTE} karakter" else null
}
