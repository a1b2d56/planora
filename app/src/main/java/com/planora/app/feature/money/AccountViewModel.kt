package com.planora.app.feature.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.planora.app.core.data.database.entities.Account
import com.planora.app.core.data.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val repo: AccountRepository
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = repo.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(account: Account)   = viewModelScope.launch { repo.insert(account) }
    fun update(account: Account) = viewModelScope.launch { repo.update(account) }
    fun delete(account: Account) = viewModelScope.launch { repo.delete(account) }

    init {
        viewModelScope.launch { repo.ensureDefaultExists() }
    }
}
