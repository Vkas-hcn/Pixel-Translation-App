package com.vkas.translationapp.ui.language

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.vkas.translationapp.R
import com.vkas.translationapp.bean.Language
import java.util.*

class LanguageRecentlyAdapter (data: MutableList<Language>?) :
    BaseQuickAdapter<Language, BaseViewHolder>(
        R.layout.item_language,
        data
    ) {
    override fun convert(holder: BaseViewHolder, item: Language) {
        holder.setText(R.id.tv_country_name, Locale(item.code).displayLanguage)
        if (item.isCheck) {
            holder.setImageResource(R.id.img_down_state, R.drawable.ic_lan_check)
            holder.setVisible(R.id.img_down_state, true)
        } else {
            holder.setVisible(R.id.img_down_state, false)
        }
    }
}