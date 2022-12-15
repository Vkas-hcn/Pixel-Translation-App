package com.vkas.translationapp.ui.main

import android.os.Bundle
import com.vkas.translationapp.BR
import com.vkas.translationapp.R
import com.vkas.translationapp.base.BaseActivity
import com.vkas.translationapp.databinding.ActivityMainBinding
import com.vkas.translationapp.ui.translation.TranslationActivity

class MainActivity : BaseActivity<ActivityMainBinding, MainViewModel>(){

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_main
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
    }

    override fun initViewObservable() {
        super.initViewObservable()
    }
    inner class Presenter {
        fun clickOcr(){

        }
        fun clickTranslation(){
            startActivity(TranslationActivity::class.java)
        }
    }
}