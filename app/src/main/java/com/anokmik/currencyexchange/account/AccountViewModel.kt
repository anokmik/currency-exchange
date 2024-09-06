package com.anokmik.currencyexchange.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anokmik.currencyexchange.helper.CommissionProcessor
import com.anokmik.currencyexchange.repository.CurrencyExchangeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

private val zero = BigDecimal(0.0).setScale(2, RoundingMode.HALF_EVEN)
private val one = BigDecimal(1.0).setScale(2, RoundingMode.HALF_EVEN)
private val oneThousand = BigDecimal(1000.0).setScale(2, RoundingMode.HALF_EVEN)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val repository: CurrencyExchangeRepository,
    private val commissionProcessor: CommissionProcessor
) : ViewModel() {

    private val _stateFlow = MutableStateFlow(AccountState())
    val stateFlow = _stateFlow.asStateFlow()

    private val _messageFlow = MutableSharedFlow<String>()
    val messageFlow = _messageFlow.asSharedFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _stateFlow.update { state -> state.copy(isLoading = true) }
            repository.getRates()
                .onSuccess { model ->
                    _stateFlow.update { state ->
                        val currencies = model.rates.keys.sorted()
                        val baseBalance = oneThousand
                        val firstCurrencyBalance = zero

                        state.copy(
                            isLoading = false,
                            baseCurrency = model.base,
                            sellCurrency = model.base,
                            receiveCurrency = currencies.first(),
                            sellCurrencyBalance = baseBalance,
                            receiveCurrencyBalance = firstCurrencyBalance,
                            currencies = currencies,
                            rates = model.rates,
                            balances = state.balances.apply {
                                put(model.base, baseBalance)
                            }
                        )
                    }
                }
                .onFailure { throwable ->
                    _stateFlow.update { state -> state.copy(isLoading = false) }
                    _messageFlow.emit(throwable.message ?: "Something wrong")
                }
        }
    }

    fun changeSellCurrency(sellCurrency: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _stateFlow.update { state ->
                val receiveCurrency = updateOppositeCurrency(
                    baseCurrency = state.baseCurrency,
                    newCurrency = sellCurrency,
                    oppositeCurrency = state.receiveCurrency
                )

                state.copy(
                    sellCurrency = sellCurrency,
                    receiveCurrency = receiveCurrency,
                    sellCurrencyBalance = state.balances.getOrDefault(sellCurrency, zero)
                        .setScale(2, RoundingMode.HALF_EVEN),
                    receiveCurrencyBalance = state.balances.getOrDefault(receiveCurrency, zero)
                        .setScale(2, RoundingMode.HALF_EVEN),
                    sellAmount = zero,
                    receiveAmount = zero,
                    commission = zero,
                    sellAmountValue = zero.toString()
                )
            }
        }
    }

    fun changeReceiveCurrency(receiveCurrency: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _stateFlow.update { state ->
                val sellCurrency = updateOppositeCurrency(
                    baseCurrency = state.baseCurrency,
                    newCurrency = receiveCurrency,
                    oppositeCurrency = state.sellCurrency
                )

                state.copy(
                    sellCurrency = sellCurrency,
                    receiveCurrency = receiveCurrency,
                    sellCurrencyBalance = state.balances.getOrDefault(sellCurrency, zero)
                        .setScale(2, RoundingMode.HALF_EVEN),
                    receiveCurrencyBalance = state.balances.getOrDefault(receiveCurrency, zero)
                        .setScale(2, RoundingMode.HALF_EVEN),
                    sellAmount = zero,
                    receiveAmount = zero,
                    commission = zero,
                    sellAmountValue = zero.toString()
                )
            }
        }
    }

    fun changeSellAmount(sellAmountValue: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sellAmount = sellAmountValue.toBigDecimal().setScale(2, RoundingMode.HALF_EVEN)

                with(stateFlow.value) {
                    val sellBalance = balances.getOrDefault(sellCurrency, zero)
                    val commission = commissionProcessor.calculate(sellAmount)

                    if (sellBalance >= sellAmount.plus(commission)) {
                        val receiveAmount = if (sellCurrency == baseCurrency) {
                            sellAmount.multiply(rates.getOrDefault(receiveCurrency, one))
                        } else {
                            sellAmount.divide(rates.getOrDefault(sellCurrency, one), 2, RoundingMode.HALF_EVEN)
                        }.setScale(2, RoundingMode.HALF_EVEN)

                        _stateFlow.update { state ->
                            state.copy(
                                sellAmount = sellAmount,
                                receiveAmount = receiveAmount,
                                commission = commission,
                                sellAmountValue = sellAmountValue,
                                sellAmountError = SellAmountError.EmptyError
                            )
                        }
                    } else {
                        _stateFlow.update { state ->
                            state.copy(
                                sellAmountValue = sellAmountValue,
                                sellAmountError = SellAmountError.SellBalanceLessZero
                            )
                        }
                    }
                }
            } catch (e: NumberFormatException) {
                _stateFlow.update { state ->
                    state.copy(
                        sellAmountValue = sellAmountValue,
                        sellAmountError = SellAmountError.InputCorrectValue
                    )
                }
            }
        }
    }

    fun performExchange() {
        viewModelScope.launch(Dispatchers.IO) {
            commissionProcessor.completeExchange()

            _stateFlow.update { state ->
                state.balances.apply {
                    get(state.sellCurrency)?.let { sellBalance ->
                        val newSellBalance = sellBalance.minus(state.sellAmount).minus(state.commission)
                        if (newSellBalance > zero) {
                            put(state.sellCurrency, newSellBalance)
                        } else {
                            remove(state.sellCurrency)
                        }
                    }
                    put(
                        state.receiveCurrency,
                        getOrDefault(state.receiveCurrency, zero).plus(state.receiveAmount)
                    )
                }

                state.copy(
                    sellCurrencyBalance = state.balances.getOrDefault(state.sellCurrency, zero),
                    receiveCurrencyBalance = state.balances.getOrDefault(state.receiveCurrency, zero),
                    sellAmount = zero,
                    receiveAmount = zero,
                    commission = zero,
                    sellAmountValue = zero.toString()
                )
            }
        }
    }

    private fun updateOppositeCurrency(baseCurrency: String, newCurrency: String, oppositeCurrency: String) =
        if ((newCurrency != baseCurrency) && (oppositeCurrency != baseCurrency)) {
            baseCurrency
        } else {
            oppositeCurrency
        }

}

data class AccountState(
    val isLoading: Boolean = false,
    val baseCurrency: String = "",
    val sellCurrency: String = "",
    val receiveCurrency: String = "",
    val sellCurrencyBalance: BigDecimal = zero,
    val receiveCurrencyBalance: BigDecimal = zero,
    val sellAmount: BigDecimal = zero,
    val receiveAmount: BigDecimal = zero,
    val commission: BigDecimal = zero,
    val currencies: List<String> = listOf(),
    val rates: Map<String, BigDecimal> = mapOf(),
    val balances: LinkedHashMap<String, BigDecimal> = linkedMapOf(),
    val sellAmountValue: String = zero.toString(),
    val sellAmountError: SellAmountError = SellAmountError.EmptyError
)

sealed interface SellAmountError {
    data object EmptyError : SellAmountError
    data object InputCorrectValue : SellAmountError
    data object SellBalanceLessZero : SellAmountError
}
