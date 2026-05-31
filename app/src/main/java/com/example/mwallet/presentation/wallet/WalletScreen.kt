package com.example.mwallet.presentation.wallet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mwallet.domain.model.Transaction
import com.example.mwallet.domain.model.TransactionType
import com.example.mwallet.domain.model.UserSearchResult
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    viewModel: WalletViewModel,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDepositDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error, uiState.successMessage) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    if (showDepositDialog) {
        AmountDialog(
            title = "Пополнение счёта",
            onDismiss = { showDepositDialog = false },
            onConfirm = { amount ->
                showDepositDialog = false
                viewModel.deposit(amount)
            }
        )
    }

    if (showTransferDialog) {
        TransferDialog(
            transferSearch = uiState.transferSearch,
            onSearchQueryChange = viewModel::onTransferSearchQueryChange,
            onSelectRecipient = viewModel::selectTransferRecipient,
            onDismiss = {
                showTransferDialog = false
                viewModel.resetTransferSearch()
            },
            onConfirm = { amount, description ->
                val recipient = uiState.transferSearch.selectedUser?.username
                    ?: uiState.transferSearch.query.trim()
                showTransferDialog = false
                viewModel.resetTransferSearch()
                viewModel.transfer(recipient, amount, description)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mwallet") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                    IconButton(onClick = { viewModel.logout(onLogout) }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Выйти")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FloatingActionButton(onClick = { showDepositDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Пополнить")
                }
                FloatingActionButton(onClick = { showTransferDialog = true }) {
                    Icon(Icons.Default.Send, contentDescription = "Перевод")
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading && uiState.wallet == null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                uiState.wallet?.let { wallet ->
                    BalanceCard(wallet.balance, wallet.currency, wallet.username)
                }
                Spacer(Modifier.height(16.dp))
                Text("История операций", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.transactions) { transaction ->
                        TransactionItem(transaction)
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceCard(balance: BigDecimal, currency: String, username: String) {
    val formatter = NumberFormat.getNumberInstance(Locale("ru", "RU"))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Привет, $username", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${formatter.format(balance)} $currency",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TransactionItem(transaction: Transaction) {
    val formatter = NumberFormat.getNumberInstance(Locale("ru", "RU"))
    val isOutgoing = transaction.counterparty?.startsWith("Кому:") == true
    val prefix = when (transaction.type) {
        TransactionType.DEPOSIT -> "+"
        TransactionType.TRANSFER -> if (isOutgoing) "-" else "+"
        else -> ""
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "$prefix${formatter.format(transaction.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            transaction.counterparty?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            transaction.description?.let { desc ->
                if (desc != transaction.counterparty) {
                    Text(desc, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun AmountDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Сумма") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(amount) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
private fun TransferDialog(
    transferSearch: TransferSearchState,
    onSearchQueryChange: (String) -> Unit,
    onSelectRecipient: (UserSearchResult) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Перевод") },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp)) {
                OutlinedTextField(
                    value = transferSearch.query,
                    onValueChange = onSearchQueryChange,
                    label = { Text("Поиск: никнейм или телефон") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (transferSearch.isSearching) {
                    Spacer(Modifier.height(8.dp))
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                transferSearch.searchError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                transferSearch.selectedUser?.let { user ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Получатель: ${user.username}" + (user.phone?.let { " ($it)" } ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (transferSearch.results.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    transferSearch.results.forEach { user ->
                        HorizontalDivider()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectRecipient(user) }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(user.username, fontWeight = FontWeight.Medium)
                            user.phone?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Сумма") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Комментарий") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(amount, description.ifBlank { null }) },
                enabled = transferSearch.query.isNotBlank() && amount.isNotBlank()
            ) {
                Text("Перевести")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
