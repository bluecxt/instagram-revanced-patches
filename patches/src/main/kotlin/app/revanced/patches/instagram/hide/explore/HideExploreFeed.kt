package app.revanced.patches.instagram.hide.explore

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instructions
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.instagram.util.forceSwitchDefaultBranch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction21c
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil

@Suppress("unused")
val hideExploreFeedPatch = bytecodePatch(
    name = "Hide explore feed",
    description = "Hides posts and reels from the explore/search page.",
    use = false,
) {
    compatibleWith("com.instagram.android"("443.0.0.48.82"))

    apply {
        exploreResponseJsonParserMethod.apply {
            // Every list the parser can fill: sectional_items (the grid itself), clusters,
            // interests and tentpole_interests. Read off the assignments rather than by name, since
            // the field names are obfuscated and renumber between builds.
            val listAssignments = instructions.filter {
                it.opcode == Opcode.IPUT_OBJECT &&
                    ((it as ReferenceInstruction).reference as FieldReference).type == "Ljava/util/List;"
            }
            if (listAssignments.isEmpty()) {
                throw PatchException("Explore response no longer holds any list of content")
            }

            // The registers the parser itself uses to build and store those lists.
            val listRegister = listAssignments.map { (it as TwoRegisterInstruction).registerA }.distinct().single()
            val responseRegister = listAssignments.map { (it as TwoRegisterInstruction).registerB }.distinct().single()
            val listFields = listAssignments.map { (it as ReferenceInstruction).reference as FieldReference }.distinct()

            val firstParameterRegister =
                implementation!!.registerCount - MethodUtil.getParameterRegisterCount(this)
            if (listRegister >= firstParameterRegister) {
                throw PatchException("Explore response parser builds its lists in a parameter register")
            }

            // Right after the response is constructed, so every path out of the parser sees the
            // lists set, including the early exit taken when the body is not an object. Placing
            // them at the return instead would not work: that instruction is the target of the
            // loop's exit branch, and inserting ahead of a branch target leaves the new code
            // unreachable.
            val bytecode = instructions.toList()
            val allocationIndex = bytecode.indexOfFirst {
                it.opcode == Opcode.NEW_INSTANCE && (it as Instruction21c).registerA == responseRegister
            }
            if (allocationIndex < 0) throw PatchException("Explore response is no longer allocated here")

            val constructionIndex = bytecode.drop(allocationIndex).indexOfFirst {
                it.opcode == Opcode.INVOKE_DIRECT &&
                    ((it as ReferenceInstruction).reference as MethodReference).name == "<init>" &&
                    (it as FiveRegisterInstruction).registerC == responseRegister
            }
            if (constructionIndex < 0) throw PatchException("Explore response is no longer constructed here")

            // The response the server sends for an empty explore carries empty arrays, not absent
            // ones. Skipping the fields alone leaves the lists null, which the search page never
            // resolves into a loaded state, so give it the shape it expects instead. A separate
            // ArrayList each, matching what the parser would have built, so a caller that adds to
            // one is not writing into a shared or immutable list. Anything the parser does go on to
            // read overwrites these.
            addInstructions(
                allocationIndex + constructionIndex + 1,
                listFields.joinToString("\n") { field ->
                    """
                        new-instance v$listRegister, Ljava/util/ArrayList;
                        invoke-direct {v$listRegister}, Ljava/util/ArrayList;-><init>()V
                        iput-object v$listRegister, v$responseRegister, $field
                    """
                },
            )

            // The parser dispatches on the hash of each field name it reads, and its default branch
            // is the app's own handler for a field it does not know: skip the value and move on.
            // Forcing every field down it leaves the response empty of content, and leaves
            // more_available and auto_load_more_enabled false so nothing tries to page for more.
            forceSwitchDefaultBranch()
        }
    }
}
