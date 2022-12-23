package com.vkas.translationapp.bean

import androidx.annotation.Keep

@Keep
data class PtAdBean(
    var pt_open: MutableList<PtDetailBean> = ArrayList(),
    var pt_home: MutableList<PtDetailBean> = ArrayList(),
    var pt_translation: MutableList<PtDetailBean> = ArrayList(),
    var pt_back: MutableList<PtDetailBean> = ArrayList(),
    var pt_click_num: Int=0,
    var pt_show_num: Int=0
)

@Keep
data class PtDetailBean(
    val pt_id: String,
    val pt_platform: String,
    val pt_type: String,
    val pt_weight: Int
)