package com.vkas.translationapp.ui.list

import androidx.appcompat.app.AppCompatActivity
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.vkas.translationapp.R
import com.vkas.translationapp.bean.PtVpnBean
import com.vkas.translationapp.enevt.Constant
import com.vkas.translationapp.utils.PixelUtils.getFlagThroughCountryPt

class ListPtAdapter (data: MutableList<PtVpnBean>?) :
    BaseQuickAdapter<PtVpnBean, BaseViewHolder>(
        R.layout.item_vpn,
        data
    ) {
    override fun convert(holder: BaseViewHolder, item: PtVpnBean) {
        if (item.pt_best == true) {
            holder.setText(R.id.txt_country, Constant.FASTER_PT_SERVER)
            holder.setImageResource(R.id.img_flag, getFlagThroughCountryPt(Constant.FASTER_PT_SERVER))
        } else {
            holder.setText(R.id.txt_country, item.pt_country + "-" + item.pt_city)
            holder.setImageResource(R.id.img_flag, getFlagThroughCountryPt(item.pt_country.toString()))
        }
        if (item.pt_check == true) {
            holder.setBackgroundResource(R.id.con_item, R.drawable.bg_item_check)
            holder.setTextColor(R.id.txt_country,context.resources.getColor(R.color.white))
        } else {
            holder.setBackgroundResource(R.id.con_item, R.drawable.bg_item)
            holder.setTextColor(R.id.txt_country,context.resources.getColor(R.color.tv_ff_333333))
        }
    }
}