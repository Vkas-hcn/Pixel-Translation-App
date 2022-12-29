package com.vkas.translationapp.ui.main

import android.app.Application
import com.vkas.translationapp.app.App.Companion.mmkvPt
import com.vkas.translationapp.base.BaseViewModel
import com.vkas.translationapp.enevt.Constant
import com.vkas.translationapp.utils.KLog
import com.vkas.translationapp.utils.PixelUtils
import com.xuexiang.xui.utils.Utils

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
}