package app.revanced.patches.instagram.misc.disableAnalytics

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val disableAnalyticsPatch = bytecodePatch(
    name = "Disable analytics",
    description = "Disables analytics that are sent periodically.",
) {
    compatibleWith("com.instagram.android"("443.0.0.48.82"))

    apply {
        // Returns BOGUS as analytics url.
        instagramAnalyticsUrlBuilderMethod.addInstructions(
            0,
            """
                const-string v0, "BOGUS"
                return-object v0
            """
        )

        // Replaces the Facebook analytics url with BOGUS, skipped on builds where the match is null.
        facebookAnalyticsUrlInitMethodMatch.methodOrNull?.let { method ->
            val urlIndex = facebookAnalyticsUrlInitMethodMatch[0]
            val register = method.getInstruction<OneRegisterInstruction>(urlIndex).registerA
            method.replaceInstruction(urlIndex, "const-string v$register, \"BOGUS\"")
        }
    }
}
