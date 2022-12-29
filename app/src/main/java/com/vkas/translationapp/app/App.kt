package com.vkas.translationapp.app

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.blankj.utilcode.util.ProcessUtils
import com.github.shadowsocks.Core
import com.google.android.gms.ads.AdActivity
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.jeremyliao.liveeventbus.LiveEventBus
import com.tencent.mmkv.MMKV
import com.vkas.translationapp.BuildConfig
import com.vkas.translationapp.base.AppManagerPtMVVM
import com.vkas.translationapp.enevt.Constant
import com.vkas.translationapp.ui.guide.GuideActivity
import com.vkas.translationapp.ui.vpn.VpnActivity
import com.vkas.translationapp.utils.ActivityUtils
import com.vkas.translationapp.utils.CalendarUtils
import com.vkas.translationapp.utils.KLog
import com.vkas.translationapp.utils.MmkvUtils
import com.vkas.translationapp.utils.SkTimerThread.sendTimerInformation
import com.xuexiang.xui.XUI
import com.xuexiang.xutil.XUtil
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class App : Application(), LifecycleObserver {
    private var flag = 0
    private var job_pt : Job? =null
    private var ad_activity_pt: Activity? = null
    private var top_activity_pt: Activity? = null
    companion object {
        // app当前是否在后台
        var isBackDataPt = false

        // 是否进入后台（三秒后）
        var whetherBackgroundPt = false
        // 原生广告刷新
        var nativeAdRefreshPt = false
        val mmkvPt by lazy {
            //启用mmkv的多进程功能
            MMKV.mmkvWithID("Pixel", MMKV.MULTI_PROCESS_MODE)
        }
        //当日日期
        var adDatePt = ""
        /**
        * 判断是否是当天打开
        */
        fun isAppOpenSameDayPt() {
            adDatePt = mmkvPt.decodeString(Constant.CURRENT_PT_DATE, "").toString()
            if (adDatePt == "") {
                MmkvUtils.set(Constant.CURRENT_PT_DATE, CalendarUtils.formatDateNow())
            } else {
                if (CalendarUtils.dateAfterDate(adDatePt, CalendarUtils.formatDateNow())) {
                    MmkvUtils.set(Constant.CURRENT_PT_DATE, CalendarUtils.formatDateNow())
                    MmkvUtils.set(Constant.CLICKS_PT_COUNT, 0)
                    MmkvUtils.set(Constant.SHOW_PT_COUNT, 0)
                }
            }
        }

    }
    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)
//        initCrash()
        setActivityLifecyclePt(this)
        MobileAds.initialize(this) {}
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        if (ProcessUtils.isMainProcess()) {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
            Firebase.initialize(this)
            FirebaseApp.initializeApp(this)
            XUI.init(this) //初始化UI框架
            XUtil.init(this)
            LiveEventBus
                .config()
                .lifecycleObserverAlwaysActive(true)
            //是否开启打印日志
            KLog.init(BuildConfig.DEBUG)
        }
        Core.init(this, VpnActivity::class)
        sendTimerInformation()
        isAppOpenSameDayPt()
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        nativeAdRefreshPt =true
        job_pt?.cancel()
        job_pt = null
        KLog.v("Lifecycle", "onMoveToForeground=$whetherBackgroundPt")
        //从后台切过来，跳转启动页
        if (whetherBackgroundPt&& !isBackDataPt) {
            jumpGuidePage()
        }
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStopState(){
        KLog.v("Lifecycle", "onSTOPJumpPage=$whetherBackgroundPt")
        job_pt = GlobalScope.launch {
            whetherBackgroundPt = false
            delay(3000L)
            whetherBackgroundPt = true
            ad_activity_pt?.finish()
            ActivityUtils.getActivity(GuideActivity::class.java)?.finish()
        }
    }
    /**
     * 跳转引导页
     */
    private fun jumpGuidePage(){
        whetherBackgroundPt = false
        val intent = Intent(top_activity_pt, GuideActivity::class.java)
        intent.putExtra(Constant.RETURN_PT_CURRENT_PAGE, true)
        MmkvUtils.set(Constant.RETURN_PT_CURRENT_PAGE,true)
        top_activity_pt?.startActivity(intent)
    }
    fun setActivityLifecyclePt(application: Application) {
        //注册监听每个activity的生命周期,便于堆栈式管理
        application.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                AppManagerPtMVVM.get().addActivity(activity)
                if (activity !is AdActivity) {
                    top_activity_pt = activity
                } else {
                    ad_activity_pt = activity
                }
                KLog.v("Lifecycle", "onActivityCreated" + activity.javaClass.name)
            }

            override fun onActivityStarted(activity: Activity) {
                KLog.v("Lifecycle", "onActivityStarted" + activity.javaClass.name)
                if (activity !is AdActivity) {
                    top_activity_pt = activity
                } else {
                    ad_activity_pt = activity
                }
                flag++
                isBackDataPt = false
            }

            override fun onActivityResumed(activity: Activity) {
                KLog.v("Lifecycle", "onActivityResumed=" + activity.javaClass.name)
                if (activity !is AdActivity) {
                    top_activity_pt = activity
                }
            }

            override fun onActivityPaused(activity: Activity) {
                if (activity is AdActivity) {
                    ad_activity_pt = activity
                } else {
                    top_activity_pt = activity
                }
                KLog.v("Lifecycle", "onActivityPaused=" + activity.javaClass.name)
            }

            override fun onActivityStopped(activity: Activity) {
                flag--
                if (flag == 0) {
                    isBackDataPt = true
                }
                KLog.v("Lifecycle", "onActivityStopped=" + activity.javaClass.name)
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                KLog.v("Lifecycle", "onActivitySaveInstanceState=" + activity.javaClass.name)

            }

            override fun onActivityDestroyed(activity: Activity) {
                AppManagerPtMVVM.get().removeActivity(activity)
                KLog.v("Lifecycle", "onActivityDestroyed" + activity.javaClass.name)
                ad_activity_pt = null
                top_activity_pt = null
            }
        })
    }
}