package com.vkas.translationapp.bean

import androidx.annotation.Keep
import java.util.*
@Keep
class Language (val code: String) : Comparable<Language> {
    var isCheck:Boolean =false
    //0：未下载；1：下载中；2：已下载
    var downloadStatus:Int =0
    // 搜索匹配
    var searchForMatches:Boolean =true
    private val displayName: String
        get() = Locale(code).displayName

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        if (other !is Language) {
            return false
        }

        val otherLang = other as Language?
        return otherLang!!.code == code
    }

    override fun toString(): String {
        return "$code - $displayName"
    }

    override fun compareTo(other: Language): Int {
        return this.displayName.compareTo(other.displayName)
    }

    override fun hashCode(): Int {
        return code.hashCode()
    }
}