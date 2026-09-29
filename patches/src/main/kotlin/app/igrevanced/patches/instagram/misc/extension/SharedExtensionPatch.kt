package app.igrevanced.patches.instagram.misc.extension

import app.igrevanced.patches.instagram.misc.extension.hooks.applicationInitHook
import app.igrevanced.patches.shared.misc.extension.sharedExtensionPatch

val sharedExtensionPatch = sharedExtensionPatch(
    "instagram",
    applicationInitHook,
)
