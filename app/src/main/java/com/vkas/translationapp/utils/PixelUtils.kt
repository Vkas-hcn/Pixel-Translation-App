package com.vkas.translationapp.utils

import android.content.Context
import android.view.View
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.gson.reflect.TypeToken
import com.vkas.translationapp.BuildConfig
import com.vkas.translationapp.R
import com.vkas.translationapp.app.App.Companion.mmkvPt
import com.vkas.translationapp.bean.PtAdBean
import com.vkas.translationapp.bean.PtDetailBean
import com.vkas.translationapp.bean.PtVpnBean
import com.vkas.translationapp.enevt.Constant
import com.vkas.translationapp.enevt.Constant.logTagPt
import com.xuexiang.xui.utils.Utils
import com.xuexiang.xutil.net.JsonUtil
import com.xuexiang.xutil.resource.ResUtils.getString
import com.xuexiang.xutil.resource.ResourceUtils
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

object PixelUtils {
    private var installReferrer: String = ""

    /**
     * 获取Fast ip
     */
    fun getFastIpPt(): PtVpnBean {
        val ptVpnBean: MutableList<PtVpnBean> = getLocalServerData()
        var intersectionList = findFastAndOrdinaryIntersection(ptVpnBean)
        if (intersectionList.size <= 0) {
            intersectionList = ptVpnBean
        }
        intersectionList.shuffled().take(1).forEach {
            it.pt_best = true
            it.pt_country = getString(R.string.fast_service)
            return it
        }
        intersectionList[0].pt_best = true
        return intersectionList[0]
    }

    /**
     * 获取本地服务器数据
     */
    fun getLocalServerData(): MutableList<PtVpnBean> {
        return if (Utils.isNullOrEmpty(mmkvPt.decodeString(Constant.PROFILE_PT_DATA))) {
            JsonUtil.fromJson(
                ResourceUtils.readStringFromAssert("ptVpnData.json"),
                object : TypeToken<MutableList<PtVpnBean>?>() {}.type
            )
        } else {
            JsonUtil.fromJson(
                mmkvPt.decodeString(Constant.PROFILE_PT_DATA),
                object : TypeToken<MutableList<PtVpnBean>?>() {}.type
            )
        }
    }

    /**
     * 获取本地Fast服务器数据
     */
    private fun getLocalFastServerData(): MutableList<String> {
        return if (Utils.isNullOrEmpty(mmkvPt.decodeString(Constant.PROFILE_PT_DATA_FAST))) {
            JsonUtil.fromJson(
                ResourceUtils.readStringFromAssert("ptVpnFastData.json"),
                object : TypeToken<MutableList<String>?>() {}.type
            )
        } else {
            JsonUtil.fromJson(
                mmkvPt.decodeString(Constant.PROFILE_PT_DATA_FAST),
                object : TypeToken<MutableList<String>?>() {}.type
            )
        }
    }
    /**
     *
     */

    /**
     * 找出fast与普通交集
     */
    private fun findFastAndOrdinaryIntersection(ptVpnBeans: MutableList<PtVpnBean>): MutableList<PtVpnBean> {
        val intersectionList: MutableList<PtVpnBean> = ArrayList()
        getLocalFastServerData().forEach { fast ->
            ptVpnBeans.forEach { skServiceBean ->
                if (fast == skServiceBean.pt_ip) {
                    intersectionList.add(skServiceBean)
                }
            }
        }
        return intersectionList
    }

    /**
     * 广告排序
     */
    private fun adSortingPt(ptAdBean: PtAdBean): PtAdBean {
        val adBean: PtAdBean = PtAdBean()
        val ptOpen = ptAdBean.pt_open.sortedWith(compareByDescending { it.pt_weight })
        val ptHome = ptAdBean.pt_home.sortedWith(compareByDescending { it.pt_weight })
        val ptTranslation = ptAdBean.pt_translation.sortedWith(compareByDescending { it.pt_weight })
        val ptBack = ptAdBean.pt_back.sortedWith(compareByDescending { it.pt_weight })

        val ptVpn = ptAdBean.pt_vpn.sortedWith(compareByDescending { it.pt_weight })
        val ptResult = ptAdBean.pt_result.sortedWith(compareByDescending { it.pt_weight })
        val ptConnect = ptAdBean.pt_connect.sortedWith(compareByDescending { it.pt_weight })


        adBean.pt_open = ptOpen.toMutableList()
        adBean.pt_home = ptHome.toMutableList()
        adBean.pt_translation = ptTranslation.toMutableList()
        adBean.pt_back = ptBack.toMutableList()

        adBean.pt_vpn = ptVpn.toMutableList()
        adBean.pt_result = ptResult.toMutableList()
        adBean.pt_connect = ptConnect.toMutableList()

        adBean.pt_show_num = ptAdBean.pt_show_num
        adBean.pt_click_num = ptAdBean.pt_click_num
        return adBean
    }

    /**
     * 取出排序后的广告ID
     */
    fun takeSortedAdIDPt(index: Int, ptAdDetails: MutableList<PtDetailBean>): String {
        return ptAdDetails.getOrNull(index)?.pt_id ?: ""
    }

    /**
     * 获取广告服务器数据
     */
    fun getAdServerDataPt(): PtAdBean {
        val serviceData: PtAdBean =
            if (Utils.isNullOrEmpty(mmkvPt.decodeString(Constant.ADVERTISING_PT_DATA))) {
                JsonUtil.fromJson(
                    ResourceUtils.readStringFromAssert("ptAdData.json"),
                    object : TypeToken<
                            PtAdBean?>() {}.type
                )
            } else {
                JsonUtil.fromJson(
                    mmkvPt.decodeString(Constant.ADVERTISING_PT_DATA),
                    object : TypeToken<PtAdBean?>() {}.type
                )
            }
        return adSortingPt(serviceData)
    }

    /**
     * 是否达到阀值
     */
    fun isThresholdReached(): Boolean {
        val clicksCount = mmkvPt.decodeInt(Constant.CLICKS_PT_COUNT, 0)
        val showCount = mmkvPt.decodeInt(Constant.SHOW_PT_COUNT, 0)
        KLog.e("TAG", "clicksCount=${clicksCount}, showCount=${showCount}")
        KLog.e(
            "TAG",
            "pt_click_num=${getAdServerDataPt().pt_click_num}, getAdServerData().pt_show_num=${getAdServerDataPt().pt_show_num}"
        )
        if (clicksCount >= getAdServerDataPt().pt_click_num || showCount >= getAdServerDataPt().pt_show_num) {
            return true
        }
        return false
    }

    /**
     * 记录广告展示次数
     */
    fun recordNumberOfAdDisplaysPt() {
        var showCount = mmkvPt.decodeInt(Constant.SHOW_PT_COUNT, 0)
        showCount++
        MmkvUtils.set(Constant.SHOW_PT_COUNT, showCount)
    }

    /**
     * 记录广告点击次数
     */
    fun recordNumberOfAdClickPt() {
        var clicksCount = mmkvPt.decodeInt(Constant.CLICKS_PT_COUNT, 0)
        clicksCount++
        MmkvUtils.set(Constant.CLICKS_PT_COUNT, clicksCount)
    }

    /**
     * 通过国家获取国旗
     */
    fun getFlagThroughCountryPt(pt_country: String): Int {
        when (pt_country) {
            "Faster server" -> {
                return R.drawable.ic_fast
            }
            "Japan" -> {
                return R.drawable.ic_japan
            }
            "United Kingdom" -> {
                return R.drawable.ic_unitedkingdom
            }
            "United States" -> {
                return R.drawable.ic_usa
            }
            "Australia" -> {
                return R.drawable.ic_australia
            }
            "Belgium" -> {
                return R.drawable.ic_belgium
            }
            "Brazil" -> {
                return R.drawable.ic_brazil
            }
            "Canada" -> {
                return R.drawable.ic_canada
            }
            "France" -> {
                return R.drawable.ic_france
            }
            "Germany" -> {
                return R.drawable.ic_germany
            }
            "India" -> {
                return R.drawable.ic_india
            }
            "Ireland" -> {
                return R.drawable.ic_ireland
            }
            "Italy" -> {
                return R.drawable.ic_italy
            }
            "Koreasouth" -> {
                return R.drawable.ic_koreasouth
            }
            "Netherlands" -> {
                return R.drawable.ic_netherlands
            }
            "Newzealand" -> {
                return R.drawable.ic_newzealand
            }
            "Norway" -> {
                return R.drawable.ic_norway
            }
            "Russianfederation" -> {
                return R.drawable.ic_russianfederation
            }
            "Singapore" -> {
                return R.drawable.ic_singapore
            }
            "Sweden" -> {
                return R.drawable.ic_sweden
            }
            "Switzerland" -> {
                return R.drawable.ic_switzerland
            }
        }

        return R.drawable.ic_fast
    }

    fun referrer(
        context: Context,
    ) {
//        installReferrer = "gclid"
//        installReferrer = "fb4a"
//        MmkvUtils.set(Constant.INSTALL_REFERRER, installReferrer)
        try {
            val referrerClient = InstallReferrerClient.newBuilder(context).build()
            referrerClient.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(p0: Int) {
                    when (p0) {
                        InstallReferrerClient.InstallReferrerResponse.OK -> {
                            installReferrer =
                                referrerClient.installReferrer.installReferrer ?: ""
                            MmkvUtils.set(Constant.INSTALL_REFERRER, installReferrer)
                            KLog.e("TAG", "installReferrer====${installReferrer}")
                            referrerClient.endConnection()
                            return
                        }
                        else -> {
                            referrerClient.endConnection()
                        }
                    }
                }

                override fun onInstallReferrerServiceDisconnected() {
                }
            })
        } catch (e: Exception) {
        }
    }

    fun isFacebookUser(): Boolean {
        val referrer = mmkvPt.decodeString(Constant.INSTALL_REFERRER) ?: ""
        return referrer.contains("fb4a", true)
                || referrer.contains("facebook", true)
    }

    fun isValuableUser(): Boolean {
        val referrer = mmkvPt.decodeString(Constant.INSTALL_REFERRER) ?: ""
        KLog.e("state", "referrer==${referrer}")
        return isFacebookUser()
                || referrer.contains("gclid", true)
                || referrer.contains("not%20set", true)
                || referrer.contains("youtubeads", true)
                || referrer.contains("%7B%22", true)
    }

    /**
     * 埋点
     */
    fun getBuriedPoint(name: String) {
        if (!BuildConfig.DEBUG) {
            Firebase.analytics.logEvent(name, null)
        } else {
            KLog.d(logTagPt, "触发埋点----name=${name}")
        }
    }

    /**
     * 埋点
     */
    fun getBuriedPointUserType(name: String, value: String) {
        if (!BuildConfig.DEBUG) {
            Firebase.analytics.setUserProperty(name, value)
        } else {
            KLog.d(logTagPt, "触发埋点----name=${name}-----value=${value}")
        }
    }

    fun getIpInformation() {
        val sb = StringBuffer()
        try {
            val url = URL("https://ip.seeip.org/geoip/")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            val code = conn.responseCode
            if (code == 200) {
                val `is` = conn.inputStream
                val b = ByteArray(1024)
                var len: Int
                while (`is`.read(b).also { len = it } != -1) {
                    sb.append(String(b, 0, len, Charset.forName("UTF-8")))
                }
                `is`.close()
                conn.disconnect()
                KLog.e("state", "sb==${sb.toString()}")
                MmkvUtils.set(Constant.IP_INFORMATION, sb.toString())
            } else {
                MmkvUtils.set(Constant.IP_INFORMATION, "")
                KLog.e("state", "code==${code.toString()}")
            }
        } catch (var1: Exception) {
            MmkvUtils.set(Constant.IP_INFORMATION, "")
            KLog.e("state", "Exception==${var1.message}")
        }
    }
}