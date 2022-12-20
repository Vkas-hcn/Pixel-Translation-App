package com.vkas.translationapp.ui.guide

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.vkas.translationapp.BR
import com.vkas.translationapp.R
import com.vkas.translationapp.base.BaseActivity
import com.vkas.translationapp.databinding.ActivityGuideBinding
import com.vkas.translationapp.ui.main.MainActivity
import com.xuexiang.xui.widget.progress.HorizontalProgressView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GuideActivity : BaseActivity<ActivityGuideBinding, GuideViewModel>(),
    HorizontalProgressView.HorizontalProgressUpdateListener {


    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_guide
    }

    override fun initVariableId(): Int {
        return BR._all
    }

    override fun initParam() {
        super.initParam()
    }

    override fun initToolbar() {
        super.initToolbar()
    }

    override fun initData() {
        super.initData()
        binding.pbStartPt.setProgressViewUpdateListener(this)
        binding.pbStartPt.startProgressAnimation()
        lifecycleScope.launch {
            delay(2000L)
            jumpMainPage()
        }
    }

    private fun jumpMainPage() {
        startActivity(MainActivity::class.java)
        finish()
    }

    override fun initViewObservable() {
        super.initViewObservable()
    }

    /**
     * 预加载广告
     */
//    private fun preloadedAdvertisement() {
//        App.isAppOpenSameDay()
//        if (advertisingOnline()) {
//            KLog.d(skAdLog, "广告达到上线")
//            lifecycleScope.launch {
//                delay(2000L)
//                jumpPage()
//            }
//        } else {
//            loadAdvertisement()
//        }
//    }
    override fun onHorizontalProgressStart(view: View?) {
    }

    override fun onHorizontalProgressUpdate(view: View?, progress: Float) {
    }

    override fun onHorizontalProgressFinished(view: View?) {
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return keyCode == KeyEvent.KEYCODE_BACK
    }
}