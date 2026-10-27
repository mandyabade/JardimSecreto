
package com.ifpr.androidapptemplatear.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedModelViewModel : ViewModel() {

    val selectedModel = MutableLiveData<String>()
    val collectedItems = MutableLiveData<List<String>>(emptyList())

    fun selectModel(modelPath: String) {
        selectedModel.value = modelPath
    }

    fun collectItem(modelPath: String) {
        val items = collectedItems.value.orEmpty()
        collectedItems.value = items + modelPath
    }
}
