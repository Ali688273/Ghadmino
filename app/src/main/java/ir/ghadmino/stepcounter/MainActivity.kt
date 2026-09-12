package ir.ghadmino.stepcounter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import ir.ghadmino.stepcounter.step.StepCounterService
import ir.ghadmino.stepcounter.ui.theme.GhadminoTheme

import kotlinx.coroutines.delay


class MainActivity : ComponentActivity() {

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            // نتیجه مجوزها در ادامه بررسی می‌شود.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions()

        setContent {
            GhadminoTheme {
                GhadminoApp()
            }
        }

        startStepService()
    }

    private fun requestPermissions() {

        val permissions = mutableListOf<String>()

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            checkSelfPermission(
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(
                Manifest.permission.ACTIVITY_RECOGNITION
            )
        }

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(
                permissions.toTypedArray()
            )
        }
    }

    private fun startStepService() {

        val intent =
            Intent(
                this,
                StepCounterService::class.java
            )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            startForegroundService(intent)

        } else {

            startService(intent)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GhadminoApp() {

    var goal by remember {
        mutableIntStateOf(8000)
    }

    var steps by remember {
        mutableIntStateOf(
            StepCounterService.todaySteps
        )
    }

    LaunchedEffect(Unit) {

        while (true) {

            steps =
                StepCounterService.todaySteps

            delay(1000)
        }
    }

    val progress =
        if (goal > 0) {

            (steps.toFloat() / goal.toFloat())
                .coerceIn(0f, 1f)

        } else {

            0f
        }

    val distanceKm =
        steps * 0.00075

    val calories =
        steps * 0.04

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Column {

                        Text(
                            text = "قدم‌شمار قدمینو",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "فعالیت امروز",
                            style =
                                MaterialTheme
                                    .typography
                                    .labelSmall
                        )
                    }
                },

                actions = {

                    IconButton(
                        onClick = {}
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Settings,
                            contentDescription =
                                "تنظیمات"
                        )
                    }
                }
            )
        }

    ) { paddingValues ->

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(
                        rememberScrollState()
                    ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )


            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                colors =
                    CardDefaults.cardColors(

                        containerColor =
                            MaterialTheme
                                .colorScheme
                                .primaryContainer
                    )
            ) {

                Column(

                    modifier =
                        Modifier.padding(24.dp),

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Icon(

                        imageVector =
                            Icons.Default.DirectionsWalk,

                        contentDescription = null,

                        modifier =
                            Modifier.size(50.dp)
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(

                        text =
                            steps.toString(),

                        style =
                            MaterialTheme
                                .typography
                                .displayLarge,

                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        "قدم امروز"
                    )

                    Spacer(
                        modifier =
                            Modifier.height(18.dp)
                    )

                    LinearProgressIndicator(

                        progress = progress,

                        modifier =
                            Modifier.fillMaxWidth()
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        "$steps از $goal قدم"
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )


            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                StatCard(

                    modifier =
                        Modifier.weight(1f),

                    icon = {

                        Icon(
                            Icons.Default.Route,
                            contentDescription = null
                        )
                    },

                    title = "مسافت",

                    value =
                        String.format(
                            "%.2f km",
                            distanceKm
                        )
                )


                StatCard(

                    modifier =
                        Modifier.weight(1f),

                    icon = {

                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = null
                        )
                    },

                    title = "کالری",

                    value =
                        "${calories.toInt()} kcal"
                )
            }


            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )


            Card(

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column(

                    modifier =
                        Modifier.padding(18.dp)
                ) {

                    Text(

                        "هدف روزانه",

                        style =
                            MaterialTheme
                                .typography
                                .titleMedium
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        "$goal قدم"
                    )

                    Slider(

                        value =
                            goal.toFloat(),

                        onValueChange = {

                            goal =
                                it.toInt()
                        },

                        valueRange =
                            1000f..30000f
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )


            Card(

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column(

                    modifier =
                        Modifier.padding(18.dp)
                ) {

                    Text(

                        "وضعیت قدم‌شمار",

                        style =
                            MaterialTheme
                                .typography
                                .titleMedium
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(

                        if (
                            StepCounterService
                                .sensorAvailable
                        ) {

                            "✓ قدم‌شمار فعال است"

                        } else {

                            "⚠ حسگر قدم‌شمار روی این گوشی پیدا نشد"
                        }
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(24.dp)
            )


            Text(

                "قدمینو • نسخه 1.0.0",

                style =
                    MaterialTheme
                        .typography
                        .labelSmall
            )
        }
    }
}


@Composable
fun StatCard(

    modifier: Modifier,

    icon: @Composable () -> Unit,

    title: String,

    value: String

) {

    Card(

        modifier =
            modifier
    ) {

        Column(

            modifier =
                Modifier.padding(16.dp)
        ) {

            icon()

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(

                title,

                style =
                    MaterialTheme
                        .typography
                        .labelMedium
            )

            Text(

                value,

                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}
