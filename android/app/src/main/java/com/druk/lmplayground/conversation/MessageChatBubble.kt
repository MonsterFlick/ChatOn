package com.druk.lmplayground.conversation

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.druk.lmplayground.R
import com.druk.lmplayground.theme.ChatOnAssistantBorder
import com.druk.lmplayground.theme.ChatOnAssistantBubble

@Composable
fun ChatItemBubble(
    message: Message,
    showActions: Boolean = true,
    onTokenCountClicked: (() -> Unit)? = null,
    onRegenerate: (() -> Unit)? = null,
    onBranch: (() -> Unit)? = null,
) {
    var showRatingSheet by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val hasToolCalls = !message.toolCalls.isNullOrEmpty()
    val isWaitingForResponse = !showActions
        && message.content.isEmpty()
        && !hasToolCalls
    val isGenerating = !showActions

    val isDark = isSystemInDarkTheme()
    val assistantCardColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f) else ChatOnAssistantBubble
    val assistantBorderColor = if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.25f) else ChatOnAssistantBorder

    Column {
        // Redesigned ChatOn Assistant Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = assistantCardColor,
            border = BorderStroke(1.dp, assistantBorderColor),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                // Header badge: AI Sparkle Avatar + "ChatOn AI"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(22.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ChatOn AI",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (isWaitingForResponse) {
                    ThinkingIndicator()
                } else {
                    // Tool rounds
                    if (hasToolCalls) {
                        val thinkingText = stringResource(R.string.thinking)
                        val inputLabel = stringResource(R.string.tool_call_input)
                        val outputLabel = stringResource(R.string.tool_call_output)
                        for (toolCall in message.toolCalls!!) {
                            if (toolCall.precedingThinking.isNotEmpty()) {
                                val pre = remember(toolCall.precedingThinking) {
                                    splitThinking(toolCall.precedingThinking)
                                }
                                if (pre.thinkingContent.isNotEmpty()) {
                                    CollapsibleSection(
                                        label = buildString {
                                            append("$thinkingText \u00B7 ${formatDuration(toolCall.precedingThinkingDurationSeconds)}")
                                            if (toolCall.precedingThinkingTokens > 0) {
                                                append(" \u00B7 ${toolCall.precedingThinkingTokens} tokens")
                                            }
                                        },
                                        content = pre.thinkingContent
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                            val durationSec = (toolCall.durationMs / 1000).toInt().coerceAtLeast(
                                if (toolCall.durationMs > 0) 1 else 0
                            )
                            CollapsibleSection(
                                label = "${toolDisplayName(toolCall.name)} \u00B7 ${formatDuration(durationSec)}",
                                content = buildString {
                                    if (toolCall.arguments.isNotBlank()) {
                                        append(inputLabel).append('\n')
                                        append(prettyJson(toolCall.arguments))
                                        append("\n\n")
                                    }
                                    append(outputLabel).append('\n')
                                    append(prettyJson(toolCall.result))
                                }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }

                    // Thinking indicator if still generating
                    if (isGenerating && hasToolCalls && message.content.isEmpty()) {
                        ThinkingIndicator()
                    }

                    // Post-tool thinking + response
                    val split = remember(message.content) { splitThinking(message.content) }
                    val hasThinking = split.thinkingContent.isNotEmpty()

                    if (hasThinking) {
                        val thinkingText = stringResource(R.string.thinking)
                        CollapsibleSection(
                            label = buildString {
                                append("$thinkingText \u00B7 ${formatDuration(message.thinkingDurationSeconds)}")
                                if (message.thinkingTokens > 0) {
                                    append(" \u00B7 ${message.thinkingTokens} tokens")
                                }
                            },
                            content = split.thinkingContent
                        )
                        if (split.responseContent.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    if (split.responseContent.isNotEmpty()) {
                        val styledMessage = messageFormatter(
                            text = split.responseContent,
                            primary = false
                        )
                        SelectionContainer {
                            Text(
                                text = styledMessage,
                                style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
                            )
                        }
                    }
                }

                message.image?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Image(
                        painter = painterResource(it),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(160.dp),
                        contentDescription = stringResource(id = R.string.attached_image)
                    )
                }

                message.imageUri?.let { uri ->
                    Spacer(modifier = Modifier.height(6.dp))
                    coil.compose.AsyncImage(
                        model = uri,
                        contentDescription = stringResource(id = R.string.attached_image),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(160.dp)
                    )
                }
            }
        }

        // Action Toolbar Below Assistant Card
        if (showActions) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val shareLabel = stringResource(id = R.string.share)
                IconButton(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, stripThinkTags(message.content))
                        }
                        context.startActivity(Intent.createChooser(sendIntent, shareLabel))
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = shareLabel,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(stripThinkTags(message.content)))
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = stringResource(id = R.string.copy),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(
                    onClick = { /* upvote - no-op */ },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ThumbUp,
                        contentDescription = stringResource(id = R.string.upvote),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(
                    onClick = { showRatingSheet = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ThumbDown,
                        contentDescription = stringResource(id = R.string.downvote),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                if (onRegenerate != null || onBranch != null) {
                    var menuOpen by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = stringResource(id = R.string.more_options),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            if (onRegenerate != null) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.regenerate)) },
                                    leadingIcon = { Icon(Icons.Outlined.Refresh, contentDescription = null) },
                                    onClick = {
                                        menuOpen = false
                                        onRegenerate()
                                    }
                                )
                            }
                            if (onBranch != null) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.branch_from_here)) },
                                    leadingIcon = {
                                        Icon(Icons.AutoMirrored.Outlined.CallSplit, contentDescription = null)
                                    },
                                    onClick = {
                                        menuOpen = false
                                        onBranch()
                                    }
                                )
                            }
                        }
                    }
                }
                if (message.responseTokens + message.thinkingTokens > 0) {
                    Text(
                        text = formatResponseStats(message),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .then(
                                if (onTokenCountClicked != null) {
                                    Modifier.clickable(onClick = onTokenCountClicked)
                                } else {
                                    Modifier
                                }
                            )
                    )
                }
            }
        }
    }

    if (showRatingSheet) {
        RatingBottomSheet(onDismiss = { showRatingSheet = false })
    }
}

/**
 * Reusable collapsible section with arrow icon, label, and expandable content.
 * Styled with soft rounded pill surface in ChatOn White & Blue theme.
 */
@Composable
private fun CollapsibleSection(
    label: String,
    content: String
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = if (expanded) stringResource(R.string.collapse_thinking) else stringResource(R.string.expand_thinking),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 6.dp, bottom = 4.dp, start = 4.dp)) {
                    SelectionContainer {
                        Text(
                            text = content,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    return if (seconds >= 60) {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        if (remainingSeconds > 0) "${minutes}m ${remainingSeconds}s" else "${minutes}m"
    } else {
        "${seconds}s"
    }
}

private fun formatResponseStats(message: Message): String {
    val totalTokens = message.responseTokens + message.thinkingTokens
    val speed = if (message.responseDurationSeconds > 0f) {
        " \u00B7 %.1f t/s".format(message.responseTokens / message.responseDurationSeconds)
    } else {
        ""
    }
    return "$totalTokens tokens$speed"
}

/** Friendly display name for a tool (falls back to the raw name for unknowns). */
@Composable
private fun toolDisplayName(name: String): String = when (name) {
    "run_javascript" -> stringResource(R.string.tool_run_javascript_title)
    "web_search" -> stringResource(R.string.tool_web_search_title)
    "web_fetch" -> stringResource(R.string.tool_web_fetch_title)
    else -> name
}

/** Pretty-print a JSON object/array for the tool input/output view; raw on failure. */
private fun prettyJson(raw: String): String {
    val trimmed = raw.trim()
    return try {
        when {
            trimmed.startsWith("{") -> org.json.JSONObject(trimmed).toString(2)
            trimmed.startsWith("[") -> org.json.JSONArray(trimmed).toString(2)
            else -> raw
        }
    } catch (_: Exception) {
        raw
    }
}

/**
 * Animated Wave Thinking Indicator with pulsating royal/electric blue dots.
 */
@Composable
private fun ThinkingIndicator() {
    val transition = rememberInfiniteTransition(label = "thinking_dots_wave")
    val animProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "animProgress"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        val thinkingText = stringResource(R.string.thinking)
        Text(
            text = thinkingText,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.primary,
            fontStyle = FontStyle.Italic
        )
        // 3 Animated wave pulsating dots
        for (i in 0..2) {
            val offset = (animProgress + i * 0.33f) % 1f
            val wave = kotlin.math.sin(offset * Math.PI).toFloat().coerceIn(0f, 1f)
            val scale = 0.7f + 0.5f * wave
            val alpha = 0.35f + 0.65f * wave
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            )
        }
    }
}
