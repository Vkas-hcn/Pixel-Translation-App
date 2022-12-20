package com.vkas.translationapp.ui.language

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.translationapp.BR
import com.vkas.translationapp.R
import com.vkas.translationapp.base.BaseActivity
import com.vkas.translationapp.bean.Language
import com.vkas.translationapp.databinding.ActivityLanguageBinding
import com.vkas.translationapp.databinding.ActivityTranslationBinding
import com.vkas.translationapp.enevt.Constant
import com.vkas.translationapp.ui.translation.TranslationViewModel
import com.vkas.translationapp.utils.CopyUtils
import com.vkas.translationapp.utils.KLog
import com.vkas.translationapp.utils.MlKitData
import com.vkas.translationapp.utils.MmkvUtils
import com.xuexiang.xui.adapter.recyclerview.DividerItemDecoration
import com.xuexiang.xutil.net.JsonUtil
import java.util.*
import kotlin.collections.ArrayList

class LanguageActivity : BaseActivity<ActivityLanguageBinding, TranslationViewModel>() {
    private lateinit var allAdapter: LanguageAdapter
    private lateinit var recentlyAdapter:LanguageRecentlyAdapter
    private lateinit var allLanguageData: MutableList<Language>
    private lateinit var recentlyLanguageData: MutableList<Language>

    private var chekType:Int = 1
    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_language
    }

    override fun initVariableId(): Int {
        return BR._all
    }

    override fun initParam() {
        super.initParam()
        val bundle = intent.extras
        chekType = bundle?.getInt(Constant.JUMP_LANGUAGE_PARAMETERS)!!
    }

    override fun initToolbar() {
        super.initToolbar()
        binding.presenter = Presenter()
        binding.inTranslationTitlePt.let {
            it.imgBack.setImageResource(R.drawable.ic_title_back)
            it.tvRight.visibility = View.VISIBLE
            it.imgBack.setOnClickListener {
                finish()
            }
            it.tvRight.setOnClickListener {
                finish()
            }
        }
        binding.editSearchView.setEditSearchListener { editString ->
            allLanguageData.forEach { all ->
                all.searchForMatches = Locale(all.code.lowercase(Locale.getDefault())).displayLanguage.contains(editString.lowercase(Locale.getDefault()))

            }
            allLanguageData.forEach { all ->
                if(!all.searchForMatches){
                    all.searchForMatches = all.code.lowercase(Locale.getDefault()).contains(editString.lowercase(Locale.getDefault()))
                }
            }
            allAdapter.notifyDataSetChanged()
        }
    }

    override fun initData() {
        super.initData()
        KLog.e("TAG","language--initdata")
        liveEventBusReceive()
        binding.selectedSourceLang = chekType
        updateLanguageItem()
        initAllRecyclerView()
        initRecentlyRecyclerView()
        recentLanguageCursor()
    }

    private fun liveEventBusReceive() {
        LiveEventBus
            .get(Constant.DOWNLOADING, Language::class.java)
            .observeForever { downloadData ->
                MlKitData.getInstance().availableLanguages.forEach { all ->
                    if (all.code == downloadData.code) {
                        all.downloadStatus = downloadData.downloadStatus
                    }
                }
                allAdapter.notifyDataSetChanged()
            }
    }
    @SuppressLint("NotifyDataSetChanged")
    private fun initRecentlyRecyclerView() {
        recentlyLanguageData = ArrayList()
        recentlyLanguageData =viewModel.commonLanguageData()
        recentlyAdapter = LanguageRecentlyAdapter(recentlyLanguageData)
        binding.rvRecently.layoutManager = LinearLayoutManager(this)
        binding.recentlyAdapter = recentlyAdapter
        recentlyAdapter.addChildClickViewIds(R.id.img_down_state)
        recentlyAdapter.setOnItemClickListener { _, _, position ->
            recentlyLanguageData.getOrNull(position)?.run {
                setLanguageOptions(this,binding.selectedSourceLang as Int)
            }
            recentlyAdapter.notifyDataSetChanged()
            finish()
        }
    }
    @SuppressLint("NotifyDataSetChanged")
    private fun initAllRecyclerView() {
        allLanguageData = ArrayList()
        allLanguageData = MlKitData.getInstance().availableLanguages
        allLanguageData.forEach { all ->
            all.searchForMatches =true
            MlKitData.getInstance().availableModels.value?.forEach { models ->
                if (all.code == models) {
                    all.downloadStatus = 2
                }
            }
        }
        allAdapter = LanguageAdapter(allLanguageData)
        binding.layoutManager = LinearLayoutManager(this)
        binding.allAdapter = allAdapter
        allAdapter.addChildClickViewIds(R.id.img_down_state)
        allAdapter.setOnItemClickListener { _, _, position ->
            allLanguageData.getOrNull(position)?.run {
                if(this.downloadStatus !=2){return@setOnItemClickListener}
                setLanguageOptions(this,binding.selectedSourceLang as Int)
            }
            allAdapter.notifyDataSetChanged()
            finish()
        }
        allAdapter.setOnItemChildClickListener { _, view, position ->
            if (view.id == R.id.img_down_state) {
                allLanguageData.getOrNull(position)?.let {
                    // 下载
                    if (it.downloadStatus == 0) {
                        it.downloadStatus = 1
                        MlKitData.getInstance().downloadLanguage(it)
                        allAdapter.notifyDataSetChanged()
                    }
                    //删除
                    if (it.downloadStatus == 2) {
                        deletePopUpFrame(it)
                    }
                }
            }
        }
    }
    /**
     * 设置语言选项
     */
    private fun setLanguageOptions(language: Language,leftOrRight:Int){
        when(leftOrRight){
            1->{
                KLog.e("TAG","33333333")

                MlKitData.getInstance().sourceLang.value = language
                MmkvUtils.set(Constant.SOURCE_LANG,language.code)
            }
            2->{
                KLog.e("TAG","444444444")

                MlKitData.getInstance().targetLang.value = language
                MmkvUtils.set(Constant.TARGET_LANG,language.code)
            }
        }
        recentlyLanguageData.remove(language)
        recentlyLanguageData.add(0,language)
        if(recentlyLanguageData.size>3){
            recentlyLanguageData.removeAt(recentlyLanguageData.size-1)
        }
        recentlyLanguageData.map { it.isCheck =false }
        recentlyLanguageData.getOrNull(0)?.isCheck =true
        MmkvUtils.set(Constant.RECENT_DATA,JsonUtil.toJson(recentlyLanguageData))
        updateLanguageItem()
    }

    private fun setLanguageOptions2(language: Language){
        when(binding.selectedSourceLang){
            1->{
                MlKitData.getInstance().sourceLang.value = language
                MmkvUtils.set(Constant.SOURCE_LANG,language.code)
            }
            2->{
                MlKitData.getInstance().targetLang.value = language
                MmkvUtils.set(Constant.TARGET_LANG,language.code)
            }
        }
        recentlyLanguageData.remove(language)
        recentlyLanguageData.add(0,language)
        if(recentlyLanguageData.size>3){
            recentlyLanguageData.removeAt(recentlyLanguageData.size-1)
        }
        recentlyLanguageData.map { it.isCheck =false }
        recentlyLanguageData.getOrNull(0)?.isCheck =true
        MmkvUtils.set(Constant.RECENT_DATA,JsonUtil.toJson(recentlyLanguageData))
        updateLanguageItem()
    }

    /**
     * 更新语言项
     */
    private fun updateLanguageItem(){
        binding.tvLanguageLeft.text = Locale(MlKitData.getInstance().sourceLang.value?.code).displayLanguage
        binding.tvLanguageRight.text = Locale(MlKitData.getInstance().targetLang.value?.code).displayLanguage
    }
    /**
     * 删除弹框
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun deletePopUpFrame(language: Language) {
        val dialog: AlertDialog? = AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_title))
            .setMessage(getString(R.string.delete_message))
            //设置对话框的按钮
            .setNegativeButton("NO") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("YES") { dialog, _ ->
                dialog.dismiss()
                MlKitData.getInstance().deleteLanguage(language)
                language.downloadStatus = 0
                recentlyLanguageData.remove(language)

                if(binding.tvLanguageLeft.text == Locale(language.code).displayLanguage){
                    setLanguageOptions(Language(TranslateLanguage.ENGLISH),1)
                }
                if(binding.tvLanguageRight.text == Locale(language.code).displayLanguage){
                    setLanguageOptions(Language(TranslateLanguage.ENGLISH),2)
                }
                recentlyAdapter.notifyDataSetChanged()
                allAdapter.notifyDataSetChanged()
            }.create()
        dialog?.show()
        dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
        dialog?.getButton(DialogInterface.BUTTON_NEGATIVE)?.setTextColor(Color.BLACK)
    }

    override fun initViewObservable() {
        super.initViewObservable()
    }
    inner class Presenter {
        fun toLanguage(type: Int) {
            binding.selectedSourceLang = type
            recentLanguageCursor()
        }

        fun toExchange() {
            if (binding.selectedSourceLang == 1) {
                binding.selectedSourceLang = 2
            } else {
                binding.selectedSourceLang = 1
            }
            viewModel.exchangeLanguage()
            updateLanguageItem()
            recentLanguageCursor()
        }
    }
    /**
     *  最近语言光标
     */
    @SuppressLint("NotifyDataSetChanged")
    fun recentLanguageCursor(){
        when(binding.selectedSourceLang){
            1->{
                recentlyLanguageData.forEach {
                    it.isCheck = it.code == MlKitData.getInstance().sourceLang.value?.code
                }
            }
            2->{
                recentlyLanguageData.forEach {
                    it.isCheck = it.code == MlKitData.getInstance().targetLang.value?.code
                }
            }
        }
        recentlyAdapter.notifyDataSetChanged()
    }
}