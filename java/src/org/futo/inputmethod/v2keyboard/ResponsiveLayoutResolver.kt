package org.futo.inputmethod.v2keyboard

import android.content.Context
import org.futo.inputmethod.latin.FoldStateProvider

const val KoreanResponsiveLayout = "korean_responsive"

private const val KoreanPhoneLayout = "korean_danmoeum"
private const val KoreanLargeScreenLayout = "korean_dubeolsik"
private const val TabletSmallestWidthDp = 600

fun Context.resolveResponsiveKeyboardLayout(layoutName: String): String {
    if (layoutName != KoreanResponsiveLayout) {
        return layoutName
    }

    return if (shouldUseLargeScreenKoreanLayout()) {
        KoreanLargeScreenLayout
    } else {
        KoreanPhoneLayout
    }
}

private fun Context.shouldUseLargeScreenKoreanLayout(): Boolean {
    val foldStateProvider = this as? FoldStateProvider
    if (foldStateProvider?.foldState?.feature != null && isFoldableInnerDisplayAllowed()) {
        return true
    }

    return resources.configuration.smallestScreenWidthDp >= TabletSmallestWidthDp
}
