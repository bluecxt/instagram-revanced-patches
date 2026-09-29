package app.igrevanced.patches.instagram.ghost.story

import app.revanced.patcher.gettingFirstMethodDeclaratively
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.returnType

// Only a void method can be skipped safely: returnEarly() on an object-returning match would hand
// the caller null.
internal val BytecodePatchContext.setMediaSeenMethod by gettingFirstMethodDeclaratively("visual_media_seen") {
    returnType("V")
}
