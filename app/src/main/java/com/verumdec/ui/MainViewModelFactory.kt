package com.verumdec.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.verumdec.forensic.ForensicEngineFacade

class MainViewModelFactory(
    private val engine: ForensicEngineFacade
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(engine) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
