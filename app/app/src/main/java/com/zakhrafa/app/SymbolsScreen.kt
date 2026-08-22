package com.zakhrafa.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zakhrafa.engine.styles.Symbols

@Composable
internal fun SymbolsScreen() {
    val context = LocalContext.current
    val categoryNames = Symbols.categories.keys.toList()
    var selectedCategory by rememberSaveable { mutableStateOf(categoryNames.firstOrNull().orEmpty()) }
    val symbols = Symbols.categories[selectedCategory].orEmpty()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(68.dp),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("اضغط على الرمز لنسخه", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${symbols.size}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(categoryNames) { category ->
                        CategoryChip(
                            label = category,
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category }
                        )
                    }
                }
            }
        }

        itemsIndexed(symbols, key = { index, symbol -> "$selectedCategory:$index:$symbol" }) { _, symbol ->
            Surface(
                onClick = { copyToClipboard(context, symbol) },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(symbol, fontSize = 24.sp, textAlign = TextAlign.Center, maxLines = 2)
                }
            }
        }
    }
}
