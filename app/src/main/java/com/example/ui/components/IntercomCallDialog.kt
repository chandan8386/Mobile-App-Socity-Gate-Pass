package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.IntercomActiveCall
import com.example.data.model.IntercomCallState
import com.example.ui.UserRole
import kotlinx.coroutines.delay

@Composable
fun IntercomCallDialog(
    call: IntercomActiveCall,
    currentRole: UserRole,
    onAnswer: () -> Unit,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onQuickDecision: (decision: String) -> Unit
) {
    // Call duration timer
    var elapsedSeconds by remember { mutableStateOf(0) }

    LaunchedEffect(call.callState) {
        if (call.callState == IntercomCallState.CONNECTED) {
            while (true) {
                delay(1000L)
                elapsedSeconds++
            }
        }
    }

    // Auto-connect simulation for dialing experience after 2.5 seconds if ringing
    LaunchedEffect(call.callState) {
        if (call.callState == IntercomCallState.RINGING) {
            delay(2500L)
            onAnswer()
        }
    }

    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val durationText = String.format("%02d:%02d", minutes, seconds)

    // Pulse animation for ringing and audio waveform
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Dialog(
        onDismissRequest = onEndCall,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
                .testTag("intercom_call_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (call.callState == IntercomCallState.CONNECTED) Color(0xFF16A34A)
                                    else Color(0xFFD97706)
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (call.callState == IntercomCallState.CONNECTED) "INTERCOM CALL ACTIVE"
                            else "CONNECTING FLAT EXTENSION...",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Intercom Target Avatar with Pulse
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(110.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size((90 * pulseScale).dp)
                            .clip(CircleShape)
                            .background(
                                if (call.callState == IntercomCallState.CONNECTED)
                                    Color(0xFF22C55E).copy(alpha = 0.2f)
                                else
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(MaterialTheme.colorScheme.primary, Color(0xFF1D4ED8))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (call.callState == IntercomCallState.CONNECTED) Icons.Default.PhoneInTalk else Icons.Default.RingVolume,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Callee / Caller details
                Text(
                    text = "Flat ${call.targetFlat} • ${call.targetResidentName}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (call.callState == IntercomCallState.CONNECTED) durationText else "Ringing Intercom Extension...",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (call.callState == IntercomCallState.CONNECTED) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Visitor Context Card (Why the guard is calling)
                if (!call.visitorName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Visitor Verification at Gate:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${call.visitorName} (${call.visitorType ?: "Guest"})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (!call.visitorVehicle.isNullOrBlank() && call.visitorVehicle != "None") {
                                Text(
                                    text = "Vehicle: ${call.visitorVehicle}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Audio Waveform Visualizer (Connected state)
                if (call.callState == IntercomCallState.CONNECTED) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val heights = listOf(14.dp, 28.dp, 38.dp, 22.dp, 34.dp, 18.dp, 26.dp)
                        heights.forEachIndexed { i, h ->
                            val dynamicHeight = (h.value * if (i % 2 == 0) pulseScale else (2.1f - pulseScale)).dp
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(dynamicHeight.coerceIn(8.dp, 40.dp))
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFF16A34A))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Quick verification decisions directly during call
                if (call.callState == IntercomCallState.CONNECTED) {
                    Text(
                        text = "Quick Verification Actions:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onQuickDecision("APPROVED") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_intercom_allow")
                        ) {
                            Text("Allow In", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = { onQuickDecision("PARCEL_AT_GATE") },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_intercom_parcel")
                        ) {
                            Text("Keep at Gate", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        }

                        Button(
                            onClick = { onQuickDecision("DENIED") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_intercom_deny")
                        ) {
                            Text("Deny", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                }

                // In-Call Audio Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute Button
                    IconButton(
                        onClick = onToggleMute,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (call.isMuted) Color(0xFFDC2626) else MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("btn_intercom_mute")
                    ) {
                        Icon(
                            imageVector = if (call.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute",
                            tint = if (call.isMuted) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // End Call Button (Big Red)
                    IconButton(
                        onClick = onEndCall,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                            .testTag("btn_intercom_end_call")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Intercom Call",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Speaker Button
                    IconButton(
                        onClick = onToggleSpeaker,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (call.isSpeakerOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("btn_intercom_speaker")
                    ) {
                        Icon(
                            imageVector = if (call.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                            contentDescription = "Speaker",
                            tint = if (call.isSpeakerOn) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
