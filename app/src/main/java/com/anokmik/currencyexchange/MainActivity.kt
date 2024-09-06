package com.anokmik.currencyexchange

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.anokmik.currencyexchange.account.Account
import com.anokmik.currencyexchange.account.AccountAction
import com.anokmik.currencyexchange.account.AccountViewModel
import com.anokmik.currencyexchange.theme.CurrencyExchangeTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CurrencyExchangeTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = stringResource(id = R.string.app_name)
                                )
                            }
                        )
                    }
                ) { innerPadding ->
                    val context = LocalContext.current
                    val viewModel: AccountViewModel by viewModels()

                    LaunchedEffect(viewModel) {
                        viewModel.messageFlow.collectLatest { message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    }

                    Account(
                        modifier = Modifier.padding(innerPadding),
                        state = viewModel.stateFlow.collectAsState().value
                    ) { action ->
                        when (action) {
                            is AccountAction.UpdateSellCurrency -> viewModel.changeSellCurrency(action.currency)
                            is AccountAction.UpdateReceiveCurrency -> viewModel.changeReceiveCurrency(action.currency)
                            is AccountAction.UpdateSellAmount -> viewModel.changeSellAmount(action.sellAmountValue)
                            AccountAction.PerformExchange -> viewModel.performExchange()
                        }
                    }
                }
            }
        }
    }

}
