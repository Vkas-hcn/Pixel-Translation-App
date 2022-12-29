package com.vkas.translationapp.ui.main

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.RemoteException
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceDataStore
import com.github.shadowsocks.aidl.IShadowsocksService
import com.github.shadowsocks.aidl.ShadowsocksConnection
import com.github.shadowsocks.bg.BaseService
import com.github.shadowsocks.preference.OnPreferenceDataStoreChangeListener
import com.github.shadowsocks.utils.Key
import com.github.shadowsocks.utils.StartService
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.vkas.translationapp.BR
import com.vkas.translationapp.R
import com.vkas.translationapp.ad.PtLoadHomeAd
import com.vkas.translationapp.ad.PtLoadVpnAd
import com.vkas.translationapp.app.App
import com.vkas.translationapp.app.App.Companion.mmkvPt
import com.vkas.translationapp.base.BaseActivity
import com.vkas.translationapp.databinding.ActivityMainBinding
import com.vkas.translationapp.enevt.Constant
import com.vkas.translationapp.enevt.Constant.logTagPt
import com.vkas.translationapp.ui.camare.CameraXActivity
import com.vkas.translationapp.ui.translation.TranslationActivity
import com.vkas.translationapp.ui.vpn.VpnActivity
import com.vkas.translationapp.ui.web.WebPtActivity
import com.vkas.translationapp.utils.KLog
import com.vkas.translationapp.utils.MmkvUtils
import com.vkas.translationapp.utils.PixelUtils
import com.xuexiang.xui.utils.Utils
import com.xuexiang.xutil.net.NetworkUtils
import com.xuexiang.xutil.tip.ToastUtils
import kotlinx.coroutines.*
import java.util.*
import kotlin.random.Random


class MainActivity : BaseActivity<ActivityMainBinding, MainViewModel>(),
    ShadowsocksConnection.Callback,
    OnPreferenceDataStoreChangeListener {
    private var jobNativeAdsPt: Job? = null
    var state = BaseService.State.Idle
    private val connection = ShadowsocksConnection(true)
    private var dialogVpnState: Boolean = false

    //是否执行A方案
    private var whetherToImplementPlanA = false

    companion object {
        var stateListener: ((BaseService.State) -> Unit)? = null
    }

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_main
    }

    override fun initVariableId(): Int {
        return BR._all
    }

    override fun initParam() {
        super.initParam()
    }

    override fun initToolbar() {
        super.initToolbar()
        binding.presenter = Presenter()
        binding.inMainTitlePt.let {
            it.imgBack.visibility = View.VISIBLE
            it.tvTitle.visibility = View.GONE
        }
        binding.inMainTitlePt.imgBack.setOnClickListener {
            binding.sidebarShowsPt = binding.sidebarShowsPt != true
        }
    }

    override fun initData() {
        super.initData()
        judgeVpnScheme()
        changeState(BaseService.State.Idle, animate = false)
        connection.connect(this, this)
        PtLoadHomeAd.getInstance().whetherToShowPt = false
        PtLoadHomeAd.getInstance().advertisementLoadingPt(this)
        initHomeAd()
        //A方案冷启动
        if (whetherToImplementPlanA) {
            lifecycleScope.launch {
                delay(300)
                vpnGuidePopUpWindow()
            }
        }
    }

    override fun initViewObservable() {
        super.initViewObservable()
    }

    private fun initHomeAd() {
        KLog.d(logTagPt, "MainActivity----initHomeAd")
        jobNativeAdsPt = lifecycleScope.launch {
            while (isActive) {
                PtLoadHomeAd.getInstance().setDisplayHomeNativeAdPt(this@MainActivity, binding)
                if (PtLoadHomeAd.getInstance().whetherToShowPt) {
                    jobNativeAdsPt?.cancel()
                    jobNativeAdsPt = null
                }
                delay(1000L)
            }
        }
    }

    inner class Presenter {
        fun clickOcr() {
            val hasWriteStoragePermission: Int =
                ContextCompat.checkSelfPermission(application, Manifest.permission.CAMERA)
            if (hasWriteStoragePermission == PackageManager.PERMISSION_GRANTED) {
                startActivity(CameraXActivity::class.java)
            } else {
                //没有权限，向用户请求权限
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.CAMERA),
                    1
                )
            }
        }

        fun clickTranslation() {
            startActivity(TranslationActivity::class.java)
        }

        fun clickVpn() {
            automaticConnectionJump(automaticConnection = false, whetherEmptyCache = false)
        }

        fun clickMainMenu() {}
        fun clickMain() {
            if (binding.sidebarShowsPt == true) {
                binding.sidebarShowsPt = false
            }
        }

        fun toContactUs() {
            val uri = Uri.parse("mailto:${Constant.MAILBOX_PT_ADDRESS}")
            val intent = Intent(Intent.ACTION_SENDTO, uri)
            runCatching {
                startActivity(intent)
            }.onFailure {
                ToastUtils.toast("Please send your problem to our email:${Constant.MAILBOX_PT_ADDRESS}")
            }
        }

        fun toPrivacyPolicy() {
            startActivity(WebPtActivity::class.java)
        }

        fun toShare() {
            val intent = Intent()
            intent.action = Intent.ACTION_SEND
            intent.putExtra(
                Intent.EXTRA_TEXT,
                Constant.SHARE_PT_ADDRESS + this@MainActivity.packageName
            )
            intent.type = "text/plain"
            startActivity(intent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startActivity(CameraXActivity::class.java)
            } else {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.CAMERA
                    )
                ) {
                    denyPermissionPopUp()
                }
            }
        }
    }

    /**
     * 拒绝权限弹框
     */
    private fun denyPermissionPopUp() {
        val dialog: AlertDialog? = AlertDialog.Builder(this)
            .setTitle(getString(R.string.permission_title))
            .setMessage(getString(R.string.permission_message))
            //设置对话框的按钮
            .setNegativeButton("CANCEL") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("SET UP") { dialog, _ ->
                dialog.dismiss()
                goSystemSetting()
            }.create()
        dialog?.show()
        dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
        dialog?.getButton(DialogInterface.BUTTON_NEGATIVE)?.setTextColor(Color.BLACK)
    }

    /**
     * 自动连接跳转
     * 是否AUTOMATIC_CONNECTION
     */
    private fun automaticConnectionJump(
        automaticConnection: Boolean,
        whetherEmptyCache: Boolean = false
    ) {
        PtLoadVpnAd.getInstance().advertisementLoadingPt(this)
        val bundle = Bundle()
        bundle.putBoolean(Constant.AUTOMATIC_CONNECTION, automaticConnection)
        //是否清空缓存
        bundle.putBoolean(Constant.WHETHER_EMPTY_CACHE, whetherEmptyCache)
        startActivity(VpnActivity::class.java, bundle)
    }

    /**
     * vpn引导弹窗(A方案)
     */
    private fun vpnGuidePopUpWindow() {
        if (!dialogVpnState && lifecycle.currentState == Lifecycle.State.RESUMED && state.name == "Stopped" && viewModel.getVpnBootPopUpWindow()) {
            val dialogVpn: AlertDialog = AlertDialog.Builder(this)
                .setTitle(getString(R.string.vpn))
                .setMessage(getString(R.string.turn_vpn))
                //设置对话框的按钮
                .setNegativeButton("CANCEL") { dialog, _ ->
                    dialog.dismiss()
                    dialogVpnState = false
                    PixelUtils.getBuriedPoint("pixel_v_close_pop")
                }
                .setPositiveButton("Turn on") { dialog, _ ->
                    dialog.dismiss()
                    dialogVpnState = false
                    automaticConnectionJump(automaticConnection = true, whetherEmptyCache = false)
                    PixelUtils.getBuriedPoint("pixel_v_click_pop")
                }.create()
            dialogVpn.setCancelable(false)
            dialogVpn.show()
            dialogVpnState = true
            PixelUtils.getBuriedPoint("pixel_v_show_pop")
            dialogVpn.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
            dialogVpn.getButton(DialogInterface.BUTTON_NEGATIVE)?.setTextColor(Color.BLACK)
        }
    }

    /**
     * vpn B 方案
     */
    private fun vpnBScheme() {
        lifecycleScope.launch {
            delay(300)
            connect.launch(null)
        }
    }

    /**
     * vpn C 方案
     * 概率
     */
    private fun vpnCScheme(mProbability: String) {
        val mProbabilityInt = mProbability.toIntOrNull()
        if (mProbabilityInt == null) {
            whetherToImplementPlanA = true
        } else {
            val random = (0..100).shuffled().last()

            when {
                random <= mProbabilityInt -> {
                    //B
                    KLog.d(logTagPt,"随机落在B方案")
                    vpnBScheme() //20，代表20%为B用户；80%为A用户
                    PixelUtils.getBuriedPointUserType("pixel_user_type", "b")

                }
                else -> {
                    //A
                    KLog.d(logTagPt,"随机落在A方案")
                    whetherToImplementPlanA = true
                    PixelUtils.getBuriedPointUserType("pixel_user_type", "a")
                }
            }
        }

    }

    private fun goSystemSetting() {
        val intent = Intent().apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            action = "android.settings.APPLICATION_DETAILS_SETTINGS"
            data = Uri.fromParts("package", this@MainActivity.packageName, null)
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            delay(300)
            if (lifecycle.currentState != Lifecycle.State.RESUMED) {
                return@launch
            }
            KLog.d(logTagPt, "MainActivity----onResume-nativeAdRefreshPt=${App.nativeAdRefreshPt}")
            if (App.nativeAdRefreshPt) {
                //A方案热启动
                if (whetherToImplementPlanA) {
                    whetherVpnBootBoxIsDisplayed()
                }

                PtLoadHomeAd.getInstance().whetherToShowPt = false
                if (PtLoadHomeAd.getInstance().appAdDataPt != null) {
                    KLog.d(logTagPt, "onResume------>1")
                    PtLoadHomeAd.getInstance().setDisplayHomeNativeAdPt(this@MainActivity, binding)
                } else {
                    binding.homeAdPt = false
                    KLog.d(logTagPt, "onResume------>2")
                    PtLoadHomeAd.getInstance().advertisementLoadingPt(this@MainActivity)
                    initHomeAd()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        connection.disconnect(this)
    }

    /**
     * 判断Vpn方案
     */
    private fun judgeVpnScheme() {
        if (!viewModel.isItABuyingUser()) {
            //非买量用户直接走A方案
            whetherToImplementPlanA = true
            PixelUtils.getBuriedPointUserType("pixel_user_type", "a")
            return
        }
        val data = mmkvPt.decodeString(Constant.PIXEL_ABT, "")
        if (Utils.isNullOrEmpty(data)) {
            KLog.e("state", "判断Vpn方案---默认")
            whetherToImplementPlanA = true
        } else {
            //C
            whetherToImplementPlanA = false
            data?.let {
                vpnCScheme(it)
            }
        }
    }

    /**
     * 是否显示VPN引导框
     */
    private fun whetherVpnBootBoxIsDisplayed() {
        val data = mmkvPt.decodeBool(Constant.RETURN_PT_CURRENT_PAGE, false)
        if (data) {
            //热启动：退出到后台3s后
            MmkvUtils.set(Constant.RETURN_PT_CURRENT_PAGE, false)
            // 没有配置或配置为1需要热启动弹框
            mmkvPt.decodeString(Constant.PIXEL_SET_P, "").let {
                if (it != "2") {
                    KLog.e("sate", "stateChanged--2--state=${state.name}")
                    vpnGuidePopUpWindow()
                }
            }
        }
    }

    private val connect = registerForActivityResult(StartService()) {
        if (it) {
            ToastUtils.toast(R.string.no_permissions)
        } else {
            PixelUtils.getBuriedPoint("pixel_v_jurisdiction")
            if (NetworkUtils.isNetworkAvailable()) {
                KLog.e("sate", "pn B 方案state.name=${this@MainActivity.state.name}")
                if (this@MainActivity.state.name == "Stopped") {
                    automaticConnectionJump(true, whetherEmptyCache = true)
                }
            } else {
                ToastUtils.toast("The current device has no network")
            }
        }
    }

    private fun changeState(
        state: BaseService.State,
        animate: Boolean = true
    ) {
        this.state = state
        KLog.e("sate", "stateChanged----state=${state.name}")
        stateListener?.invoke(state)
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

}