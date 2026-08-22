package com.zakhrafa.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
        columns = GridCells.Adaptive(76.dp),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("مكتبة الرموز", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                            Text("اضغط على أي رمز لنسخه مباشرة", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                "${Symbols.categories.values.sumOf { it.size }} رمز",
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
                Text(selectedCategory, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text("${symbols.size} رمز جاهز", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        itemsIndexed(symbols, key = { index, symbol -> "$selectedCategory:$index:$symbol" }) { _, symbol ->
            Surface(
                onClick = { copyToClipboard(context, symbol) },
                modifier = Modifier.size(76.dp),
                shape = RoundedCornerShape(18.dp),
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
