package com.vkas.translationapp.ui.list

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.google.gson.reflect.TypeToken
import com.vkas.translationapp.app.App.Companion.mmkvPt
import com.vkas.translationapp.base.BaseViewModel
import com.vkas.translationapp.bean.PtVpnBean
import com.vkas.translationapp.enevt.Constant
import com.vkas.translationapp.utils.KLog
import com.vkas.translationapp.utils.PixelUtils.getFastIpPt
import com.vkas.translationapp.utils.PixelUtils.getLocalServerData
import com.xuexiang.xui.utils.Utils.isNullOrEmpty
import com.xuexiang.xutil.net.JsonUtil

class ListViewModel (application: Application) : BaseViewModel(application) {
    private lateinit var skServiceBean : PtVpnBean
    private lateinit var skServiceBeanList :MutableList<PtVpnBean>

    // 服务器列表数据
    val liveServerListData: MutableLiveData<MutableList<PtVpnBean>> by lazy {
        MutableLiveData<MutableList<PtVpnBean>>()
    }

    /**
     * 获取服务器列表
     */
    fun getServerListData(){
        skServiceBeanList = ArrayList()
        skServiceBean = PtVpnBean()
        skServiceBeanList = if (isNullOrEmpty(mmkvPt.decodeString(Constant.PROFILE_PT_DATA))) {
            KLog.e("TAG","skServiceBeanList--1--->")

            getLocalServerData()
        } else {
            KLog.e("TAG","skServiceBeanList--2--->")

            JsonUtil.fromJson(
                mmkvPt.decodeString(Constant.PROFILE_PT_DATA),
                object : TypeToken<MutableList<PtVpnBean>?>() {}.type
            )
        }
        skServiceBeanList.add(0, getFastIpPt())
        KLog.e("LOG","skServiceBeanList---->${JsonUtil.toJson(skServiceBeanList)}")

        liveServerListData.postValue(skServiceBeanList)
    }
}