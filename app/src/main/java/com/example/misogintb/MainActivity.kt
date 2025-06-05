package com.example.misogintb

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.ToneGenerator
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcA
import android.os.*
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class MainActivity : ComponentActivity() {

    private lateinit var vibrator: Vibrator
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var pendingIntent: PendingIntent
    private lateinit var intentFiltersArray: Array<IntentFilter>
    private lateinit var techListsArray: Array<Array<String>>
    private var tag: Tag? = null
    private var currentTagId: String = ""
    private val cardStatusMap = HashMap<String, UserData>()
    private var toneGenerator: ToneGenerator? = null

    private var _currentStatus = mutableStateOf(NFCStatus.STANDBY)
    private val currentStatus: NFCStatus
        get() = _currentStatus.value

    private var _pendingRemainingTime = mutableStateOf(9)
    private val pendingRemainingTime: Int
        get() = _pendingRemainingTime.value

    private fun updatePendingRemainingTime(newTime: Int) {
        runOnUiThread { _pendingRemainingTime.value = newTime }
    }

    private fun updateCurrentStatus(newStatus: NFCStatus) {
        runOnUiThread { _currentStatus.value = newStatus }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vibrator = getSystemService(Vibrator::class.java)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null || !nfcAdapter!!.isEnabled) {
            Log.e("NFC", "NFC不可用或被禁用")
            return
        }

        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )


        val ndef = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        try {
            ndef.addDataType("*/*")  // 允许所有类型
        } catch (e: IntentFilter.MalformedMimeTypeException) {
            e.printStackTrace()
        }
        intentFiltersArray = arrayOf(ndef)

        techListsArray = arrayOf(
            arrayOf(android.nfc.tech.NfcA::class.java.name),
            arrayOf(android.nfc.tech.NfcB::class.java.name),
            arrayOf(android.nfc.tech.NfcF::class.java.name),
            arrayOf(android.nfc.tech.NfcV::class.java.name),
            arrayOf(android.nfc.tech.IsoDep::class.java.name),
            arrayOf(android.nfc.tech.MifareClassic::class.java.name),
            arrayOf(android.nfc.tech.MifareUltralight::class.java.name),
            arrayOf(android.nfc.tech.Ndef::class.java.name)
        )


        toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)

        setContent {
            MaterialTheme {
                MainScreen()
            }
        }
    }

    private fun vibrate(milliseconds: Long) {
        @Suppress("DEPRECATION")
        vibrator.vibrate(milliseconds)
    }

    private fun processTag(tag: Tag?) {
        if (tag == null) return
        val id = tag.id ?: return

        val stringBuilder = StringBuilder()
        for (byte in id) {
            stringBuilder.append(String.format("%02X", byte))
        }
        currentTagId = stringBuilder.toString()

        playBeepSound()
        updatePendingRemainingTime(9)

        if (currentTagId.isNotEmpty()) {
            updateCurrentStatus(NFCStatus.PENDING)
        } else {
            updateCurrentStatus(NFCStatus.STANDBY)
        }
    }


    private fun playBeepSound() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        if (tag == null) {
            Log.e("NFC", "tag is null!")
            return
        }
        Log.d("NFC", "Tag detected: ${tag.id?.joinToString("") { String.format("%02X", it) }}")
        processTag(tag)
    }


    override fun onDestroy() {
        super.onDestroy()
        toneGenerator?.release()
        toneGenerator = null
        _pendingRemainingTime.value = 9
    }

    data class UserData(
        val tagId: String = "",
        var isOnline: Boolean = false,
        var startTime: Long = 0,
        var duration: Long = 0,
        var cost: Double = 0.0
    )

    enum class NFCStatus {
        STANDBY, PENDING, PAYMENT
    }

    @Composable
    fun MainScreen() {
        var currentTime by remember {
            mutableStateOf(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
        }
        LaunchedEffect(Unit) {
            while (true) {
                delay(1000)
                currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            }
        }
        LandscapeScreen(currentTime)
    }

    @Composable
    fun LandscapeScreen(currentTime: String) {
        var localRemainingTime by remember { mutableStateOf(9) }
        val status = currentStatus
        val tagId = currentTagId
        val pendingTime = pendingRemainingTime

        LaunchedEffect(status) {
            if (status == NFCStatus.PAYMENT) {
                localRemainingTime = 9
                while (localRemainingTime > 0) {
                    delay(1000)
                    localRemainingTime--
                }
                updateCurrentStatus(NFCStatus.STANDBY)
                currentTagId = ""
            } else if (status == NFCStatus.PENDING && tagId.isNotEmpty()) {
                var countdown = 9
                while (countdown > 0) {
                    delay(1000)
                    updatePendingRemainingTime(countdown - 1)
                    countdown--
                }
                updateCurrentStatus(NFCStatus.STANDBY)
                currentTagId = ""
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = currentTime,
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                when (status) {
                    NFCStatus.STANDBY -> Text(
                        text = "请刷卡",
                        color = Color.White,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    NFCStatus.PENDING -> {
                        val user = cardStatusMap[tagId]

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // 卡号显示（放在按钮上方，字体小点）
                            Text(
                                text = "卡号: $tagId",
                                fontSize = 16.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )

                            if (user?.isOnline == true) {
                                Button(
                                    onClick = {
                                        user.isOnline = false
                                        val durationMillis = System.currentTimeMillis() - user.startTime
                                        user.duration = durationMillis

                                        val billingUnitMillis = 720_000L // 12分钟，12 * 60 * 1000 毫秒

                                        val units = (durationMillis + billingUnitMillis - 1) / billingUnitMillis
                                        user.cost = units.toDouble()

                                        updateCurrentStatus(NFCStatus.PAYMENT)
                                    },
                                    shape = RectangleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    modifier = Modifier.fillMaxWidth(0.8f)
                                ) {
                                    Text("确认下机($pendingTime)", fontSize = 64.sp)
                                }
                            }
                            else {
                                Button(
                                    onClick = {
                                        cardStatusMap[tagId] = UserData(
                                            tagId = tagId,
                                            isOnline = true,
                                            startTime = System.currentTimeMillis()
                                        )
                                        updateCurrentStatus(NFCStatus.STANDBY)
                                    },
                                    shape = RectangleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                                    modifier = Modifier.fillMaxWidth(0.8f)
                                ) {
                                    Text("确认上机($pendingTime)", fontSize = 64.sp)
                                }
                            }
                        }
                    }




                    NFCStatus.PAYMENT -> {
                        cardStatusMap[tagId]?.let { user ->
                            Text(
                                text = "总时长: ${user.duration.toDuration(DurationUnit.MILLISECONDS)}",
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "总费用: ${"%.2f".format(user.cost)} 元",
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                        Text(
                            text = "请寻找收款码支付",
                            color = Color.White,
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "$localRemainingTime",
                            color = Color.White,
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
