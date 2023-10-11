package me.rhunk.snapenhance.core.features.impl.tweaks

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraCharacteristics.Key
import android.hardware.camera2.CameraManager
import android.util.Range
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.features.FeatureLoadParams
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.core.util.hook.hookConstructor
import me.rhunk.snapenhance.core.util.ktx.setObjectField
import me.rhunk.snapenhance.core.wrapper.impl.ScSize

class CameraTweaks : Feature("Camera Tweaks", loadParams = FeatureLoadParams.ACTIVITY_CREATE_SYNC) {

    private fun parseResolution(resolution: String): IntArray {
        return resolution.split("x").map { it.toInt() }.toIntArray()
    }

    @SuppressLint("MissingPermission", "DiscouragedApi")
    override fun onActivityCreate() {
        if (context.config.camera.disable.get()) {
            ContextWrapper::class.java.hook("checkPermission", HookStage.BEFORE) { param ->
                val permission = param.arg<String>(0)
                if (permission == Manifest.permission.CAMERA) {
                    param.setResult(PackageManager.PERMISSION_GRANTED)
                }
            }

            CameraManager::class.java.hook("openCamera", HookStage.BEFORE) { param ->
                param.setResult(null)
            }
        }

        val previewResolutionConfig = context.config.camera.overridePreviewResolution.getNullable()?.let { parseResolution(it) }
        val captureResolutionConfig = context.config.camera.overridePictureResolution.getNullable()?.let { parseResolution(it) }

        context.config.camera.customFrameRate.getNullable()?.also { value ->
            val customFrameRate = value.toInt()
            CameraCharacteristics::class.java.hook("get", HookStage.AFTER)  { param ->
                val key = param.arg<Key<*>>(0)
                if (key == CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES) {
                    val fpsRanges = param.getResult() as? Array<*> ?: return@hook
                    fpsRanges.forEach {
                        val range = it as? Range<*> ?: return@forEach
                        range.setObjectField("mUpper", customFrameRate)
                        range.setObjectField("mLower", customFrameRate)
                    }
                }
            }
        }

        context.mappings.getMappedClass("ScCameraSettings").hookConstructor(HookStage.BEFORE) { param ->
            val previewResolution = ScSize(param.argNullable(2))
            val captureResolution = ScSize(param.argNullable(3))

            if (previewResolution.isPresent() && captureResolution.isPresent()) {
                previewResolutionConfig?.let {
                    previewResolution.first = it[0]
                    previewResolution.second = it[1]
                }

                captureResolutionConfig?.let {
                    captureResolution.first = it[0]
                    captureResolution.second = it[1]
                }
            }
        }
    }
}