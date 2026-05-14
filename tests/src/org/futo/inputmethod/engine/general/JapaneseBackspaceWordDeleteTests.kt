/*
 * Copyright (C) 2026 FUTO Holdings, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package org.futo.inputmethod.engine.general

import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class JapaneseBackspaceWordDeleteTests {
    @Test
    fun testDeletesLastJapaneseToken() {
        val text = "\u4ECA\u65E5\u306F\u5B66\u6821"

        Assert.assertEquals(2, computeJapaneseWordDeleteLengthBeforeCursor(text))
    }

    @Test
    fun testPunctuationDeletedFirst() {
        val text = "\u4ECA\u65E5\u306F\u5B66\u6821\u3002"

        Assert.assertEquals(1, computeJapaneseWordDeleteLengthBeforeCursor(text))
    }

    @Test
    fun testTrailingWhitespaceDeletesPreviousTokenToo() {
        val text = "\u4ECA\u65E5\u306F\u5B66\u6821\u3000"

        Assert.assertEquals(3, computeJapaneseWordDeleteLengthBeforeCursor(text))
    }

    @Test
    fun testSurrogatePairIsNotSplit() {
        val text = "\u4ECA\u65E5\u306F\uD83D\uDE00"

        Assert.assertEquals(2, computeJapaneseWordDeleteLengthBeforeCursor(text))
    }

    @Test
    fun testEmptyTextReturnsZero() {
        Assert.assertEquals(0, computeJapaneseWordDeleteLengthBeforeCursor(""))
    }
}
