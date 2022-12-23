package com.vkas.translationapp.bean

import androidx.annotation.Keep

@Keep
class PtVpnBean (
    var pt_city: String? = null,
    var pt_country: String? = null,
    var pt_ip: String? = null,
    var pt_method: String? = null,
    var pt_port: Int? = null,
    var pt_pwd: String? = null,
    var pt_check: Boolean? = false,
    var pt_best: Boolean? = false,
    var isAd:Boolean? = false
)