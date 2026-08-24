package app.revanced.patches.instagram.misc.share

import app.revanced.patcher.composingFirstMethod
import app.revanced.patcher.instructions
import app.revanced.patcher.invoke
import app.revanced.patcher.name
import app.revanced.patcher.patch.BytecodePatchContext

internal val BytecodePatchContext.permalinkResponseJsonParserMethodMatch by composingFirstMethod {
    name("unsafeParseFromJson")
    instructions("permalink"(), "XDTPermalinkResponse"())
}

internal val BytecodePatchContext.storyUrlResponseJsonParserMethodMatch by composingFirstMethod {
    name("unsafeParseFromJson")
    instructions("story_item_to_share_url"())
}

internal val BytecodePatchContext.profileUrlResponseJsonParserMethodMatch by composingFirstMethod {
    name("unsafeParseFromJson")
    instructions("profile_to_share_url"())
}
