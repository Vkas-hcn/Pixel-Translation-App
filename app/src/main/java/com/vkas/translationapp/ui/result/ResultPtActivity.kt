package com.vkas.translationapp.ui.result

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
import androidx.lifecycle.lifecycleScope
import com.google.gson.reflect.TypeToken
import com.vkas.translationapp.BR
import com.vkas.translationapp.R
import com.vkas.translationapp.ad.PtLoadConnectAd
import com.vkas.translationapp.ad.PtLoadHomeAd
import com.vkas.translationapp.ad.PtLoadResultAd
import com.vkas.translationapp.app.App
import com.vkas.translationapp.base.BaseActivity
import com.vkas.translationapp.base.BaseViewModel
import com.vkas.translationapp.bean.PtVpnBean
import com.vkas.translationapp.databinding.ActivityResultPtBinding
import com.vkas.translationapp.enevt.Constant
import com.vkas.translationapp.ui.camare.CameraXActivity
import com.vkas.translationapp.ui.translation.TranslationActivity
import com.vkas.translationapp.utils.KLog
import com.xuexiang.xutil.net.JsonUtil
import kotlinx.coroutines.*

class ResultPtActivity : BaseActivity<ActivityResultPtBinding, BaseViewModel>() {
    private var isConnectionPt: Boolean = false

    //当前服务器
    private lateinit var currentServerBeanPt: PtVpnBean
    private var jobResultPt: Job? = null
    private var whetherEmptyCache: Boolean = false

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_result_pt
    }

    override fun initVariableId(): Int {
        return BR._all
    }

    override fun initParam() {
        super.initParam()
        val bundle = intent.extras
        isConnectionPt = bundle?.getBoolean(Constant.CONNECTION_PT_STATUS) == true
        currentServerBeanPt = JsonUtil.fromJson(
            bundle?.getString(Constant.SERVER_PT_INFORMATION),
            object : TypeToken<PtVpnBean?>() {}.type
        )
        whetherEmptyCache = bundle?.getBoolean(Constant.WHETHER_EMPTY_CACHE, false) == true

    }

    override fun initToolbar() {
        super.initToolbar()
        binding.presenter = PtClick()
        binding.resultTitle.imgBack.setImageResource(R.drawable.ic_title_back)
        binding.resultTitle.tvTitle.text = if (isConnectionPt) {getString(R.string.vpn_connect)}else{getString(
                    R.string.vpn_disconnect)}
        binding.resultTitle.tvRight.visibility = View.GONE
        binding.resultTitle.imgBack.setOnClickListener {
            finish()
        }
    }

    override fun initData() {
        super.initData()
        if (isConnectionPt) {
            binding.imgVpnState.setImageResource(R.drawable.ic_vpn_success)
            binding.tvConnected.text = getString(R.string.connected)
            binding.tvConnected.setTextColor(getColor(R.color.tv_vpn_success))
        } else {
            binding.imgVpnState.setImageResource(R.drawable.ic_break_off)
            binding.tvConnected.text = getString(R.string.disconnected)
            binding.tvConnected.setTextColor(getColor(R.color.tv_vpn_dis))
        }
        PtLoadResultAd.getInstance().whetherToShowPt =false
        initResultAds()
    }
    inner class PtClick {
        fun clickOcr() {
            val hasWriteStoragePermission: Int =
                ContextCompat.checkSelfPermission(application, Manifest.permission.CAMERA)
            if (hasWriteStoragePermission == PackageManager.PERMISSION_GRANTED) {
                startActivity(CameraXActivity::class.java)
            } else {
                //没有权限，向用户请求权限
                ActivityCompat.requestPermissions(
                    this@ResultPtActivity,
                    arrayOf(Manifest.permission.CAMERA),
                    1
                )
            }
        }

        fun clickTranslation() {
            startActivity(TranslationActivity::class.java)
        }
    }
    private fun initResultAds() {
        jobResultPt= lifecycleScope.launch {
                while (isActive) {
                    PtLoadResultAd.getInstance().setDisplayResultNativeAd(this@ResultPtActivity,binding)
                    if (PtLoadHomeAd.getInstance().whetherToShowPt) {
                        jobResultPt?.cancel()
                        jobResultPt = null
                    }
                    delay(1000L)
                }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            delay(300)
            if(lifecycle.currentState != Lifecycle.State.RESUMED){return@launch}
            if(App.nativeAdRefreshPt){
                PtLoadResultAd.getInstance().whetherToShowPt =false
                if(PtLoadResultAd.getInstance().appAdDataPt !=null){
                    PtLoadResultAd.getInstance().setDisplayResultNativeAd(this@ResultPtActivity,binding)
                }else{
                    PtLoadResultAd.getInstance().advertisementLoadingPt(this@ResultPtActivity)
                    initResultAds()
                }
            }

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
            data = Uri.fromParts("package", this@ResultPtActivity.packageName, null)
        }
        startActivity(intent)
    }
}