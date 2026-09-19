package com.rizal.radiotune.ui.countries

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rizal.radiotune.data.model.Country
import com.rizal.radiotune.ui.AppViewModelProvider
import com.rizal.radiotune.ui.components.EmptyView
import com.rizal.radiotune.ui.components.ErrorView
import com.rizal.radiotune.ui.components.LoadingView
import com.rizal.radiotune.ui.components.SearchField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountriesScreen(
    onCountryClick: (Country) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: CountriesViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier.fillMaxSize()) {
        TopAppBar(title = { Text("RadioTune") })

        SearchField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            placeholder = "Search countries",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        when {
            state.isLoading -> LoadingView()

            state.error != null && state.countries.isEmpty() ->
                ErrorView(state.error.orEmpty(), onRetry = { viewModel.load(force = true) })

            state.visibleCountries.isEmpty() ->
                EmptyView("No countries match your search")

            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(state.visibleCountries, key = { it.code }) { country ->
                    CountryRow(country = country, onClick = { onCountryClick(country) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun CountryRow(
    country: Country,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(country.name) },
        supportingContent = {
            Text(
                text = "${country.stationCount} stations",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}
