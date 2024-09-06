package com.anokmik.currencyexchange.repository

import dagger.hilt.android.scopes.ViewModelScoped
import retrofit2.HttpException
import javax.inject.Inject

@ViewModelScoped
class CurrencyExchangeRepository @Inject constructor(
    private val api: CurrencyExchangeApi
) {

    suspend fun getRates() =
        try {
            Result.success(api.getRates())
        } catch (e: HttpException) {
            Result.failure(e)
        }

}
