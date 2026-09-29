package app.igrevanced.patches.instagram.reels.autoscroll

import app.revanced.patcher.gettingFirstMethodDeclaratively
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.returnType

/**
 * Matches the feature availability gate that determines
 * whether auto-scroll should be available for Reels.
 */
// The patch forces this to return false, so it must be a boolean method: "auto_scroll" alone
// also matches unrelated methods, and returnEarly() on an object-returning one returns null.
internal val BytecodePatchContext.clipsAutoScrollFeatureCheckMethod by gettingFirstMethodDeclaratively("auto_scroll") {
    returnType("Z")
}

/**
 * Matches the toggle handler called when the user taps
 * the auto-scroll button. Contains analytics logging strings.
 */
internal val BytecodePatchContext.clipsAutoScrollToggleMethod by gettingFirstMethodDeclaratively(
    "clips_viewer_autoscroll",
) {
    returnType("V")
}
