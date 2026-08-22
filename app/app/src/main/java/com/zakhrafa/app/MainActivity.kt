package com.zakhrafa.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.zakhrafa.engine.ZakhrafaEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal const val DECORATOR_PACKAGE_ID = "com.zakhrafa.tech"
internal const val DECORATOR_PLAY_URL = "https://play.google.com/store/apps/details?id=$DECORATOR_PACKAGE_ID"
internal const val PRIVACY_URL = "https://zakhrafa.tech/privacy-policy.html"
internal const val SUPPORT_URL = "https://zakhrafa.tech/support.html"
internal const val TERMS_URL = "https://zakhrafa.tech/terms.html"
internal const val WEBSITE_URL = "https://zakhrafa.tech"
internal const val MAX_INPUT_LENGTH = 60
private val copyHistoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

private val LightColors = lightColorScheme(
    primary = Color(0xFF006B5B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9EF2DB),
    onPrimaryContainer = Color(0xFF00201A),
    secondary = Color(0xFF6750A4),
    secondaryContainer = Color(0xFFEADDFF),
    background = Color(0xFFF7F9FC),
    surface = Color.White,
    surfaceVariant = Color(0xFFE4E9E7),
    outline = Color(0xFF707976)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF82D5C0),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF005144),
    onPrimaryContainer = Color(0xFF9EF2DB),
    secondary = Color(0xFFCEBDFF),
    secondaryContainer = Color(0xFF4E378A),
    background = Color(0xFF101412),
    surface = Color(0xFF171C1A),
    surfaceVariant = Color(0xFF3F4946),
    outline = Color(0xFF89938F)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val darkPreference by AppearanceManager.getDarkMode(this)
                .collectAsState(initial = null)
            val systemDark = isSystemInDarkTheme()
            val darkTheme = darkPreference ?: systemDark
            val scope = rememberCoroutineScope()

            ZakhrafaTheme(darkTheme = darkTheme) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    MainScreen(
                        darkTheme = darkTheme,
                        onToggleTheme = {
                            scope.launch { AppearanceManager.setDarkMode(this@MainActivity, !darkTheme) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ZakhrafaTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}

private enum class AppTab(val label: String) {
    DECORATE("زخرفة"),
    SYMBOLS("رموز"),
    FAVORITES("المفضلة"),
    ABOUT("المزيد")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(darkTheme: Boolean, onToggleTheme: () -> Unit) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val context = LocalContext.current
    val tabs = AppTab.entries

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { BrandTitle() },
                navigationIcon = {
                    IconButton(onClick = { shareApp(context) }) {
                        Icon(Icons.Default.Share, contentDescription = "مشاركة التطبيق")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier.semantics {
                            contentDescription = if (darkTheme) "تفعيل الوضع النهاري" else "تفعيل الوضع الليلي"
                        }
                    ) {
                        Text(
                            text = if (darkTheme) "☀" else "☾",
                            fontSize = 23.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    val icon = when (tab) {
                        AppTab.DECORATE -> Icons.Default.Star
                        AppTab.SYMBOLS -> Icons.Default.Face
                        AppTab.FAVORITES -> Icons.Default.Favorite
                        AppTab.ABOUT -> Icons.Default.Info
                    }
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(icon, contentDescription = tab.label) },
                        label = { Text(tab.label, maxLines = 1) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (tabs[selectedTab]) {
                AppTab.DECORATE -> DecoratorScreen()
                AppTab.SYMBOLS -> SymbolsScreen()
                AppTab.FAVORITES -> FavoritesScreen()
                AppTab.ABOUT -> AboutScreen()
            }
        }
    }
}

@Composable
private fun BrandTitle() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.zakhrafa_brand),
            contentDescription = null,
            modifier = Modifier.size(36.dp)
        )
        Box(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = "زخرفة مزخرف",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
            Text(
                text = "${ZakhrafaEngine.countStyles()} شكلاً يعمل بدون إنترنت",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

internal fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Zakhrafa", text))
    copyHistoryScope.launch {
        CopyHistoryManager.recordCopy(context.applicationContext, text)
    }
    Toast.makeText(context, "تم النسخ ✓", Toast.LENGTH_SHORT).show()
}

internal fun readClipboard(context: Context): String? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    return clipboard.primaryClip
        ?.takeIf { it.itemCount > 0 }
        ?.getItemAt(0)
        ?.coerceToText(context)
        ?.toString()
        ?.takeIf { it.isNotBlank() }
}

internal fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "شارك عبر"))
}

internal fun shareApp(context: Context) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "زخرفة مزخرف")
        putExtra(
            Intent.EXTRA_TEXT,
            "جرّب تطبيق زخرفة مزخرف لزخرفة الأسماء والنصوص:\n$DECORATOR_PLAY_URL"
        )
    }
    context.startActivity(Intent.createChooser(intent, "مشاركة التطبيق"))
}

internal fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }.onFailure {
        Toast.makeText(context, "تعذر فتح الرابط", Toast.LENGTH_SHORT).show()
    }
}

internal fun rateApp(context: Context) {
    val marketUrl = "market://details?id=$DECORATOR_PACKAGE_ID"
    val openedMarket = runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, marketUrl.toUri()).apply {
            setPackage("com.android.vending")
        })
    }.isSuccess
    if (!openedMarket) openUrl(context, DECORATOR_PLAY_URL)
}
