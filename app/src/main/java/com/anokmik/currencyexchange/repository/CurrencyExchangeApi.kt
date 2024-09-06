package com.anokmik.currencyexchange.repository

import retrofit2.http.GET

interface CurrencyExchangeApi {

    @GET("tasks/api/currency-exchange-rates")
    suspend fun getRates(): CurrencyExchangeModel

}
