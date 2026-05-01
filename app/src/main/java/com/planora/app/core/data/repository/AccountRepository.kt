package com.planora.app.core.data.repository

import com.planora.app.core.data.database.dao.AccountDao
import com.planora.app.core.data.database.entities.Account
import com.planora.app.core.data.database.entities.AccountType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(private val dao: AccountDao) {

    fun getAll(): Flow<List<Account>> = dao.getAll()


    suspend fun insert(account: Account): Long {
        if (account.isDefault) dao.clearDefault()
        return dao.insert(account)
    }

    suspend fun update(account: Account) {
        if (account.isDefault) dao.clearDefault()
        dao.update(account)
    }

    suspend fun delete(account: Account) = dao.delete(account)

    suspend fun ensureDefaultExists() {
        if (dao.getDefault() == null) {
            dao.insert(Account(id = 1, name = "Cash", type = AccountType.CASH, isDefault = true))
        }
    }
}
