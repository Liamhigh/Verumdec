package com.verumdec.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verumdec.forensic.CaseResult
import com.verumdec.forensic.ForensicEngineFacade
import kotlinx.coroutines.launch

class MainViewModel(
    private val engine: ForensicEngineFacade
) : ViewModel() {
    
    private val _caseResult = MutableLiveData<CaseResult>()
    val caseResult: LiveData<CaseResult> = _caseResult
    
    fun createCase(name: String) {
        viewModelScope.launch {
            val result = engine.createCase(name)
            _caseResult.postValue(result)
        }
    }
}
