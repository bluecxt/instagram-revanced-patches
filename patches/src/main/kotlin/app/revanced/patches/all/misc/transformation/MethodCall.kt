package app.revanced.patches.all.misc.transformation

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.patcher.extensions.replaceInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

typealias Instruction35cInfo = Triple<IMethodCall, Instruction35c, Int>

interface IMethodCall {
    val definedClassName: String
    val methodName: String
    val methodParams: Array<String>
    val returnType: String

    fun replaceInvokeVirtualWithExtension(
        definingClassDescriptor: String,
        method: MutableMethod,
        instruction: Instruction35c,
        instructionIndex: Int,
    ) {
        val args = with(instruction) {
            arrayOf(registerC, registerD, registerE, registerF, registerG)
                .take(registerCount).joinToString(", ") { "v$it" }
        }
        val replacementMethod =
            "$methodName(${definedClassName}${methodParams.joinToString(separator = "")})$returnType"

        method.replaceInstruction(
            instructionIndex,
            "invoke-static { $args }, $definingClassDescriptor->$replacementMethod",
        )
    }
}

inline fun <reified E> fromMethodReference(
    methodReference: MethodReference,
) where E : Enum<E>, E : IMethodCall = enumValues<E>().firstOrNull { search ->
    search.definedClassName == methodReference.definingClass &&
        search.methodName == methodReference.name &&
        methodReference.parameterTypes.toTypedArray().contentEquals(search.methodParams) &&
        search.returnType == methodReference.returnType
}

inline fun <reified E> filterMapInstruction35c(
    extensionClassDescriptorPrefix: String,
    classDef: ClassDef,
    instruction: Instruction,
    instructionIndex: Int,
): Instruction35cInfo? where E : Enum<E>, E : IMethodCall {
    if (classDef.type.startsWith(extensionClassDescriptorPrefix)) {
        return null
    }

    if (instruction.opcode != Opcode.INVOKE_VIRTUAL) {
        return null
    }

    val invokeInstruction = instruction as Instruction35c
    val methodRef = invokeInstruction.reference as MethodReference
    val methodCall = fromMethodReference<E>(methodRef) ?: return null

    return Instruction35cInfo(methodCall, invokeInstruction, instructionIndex)
}
