package com.vkas.translationapp.ui.translation

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.nl.translate.TranslateLanguage
import com.vkas.translationapp.BR
import com.vkas.translationapp.R
import com.vkas.translationapp.app.App.Companion.mmkvPt
import com.vkas.translationapp.base.BaseActivity
import com.vkas.translationapp.bean.Language
import com.vkas.translationapp.databinding.ActivityTranslationBinding
import com.vkas.translationapp.enevt.Constant
import com.vkas.translationapp.ui.language.LanguageActivity
import com.vkas.translationapp.utils.CopyUtils
import com.vkas.translationapp.utils.KLog
import com.vkas.translationapp.utils.MlKitData
import com.vkas.translationapp.widget.PtLoadingDialog
import com.xuexiang.xutil.app.ActivityUtils
import com.xuexiang.xutil.tip.ToastUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class TranslationActivity : BaseActivity<ActivityTranslationBinding, TranslationViewModel>() {
    private lateinit var ptLoadingDialog: PtLoadingDialog

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
                finish()
            }
        }
    }

    override fun initData() {
        super.initData()
        ptLoadingDialog = PtLoadingDialog(this)
        viewModel.initializeLanguageBox(binding)
        updateLanguageItem()
        MlKitData.getInstance().fetchDownloadedModels()
    }

    /**
     *
     */
    override fun initViewObservable() {
        super.initViewObservable()
        showTranslationResult()
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
            if(binding.edTranslationTop.text.trim().isEmpty()){
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constant.JUMP_LANGUAGE_PAGE) {
            updateLanguageItem()
        }
    }
}