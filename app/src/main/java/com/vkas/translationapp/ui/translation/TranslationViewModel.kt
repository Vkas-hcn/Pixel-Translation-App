package com.vkas.translationapp.ui.translation

import android.app.Application
import com.google.gson.reflect.TypeToken

import com.google.mlkit.nl.translate.*
import com.vkas.translationapp.R
import com.vkas.translationapp.app.App
import com.vkas.translationapp.base.BaseViewModel
import com.vkas.translationapp.bean.Language
import com.vkas.translationapp.databinding.ActivityTranslationBinding
import com.vkas.translationapp.enevt.Constant
import com.vkas.translationapp.utils.KLog
import com.vkas.translationapp.utils.MlKitData
import com.vkas.translationapp.utils.MmkvUtils
import com.xuexiang.xui.utils.Utils
import com.xuexiang.xutil.net.JsonUtil
import com.xuexiang.xutil.resource.ResourceUtils
import java.util.*
import kotlin.collections.ArrayList

class TranslationViewModel(application: Application) : BaseViewModel(application) {

    /**
     * 翻译识别文字
     */
    fun translateRecognizedText(text: String) {
        MlKitData.getInstance().translate(text)
    }

    /**
     * 初始化语言框
     */
    fun initializeLanguageBox(binding: ActivityTranslationBinding) {
        binding.edTranslationTop.text.clear()
        binding.edTranslationDown.text.clear()
        MlKitData.getInstance().sourceText.value = ""
        MlKitData.getInstance().sourceLang.value =
            App.mmkvPt.decodeString(Constant.SOURCE_LANG, TranslateLanguage.ENGLISH)
                ?.let { Language(it) }
        Language(TranslateLanguage.ENGLISH)
        MlKitData.getInstance().targetLang.value =
            App.mmkvPt.decodeString(Constant.TARGET_LANG, TranslateLanguage.ENGLISH)
                ?.let { Language(it) }
    }

    /**
     * 交换语言
     */
    fun exchangeLanguage() {
        val sourceLang = MlKitData.getInstance().sourceLang.value
        MlKitData.getInstance().sourceLang.value = MlKitData.getInstance().targetLang.value
        MlKitData.getInstance().targetLang.value = sourceLang
        MmkvUtils.set(Constant.SOURCE_LANG, MlKitData.getInstance().sourceLang.value?.code)
        MmkvUtils.set(Constant.TARGET_LANG, MlKitData.getInstance().targetLang.value?.code)
    }

    /**
     * 常用语言数据
     */
    fun commonLanguageData(): MutableList<Language> {
        var languages: MutableList<Language> = ArrayList()

        if (Utils.isNullOrEmpty(App.mmkvPt.decodeString(Constant.RECENT_DATA))) {
            languages.add(Language(TranslateLanguage.ENGLISH))
        } else {
            languages = JsonUtil.fromJson(
                App.mmkvPt.decodeString(Constant.RECENT_DATA),
                object : TypeToken<MutableList<Language>?>() {}.type
            )
        }
        return languages.distinct().toMutableList()
    }
}
