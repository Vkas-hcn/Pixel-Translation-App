package com.vkas.translationapp.ui.vpn

import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.github.shadowsocks.database.Profile
import com.github.shadowsocks.database.ProfileManager
import com.github.shadowsocks.preference.DataStore
import com.google.gson.reflect.TypeToken
import com.vkas.translationapp.R
import com.vkas.translationapp.ad.*
import com.vkas.translationapp.app.App.Companion.mmkvPt
import com.vkas.translationapp.base.BaseViewModel
import com.vkas.translationapp.bean.PtAdBean
import com.vkas.translationapp.bean.PtIpBean
import com.vkas.translationapp.bean.PtVpnBean
import com.vkas.translationapp.enevt.Constant
import com.vkas.translationapp.utils.MmkvUtils
import com.vkas.translationapp.utils.PixelUtils
import com.xuexiang.xui.utils.Utils
import com.xuexiang.xutil.net.JsonUtil

class VpnViewModel (application: Application) : BaseViewModel(application){
    //初始化服务器数据
    val liveInitializeServerData: MutableLiveData<PtVpnBean> by lazy {
        MutableLiveData<PtVpnBean>()
    }
    //更新服务器数据(未连接)
    val liveNoUpdateServerData: MutableLiveData<PtVpnBean> by lazy {
        MutableLiveData<PtVpnBean>()
    }
    //更新服务器数据(已连接)
    val liveUpdateServerData: MutableLiveData<PtVpnBean> by lazy {
        MutableLiveData<PtVpnBean>()
    }

    //当前服务器
    var currentServerData: PtVpnBean = PtVpnBean()
    //断开后选中服务器
    var afterDisconnectionServerData: PtVpnBean = PtVpnBean()
    //跳转结果页
    val liveJumpResultsPage: MutableLiveData<Bundle> by lazy {
        MutableLiveData<Bundle>()
    }
    fun initializeServerData() {
        val bestData = PixelUtils.getFastIpPt()
        ProfileManager.getProfile(DataStore.profileId).let {
            if (it != null) {
                ProfileManager.updateProfile(setSkServerData(it, bestData))
            } else {
                val profile = Profile()
                ProfileManager.createProfile(setSkServerData(profile, bestData))
            }
        }
        DataStore.profileId = 1L
        currentServerData = bestData
        val serviceData = JsonUtil.toJson(currentServerData)
        MmkvUtils.set("currentServerData",serviceData)
        liveInitializeServerData.postValue(bestData)
    }

    fun updateSkServer(skServiceBean: PtVpnBean,isConnect:Boolean) {
        ProfileManager.getProfile(DataStore.profileId).let {
            if (it != null) {
                setSkServerData(it, skServiceBean)
                ProfileManager.updateProfile(it)
            } else {
                ProfileManager.createProfile(Profile())
            }
        }
        DataStore.profileId = 1L
        if(isConnect){
            afterDisconnectionServerData = skServiceBean
            liveUpdateServerData.postValue(skServiceBean)
        }else{
            currentServerData = skServiceBean
            val serviceData = JsonUtil.toJson(currentServerData)
            MmkvUtils.set("currentServerData",serviceData)
            liveNoUpdateServerData.postValue(skServiceBean)
        }
    }

    /**
     * 设置服务器数据
     */
    private fun setSkServerData(profile: Profile, bestData: PtVpnBean): Profile {
        profile.name = bestData.pt_country + "-" + bestData.pt_city
        profile.host = bestData.pt_ip.toString()
        profile.password = bestData.pt_pwd!!
        profile.method = bestData.pt_method!!
        profile.remotePort = bestData.pt_port!!
        return profile
    }
    /**
     * 跳转连接结果页
     */
    fun jumpConnectionResultsPage(isConnection: Boolean){
        val bundle = Bundle()
        val serviceData = mmkvPt.decodeString("currentServerData", "").toString()
        bundle.putBoolean(Constant.CONNECTION_PT_STATUS, isConnection)
        bundle.putString(Constant.SERVER_PT_INFORMATION, serviceData)
        liveJumpResultsPage.postValue(bundle)
    }
    /**
     * 清空广告缓存重新加载
     */
    fun emptyAdvertisementCacheAndReload(context: Context){

        //开屏
        PtLoadOpenAd.getInstance().appAdDataPt = null
        PtLoadOpenAd.getInstance().isLoadingPt = false
        PtLoadOpenAd.getInstance().adIndexPt = 0
        PtLoadOpenAd.getInstance().advertisementLoadingPt(context)

        PtLoadHomeAd.getInstance().appAdDataPt = null
        PtLoadHomeAd.getInstance().isLoadingPt = false
        PtLoadHomeAd.getInstance().adIndexPt = 0
        PtLoadHomeAd.getInstance().advertisementLoadingPt(context)

        PtLoadTranslationAd.getInstance().appAdDataPt = null
        PtLoadTranslationAd.getInstance().isLoadingPt = false
        PtLoadTranslationAd.getInstance().adIndexPt = 0
        PtLoadTranslationAd.getInstance().advertisementLoadingPt(context)

        PtLoadBackAd.getInstance().appAdDataPt = null
        PtLoadBackAd.getInstance().isLoadingPt = false
        PtLoadBackAd.getInstance().adIndexPt = 0
        PtLoadBackAd.getInstance().advertisementLoadingPt(context)

        PtLoadVpnAd.getInstance().appAdDataPt = null
        PtLoadVpnAd.getInstance().isLoadingPt = false
        PtLoadVpnAd.getInstance().adIndexPt = 0
        PtLoadVpnAd.getInstance().advertisementLoadingPt(context)

//        PtLoadResultAd.getInstance().appAdDataPt = null
//        PtLoadResultAd.getInstance().isLoadingPt = false
//        PtLoadResultAd.getInstance().adIndexPt = 0
//        PtLoadResultAd.getInstance().advertisementLoadingPt(context)

        PtLoadConnectAd.getInstance().appAdDataPt = null
        PtLoadConnectAd.getInstance().isLoadingPt = false
        PtLoadConnectAd.getInstance().adIndexPt = 0
        PtLoadConnectAd.getInstance().advertisementLoadingPt(context)
    }

}