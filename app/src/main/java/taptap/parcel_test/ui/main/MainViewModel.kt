package taptap.parcel_test.ui.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import taptap.pub.*

class MainViewModel : ViewModel() {
    private val repository = MainRepository()

    private val liveData = MutableLiveData<State>()
    fun getLiveData(): LiveData<State> = liveData

    fun getData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getData()
                .map { "covert to another string" }
                .doOnError { Log.d("LOG", "it is an error") }
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

    fun getAnotherData() {
        Result
        viewModelScope.launch(Dispatchers.IO) {
            val data = repository.getData()
                .check { it.isNotEmpty() }
                .flatMap { Reaction.on { "Flatmapped data" } }
                .takeOrReturn {
                    Log.d("LOG", "it is an error again")
                    return@launch
                }
            repository.getAnotherData(data)
                .handle(
                        success = { liveData.postValue(State.Success(it)) },
                        error = { Log.d("LOG", "Error too") }
                )
        }
    }

    sealed class State {
        data class Success(val data: String) : State()
        object Error : State()
    }
}