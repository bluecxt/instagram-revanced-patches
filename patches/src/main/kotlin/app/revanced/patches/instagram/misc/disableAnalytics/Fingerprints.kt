package app.revanced.patches.instagram.misc.disableAnalytics

import app.revanced.patcher.composingFirstMethod
import app.revanced.patcher.gettingFirstMethodDeclaratively
import app.revanced.patcher.instructions
import app.revanced.patcher.invoke
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.strings

internal const val FACEBOOK_ANALYTICS_URL = "https://graph.facebook.com/logging_client_events"

internal val BytecodePatchContext.instagramAnalyticsUrlBuilderMethod by gettingFirstMethodDeclaratively {
    strings("/logging_client_events")
}

// Absent on newer builds where the two anchor strings no longer share a method, so the match is null there.
internal val BytecodePatchContext.facebookAnalyticsUrlInitMethodMatch by composingFirstMethod(
    "analytics_endpoint",
    FACEBOOK_ANALYTICS_URL,
) {
    instructions(FACEBOOK_ANALYTICS_URL())
}
