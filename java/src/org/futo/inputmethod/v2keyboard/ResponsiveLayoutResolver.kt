package org.futo.inputmethod.v2keyboard

import android.content.Context
import org.futo.inputmethod.latin.FoldStateProvider

const val KoreanResponsiveLayout = "korean_responsive"
const val JapaneseMoakiResponsiveLayout = "japanese_moaki_responsive"

private const val KoreanPhoneLayout = "korean_danmoeum"
private const val KoreanLargeScreenLayout = "korean_dubeolsik"
private const val JapaneseLargeScreenLayout = "japanese_qwerty"
private const val TabletSmallestWidthDp = 600

fun Context.resolveResponsiveKeyboardLayout(layoutName: String): String {
    return when (layoutName) {
        KoreanResponsiveLayout -> if (shouldUseLargeScreenLayout()) {
            KoreanLargeScreenLayout
        } else {
            KoreanPhoneLayout
        }

        JapaneseMoakiResponsiveLayout -> if (shouldUseLargeScreenLayout()) {
            JapaneseLargeScreenLayout
        } else {
            JapaneseMoakiResponsiveLayout
        }

        else -> layoutName
    }
}

private fun Context.shouldUseLargeScreenLayout(): Boolean {
    val foldStateProvider = this as? FoldStateProvider
    if (foldStateProvider?.foldState?.feature != null && isFoldableInnerDisplayAllowed()) {
        return true
    }

    return resources.configuration.smallestScreenWidthDp >= TabletSmallestWidthDp
}
