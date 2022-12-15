package com.vkas.translationapp.ui.language

import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.vkas.translationapp.R
import com.vkas.translationapp.bean.Language
import java.util.*
import android.widget.LinearLayout

import androidx.recyclerview.widget.RecyclerView




class LanguageAdapter (data: MutableList<Language>?) :
    BaseQuickAdapter<Language, BaseViewHolder>(
        R.layout.item_language,
        data
    ) {
    override fun convert(holder: BaseViewHolder, item: Language) {
        holder.setText(R.id.tv_country_name,Locale(item.code).displayLanguage)
        setVisibility(item.searchForMatches,holder.itemView)
        when(item.downloadStatus){
            0->{
                holder.setVisible(R.id.img_down_state,true)
                holder.setVisible(R.id.pro_down_state,false)
                holder.setImageResource(R.id.img_down_state,R.mipmap.ic_download)
            }
            1->{
                holder.setVisible(R.id.img_down_state,false)
                holder.setVisible(R.id.pro_down_state,true)
            }
            2->{
                holder.setVisible(R.id.img_down_state,true)
                holder.setVisible(R.id.pro_down_state,false)
                if(item.isCheck){
                    holder.setImageResource(R.id.img_down_state,R.drawable.ic_lan_check)
                }else{
                    holder.setImageResource(R.id.img_down_state,R.mipmap.ic_lan_delete)
                }
            }
        }

    }
    private fun setVisibility(isVisible: Boolean, itemView:View) {
        val param = itemView.layoutParams as RecyclerView.LayoutParams
        if (isVisible) {
            param.height = LinearLayout.LayoutParams.WRAP_CONTENT
            param.width = LinearLayout.LayoutParams.MATCH_PARENT
            itemView.visibility = View.VISIBLE
        } else {
            itemView.visibility = View.GONE
            param.height = 0
            param.width = 0
        }
        itemView.layoutParams = param
    }
}