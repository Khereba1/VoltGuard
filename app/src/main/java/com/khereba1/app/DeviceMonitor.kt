package com.khereba1.app

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.provider.Settings
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.NetworkInterface
import kotlin.math.abs

data class ChargingNode(
    val label: String,
    val path: String,
    val enableVal: String,
    val disableVal: String
)

data class ProcEntry(
    val name: String,
    val cpu: String,
    val mem: String
)

data class DeepDiagnostics(
    val processes: List<ProcEntry> = emptyList(),
    val wakeLocks: List<String> = emptyList()
)

data class BatterySnapshot(
    val level: Int = 0,
    val statusCode: Int = BatteryManager.BATTERY_STATUS_UNKNOWN,
    val currentMa: Int = 0,
    val powerW: Double = 0.0,
    val voltageV: Double = 0.0,
    val tempC: Double = 0.0,
    val healthCode: Int = BatteryManager.BATTERY_HEALTH_UNKNOWN,
    val tech: String = "Li-ion",
    val pluggedCode: Int = 0,
    val designCapacityMah: Int = 0,
    val bypassActive: Boolean = false
)

data class CoreInfo(
    val index: Int,
    val curMhz: Int,
    val maxMhz: Int
)

data class SystemSnapshot(
    val ramUsedGb: Double = 0.0,
    val ramTotalGb: Double = 0.0,
    val ramPct: Float = 0f,
    val storageUsedGb: Double = 0.0,
    val storageTotalGb: Double = 0.0,
    val storagePct: Float = 0f,
    val deviceName: String = "",
    val abi: String = "",
    val cores: List<CoreInfo> = emptyList(),
    val governor: String = "—",
    val displayInfo: String = "—",
    val androidVer: String = "",
    val kernelVer: String = "",
    val uptimeText: String = "—",
    val hasRoot: Boolean = false
)

data class NetworkSnapshot(
    val localIp: String = "—",
    val freqMhz: Int = 0,
    val linkSpeedMbps: Int = 0,
    val rssiDbm: Int = 0,
    val interfaces: List<Pair<String, String>> = emptyList(),
    val airplaneOn: Boolean = false
)

object RootShell {
    private val KNOWN_NODES = listOf(
        ChargingNode("Samsung • store_mode", "/sys/class/power_supply/battery/store_mode", "1", "0"),
        ChargingNode("Samsung • batt_slate_mode", "/sys/class/power_supply/battery/batt_slate_mode", "1", "0"),
        ChargingNode("Kernel • input_suspend", "/sys/class/power_supply/battery/input_suspend", "1", "0"),
        ChargingNode("OPPO • charger_enable", "/sys/class/power_supply/battery/charger_enable", "0", "1"),
        ChargingNode("Kernel • charging_enabled", "/sys/class/power_supply/battery/charging_enabled", "0", "1"),
        ChargingNode("Kernel • battery_charging_enabled", "/sys/class/power_supply/battery/battery_charging_enabled", "0", "1")
    )

    fun runRoot(cmd: String): Pair<Int, String> {
        return try {
            val proc = ProcessBuilder("su", "-c", cmd)
                .redirectErrorStream(true)
                .start()
            val out = BufferedReader(InputStreamReader(proc.inputStream)).use { it.readText() }
            val code = proc.waitFor()
            code to out.trim()
        } catch (e: Exception) {
            -1 to (e.message ?: "")
        }
    }

    fun checkRoot(): Boolean {
        val (code, out) = runRoot("id -u")
        return code == 0 && out.trim() == "0"
    }

    fun detectChargingNode(): ChargingNode? {
        for (node in KNOWN_NODES) {
            val (code, out) = runRoot("[ -f \"${node.path}\" ] && echo YES")
            if (code == 0 && out.contains("YES")) return node
        }
        return null
    }

    fun isBypassActive(node: ChargingNode?): Boolean {
        if (node == null) return false
        val (code, out) = runRoot("cat \"${node.path}\" 2>/dev/null")
        return code == 0 && out.trim() == node.enableVal
    }

    fun setBypass(enabled: Boolean): Boolean {
        val node = detectChargingNode() ?: return false
        val value = if (enabled) node.enableVal else node.disableVal
        val (code, _) = runRoot("echo $value > \"${node.path}\"")
        return code == 0
    }

    fun readDeepDiagnostics(): DeepDiagnostics {
        val procs = mutableListOf<ProcEntry>()
        val (topCode, topOut) = runRoot("top -n 1 -b -m 8 2>/dev/null")
        if (topCode == 0 && topOut.isNotBlank()) {
            val lines = topOut.lines()
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue
                val first = trimmed.firstOrNull() ?: continue
                if (!first.isDigit()) continue
                val parts = trimmed.split(Regex("\\s+"))
                if (parts.size >= 11) {
                    val cpu = parts[8]
                    val mem = parts[9]
                    val name = parts.drop(11).joinToString(" ").ifBlank { parts.last() }
                    if (name.contains("top -n") || name.contains("com.topjohnwu.magisk.provider")) continue
                    val shortName = name.substringBefore(" ").substringAfterLast("/")
                    procs.add(ProcEntry(shortName, "${cpu}%", "${mem}%"))
                }
            }
        }

        val wakeLocks = mutableListOf<String>()
        val (wlCode, wlOut) = runRoot("dumpsys power 2>/dev/null | grep -A 12 'Wake Locks:'")
        if (wlCode == 0 && wlOut.isNotBlank()) {
            for (line in wlOut.lines().drop(1)) {
                val t = line.trim()
                if (t.isEmpty() || t.startsWith("Suspend") || t.startsWith("mWake")) break
                if (t.contains("WAKE_LOCK") || t.contains("uid=")) {
                    wakeLocks.add(t.take(90))
                }
            }
        }
        return DeepDiagnostics(procs.take(7), wakeLocks.take(6))
    }

    fun reboot(recovery: Boolean) {
        if (recovery) runRoot("reboot recovery") else runRoot("reboot")
    }
}

object DeviceMonitor {

    fun readBattery(context: Context, node: ChargingNode?, hasRoot: Boolean): BatterySnapshot {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val pct = if (scale > 0) (level * 100) / scale else 0
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            ?: BatteryManager.BATTERY_STATUS_UNKNOWN
        val voltageMv = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val tempTenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val health = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
            ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
        val tech = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0

        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        var rawCurr = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0
        if (hasRoot && (rawCurr == 0 || rawCurr == Int.MIN_VALUE)) {
            val (c, o) = RootShell.runRoot("cat /sys/class/power_supply/battery/current_now 2>/dev/null")
            if (c == 0) rawCurr = o.trim().toIntOrNull() ?: 0
        }
        val currentMa = if (abs(rawCurr) > 20000) rawCurr / 1000 else rawCurr
        val voltageV = voltageMv / 1000.0
        val powerW = abs(currentMa * voltageV / 1000.0)
        val tempC = tempTenths / 10.0

        val bypassActive = if (hasRoot && node != null) RootShell.isBypassActive(node) else false
        val designCap = readDesignCapacity(context, hasRoot)

        return BatterySnapshot(
            level = pct,
            statusCode = status,
            currentMa = currentMa,
            powerW = powerW,
            voltageV = voltageV,
            tempC = tempC,
            healthCode = health,
            tech = tech,
            pluggedCode = plugged,
            designCapacityMah = designCap,
            bypassActive = bypassActive
        )
    }

    private fun readDesignCapacity(context: Context, hasRoot: Boolean): Int {
        try {
            val cls = Class.forName("com.android.internal.os.PowerProfile")
            val ctor = cls.getConstructor(Context::class.java)
            val profile = ctor.newInstance(context)
            val method = cls.getMethod("getBatteryCapacity")
            val cap = (method.invoke(profile) as? Double)?.toInt() ?: 0
            if (cap > 500) return cap
        } catch (_: Exception) {
            // fallback below
        }
        if (hasRoot) {
            val (c, o) = RootShell.runRoot("cat /sys/class/power_supply/battery/charge_full_design 2>/dev/null")
            val raw = if (c == 0) o.trim().toIntOrNull() ?: 0 else 0
            if (raw > 100000) return raw / 1000
            if (raw > 500) return raw
        }
        return 0
    }

    fun readSystem(context: Context, hasRoot: Boolean): SystemSnapshot {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val totalRam = mi.totalMem / 1_073_741_824.0
        val usedRam = (mi.totalMem - mi.availMem) / 1_073_741_824.0
        val ramPct = if (mi.totalMem > 0) ((mi.totalMem - mi.availMem).toFloat() / mi.totalMem.toFloat()) else 0f

        val stat = StatFs(Environment.getDataDirectory().path)
        val totalBytes = stat.totalBytes
        val availBytes = stat.availableBytes
        val totalStorage = totalBytes / 1_073_741_824.0
        val usedStorage = (totalBytes - availBytes) / 1_073_741_824.0
        val storagePct = if (totalBytes > 0) ((totalBytes - availBytes).toFloat() / totalBytes.toFloat()) else 0f

        val coreCount = Runtime.getRuntime().availableProcessors().coerceIn(1, 16)
        val cores = readCpuCores(coreCount, hasRoot)
        val governor = readFirstLine("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor", hasRoot) ?: "—"

        val dm = context.resources.displayMetrics
        val displayInfo = "${dm.widthPixels} × ${dm.heightPixels} • ${dm.densityDpi} dpi"

        val upSec = SystemClock.elapsedRealtime() / 1000
        val hrs = upSec / 3600
        val mins = (upSec % 3600) / 60
        val uptimeText = "${hrs}h ${mins}m"

        return SystemSnapshot(
            ramUsedGb = usedRam,
            ramTotalGb = totalRam,
            ramPct = ramPct.coerceIn(0f, 1f),
            storageUsedGb = usedStorage,
            storageTotalGb = totalStorage,
            storagePct = storagePct.coerceIn(0f, 1f),
            deviceName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
            abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "—",
            cores = cores,
            governor = governor,
            displayInfo = displayInfo,
            androidVer = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            kernelVer = (System.getProperty("os.version") ?: "—").substringBefore("-"),
            uptimeText = uptimeText,
            hasRoot = hasRoot
        )
    }

    private fun readCpuCores(count: Int, hasRoot: Boolean): List<CoreInfo> {
        val list = mutableListOf<CoreInfo>()
        var anyRead = false
        for (i in 0 until count) {
            val cur = readIntFile("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq")
            val max = readIntFile("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
            if (cur > 0 || max > 0) anyRead = true
            list.add(CoreInfo(i, cur / 1000, (if (max > 0) max else cur) / 1000))
        }
        if (!anyRead && hasRoot) {
            val d = '$'
            val cmd = """for i in $(seq 0 ${count - 1}); do c=$(cat /sys/devices/system/cpu/cpu${d}i/cpufreq/scaling_cur_freq 2>/dev/null || echo 0); m=$(cat /sys/devices/system/cpu/cpu${d}i/cpufreq/cpuinfo_max_freq 2>/dev/null || echo 0); echo "${d}i:${d}c:${d}m"; done"""
            val (code, out) = RootShell.runRoot(cmd)
            if (code == 0 && out.isNotBlank()) {
                val rootList = mutableListOf<CoreInfo>()
                for (line in out.lines()) {
                    val p = line.trim().split(":")
                    if (p.size == 3) {
                        val idx = p[0].toIntOrNull() ?: continue
                        val c = (p[1].toIntOrNull() ?: 0) / 1000
                        val m = (p[2].toIntOrNull() ?: c * 1000) / 1000
                        rootList.add(CoreInfo(idx, c, m))
                    }
                }
                if (rootList.isNotEmpty()) return rootList
            }
        }
        return list
    }

    private fun readIntFile(path: String): Int {
        return try {
            File(path).readText().trim().toIntOrNull() ?: 0
        } catch (_: Exception) {
            0
        }
    }

    private fun readFirstLine(path: String, hasRoot: Boolean): String? {
        try {
            val t = File(path).readText().trim()
            if (t.isNotBlank()) return t
        } catch (_: Exception) {}
        if (hasRoot) {
            val (c, o) = RootShell.runRoot("cat \"$path\" 2>/dev/null")
            if (c == 0 && o.isNotBlank()) return o.trim()
        }
        return null
    }

    fun readNetwork(context: Context): NetworkSnapshot {
        val ifaces = mutableListOf<Pair<String, String>>()
        var primaryIp = "—"
        try {
            val nis = NetworkInterface.getNetworkInterfaces()
            while (nis != null && nis.hasMoreElements()) {
                val ni = nis.nextElement()
                if (!ni.isUp || ni.isLoopback) continue
                val addrs = ni.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val ip = addr.hostAddress ?: continue
                        ifaces.add(ni.name to ip)
                        if (ni.name == "wlan0" || primaryIp == "—") primaryIp = ip
                    }
                }
            }
        } catch (_: Exception) {}

        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        @Suppress("DEPRECATION")
        val info = wm?.connectionInfo
        val freq = info?.frequency ?: 0
        val speed = info?.linkSpeed ?: 0
        val rssi = info?.rssi ?: 0

        val airplane = Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON,
            0
        ) != 0

        return NetworkSnapshot(
            localIp = primaryIp,
            freqMhz = freq,
            linkSpeedMbps = speed,
            rssiDbm = rssi,
            interfaces = ifaces,
            airplaneOn = airplane
        )
    }
}
