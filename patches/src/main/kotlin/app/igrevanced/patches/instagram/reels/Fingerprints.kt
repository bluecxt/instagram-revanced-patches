package app.igrevanced.patches.instagram.reels

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.igrevanced.util.getting
import app.igrevanced.util.literal
import app.igrevanced.util.using

internal val BytecodePatchContext.clipsViewPagerImplGetViewAtIndexMethod by gettingFirstMethodDeclaratively("ClipsViewPagerImpl_getViewAtIndex")

internal val BytecodePatchContext.clipsSwipeRefreshLayoutOnInterceptTouchEventMethod by gettingFirstMethodDeclaratively {
    parameterTypes("Landroid/view/MotionEvent;")
    definingClass("Linstagram/features/clips/viewer/ui/ClipsSwipeRefreshLayout;")
}

internal val BytecodePatchContext.clipsSwipeDirectionControllerResetMethod by gettingFirstMethodDeclaratively {
    parameterTypes("Z")
    returnType("V")
    instructions(method("setUserInputEnabled"))
}

internal val BytecodePatchContext.clipsViewPagerImplReEnableScrollingMethod by getting {
    firstMethodDeclaratively {
        parameterTypes()
        returnType("V")
        instructions(
            1L(),
            method("setUserInputEnabled")
        )
    }
} using { clipsViewPagerImplGetViewAtIndexMethod }
