package com.vkas.translationapp.ui.translation

import android.content.Intent
import android.os.Bundle
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
import com.xuexiang.xutil.app.ActivityUtils
import java.util.*

class TranslationActivity : BaseActivity<ActivityTranslationBinding, TranslationViewModel>() {
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
    }

    override fun initData() {
        super.initData()
        MlKitData.getInstance().sourceLang.value =
            mmkvPt.decodeString(Constant.SOURCE_LANG, TranslateLanguage.ENGLISH)
                ?.let { Language(it) }
        Language(TranslateLanguage.ENGLISH)
        MlKitData.getInstance().targetLang.value =
            mmkvPt.decodeString(Constant.TARGET_LANG, TranslateLanguage.ENGLISH)
                ?.let { Language(it) }
        updateLanguageItem()
        MlKitData.getInstance().fetchDownloadedModels()
    }

    override fun initViewObservable() {
        super.initViewObservable()
        showTranslationResult()
    }

    private fun showTranslationResult() {
        MlKitData.getInstance().sourceText.observe(this, {
            binding.tvTranslationDown.text = it
        })
    }

    /**
     * 更新语言项
     */
    private fun updateLanguageItem(){
        binding.tvLanguageLeft.text = Locale( MlKitData.getInstance().sourceLang.value?.code).displayLanguage
        binding.tvLanguageTopName.text = Locale( MlKitData.getInstance().sourceLang.value?.code).displayLanguage
        binding.tvLanguageRight.text = Locale( MlKitData.getInstance().targetLang.value?.code).displayLanguage
        binding.tvLanguageDownName.text = Locale( MlKitData.getInstance().targetLang.value?.code).displayLanguage
    }


    inner class Presenter {
        fun toLanguage(type: Int) {
            KLog.e("TAG", "type==$type")
            ActivityUtils.startActivityForResult(
                this@TranslationActivity,
                LanguageActivity().javaClass,
                Constant.JUMP_LANGUAGE_PAGE,
                Constant.JUMP_LANGUAGE_PARAMETERS,
                type)
        }

        fun toExchange() {
            if (binding.selectedSourceLang == 1) {
                binding.selectedSourceLang = 2
            } else {
                binding.selectedSourceLang = 1
            }
            viewModel.exchangeLanguage()
            updateLanguageItem()
        }

        fun toDelete() {
            binding.edTranslationTop.setText("")
        }

        fun toTranslation() {
            MlKitData.getInstance().sourceText.value = binding.edTranslationTop.text.toString()
            MlKitData.getInstance().translate()
        }

        fun toCopy() {
            CopyUtils.copyClicks(this@TranslationActivity, binding.tvTranslationDown.toString())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == Constant.JUMP_LANGUAGE_PAGE){
            updateLanguageItem()
        }
    }
}