package com.vkas.translationapp.utils

import android.content.Context
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
import android.renderscript.Allocation

import android.graphics.Bitmap
import androidx.annotation.NonNull

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool

import com.bumptech.glide.load.resource.bitmap.CenterCrop
import android.os.Build

import android.renderscript.ScriptIntrinsicBlur

import android.renderscript.RenderScript

import android.annotation.TargetApi
import android.renderscript.Element
import java.security.MessageDigest
import java.util.*


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
    val selectedLang = MutableLiveData<Language>()
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
    fun translate(text:String): Task<String> {
        val source = sourceLang.value
        val target = targetLang.value
        KLog.e("TAG","识别内容==${text}")
        if (source == null || target == null || text.isEmpty()) {
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
                KLog.e("TAG","remoteModels=${Locale(it.language).displayLanguage}")
            }
            availableModels.value = remoteModels.sortedBy { it.language }.map { it.language }
            KLog.e("TAG","availableModels.value=${availableModels.value}")

        }
        availableModels.value?.map {
            KLog.e("TAG","availableModels=${it}")
        }
    }

    class GlideBlurTransformation(private val context: Context) : CenterCrop() {
        override fun transform(
            pool: BitmapPool,
            toTransform: Bitmap,
            outWidth: Int,
            outHeight: Int
        ): Bitmap? {
            val bitmap = super.transform(pool, toTransform, outWidth, outHeight)
            return getInstance().blurBitmap(
                context, bitmap, 25F,
                (outWidth * 0.5).toInt(), (outHeight * 0.5).toInt()
            )
        }
        override fun updateDiskCacheKey(messageDigest: MessageDigest) {}
    }

    /**
     * @param image         需要模糊的图片
     * @param blurRadius    模糊的半径（1-25之间）
     * @return 模糊处理后的Bitmap
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    fun blurBitmap(
        context: Context?,
        image: Bitmap?,
        blurRadius: Float,
        outWidth: Int,
        outHeight: Int
    ): Bitmap? {
        // 将缩小后的图片做为预渲染的图片
        val inputBitmap = Bitmap.createScaledBitmap(image!!, outWidth, outHeight, false)
        // 创建一张渲染后的输出图片
        val outputBitmap = Bitmap.createBitmap(inputBitmap)
        // 创建RenderScript内核对象
        val rs = RenderScript.create(context)
        // 创建一个模糊效果的RenderScript的工具对象
        val blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        // 由于RenderScript并没有使用VM来分配内存,所以需要使用Allocation类来创建和分配内存空间
        // 创建Allocation对象的时候其实内存是空的,需要使用copyTo()将数据填充进去
        val tmpIn = Allocation.createFromBitmap(rs, inputBitmap)
        val tmpOut = Allocation.createFromBitmap(rs, outputBitmap)
        // 设置渲染的模糊程度, 25f是最大模糊度
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            blurScript.setRadius(blurRadius)
        }
        // 设置blurScript对象的输入内存
        blurScript.setInput(tmpIn)
        // 将输出数据保存到输出内存中
        blurScript.forEach(tmpOut)
        // 将数据填充到Allocation中
        tmpOut.copyTo(outputBitmap)
        return outputBitmap
    }
}