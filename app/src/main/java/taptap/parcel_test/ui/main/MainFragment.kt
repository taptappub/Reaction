package taptap.parcel_test.ui.main

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.taptap.parcel_test.R
import kotlinx.coroutines.*
import taptap.pub.Reaction
import java.lang.IllegalStateException

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        viewModel.getData()
        viewModel.getLiveData().observe(viewLifecycleOwner) { state ->
            when (state) {
                is MainViewModel.State.Success -> Toast.makeText(
                    requireContext(),
                    "Success",
                    Toast.LENGTH_LONG
                ).show()
                MainViewModel.State.Error -> Toast.makeText(
                    requireContext(),
                    "Error",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        Log.d("TAGGGGG", "start")
        val r = CoroutineScope(Dispatchers.IO) + Job()
        r.launch {
            try {
                Log.d("TAGGGGG", "launch")
                val reaction = Reaction.on {
                    throw IllegalStateException()
                }
                if (reaction is Reaction.Error) {
                    Log.d("TAGGGGG", "catcha " +reaction.exception.toString())
                }
            } catch (e: Exception) {
                Log.d("TAGGGGG", e.toString())
            }
        }
        Log.d("TAGGGGG", "end")
    }

}