package app.revanced.patches.meta.ads

import app.revanced.patcher.gettingFirstMethodDeclaratively
import app.revanced.patcher.gettingFirstMethodDeclarativelyOrNull
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.returnType

internal val BytecodePatchContext.adInjectorMethod by gettingFirstMethodDeclaratively(
    "onInjectionOpportunity",
) {
    returnType("V")
}

internal const val STORY_AD_INSERTED_LOG = "Inserted ad/netego at position %d"

internal val BytecodePatchContext.storyAdInsertionMethodOrNull by gettingFirstMethodDeclarativelyOrNull(
    STORY_AD_INSERTED_LOG,
) {
    returnType("Ljava/lang/Integer;")
}
