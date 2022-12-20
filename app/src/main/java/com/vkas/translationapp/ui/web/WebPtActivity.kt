package com.vkas.translationapp.ui.web

import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import com.vkas.translationapp.BR
import com.vkas.translationapp.R
import com.vkas.translationapp.base.BaseActivity
import com.vkas.translationapp.base.BaseViewModel
import com.vkas.translationapp.databinding.ActivityWebBinding
import com.vkas.translationapp.enevt.Constant

class WebPtActivity : BaseActivity<ActivityWebBinding, BaseViewModel>() {
    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_web
    }

    override fun initVariableId(): Int {
        return BR._all
    }

    override fun initToolbar() {
        super.initToolbar()
        binding.webTitlePt.imgBack.visibility = View.VISIBLE
        binding.webTitlePt.imgBack.setImageResource(R.drawable.ic_title_back)

        binding.webTitlePt.imgBack.setOnClickListener {
            finish()
        }
        binding.webTitlePt.tvTitle.visibility = View.GONE
    }

    override fun initData() {
        super.initData()
        binding.ppWebFs.loadUrl(Constant.PRIVACY_PT_AGREEMENT)
        binding.ppWebFs.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            }

            override fun onPageFinished(view: WebView, url: String) {
            }

            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler,
                error: SslError
            ) {
                handler.proceed()
            }
        }

        binding.ppWebFs.webViewClient = object : WebViewClient() {
            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler, error: SslError
            ) {
                handler.proceed()
            }

            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (Constant.PRIVACY_PT_AGREEMENT == url) {
                    view.loadUrl(url)
                } else {
                    // 系统处理
                    return super.shouldOverrideUrlLoading(view, url)
                }
                return true
            }
        }


    }


    //点击返回上一页面而不是退出浏览器
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && binding.ppWebFs.canGoBack()) {
            binding.ppWebFs.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        binding.ppWebFs.loadDataWithBaseURL(null, "", "text/html", "utf-8", null)
        binding.ppWebFs.clearHistory()
        (binding.ppWebFs.parent as ViewGroup).removeView(binding.ppWebFs)
        binding.ppWebFs.destroy()
        super.onDestroy()
    }
}