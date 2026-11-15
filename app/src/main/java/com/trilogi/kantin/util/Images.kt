package com.trilogi.kantin.util

import com.trilogi.kantin.R

/** Memetakan kunci gambar di database ke aset di folder res/drawable. */
fun menuImageRes(key: String): Int = when (key) {
    "nasi" -> R.drawable.img_nasi
    "ayam" -> R.drawable.img_ayam
    "mangkuk" -> R.drawable.img_mangkuk
    "minuman" -> R.drawable.img_minuman
    "snack" -> R.drawable.img_snack
    else -> R.drawable.ic_logo_kantin
}
