package com.anokmik.currencyexchange.account

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anokmik.currencyexchange.R
import com.anokmik.currencyexchange.theme.CurrencyExchangeTheme
import java.math.BigDecimal
import java.math.RoundingMode

private val zero = BigDecimal(0.0).setScale(2, RoundingMode.HALF_EVEN)

@Composable
fun Account(
    modifier: Modifier = Modifier,
    state: AccountState,
    action: (AccountAction) -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.sell),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CurrencyDropDownMenu(
                        currencies = state.currencies,
                        selectedCurrency = state.sellCurrency
                    ) { currency ->
                        action(AccountAction.UpdateSellCurrency(currency))
                    }
                }
                Spacer(modifier = Modifier.width(32.dp))
                Column {
                    CurrencyBalanceTitle(
                        currencyBalance = state.sellCurrencyBalance
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.sellAmountValue,
                        onValueChange = { value ->
                            action(AccountAction.UpdateSellAmount(value))
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        textStyle = MaterialTheme.typography.titleMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            BalanceError(
                error = state.sellAmountError
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.receive),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CurrencyDropDownMenu(
                        currencies = state.currencies,
                        selectedCurrency = state.receiveCurrency
                    ) { currency ->
                        action(AccountAction.UpdateReceiveCurrency(currency))
                    }
                }
                Spacer(modifier = Modifier.width(32.dp))
                Column {
                    CurrencyBalanceTitle(
                        currencyBalance = state.receiveCurrencyBalance
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.receiveAmount.toString(),
                        onValueChange = {

                        },
                        readOnly = true,
                        textStyle = MaterialTheme.typography.titleMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            CommissionInfo(
                commission = state.commission
            )
            Spacer(modifier = Modifier.height(16.dp))
            ExchangeButton(
                isEnabled = state.sellAmountError == SellAmountError.EmptyError || state.receiveAmount > zero
            ) {
                action(AccountAction.PerformExchange)
            }
            Spacer(modifier = Modifier.height(32.dp))
            BalancesTitle()
            Spacer(modifier = Modifier.height(16.dp))
            Balances(
                balances = state.balances
            )
        }

        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun BalanceError(
    modifier: Modifier = Modifier,
    error: SellAmountError
) {
    Text(
        modifier = modifier.fillMaxWidth(),
        text = when (error) {
            SellAmountError.InputCorrectValue -> stringResource(R.string.error_input_correct_value)
            SellAmountError.SellBalanceLessZero -> stringResource(R.string.error_sell_balance_not_less_zero)
            SellAmountError.EmptyError -> ""
        },
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.titleMedium.copy(
            color = MaterialTheme.colorScheme.error
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyDropDownMenu(
    modifier: Modifier = Modifier,
    currencies: List<String>,
    selectedCurrency: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        }
    ) {
        OutlinedTextField(
            modifier = modifier
                .menuAnchor()
                .width(104.dp),
            value = selectedCurrency,
            onValueChange = {

            },
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            textStyle = MaterialTheme.typography.titleMedium
        )
        ExposedDropdownMenu(
            modifier = modifier
                .exposedDropdownSize(true),
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            currencies.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(text = currency) },
                    onClick = {
                        onSelected(currency)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CurrencyBalanceTitle(
    currencyBalance: BigDecimal
) {
    Text(
        text = stringResource(R.string.balance, currencyBalance.toString()),
        style = MaterialTheme.typography.titleSmall
    )
}

@Composable
private fun CommissionInfo(
    modifier: Modifier = Modifier,
    commission: BigDecimal
) {
    Text(
        modifier = modifier.fillMaxWidth(),
        text = if (commission > BigDecimal(0.0)) stringResource(R.string.commission, commission.toString()) else "",
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.titleSmall
    )
}

@Composable
private fun ExchangeButton(
    modifier: Modifier = Modifier,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier.fillMaxWidth(),
        shape = RectangleShape,
        contentPadding = PaddingValues(8.dp),
        enabled = isEnabled,
        onClick = onClick
    ) {
        Text(
            text = stringResource(R.string.exchange),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun BalancesTitle(
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier.fillMaxWidth(),
        text = stringResource(R.string.balances),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.titleMedium
    )
}

@Composable
private fun Balances(
    modifier: Modifier = Modifier,
    balances: LinkedHashMap<String, BigDecimal>
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        val items = balances.entries.toList()

        itemsIndexed(items) { index, entry ->
            BalanceItem(
                currency = entry.key,
                value = entry.value
            )
            if (index != items.lastIndex) {
                Spacer(modifier = modifier.height(16.dp))
                HorizontalDivider(
                    modifier = modifier.padding(horizontal = 8.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun BalanceItem(
    modifier: Modifier = Modifier,
    currency: String,
    value: BigDecimal
) {
    Row {
        Text(
            text = currency,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = modifier.weight(1.0f))
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            )
        )
    }
}

sealed interface AccountAction {
    data class UpdateSellCurrency(val currency: String) : AccountAction
    data class UpdateReceiveCurrency(val currency: String) : AccountAction
    data class UpdateSellAmount(val sellAmountValue: String) : AccountAction
    data object PerformExchange : AccountAction
}

@Preview(showBackground = true)
@Composable
fun AccountPreview() {
    val zero = BigDecimal(0.0).setScale(2, RoundingMode.HALF_EVEN)
    val pointSeven = BigDecimal(0.7).setScale(2, RoundingMode.HALF_EVEN)
    val oneHundred = BigDecimal(100.0).setScale(2, RoundingMode.HALF_EVEN)
    val oneHundredTen = BigDecimal(110.0).setScale(2, RoundingMode.HALF_EVEN)
    val oneThousand = BigDecimal(1000.0).setScale(2, RoundingMode.HALF_EVEN)

    CurrencyExchangeTheme {
        Account(
            state = AccountState(
                sellCurrency = "EUR",
                receiveCurrency = "USD",
                sellCurrencyBalance = oneThousand,
                receiveCurrencyBalance = zero,
                sellAmount = oneHundred,
                receiveAmount = oneHundredTen,
                commission = pointSeven,
                currencies = listOf(
                    "USD",
                    "EUR",
                    "UAH"
                ),
                balances = linkedMapOf(
                    "EUR" to oneThousand,
                    "UAH" to oneHundred
                ),
                sellAmountValue = oneHundred.toString(),
                sellAmountError = SellAmountError.SellBalanceLessZero,
            )
        ) {

        }
    }
}