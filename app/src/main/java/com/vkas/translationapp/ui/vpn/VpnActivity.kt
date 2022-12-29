package com.vkas.translationapp.ui.vpn

import android.content.Intent
import android.os.Bundle
import android.os.RemoteException
import android.view.KeyEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceDataStore
import com.github.shadowsocks.Core
import com.github.shadowsocks.aidl.IShadowsocksService
import com.github.shadowsocks.aidl.ShadowsocksConnection
import com.github.shadowsocks.bg.BaseService
import com.github.shadowsocks.preference.DataStore
import com.github.shadowsocks.preference.OnPreferenceDataStoreChangeListener
import com.github.shadowsocks.utils.Key
import com.github.shadowsocks.utils.StartService
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.gson.reflect.TypeToken
import com.jeremyliao.liveeventbus.LiveEventBus
import com.jeremyliao.liveeventbus.core.Console
import com.vkas.translationapp.BR
import com.vkas.translationapp.R
import com.vkas.translationapp.ad.PtLoadConnectAd
import com.vkas.translationapp.ad.PtLoadResultAd
import com.vkas.translationapp.ad.PtLoadVpnAd
import com.vkas.translationapp.app.App
import com.vkas.translationapp.app.App.Companion.mmkvPt
import com.vkas.translationapp.base.BaseActivity
import com.vkas.translationapp.bean.PtVpnBean
import com.vkas.translationapp.databinding.ActivityVpnBinding
import com.vkas.translationapp.enevt.Constant
import com.vkas.translationapp.enevt.Constant.logTagPt
import com.vkas.translationapp.ui.list.ListPtActivity
import com.vkas.translationapp.ui.result.ResultPtActivity
import com.vkas.translationapp.utils.KLog
import com.vkas.translationapp.utils.MmkvUtils
import com.vkas.translationapp.utils.PixelUtils
import com.vkas.translationapp.utils.PixelUtils.getFlagThroughCountryPt
import com.vkas.translationapp.utils.PixelUtils.isThresholdReached
import com.vkas.translationapp.utils.SkTimerThread
import com.xuexiang.xutil.net.JsonUtil
import com.xuexiang.xutil.net.JsonUtil.toJson
import com.xuexiang.xutil.net.NetworkUtils.isNetworkAvailable
import com.xuexiang.xutil.tip.ToastUtils
import kotlinx.coroutines.*

class VpnActivity : BaseActivity<ActivityVpnBinding, VpnViewModel>(),
    ShadowsocksConnection.Callback,
    OnPreferenceDataStoreChangeListener {
    var state = BaseService.State.Idle

    //重复点击
    var repeatClick = false
    private var jobRepeatClick: Job? = null

    // 跳转结果页
    private var liveJumpResultsPage = MutableLiveData<Bundle>()
    private val connection = ShadowsocksConnection(true)

    // 是否返回刷新服务器
    var whetherRefreshServer = false
    private var jobNativeAdsPt: Job? = null
    private var jobStartPt: Job? = null
    private var automaticConnection: Boolean = false
    private var whetherEmptyCache: Boolean = false

    //当前执行连接操作
    private var performConnectionOperations: Boolean = false

    companion object {
        var stateListener: ((BaseService.State) -> Unit)? = null
    }

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_vpn
    }

    override fun initVariableId(): Int {
        return BR._all
    }

    override fun initParam() {
        super.initParam()
        val bundle = intent.extras
        automaticConnection = bundle?.getBoolean(Constant.AUTOMATIC_CONNECTION, false) == true
        whetherEmptyCache = bundle?.getBoolean(Constant.WHETHER_EMPTY_CACHE, false) == true
    }

    override fun initToolbar() {
        super.initToolbar()
        binding.presenter = PtClick()
        liveEventBusReceive()
        binding.mainTitlePt.imgBack.setImageResource(R.drawable.ic_title_back)
        binding.mainTitlePt.tvTitle.text = getString(R.string.vpn)
        binding.mainTitlePt.tvRight.visibility = View.GONE
        binding.mainTitlePt.imgBack.setOnClickListener {
            finish()
        }
    }

    private fun liveEventBusReceive() {
        LiveEventBus
            .get(Constant.TIMER_PT_DATA, String::class.java)
            .observeForever {
                binding.txtTimerPt.text = it
            }
        //更新服务器(未连接)
        LiveEventBus
            .get(Constant.NOT_CONNECTED_RETURN, PtVpnBean::class.java)
            .observeForever {
                viewModel.updateSkServer(it, false)
            }
        //更新服务器(已连接)
        LiveEventBus
            .get(Constant.CONNECTED_RETURN, PtVpnBean::class.java)
            .observeForever {
                viewModel.updateSkServer(it, true)
            }
        //插屏关闭后跳转
        LiveEventBus
            .get(Constant.PLUG_PT_ADVERTISEMENT_SHOW, Boolean::class.java)
            .observeForever {
                // 如果是B方案连接成功关闭插屏，不需要重新加载插屏广告，之后会全部清空加载
                if (!whetherEmptyCache) {
                    PtLoadConnectAd.getInstance().advertisementLoadingPt(this)
                }
                KLog.e("state", "插屏关闭接收=${it}")

                //重复点击
                jobRepeatClick = lifecycleScope.launch {
                    if (!repeatClick) {
                        KLog.e("state", "插屏关闭后跳转=${it}")
                        connectOrDisconnectPt(it)
                        repeatClick = true
                    }
                    delay(1000)
                    repeatClick = false
                }
            }
    }

    override fun initData() {
        super.initData()
        changeState(BaseService.State.Idle, animate = false)
        connection.connect(this, this)
        DataStore.publicStore.registerChangeListener(this)
        if (SkTimerThread.isStopThread) {
            viewModel.initializeServerData()
        } else {
            val serviceData = mmkvPt.decodeString("currentServerData", "").toString()
            val currentServerData: PtVpnBean = JsonUtil.fromJson(
                serviceData,
                object : TypeToken<PtVpnBean?>() {}.type
            )
            setFastInformation(currentServerData)
        }
        PtLoadVpnAd.getInstance().whetherToShowPt = false
        initHomeAd()
        if (automaticConnection) {
            connect.launch(null)
        }
    }

    private fun initHomeAd() {
        jobNativeAdsPt = lifecycleScope.launch {
            while (isActive) {
                PtLoadVpnAd.getInstance().setDisplayHomeNativeAdPt(this@VpnActivity, binding)
                if (PtLoadVpnAd.getInstance().whetherToShowPt) {
                    jobNativeAdsPt?.cancel()
                    jobNativeAdsPt = null
                }
                delay(1000L)
            }
        }
    }

    override fun initViewObservable() {
        super.initViewObservable()
        // 跳转结果页
        jumpResultsPageData()
        setServiceData()
    }

    private fun jumpResultsPageData() {
        liveJumpResultsPage.observe(this, {
            lifecycleScope.launch(Dispatchers.Main.immediate) {
                delay(300L)
                if (lifecycle.currentState == Lifecycle.State.RESUMED) {
                    it.putBoolean(Constant.WHETHER_EMPTY_CACHE, whetherEmptyCache)
                    startActivityForResult(ResultPtActivity::class.java, 0x11, it)
                }
            }
        })
        viewModel.liveJumpResultsPage.observe(this, {
            liveJumpResultsPage.postValue(it)
        })
    }

    private fun setServiceData() {
        viewModel.liveInitializeServerData.observe(this, {
            setFastInformation(it)
        })
        viewModel.liveUpdateServerData.observe(this, {
            whetherRefreshServer = true
            connect.launch(null)
        })
        viewModel.liveNoUpdateServerData.observe(this, {
            whetherRefreshServer = false
            setFastInformation(it)
            connect.launch(null)
        })
    }

    inner class PtClick {
        fun linkService() {
            if (binding.vpnState != 1) {
                if (state.name == "Stopped") {
                    KLog.e("state", "连接点击")
                    PixelUtils.getBuriedPoint("pixel_vpn_link")
                }
                connect.launch(null)
            }
        }

        fun clickService() {
            if (binding.vpnState != 1) {
                jumpToServerList()
            }
        }
    }

    /**
     * 跳转服务器列表
     */
    fun jumpToServerList() {
        lifecycleScope.launch {
            if (lifecycle.currentState != Lifecycle.State.RESUMED) {
                return@launch
            }
            val bundle = Bundle()
            if (state.name == "Connected") {
                bundle.putBoolean(Constant.WHETHER_PT_CONNECTED, true)
            } else {
                bundle.putBoolean(Constant.WHETHER_PT_CONNECTED, false)
            }
            val serviceData = mmkvPt.decodeString("currentServerData", "").toString()
            bundle.putString(Constant.CURRENT_PT_SERVICE, serviceData)
            startActivity(ListPtActivity::class.java, bundle)
        }
    }

    /**
     * 设置fast信息
     */
    private fun setFastInformation(ptVpnBean: PtVpnBean) {
        if (ptVpnBean.pt_best == true) {
            binding.txtCountry.text = Constant.FASTER_PT_SERVER
            binding.imgCountry.setImageResource(getFlagThroughCountryPt(Constant.FASTER_PT_SERVER))
        } else {
            binding.txtCountry.text =
                String.format(ptVpnBean.pt_country + "-" + ptVpnBean.pt_city)
            binding.imgCountry.setImageResource(getFlagThroughCountryPt(ptVpnBean.pt_country.toString()))
        }
    }

    private val connect = registerForActivityResult(StartService()) {
        if (it) {
            ToastUtils.toast(R.string.no_permissions)
        } else {
            PixelUtils.getBuriedPoint("pixel_v_jurisdiction")
            if (isNetworkAvailable()) {
                startVpn()
            } else {
                ToastUtils.toast("The current device has no network")
            }
        }
    }

    /**
     * 启动VPN
     */
    private fun startVpn() {
        binding.vpnState = 1
        changeOfVpnStatus()
        App.isAppOpenSameDayPt()
        if (isThresholdReached()) {
            KLog.d(logTagPt, "广告达到上线")
            connectOrDisconnectPt(false)
            return
        }
        PtLoadConnectAd.getInstance().advertisementLoadingPt(this)
        PtLoadResultAd.getInstance().advertisementLoadingPt(this)

        jobStartPt = lifecycleScope.launch {
            try {
                withTimeout(10000L) {
                    delay(2000L)
                    KLog.e(logTagPt, "jobStartPt?.isActive=${jobStartPt?.isActive}")
                    while (jobStartPt?.isActive == true) {
                        val showState =
                            PtLoadConnectAd.getInstance()
                                .displayConnectAdvertisementPt(this@VpnActivity)
                        if (showState) {
                            jobStartPt?.cancel()
                            jobStartPt = null
                        }
                        delay(1000L)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                KLog.d(logTagPt, "connect---插屏超时")
                if (jobStartPt != null) {
                    connectOrDisconnectPt(false)
                }
            }
        }
    }

    /**
     * 连接或断开
     * 是否后台关闭（true：后台关闭；false：手动关闭）
     */
    private fun connectOrDisconnectPt(isBackgroundClosed: Boolean) {
        KLog.e("state", "连接或断开")
        if (state.canStop) {
            if (!isBackgroundClosed) {
                viewModel.jumpConnectionResultsPage(false)
            }
            Core.stopService()
            performConnectionOperations = false
        } else {
            if (!isBackgroundClosed) {
                viewModel.jumpConnectionResultsPage(true)
            }
            Core.startService()
            performConnectionOperations = true
            PixelUtils.getBuriedPoint("pixel_v_link")
        }
    }

    private fun changeState(
        state: BaseService.State,
        animate: Boolean = true
    ) {
        this.state = state
        connectionStatusJudgment(state.name)
        stateListener?.invoke(state)
    }

    /**
     * 连接状态判断
     */
    private fun connectionStatusJudgment(state: String) {
        KLog.e("TAG", "connectionStatusJudgment=${state}")
        if (performConnectionOperations && state != "Connected") {
            //vpn连接失败
            KLog.d(logTagPt, "vpn连接失败")
            ToastUtils.toast(getString(R.string.connected_failed))
            PixelUtils.getBuriedPoint("pixel_v_fail_clcik")
        }
        when (state) {
            "Connected" -> {
                // 连接成功
                connectionServerSuccessful()
            }
            "Stopped" -> {
                disconnectServerSuccessful()
            }
            else -> {
//                binding.txtConnectionStatusPt.text = getString(R.string.connect)
            }
        }
    }

    /**
     * 连接服务器成功
     */
    private fun connectionServerSuccessful() {
        KLog.e("TAG", "连接服务器成功=whetherEmptyCache=${whetherEmptyCache}")
        PixelUtils.getBuriedPoint("pixel_v_succ_link")
        if (whetherEmptyCache) {
            //B方案清空广告缓存重新加载
            viewModel.emptyAdvertisementCacheAndReload(this)
        }
        binding.vpnState = 2
        changeOfVpnStatus()
    }

    /**
     * 断开服务器
     */
    private fun disconnectServerSuccessful() {
        KLog.e("TAG", "断开服务器")
        binding.vpnState = 0
        changeOfVpnStatus()
    }

    /**
     * vpn状态变化
     */
    private fun changeOfVpnStatus() {
        when (binding.vpnState) {
            0 -> {
                binding.imgState.setImageResource(R.drawable.ic_vpn_swich)
                binding.frState.background =
                    ContextCompat.getDrawable(this, R.drawable.bg_vpn_switch)
                binding.txtTimerPt.text = getString(R.string._00_00_00)
                SkTimerThread.endTiming()
                binding.lavViewPt.pauseAnimation()
                binding.lavViewPt.visibility = View.GONE
            }
            1 -> {
                binding.frState.background =
                    ContextCompat.getDrawable(this, R.drawable.bg_vpn_switch)
                binding.lavViewPt.setAnimation("data_connect.json")
                binding.lavViewPt.visibility = View.VISIBLE
                binding.lavViewPt.playAnimation()
            }
            2 -> {
                binding.imgState.setImageResource(R.drawable.ic_vpn_turn)
                binding.frState.background =
                    ContextCompat.getDrawable(this, R.drawable.bg_vpn_switch_chek)
                binding.lavViewPt.setAnimation("data_success.json")
                SkTimerThread.startTiming()
                binding.lavViewPt.visibility = View.VISIBLE
                binding.lavViewPt.playAnimation()
            }
        }
    }

    override fun stateChanged(state: BaseService.State, profileName: String?, msg: String?) {
        changeState(state)
    }

    override fun onServiceConnected(service: IShadowsocksService) {
        changeState(
            try {
                BaseService.State.values()[service.state]
            } catch (_: RemoteException) {
                BaseService.State.Idle
            }
        )
    }

    override fun onPreferenceDataStoreChanged(store: PreferenceDataStore, key: String) {
        when (key) {
            Key.serviceMode -> {
                connection.disconnect(this)
                connection.connect(this, this)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        connection.bandwidthTimeout = 500
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            delay(300)
            if (lifecycle.currentState != Lifecycle.State.RESUMED) {
                return@launch
            }
            if (App.nativeAdRefreshPt) {
                if (viewModel.afterDisconnectionServerData.pt_ip == null) {
                    setFastInformation(viewModel.currentServerData)
                } else {
                    setFastInformation(viewModel.afterDisconnectionServerData)
                }

                PtLoadVpnAd.getInstance().whetherToShowPt = false
                if (PtLoadVpnAd.getInstance().appAdDataPt != null) {
                    KLog.d(logTagPt, "onResume------>1")
                    PtLoadVpnAd.getInstance().setDisplayHomeNativeAdPt(this@VpnActivity, binding)
                } else {
                    binding.vpnAdPt = false
                    KLog.d(logTagPt, "onResume------>2")
                    PtLoadVpnAd.getInstance().advertisementLoadingPt(this@VpnActivity)
                    initHomeAd()
                }
            }
        }

    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        connection.bandwidthTimeout = 0
    }

    override fun onDestroy() {
        super.onDestroy()
        LiveEventBus
            .get(Constant.PLUG_PT_ADVERTISEMENT_SHOW, Boolean::class.java)
            .removeObserver {}
        DataStore.publicStore.unregisterChangeListener(this)
        connection.disconnect(this)
        jobStartPt?.cancel()
        jobStartPt = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0x11 && whetherRefreshServer) {
            setFastInformation(viewModel.afterDisconnectionServerData)
            val serviceData = toJson(viewModel.afterDisconnectionServerData)
            MmkvUtils.set("currentServerData", serviceData)
            viewModel.currentServerData = viewModel.afterDisconnectionServerData
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish()
        }
        return true
    }
}