package app.revanced.patches.instagram.interaction.disableswipenavigation

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal const val SWIPE_NAVIGATION_CONTAINER_CLASS_DESCRIPTOR =
    "Lcom/instagram/ui/swipenavigation/container/SwipeNavigationContainer;"
internal const val POSITION_CONFIG_CLASS_DESCRIPTOR =
    "Lcom/instagram/ui/swipenavigation/container/PositionConfig;"

// onInterceptTouchEvent; the match captures the setUserInputEnabled call to get its index.
internal val BytecodePatchContext.onInterceptTouchEventMethodMatch by composingFirstMethod {
    definingClass(SWIPE_NAVIGATION_CONTAINER_CLASS_DESCRIPTOR)
    name("onInterceptTouchEvent")
    instructions(
        allOf(
            Opcode.INVOKE_VIRTUAL(),
            method { name == "setUserInputEnabled" },
        ),
    )
}

// Applies a new position to the container's edge-panel spring.
internal val BytecodePatchContext.setInternalPositionMethodMatch by composingFirstMethod {
    definingClass(SWIPE_NAVIGATION_CONTAINER_CLASS_DESCRIPTOR)
    name("setInternalPosition")
    instructions(field { definingClass == POSITION_CONFIG_CLASS_DESCRIPTOR && type == "Ljava/lang/String;" })
}

// Settles the edge swipe on release, feeding the fling velocity (2nd parameter) into the spring.
internal val BytecodePatchContext.swipeSettleMethod by gettingFirstMethodDeclaratively {
    definingClass(SWIPE_NAVIGATION_CONTAINER_CLASS_DESCRIPTOR)
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("Landroid/view/MotionEvent;", "F", "J")
}
