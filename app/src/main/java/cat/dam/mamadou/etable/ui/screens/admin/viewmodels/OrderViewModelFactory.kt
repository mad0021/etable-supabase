package cat.dam.mamadou.etable.ui.screens.admin.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.utils.firebase.FirebaseManager
import cat.dam.mamadou.etable.utils.firebase.FirebaseOrderManager

/**
 * Fábrica para crear ViewModels relacionados con órdenes
 */
class OrderViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    
    private val firebaseManager = FirebaseManager()
    private val orderManager = firebaseManager.orderManager
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(PendingOrdersViewModel::class.java) -> {
                PendingOrdersViewModel(orderManager, context) as T
            }
            modelClass.isAssignableFrom(InProgressOrdersViewModel::class.java) -> {
                InProgressOrdersViewModel(orderManager, context) as T
            }
            modelClass.isAssignableFrom(ReadyOrdersViewModel::class.java) -> {
                ReadyOrdersViewModel(orderManager, context) as T
            }
            modelClass.isAssignableFrom(HistoryOrdersViewModel::class.java) -> {
                HistoryOrdersViewModel(orderManager, context) as T
            }
            else -> throw IllegalArgumentException(
                context.getString(R.string.unknown_viewmodel_error, modelClass.name)
            )
        }
    }
}