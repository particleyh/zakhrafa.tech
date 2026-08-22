package com.zakhrafa.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zakhrafa.engine.models.DecorationResult
import kotlinx.coroutines.launch

@Composable
internal fun FavoritesScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val favorites by FavoritesManager.getFavorites(context).collectAsState(initial = emptySet())
    val history by CopyHistoryManager.getHistory(context).collectAsState(initial = emptyList())
    var selectedLibrary by rememberSaveable { mutableStateOf("favorites") }
    var query by rememberSaveable { mutableStateOf("") }
    var showClearDialog by rememberSaveable { mutableStateOf(false) }
    val sourceItems = if (selectedLibrary == "favorites") {
        favorites.sorted().map { it to it }
    } else {
        history.map { it.id to it.text }
    }
    val visibleItems = sourceItems.filter { (_, text) ->
        query.isBlank() || text.contains(query, ignoreCase = true)
    }
    val activeCount = sourceItems.size
    val activeLabel = if (selectedLibrary == "favorites") "المفضلة" else "النسخ الأخير"

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("مسح $activeLabel؟") },
            text = { Text("سيتم حذف $activeCount عنصراً من هذا الجهاز.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        if (selectedLibrary == "favorites") {
                            FavoritesManager.clearFavorites(context)
                        } else {
                            CopyHistoryManager.clearHistory(context)
                        }
                    }
                    showClearDialog = false
                }) { Text("مسح الكل", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("إلغاء") }
            }
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(300.dp),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("مكتبتك", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        Text("احتفظ بما يعجبك وارجع لما نسخته", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (activeCount > 0) {
                        TextButton(onClick = { showClearDialog = true }) {
                            Text("مسح الكل", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                Row(
                    modifier = Modifier.padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryChip(
                        label = "المفضلة (${favorites.size})",
                        selected = selectedLibrary == "favorites",
                        onClick = {
                            selectedLibrary = "favorites"
                            query = ""
                        }
                    )
                    CategoryChip(
                        label = "نسختها مؤخراً (${history.size})",
                        selected = selectedLibrary == "history",
                        onClick = {
                            selectedLibrary = "history"
                            query = ""
                        }
                    )
                }

                if (activeCount > 5) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it.take(MAX_INPUT_LENGTH) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        label = { Text("ابحث في المفضلة") },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "مسح البحث")
                                }
                            }
                        }
                    )
                }
            }
        }

        if (visibleItems.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(
                    icon = if (activeCount == 0 && selectedLibrary == "favorites") "♡" else if (activeCount == 0) "⌘" else "⌕",
                    title = when {
                        activeCount > 0 -> "لا توجد نتيجة"
                        selectedLibrary == "favorites" -> "لا توجد مفضلات بعد"
                        else -> "لم تنسخ شيئاً بعد"
                    },
                    subtitle = when {
                        activeCount > 0 -> "جرّب كلمة بحث أخرى"
                        selectedLibrary == "favorites" -> "اضغط على القلب بجانب أي زخرفة لحفظها"
                        else -> "أي زخرفة أو رمز تنسخه سيظهر هنا"
                    }
                )
            }
        } else {
            items(visibleItems, key = { it.first }) { (_, text) ->
                ResultCard(
                    result = DecorationResult(
                        text,
                        if (selectedLibrary == "favorites") "مفضلة" else "نُسخ مؤخراً",
                        selectedLibrary
                    ),
                    isFavorite = text in favorites,
                    onFavorite = {
                        scope.launch { FavoritesManager.toggleFavorite(context, text) }
                    }
                )
            }
        }
    }
}
