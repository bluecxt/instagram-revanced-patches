package app.revanced.patches.instagram.misc.removeBuildExpiredPopup

import app.revanced.patcher.gettingFirstMethodDeclaratively
import app.revanced.patcher.instructions
import app.revanced.patcher.method
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.returnType

/*
 * This previously anchored on a method holding both "android.hardware.sensor.hinge_angle" and the
 * milliseconds-in-a-day literal, and then zeroed the computed build age. That was an incidental
 * co-occurrence inside a device capability helper, and it no longer holds: as of 442.0.0.46.79 the
 * hinge angle string has moved into a pooled string getter, and none of the four methods which
 * still reference it contain the day literal, so the fingerprint resolves to nothing.
 *
 * The lockout screen ("com.instagram.release.lockout.expired_build") is instead launched from a
 * single method on the lockout manager, reached only from InstagramMainActivity during startup. It
 * reads the "lockout_active" preference and, when set, shows the lockout fragment.
 *
 * Anchoring on that preference name is durable: it is a semantic, unobfuscated constant, and only
 * two methods in the app reference it -- this launcher and the setter which writes it. Requiring a
 * getBoolean call excludes the setter without depending on parameter shape, which is obfuscated.
 */
internal val BytecodePatchContext.lockoutScreenLauncherMethod by gettingFirstMethodDeclaratively(
    "lockout_active",
) {
    returnType("V")
    instructions(method("getBoolean"))
}
