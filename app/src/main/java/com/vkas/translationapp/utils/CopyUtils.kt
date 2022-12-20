package com.vkas.translationapp.utils

import android.content.Context
import android.text.ClipboardManager
import android.widget.Toast
import com.vkas.translationapp.R


object CopyUtils {
    //保存文本内容到剪切板
    fun copyClicks(context: Context,text: String?) {
        val cbm =context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cbm.text = text
        Toast.makeText(context, context.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
    }
}