package com.rizal.radiotune.ui.countries

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rizal.radiotune.R
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
    onCheckForUpdates: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: CountriesViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val state by viewModel.state.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var searchFocused by remember { mutableStateOf(false) }
    val searching = state.query.isNotEmpty() || searchFocused
    val barCollapsed = scrollBehavior.state.collapsedFraction >= 1f

    Column(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        TopAppBar(
            title = { Text("RadioTune") },
            actions = {
                IconButton(onClick = onCheckForUpdates) {
                    Icon(
                        painter = painterResource(R.drawable.ic_download),
                        contentDescription = "Check for updates",
                    )
                }
            },
            scrollBehavior = scrollBehavior,
        )

        // Collapses with the bar unless the user is actually searching.
        AnimatedVisibility(visible = !barCollapsed || searching) {
            SearchField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = "Search countries",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .onFocusChanged { searchFocused = it.isFocused },
            )
        }

        when {
            state.isLoading -> LoadingView()

            state.error != null && state.countries.isEmpty() ->
                ErrorView(state.error.orEmpty(), onRetry = { viewModel.load(force = true) })

            state.visibleCountries.isEmpty() ->
                EmptyView("No countries match your search")

            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = COUNTRY_COLUMN_MIN_WIDTH),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.visibleCountries, key = { it.code }) { country ->
                    CountryRow(country = country, onClick = { onCountryClick(country) })
                }
            }
        }
    }
}

private val COUNTRY_COLUMN_MIN_WIDTH = 220.dp

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
