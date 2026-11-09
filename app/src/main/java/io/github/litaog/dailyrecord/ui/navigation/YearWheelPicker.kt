package io.github.litaog.dailyrecord.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyColumnItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.ui.components.BrandIcon
import io.github.litaog.dailyrecord.ui.components.BrandIconAsset
import io.github.litaog.dailyrecord.ui.components.ChevronIcon
import io.github.litaog.dailyrecord.ui.components.brandIconTheme
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSizes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurfaceMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import java.time.YearMonth

@Composable
internal fun YearWheelPicker(
    selectedYear: Int,
    years: List<Int>,
    colors: RecordModuleColorTokens,
    onYearSelected: (Int) -> Unit,
) {
    val firstYear = years.firstOrNull() ?: selectedYear
    val lastYear = years.lastOrNull() ?: selectedYear
    val visibleYears = ((firstYear - 3)..(lastYear + 3)).toList()
    val selectedIndex = visibleYears.indexOf(selectedYear).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (selectedIndex - 2).coerceAtLeast(0))
    LaunchedEffect(selectedYear) {
        // Keep the selected row and its neighbours in the semantics tree immediately.
        // An animated first scroll can briefly expose only the selected row, making the
        // adjacent years impossible to discover through TalkBack or UI tests.
        listState.scrollToItem((visibleYears.indexOf(selectedYear) - 2).coerceAtLeast(0))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(270.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(DailyRecordSurfaceMuted)
            .border(1.dp, DailyRecordDivider, RoundedCornerShape(14.dp)),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 42.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            lazyColumnItems(visibleYears, key = { it }) { year ->
                val selected = year == selectedYear
                val enabled = year in years
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (selected) colors.primary.copy(alpha = .16f) else Color.Transparent,
                        )
                        .border(
                            width = if (selected) 1.dp else 0.dp,
                            color = if (selected) colors.primary.copy(alpha = .72f) else Color.Transparent,
                            shape = RoundedCornerShape(16.dp),
                        )
                        .clickable(enabled = enabled, role = Role.Button) {
                            onYearSelected(year)
                        }
                        .semantics {
                            role = Role.Button
                            this.selected = selected
                            contentDescription = AppCopy.Navigation.selectYearDescription(year)
                            if (!enabled) disabled()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = AppCopy.Navigation.yearTitle(year),
                        color = when {
                            selected -> colors.primary
                            enabled -> DailyRecordTextSecondary
                            else -> DailyRecordDivider
                        },
                        style = if (selected) {
                            MaterialTheme.typography.titleLarge
                        } else {
                            MaterialTheme.typography.titleMedium
                        },
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
internal fun YearArrow(
    forward: Boolean,
    enabled: Boolean,
    colors: RecordModuleColorTokens,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(DailyRecordSizes.MinimumTouchTarget)
            .clip(CircleShape)
            .background(if (enabled) colors.soft.copy(alpha = .16f) else Color.Transparent)
            .border(
                width = if (enabled) 1.dp else 0.dp,
                color = if (enabled) colors.soft.copy(alpha = .60f) else Color.Transparent,
                shape = CircleShape,
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = AppCopy.Navigation.nextYearDescription(forward)
                if (!enabled) disabled()
            },
        contentAlignment = Alignment.Center,
    ) {
        ChevronIcon(
            forward = forward,
            // The locked brand artwork cannot be tinted, so the disabled state
            // is carried by alpha plus the disabled() semantics above.
            modifier = Modifier.size(20.dp).alpha(if (enabled) 1f else .3f),
            theme = colors.brandIconTheme,
        )
    }
}

@Composable
internal fun DownChevronIcon(
    theme: io.github.litaog.dailyrecord.ui.components.BrandIconTheme,
) {
    BrandIcon(
        asset = BrandIconAsset.Down,
        theme = theme,
        modifier = Modifier.size(20.dp),
    )
}

internal fun YearMonth.coerceIn(minimum: YearMonth, maximum: YearMonth): YearMonth = when {
    this < minimum -> minimum
    this > maximum -> maximum
    else -> this
}
