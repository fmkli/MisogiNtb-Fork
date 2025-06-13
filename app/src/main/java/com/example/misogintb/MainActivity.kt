package com.example.misogintb // 替换为你的包名

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.ToneGenerator
import android.nfc.NfcAdapter
import android.nfc.Tag
// 保持你原有的NfcA，或者按需添加其他，但对于只读ID，NfcA通常足够
import android.nfc.tech.NfcA
import android.os.*
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
// ... (其他 imports 保持不变) ...
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.room.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.text.filter
import kotlin.text.isDigit
import kotlin.text.take

// ... (你的 Room 实体和 DAO 保持不变)

// --- Room Database Entities ---
@androidx.room.Entity(tableName = "card_info")
data class CardInfo(
    @androidx.room.PrimaryKey val cardId: String,
    @androidx.room.ColumnInfo(name = "balance_in_fen") val balanceInFen: Int,
    @androidx.room.ColumnInfo(name = "status") val status: String = "active"
)

@androidx.room.Entity(
    tableName = "access_log",
    foreignKeys = [androidx.room.ForeignKey(
        entity = CardInfo::class,
        parentColumns = ["cardId"],
        childColumns = ["card_id_fk"],
        onDelete = androidx.room.ForeignKey.CASCADE
    )],
    indices = [androidx.room.Index(value = ["card_id_fk"])]
)
data class AccessLog(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    @androidx.room.ColumnInfo(name = "card_id_fk") val cardIdFk: String,
    @androidx.room.ColumnInfo(name = "entry_time_millis") val entryTimeMillis: Long,
    @androidx.room.ColumnInfo(name = "exit_time_millis") val exitTimeMillis: Long? = null,
    @androidx.room.ColumnInfo(name = "amount_deducted_in_fen") val amountDeductedInFen: Int? = null,
    @androidx.room.ColumnInfo(name = "is_inside") var isInside: Boolean
)

// --- Room DAOs ---
@androidx.room.Dao
interface CardInfoDao {
    @androidx.room.Query("SELECT * FROM card_info WHERE cardId = :cardId")
    suspend fun getCard(cardId: String): CardInfo?

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertCard(cardInfo: CardInfo)

    @androidx.room.Update
    suspend fun updateCard(cardInfo: CardInfo)

    @androidx.room.Query("SELECT * FROM card_info")
    suspend fun getAllCards(): List<CardInfo>
}

@androidx.room.Dao
interface AccessLogDao {
    @androidx.room.Query("SELECT * FROM access_log WHERE card_id_fk = :cardId AND is_inside = 1 ORDER BY entry_time_millis DESC LIMIT 1")
    suspend fun getLatestUnexitedLog(cardId: String): AccessLog?

    @androidx.room.Insert
    suspend fun insertLog(accessLog: AccessLog): Long

    @androidx.room.Update
    suspend fun updateLog(accessLog: AccessLog)

    @androidx.room.Query("SELECT * FROM access_log WHERE card_id_fk = :cardId ORDER BY entry_time_millis DESC")
    suspend fun getAllLogsForCard(cardId: String): List<AccessLog>
}

// --- Room Database ---
@androidx.room.Database(entities = [CardInfo::class, AccessLog::class], version = 1, exportSchema = false)
abstract class AppDatabase : androidx.room.RoomDatabase() {
    abstract fun cardInfoDao(): CardInfoDao
    abstract fun accessLogDao(): AccessLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nfc_local_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}


class MainActivity : ComponentActivity() {

    private lateinit var vibrator: Vibrator
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var pendingIntent: PendingIntent
    private lateinit var intentFiltersArray: Array<IntentFilter>
    private lateinit var techListsArray: Array<Array<String>> // 修改点
    private var toneGenerator: ToneGenerator? = null

    // --- Database Instances ---
    private lateinit var db: AppDatabase
    private lateinit var cardInfoDao: CardInfoDao
    private lateinit var accessLogDao: AccessLogDao

    // --- UI State (保持你原有的状态管理) ---
    private val _currentCardId = mutableStateOf<String?>(null)
    private val _currentBalance = mutableStateOf<Int?>(null)
    private val DEFAULT_MESSAGE_NORMAL = "请刷卡进入或离开"
    private val DEFAULT_MESSAGE_ADMIN = "管理员模式: 请扫卡操作"
    private val _operationStatusMessage = mutableStateOf(DEFAULT_MESSAGE_NORMAL)
    private var _isNfcReady = mutableStateOf(false)
    private var _isInsideArea = mutableStateOf(false)
    private var _entryCardId = mutableStateOf<String?>(null)

    // --- 修改点：将固定扣款金额变为可配置的状态 ---
    private val INITIAL_DEDUCT_AMOUNT_ON_EXIT_IN_FEN = 300 // 初始值
    private val _autoDeductAmountInFen = mutableStateOf(INITIAL_DEDUCT_AMOUNT_ON_EXIT_IN_FEN)
    val autoDeductAmountInFen: State<Int> = _autoDeductAmountInFen // 公开为 State 以便 Composable 观察

    private var _showAdminPasswordDialog = mutableStateOf(false)
    private var _isAdminModeActive = mutableStateOf(false)
    private var _adminPasswordInput = mutableStateOf("")
    private var _adminStatusMessage = mutableStateOf<String?>(null)
    private val ADMIN_PASSWORD = "114514"
    private var clearMessageJob: Job? = null
    private val MESSAGE_DISPLAY_DURATION = 3000L

    // ... (你已有的其他成员变量如 vibrator, nfcAdapter, db, DAOs, UI States etc.) ...

    private var lastProcessedCardId: String? = null
    private var lastProcessedTimeMillis: Long = 0
    private val NFC_PROCESSING_DEBOUNCE_INTERVAL = 2000L // 2秒的去抖动间隔，可以调整

    private fun bytesToHexString(bytes: ByteArray?): String? {
        if (bytes == null) return null
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = "0123456789ABCDEF"[v ushr 4] // ushr is unsigned shift right
            hexChars[j * 2 + 1] = "0123456789ABCDEF"[v and 0x0F]
        }
        return String(hexChars)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vibrator = getSystemService(Vibrator::class.java)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        db = AppDatabase.getDatabase(applicationContext)
        cardInfoDao = db.cardInfoDao()
        accessLogDao = db.accessLogDao()

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter == null) {
            Log.e("NFC_SETUP", "此设备不支持NFC")
            updateOperationStatus("错误: 此设备不支持NFC", permanent = true)
            _isNfcReady.value = false
        } else if (!nfcAdapter!!.isEnabled) {
            Log.e("NFC_SETUP", "NFC被禁用")
            updateOperationStatus("错误: NFC被禁用，请在系统设置中开启", permanent = true)
            _isNfcReady.value = false
        } else {
            _isNfcReady.value = true
            Log.d("NFC_SETUP", "NFC is available and enabled.")

            // --- 修改点：PendingIntent Flags ---
            val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // For Android 12 (S) and above, FLAG_IMMUTABLE or FLAG_MUTABLE is required.
                // FLAG_UPDATE_CURRENT will update the intent's extras.
                // FLAG_IMMUTABLE is generally safer unless you need to modify the PendingIntent later.
                // Given the second example used FLAG_MUTABLE, we'll align with that,
                // but consider if FLAG_IMMUTABLE is more appropriate for your exact needs.
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            pendingIntent = PendingIntent.getActivity(this, 0, intent, pendingIntentFlags)
            Log.d("NFC_SETUP", "PendingIntent created with flags: $pendingIntentFlags")


            // IntentFilter - 保持你原来的 ACTION_TECH_DISCOVERED
            val techDiscovered = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
            try {
                techDiscovered.addDataType("*/*")
                Log.d("NFC_SETUP", "IntentFilter for TECH_DISCOVERED created with data type */*")
            } catch (e: IntentFilter.MalformedMimeTypeException) {
                Log.e("NFC_SETUP", "MalformedMimeTypeException for IntentFilter", e)
                throw RuntimeException("Check your MIME type setup for IntentFilter.")
            }
            intentFiltersArray = arrayOf(techDiscovered)


            // --- 修改点：techListsArray (更广泛的兼容性，借鉴于第二段代码) ---
            // 虽然只读ID主要依赖NfcA，但包含更多技术可以增加捕获不同类型标签的机会
            techListsArray = arrayOf(
                arrayOf(android.nfc.tech.NfcA::class.java.name),
                arrayOf(android.nfc.tech.NfcB::class.java.name),
                arrayOf(android.nfc.tech.NfcF::class.java.name),
                arrayOf(android.nfc.tech.NfcV::class.java.name),
                arrayOf(android.nfc.tech.IsoDep::class.java.name)
                // MifareClassic/Ultralight/Ndef 也可以添加，但如果只是读ID，前面的可能已足够
                // arrayOf(android.nfc.tech.MifareClassic::class.java.name),
                // arrayOf(android.nfc.tech.MifareUltralight::class.java.name),
                // arrayOf(android.nfc.tech.Ndef::class.java.name)
            )
            Log.d("NFC_SETUP", "Tech lists created: ${techListsArray.joinToString { it.contentToString() }}")
        }

        toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)

        setContent {
            // ... (你的UI setContent 保持不变)
            val myCustomColorScheme = darkColorScheme(
                primary = Color.Cyan,
                onSurface = Color.White,
                outline = Color.Gray,
                surface = Color.DarkGray,
                background = Color.Black,
                onBackground = Color.White,
                error = Color(0xFFCF6679)
            )

            MaterialTheme(
                colorScheme = myCustomColorScheme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isNfcActuallyReady = _isNfcReady.value
                    val currentStatusMessageVal by _operationStatusMessage

                    if (!isNfcActuallyReady) {
                        ErrorScreen(message = currentStatusMessageVal)
                    } else {
                        MainNFCScreen()
                        if (_showAdminPasswordDialog.value) {
                            AdminPasswordDialog()
                        }
                    }
                }
            }
        }
    }

    // updateOperationStatus, vibrate, playBeepSound 保持不变

    override fun onResume() {
        super.onResume()
        Log.d("NFC_LIFECYCLE", "onResume called.")
        if (_isNfcReady.value) {
            try {
                nfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray)
                Log.i("NFC_LIFECYCLE", "Foreground dispatch enabled successfully.")
            } catch (ex: IllegalStateException) {
                Log.e("NFC_LIFECYCLE", "Error enabling foreground dispatch: ${ex.message}", ex)
            }
        } else {
            Log.w("NFC_LIFECYCLE", "NFC is not ready, foreground dispatch NOT enabled in onResume.")
        }
        // 处理 Activity 启动时可能已有的 Intent
        val currentOnResumeIntent = intent
        if (_isNfcReady.value && currentOnResumeIntent != null && isNfcIntent(currentOnResumeIntent)) {
            Log.d("NFC_LIFECYCLE", "onResume: Intent has NFC action: ${currentOnResumeIntent.action}. Handling it.")
            handleIntent(currentOnResumeIntent)
            setIntent(Intent()) // 清除 Intent，避免重复处理
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("NFC_LIFECYCLE", "onPause called.")
        if (_isNfcReady.value) {
            try {
                nfcAdapter?.disableForegroundDispatch(this)
                Log.i("NFC_LIFECYCLE", "Foreground dispatch disabled successfully.")
            } catch (ex: IllegalStateException) {
                Log.e("NFC_LIFECYCLE", "Error disabling foreground dispatch: ${ex.message}", ex)
            }
        } else {
            Log.w("NFC_LIFECYCLE", "NFC is not ready, foreground dispatch NOT disabled in onPause.")
        }
        clearMessageJob?.cancel()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("NFC_INTENT_RAW", "onNewIntent received action: ${intent.action}") // 新增日志，查看原始接收到的action

        // 检查是否是NFC相关的Intent Action
        if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TAG_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {

            Log.d("NFC_INTENT_HANDLING", "Processing NFC Intent: ${intent.action}")

            val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }

            if (tag != null) {
                val cardId = bytesToHexString(tag.id)
                if (cardId == null) {
                    Log.e("NFC_DEBOUNCE", "Failed to get card ID from tag.")
                    // 可以考虑给用户一个通用错误提示
                    // updateOperationStatus("无法读取卡片ID", clearAfterDelay = true)
                    return
                }

                val currentTime = System.currentTimeMillis()

                // --- 去抖动逻辑开始 ---
                synchronized(this) { // 使用 synchronized 块确保线程安全
                    if (cardId == lastProcessedCardId &&
                        (currentTime - lastProcessedTimeMillis) < NFC_PROCESSING_DEBOUNCE_INTERVAL) {
                        Log.w("NFC_DEBOUNCE", "Duplicate NFC scan for card $cardId within ${NFC_PROCESSING_DEBOUNCE_INTERVAL}ms. CurrentTime: $currentTime, LastProcessed: $lastProcessedTimeMillis. IGNORED.")
                        return // 忽略这个重复的事件
                    }
                    // 如果不是重复事件，更新时间和ID，然后继续处理
                    lastProcessedCardId = cardId
                    lastProcessedTimeMillis = currentTime
                    Log.i("NFC_DEBOUNCE", "Processing NEW NFC scan for card $cardId at $currentTime. Debounce passed.")
                }
                // --- 去抖动逻辑结束 ---

                // 如果通过了去抖动检查，才调用 handleTag
                handleTag(tag) // 确保你的 handleTag 方法仍然存在并被正确调用

            } else {
                Log.w("NFC_INTENT_HANDLING", "Tag object is NULL in onNewIntent for action: ${intent.action}")
            }
        } else {
            Log.d("NFC_INTENT_RAW", "Intent action is not NFC related: ${intent.action}")
        }
    }

    private fun handleTag(tag: Tag) {
        // 播放声音和震动 (如果需要)
        playBeepSound()
        vibrate(100)

        val cardId = bytesToHexString(tag.id) // 你可能已经有这个了
        if (cardId == null) {
            Log.e("NFC_HANDLE_TAG", "Card ID is null in handleTag. Cannot proceed.")
            updateOperationStatus("无法读取卡片ID", clearAfterDelay = true)
            return
        }

        Log.d("NFC_HANDLE_TAG", "Tag handled, Card ID: $cardId. Launching scan processing.")

        // 确保你的 isAdminModeActive 逻辑在这里，或者在 handleUserTagScan/handleAdminTagScan 内部
        if (_isAdminModeActive.value) {
            Log.d("NFC_HANDLE_TAG", "Admin mode active. Calling admin scan handler.")
            lifecycleScope.launch {
                handleAdminTagScan(cardId) // 假设你有这个函数
            }
        } else {
            Log.d("NFC_HANDLE_TAG", "User mode active. Calling user scan handler.")
            lifecycleScope.launch {
                val cardInfo = withContext(Dispatchers.IO) { cardInfoDao.getCard(cardId) }
                handleUserTagScan(cardId, cardInfo)
            }
        }
    }

    // 假设你有 handleAdminTagScan，如果没有或者逻辑不同，请相应调整
    private suspend fun handleAdminTagScan(cardId: String) {
        Log.i("NFC_ADMIN_SCAN", "Admin scan for card: $cardId")
        withContext(Dispatchers.Main) {
            _currentCardId.value = cardId // 更新UI以显示当前扫描的卡号
            // 根据你的管理员逻辑更新操作状态或执行其他操作
            // 例如，可以直接调用 adminReadBalance() 或提示管理员可以进行的操作
            updateOperationStatus("管理员模式: 已扫描卡 $cardId. 请选择操作.", clearAfterDelay = false)
            adminReadBalance() // 例如，自动读取余额
        }
    }

    // isNfcIntent 保持不变，但可以考虑添加 NDEF_DISCOVERED
    private fun isNfcIntent(intent: Intent?): Boolean {
        if (intent == null || intent.action == null) {
            Log.d("NFC_CHECK", "isNfcIntent: Intent or action is null.")
            return false
        }
        val action = intent.action
        Log.d("NFC_CHECK", "isNfcIntent: Received action: $action")
        val isActionMatch = NfcAdapter.ACTION_TECH_DISCOVERED == action ||
                NfcAdapter.ACTION_TAG_DISCOVERED == action ||
                NfcAdapter.ACTION_NDEF_DISCOVERED == action // 建议添加
        if (!isActionMatch) {
            Log.d("NFC_CHECK", "isNfcIntent: Action does not match expected NFC actions.")
        }
        return isActionMatch
    }

    override fun onDestroy() {
        super.onDestroy()
        toneGenerator?.release()
        toneGenerator = null
        clearMessageJob?.cancel()
    }

    private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

    // handleIntent 保持你原有的逻辑，它会从传入的 intent 中提取 Tag
    // 如果你在 onNewIntent 中已经提取了 Tag，你可以选择:
    // 1. 仍然调用 handleIntent(intent) 让它自己提取。
    // 2. 修改 handleIntent 的参数为 handleIntent(tag: Tag) 并直接传递。
    //    (为了最小化改动，我这里保持了原样)
    private fun handleIntent(intent: Intent) {
        val action = intent.action
        Log.i("NFC_HANDLE", "handleIntent called with action: $action") // 添加日志

        // 确保 action 是有效的 NFC action
        if (!(NfcAdapter.ACTION_TECH_DISCOVERED == action ||
                    NfcAdapter.ACTION_TAG_DISCOVERED == action ||
                    NfcAdapter.ACTION_NDEF_DISCOVERED == action)) { // 保持与 isNfcIntent 一致
            Log.w("NFC_HANDLE", "handleIntent called with non-NFC or action mismatch: $action")
            // 可以选择在这里更新状态或直接返回
            // updateOperationStatus("无效的NFC操作: $action", clearAfterDelay = true)
            return
        }

        val newTagFromIntent: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }

        if (newTagFromIntent == null) {
            Log.w("NFC_HANDLE", "handleIntent: Tag from intent is NULL.") // 已有日志
            updateOperationStatus("错误: 未能获取标签信息")
            _currentCardId.value = null
            _currentBalance.value = null
            return
        }

        val incomingCardId = newTagFromIntent.id?.toHexString() ?: "未知ID"
        Log.d("NFC_HANDLE", "Tag detected. ID: $incomingCardId") // 已有日志

        playBeepSound()
        vibrate(100)

        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                _currentCardId.value = incomingCardId
                _currentBalance.value = null
                updateOperationStatus("卡片已检测 ($incomingCardId)... 正在处理...", clearAfterDelay = false)
            }

            withContext(Dispatchers.IO) {
                val cardInfo = cardInfoDao.getCard(incomingCardId)
                if (_isAdminModeActive.value) {
                    handleAdminTagScan(incomingCardId, cardInfo)
                } else {
                    handleUserTagScan(incomingCardId, cardInfo)
                }
            }
        }
    }

    // --- Admin Operations & User Tag Scan & UI Composables (ErrorScreen, MainNFCScreen, AdminPasswordDialog, AdminControls) ---
    // 保持你原有的这些函数不变
    // ...
    private fun updateOperationStatus(message: String, clearAfterDelay: Boolean = true, permanent: Boolean = false) {
        clearMessageJob?.cancel()
        _operationStatusMessage.value = message

        if (!permanent && clearAfterDelay && message != DEFAULT_MESSAGE_NORMAL && message != DEFAULT_MESSAGE_ADMIN) {
            clearMessageJob = lifecycleScope.launch {
                delay(MESSAGE_DISPLAY_DURATION)
                val defaultMsg = if (_isAdminModeActive.value) DEFAULT_MESSAGE_ADMIN else DEFAULT_MESSAGE_NORMAL
                if (_operationStatusMessage.value == message) {
                    _operationStatusMessage.value = defaultMsg
                }
            }
        } else if (permanent) {
            // For permanent messages like errors, no timed clear.
        }
    }


    private fun vibrate(milliseconds: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(milliseconds)
        }
    }

    private fun playBeepSound() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
    }
    private suspend fun handleAdminTagScan(cardId: String, cardInfo: CardInfo?) {
        withContext(Dispatchers.Main) {
            if (cardInfo != null) {
                _currentBalance.value = cardInfo.balanceInFen
                updateOperationStatus(
                    "管理员: 卡(${cardId}) 余额: ${"%.2f".format(cardInfo.balanceInFen / 100.0)}元",
                    clearAfterDelay = false
                )
            } else {
                _currentBalance.value = null
                updateOperationStatus(
                    "管理员: 新卡(${cardId}). 可设置初始余额.",
                    clearAfterDelay = false
                )
            }
        }
    }

    private suspend fun handleUserTagScan(cardId: String, cardInfo: CardInfo?) {
        Log.i("NFC_DEBUG_SCAN", "---------- New Scan Event Start ----------")
        Log.i("NFC_DEBUG_SCAN", "Handling user scan for Card ID: $cardId")

        if (cardInfo == null) {
            Log.w("NFC_DEBUG_SCAN", "CardInfo is NULL. Processing as UNKNOWN CARD.")
            withContext(Dispatchers.Main) {
                _currentCardId.value = cardId // Show scanned ID even if unknown
                _currentBalance.value = null
                updateOperationStatus("错误: 未知卡 ($cardId)。请先注册。", clearAfterDelay = true)
            }
            Log.i("NFC_DEBUG_SCAN", "---------- Scan Event End (Unknown Card) ----------")
            return
        }

        // Update UI with current card's balance immediately
        withContext(Dispatchers.Main) {
            _currentBalance.value = cardInfo.balanceInFen
            Log.d("NFC_DEBUG_SCAN", "UI: Displaying initial balance for $cardId: ${cardInfo.balanceInFen} fen")
        }

        Log.d("NFC_DEBUG_SCAN", "DB: Querying for latest unexited log for card: $cardId")
        val unexitedLog = accessLogDao.getLatestUnexitedLog(cardId)

        if (unexitedLog == null) {
            Log.i("NFC_DEBUG_SCAN", "DB_QUERY_RESULT: No unexited log found for $cardId. ==> Processing as ENTRY.")
            // --- 入场逻辑 ---
            val entryTime = System.currentTimeMillis()
            val newLog = AccessLog(
                cardIdFk = cardId,
                entryTimeMillis = entryTime,
                isInside = true // Mark as inside
            )
            val insertedLogId = accessLogDao.insertLog(newLog)
            Log.i("NFC_DEBUG_SCAN", "DB_INSERT: New AccessLog inserted. ID: $insertedLogId, CardID: $cardId, EntryTime: $entryTime, IsInside: true")

            withContext(Dispatchers.Main) {
                _isInsideArea.value = true
                _entryCardId.value = cardId
                val balanceInYuan = "%.2f".format(cardInfo.balanceInFen / 100.0)
                updateOperationStatus("欢迎入场! 卡号: $cardId. 余额: $balanceInYuan 元")
                Log.d("NFC_DEBUG_SCAN", "UI_UPDATE: Entry successful. isInsideArea=true, entryCardId=$cardId")
            }
        } else {
            Log.i("NFC_DEBUG_SCAN", "DB_QUERY_RESULT: Unexited log FOUND for $cardId. ID: ${unexitedLog.id}, CardID: ${unexitedLog.cardIdFk}, EntryTime: ${unexitedLog.entryTimeMillis}, IsInside: ${unexitedLog.isInside} ==> Processing as EXIT.")
            // --- 出场逻辑 ---
            // Sanity check, though query should ensure this
            if (unexitedLog.cardIdFk != cardId) {
                Log.e("NFC_DEBUG_SCAN", "CRITICAL_ERROR: Mismatched card ID in unexitedLog! Expected $cardId, got ${unexitedLog.cardIdFk}. Aborting exit.")
                updateOperationStatus("系统错误: 卡ID不匹配", clearAfterDelay = true)
                Log.i("NFC_DEBUG_SCAN", "---------- Scan Event End (Critical Error) ----------")
                return
            }
            if (!unexitedLog.isInside) {
                Log.e("NFC_DEBUG_SCAN", "LOGIC_ERROR: Found an 'unexited' log (ID: ${unexitedLog.id}) but its 'isInside' is already false. This indicates a previous error or an issue with getLatestUnexitedLog query. Treating as a new entry to be safe.")
                // To be safe, maybe treat as a new entry or show an error
                // For now, let's log and proceed to create a new entry log to avoid getting stuck.
                // This part might need more sophisticated handling based on business rules.
                updateOperationStatus("系统提示: 发现异常记录,已重置为入场.", clearAfterDelay = true)
                // Effectively restart the process for this card as an entry
                val entryTime = System.currentTimeMillis()
                val newLogOnError = AccessLog(cardIdFk = cardId, entryTimeMillis = entryTime, isInside = true)
                accessLogDao.insertLog(newLogOnError)
                Log.i("NFC_DEBUG_SCAN", "DB_INSERT_RECOVERY: New AccessLog inserted due to prior inconsistent state. CardID: $cardId, IsInside: true")
                withContext(Dispatchers.Main) {
                    _isInsideArea.value = true
                    _entryCardId.value = cardId
                    updateOperationStatus("欢迎重新入场! 卡号: $cardId")
                }
                Log.i("NFC_DEBUG_SCAN", "---------- Scan Event End (Recovery as Entry) ----------")
                return
            }


            val exitTime = System.currentTimeMillis()
            val amountToDeduct = _autoDeductAmountInFen.value // Assuming this is defined elsewhere
            Log.d("NFC_DEBUG_SCAN", "EXIT_CALC: Amount to deduct: $amountToDeduct fen")

            if (cardInfo.balanceInFen >= amountToDeduct) {
                val newBalance = cardInfo.balanceInFen - amountToDeduct
                Log.d("NFC_DEBUG_SCAN", "EXIT_BALANCE: Sufficient balance. Current: ${cardInfo.balanceInFen}, New: $newBalance")

                val updatedCardInfo = cardInfo.copy(balanceInFen = newBalance)
                cardInfoDao.updateCard(updatedCardInfo)
                Log.i("NFC_DEBUG_SCAN", "DB_UPDATE: CardInfo updated for $cardId. New balance: $newBalance fen")

                val logToUpdate = unexitedLog.copy(
                    exitTimeMillis = exitTime,
                    amountDeductedInFen = amountToDeduct,
                    isInside = false // CRUCIAL: Setting isInside to false
                )
                accessLogDao.updateLog(logToUpdate)
                Log.i("NFC_DEBUG_SCAN", "DB_UPDATE: AccessLog updated. ID: ${logToUpdate.id}, CardID: ${logToUpdate.cardIdFk}, ExitTime: $exitTime, AmountDeducted: $amountToDeduct, IsInside: ${logToUpdate.isInside}")

                // --- IMPORTANT VERIFICATION STEP ---
                val verificationLog = accessLogDao.getLatestUnexitedLog(cardId)
                if (verificationLog == null) {
                    Log.i("NFC_DEBUG_SCAN", "DB_VERIFICATION_AFTER_EXIT: SUCCESS! getLatestUnexitedLog for $cardId now returns NULL (as expected).")
                } else {
                    Log.e("NFC_DEBUG_SCAN", "DB_VERIFICATION_AFTER_EXIT: FAILED! getLatestUnexitedLog for $cardId still returns a log. ID: ${verificationLog.id}, IsInside: ${verificationLog.isInside}. THIS IS THE PROBLEM!")
                }
                // --- END OF VERIFICATION STEP ---

                withContext(Dispatchers.Main) {
                    _currentBalance.value = newBalance
                    _isInsideArea.value = false
                    _entryCardId.value = null
                    val newBalanceInYuan = "%.2f".format(newBalance / 100.0)
                    val amountDeductedInYuan = "%.2f".format(amountToDeduct / 100.0)
                    updateOperationStatus("再见! 扣款: $amountDeductedInYuan 元. 新余额: $newBalanceInYuan 元")
                    Log.d("NFC_DEBUG_SCAN", "UI_UPDATE: Exit successful. isInsideArea=false, entryCardId=null")
                }
            } else {
                Log.w("NFC_DEBUG_SCAN", "EXIT_BALANCE: Insufficient balance. Current: ${cardInfo.balanceInFen}, Required: $amountToDeduct")
                withContext(Dispatchers.Main) {
                    val currentBalanceInYuan = "%.2f".format(cardInfo.balanceInFen / 100.0)
                    val amountToDeductInYuan = "%.2f".format(amountToDeduct / 100.0)
                    updateOperationStatus("错误: 余额不足 ($currentBalanceInYuan 元). 无法扣款 $amountToDeductInYuan 元")
                }
            }
        }
        Log.i("NFC_DEBUG_SCAN", "---------- Scan Event End ----------")
    }

    private fun adminReadBalance() {
        val cardId = _currentCardId.value
        if (cardId == null) {
            updateOperationStatus("管理员: 请先扫描卡片.")
            return
        }
        updateOperationStatus("管理员: 正在读取余额...", clearAfterDelay = false)
        lifecycleScope.launch {
            val cardInfo = withContext(Dispatchers.IO) { cardInfoDao.getCard(cardId) }
            withContext(Dispatchers.Main) {
                if (cardInfo != null) {
                    _currentBalance.value = cardInfo.balanceInFen
                    updateOperationStatus("管理员: 卡(${cardId}) 余额: ${"%.2f".format(cardInfo.balanceInFen / 100.0)}元", clearAfterDelay = false)
                } else {
                    _currentBalance.value = null
                    updateOperationStatus("管理员: 未找到卡(${cardId})的信息.", clearAfterDelay = false)
                }
            }
        }
    }

    private fun adminSetBalance(amountInYuanString: String) {
        val cardId = _currentCardId.value
        if (cardId == null) {
            updateOperationStatus("管理员: 请先扫描有效的卡")
            return
        }
        val amountInYuan = amountInYuanString.toDoubleOrNull()
        if (amountInYuan == null || amountInYuan < 0) {
            updateOperationStatus("管理员: 请输入有效金额 (可为0)")
            return
        }
        val amountInFen = (amountInYuan * 100).toInt()
        updateOperationStatus("管理员: 正在设置余额...", clearAfterDelay = false)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                var cardInfo = cardInfoDao.getCard(cardId)
                if (cardInfo != null) {
                    cardInfoDao.updateCard(cardInfo.copy(balanceInFen = amountInFen))
                } else {
                    cardInfoDao.insertCard(CardInfo(cardId = cardId, balanceInFen = amountInFen))
                }
            }
            withContext(Dispatchers.Main) {
                _currentBalance.value = amountInFen
                updateOperationStatus("管理员: 卡(${cardId})余额已设为 ${"%.2f".format(amountInFen / 100.0)}元")
            }
        }
    }

    private fun adminManualDeduct(amountInYuanString: String) {
        val cardId = _currentCardId.value
        if (cardId == null) {
            updateOperationStatus("管理员: 请先扫描有效的卡")
            return
        }
        val amountInYuan = amountInYuanString.toDoubleOrNull()
        if (amountInYuan == null || amountInYuan <= 0) {
            updateOperationStatus("管理员: 请输入有效的正数扣款金额")
            return
        }
        val amountInFenToDeduct = (amountInYuan * 100).toInt()
        updateOperationStatus("管理员: 手动扣款中...", clearAfterDelay = false)
        lifecycleScope.launch {
            val cardInfo = withContext(Dispatchers.IO) { cardInfoDao.getCard(cardId) }
            withContext(Dispatchers.Main) {
                if (cardInfo != null) {
                    if (cardInfo.balanceInFen >= amountInFenToDeduct) {
                        val newBalance = cardInfo.balanceInFen - amountInFenToDeduct
                        withContext(Dispatchers.IO) {
                            cardInfoDao.updateCard(cardInfo.copy(balanceInFen = newBalance))
                        }
                        _currentBalance.value = newBalance
                        updateOperationStatus("管理员: 卡(${cardId})手动扣款成功. 新余额: ${"%.2f".format(newBalance / 100.0)}元")
                    } else {
                        updateOperationStatus("管理员: 余额不足 (${"%.2f".format(cardInfo.balanceInFen / 100.0)}元) 无法扣款.")
                    }
                } else {
                    updateOperationStatus("管理员: 未找到卡(${cardId})的信息，无法扣款.")
                }
            }
        }
    }
    @Composable
    fun ErrorScreen(message: String) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )
        }
    }

    @Composable
    fun AdminPasswordDialog() {
        val adminPasswordInputVal by _adminPasswordInput
        val adminStatusMessageVal by _adminStatusMessage
        AlertDialog(
            onDismissRequest = {
                _showAdminPasswordDialog.value = false
                _adminPasswordInput.value = ""
                _adminStatusMessage.value = null
            },
            title = { Text("管理员登录") },
            text = {
                Column {
                    OutlinedTextField(
                        value = adminPasswordInputVal,
                        onValueChange = { _adminPasswordInput.value = it },
                        label = { Text("请输入密码") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = adminStatusMessageVal?.startsWith("错误") == true
                    )
                    if (adminStatusMessageVal != null) {
                        Text(
                            adminStatusMessageVal!!,
                            color = if (adminStatusMessageVal!!.startsWith("错误")) MaterialTheme.colorScheme.error else Color.Green,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (adminPasswordInputVal == ADMIN_PASSWORD) {
                        _adminStatusMessage.value = "密码正确。"
                        _isAdminModeActive.value = true
                        _isInsideArea.value = false
                        _entryCardId.value = null
                        _showAdminPasswordDialog.value = false
                        _adminPasswordInput.value = ""
                        updateOperationStatus(DEFAULT_MESSAGE_ADMIN, clearAfterDelay = false)

                        if (_currentCardId.value != null) {
                            adminReadBalance()
                        } else {
                            _currentBalance.value = null
                        }
                    } else {
                        _adminStatusMessage.value = "错误: 密码错误！"
                    }
                }) { Text("登录") }
            },
            dismissButton = {
                Button(onClick = {
                    _showAdminPasswordDialog.value = false
                    _adminPasswordInput.value = ""
                    _adminStatusMessage.value = null
                }) { Text("取消") }
            }
        )
    }

    // 在 MainActivity 中添加一个方法来更新自动扣款金额
    private fun adminSetAutoDeductAmount(amountInYuanString: String) {
        val amountInYuan = amountInYuanString.toDoubleOrNull()
        if (amountInYuan == null || amountInYuan < 0) {
            updateOperationStatus("管理员: 请输入有效的自动扣款金额 (可为0)")
            return
        }
        val amountInFen = (amountInYuan * 100).toInt()
        _autoDeductAmountInFen.value = amountInFen
        updateOperationStatus("管理员: 自动扣款金额已设置为 ${"%.2f".format(amountInFen / 100.0)}元")
    }

    @Composable
    fun MainNFCScreen() {
        var currentTime by remember {
            mutableStateOf(SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()))
        }
        LaunchedEffect(Unit) { // For current time update
            while (true) {
                currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                delay(1000)
            }
        }

        val cardIdFromState by _currentCardId
        val balanceState by _currentBalance
        val operationStatus by _operationStatusMessage
        var adminAmountInput by remember { mutableStateOf("") }

        // --- 新增：用于设置自动扣款金额的输入状态 ---
        var adminAutoDeductAmountInput by remember { mutableStateOf("%.2f".format(_autoDeductAmountInFen.value / 100.0)) }

        val isAdminMode by _isAdminModeActive
        val isInside by _isInsideArea
        val entryCardDisplay by _entryCardId

        // --- 状态：控制普通模式下卡号和余额的临时可见性 ---
        var showCardDetailsTemporarily_UserMode by remember { mutableStateOf(false) }
        // --- 状态：记录触发普通模式下显示详情的卡号 ---
        var cardIdForTemporaryDisplay_UserMode by remember { mutableStateOf<String?>(null) }

        // 当卡号变化时，处理普通用户模式下的临时显示
        LaunchedEffect(cardIdFromState, isAdminMode) { // 也监听 isAdminMode 的变化
            if (!isAdminMode && cardIdFromState != null) { // 只在非管理员模式下触发
                cardIdForTemporaryDisplay_UserMode = cardIdFromState
                showCardDetailsTemporarily_UserMode = true
                delay(2000L) // 显示 2 秒
                if (cardIdForTemporaryDisplay_UserMode == _currentCardId.value && !isAdminMode) { // 再次检查模式
                    showCardDetailsTemporarily_UserMode = false
                }
            } else if (!isAdminMode && cardIdFromState == null) {
                // 如果在非管理员模式下卡号变为空
                showCardDetailsTemporarily_UserMode = false
                cardIdForTemporaryDisplay_UserMode = null
            } else if (isAdminMode) {
                // 进入管理员模式时，确保用户模式的临时显示被关闭
                showCardDetailsTemporarily_UserMode = false
            }
        }
        // --- 新增：当 _autoDeductAmountInFen (来自 MainActivity 的状态) 变化时，更新本地输入框 ---
        // 这样如果通过其他方式修改了该值（虽然本例中不太可能），UI也能同步
        LaunchedEffect(_autoDeductAmountInFen.value) {
            adminAutoDeductAmountInput = "%.2f".format(_autoDeductAmountInFen.value / 100.0)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- 上部显示区域 ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = currentTime,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = operationStatus,
                    fontSize = 28.sp,
                    color = if (operationStatus.startsWith("错误") || operationStatus.startsWith("管理员: 错误")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // --- 修改点：根据模式控制卡号和余额显示 ---
                if (isAdminMode && cardIdFromState != null) {
                    // 管理员模式：持续显示卡号和余额
                    Text(
                        text = "当前卡号 (管理员): $cardIdFromState", // 可以加个标识
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    balanceState?.let { fen ->
                        Text(
                            text = "卡内余额: ${"%.2f".format(fen / 100.0)} 元",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                } else if (!isAdminMode && showCardDetailsTemporarily_UserMode && cardIdFromState != null) {
                    // 普通用户模式：临时显示卡号和余额
                    Text(
                        text = "当前卡号: $cardIdFromState",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    balanceState?.let { fen ->
                        Text(
                            text = "卡内余额: ${"%.2f".format(fen / 100.0)} 元",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                } else if (!isAdminMode && cardIdFromState != null && !showCardDetailsTemporarily_UserMode) {
                    // 普通用户模式：卡号存在但临时显示已结束
                    // Spacer(modifier = Modifier.height(44.dp)) // 可选：保持空间防止跳动
                }


                // 非管理员模式下的入场状态显示
                if (isInside && entryCardDisplay != null && !isAdminMode) {
                    Text(
                        text = "卡 ($entryCardDisplay) 已入场",
                        fontSize = 18.sp,
                        color = Color.Green,
                        fontWeight = FontWeight.Bold
                    )
                }
            } // End of scrollable content Column

            // --- 底部控制区域 (管理员控件 或 切换按钮) ---
            if (isAdminMode) {
                AdminControls(
                    currentBalanceInFen = balanceState, // <<<< 新增：传递余额状态
                    adminAmountInput = adminAmountInput,
                    onAdminAmountChange = { adminAmountInput = it.filter { char -> char.isDigit() || char == '.' }.take(6) },
                    onSetBalance = {
                        adminSetBalance(adminAmountInput)
                        adminAmountInput = ""
                    },
                    onManualDeduct = {
                        adminManualDeduct(adminAmountInput)
                        adminAmountInput = ""
                    },
                    onReadBalance = { adminReadBalance() },
                    onExitAdminMode = {
                        _isAdminModeActive.value = false
                        _currentCardId.value = null
                        _currentBalance.value = null
                        showCardDetailsTemporarily_UserMode = false
                        updateOperationStatus(DEFAULT_MESSAGE_NORMAL, clearAfterDelay = false)
                    },
                    // --- 新增参数传递 ---
                    currentAutoDeductAmountInFen = _autoDeductAmountInFen.value, // 传递当前自动扣款金额
                    adminAutoDeductAmountInput = adminAutoDeductAmountInput, // 传递输入框状态
                    onAdminAutoDeductAmountChange = { adminAutoDeductAmountInput = it.filter { char -> char.isDigit() || char == '.' }.take(6) }, // 传递输入框变化回调
                    onSetAutoDeductAmount = { // 传递设置金额的回调
                        adminSetAutoDeductAmount(adminAutoDeductAmountInput)
                        // adminAutoDeductAmountInput = "" // 清空或保持当前设置值都可以
                    }
                )
            } else {
                Button(
                    onClick = { _showAdminPasswordDialog.value = true },
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .padding(vertical = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("管理员模式", fontSize = 20.sp)
                }
            }
        }
    }
    @Composable
    fun AdminControls(
        currentBalanceInFen: Int?,
        adminAmountInput: String, // 用于设置余额/手动扣款
        onAdminAmountChange: (String) -> Unit,
        onSetBalance: () -> Unit,
        onManualDeduct: () -> Unit,
        onReadBalance: () -> Unit,
        onExitAdminMode: () -> Unit,
        // --- 新增参数 ---
        currentAutoDeductAmountInFen: Int,
        adminAutoDeductAmountInput: String, // 用于设置自动扣款金额
        onAdminAutoDeductAmountChange: (String) -> Unit,
        onSetAutoDeductAmount: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "管理员操作",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp)) // 稍微减小一点与上方主余额的间距

            // --- 在这里显示余额 ---
            if (_currentCardId.value != null) { // 只在有卡被扫描时显示余额
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(0.9f) // 与输入框宽度对齐
                ) {
                    Text(
                        text = "当前卡余额: ",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant // 使用稍微柔和的颜色
                    )
                    currentBalanceInFen?.let { fen ->
                        Text(
                            text = "%.2f 元".format(fen / 100.0),
                            fontSize = 18.sp, // 让余额数字稍大一点
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary // 或其他醒目的颜色
                        )
                    } ?: Text(
                        text = "读取中...", // 如果余额为null（例如刚扫卡还没查到）
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp)) // 余额和输入框之间的间距
            }


            OutlinedTextField(
                value = adminAmountInput,
                onValueChange = onAdminAmountChange,
                label = { Text("金额 (元)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.9f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onSetBalance,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(vertical = 4.dp)
            ) { Text("设置余额", fontSize = 18.sp) }

            Button(
                onClick = onManualDeduct,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(vertical = 4.dp)
            ) { Text("手动扣款", fontSize = 18.sp) }

            Button(
                onClick = onReadBalance,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(vertical = 4.dp)
            ) { Text("刷新卡余额", fontSize = 18.sp) }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onExitAdminMode,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(vertical = 4.dp)
            ) { Text("退出管理员模式", fontSize = 18.sp) }
            Spacer(modifier = Modifier.height(8.dp))

            // --- 新增：设置自动扣款金额的区域 ---
            Text(
                text = "当前自动扣款: %.2f 元".format(currentAutoDeductAmountInFen / 100.0),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(0.9f),
                verticalAlignment = Alignment.CenterVertically,

                ) {
                OutlinedTextField(
                    value = adminAutoDeductAmountInput,
                    onValueChange = onAdminAutoDeductAmountChange,
                    label = { Text("新自动扣款额 (元)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f) // 让输入框占据可用空间
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onSetAutoDeductAmount,
                    modifier = Modifier.wrapContentWidth() // 按钮宽度根据内容自适应
                ) {
                    Text("确认", fontSize = 16.sp)
                }
            }
        }
    } }