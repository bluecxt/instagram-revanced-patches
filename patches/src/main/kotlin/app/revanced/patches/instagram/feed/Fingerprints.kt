package app.revanced.patches.instagram.feed

import app.revanced.patcher.gettingFirstMethodDeclaratively
import app.revanced.patcher.patch.BytecodePatchContext

/*
 * The feed request class used to be fingerprinted here by its toString() debug string, but that
 * matches two classes on both 401.0.0.48.79 and 442.0.0.46.79, so which one resolved depended on
 * dex ordering. The class is now derived from the header map field read by the finder below, which
 * is unique, and which also drops a dependency on a debug string that a release build may strip.
 */
internal val BytecodePatchContext.mainFeedHeaderMapFinderMethod by gettingFirstMethodDeclaratively(
    "pagination_source", "FEED_REQUEST_SENT"
)
