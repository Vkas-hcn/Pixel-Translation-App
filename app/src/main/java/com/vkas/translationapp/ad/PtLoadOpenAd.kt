package com.vkas.translationapp.ad

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
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
import com.xuexiang.xutil.net.JsonUtil
import java.util.*

class PtLoadOpenAd {
    companion object {
        fun getInstance() = InstanceHelper.openLoadPt
    }

    object InstanceHelper {
        val openLoadPt = PtLoadOpenAd()
    }

    var appAdDataPt: Any? = null

    // 是否正在加载中
    var isLoadingPt = false

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
        KLog.d(logTagPt, "open--isLoading=${isLoadingPt}")

        if (isLoadingPt) {
            KLog.d(logTagPt, "open--广告加载中，不能再次加载")
            return
        }

        if (appAdDataPt == null) {
            isLoadingPt = true
            loadStartupPageAdvertisementPt(context, getAdServerDataPt())
        }
        if (appAdDataPt != null && !whetherAdExceedsOneHour(loadTimePt)) {
            isLoadingPt = true
            appAdDataPt = null
            loadStartupPageAdvertisementPt(context, getAdServerDataPt())
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
     * 加载启动页广告
     */
    private fun loadStartupPageAdvertisementPt(context: Context, adData: PtAdBean) {
        if (adData.pt_open.getOrNull(adIndexPt)?.pt_type == "screen") {
            loadStartInsertAdPt(context, adData)
        } else {
            loadOpenAdvertisementPt(context, adData)
        }
    }

    /**
     * 加载开屏广告
     */
    private fun loadOpenAdvertisementPt(context: Context, adData: PtAdBean) {
        KLog.e("loadOpenAdvertisementPt", "adData().pt_open=${JsonUtil.toJson(adData.pt_open)}")
        KLog.e(
            "loadOpenAdvertisementPt",
            "id=${JsonUtil.toJson(takeSortedAdIDPt(adIndexPt, adData.pt_open))}"
        )

        val id = takeSortedAdIDPt(adIndexPt, adData.pt_open)

        KLog.d(logTagPt, "open--开屏广告id=$id;权重=${adData.pt_open.getOrNull(adIndexPt)?.pt_weight}")
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            id,
            request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    loadTimePt = Date().time
                    isLoadingPt = false
                    appAdDataPt = ad

                    KLog.d(logTagPt, "open--开屏广告加载成功")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingPt = false
                    appAdDataPt = null
                    if (adIndexPt < adData.pt_open.size - 1) {
                        adIndexPt++
                        loadStartupPageAdvertisementPt(context, adData)
                    } else {
                        adIndexPt = 0
                    }
                    KLog.d(logTagPt, "open--开屏广告加载失败: " + loadAdError.message)
                }
            }
        )
    }


    /**
     * 开屏广告回调
     */
    private fun advertisingOpenCallbackPt() {
        if (appAdDataPt !is AppOpenAd) {
            return
        }
        (appAdDataPt as AppOpenAd).fullScreenContentCallback =
            object : FullScreenContentCallback() {
                //取消全屏内容
                override fun onAdDismissedFullScreenContent() {
                    KLog.d(logTagPt, "open--关闭开屏内容")
                    whetherToShowPt = false
                    appAdDataPt = null
                    if (!App.whetherBackgroundPt) {
                        LiveEventBus.get<Boolean>(Constant.OPEN_CLOSE_JUMP)
                            .post(true)
                    }
                }

                //全屏内容无法显示时调用
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    whetherToShowPt = false
                    appAdDataPt = null
                    KLog.d(logTagPt, "open--全屏内容无法显示时调用")
                }

                //显示全屏内容时调用
                override fun onAdShowedFullScreenContent() {
                    appAdDataPt = null
                    whetherToShowPt = true
                    recordNumberOfAdDisplaysPt()
                    adIndexPt = 0
                    KLog.d(logTagPt, "open---开屏广告展示")
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    KLog.d(logTagPt, "open---点击open广告")
                    recordNumberOfAdClickPt()
                }
            }
    }

    /**
     * 展示Open广告
     */
    fun displayOpenAdvertisementPt(activity: AppCompatActivity): Boolean {

        if (appAdDataPt == null) {
            KLog.d(logTagPt, "open---开屏广告加载中。。。")
            return false
        }
        if (whetherToShowPt || activity.lifecycle.currentState != Lifecycle.State.RESUMED) {
            KLog.d(logTagPt, "open---前一个开屏广告展示中或者生命周期不对")
            return false
        }
        if (appAdDataPt is AppOpenAd) {
            advertisingOpenCallbackPt()
            (appAdDataPt as AppOpenAd).show(activity)
        } else {
            startInsertScreenAdCallbackPt()
            (appAdDataPt as InterstitialAd).show(activity)
        }
        return true
    }

    /**
     * 加载启动页插屏广告
     */
    private fun loadStartInsertAdPt(context: Context, adData: PtAdBean) {
        val adRequest = AdRequest.Builder().build()
        val id = takeSortedAdIDPt(adIndexPt, adData.pt_open)
        KLog.d(
            logTagPt,
            "open--插屏广告id=$id;权重=${adData.pt_open.getOrNull(adIndexPt)?.pt_weight}"
        )

        InterstitialAd.load(
            context,
            id,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    adError.toString().let { KLog.d(logTagPt, "open---连接插屏加载失败=$it") }
                    isLoadingPt = false
                    appAdDataPt = null
                    if (adIndexPt < adData.pt_open.size - 1) {
                        adIndexPt++
                        loadStartupPageAdvertisementPt(context, adData)
                    } else {
                        adIndexPt = 0
                    }
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    loadTimePt = Date().time
                    isLoadingPt = false
                    appAdDataPt = interstitialAd
                    KLog.d(logTagPt, "open--启动页插屏加载完成")
                }
            })
    }

    /**
     * StartInsert插屏广告回调
     */
    private fun startInsertScreenAdCallbackPt() {
        if (appAdDataPt !is InterstitialAd) {
            return
        }
        (appAdDataPt as InterstitialAd).fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    // Called when a click is recorded for an ad.
                    KLog.d(logTagPt, "open--插屏广告点击")
                    recordNumberOfAdClickPt()
                }

                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    KLog.d(logTagPt, "open--关闭StartInsert插屏广告${App.isBackDataPt}")
                    if (!App.whetherBackgroundPt) {
                        LiveEventBus.get<Boolean>(Constant.OPEN_CLOSE_JUMP)
                            .post(true)
                    }
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
                    adIndexPt = 0
                    KLog.d(logTagPt, "open----插屏show")
                }
            }
    }

    /**
     * 展示StartInsert广告
     */
//    fun displayStartInsertAdvertisementPt(activity: AppCompatActivity): Int {
//        if (appAdDataPt !is InterstitialAd) {
//            KLog.e("open------>","展示StartInsert广告")
//
//            displayOpenAdvertisementPt(activity)
//            return 1
//        }
//
//        if (appAdDataPt == null) {
//            KLog.d(logTagPt, "open----插屏广告加载中。。。")
//            return 2
//        }
//        if (whetherToShowPt || activity.lifecycle.currentState != Lifecycle.State.RESUMED) {
//            KLog.d(logTagPt, "open--前一个插屏广告展示中或者生命周期不对")
//            return 2
//        }
//        startInsertScreenAdCallbackPt()
//        (appAdDataPt as InterstitialAd).show(activity)
//        return 3
//    }

}