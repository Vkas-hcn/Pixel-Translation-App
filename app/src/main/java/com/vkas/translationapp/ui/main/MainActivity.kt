package com.vkas.translationapp.ui.main

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.translationapp.BR
import com.vkas.translationapp.R
import com.vkas.translationapp.ad.PtLoadHomeAd
import com.vkas.translationapp.ad.PtLoadTranslationBackAd
import com.vkas.translationapp.app.App
import com.vkas.translationapp.base.BaseActivity
import com.vkas.translationapp.bean.Language
import com.vkas.translationapp.databinding.ActivityMainBinding
import com.vkas.translationapp.enevt.Constant
import com.vkas.translationapp.enevt.Constant.logTagPt
import com.vkas.translationapp.ui.camare.CameraXActivity
import com.vkas.translationapp.ui.translation.TranslationActivity
import com.vkas.translationapp.ui.web.WebPtActivity
import com.vkas.translationapp.utils.KLog
import com.vkas.translationapp.utils.MlKitData
import com.xuexiang.xutil.tip.ToastUtils
import kotlinx.coroutines.*

class MainActivity : BaseActivity<ActivityMainBinding, MainViewModel>() {
    private var jobNativeAdsPt: Job? = null
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
        PtLoadHomeAd.getInstance().whetherToShowPt = false
        PtLoadHomeAd.getInstance().advertisementLoadingPt(this)
        initHomeAd()
    }

    override fun initViewObservable() {
        super.initViewObservable()
    }

    private fun initHomeAd() {
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
            if (App.nativeAdRefreshPt) {
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

}