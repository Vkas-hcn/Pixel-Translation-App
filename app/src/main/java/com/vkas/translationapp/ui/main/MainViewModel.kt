package com.vkas.translationapp.ui.main

import android.app.AlertDialog
import android.app.Application
import android.content.DialogInterface
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.reflect.TypeToken
import com.vkas.translationapp.R
import com.vkas.translationapp.app.App.Companion.mmkvPt
import com.vkas.translationapp.base.BaseViewModel
import com.vkas.translationapp.bean.PtIpBean
import com.vkas.translationapp.enevt.Constant
import com.vkas.translationapp.utils.KLog
import com.vkas.translationapp.utils.PixelUtils
import com.xuexiang.xui.utils.Utils
import com.xuexiang.xutil.net.JsonUtil

class MainViewModel(application: Application) : BaseViewModel(application) {
    /**
     * 获取VPN引导弹窗
     */
    fun getVpnBootPopUpWindow(): Boolean {
        return mmkvPt.decodeString(Constant.PIXEL_SET).let {
            if (Utils.isNullOrEmpty(it)) {
                return true
            } else {
                when (it) {
                    "1" -> return true
                    "2" -> {
                        KLog.e("state","PixelUtils.isValuableUser()=${PixelUtils.isValuableUser()}")
                        return PixelUtils.isValuableUser()}
                    "3" -> return PixelUtils.isFacebookUser()
                    "4" -> return false
                    else -> true
                }
            }
        }
    }
    /**
     * 是否是买量用户
     */
    fun isItABuyingUser():Boolean{
        return PixelUtils.isValuableUser()
    }
    /**
     * 解析是否是非法ip；中国大陆ip、伊朗ip
     */
    fun whetherParsingIsIllegalIp():Boolean{
        val data = mmkvPt.decodeString(Constant.IP_INFORMATION)
        return if(Utils.isNullOrEmpty(data)){
            true
        }else{
            val ptIpBean: PtIpBean = JsonUtil.fromJson(
                mmkvPt.decodeString(Constant.IP_INFORMATION),
                object : TypeToken<PtIpBean?>() {}.type
            )
            return ptIpBean.country_code =="IR" || ptIpBean.country_code =="CN123"
        }
    }
    /**
     * 是否显示不能使用弹框
     */
    fun whetherTheBulletBoxCannotBeUsed(context: AppCompatActivity){
        val dialogVpn: AlertDialog = AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.vpn))
            .setMessage(context.getString(R.string.cant_user_vpn))
            .setCancelable(false)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }.create()
        dialogVpn.setCancelable(false)
        dialogVpn.show()
        dialogVpn.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
        dialogVpn.getButton(DialogInterface.BUTTON_NEGATIVE)?.setTextColor(Color.BLACK)
    }
}