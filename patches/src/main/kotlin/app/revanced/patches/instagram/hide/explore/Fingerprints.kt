package app.revanced.patches.instagram.hide.explore

import app.revanced.patcher.gettingFirstMethodDeclaratively
import app.revanced.patcher.name
import app.revanced.patcher.patch.BytecodePatchContext

/*
 * The parser for the response behind the search tab's grid. Instagram generates one
 * unsafeParseFromJson per response model; this is the one holding the explore-specific keys, so
 * the three of them together identify it without needing the obfuscated class name.
 *
 * The previous fingerprint also required "clusters", a String-returning invoke-static, a
 * move-result-object, "next_max_id" and "interests" to appear in that order. That held while the
 * parser was a chain of comparisons, but 442.0.0.46.79 compiles it to a switch on the field name's
 * hash, which reorders the keys into hash order ("interests" now precedes "clusters", which
 * precedes "next_max_id") and moves the only String-returning invoke-static ahead of all of them.
 * The ordered match therefore failed and the patch was silently skipped. Nothing about the intent
 * needed the order, so it is now recovered from the switch itself in the patch.
 */
internal val BytecodePatchContext.exploreResponseJsonParserMethod by gettingFirstMethodDeclaratively(
    "clusters",
    "next_max_id",
    "interests",
) {
    name("unsafeParseFromJson")
}
