package com.example.mwallet.presentation.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mwallet.domain.model.Transaction
import com.example.mwallet.domain.model.UserSearchResult
import com.example.mwallet.domain.model.Wallet
import com.example.mwallet.domain.usecase.DepositUseCase
import com.example.mwallet.domain.usecase.GetTransactionsUseCase
import com.example.mwallet.domain.usecase.GetWalletUseCase
import com.example.mwallet.domain.usecase.LogoutUseCase
import com.example.mwallet.domain.usecase.SearchUsersUseCase
import com.example.mwallet.data.remote.ApiException
import com.example.mwallet.domain.usecase.TransferUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class TransferSearchState(
    val query: String = "",
    val results: List<UserSearchResult> = emptyList(),
    val selectedUser: UserSearchResult? = null,
    val isSearching: Boolean = false,
    val searchError: String? = null
)

data class WalletUiState(
    val isLoading: Boolean = true,
    val wallet: Wallet? = null,
    val transactions: List<Transaction> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null,
    val transferSearch: TransferSearchState = TransferSearchState()
)

class WalletViewModel(
    private val getWalletUseCase: GetWalletUseCase,
    private val depositUseCase: DepositUseCase,
    private val transferUseCase: TransferUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val searchUsersUseCase: SearchUsersUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val onSessionExpired: () -> Unit = {}
) : ViewModel() {

    private fun mapError(e: Throwable): String {
        if (e is ApiException && e.code == 401) {
            onSessionExpired()
            return "Сессия истекла. Войдите снова"
        }
        return e.message ?: "Ошибка"
    }

    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val wallet = getWalletUseCase()
                val transactions = getTransactionsUseCase()
                wallet to transactions
            }.onSuccess { (wallet, transactions) ->
                _uiState.update {
                    it.copy(isLoading = false, wallet = wallet, transactions = transactions)
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = mapError(e)) }
            }
        }
    }

    fun deposit(amountText: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }
            runCatching {
                depositUseCase(BigDecimal(amountText.replace(',', '.')))
            }.onSuccess { wallet ->
                val transactions = getTransactionsUseCase()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        wallet = wallet,
                        transactions = transactions,
                        successMessage = "Счёт пополнен"
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = mapError(e)) }
            }
        }
    }

    fun transfer(toUsername: String, amountText: String, description: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }
            runCatching {
                transferUseCase(toUsername, BigDecimal(amountText.replace(',', '.')), description)
            }.onSuccess { wallet ->
                val transactions = getTransactionsUseCase()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        wallet = wallet,
                        transactions = transactions,
                        successMessage = "Перевод выполнен"
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = mapError(e)) }
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            logoutUseCase()
            onLoggedOut()
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }

    fun resetTransferSearch() {
        _uiState.update { it.copy(transferSearch = TransferSearchState()) }
    }

    fun onTransferSearchQueryChange(query: String) {
        _uiState.update {
            it.copy(
                transferSearch = it.transferSearch.copy(
                    query = query,
                    selectedUser = null,
                    searchError = null
                )
            )
        }
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _uiState.update {
                it.copy(transferSearch = it.transferSearch.copy(results = emptyList(), isSearching = false))
            }
            return
        }
        searchJob = viewModelScope.launch {
            delay(350)
            _uiState.update {
                it.copy(transferSearch = it.transferSearch.copy(isSearching = true))
            }
            runCatching { searchUsersUseCase(query) }
                .onSuccess { results ->
                    _uiState.update {
                        it.copy(
                            transferSearch = it.transferSearch.copy(
                                results = results,
                                isSearching = false
                            )
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            transferSearch = it.transferSearch.copy(
                                isSearching = false,
                                searchError = mapError(e),
                                results = emptyList()
                            )
                        )
                    }
                }
        }
    }

    fun selectTransferRecipient(user: UserSearchResult) {
        _uiState.update {
            it.copy(
                transferSearch = it.transferSearch.copy(
                    selectedUser = user,
                    query = user.username
                )
            )
        }
    }
}
