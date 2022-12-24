package com.vkas.translationapp.ui.translation

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.translationapp.BR
import com.vkas.translationapp.R
import com.vkas.translationapp.ad.PtLoadHomeAd
import com.vkas.translationapp.ad.PtLoadTranslationAd
import com.vkas.translationapp.ad.PtLoadBackAd
import com.vkas.translationapp.app.App
import com.vkas.translationapp.base.BaseActivity
import com.vkas.translationapp.databinding.ActivityTranslationBinding
import com.vkas.translationapp.enevt.Constant
import com.vkas.translationapp.enevt.Constant.logTagPt
import com.vkas.translationapp.ui.language.LanguageActivity
import com.vkas.translationapp.utils.CopyUtils
import com.vkas.translationapp.utils.KLog
import com.vkas.translationapp.utils.MlKitData
import com.vkas.translationapp.utils.PixelUtils
import com.vkas.translationapp.widget.PtLoadingDialog
import com.xuexiang.xutil.app.ActivityUtils
import com.xuexiang.xutil.tip.ToastUtils
import kotlinx.coroutines.*
import java.util.*

class TranslationActivity : BaseActivity<ActivityTranslationBinding, TranslationViewModel>() {
    private lateinit var ptLoadingDialog: PtLoadingDialog
    private var jobNativeAdsPt: Job? = null
    private var jobBack: Job? = null
    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_translation
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
        binding.inTranslationTitlePt.let {
            it.imgBack.setImageResource(R.drawable.ic_title_back)
            it.imgBack.setOnClickListener {
                returnToHomePage()
            }
        }
    }

    override fun initData() {
        super.initData()
        liveEventBusReceive()
        ptLoadingDialog = PtLoadingDialog(this)
        viewModel.initializeLanguageBox(binding)
        updateLanguageItem()
        MlKitData.getInstance().fetchDownloadedModels()
        PtLoadTranslationAd.getInstance().whetherToShowPt = false
        PtLoadTranslationAd.getInstance().advertisementLoadingPt(this)
        initTranslationAd()
    }

    private fun liveEventBusReceive() {
        //插屏关闭后跳转
        LiveEventBus
            .get(Constant.PLUG_PT_TRANSLATION_SHOW, Boolean::class.java)
            .observeForever {
                PtLoadBackAd.getInstance().advertisementLoadingPt(this)
//                if(!it){
                finish()
//                }
            }
    }

    /**
     *
     */
    override fun initViewObservable() {
        super.initViewObservable()
        showTranslationResult()
    }

    private fun initTranslationAd() {
        jobNativeAdsPt = lifecycleScope.launch {
            while (isActive) {
                PtLoadTranslationAd.getInstance()
                    .setDisplayNativeAdPt(this@TranslationActivity, binding)
                if (PtLoadHomeAd.getInstance().whetherToShowPt) {
                    jobNativeAdsPt?.cancel()
                    jobNativeAdsPt = null
                }
                delay(1000L)
            }

        }
    }

    private fun showTranslationResult() {
        MlKitData.getInstance().sourceText.observe(this, {
            binding.edTranslationDown.setText(it)
            ptLoadingDialog.dismiss()
        })
    }

    /**
     * 更新语言项
     */
    private fun updateLanguageItem() {
        binding.tvLanguageLeft.text =
            Locale(MlKitData.getInstance().sourceLang.value?.code).displayLanguage
        binding.tvLanguageTopName.text =
            Locale(MlKitData.getInstance().sourceLang.value?.code).displayLanguage
        binding.tvLanguageRight.text =
            Locale(MlKitData.getInstance().targetLang.value?.code).displayLanguage
        binding.tvLanguageDownName.text =
            Locale(MlKitData.getInstance().targetLang.value?.code).displayLanguage
    }

    /**
     * 交换翻译结果
     */
    fun exchangeTranslationResults() {
        if (binding.edTranslationDown.text.isNullOrEmpty()) {
            return
        }
        val edTranslationTop = binding.edTranslationTop.text
        binding.edTranslationTop.text = binding.edTranslationDown.text
        binding.edTranslationDown.text = edTranslationTop
    }

    /**
     * 返回主页
     */
    private fun returnToHomePage() {
        App.isAppOpenSameDayPt()
        if (PixelUtils.isThresholdReached()) {
            KLog.d(logTagPt, "广告达到上线")
            finish()
            return
        }
        PtLoadBackAd.getInstance().advertisementLoadingPt(this)
        jobBack = GlobalScope.launch {
            try {
                withTimeout(3000L) {
                    while (isActive) {
                        val showState =
                            PtLoadBackAd.getInstance()
                                .displayBackAdvertisementPt(this@TranslationActivity)
                        if (showState) {
                            jobBack?.cancel()
                            jobBack = null
                        }
                        delay(1000L)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                KLog.d(logTagPt, "translation-back---插屏超时")
                if (jobBack != null) {
                    finish()
                }
            }
        }
    }

    inner class Presenter {
        fun toLanguage(type: Int) {
            ActivityUtils.startActivityForResult(
                this@TranslationActivity,
                LanguageActivity().javaClass,
                Constant.JUMP_LANGUAGE_PAGE,
                Constant.JUMP_LANGUAGE_PARAMETERS,
                type
            )
        }

        fun toExchange() {
            if (binding.selectedSourceLang == 1) {
                binding.selectedSourceLang = 2
            } else {
                binding.selectedSourceLang = 1
            }
            viewModel.exchangeLanguage()
            updateLanguageItem()
            exchangeTranslationResults()
        }

        fun toDelete() {
            binding.edTranslationTop.setText("")
        }

        fun toTranslation() {
            if (binding.edTranslationTop.text.trim().isEmpty()) {
                ToastUtils.toast(getString(R.string.please_enter_the_translation_content))
                return
            }
            lifecycleScope.launch {
                ptLoadingDialog.show()
                delay(500L)
                viewModel.translateRecognizedText(binding.edTranslationTop.text.toString())
            }
        }

        fun toCopy() {
            CopyUtils.copyClicks(
                this@TranslationActivity,
                binding.edTranslationDown.text.toString()
            )
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            delay(300)
            if (lifecycle.currentState != Lifecycle.State.RESUMED) {
                return@launch
            }
            if (App.nativeAdRefreshPt) {
                PtLoadTranslationAd.getInstance().whetherToShowPt = false
                if (PtLoadTranslationAd.getInstance().appAdDataPt != null) {
                    KLog.d(Constant.logTagPt, "onResume------>1")
                    PtLoadTranslationAd.getInstance()
                        .setDisplayNativeAdPt(this@TranslationActivity, binding)
                } else {
                    binding.translationAdPt = false
                    KLog.d(Constant.logTagPt, "onResume------>2")
                    PtLoadTranslationAd.getInstance()
                        .advertisementLoadingPt(this@TranslationActivity)
                    initTranslationAd()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constant.JUMP_LANGUAGE_PAGE) {
            updateLanguageItem()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            returnToHomePage()
        }
        return true
    }

}