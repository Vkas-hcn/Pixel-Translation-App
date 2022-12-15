package com.vkas.translationapp.base
import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.vkas.translationapp.enevt.StateLiveData

open class BaseViewModel (application: Application) : BaseViewModelMVVM(application) {
    var stateLiveData: StateLiveData<Any> = StateLiveData()
    fun getStateLiveData(): MutableLiveData<Any> {
        return stateLiveData
    }
}