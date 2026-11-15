package com.trilogi.kantin.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatRupiah(value: Int): String {
    val digits = if (value < 0) (-value).toString() else value.toString()
    val sb = StringBuilder()
    digits.reversed().forEachIndexed { index, c ->
        if (index > 0 && index % 3 == 0) sb.append('.')
        sb.append(c)
    }
    val sign = if (value < 0) "-" else ""
    return sign + "Rp " + sb.reverse().toString()
}

/** Nomor antrian, contoh: #007 */
fun formatQueue(id: Int): String = "#" + id.toString().padStart(3, '0')

fun formatDateTime(millis: Long): String =
    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(millis))
