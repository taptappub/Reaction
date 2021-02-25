package taptap.parcel_test.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import taptap.pub.zip

class MainViewModel : ViewModel() {
    private val repository = MainRepository()

    private val liveData = MutableLiveData<State>()
    fun getLiveData(): LiveData<State> = liveData

    fun getData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getData()
                .zip(
                    success = {
                        State.Success(it)
                    },
                    error = {
                        State.Error
                    }
                )
                .let { liveData.postValue(it) }
        }
    }

    sealed class State {
        data class Success(val data: String) : State()
        object Error : State()
    }
}