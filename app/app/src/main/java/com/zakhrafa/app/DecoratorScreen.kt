package com.zakhrafa.app

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
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

@Composable
internal fun DecoratorScreen() {
    var text by rememberSaveable { mutableStateOf("زخرفة") }
    var filter by rememberSaveable { mutableStateOf("all") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val favorites by FavoritesManager.getFavorites(context).collectAsState(initial = emptySet())
    val results = remember(text, filter) {
        if (text.isBlank()) emptyList() else ZakhrafaEngine.generateAll(text, filter)
    }
    val selectedLabel = categories.firstOrNull { it.first == filter }?.second ?: "الكل"

    LazyVerticalGrid(
        columns = GridCells.Adaptive(300.dp),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            GeneratorCard(
                text = text,
                filter = filter,
                resultCount = results.size,
                favoriteCount = favorites.size,
                onTextChange = { text = limitInput(it) },
                onFilterChange = { filter = it },
                onClear = { text = "" },
                onPaste = {
                    readClipboard(context)?.let { text = limitInput(it) }
                        ?: Toast.makeText(context, "الحافظة فارغة", Toast.LENGTH_SHORT).show()
                },
                onRandomCopy = { results.randomOrNull()?.let { copyToClipboard(context, it.text) } }
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            ResultsSummary(
                label = selectedLabel,
                count = results.size,
                canShare = results.isNotEmpty(),
                onShare = {
                    shareText(context, results.take(40).joinToString("\n") { it.text })
                }
            )
        }

        if (results.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(
                    icon = "✦",
                    title = "اكتب اسماً أو نصاً",
                    subtitle = "ستظهر لك الزخارف مباشرة"
                )
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
private fun GeneratorCard(
    text: String,
    filter: String,
    resultCount: Int,
    favoriteCount: Int,
    onTextChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onClear: () -> Unit,
    onPaste: () -> Unit,
    onRandomCopy: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "زخرف اسمك بذوقك",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "انسخ، شارك، واحفظ الأشكال التي تعجبك",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
                    ) {
                        Text(
                            "بدون إنترنت",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    label = { Text("اكتب الاسم أو النص") },
                    supportingText = { Text("${text.codePointCount(0, text.length)} / $MAX_INPUT_LENGTH") },
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

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onPaste, modifier = Modifier.weight(1f)) {
                        Text("لصق")
                    }
                    FilledTonalButton(
                        onClick = onRandomCopy,
                        enabled = resultCount > 0,
                        modifier = Modifier.weight(2f)
                    ) {
                        Text("فاجئني وانسخ")
                    }
                }

                Spacer(Modifier.height(14.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { (id, label) ->
                        CategoryChip(
                            label = label,
                            selected = filter == id,
                            onClick = { onFilterChange(id) }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill("النتائج", resultCount.toString())
                    StatPill("المفضلة", favoriteCount.toString())
                }
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(value, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ResultsSummary(label: String, count: Int, canShare: Boolean, onShare: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("نتائج $label", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text("$count شكل جاهز", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onShare, enabled = canShare) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("مشاركة مجموعة")
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
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.widthIn(max = 220.dp)
                ) {
                    Text(
                        result.style,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onFavorite) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "إزالة من المفضلة" else "إضافة للمفضلة",
                        tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { copyToClipboard(context, result.text) },
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    result.text,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 20.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 21.sp,
                    lineHeight = 31.sp,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { copyToClipboard(context, result.text) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("نسخ", fontWeight = FontWeight.ExtraBold)
                }
                OutlinedButton(
                    onClick = { shareText(context, result.text) },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp)
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
