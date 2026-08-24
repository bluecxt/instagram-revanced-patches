package app.revanced.patches.instagram.misc.screenshot

import app.revanced.patcher.extensions.removeInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.transformation.transformInstructionsPatch
import app.revanced.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil

private val registerScreenCaptureCallbackMethodReference = ImmutableMethodReference(
    "Landroid/app/Activity;",
    "registerScreenCaptureCallback",
    listOf(
        "Ljava/util/concurrent/Executor;",
        "Landroid/app/Activity\$ScreenCaptureCallback;",
    ),
    "V",
)

private val unregisterScreenCaptureCallbackMethodReference = ImmutableMethodReference(
    "Landroid/app/Activity;",
    "unregisterScreenCaptureCallback",
    listOf(
        "Landroid/app/Activity\$ScreenCaptureCallback;",
    ),
    "V",
)

@Suppress("unused")
val preventScreenshotDetectionPatch = bytecodePatch(
    name = "Prevent screenshot detection",
    description = "Removes the registration of screen capture callbacks, preventing Instagram from detecting screenshots or notifying the sender.",
    use = true,
) {
    compatibleWith("com.instagram.android"("443.0.0.48.82"))

    dependsOn(
        transformInstructionsPatch(
            filterMap = { _, _, instruction, instructionIndex ->
                if (instruction.opcode != Opcode.INVOKE_VIRTUAL) return@transformInstructionsPatch null

                val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: return@transformInstructionsPatch null

                instructionIndex.takeIf {
                    MethodUtil.methodSignaturesMatch(reference, registerScreenCaptureCallbackMethodReference) ||
                        MethodUtil.methodSignaturesMatch(reference, unregisterScreenCaptureCallbackMethodReference)
                }
            },
            transform = { mutableMethod, instructionIndex ->
                mutableMethod.removeInstruction(instructionIndex)
            },
        ),
    )
}
