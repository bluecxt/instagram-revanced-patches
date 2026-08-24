package app.revanced.patches.instagram.ads

import app.revanced.patcher.gettingFirstMethodDeclarativelyOrNull
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.returnType

internal val BytecodePatchContext.isAdPodAdInjectorMethodOrNull by
    gettingFirstMethodDeclarativelyOrNull("Is ad pod") {
        returnType("Z")
    }

internal val BytecodePatchContext.insertItemAdInjectorMethodOrNull by
    gettingFirstMethodDeclarativelyOrNull("SponsoredContentController.insertItem") {
        returnType("Z")
    }
