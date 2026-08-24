package app.revanced.patches.instagram.hide.navigation

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext

/*
 * The button name constants on their own match five methods as of 442.0.0.46.79: the enum's own
 * <clinit> plus four unrelated methods which merely mention the names. Resolving the wrong one
 * silently yields the wrong enum name field, so restrict the match to <clinit>, where enum
 * constants are always built.
 *
 * The method which builds the navigation button list is deliberately not fingerprinted here. Its
 * (UserSession, boolean) -> List shape is not unique, and the only durable discriminator is a
 * reference to the enum class resolved below, so that selection lives in the patch instead.
 */
internal val BytecodePatchContext.navigationButtonsEnumMethod by gettingFirstImmutableMethodDeclaratively(
    "fragment_clips",
    "fragment_feed",
    "fragment_news",
    "fragment_search",
) {
    name("<clinit>")
}
