package com.zakhrafa.app

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zakhrafa.engine.ZakhrafaEngine
import com.zakhrafa.engine.models.DecorationResult
import kotlinx.coroutines.launch

private val categories = listOf(
    "all" to "الكل",
    "arabic" to "عربي",
    "english" to "English",
    "complex" to "نادر",
    "symbols" to "إطارات",
    "pubg" to "PUBG",
    "freefire" to "Free Fire",
    "tiktok" to "TikTok",
    "instagram" to "Instagram",
    "facebook" to "Facebook"
)

private data class PreviewExample(val input: String, val decorated: String)

@Composable
internal fun DecoratorScreen() {
    var text by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("all") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val favorites by FavoritesManager.getFavorites(context).collectAsState(initial = emptySet())
    val results = remember(text, filter) {
        if (text.isBlank()) emptyList() else ZakhrafaEngine.generateAll(text, filter)
    }
    val selectedLabel = categories.firstOrNull { it.first == filter }?.second ?: "الكل"

    LazyVerticalGrid(
        columns = GridCells.Adaptive(320.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            InputCard(
                text = text,
                resultCount = results.size,
                onTextChange = { text = limitInput(it) },
                onClear = { text = "" },
                onPaste = {
                    readClipboard(context)?.let { text = limitInput(it) }
                        ?: Toast.makeText(context, "الحافظة فارغة", Toast.LENGTH_SHORT).show()
                },
                onRandomCopy = { results.randomOrNull()?.let { copyToClipboard(context, it.text) } }
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            LazyRow(
                contentPadding = PaddingValues(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                items(categories) { (id, label) ->
                    CategoryChip(
                        label = label,
                        selected = filter == id,
                        onClick = { filter = id }
                    )
                }
            }
        }

        if (results.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ResultsSummary(
                    label = selectedLabel,
                    count = results.size,
                    onShare = {
                        shareText(context, results.take(40).joinToString("\n") { it.text })
                    }
                )
            }
        }

        if (results.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                StarterPreview(onSelect = { text = it })
            }
        } else {
            items(results, key = { "${it.category}:${it.style}:${it.text}" }) { result ->
                ResultCard(
                    result = result,
                    isFavorite = result.text in favorites,
                    onFavorite = {
                        scope.launch { FavoritesManager.toggleFavorite(context, result.text) }
                    }
                )
            }
        }
    }
}

@Composable
private fun StarterPreview(onSelect: (String) -> Unit) {
    val examples = remember {
        listOf(
            previewExample("محمد", "arabic", "ar-legacy"),
            previewExample("Zakhrafa", "english", "en-legacy")
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("معاينة سريعة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("اضغط على مثال لتجربته", color = MaterialTheme.colorScheme.onSurfaceVariant)

            examples.forEach { example ->
                Surface(
                    onClick = { onSelect(example.input) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                example.input,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                            Text(
                                example.decorated,
                                style = MaterialTheme.typography.titleLarge.copy(textDirection = TextDirection.Content),
                                maxLines = 2
                            )
                        }
                        Text("جرّب", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun previewExample(input: String, filter: String, preferredCategory: String): PreviewExample {
    val results = ZakhrafaEngine.generateAll(input, filter)
    val decorated = results.firstOrNull { it.category == preferredCategory }?.text
        ?: results.firstOrNull()?.text
        ?: input
    return PreviewExample(input, decorated)
}

@Composable
private fun InputCard(
    text: String,
    resultCount: Int,
    onTextChange: (String) -> Unit,
    onClear: () -> Unit,
    onPaste: () -> Unit,
    onRandomCopy: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(14.dp),
                placeholder = { Text("اكتب اسماً أو نصاً") },
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    textDirection = TextDirection.Content
                ),
                trailingIcon = {
                    if (text.isNotEmpty()) {
                        IconButton(onClick = onClear) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح النص")
                        }
                    }
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onPaste) { Text("لصق") }
                TextButton(onClick = onRandomCopy, enabled = resultCount > 0) {
                    Text("اختيار عشوائي")
                }
            }
        }
    }
}

@Composable
internal fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun ResultsSummary(label: String, count: Int, onShare: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$count نتيجة · $label",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onShare) {
            Icon(Icons.Default.Share, contentDescription = "مشاركة النتائج")
        }
    }
}

@Composable
internal fun ResultCard(
    result: DecorationResult,
    isFavorite: Boolean,
    onFavorite: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier
                .clickable { copyToClipboard(context, result.text) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    result.style,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                IconButton(onClick = onFavorite, modifier = Modifier.size(40.dp)) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "إزالة من المفضلة" else "إضافة للمفضلة",
                        tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                result.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center,
                fontSize = 21.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { copyToClipboard(context, result.text) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("نسخ")
                }
                OutlinedButton(
                    onClick = { shareText(context, result.text) },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "مشاركة", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

internal fun limitInput(value: String): String {
    val count = value.codePointCount(0, value.length)
    if (count <= MAX_INPUT_LENGTH) return value
    return value.substring(0, value.offsetByCodePoints(0, MAX_INPUT_LENGTH))
}
