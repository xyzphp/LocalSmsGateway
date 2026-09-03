package com.example.smsgateway

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import org.json.JSONObject

data class SubscriptionOption(
    val subscriptionId: Int?,
    val simSlot: Int,
    val label: String,
    val number: String?
) {
    fun toJson(): JSONObject = JSONObject()
        .put("subscriptionId", subscriptionId ?: JSONObject.NULL)
        .put("simSlot", simSlot)
        .put("label", label)
        .put("number", number ?: JSONObject.NULL)
}

object SubscriptionUtils {
    fun activeSubscriptions(context: Context): List<SubscriptionOption> {
        if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return listOf(SubscriptionOption(null, 1, "默认 SIM", null))
        }
        return try {
            val manager = context.getSystemService(SubscriptionManager::class.java)
            val infos = manager?.activeSubscriptionInfoList.orEmpty()
            if (infos.isEmpty()) {
                listOf(SubscriptionOption(null, 1, "默认 SIM", null))
            } else {
                infos.map { toOption(context, it) }.sortedBy { it.simSlot }
            }
        } catch (_: SecurityException) {
            listOf(SubscriptionOption(null, 1, "默认 SIM", null))
        } catch (_: Exception) {
            listOf(SubscriptionOption(null, 1, "默认 SIM", null))
        }
    }

    fun singleActiveSubscriptionId(context: Context): Int? =
        activeSubscriptions(context).singleOrNull()?.subscriptionId

    /**
     * Resolve the subscription carried by an incoming SMS broadcast when the
     * device exposes it. On single-SIM devices, the only active subscription
     * is a safe fallback for broadcasts that omit the extra.
     */
    fun incomingSubscriptionId(context: Context, intent: Intent): Int? {
        val subscriptionId = listOf(
            readIntExtra(intent, "subscription"),
            readIntExtra(intent, "android.telephony.extra.SUBSCRIPTION_INDEX")
        ).firstOrNull { it != null && it > 0 }
        if (subscriptionId != null) return subscriptionId

        val slotIndex = listOf(
            readIntExtra(intent, "slot"),
            readIntExtra(intent, "android.telephony.extra.SLOT_INDEX")
        ).firstOrNull { it != null && it >= 0 }
        if (slotIndex != null) {
            return activeSubscriptions(context)
                .firstOrNull { it.simSlot == slotIndex + 1 }
                ?.subscriptionId
        }

        return singleActiveSubscriptionId(context)
    }

    private fun toOption(context: Context, info: SubscriptionInfo): SubscriptionOption {
        val slot = info.simSlotIndex + 1
        val displayName = info.displayName?.toString()?.takeIf { it.isNotBlank() } ?: "SIM $slot"
        val number = phoneNumber(context, info)
        return SubscriptionOption(info.subscriptionId, slot, displayName, number)
    }

    private fun phoneNumber(context: Context, info: SubscriptionInfo): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            context.checkSelfPermission(Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED
        ) return null

        val manager = context.getSystemService(SubscriptionManager::class.java)
        val subscriptionManagerNumber = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { manager?.getPhoneNumber(info.subscriptionId) }.getOrNull()
        } else {
            null
        }
        val lineNumber = runCatching {
            context.getSystemService(TelephonyManager::class.java)
                ?.createForSubscriptionId(info.subscriptionId)
                ?.line1Number
        }.getOrNull()
        val subscriptionInfoNumber = runCatching { info.number }.getOrNull()
        return listOf(subscriptionManagerNumber, subscriptionInfoNumber, lineNumber)
            .asSequence()
            .mapNotNull { it?.trim()?.takeIf(String::isNotBlank) }
            .firstOrNull()
    }

    private fun readIntExtra(intent: Intent, key: String): Int? {
        val value = intent.extras?.get(key) ?: return null
        return when (value) {
            is Int -> value
            is Long -> value.toInt()
            is Short -> value.toInt()
            is Byte -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }
    }
}
