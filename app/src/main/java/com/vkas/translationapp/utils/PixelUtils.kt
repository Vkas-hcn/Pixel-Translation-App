package com.vkas.translationapp.utils

import com.google.gson.reflect.TypeToken
import com.vkas.translationapp.R
import com.vkas.translationapp.app.App.Companion.mmkvPt
import com.vkas.translationapp.bean.PtAdBean
import com.vkas.translationapp.bean.PtDetailBean
import com.vkas.translationapp.bean.PtVpnBean
import com.vkas.translationapp.enevt.Constant
import com.xuexiang.xui.utils.Utils
import com.xuexiang.xutil.net.JsonUtil
import com.xuexiang.xutil.resource.ResUtils.getString
import com.xuexiang.xutil.resource.ResourceUtils

object PixelUtils {
    /**
     * 获取Fast ip
     */
    fun getFastIpPt(): PtVpnBean {
        val ptVpnBean: MutableList<PtVpnBean> = getLocalServerData()
        var intersectionList = findFastAndOrdinaryIntersection(ptVpnBean)
        if(intersectionList.size<=0){
            intersectionList =ptVpnBean
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
        val intersectionList:MutableList<PtVpnBean> =ArrayList()
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
        val adBean:PtAdBean = PtAdBean()
        val ptOpen = ptAdBean.pt_open.sortedWith(compareByDescending { it.pt_weight })
        val ptHome = ptAdBean.pt_home.sortedWith(compareByDescending { it.pt_weight })
        val ptTranslation = ptAdBean.pt_translation.sortedWith(compareByDescending { it.pt_weight })
        val ptBack = ptAdBean.pt_back.sortedWith(compareByDescending { it.pt_weight })

        adBean.pt_open = ptOpen.toMutableList()
        adBean.pt_home = ptHome.toMutableList()
        adBean.pt_translation = ptTranslation.toMutableList()
        adBean.pt_back = ptBack.toMutableList()

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
    fun isThresholdReached():Boolean{
        val clicksCount = mmkvPt.decodeInt(Constant.CLICKS_PT_COUNT, 0)
        val showCount = mmkvPt.decodeInt(Constant.SHOW_PT_COUNT, 0)
        KLog.e("TAG","clicksCount=${clicksCount}, showCount=${showCount}")
        KLog.e("TAG","pt_click_num=${getAdServerDataPt().pt_click_num}, getAdServerData().pt_show_num=${getAdServerDataPt().pt_show_num}")
        if (clicksCount >= getAdServerDataPt().pt_click_num || showCount >= getAdServerDataPt().pt_show_num) {
            return true
        }
        return false
    }

    /**
     * 记录广告展示次数
     */
    fun recordNumberOfAdDisplaysPt(){
        var showCount = mmkvPt.decodeInt(Constant.SHOW_PT_COUNT, 0)
        showCount++
        MmkvUtils.set(Constant.SHOW_PT_COUNT, showCount)
    }
    /**
     * 记录广告点击次数
     */
    fun recordNumberOfAdClickPt(){
        var clicksCount = mmkvPt.decodeInt(Constant.CLICKS_PT_COUNT, 0)
        clicksCount++
        MmkvUtils.set(Constant.CLICKS_PT_COUNT, clicksCount)
    }
}