package app.revanced.patches.instagram.misc.removeBuildExpiredPopup

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.util.returnEarly

@Suppress("unused")
val removeBuildExpiredPopupPatch = bytecodePatch(
    name = "Remove build expired popup",
    description = "Removes the popup that appears after a while, when the app version ages.",
) {
    compatibleWith("com.instagram.android"("443.0.0.48.82"))

    apply {
        // The whole method body is lockout logic, it returns void, and it is only reached from
        // InstagramMainActivity during startup, so suppressing it simply means the lockout check
        // never runs and the expired build screen is never shown.
        lockoutScreenLauncherMethod.returnEarly()
    }
}
