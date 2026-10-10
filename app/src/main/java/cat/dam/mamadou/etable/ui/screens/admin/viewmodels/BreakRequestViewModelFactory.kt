package cat.dam.mamadou.etable.ui.screens.admin.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class BreakRequestViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BreakRequestViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BreakRequestViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}