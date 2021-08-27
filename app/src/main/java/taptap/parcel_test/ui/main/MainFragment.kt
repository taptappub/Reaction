package taptap.parcel_test.ui.main

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.taptap.parcel_test.R

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        viewModel.getData()
        viewModel.getLiveData().observe(viewLifecycleOwner) { state ->
            when(state) {
                is MainViewModel.State.Success -> Toast.makeText(requireContext(), "Success", Toast.LENGTH_LONG).show()
                MainViewModel.State.Error -> Toast.makeText(requireContext(), "Error", Toast.LENGTH_LONG).show()
            }
        }
    }

}