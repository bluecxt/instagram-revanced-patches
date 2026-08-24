package app.revanced.patches.instagram.hide.suggestions

import app.revanced.patcher.composingFirstMethod
import app.revanced.patcher.instructions
import app.revanced.patcher.invoke
import app.revanced.patcher.name
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.unorderedAllOf

internal val FEED_ITEM_KEYS_TO_BE_HIDDEN = arrayOf(
    "clips_netego",
    "stories_netego",
    "in_feed_survey",
    "bloks_netego",
    "suggested_igd_channels",
    "suggested_top_accounts",
    "suggested_users",
)

/*
 * The lookup strings were dropped. "text_app_suggested_users_kickstart_unit" is not one of the
 * keys this patch hides, so anchoring on it only added a way for the fingerprint to break: it is
 * exactly the kind of incidental literal Instagram moves into a pooled string getter between
 * builds, and the parser would then stop resolving.
 *
 * The keys the patch does act on are enough on their own. All seven together match three methods
 * in 442.0.0.46.79 -- the parser, a sibling writer on the same class and an unrelated <clinit> --
 * and name() narrows that to the parser itself.
 */
internal val BytecodePatchContext.feedItemParseFromJsonMethodMatch by composingFirstMethod {
    name("unsafeParseFromJson")
    instructions(predicates = unorderedAllOf(predicates = FEED_ITEM_KEYS_TO_BE_HIDDEN.map { it() }.toTypedArray()))
}
