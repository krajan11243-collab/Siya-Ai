package com.siya.ai.agent

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** Executes only explicitly routed actions. CALL/SMS-style actions require confirmation first. */
class AgentActionExecutor(private val context: Context) {
    data class Result(
        val executed: Boolean,
        val confirmationRequired: Boolean = false,
        val message: String,
    )

    fun execute(routed: RoutedIntent, confirmed: Boolean = false): Result {
        if (routed.confirmationRequired && !confirmed) {
            return Result(false, confirmationRequired = true, message = "Confirmation required before this action")
        }
        return runCatching {
            when (val action = routed.intent) {
                is AgentIntent.LaunchApp -> {
                    val launch = context.packageManager.getLaunchIntentForPackage(action.packageName)
                        ?: return Result(false, message = "App is not installed")
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launch)
                    Result(true, message = "App opened")
                }
                is AgentIntent.WebSearch -> {
                    val uri = Uri.parse("https://www.google.com/search?q=" + Uri.encode(action.query))
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    Result(true, message = "Search opened")
                }
                is AgentIntent.OpenUrl -> {
                    val uri = Uri.parse(action.url)
                    require(uri.scheme == "http" || uri.scheme == "https") { "Only web URLs are allowed" }
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    Result(true, message = "Link opened")
                }
                is AgentIntent.ShareText -> {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, action.text)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(Intent.createChooser(share, "Share with").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    Result(true, message = "Share sheet opened")
                }
                AgentIntent.MediaPlayPause -> media("android.intent.action.MEDIA_PLAY_PAUSE", "Play/pause sent")
                AgentIntent.MediaNext -> media("android.intent.action.MEDIA_NEXT", "Next track sent")
                AgentIntent.MediaPrevious -> media("android.intent.action.MEDIA_PREVIOUS", "Previous track sent")
                is AgentIntent.Call -> {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(action.numberOrName)))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    Result(true, message = "Dialer opened")
                }
                is AgentIntent.SendMessage -> {
                    val sms = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(action.recipient))).apply {
                        putExtra("sms_body", action.text)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(sms)
                    Result(true, message = "Messaging app opened")
                }
            }
        }.getOrElse { Result(false, message = it.message ?: "Action failed") }
    }

    fun openAppSettings(): Result {
        return runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            Result(true, message = "App settings opened")
        }.getOrElse { Result(false, message = it.message ?: "Could not open settings") }
    }

    private fun media(action: String, message: String): Result {
        context.sendBroadcast(Intent(action))
        return Result(true, message = message)
    }
}
