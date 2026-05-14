package org.futo.inputmethod.keyboard.internal

import android.graphics.Rect
import android.test.AndroidTestCase
import android.view.inputmethod.EditorInfo
import org.futo.inputmethod.keyboard.Key
import org.futo.inputmethod.latin.common.Constants
import org.futo.inputmethod.latin.uix.actions.ActionRegistry
import org.futo.inputmethod.v2keyboard.KeyboardLayoutSetV2
import org.futo.inputmethod.v2keyboard.KeyboardLayoutSetV2Params
import org.futo.inputmethod.v2keyboard.LayoutManager
import org.futo.inputmethod.v2keyboard.RegularKeyboardSize
import org.futo.inputmethod.v2keyboard.SplitKeyboardSize
import java.util.Locale

class KeyboardLayoutSetV2Tests : AndroidTestCase() {
    private val layoutParams = KeyboardLayoutSetV2Params(
        computedSize = RegularKeyboardSize(1024, 1024, Rect()),
        keyboardLayoutSet = "qwerty",
        locale = Locale.ENGLISH,
        editorInfo = EditorInfo(),
        numberRow = false,
        arrowRow = false,
        bottomActionKey = null,
        multilingualTypingLocales = emptyList(),
        numberRowMode = 0,
        useLocalNumbers = false,
        alternativePeriodKey = false
    )

    private fun getActualHeight(layoutSet: KeyboardLayoutSetV2): Int {
        return layoutSet.getKeyboard(
            KeyboardLayoutElement(
                kind = KeyboardLayoutKind.Alphabet0,
                page = KeyboardLayoutPage.Base
            )
        ).mBaseHeight
    }

    private fun actionKeyCode(action: String): Int =
        Constants.CODE_ACTION_0 + ActionRegistry.actionStringIdToIdx(action)

    private fun getRequiredKey(keys: List<Key>, code: Int): Key =
        keys.firstOrNull { it.code == code } ?: throw AssertionError("Missing key code $code")

    fun testKeyboardHeightSettingAffectsHeight() {
        LayoutManager.init(context)
        val testHeight = { tgtHeight: Int -> assertEquals(getActualHeight(KeyboardLayoutSetV2(context, layoutParams.copy(computedSize = RegularKeyboardSize(1024, tgtHeight, Rect())))), tgtHeight) }

        testHeight(600)
        testHeight(1200)
        testHeight(67)
        testHeight(185)
        testHeight(4440)
    }

    fun testArrowRowSplitsInSplitLayout() {
        LayoutManager.init(context)

        val totalWidth = 1000
        val splitLayoutWidth = 600
        val layoutSet = KeyboardLayoutSetV2(
            context,
            layoutParams.copy(
                computedSize = SplitKeyboardSize(
                    width = totalWidth,
                    height = 1000,
                    padding = Rect(),
                    singleRowHeight = 250,
                    splitLayoutWidth = splitLayoutWidth
                ),
                arrowRow = true
            )
        )

        val keys = layoutSet.getKeyboard(
            KeyboardLayoutElement(
                kind = KeyboardLayoutKind.Alphabet0,
                page = KeyboardLayoutPage.Base
            )
        ).sortedKeys

        val downKey = getRequiredKey(keys, actionKeyCode("down"))
        val leftKey = getRequiredKey(keys, actionKeyCode("left"))

        assertTrue(
            "Arrow row should leave the split keyboard center gap",
            leftKey.x - (downKey.x + downKey.totalWidth) >= totalWidth - splitLayoutWidth - 1
        )
    }
}
