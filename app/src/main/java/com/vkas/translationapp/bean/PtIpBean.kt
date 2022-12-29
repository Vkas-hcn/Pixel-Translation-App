package com.vkas.translationapp.bean

import androidx.annotation.Keep

@Keep
data class PtIpBean(
    val country: String,
    val country_code: String,
    val country_code3: String,
    val ip: String
)