package com.vkas.translationapp.ad

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.vkas.translationapp.app.App
import com.vkas.translationapp.bean.PtAdBean
import com.vkas.translationapp.databinding.ActivityMainBinding
import com.vkas.translationapp.enevt.Constant.logTagPt
import com.vkas.translationapp.utils.KLog
import com.vkas.translationapp.utils.PixelUtils
import com.vkas.translationapp.utils.PixelUtils.getAdServerDataPt
import com.vkas.translationapp.utils.PixelUtils.recordNumberOfAdClickPt
import com.vkas.translationapp.utils.PixelUtils.takeSortedAdIDPt
import java.util.*
import com.vkas.translationapp.R
import com.vkas.translationapp.databinding.ActivityLanguageBinding
import com.vkas.translationapp.databinding.ActivityTranslationBinding
import com.vkas.translationapp.utils.PixelUtils.recordNumberOfAdDisplaysPt
import com.vkas.translationapp.utils.RoundCornerOutlineProvider

class PtLoadTranslationAd {
    companion object {
        fun getInstance() = InstanceHelper.translationLoadPt
    }

    object InstanceHelper {
        val translationLoadPt = PtLoadTranslationAd()
    }

    var appAdDataPt: NativeAd? = null

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
        KLog.d(logTagPt, "translation--isLoading=${isLoadingPt}")

        if (isLoadingPt) {
            KLog.d(logTagPt, "translation--广告加载中，不能再次加载")
            return
        }
        if (appAdDataPt == null) {
            isLoadingPt = true
            loadAdvertisementPt(context, getAdServerDataPt())
        }
        if (appAdDataPt != null && !whetherAdExceedsOneHour(loadTimePt)) {
            isLoadingPt = true
            appAdDataPt = null
            loadAdvertisementPt(context, getAdServerDataPt())
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
     * 加载translation原生广告
     */
    private fun loadAdvertisementPt(context: Context, adData: PtAdBean) {
        val id = takeSortedAdIDPt(adIndexPt, adData.pt_translation)
        KLog.d(logTagPt, "translation---原生广告id=$id;权重=${adData.pt_translation.getOrNull(adIndexPt)?.pt_weight}")

        val translationNativeAds = AdLoader.Builder(
            context.applicationContext,
            id
        )
        val videoOptions = VideoOptions.Builder()
            .setStartMuted(true)
            .build()

        val adOptions = NativeAdOptions.Builder()
            .setVideoOptions(videoOptions)
            .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_LEFT)
            .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_PORTRAIT)
            .build()

        translationNativeAds.withNativeAdOptions(adOptions)
        translationNativeAds.forNativeAd {
            appAdDataPt = it
        }
        translationNativeAds.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                val error =
                    """
           domain: ${loadAdError.domain}, code: ${loadAdError.code}, message: ${loadAdError.message}
          """"
                isLoadingPt = false
                appAdDataPt = null
                KLog.d(logTagPt, "translation---加载translation原生加载失败: $error")

                if (adIndexPt < adData.pt_translation.size - 1) {
                    adIndexPt++
                    loadAdvertisementPt(context, adData)
                } else {
                    adIndexPt = 0
                }
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                KLog.d(logTagPt, "translation---加载translation原生广告成功")
                loadTimePt = Date().time
                isLoadingPt = false
                adIndexPt = 0
            }

            override fun onAdOpened() {
                super.onAdOpened()
                KLog.d(logTagPt, "translation---点击translation原生广告")
                recordNumberOfAdClickPt()
            }
        }).build().loadAd(AdRequest.Builder().build())
    }

    /**
     * 设置展示translation原生广告
     */
    fun setDisplayNativeAdPt(activity: AppCompatActivity, binding: ActivityTranslationBinding) {
        activity.runOnUiThread {
            appAdDataPt.let {
                if (it != null && !whetherToShowPt && activity.lifecycle.currentState == Lifecycle.State.RESUMED) {
                    val activityDestroyed: Boolean = activity.isDestroyed
                    if (activityDestroyed || activity.isFinishing || activity.isChangingConfigurations) {
                        it.destroy()
                        return@let
                    }
                    val adView = activity.layoutInflater
                        .inflate(R.layout.layout_translation_native, null) as NativeAdView
                    // 对应原生组件
                    setCorrespondingNativeComponentPt(it, adView)
                    binding.ptAdFrame.removeAllViews()
                    binding.ptAdFrame.addView(adView)
                    binding.translationAdPt = true
                    recordNumberOfAdDisplaysPt()
                    whetherToShowPt = true
                    App.nativeAdRefreshPt = false
                    appAdDataPt = null
                    KLog.d(logTagPt, "translation--原生广告--展示")
                    //重新缓存
                    advertisementLoadingPt(activity)
                }
            }

        }
    }


    private fun setCorrespondingNativeComponentPt(nativeAd: NativeAd, adView: NativeAdView) {
        // Set other ad assets.
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)

        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        (adView.headlineView as TextView).text = nativeAd.headline

        if (nativeAd.body == null) {
            adView.bodyView?.visibility = View.INVISIBLE
        } else {
            adView.bodyView?.visibility = View.VISIBLE
            (adView.bodyView as TextView).text = nativeAd.body
        }
        if (nativeAd.callToAction == null) {
            adView.callToActionView?.visibility = View.INVISIBLE
        } else {
            adView.callToActionView?.visibility = View.VISIBLE
            (adView.callToActionView as TextView).text = nativeAd.callToAction
        }

        if (nativeAd.icon == null) {
            adView.iconView?.visibility = View.GONE
        } else {
            (adView.iconView as ImageView).setImageDrawable(
                nativeAd.icon?.drawable
            )
            adView.iconView?.visibility = View.VISIBLE
        }

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad.
        adView.setNativeAd(nativeAd)
    }

    /**
     * 设置展示language原生广告
     */
    fun setDisplayLanguageNativeAdPt(activity: AppCompatActivity, binding: ActivityLanguageBinding) {
        activity.runOnUiThread {
            appAdDataPt.let {
                if (it != null && !whetherToShowPt && activity.lifecycle.currentState == Lifecycle.State.RESUMED) {
                    val activityDestroyed: Boolean = activity.isDestroyed
                    if (activityDestroyed || activity.isFinishing || activity.isChangingConfigurations) {
                        it.destroy()
                        return@let
                    }
                    val adView = activity.layoutInflater
                        .inflate(R.layout.layout_language_native, null) as NativeAdView
                    // 对应原生组件
                    setCorrespondingLanguageNativeComponentPt(it, adView)
                    binding.ptAdFrame.removeAllViews()
                    binding.ptAdFrame.addView(adView)
                    binding.languageAdPt = true
                    recordNumberOfAdDisplaysPt()
                    whetherToShowPt = true
                    App.nativeAdRefreshPt = false
                    appAdDataPt = null
                    KLog.d(logTagPt, "language--原生广告--展示")
                    //重新缓存
                    advertisementLoadingPt(activity)
                }
            }

        }
    }
    private fun setCorrespondingLanguageNativeComponentPt(nativeAd: NativeAd, adView: NativeAdView) {
        adView.mediaView = adView.findViewById(R.id.ad_media)
        // Set other ad assets.
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        (adView.headlineView as TextView).text = nativeAd.headline
        nativeAd.mediaContent?.let {
            adView.mediaView?.apply { setImageScaleType(ImageView.ScaleType.CENTER_CROP) }
                ?.setMediaContent(it)
        }
        adView.mediaView.clipToOutline = true
        adView.mediaView.outlineProvider = RoundCornerOutlineProvider(5f)


        if (nativeAd.callToAction == null) {
            adView.callToActionView?.visibility = View.INVISIBLE
        } else {
            adView.callToActionView?.visibility = View.VISIBLE
            (adView.callToActionView as TextView).text = nativeAd.callToAction
        }

        if (nativeAd.icon == null) {
            adView.iconView?.visibility = View.GONE
        } else {
            (adView.iconView as ImageView).setImageDrawable(
                nativeAd.icon?.drawable
            )
            adView.iconView?.visibility = View.VISIBLE
        }

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad.
        adView.setNativeAd(nativeAd)
    }
}