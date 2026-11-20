package com.iilmchat.chessproblem

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras

/**
 * Фабрика передаёт ViewModel [SavedStateHandle] из CreationExtras — иначе
 * Compose-вызов viewModel(factory = ...) не получает состояние process death.
 */
class AppViewModelFactory<VM : ViewModel>(
    private val create: (SavedStateHandle) -> VM,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = create(SavedStateHandle()) as T

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: CreationExtras,
    ): T = create(extras.createSavedStateHandle()) as T
}
