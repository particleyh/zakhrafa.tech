package com.zakhrafa.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zakhrafa.engine.ZakhrafaEngine
import com.zakhrafa.engine.models.DecorationResult
import com.zakhrafa.engine.styles.Symbols
import kotlinx.coroutines.launch

private val AppBackground = Color(0xFFF5F7FB)
private val AppSurface = Color.White
private val AppBorder = Color(0xFFDDE3EA)
private val AppText = Color(0xFF101828)
private val AppMuted = Color(0xFF667085)
private val AppPrimary = Color(0xFF136F63)
private val AppPrimarySoft = Color(0xFFE7F4F1)
private const val DECORATOR_PACKAGE_ID = "com.zakhrafa.tech"
private const val DECORATOR_PLAY_URL = "https://play.google.com/store/apps/details?id=$DECORATOR_PACKAGE_ID"
private const val DECORATOR_MARKET_URL = "market://details?id=$DECORATOR_PACKAGE_ID"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZakhrafaTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun ZakhrafaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = AppPrimary,
            secondary = Color(0xFF7C3AED),
            background = AppBackground,
            surface = AppSurface,
            onSurface = AppText
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.mipmap.ic_launcher),
                            contentDescription = "زخرفة مزخرف",
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("زخرفة مزخرف", fontWeight = FontWeight.ExtraBold, color = AppText)
                            Text("${ZakhrafaEngine.countStyles()} نمط ورموز", color = AppMuted, fontSize = 12.sp)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { shareApp(context) }) {
                        Icon(Icons.Default.Share, contentDescription = "مشاركة التطبيق", tint = AppText)
                    }
                    IconButton(onClick = { rateApp(context) }) {
                        Icon(Icons.Default.Star, contentDescription = "تقييم التطبيق", tint = AppText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppSurface)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = AppSurface, tonalElevation = 0.dp) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Star, contentDescription = null) },
                    label = { Text("زخرفة") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Face, contentDescription = null) },
                    label = { Text("رموز") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                    label = { Text("المفضلة") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                0 -> DecorateTab()
                1 -> SymbolsTab()
                2 -> FavoritesTab()
            }
        }
    }
}

@Composable
fun DecorateTab() {
    var text by remember { mutableStateOf("زخرفة") }
    var filter by remember { mutableStateOf("all") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val favorites by FavoritesManager.getFavorites(context).collectAsState(initial = emptySet())

    val categories = listOf(
        "all" to "الكل",
        "arabic" to "عربي",
        "english" to "انجليزي",
        "complex" to "نادر",
        "symbols" to "رموز",
        "pubg" to "ببجي",
        "freefire" to "فري فاير",
        "tiktok" to "تيك توك",
        "instagram" to "انستقرام",
        "facebook" to "فيسبوك"
    )
    val results = remember(text, filter) {
        if (text.isBlank()) emptyList() else ZakhrafaEngine.generateAll(text, filter)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GeneratorPanel(
            text = text,
            filter = filter,
            categories = categories,
            resultCount = results.size,
            onTextChange = { text = it },
            onCategorySelect = { selected ->
                filter = selected
                if (selected == "english" && !text.any { it.lowercaseChar() in 'a'..'z' }) {
                    text = "Hello"
                } else if ((selected == "arabic" || selected == "all") && text == "Hello") {
                    text = "زخرفة"
                }
            },
            onClear = { text = "" },
            onShuffle = {
                results.randomOrNull()?.let { copyToClipboard(context, it.text) }
            }
        )

        ResultsHeader(
            label = categories.firstOrNull { it.first == filter }?.second ?: "الكل",
            count = results.size
        )

        ResultsGrid(
            results = results,
            favorites = favorites,
            onFavToggle = { res -> scope.launch { FavoritesManager.toggleFavorite(context, res.text) } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorPanel(
    text: String,
    filter: String,
    categories: List<Pair<String, String>>,
    resultCount: Int,
    onTextChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onClear: () -> Unit,
    onShuffle: () -> Unit
) {
    Surface(
        color = AppSurface,
        border = BorderStroke(1.dp, AppBorder),
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("اكتب الاسم", color = AppText, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    Text("$resultCount نتيجة جاهزة", color = AppMuted, fontSize = 13.sp)
                }
                OutlinedButton(onClick = onShuffle, enabled = resultCount > 0, shape = RoundedCornerShape(8.dp)) {
                    Text("انسخ عشوائي", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("اكتب اسمك هنا", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppPrimary,
                    unfocusedBorderColor = AppBorder,
                    focusedContainerColor = Color(0xFFFAFBFC),
                    unfocusedContainerColor = Color(0xFFFAFBFC)
                ),
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.ExtraBold
                ),
                trailingIcon = {
                    if (text.isNotEmpty()) {
                        IconButton(onClick = onClear) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { (id, label) ->
                    FilterChip(label = label, selected = filter == id) { onCategorySelect(id) }
                }
            }
        }
    }
}

@Composable
fun ResultsHeader(label: String, count: Int) {
    Surface(color = AppBackground, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("نتائج $label", color = AppText, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Text("$count شكل متاح", color = AppMuted, fontSize = 12.sp)
            }
            Surface(
                color = AppPrimarySoft,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, Color(0xFFD0E9E4))
            ) {
                Text(
                    label,
                    color = AppPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = if (selected) AppPrimary else AppSurface,
        border = BorderStroke(1.dp, if (selected) AppPrimary else AppBorder)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            color = if (selected) Color.White else AppMuted,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp
        )
    }
}

@Composable
fun ResultsGrid(
    results: List<DecorationResult>,
    favorites: Set<String>,
    onFavToggle: (DecorationResult) -> Unit
) {
    val context = LocalContext.current
    if (results.isEmpty()) {
        EmptyState("اكتب شيئاً للبدء")
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(170.dp),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(12.dp)
    ) {
        items(results, key = { it.text + it.style }) { result ->
            ResultCard(
                result = result,
                isFavorite = favorites.contains(result.text),
                onCopy = { copyToClipboard(context, result.text) },
                onShare = { shareText(context, result.text) },
                onFav = { onFavToggle(result) }
            )
        }
    }
}

@Composable
fun ResultCard(
    result: DecorationResult,
    isFavorite: Boolean,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onFav: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = BorderStroke(1.dp, AppBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    result.style,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp,
                    color = AppPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "مفضلة",
                    tint = if (isFavorite) Color(0xFFDC2626) else AppMuted,
                    modifier = Modifier.size(22.dp).clickable { onFav() }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(result.text, color = AppText, fontSize = 17.sp, textAlign = TextAlign.Center)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onCopy,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text("نسخ", fontWeight = FontWeight.ExtraBold)
                }
                OutlinedButton(
                    onClick = onShare,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.width(48.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "مشاركة", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun SymbolsTab() {
    val context = LocalContext.current
    val categories = Symbols.categories.keys.toList()
    var selectedCat by remember { mutableStateOf(categories.firstOrNull().orEmpty()) }
    val symbols = Symbols.categories[selectedCat].orEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = AppSurface, border = BorderStroke(1.dp, AppBorder), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("رموز زخرفة", color = AppText, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Text("اضغط على أي رمز لنسخه مباشرة", color = AppMuted, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        FilterChip(label = cat, selected = selectedCat == cat) { selectedCat = cat }
                    }
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(64.dp),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(12.dp)
        ) {
            items(symbols) { symbol ->
                SymbolCell(symbol = symbol) { copyToClipboard(context, symbol) }
            }
        }
    }
}

@Composable
fun SymbolCell(symbol: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = AppSurface,
        border = BorderStroke(1.dp, AppBorder)
    ) {
        Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
            Text(symbol, fontSize = 23.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun FavoritesTab() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val favorites by FavoritesManager.getFavorites(context).collectAsState(initial = emptySet())

    if (favorites.isEmpty()) {
        EmptyState("لا يوجد مفضلات بعد")
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(170.dp),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(12.dp)
    ) {
        items(favorites.toList(), key = { it }) { fav ->
            ResultCard(
                result = DecorationResult(fav, "مفضلة", "fav"),
                isFavorite = true,
                onCopy = { copyToClipboard(context, fav) },
                onShare = { shareText(context, fav) },
                onFav = { scope.launch { FavoritesManager.toggleFavorite(context, fav) } }
            )
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(color = AppPrimarySoft, shape = RoundedCornerShape(18.dp)) {
                Text("✦", color = AppPrimary, fontSize = 34.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(message, color = AppMuted, fontWeight = FontWeight.Bold)
        }
    }
}

fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Zakhrafa", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "تم النسخ", Toast.LENGTH_SHORT).show()
}

fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "شارك عبر"))
}

fun shareApp(context: Context) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "زخرفة مزخرف")
        putExtra(
            Intent.EXTRA_TEXT,
            "جرّب تطبيق زخرفة مزخرف لزخرفة الأسماء والنصوص العربية والإنجليزية والرموز الجاهزة للنسخ:\n$DECORATOR_PLAY_URL"
        )
    }
    context.startActivity(Intent.createChooser(intent, "مشاركة التطبيق"))
}

fun rateApp(context: Context) {
    val openedMarket = runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DECORATOR_MARKET_URL)).apply {
            setPackage("com.android.vending")
        })
    }.isSuccess
    if (!openedMarket) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DECORATOR_PLAY_URL)))
    }
}
