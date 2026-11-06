package com.zakhrafa.app

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
internal fun AboutScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDataResetDialog by rememberSaveable { mutableStateOf(false) }

    if (showDataResetDialog) {
        AlertDialog(
            onDismissRequest = { showDataResetDialog = false },
            title = { Text("مسح بيانات التطبيق؟") },
            text = { Text("سيتم مسح المفضلة، سجل النسخ، وتفضيل المظهر من هذا الجهاز.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { LocalDataManager.clearAll(context) }
                    showDataResetDialog = false
                }) {
                    Text("مسح بياناتي", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDataResetDialog = false }) { Text("إلغاء") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            InfoCard(title = "قيّم التطبيق") {
                Text(
                    "شارك رأيك الصادق على Google Play وساعد الآخرين على التعرّف على التطبيق.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(onClick = { rateApp(context) }, modifier = Modifier.weight(1f)) {
                        Text("قيّمنا على Google Play")
                    }
                    OutlinedButton(onClick = { shareApp(context) }, modifier = Modifier.weight(1f)) {
                        Text("شاركه")
                    }
                }
            }
        }

        item {
            val adFree by AdManager.adFree.collectAsState()
            var rewardCancel by remember { mutableStateOf<(() -> Unit)?>(null) }
            DisposableEffect(Unit) { onDispose { rewardCancel?.invoke() } }
            InfoCard(title = "بدون إعلانات") {
                if (adFree) {
                    Text(
                        "الإعلانات معطلة على هذا الجهاز لمدة 24 ساعة.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "شاهد إعلاناً قصيراً لإيقاف جميع الإعلانات لمدة 24 ساعة.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = {
                        val activity = context as? Activity ?: return@Button
                        rewardCancel?.invoke()
                        rewardCancel = AdManager.requestRewarded(context) { available ->
                            rewardCancel = null
                            if (available) {
                                AdManager.showRewarded(
                                    activity,
                                    onReward = { AdManager.startAdFree(context) }
                                )
                            } else {
                                Toast.makeText(
                                    context,
                                    "تعذّر تحميل الإعلان، حاول لاحقاً",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }) {
                        Text("شاهد الإعلان وأزلها 24 ساعة")
                    }
                }
            }
        }

        item {
            InfoCard(title = "خصوصيتك") {
                Text(
                    "الزخرفة تتم بالكامل على جهازك ولا نجمع النصوص التي تكتبها. نستخدم بيانات استخدام مجهولة الهوية لتحسين التطبيق.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { openUrl(context, PRIVACY_URL) }) {
                    Text("سياسة الخصوصية")
                }
            }
        }

        item {
            InfoCard(title = "روابط مهمة") {
                LinkButton("الموقع الرسمي", WEBSITE_URL)
                LinkButton("الدعم والتواصل", SUPPORT_URL)
                LinkButton("شروط الاستخدام", TERMS_URL)
            }
        }

        item {
            InfoCard(title = "بياناتك على جهازك") {
                Text(
                    "امسح المفضلة وسجل النسخ وإعداد المظهر.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = { showDataResetDialog = true }) {
                    Text("مسح بياناتي", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        item {
            Text(
                "الإصدار ${BuildConfig.VERSION_NAME}  •  صُنع بعناية لمحبي الزخرفة",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun LinkButton(label: String, url: String) {
    val context = LocalContext.current
    Surface(
        onClick = { openUrl(context, url) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontWeight = FontWeight.Bold)
            Text("↗", color = MaterialTheme.colorScheme.primary)
        }
    }
}
