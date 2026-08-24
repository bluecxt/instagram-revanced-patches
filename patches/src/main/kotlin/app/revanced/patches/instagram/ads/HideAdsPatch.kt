package app.revanced.patches.instagram.ads

import app.revanced.patcher.firstMethod
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.meta.ads.STORY_AD_INSERTED_LOG
import app.revanced.patches.meta.ads.storyAdInsertionMethodOrNull
import app.revanced.util.p0Register
import app.revanced.util.returnEarly
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

@Suppress("unused")
val hideAdsPatch = bytecodePatch("Hide ads") {
    compatibleWith("com.instagram.android"("443.0.0.48.82"))

    apply {
        // 1. Collect and patch Dynamic Reels & Feed Insertion points
        val voidEarlyReturnTargets = mutableListOf<Pair<String, String>>()
        val booleanFalseTargets = mutableListOf<Pair<String, String>>()

        classDefs.forEach { classDef ->
            classDef.methods.forEach { method ->
                var hasPushDown = false
                var hasInsertSuccess = false
                var hasInjectionOpportunity = false

                method.implementation?.instructions?.forEach { ins ->
                    if (ins is ReferenceInstruction) {
                        val ref = ins.reference
                        if (ref is StringReference) {
                            val str = ref.string
                            if (str == "Insert push down") hasPushDown = true
                            if (str == "Insert success") hasInsertSuccess = true
                            if (str == "onInjectionOpportunity") hasInjectionOpportunity = true
                        }
                    }
                }

                if (hasPushDown && method.returnType == "V" && method.parameterTypes.size == 1 && method.parameterTypes[0].toString() == "I") {
                    voidEarlyReturnTargets.add(Pair(classDef.type, method.name))
                }

                if (hasInsertSuccess && method.returnType == "V") {
                    voidEarlyReturnTargets.add(Pair(classDef.type, method.name))
                }

                if (hasInjectionOpportunity && method.returnType == "V") {
                    voidEarlyReturnTargets.add(Pair(classDef.type, method.name))
                }
            }

            if (classDef.type == "LX/02xg;") {
                classDef.methods.forEach { method ->
                    if (method.name == "FKh" && method.returnType == "Z") {
                        booleanFalseTargets.add(Pair(classDef.type, method.name))
                    }
                }
            }
        }

        // Apply Feed & Reels patches
        voidEarlyReturnTargets.forEach { (classType, methodName) ->
            val classDef = classDefs[classType] ?: return@forEach
            val mutableClassDef = classDefs.getOrReplaceMutable(classDef)
            val methodToPatch = mutableClassDef.methods.firstOrNull { it.name == methodName } ?: return@forEach
            mutableClassDef.firstMethod(methodToPatch).returnEarly()
        }

        booleanFalseTargets.forEach { (classType, methodName) ->
            val classDef = classDefs[classType] ?: return@forEach
            val mutableClassDef = classDefs.getOrReplaceMutable(classDef)
            val methodToPatch = mutableClassDef.methods.firstOrNull { it.name == methodName } ?: return@forEach
            mutableClassDef.firstMethod(methodToPatch).returnEarly(false)
        }

        // 2. Patch Story Ad Insertion (Delath method)
        storyAdInsertionMethodOrNull?.apply {
            val bytecode = instructions.toList()
            val insertedLogIndex = bytecode.indexOfFirst {
                it.opcode == Opcode.CONST_STRING &&
                    ((it as ReferenceInstruction).reference as StringReference).string == STORY_AD_INSERTED_LOG
            }

            if (insertedLogIndex >= 0) {
                val notInsertedIndex = (0 until insertedLogIndex).firstOrNull {
                    bytecode[it].opcode == Opcode.SGET_OBJECT &&
                        bytecode[it + 1].opcode == Opcode.RETURN_OBJECT
                }

                if (notInsertedIndex != null && p0Register > 0) {
                    val notInsertedValue =
                        (bytecode[notInsertedIndex] as ReferenceInstruction).reference as FieldReference

                    addInstructions(
                        0,
                        """
                            sget-object v0, $notInsertedValue
                            return-object v0
                        """,
                    )
                }
            }
        }
    }
}
