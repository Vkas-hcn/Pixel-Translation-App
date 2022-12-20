package com.vkas.translationapp.ui.camare

import android.app.Application
import com.google.mlkit.nl.translate.TranslateLanguage
import com.vkas.translationapp.app.App
import com.vkas.translationapp.base.BaseViewModel
import com.vkas.translationapp.bean.Language
import com.vkas.translationapp.databinding.ActivityTranslationBinding
import com.vkas.translationapp.enevt.Constant
import com.vkas.translationapp.utils.MlKitData
import com.vkas.translationapp.utils.MmkvUtils

class CameraXViewModel (application: Application) : BaseViewModel(application) {
    /**
     * 翻译识别文字
     */
    fun translateRecognizedText(text:String){
        MlKitData.getInstance().translate(text)
    }
    /**
     * 交换语言
     */
    fun exchangeLanguage() {
        val sourceLang = MlKitData.getInstance().sourceLang.value
        MlKitData.getInstance().sourceLang.value = MlKitData.getInstance().targetLang.value
        MlKitData.getInstance().targetLang.value = sourceLang
        MmkvUtils.set(Constant.SOURCE_LANG,MlKitData.getInstance().sourceLang.value?.code)
        MmkvUtils.set(Constant.TARGET_LANG,MlKitData.getInstance().targetLang.value?.code)
    }
    /**
     * 初始化语言框
     */
    fun initializeLanguageBox(){
        MlKitData.getInstance().sourceLang.value =
            App.mmkvPt.decodeString(Constant.SOURCE_LANG, TranslateLanguage.ENGLISH)
                ?.let { Language(it) }
        Language(TranslateLanguage.ENGLISH)
        MlKitData.getInstance().targetLang.value =
            App.mmkvPt.decodeString(Constant.TARGET_LANG, TranslateLanguage.ENGLISH)
                ?.let { Language(it) }
    }
}