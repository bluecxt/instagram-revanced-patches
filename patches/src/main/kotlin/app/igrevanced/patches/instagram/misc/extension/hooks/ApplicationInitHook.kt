package app.igrevanced.patches.instagram.misc.extension.hooks

import app.igrevanced.patches.shared.misc.extension.activityOnCreateExtensionHook

internal val applicationInitHook = activityOnCreateExtensionHook(
    "/InstagramAppShell;"
)
