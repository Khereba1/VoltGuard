package com.khereba1.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.BatteryManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private val IbmPlexSans = FontFamily(
    Font(R.font.ibm_plex_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_bold, FontWeight.Bold)
)

private val IbmPlexMono = FontFamily(
    Font(R.font.ibm_plex_mono, FontWeight.Normal)
)

private val VoltTypography = Typography(
    headlineLarge = TextStyle(fontFamily = IbmPlexSans, fontWeight = FontWeight.Bold, fontSize = 36.sp),
    headlineSmall = TextStyle(fontFamily = IbmPlexSans, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = IbmPlexSans, fontWeight = FontWeight.Bold, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = IbmPlexSans, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    bodyMedium = TextStyle(fontFamily = IbmPlexSans, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = IbmPlexSans, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = IbmPlexSans, fontWeight = FontWeight.Medium, fontSize = 13.sp),
    labelMedium = TextStyle(fontFamily = IbmPlexSans, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = IbmPlexSans, fontWeight = FontWeight.Normal, fontSize = 11.sp)
)

private val DarkPalette = darkColorScheme(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF062825),
    background = Color(0xFF0F1419),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF161D24),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1C252E),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF25303B),
    error = Color(0xFFE06C75),
    onError = Color(0xFFFFFFFF)
)

private val LightPalette = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val dark = isSystemInDarkTheme()
            MaterialTheme(
                colorScheme = if (dark) DarkPalette else LightPalette,
                typography = VoltTypography
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VoltGuardApp(context = this)
                }
            }
        }
    }
}

private fun copyText(context: Context, label: String, text: String, toastMsg: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
}

@Composable
private fun VoltGuardApp(context: Context) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })

    var hasRoot by remember { mutableStateOf(false) }
    var chargingNode by remember { mutableStateOf<ChargingNode?>(null) }
    var battery by remember { mutableStateOf(BatterySnapshot()) }
    var system by remember { mutableStateOf(SystemSnapshot()) }
    var network by remember { mutableStateOf(NetworkSnapshot()) }
    var currentHistory by remember { mutableStateOf<List<Int>>(emptyList()) }
    var autoBoot by remember { mutableStateOf(Prefs.isAutoBoot(context)) }

    var diagExpanded by remember { mutableStateOf(false) }
    var diagLoading by remember { mutableStateOf(false) }
    var deepDiag by remember { mutableStateOf(DeepDiagnostics()) }

    var pendingPowerAction by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val (rootOk, node) = withContext(Dispatchers.IO) {
            val r = RootShell.checkRoot()
            val n = if (r) RootShell.detectChargingNode() else null
            r to n
        }
        hasRoot = rootOk
        chargingNode = node

        while (true) {
            val (b, s, n) = withContext(Dispatchers.IO) {
                Triple(
                    DeviceMonitor.readBattery(context, node, rootOk),
                    DeviceMonitor.readSystem(context, rootOk),
                    DeviceMonitor.readNetwork(context)
                )
            }
            battery = b
            system = s
            network = n
            currentHistory = (currentHistory + b.currentMa).takeLast(24)
            delay(2000)
        }
    }

    val statusText = when {
        battery.bypassActive -> stringResource(R.string.status_bypass)
        battery.statusCode == BatteryManager.BATTERY_STATUS_CHARGING -> stringResource(R.string.status_charging)
        battery.statusCode == BatteryManager.BATTERY_STATUS_FULL -> stringResource(R.string.status_full)
        battery.statusCode == BatteryManager.BATTERY_STATUS_NOT_CHARGING -> stringResource(R.string.status_not_charging)
        else -> stringResource(R.string.status_discharging)
    }

    val isChargingWarn = !battery.bypassActive && battery.statusCode == BatteryManager.BATTERY_STATUS_CHARGING
    val badgeColor = if (isChargingWarn) Color(0xFFF59E0B) else MaterialTheme.colorScheme.primary
    val badgeBg = badgeColor.copy(alpha = 0.14f)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf(
                        stringResource(R.string.tab_battery),
                        stringResource(R.string.tab_system),
                        stringResource(R.string.tab_network)
                    )
                    tabs.forEachIndexed { idx, title ->
                        val selected = pagerState.currentPage == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent)
                                .clickable { scope.launch { pagerState.animateScrollToPage(idx) } }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Fixed Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 580.dp)
                    .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = stringResource(R.string.subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(badgeBg)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(badgeColor)
                    )
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = badgeColor
                    )
                }
            }

            // Swipeable HorizontalPager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 6.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 580.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        when (page) {
                            0 -> BatteryTabContent(
                                context = context,
                                battery = battery,
                                currentHistory = currentHistory,
                                hasRoot = hasRoot,
                                chargingNode = chargingNode,
                                autoBoot = autoBoot,
                                onToggleBypass = { enabled ->
                                    Prefs.saveBypassEnabled(context, enabled)
                                    scope.launch(Dispatchers.IO) {
                                        RootShell.setBypass(enabled)
                                        val updated = DeviceMonitor.readBattery(context, chargingNode, hasRoot)
                                        withContext(Dispatchers.Main) { battery = updated }
                                    }
                                },
                                onToggleAutoBoot = { enabled ->
                                    autoBoot = enabled
                                    Prefs.saveAutoBoot(context, enabled)
                                },
                                onRequestReboot = { pendingPowerAction = "reboot" },
                                onRequestRecovery = { pendingPowerAction = "recovery" },
                                diagExpanded = diagExpanded,
                                diagLoading = diagLoading,
                                deepDiag = deepDiag,
                                onToggleDiag = {
                                    diagExpanded = !diagExpanded
                                    if (diagExpanded && deepDiag.processes.isEmpty()) {
                                        diagLoading = true
                                        scope.launch(Dispatchers.IO) {
                                            val d = RootShell.readDeepDiagnostics()
                                            withContext(Dispatchers.Main) {
                                                deepDiag = d
                                                diagLoading = false
                                            }
                                        }
                                    }
                                },
                                onRefreshDiag = {
                                    diagLoading = true
                                    scope.launch(Dispatchers.IO) {
                                        val d = RootShell.readDeepDiagnostics()
                                        withContext(Dispatchers.Main) {
                                            deepDiag = d
                                            diagLoading = false
                                        }
                                    }
                                }
                            )
                            1 -> SystemTabContent(system = system)
                            else -> NetworkTabContent(context = context, network = network)
                        }

                        Text(
                            text = stringResource(R.string.footer_version),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 14.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (pendingPowerAction != null) {
        val isRec = pendingPowerAction == "recovery"
        AlertDialog(
            onDismissRequest = { pendingPowerAction = null },
            title = { Text(stringResource(if (isRec) R.string.dlg_rec_title else R.string.dlg_reboot_title)) },
            text = { Text(stringResource(if (isRec) R.string.dlg_rec_desc else R.string.dlg_reboot_desc)) },
            confirmButton = {
                TextButton(onClick = {
                    val rec = isRec
                    pendingPowerAction = null
                    scope.launch(Dispatchers.IO) { RootShell.reboot(rec) }
                }) {
                    Text(stringResource(R.string.btn_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPowerAction = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
private fun BatteryTabContent(
    context: Context,
    battery: BatterySnapshot,
    currentHistory: List<Int>,
    hasRoot: Boolean,
    chargingNode: ChargingNode?,
    autoBoot: Boolean,
    onToggleBypass: (Boolean) -> Unit,
    onToggleAutoBoot: (Boolean) -> Unit,
    onRequestReboot: () -> Unit,
    onRequestRecovery: () -> Unit,
    diagExpanded: Boolean,
    diagLoading: Boolean,
    deepDiag: DeepDiagnostics,
    onToggleDiag: () -> Unit,
    onRefreshDiag: () -> Unit
) {
    val plugLabel = when (battery.pluggedCode) {
        BatteryManager.BATTERY_PLUGGED_AC -> stringResource(R.string.plug_ac)
        BatteryManager.BATTERY_PLUGGED_USB -> stringResource(R.string.plug_usb)
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> stringResource(R.string.plug_wireless)
        else -> stringResource(R.string.plug_battery)
    }
    val healthLabel = when (battery.healthCode) {
        BatteryManager.BATTERY_HEALTH_GOOD -> stringResource(R.string.health_good)
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> stringResource(R.string.health_overheat)
        BatteryManager.BATTERY_HEALTH_DEAD -> stringResource(R.string.health_dead)
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> stringResource(R.string.health_overvoltage)
        BatteryManager.BATTERY_HEALTH_COLD -> stringResource(R.string.health_cold)
        else -> stringResource(R.string.health_unknown)
    }

    // Hero Battery Card
    FlatCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${battery.level}%",
                fontFamily = IbmPlexMono,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = plugLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LinearProgressIndicator(
            progress = { (battery.level / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val sign = if (battery.currentMa > 0) "+" else ""
            MetricTile(
                label = stringResource(R.string.lbl_current),
                value = "$sign${battery.currentMa} mA",
                highlight = battery.bypassActive,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = stringResource(R.string.lbl_power),
                value = String.format(Locale.US, "%.2f W", battery.powerW),
                highlight = false,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile(
                label = stringResource(R.string.lbl_voltage),
                value = String.format(Locale.US, "%.2f V", battery.voltageV),
                highlight = false,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = stringResource(R.string.lbl_temp),
                value = String.format(Locale.US, "%.1f °C", battery.tempC),
                highlight = false,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Live Current Sparkline
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.lbl_live_trend),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${battery.currentMa} mA",
                        fontFamily = IbmPlexMono,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                val lineColor = MaterialTheme.colorScheme.primary
                val baseColor = MaterialTheme.colorScheme.outline
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                ) {
                    drawLine(
                        color = baseColor,
                        start = Offset(0f, size.height * 0.5f),
                        end = Offset(size.width, size.height * 0.5f),
                        strokeWidth = 2f
                    )
                    if (currentHistory.size >= 2) {
                        val minV = (currentHistory.minOrNull() ?: -100).coerceAtMost(-50)
                        val maxV = (currentHistory.maxOrNull() ?: 100).coerceAtLeast(50)
                        val span = (maxV - minV).coerceAtLeast(1).toFloat()
                        val stepX = size.width / (currentHistory.size - 1).toFloat()
                        val path = Path()
                        currentHistory.forEachIndexed { idx, v ->
                            val x = idx * stepX
                            val norm = (v - minV) / span
                            val y = size.height - (norm * (size.height - 6f) + 3f)
                            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(
                            path = path,
                            color = lineColor,
                            style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }
            }
        }
    }

    // Root Charging Control Card
    if (hasRoot) {
        FlatCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.section_root_ctrl),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (chargingNode != null) {
                    Text(
                        text = chargingNode.label,
                        fontFamily = IbmPlexMono,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (chargingNode != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                        Text(stringResource(R.string.lbl_bypass), style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(R.string.desc_bypass),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = battery.bypassActive, onCheckedChange = onToggleBypass)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                        Text(stringResource(R.string.lbl_auto_boot), style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(R.string.desc_auto_boot),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = autoBoot, onCheckedChange = onToggleAutoBoot)
                }
            } else {
                Text(
                    text = stringResource(R.string.msg_no_node),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onRequestReboot,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(11.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(stringResource(R.string.btn_reboot), fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onRequestRecovery,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(11.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        stringResource(R.string.btn_recovery),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Collapsible Top Processes & WakeLocks (Root-only deep monitor)
        FlatCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleDiag() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.section_deep_diag),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (diagExpanded) {
                        TextButton(
                            onClick = onRefreshDiag,
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(stringResource(R.string.btn_refresh), fontSize = 12.sp)
                        }
                    }
                    Text(
                        text = if (diagExpanded) "▾" else "▸",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp
                    )
                }
            }

            if (diagExpanded) {
                Spacer(modifier = Modifier.height(10.dp))
                if (diagLoading) {
                    Text(
                        text = stringResource(R.string.msg_loading),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = stringResource(R.string.lbl_top_procs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    deepDiag.processes.forEach { proc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = proc.name,
                                fontFamily = IbmPlexMono,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            Text(
                                text = "CPU ${proc.cpu}  RAM ${proc.mem}",
                                fontFamily = IbmPlexMono,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = stringResource(R.string.lbl_wakelocks),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (deepDiag.wakeLocks.isEmpty()) {
                        Text(
                            text = stringResource(R.string.msg_no_wakelocks),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        deepDiag.wakeLocks.forEach { wl ->
                            Text(
                                text = wl,
                                fontFamily = IbmPlexMono,
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    } else {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Text(
                text = stringResource(R.string.msg_no_root),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(14.dp)
            )
        }
    }

    // Battery Details Card
    FlatCard {
        Text(
            text = stringResource(R.string.section_batt_details),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        SpecRow(stringResource(R.string.k_health), healthLabel, mono = false)
        SpecRow(stringResource(R.string.k_tech), battery.tech, mono = true)
        if (battery.designCapacityMah > 0) {
            SpecRow(stringResource(R.string.k_capacity), "${battery.designCapacityMah} mAh", mono = true)
        }
        SpecRow(stringResource(R.string.k_plugged), plugLabel, mono = false, last = true)
    }
}

@Composable
private fun SystemTabContent(system: SystemSnapshot) {
    // Memory & Storage
    FlatCard {
        Text(
            text = stringResource(R.string.section_mem_storage),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.k_ram), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                String.format(Locale.US, "%.2f / %.2f GB", system.ramUsedGb, system.ramTotalGb),
                fontFamily = IbmPlexMono,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { system.ramPct },
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(999.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.k_storage), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                String.format(Locale.US, "%.1f / %.1f GB", system.storageUsedGb, system.storageTotalGb),
                fontFamily = IbmPlexMono,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { system.storagePct },
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(999.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    // Per-Core CPU Monitor (Clean 2-column grid)
    FlatCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.section_cpu_cores),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${stringResource(R.string.k_governor)}: ${system.governor}",
                fontFamily = IbmPlexMono,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        val rows = system.cores.chunked(2)
        rows.forEachIndexed { rIdx, pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { core ->
                    val ratio = if (core.maxMhz > 0) (core.curMhz.toFloat() / core.maxMhz.toFloat()).coerceIn(0f, 1f) else 0f
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Core ${core.index}",
                                    fontFamily = IbmPlexMono,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (core.curMhz > 0) "${core.curMhz} MHz" else "Idle",
                                    fontFamily = IbmPlexMono,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(999.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
            if (rIdx < rows.lastIndex) Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Hardware & OS Info
    FlatCard {
        Text(
            text = stringResource(R.string.section_hw_os),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        SpecRow(stringResource(R.string.k_device), system.deviceName, mono = true)
        SpecRow(stringResource(R.string.k_arch), system.abi, mono = true)
        SpecRow(stringResource(R.string.k_display), system.displayInfo, mono = true)
        SpecRow(stringResource(R.string.k_os), system.androidVer, mono = true)
        SpecRow(stringResource(R.string.k_kernel), system.kernelVer, mono = true)
        SpecRow(
            stringResource(R.string.k_root),
            stringResource(if (system.hasRoot) R.string.val_root_yes else R.string.val_root_no),
            mono = false
        )
        SpecRow(stringResource(R.string.k_uptime), system.uptimeText, mono = true, last = true)
    }
}

@Composable
private fun NetworkTabContent(context: Context, network: NetworkSnapshot) {
    val copiedMsg = stringResource(R.string.msg_copied)

    FlatCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.section_wifi),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (network.localIp != "—") {
                TextButton(
                    onClick = { copyText(context, "IP", network.localIp, copiedMsg) },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(stringResource(R.string.btn_copy_ip), fontSize = 12.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        SpecRow(stringResource(R.string.k_ip), network.localIp, mono = true)
        SpecRow(
            stringResource(R.string.k_freq),
            if (network.freqMhz > 0) "${network.freqMhz} MHz" else "—",
            mono = true
        )
        SpecRow(
            stringResource(R.string.k_speed),
            if (network.linkSpeedMbps > 0) "${network.linkSpeedMbps} Mbps" else "—",
            mono = true
        )
        SpecRow(
            stringResource(R.string.k_signal),
            if (network.rssiDbm != 0) "${network.rssiDbm} dBm" else "—",
            mono = true,
            last = true
        )
    }

    FlatCard {
        Text(
            text = stringResource(R.string.section_ifaces),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        network.interfaces.forEach { (name, ip) ->
            SpecRow(name, ip, mono = true)
        }
        SpecRow(
            stringResource(R.string.k_airplane),
            stringResource(if (network.airplaneOn) R.string.val_on else R.string.val_off),
            mono = false,
            last = true
        )
    }
}

@Composable
private fun FlatCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, highlight: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontFamily = IbmPlexMono,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String, mono: Boolean, last: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontFamily = if (mono) IbmPlexMono else IbmPlexSans,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
    if (!last) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}
