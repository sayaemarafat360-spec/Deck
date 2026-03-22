package com.sayaem.nebula.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.material3.*
import com.sayaem.nebula.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun DeckSplashScreen(onFinished: () -> Unit) {

    // Phase 0→1→2→3: rings expand → logo appears → text fades in → exit
    var phase by remember { mutableStateOf(0) }

    val ringScale1 by animateFloatAsState(
        if (phase >= 1) 3.5f else 0f,
        animationSpec = tween(900, easing = EaseOutCubic), label = "r1"
    )
    val ringScale2 by animateFloatAsState(
        if (phase >= 1) 2.5f else 0f,
        animationSpec = tween(750, 120, EaseOutCubic), label = "r2"
    )
    val ringScale3 by animateFloatAsState(
        if (phase >= 1) 1.8f else 0f,
        animationSpec = tween(600, 240, EaseOutCubic), label = "r3"
    )
    val logoScale by animateFloatAsState(
        if (phase >= 2) 1f else 0.3f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "logo"
    )
    val logoAlpha by animateFloatAsState(
        if (phase >= 2) 1f else 0f,
        animationSpec = tween(400), label = "logoA"
    )
    val textAlpha by animateFloatAsState(
        if (phase >= 3) 1f else 0f,
        animationSpec = tween(500), label = "textA"
    )
    val exitAlpha by animateFloatAsState(
        if (phase >= 4) 0f else 1f,
        animationSpec = tween(400), label = "exit"
    )

    // Pulsing animation on the center disc
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        0.92f, 1.0f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "pulse"
    )

    // Rotating gradient ring
    val rotation by infiniteTransition.animateFloat(
        0f, 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "rot"
    )

    LaunchedEffect(Unit) {
        delay(100); phase = 1   // rings expand
        delay(400); phase = 2   // logo pops in
        delay(300); phase = 3   // text fades in
        delay(900); phase = 4   // fade out
        delay(450)
        onFinished()
    }

    Box(
        Modifier.fillMaxSize()
            .background(Brush.radialGradient(
                listOf(Color(0xFF0D0B1E), Color(0xFF050510))
            ))
            .alpha(exitAlpha),
        contentAlignment = Alignment.Center
    ) {
        // Expanding rings
        listOf(ringScale1 to 0.07f, ringScale2 to 0.12f, ringScale3 to 0.18f)
            .forEach { (scale, alpha) ->
                Box(
                    Modifier.size(200.dp).scale(scale)
                        .clip(CircleShape)
                        .border(1.5.dp, NebulaViolet.copy(alpha), CircleShape)
                )
            }

        // Rotating gradient arc
        androidx.compose.foundation.Canvas(
            Modifier.size(220.dp).rotate(rotation)
        ) {
            drawArc(
                brush = Brush.sweepGradient(listOf(
                    NebulaViolet.copy(0f),
                    NebulaViolet.copy(0.8f),
                    NebulaPink.copy(0.6f),
                    NebulaViolet.copy(0f),
                )),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 3f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
        }

        // Center disc with D logo
        Box(
            Modifier
                .size(110.dp)
                .scale(logoScale * pulse)
                .alpha(logoAlpha)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(NebulaViolet, NebulaPink.copy(0.8f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Inner glow ring
            Box(
                Modifier.size(96.dp).clip(CircleShape)
                    .border(1.dp, Color.White.copy(0.15f), CircleShape)
            )
            Text(
                "D",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        // App name below
        Column(
            Modifier.offset(y = 90.dp).alpha(textAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Deck",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Your music universe",
                style = MaterialTheme.typography.labelMedium,
                color = NebulaViolet.copy(0.8f),
                letterSpacing = 1.5.sp
            )
        }
    }
}
