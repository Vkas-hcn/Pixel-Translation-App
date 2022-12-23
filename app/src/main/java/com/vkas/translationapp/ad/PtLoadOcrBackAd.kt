package com.vkas.translationapp.ad

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.translationapp.app.App
import com.vkas.translationapp.bean.PtAdBean
import com.vkas.translationapp.enevt.Constant
import com.vkas.translationapp.enevt.Constant.logTagPt
import com.vkas.translationapp.utils.KLog
import com.vkas.translationapp.utils.PixelUtils
import com.vkas.translationapp.utils.PixelUtils.getAdServerDataPt
import com.vkas.translationapp.utils.PixelUtils.recordNumberOfAdClickPt
import com.vkas.translationapp.utils.PixelUtils.recordNumberOfAdDisplaysPt
import com.vkas.translationapp.utils.PixelUtils.takeSortedAdIDPt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class PtLoadOcrBackAd {

    companion object {
        fun getInstance() = InstanceHelper.ocrBackLoadPt
    }

    object InstanceHelper {
        val ocrBackLoadPt = PtLoadOcrBackAd()
    }
    var appAdDataPt: InterstitialAd? = null

    // 是否正在加载中
    private var isLoadingPt = false

    //加载时间
    private var loadTimePt: Long = Date().time

    // 是否展示
    var whetherToShowPt = false

    // openIndex
    var adIndexPt = 0


    /**
     * 广告加载前判断
     */
    fun advertisementLoadingPt(context: Context) {
        App.isAppOpenSameDayPt()
        if (PixelUtils.isThresholdReached()) {
            KLog.d(logTagPt, "广告达到上线")
            return
        }
        KLog.d(logTagPt, "ocr_back--isLoading=${isLoadingPt}")

        if (isLoadingPt) {
            KLog.d(logTagPt, "ocr_back--广告加载中，不能再次加载")
            return
        }

        if(appAdDataPt == null){
            isLoadingPt = true
            loadTOcrBackAdvertisementPt(context,getAdServerDataPt())
        }
        if (appAdDataPt != null && !whetherAdExceedsOneHour(loadTimePt)) {
            isLoadingPt = true
            appAdDataPt =null
            loadTOcrBackAdvertisementPt(context,getAdServerDataPt())
        }
    }

    /**
     * 广告是否超过过期（false:过期；true：未过期）
     */
    private fun whetherAdExceedsOneHour(loadTime: Long): Boolean {
        val dateDifference: Long = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour
    }


    /**
     * 加载首页插屏广告
     */
    private fun loadTOcrBackAdvertisementPt(context: Context,adData: PtAdBean) {
        val adRequest = AdRequest.Builder().build()
        val id = takeSortedAdIDPt(adIndexPt, adData.pt_ocr_back)
        KLog.d(logTagPt, "ocr_back--插屏广告id=$id;权重=${adData.pt_ocr_back.getOrNull(adIndexPt)?.pt_weight}")

        InterstitialAd.load(
            context,
            id,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    adError.toString().let { KLog.d(logTagPt, "ocr_back---连接插屏加载失败=$it") }
                    isLoadingPt = false
                    appAdDataPt = null
                    if (adIndexPt < adData.pt_ocr_back.size - 1) {
                        adIndexPt++
                        loadTOcrBackAdvertisementPt(context,adData)
                    }else{
                        adIndexPt = 0
                    }
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    loadTimePt = Date().time
                    isLoadingPt = false
                    appAdDataPt = interstitialAd
                    adIndexPt = 0
                    KLog.d(logTagPt, "ocr_back---连接插屏加载完成")
                }
            })
    }

    /**
     * ocr_back插屏广告回调
     */
    private fun translationBackScreenAdCallback() {
        appAdDataPt?.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    // Called when a click is recorded for an ad.
                    KLog.d(logTagPt, "ocr_back插屏广告点击")
                    recordNumberOfAdClickPt()
                }

                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    KLog.d(logTagPt, "关闭ocr_back插屏广告${App.isBackDataPt}")
                    LiveEventBus.get<Boolean>(Constant.PLUG_PT_OCR_AD_SHOW)
                        .post(App.isBackDataPt)
                    appAdDataPt = null
                    whetherToShowPt = false
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    // Called when ad fails to show.
                    KLog.d(logTagPt, "Ad failed to show fullscreen content.")
                    appAdDataPt = null
                    whetherToShowPt = false
                }

                override fun onAdImpression() {
                    // Called when an impression is recorded for an ad.
                    KLog.e("TAG", "Ad recorded an impression.")
                }

                override fun onAdShowedFullScreenContent() {
                    appAdDataPt = null
                    recordNumberOfAdDisplaysPt()
                    // Called when ad is shown.
                    whetherToShowPt = true
                    KLog.d(logTagPt, "ocr_back----show")
                }
            }
    }

    /**
     * 展示ocr_back广告
     */
    fun displayOcrBackAdvertisementPt(activity: AppCompatActivity): Boolean {
        if (appAdDataPt == null) {
            KLog.d(logTagPt, "ocr_back--插屏广告加载中。。。")
            return false
        }
        if (whetherToShowPt || activity.lifecycle.currentState != Lifecycle.State.RESUMED) {
            KLog.d(logTagPt, "ocr_back--前一个插屏广告展示中或者生命周期不对")
            return false
        }
        translationBackScreenAdCallback()
        activity.lifecycleScope.launch(Dispatchers.Main) {
            (appAdDataPt as InterstitialAd).show(activity)
        }
        return true
    }
}