package com.vkas.translationapp.ui.translation

import android.app.Application
import android.util.LruCache
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.*
import com.vkas.translationapp.R
import com.vkas.translationapp.base.BaseViewModel
import com.vkas.translationapp.bean.Language
import com.vkas.translationapp.utils.KLog
import com.vkas.translationapp.utils.MlKitData
import com.xuexiang.xutil.net.JsonUtil
import java.util.*

class TranslationViewModel(application: Application) : BaseViewModel(application) {
    companion object {
        private const val NUM_TRANSLATORS = 3
    }

    /**
     * 设置语言选项
     */
    fun setLanguageOptions(){

    }
    /**
     * 交换语言
     */
    fun exchangeLanguage(){
        val sourceLang = MlKitData.getInstance().sourceLang.value
        MlKitData.getInstance().sourceLang.value = MlKitData.getInstance().targetLang.value
        MlKitData.getInstance().targetLang.value = sourceLang
    }
    /**
     * 常用语言数据
     */
    fun commonLanguageData(){

    }
}
