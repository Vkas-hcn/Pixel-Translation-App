package com.vkas.translationapp.ui.guide

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.translationapp.BR
import com.vkas.translationapp.BuildConfig
import com.vkas.translationapp.R
import com.vkas.translationapp.ad.*
import com.vkas.translationapp.app.App
import com.vkas.translationapp.base.BaseActivity
import com.vkas.translationapp.bean.PtAdBean
import com.vkas.translationapp.databinding.ActivityGuideBinding
import com.vkas.translationapp.enevt.Constant
import com.vkas.translationapp.enevt.Constant.logTagPt
import com.vkas.translationapp.ui.main.MainActivity
import com.vkas.translationapp.utils.KLog
import com.vkas.translationapp.utils.MmkvUtils
import com.vkas.translationapp.utils.PixelUtils
import com.vkas.translationapp.utils.PixelUtils.getAdServerDataPt
import com.vkas.translationapp.utils.PixelUtils.isThresholdReached
import com.xuexiang.xui.widget.progress.HorizontalProgressView
import com.xuexiang.xutil.resource.ResourceUtils
import kotlinx.coroutines.*

class GuideActivity : BaseActivity<ActivityGuideBinding, GuideViewModel>(),
    HorizontalProgressView.HorizontalProgressUpdateListener {
    companion object {
        var isCurrentPage: Boolean = false
    }
    private var liveJumpHomePage = MutableLiveData<Boolean>()
    private var liveJumpHomePage2 = MutableLiveData<Boolean>()
    private var jobOpenAdsPt: Job? = null

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_guide
    }

    override fun initVariableId(): Int {
        return BR._all
    }

    override fun initParam() {
        super.initParam()
        isCurrentPage = intent.getBooleanExtra(Constant.RETURN_PT_CURRENT_PAGE, false)

    }

    override fun initToolbar() {
        super.initToolbar()
    }

    override fun initData() {
        super.initData()
        binding.pbStartPt.setProgressViewUpdateListener(this)
        binding.pbStartPt.startProgressAnimation()
        liveEventBusPt()
        getFirebaseDataPt()
        jumpHomePageData()
    }
    private fun liveEventBusPt() {
        LiveEventBus
            .get(Constant.OPEN_CLOSE_JUMP, Boolean::class.java)
            .observeForever {
                KLog.d(logTagPt, "关闭开屏内容-接收==${this.lifecycle.currentState}")
                if (this.lifecycle.currentState == Lifecycle.State.STARTED) {
                    jumpPage()
                }
            }
    }

    private fun getFirebaseDataPt() {
        PixelUtils.referrer(this)
        if (BuildConfig.DEBUG) {
            preloadedAdvertisement()
//            MmkvUtils.set(Constant.PIXEL_SET, "2")
//
//            MmkvUtils.set(Constant.PIXEL_SET_P, "1")
//
//            MmkvUtils.set(Constant.PIXEL_ABT, "100")

//            lifecycleScope.launch {
//                delay(500)
//                MmkvUtils.set(Constant.ADVERTISING_PT_DATA, ResourceUtils.readStringFromAssert("ptAdDataFireBase.json"))
//            }
            return
        } else {
            preloadedAdvertisement()
            val auth = Firebase.remoteConfig
            auth.fetchAndActivate().addOnSuccessListener {
                MmkvUtils.set(Constant.PROFILE_PT_DATA, auth.getString("PtServiceData"))
                MmkvUtils.set(Constant.PROFILE_PT_DATA_FAST, auth.getString("PtServiceDataFast"))
                MmkvUtils.set(Constant.AROUND_PT_FLOW_DATA, auth.getString("PtAroundFlow_Data"))
                MmkvUtils.set(Constant.ADVERTISING_PT_DATA, auth.getString("PtAd_Data"))

                MmkvUtils.set(Constant.PIXEL_SET, auth.getString(Constant.PIXEL_SET))
                MmkvUtils.set(Constant.PIXEL_SET_P, auth.getString(Constant.PIXEL_SET_P))
                MmkvUtils.set(Constant.PIXEL_ABT, auth.getString(Constant.PIXEL_ABT))

            }
        }
    }

    override fun initViewObservable() {
        super.initViewObservable()
    }

    private fun jumpHomePageData() {
        liveJumpHomePage2.observe(this, {
            lifecycleScope.launch(Dispatchers.Main.immediate) {
                KLog.e("TAG", "isBackDataPt==${App.isBackDataPt}")
                delay(300)
                if (lifecycle.currentState == Lifecycle.State.RESUMED) {
                    jumpPage()
                }
            }
        })
        liveJumpHomePage.observe(this, {
            liveJumpHomePage2.postValue(true)
        })
    }

    /**
     * 跳转页面
     */
    private fun jumpPage() {
        // 不是后台切回来的跳转，是后台切回来的直接finish启动页
        if (!isCurrentPage) {
            val intent = Intent(this@GuideActivity, MainActivity::class.java)
            startActivity(intent)
        }
        finish()

    }
    /**
     * 加载广告
     */
    private fun loadAdvertisement() {
        //开屏
        PtLoadOpenAd.getInstance().adIndexPt = 0
        PtLoadOpenAd.getInstance().advertisementLoadingPt(this)
        rotationDisplayOpeningAdPt()

        PtLoadHomeAd.getInstance().adIndexPt = 0
        PtLoadHomeAd.getInstance().advertisementLoadingPt(this)

        PtLoadTranslationAd.getInstance().adIndexPt = 0
        PtLoadTranslationAd.getInstance().advertisementLoadingPt(this)

        PtLoadBackAd.getInstance().adIndexPt = 0
        PtLoadBackAd.getInstance().advertisementLoadingPt(this)

        PtLoadVpnAd.getInstance().adIndexPt = 0
        PtLoadVpnAd.getInstance().advertisementLoadingPt(this)
        PtLoadResultAd.getInstance().adIndexPt = 0
        PtLoadResultAd.getInstance().advertisementLoadingPt(this)
        PtLoadConnectAd.getInstance().adIndexPt = 0
        PtLoadConnectAd.getInstance().advertisementLoadingPt(this)
    }
    /**
     * 轮训展示开屏广告
     */
    private fun rotationDisplayOpeningAdPt() {
        jobOpenAdsPt = lifecycleScope.launch {
            try {
                withTimeout(8000L) {
                    delay(1000L)
                    while (isActive) {
                        val showState = PtLoadOpenAd.getInstance()
                            .displayOpenAdvertisementPt(this@GuideActivity)
                        if (showState) {
                            jobOpenAdsPt?.cancel()
                            jobOpenAdsPt =null
                        }
                        delay(1000L)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                KLog.e("TimeoutCancellationException I'm sleeping $e")
                jumpPage()
            }
        }
    }
    /**
     * 预加载广告
     */
    private fun preloadedAdvertisement() {
        App.isAppOpenSameDayPt()
        if (isThresholdReached()) {
            KLog.d(logTagPt, "广告达到上线")
            lifecycleScope.launch {
                delay(2000L)
                liveJumpHomePage.postValue(true)
            }
        } else {
            loadAdvertisement()
        }
    }
    override fun onHorizontalProgressStart(view: View?) {
    }

    override fun onHorizontalProgressUpdate(view: View?, progress: Float) {
    }

    override fun onHorizontalProgressFinished(view: View?) {
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return keyCode == KeyEvent.KEYCODE_BACK
    }
}