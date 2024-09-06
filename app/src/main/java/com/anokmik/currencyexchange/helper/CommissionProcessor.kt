package com.anokmik.currencyexchange.helper

import dagger.hilt.android.scopes.ViewModelScoped
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

@ViewModelScoped
class CommissionProcessor @Inject constructor() {

    private val zero: BigDecimal = BigDecimal(0.0).setScale(2, RoundingMode.HALF_EVEN)
    private val multiplier: BigDecimal = BigDecimal(0.007).setScale(3, RoundingMode.HALF_EVEN)
    private var exchangeCount: Int = 0

    fun calculate(amount: BigDecimal): BigDecimal =
        if (exchangeCount < 5) zero else amount.multiply(multiplier)
            .setScale(2, RoundingMode.HALF_EVEN)

    fun completeExchange() {
        exchangeCount += 1
    }

}
