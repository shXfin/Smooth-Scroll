package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = androidx.activity.SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            MyApplicationTheme(darkTheme = false, dynamicColor = false) {
                SmoothScrollFixerApp()
            }
        }
    }
}

@Composable
fun Modifier.expressiveClick(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.45f,
            stiffness = Float.MAX_VALUE // Snappy
        ),
        label = "expressive_click_scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1.0f,
        label = "expressive_click_alpha"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            onClick = onClick
        )
}

@Composable
fun SmoothScrollFixerApp(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val appState by viewModel.appState.collectAsState()
    val selectedVideo by viewModel.selectedVideo.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val outputUri by viewModel.outputUri.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.selectVideo(context, uri)
            }
        }
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = PastelLavenderBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PastelLavenderBg)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "SmoothScroll",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp,
                            color = DeepVioletText,
                            lineHeight = 36.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "FIXER",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = DeepLavenderPurple,
                                letterSpacing = 4.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PastelActivePurple,
                                modifier = Modifier.padding(top = 1.dp)
                            ) {
                                Text(
                                    text = "60FPS CFR",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepVioletText,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            val interactionSourceChip = remember { MutableInteractionSource() }
                            val isChipPressed by interactionSourceChip.collectIsPressedAsState()
                            val chipCornerRadius by animateDpAsState(
                                targetValue = if (isChipPressed) 14.dp else 0.dp,
                                animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
                                label = "chip_corner_anim"
                            )
                            Surface(
                                shape = RoundedCornerShape(chipCornerRadius),
                                color = DeepLavenderPurple,
                                modifier = Modifier
                                    .padding(top = 1.dp)
                                    .clickable(
                                        interactionSource = interactionSourceChip,
                                        indication = null,
                                        onClick = { /* Status chip interactivity */ }
                                    )
                            ) {
                                Text(
                                    text = "H.264",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (appState != AppState.IDLE) {
                        IconButton(
                            onClick = { viewModel.resetState() },
                            modifier = Modifier
                                .testTag("reset_button")
                                .expressiveClick { viewModel.resetState() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Screen",
                                tint = DeepLavenderPurple
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Primary Screen State Switcher
                when (appState) {
                    AppState.IDLE -> {
                        IdleStateLayout(onPickClick = {
                            filePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                        })
                    }

                    AppState.VIDEO_SELECTED -> {
                        VideoSelectedLayout(
                            selectedVideo = selectedVideo!!,
                            onConvertClick = { viewModel.startConversion(context) },
                            onSelectDifferent = {
                                filePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                            }
                        )
                    }

                    AppState.CONVERTING -> {
                        ConvertingLayout(progress = progress)
                    }

                    AppState.COMPLETED -> {
                        CompletedLayout(
                            videoName = selectedVideo?.name ?: "video.mp4",
                            outputUri = outputUri,
                            onShareClick = {
                                outputUri?.let { shareVideo(context, it) }
                            },
                            onDoneClick = { viewModel.resetState() }
                        )
                    }

                    AppState.FAILED -> {
                        FailedLayout(
                            errorMessage = errorMessage ?: "An unknown transcoding error occurred.",
                            onRetryClick = {
                                if (selectedVideo != null) {
                                    viewModel.startConversion(context)
                                } else {
                                    viewModel.resetState()
                                }
                            },
                            onResetClick = { viewModel.resetState() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IdleStateLayout(onPickClick: () -> Unit) {
    val interactionSourceCard = remember { MutableInteractionSource() }
    val isCardPressed by interactionSourceCard.collectIsPressedAsState()

    // 1. Center card morphs from soft rectangle (32dp) to squashed organic oval on touch down
    val cardTopStart by animateDpAsState(
        targetValue = if (isCardPressed) 110.dp else 32.dp,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "card_ts"
    )
    val cardTopEnd by animateDpAsState(
        targetValue = if (isCardPressed) 50.dp else 32.dp,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "card_te"
    )
    val cardBottomEnd by animateDpAsState(
        targetValue = if (isCardPressed) 110.dp else 32.dp,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "card_be"
    )
    val cardBottomStart by animateDpAsState(
        targetValue = if (isCardPressed) 50.dp else 32.dp,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "card_bs"
    )
    val cardShape = RoundedCornerShape(
        topStart = cardTopStart,
        topEnd = cardTopEnd,
        bottomEnd = cardBottomEnd,
        bottomStart = cardBottomStart
    )

    val cardScale by animateFloatAsState(
        targetValue = if (isCardPressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "card_scale_anim"
    )

    // 2. Bottom "Select Video" button morphs from sharp rectangular pill to distorted octagon blob
    val interactionSourceBtn = remember { MutableInteractionSource() }
    val isBtnPressed by interactionSourceBtn.collectIsPressedAsState()

    val btnTopStart by animateDpAsState(
        targetValue = if (isBtnPressed) 32.dp else 8.dp,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "btn_ts"
    )
    val btnTopEnd by animateDpAsState(
        targetValue = if (isBtnPressed) 16.dp else 8.dp,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "btn_te"
    )
    val btnBottomEnd by animateDpAsState(
        targetValue = if (isBtnPressed) 28.dp else 8.dp,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "btn_be"
    )
    val btnBottomStart by animateDpAsState(
        targetValue = if (isBtnPressed) 12.dp else 8.dp,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "btn_bs"
    )
    val btnShape = RoundedCornerShape(
        topStart = btnTopStart,
        topEnd = btnTopEnd,
        bottomEnd = btnBottomEnd,
        bottomStart = btnBottomStart
    )

    val btnScale by animateFloatAsState(
        targetValue = if (isBtnPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "btn_scale_anim"
    )

    Column {
        // Drag-like target - Core center card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .graphicsLayer {
                    scaleX = cardScale
                    scaleY = cardScale
                }
                .clip(cardShape)
                .background(LightLilacCard)
                .border(
                    BorderStroke(2.dp, PastelActivePurple),
                    cardShape
                )
                .testTag("select_video_target")
                .clickable(
                    interactionSource = interactionSourceCard,
                    indication = LocalIndication.current,
                    onClick = onPickClick
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Large styled upload/play icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(TintLavender),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = "Import Video icon",
                        tint = DeepLavenderPurple,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Select VFR Recording",
                    color = DeepVioletText,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Supports MP4, MOV. Recalibrates variable frame drop timelines into rigid 60.00 FPS.",
                    color = MutedLavenderText,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Large Wide Pill "Select Video" Button in the Idle State
        Button(
            onClick = onPickClick,
            shape = btnShape,
            colors = ButtonDefaults.buttonColors(containerColor = DeepLavenderPurple),
            contentPadding = PaddingValues(),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .graphicsLayer {
                    scaleX = btnScale
                    scaleY = btnScale
                }
                .clip(btnShape)
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)), btnShape)
                .testTag("select_video_btn_bottom")
                .clickable(
                    interactionSource = interactionSourceBtn,
                    indication = LocalIndication.current,
                    onClick = onPickClick
                )
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = "Select Video Icon",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "SELECT VIDEO TO CONVERT",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Technical properties cards
        Text(
            text = "TIMELINE RESOLVER ENGINE",
            color = DeepLavenderPurple,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        FeatureCard(
            icon = Icons.Default.Info,
            title = "Strict Constant Frame Rate (CFR)",
            description = "Overwrites frame presentation timestamps to absolute 16.66ms intervals, stabilizing playback on Adobe Premiere and DaVinci Resolve."
        )

        Spacer(modifier = Modifier.height(12.dp))

        FeatureCard(
            icon = Icons.Default.VideoLibrary,
            title = "Pro-Level H.264 Encoder Profile",
            description = "Transcodes Variable Frame Rate (VFR) codecs (like HEVC or dynamic AV1) into editing-ready H.264 video with clean 1s keyframe distance."
        )

        Spacer(modifier = Modifier.height(12.dp))

        FeatureCard(
            icon = Icons.Default.FolderOpen,
            title = "Sandbox Local Save",
            description = "Muxes original lossless audio track and outputs directly to your public Movies collection under 'Movies/SmoothScrollFixer'."
        )
    }
}

@Composable
fun VideoSelectedLayout(
    selectedVideo: SelectedVideo,
    onConvertClick: () -> Unit,
    onSelectDifferent: () -> Unit
) {
    val interactionSourceConvertBtn = remember { MutableInteractionSource() }
    val isPressedConvert by interactionSourceConvertBtn.collectIsPressedAsState()

    val btnTopStart by animateDpAsState(
        targetValue = if (isPressedConvert) 32.dp else 18.dp,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "btn_conv_ts"
    )
    val btnTopEnd by animateDpAsState(
        targetValue = if (isPressedConvert) 16.dp else 18.dp,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "btn_conv_te"
    )
    val btnBottomEnd by animateDpAsState(
        targetValue = if (isPressedConvert) 28.dp else 18.dp,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "btn_conv_be"
    )
    val btnBottomStart by animateDpAsState(
        targetValue = if (isPressedConvert) 12.dp else 18.dp,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "btn_conv_bs"
    )
    val convertBtnShape = RoundedCornerShape(
        topStart = btnTopStart,
        topEnd = btnTopEnd,
        bottomEnd = btnBottomEnd,
        bottomStart = btnBottomStart
    )

    val scaleConv by animateFloatAsState(
        targetValue = if (isPressedConvert) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "btn_conv_scale"
    )

    Column {
        // Video Preview Details Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = LightLilacCard),
            border = BorderStroke(1.dp, TintLavender),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(TintLavender),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = "Video file icon",
                            tint = DeepLavenderPurple,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "IMPORT READY",
                            color = DeepLavenderPurple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = selectedVideo.name,
                            color = DeepVioletText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                HorizontalDivider(color = TintLavender, thickness = 1.dp)

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DetailMetadatum(label = "FILE SIZE", valText = selectedVideo.sizeString)
                    DetailMetadatum(label = "DURATION", valText = selectedVideo.durationString)
                    DetailMetadatum(label = "TARGET RATE", valText = "60.00 CFR")
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Conversion primary action button
        Button(
            onClick = onConvertClick,
            shape = convertBtnShape,
            colors = ButtonDefaults.buttonColors(containerColor = DeepLavenderPurple),
            contentPadding = PaddingValues(),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .graphicsLayer {
                    scaleX = scaleConv
                    scaleY = scaleConv
                }
                .clip(convertBtnShape)
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)), convertBtnShape)
                .testTag("convert_button")
                .clickable(
                    interactionSource = interactionSourceConvertBtn,
                    indication = LocalIndication.current,
                    onClick = onConvertClick
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Start converting icon",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CONVERT TO CFR 60 FPS",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Select a different file
        OutlinedButton(
            onClick = onSelectDifferent,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepLavenderPurple),
            border = BorderStroke(1.dp, TintLavender),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("select_different_button")
                .expressiveClick(onClick = onSelectDifferent)
        ) {
            Text(
                text = "Select Different Video",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = DeepLavenderPurple
            )
        }
    }
}

@Composable
fun ConvertingLayout(progress: Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // High fidelity circular container progress meter
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = progress,
                strokeWidth = 12.dp,
                color = DeepLavenderPurple,
                trackColor = TintLavender,
                modifier = Modifier.fillMaxSize()
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    color = DeepVioletText,
                    fontWeight = FontWeight.Black,
                    fontSize = 42.sp,
                )
                Text(
                    text = "RESOLVING",
                    color = DeepLavenderPurple,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Running logs status
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = LightLilacCard),
            border = BorderStroke(1.dp, TintLavender),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PROCESSING HARDWARE EXTRACTOR",
                    fontWeight = FontWeight.Bold,
                    color = DeepLavenderPurple,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when {
                        progress < 0.25f -> "Demuxing variable video and audio tracks..."
                        progress < 0.50f -> "Rebuilding presentation framework table at 60 FPS..."
                        progress < 0.80f -> "Rendering frame buffers via hardware surface..."
                        else -> "Synchronizing lossless audio track streams..."
                    },
                    color = DeepVioletText.copy(alpha = 0.85f),
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun CompletedLayout(
    videoName: String,
    outputUri: Uri?,
    onShareClick: () -> Unit,
    onDoneClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Successful circle illustration
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(RoundedCornerShape(38.dp))
                .background(TintLavender),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success checkmark icon",
                tint = SuccessTeal,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Video Calibrated!",
            color = DeepVioletText,
            fontWeight = FontWeight.Black,
            fontSize = 26.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Successfully output true Constant Frame Rate (60.00 FPS CFR) edit-ready H.264 file.",
            color = MutedLavenderText,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Saving Path Info Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = LightLilacCard),
            border = BorderStroke(1.dp, TintLavender),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Text(
                    text = "SAVED TO DEVICE",
                    color = SuccessTeal,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Movies/SmoothScrollFixer/",
                    color = DeepVioletText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "${videoName.substringBeforeLast(".")}_CFR_Fixed.mp4",
                    color = MutedLavenderText,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Share / Airdrop replacement sheet button
        Button(
            onClick = onShareClick,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DeepLavenderPurple),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .testTag("share_video_button")
                .expressiveClick(onClick = onShareClick)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share video",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SHARE FIXED VIDEO FILE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Done / conversion screen dismiss
        OutlinedButton(
            onClick = onDoneClick,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.5.dp, TintLavender),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepLavenderPurple),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("done_button")
                .expressiveClick(onClick = onDoneClick)
        ) {
            Text(
                text = "Back to Start",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = DeepLavenderPurple
            )
        }
    }
}

@Composable
fun FailedLayout(
    errorMessage: String,
    onRetryClick: () -> Unit,
    onResetClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Warning circle illustration
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(34.dp))
                .background(Color(0xFFFF3B30).copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error icon",
                tint = Color(0xFFFF3B30),
                modifier = Modifier.size(52.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Transconversion Failed",
            color = DeepVioletText,
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF6F6)),
            border = BorderStroke(1.dp, Color(0xFFFFC1C1)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = errorMessage,
                color = DeepVioletText,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp),
                lineHeight = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Try again button
        Button(
            onClick = onRetryClick,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DeepLavenderPurple),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .testTag("retry_button")
                .expressiveClick(onClick = onRetryClick)
        ) {
            Text(
                text = "TRY AGAIN",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Reset button
        OutlinedButton(
            onClick = onResetClick,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.5.dp, TintLavender),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepLavenderPurple),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("failed_reset_button")
                .expressiveClick(onClick = onResetClick)
        ) {
            Text(
                text = "Back to Selection",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = DeepLavenderPurple
            )
        }
    }
}



@Composable
fun FeatureCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LightLilacCard),
        border = BorderStroke(1.dp, TintLavender),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TintLavender),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = DeepLavenderPurple,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    color = DeepVioletText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = MutedLavenderText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun DetailMetadatum(label: String, valText: String) {
    Column {
        Text(
            text = label,
            color = MutedLavenderText,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = valText,
            color = DeepVioletText,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}

fun shareVideo(context: Context, videoUri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, videoUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share CFR Fixed Video"))
}

@Preview(showBackground = true)
@Composable
fun PreviewApp() {
    MyApplicationTheme(darkTheme = false) {
        SmoothScrollFixerApp()
    }
}
