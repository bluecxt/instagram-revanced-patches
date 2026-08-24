package app.revanced.patches.instagram.interaction.disableswipenavigation

import app.revanced.patcher.extensions.ExternalLabel
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.fieldReference
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.util.addInstructionsAtControlFlowLabel
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

@Suppress("unused")
val disableSwipeNavigationPatch = bytecodePatch(
    name = "Disable swipe navigation",
    description = "Disables swiping between the main navigation tabs and swiping to the camera. " +
        "Tapping the tabs still works.",
    use = false,
) {
    compatibleWith("com.instagram.android"("443.0.0.48.82"))

    apply {
        // The container re-asserts the nav pager's setUserInputEnabled on every touch, force it false.
        onInterceptTouchEventMethodMatch.let {
            // invoke-virtual { receiver, flag } — flag is the second (D) register.
            val flagRegister = it.method.getInstruction<FiveRegisterInstruction>(it[0]).registerD
            // The call is a branch target, relocate incoming labels onto the injection to cover every path.
            it.method.addInstructionsAtControlFlowLabel(it[0], "const/4 v$flagRegister, 0x0")
        }

        // Gesture-source string field, taken by reference from its use so no obfuscated name is hardcoded.
        val positionSourceField = setInternalPositionMethodMatch.let {
            it.method.getInstruction(it[0]).fieldReference!!
        }
        setInternalPositionMethodMatch.method.addInstructionsWithLabels(
            0,
            """
                move-object/from16 v0, p1
                iget-object v0, v0, $positionSourceField
                const-string v1, "tap_partially_visible_panel"
                invoke-virtual { v1, v0 }, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v1
                if-nez v1, :ig_swipe_block
                const-string v1, "swipe"
                invoke-virtual { v1, v0 }, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v1
                if-eqz v1, :ig_swipe_continue
                :ig_swipe_block
                return-void
            """,
            ExternalLabel("ig_swipe_continue", setInternalPositionMethodMatch.method.getInstruction(0)),
        )

        // A hard fling feeds velocity (p2) straight into the spring, bypassing the block above, zero it.
        swipeSettleMethod.addInstructions(
            0,
            """
                const/4 v0, 0x0
                move/from16 p2, v0
            """,
        )
    }
}
