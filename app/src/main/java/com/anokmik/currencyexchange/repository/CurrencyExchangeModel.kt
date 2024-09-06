package com.anokmik.currencyexchange.repository

import java.math.BigDecimal

data class CurrencyExchangeModel(
    val base: String,
    val date: String,
    val rates: Map<String, BigDecimal>
)
