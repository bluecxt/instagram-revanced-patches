package app.revanced.patches.instagram.util

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instructions
import app.revanced.patcher.patch.PatchException
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.SwitchPayload

/**
 * Sends every execution of the method's switch down its default branch by overwriting the switch
 * register with a key the payload does not contain. In dex, a key that is absent from the payload
 * falls through to the instruction directly after the switch, so this reuses whatever the app
 * itself already does for a value it does not recognise instead of injecting new behaviour.
 *
 * The key is derived from the payload at patch time rather than hardcoded, so the patch keeps
 * working when Instagram adds, removes or renumbers cases.
 */
internal fun MutableMethod.forceSwitchDefaultBranch() {
    val bytecode = instructions.toList()

    val switchIndex = bytecode.indexOfFirst {
        it.opcode == Opcode.PACKED_SWITCH || it.opcode == Opcode.SPARSE_SWITCH
    }
    if (switchIndex < 0) throw PatchException("$this no longer dispatches through a switch")

    val keys = bytecode.filterIsInstance<SwitchPayload>().flatMap { it.switchElements }.mapTo(HashSet()) { it.key }
    if (keys.isEmpty()) throw PatchException("$this has a switch without a payload")

    // 0, 1, -1, 2, -2, ... so a key is found without relying on the payload's range.
    val unusedKey = generateSequence(0) { if (it > 0) -it else -it + 1 }.first { it !in keys }

    val register = (bytecode[switchIndex] as OneRegisterInstruction).registerA

    addInstructions(switchIndex, "const v$register, $unusedKey")
}
