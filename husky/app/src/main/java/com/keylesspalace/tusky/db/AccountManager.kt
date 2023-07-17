/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2023  The Husky Developers
 * Copyright (C) 2018  Conny Duck
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.keylesspalace.tusky.db

import com.keylesspalace.tusky.core.extensions.orEmpty
import com.keylesspalace.tusky.entity.Account
import com.keylesspalace.tusky.entity.Status
import com.keylesspalace.tusky.settings.PrefKeys
import timber.log.Timber
import java.util.Locale

/**
 * This class caches the account database and handles all account related operations
 * @author ConnyDuck
 */
class AccountManager(db: AppDatabase) {

    @Volatile
    var activeAccount: AccountEntity? = null

    var accounts: MutableList<AccountEntity> = mutableListOf()
        private set
    private val accountDao: AccountDao = db.accountDao()

    init {
        accounts = accountDao.loadAll().toMutableList()

        activeAccount = accounts.find { acc ->
            acc.isActive
        }
    }

    /**
     * Adds a new empty account and makes it the active account.
     * More account information has to be added later with [updateActiveAccount]
     * or the account wont be saved to the database.
     * @param accessToken the access token for the new account
     * @param domain the domain of the accounts Mastodon instance
     */
    fun addAccount(accessToken: String, domain: String) {
        activeAccount?.let {
            it.isActive = false
            Timber.d("Saving account with id ${it.id}")

            accountDao.insertOrReplace(it)
        }

        val maxAccountId = accounts.maxByOrNull { it.id }?.id ?: 0
        val newAccountId = maxAccountId + 1
        activeAccount = AccountEntity(
            id = newAccountId,
            domain = domain.toLowerCase(Locale.ROOT),
            accessToken = accessToken,
            isActive = true
        )
    }

    /**
     * Saves an already known account to the database.
     * New accounts must be created with [addAccount]
     * @param account the account to save
     */
    fun saveAccount(account: AccountEntity) {
        if (account.id != 0L) {
            Timber.d("Saving account with id ${account.id}")

            accountDao.insertOrReplace(account)
        }
    }

    /**
     * Logs the current account out by deleting all data of the account.
     * @return the new active account, or null if no other account was found
     */
    fun logActiveAccountOut(): AccountEntity? {
        if (activeAccount == null) {
            return null
        } else {
            accounts.remove(activeAccount!!)
            accountDao.delete(activeAccount!!)

            if (accounts.size > 0) {
                accounts[0].isActive = true
                activeAccount = accounts[0]

                Timber.d("Saving account with id ${accounts[0].id}")

                accountDao.insertOrReplace(accounts[0])
            } else {
                activeAccount = null
            }
            return activeAccount
        }
    }

    /**
     * updates the current account with new information from the mastodon api
     * and saves it in the database
     * @param account the [Account] object returned from the api
     */
    fun updateActiveAccount(account: Account) {
        activeAccount?.let {
            it.accountId = account.id
            it.username = account.username
            it.displayName = account.name
            it.profilePictureUrl = account.avatar
            it.defaultPostPrivacy = account.source?.privacy ?: Status.Visibility.PUBLIC
            it.defaultMediaSensitivity = account.source?.sensitive ?: false
            it.emojis = account.emojis ?: emptyList()

            Timber.d("updateActiveAccount: saving account with id ${it.id}")

            it.id = accountDao.insertOrReplace(it)

            val accountIndex = accounts.indexOf(it)

            if (accountIndex != -1) {
                // in case the user was already logged in with this account, remove the
                // old information
                accounts.removeAt(accountIndex)
                accounts.add(accountIndex, it)
            } else {
                accounts.add(it)
            }
        }
    }

    /**
     * changes the active account
     * @param accountId the database id of the new active account
     */
    fun setActiveAccount(accountId: Long) {
        activeAccount?.let {
            Timber.d("Saving account with id ${it.id}")

            it.isActive = false
            saveAccount(it)
        }

        activeAccount = accounts.find { (id) ->
            id == accountId
        }

        activeAccount?.let {
            it.isActive = true
            accountDao.insertOrReplace(it)
        }
    }

    /**
     * @return an immutable list of all accounts in the database with the active account first
     */
    fun getAllAccountsOrderedByActive(): List<AccountEntity> {
        val accountsCopy = accounts.toMutableList()
        accountsCopy.sortWith(
            Comparator { l, r ->
                when {
                    l.isActive && !r.isActive -> -1
                    r.isActive && !l.isActive -> 1
                    else -> 0
                }
            }
        )

        return accountsCopy
    }

    /**
     * Get the correct key to save the preference for push notifications per account
     *
     * @return Key + username and instance, key if account is null
     */
    fun pushNotificationPrefKey(): String {
        return activeAccount?.let { account ->
            "${PrefKeys.LIVE_NOTIFICATIONS}+${account.fullName}"
        } ?: PrefKeys.LIVE_NOTIFICATIONS
    }

    fun hideUnifiedPushEnrollDialogPrefKey(): String {
        return activeAccount?.let { account ->
            "${PrefKeys.UNIFIEDPUSH_ENROLL_DIALOG}+${account.fullName}"
        } ?: PrefKeys.UNIFIEDPUSH_ENROLL_DIALOG
    }

    fun hasNotificationsEnabled(): Boolean {
        return (
            activeAccount?.unifiedPushUrl?.isNotBlank().orEmpty() &&
                activeAccount?.unifiedPushInstance?.isNotBlank().orEmpty()
            )
    }

    fun getAccountByUnifiedPushInstance(instance: String): AccountEntity? {
        return accounts.firstOrNull { it.unifiedPushInstance.equals(instance, true) }
    }

    /**
     * Finds an account by its database id
     * @param accountId the id of the account
     * @return the requested account or null if it was not found
     */
    fun getAccountById(accountId: Long): AccountEntity? {
        return accounts.find { (id) ->
            id == accountId
        }
    }
}
