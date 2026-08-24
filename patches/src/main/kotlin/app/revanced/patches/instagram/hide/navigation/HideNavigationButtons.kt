package app.revanced.patches.instagram.hide.navigation

import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.firstMethod
import app.revanced.patcher.firstMethodDeclaratively
import app.revanced.patcher.immutableClassDef
import app.revanced.patcher.name
import app.revanced.patcher.patch.booleanOption
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.instagram.misc.extension.sharedExtensionPatch
import app.revanced.util.addInstructionsAtControlFlowLabel
import app.revanced.util.findFreeRegister
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import java.util.logging.Logger

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/instagram/hide/navigation/HideNavigationButtonsPatch;"

@Suppress("unused")
val hideNavigationButtonsPatch = bytecodePatch(
    name = "Hide navigation buttons",
    description = "Hides navigation bar buttons, such as the Reels and Create button.",
    use = false,
) {
    compatibleWith("com.instagram.android"("443.0.0.48.82"))

    dependsOn(sharedExtensionPatch)

    val hideHome by booleanOption(
        default = false,
        name = "Hide Home",
        description = "Permanently hides the Home button. App starts at next available tab.", // On the "homecoming" / current instagram layout.
    )

    val hideReels by booleanOption(
        default = true,
        name = "Hide Reels",
        description = "Permanently hides the Reels button.",
    )

    val hideDirect by booleanOption(
        default = false,
        name = "Hide Direct",
        description = "Permanently hides the Direct button.",
    )

    val hideSearch by booleanOption(
        default = false,
        name = "Hide Search",
        description = "Permanently hides the Search button.",
    )

    val hideProfile by booleanOption(
        default = false,
        name = "Hide Profile",
        description = "Permanently hides the Profile button.",
    )

    val hideCreate by booleanOption(
        default = true,
        name = "Hide Create",
        description = "Permanently hides the Create button.",
    )

    apply {
        val doHideHome = hideHome ?: false
        val doHideReels = hideReels ?: true
        val doHideDirect = hideDirect ?: false
        val doHideSearch = hideSearch ?: false
        val doHideProfile = hideProfile ?: false
        val doHideCreate = hideCreate ?: true

        if (!doHideHome && !doHideReels && !doHideDirect && !doHideSearch && !doHideProfile && !doHideCreate) {
            return@apply Logger.getLogger(this::class.java.name).warning(
                "No hide navigation buttons options are enabled. No changes made.",
            )
        }

        // The navigation button enum class, located via the button name constants.
        val enumClassDef = navigationButtonsEnumMethod.immutableClassDef

        // Get the field name which contains the name of the enum for the navigation button
        // ("fragment_clips", "fragment_share", ...)
        val enumNameField = enumClassDef.firstMethodDeclaratively {
            name("<init>")
        }.let { method ->
            method.indexOfFirstInstructionOrThrow {
                opcode == Opcode.IPUT_OBJECT &&
                    (this as TwoRegisterInstruction).registerA == 2 // p2 register.
            }.let {
                method.getInstruction(it).getReference<FieldReference>()!!.name
            }
        }

        // The (UserSession, boolean) -> List shape on its own matches four methods as of
        // 442.0.0.46.79. Narrow it to the one which actually assembles the navigation bar: it is
        // the only public final candidate which references the enum class resolved above.
        val initializeNavigationButtonsListMethod = classDefs.asSequence()
            .flatMap { it.methods.asSequence() }
            .filter {
                AccessFlags.PUBLIC.isSet(it.accessFlags) &&
                    AccessFlags.FINAL.isSet(it.accessFlags) &&
                    it.returnType == "Ljava/util/List;" &&
                    it.parameterTypes.size == 2 &&
                    it.parameterTypes[0].toString() == "Lcom/instagram/common/session/UserSession;" &&
                    it.parameterTypes[1].toString() == "Z"
            }.filter { method ->
                method.implementation?.instructions?.any { instruction ->
                    (instruction as? ReferenceInstruction)?.reference?.toString()
                        ?.contains(enumClassDef.type) == true
                } == true
            }.single().let { firstMethod(it) }

        initializeNavigationButtonsListMethod.apply {
            val returnIndex = indexOfFirstInstructionOrThrow(Opcode.RETURN_OBJECT)
            val buttonsListRegister = getInstruction<OneRegisterInstruction>(returnIndex).registerA
            val freeRegister = findFreeRegister(returnIndex)
            val freeRegister2 = findFreeRegister(returnIndex, freeRegister)

            fun instructionsRemoveButtonByName(buttonEnumName: String): String = """
                    const-string v$freeRegister, "$buttonEnumName"
                    const-string v$freeRegister2, "$enumNameField"
                    invoke-static { v$buttonsListRegister, v$freeRegister, v$freeRegister2 }, $EXTENSION_CLASS_DESCRIPTOR->removeNavigationButtonByName(Ljava/util/List;Ljava/lang/String;Ljava/lang/String;)Ljava/util/List;
                    move-result-object v$buttonsListRegister
                """

            if (doHideHome) {
                addInstructionsAtControlFlowLabel(
                    returnIndex,
                    instructionsRemoveButtonByName("fragment_feed"),
                )
            }

            if (doHideReels) {
                addInstructionsAtControlFlowLabel(
                    returnIndex,
                    instructionsRemoveButtonByName("fragment_clips"),
                )
            }

            if (doHideDirect) {
                addInstructionsAtControlFlowLabel(
                    returnIndex,
                    instructionsRemoveButtonByName("fragment_direct_tab"),
                )
            }
            if (doHideSearch) {
                addInstructionsAtControlFlowLabel(
                    returnIndex,
                    instructionsRemoveButtonByName("fragment_search"),
                )
            }

            if (doHideCreate) {
                addInstructionsAtControlFlowLabel(
                    returnIndex,
                    instructionsRemoveButtonByName("fragment_share"),
                )
            }

            if (doHideProfile) {
                addInstructionsAtControlFlowLabel(
                    returnIndex,
                    instructionsRemoveButtonByName("fragment_profile"),
                )
            }
        }
    }
}
