package com.rainan.quickcts

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import org.lsposed.hiddenapibypass.HiddenApiBypass

/**
 * Tiện ích kích hoạt Circle to Search (CTS / Google Omni) thông qua IPC Service của Android.
 * Thiết kế độc lập (clean-room), tương thích với nhiều giao diện OEM (HyperOS, Pixel, One UI, ColorOS...).
 */
object CtsTriggerHelper {
    private const val TAG = "QuickCTS"

    // Định danh AIDL Service trong AOSP
    private const val SERVICE_VOICE_INTERACTION = "voiceinteraction"
    private const val CLASS_SERVICE_INTERFACE = "com.android.internal.app.IVoiceInteractionManagerService"
    private const val CLASS_SERVICE_STUB = "com.android.internal.app.IVoiceInteractionManagerService\$Stub"
    private const val METHOD_SHOW_SESSION = "showSessionFromSession"

    // Hằng số tham số cho phiên tìm kiếm ngữ cảnh của Google Omni
    private const val KEY_INVOCATION_TIME = "invocation_time_ms"
    private const val KEY_ENTRY_POINT = "omni.entry_point"
    private const val ENTRY_POINT_GESTURE = 1
    private const val SHOW_SOURCE_ASSIST_GESTURE = 7
    private const val ATTRIBUTION_HYPEROS = "hyperOS_home"

    /**
     * Kích hoạt phiên Circle to Search.
     *
     * @param context Context của ứng dụng
     * @param vibrate Có kích hoạt phản hồi xúc giác nhẹ khi thành công hay không
     * @return true nếu lệnh gọi IPC được gửi thành công, ngược lại false
     */
    fun triggerCircleToSearch(context: Context, vibrate: Boolean = true): Boolean {
        val (serviceClass, serviceInstance) = resolveServiceBinder() ?: run {
            Log.e(TAG, "Không thể kết nối tới VoiceInteractionManagerService")
            return false
        }

        val sessionArgs = createSessionArguments()
        val success = invokeSessionDisplay(serviceClass, serviceInstance, sessionArgs)

        if (success && vibrate) {
            triggerHapticFeedback(context)
        }

        return success
    }

    /**
     * Khởi tạo Bundle tham số chuẩn của Google Omni.
     */
    private fun createSessionArguments(): Bundle {
        return Bundle().apply {
            putLong(KEY_INVOCATION_TIME, SystemClock.elapsedRealtime())
            putInt(KEY_ENTRY_POINT, ENTRY_POINT_GESTURE)
        }
    }

    /**
     * Tìm kiếm và chuyển đổi Service Binder thông qua ServiceManager ẩn của AOSP.
     */
    @SuppressLint("PrivateApi")
    private fun resolveServiceBinder(): Pair<Class<*>, Any>? {
        return runCatching {
            val serviceManagerClass = Class.forName("android.os.ServiceManager")
            val getServiceMethod = serviceManagerClass.getMethod("getService", String::class.java)
            val rawBinder = getServiceMethod.invoke(null, SERVICE_VOICE_INTERACTION) as? IBinder
                ?: return null

            val stubClass = Class.forName(CLASS_SERVICE_STUB)
            val asInterfaceMethod = stubClass.getMethod("asInterface", IBinder::class.java)
            val serviceInstance = asInterfaceMethod.invoke(null, rawBinder) ?: return null

            val serviceInterfaceClass = Class.forName(CLASS_SERVICE_INTERFACE)
            serviceInterfaceClass to serviceInstance
        }.onFailure { ex ->
            Log.e(TAG, "Lỗi phân giải Binder service: ${ex.message}", ex)
        }.getOrNull()
    }

    /**
     * Gọi phương thức showSessionFromSession qua HiddenApiBypass để vượt rào API ẩn.
     */
    private fun invokeSessionDisplay(
        serviceClass: Class<*>,
        serviceInstance: Any,
        args: Bundle
    ): Boolean {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Trên Android 14+ (API 34+), hệ thống hỗ trợ thêm tham số attributionTag.
                // Thử trước với attributionTag của HyperOS, nếu không tương thích sẽ gọi với tag mặc định.
                val hyperOsTriggered = runCatching {
                    HiddenApiBypass.invoke(
                        serviceClass,
                        serviceInstance,
                        METHOD_SHOW_SESSION,
                        null,
                        args,
                        SHOW_SOURCE_ASSIST_GESTURE,
                        ATTRIBUTION_HYPEROS
                    ) as? Boolean ?: false
                }.getOrDefault(false)

                if (hyperOsTriggered) {
                    true
                } else {
                    HiddenApiBypass.invoke(
                        serviceClass,
                        serviceInstance,
                        METHOD_SHOW_SESSION,
                        null,
                        args,
                        SHOW_SOURCE_ASSIST_GESTURE,
                        null
                    ) as? Boolean ?: false
                }
            } else {
                // Android 13 trở xuống
                HiddenApiBypass.invoke(
                    serviceClass,
                    serviceInstance,
                    METHOD_SHOW_SESSION,
                    null,
                    args,
                    SHOW_SOURCE_ASSIST_GESTURE
                ) as? Boolean ?: false
            }
        }.onFailure { ex ->
            Log.e(TAG, "Lỗi kích hoạt showSessionFromSession: ${ex.message}", ex)
        }.getOrDefault(false)
    }

    /**
     * Tạo phản hồi rung ngắn (click haptic) khi kích hoạt thành công.
     */
    private fun triggerHapticFeedback(context: Context) {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator?.let { vib ->
                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .build()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vib.vibrate(
                        VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK),
                        attributes
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(longArrayOf(0, 20), -1, attributes)
                }
            }
        }.onFailure { ex ->
            Log.w(TAG, "Không thể phát phản hồi rung: ${ex.message}")
        }
    }
}
