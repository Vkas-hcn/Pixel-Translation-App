package com.vkas.translationapp.utils

import android.util.LruCache
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.*
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.translationapp.R
import com.vkas.translationapp.bean.Language
import com.vkas.translationapp.enevt.Constant
import java.util.HashMap

class MlKitData {
    companion object {
        fun getInstance() = InstanceHelper.mlKitData
        private const val NUM_TRANSLATORS = 3

    }

    object InstanceHelper {
        val mlKitData = MlKitData()

    }
    //同步
    private val modelManager: RemoteModelManager = RemoteModelManager.getInstance()
    val availableModels = MutableLiveData<List<String>>()
    // Gets a list of all available translation languages.
    val availableLanguages: MutableList<Language> = TranslateLanguage.getAllLanguages().map { Language(it) } as MutableList<Language>
    val sourceLang = MutableLiveData<Language>()
    val targetLang = MutableLiveData<Language>()
    val sourceText = MutableLiveData<String>()
    private val pendingDownloads: HashMap<String, Task<Void>> = hashMapOf()

    private val translators =
        object : LruCache<TranslatorOptions, Translator>(NUM_TRANSLATORS) {
            override fun create(options: TranslatorOptions): Translator {
                return Translation.getClient(options)
            }
            override fun entryRemoved(
                evicted: Boolean,
                key: TranslatorOptions,
                oldValue: Translator,
                newValue: Translator?,
            ) {
                oldValue.close()
            }
        }
    fun translate(): Task<String> {
        val text = sourceText.value
        val source = sourceLang.value
        val target = targetLang.value
        if (source == null || target == null || text == null || text.isEmpty()) {
            return Tasks.forResult("")
        }
        val sourceLangCode = TranslateLanguage.fromLanguageTag(source.code)!!
        val targetLangCode = TranslateLanguage.fromLanguageTag(target.code)!!
        val options =
            TranslatorOptions.Builder()
                .setSourceLanguage(sourceLangCode)
                .setTargetLanguage(targetLangCode)
                .build()
        return translators[options].translate(text).addOnCompleteListener {
            it.addOnSuccessListener { text->
                KLog.e("TAG","翻译成功=${text}")
                sourceText.postValue(text)
            }
            it.addOnFailureListener {error->
                KLog.e("TAG","翻译失败=${error}")
                sourceText.postValue(R.string.translate_failed.toString())
            }
        }
    }
    private fun getModel(languageCode: String): TranslateRemoteModel {
        return TranslateRemoteModel.Builder(languageCode).build()
    }
    // Starts downloading a remote model for local translation.
    internal fun downloadLanguage(language: Language) {
        val model = getModel(TranslateLanguage.fromLanguageTag(language.code)!!)
        var downloadTask: Task<Void>?
        if (pendingDownloads.containsKey(language.code)) {
            downloadTask = pendingDownloads[language.code]
            // found existing task. exiting
            if (downloadTask != null && !downloadTask.isCanceled) {
                return
            }
        }
        downloadTask =
            modelManager.download(model, DownloadConditions.Builder().build()).addOnSuccessListener {
                pendingDownloads.remove(language.code)
                fetchDownloadedModels()
               language.downloadStatus =2
            }.addOnFailureListener {
                language.downloadStatus =0
            }.addOnCompleteListener {
                LiveEventBus.get<Language>(Constant.DOWNLOADING)
                    .post(language)
            }

        pendingDownloads[language.code] = downloadTask
    }
    // Deletes a locally stored translation model.
    internal fun deleteLanguage(language: Language) {
        val model = getModel(TranslateLanguage.fromLanguageTag(language.code)!!)
        modelManager.deleteDownloadedModel(model).addOnCompleteListener { fetchDownloadedModels()
        }
        pendingDownloads.remove(language.code)
    }
    fun fetchDownloadedModels() {
        modelManager.getDownloadedModels(TranslateRemoteModel::class.java).addOnSuccessListener {
                remoteModels ->
            remoteModels?.forEach {
                KLog.e("TAG","remoteModels=${it.language}")
            }
            availableModels.value = remoteModels.sortedBy { it.language }.map { it.language }
            KLog.e("TAG","availableModels.value=${availableModels.value}")

        }
        availableModels.value?.map {
            KLog.e("TAG","availableModels=${it}")
        }
    }
}