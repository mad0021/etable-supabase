package cat.dam.mamadou.etable.ui.screens.admin.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.utils.firebase.FirebaseOrderManager

class InProgressOrdersViewModelFactory(
    private val orderManager: FirebaseOrderManager,
    private val context: Context
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(InProgressOrdersViewModel::class.java)) {
            InProgressOrdersViewModel(orderManager, context) as T
        } else {
            throw IllegalArgumentException(
                context.getString(R.string.unknown_viewmodel_error, modelClass.name)
            )
        }
    }
}