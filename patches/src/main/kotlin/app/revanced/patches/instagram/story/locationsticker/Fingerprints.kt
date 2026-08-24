package app.revanced.patches.instagram.story.locationsticker

import app.revanced.patcher.composingFirstMethod
import app.revanced.patcher.instructions
import app.revanced.patcher.invoke
import app.revanced.patcher.patch.BytecodePatchContext

// MobileConfig boolean key that gates the redesigned location sticker styles.
// In newer builds, the original key was replaced with a new gate that decides
// whether the sticker editor exposes the full redesigned style set.
private const val LOCATION_STICKER_REDESIGN_CONFIG_KEY = 0x81053800041938L

internal val BytecodePatchContext.locationStickerRedesignGateMethodMatch by composingFirstMethod {
    instructions(LOCATION_STICKER_REDESIGN_CONFIG_KEY())
}
