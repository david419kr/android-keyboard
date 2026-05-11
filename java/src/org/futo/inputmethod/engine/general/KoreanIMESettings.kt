package org.futo.inputmethod.engine.general

import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.settings.Settings
import org.futo.inputmethod.latin.uix.settings.SettingSliderSharedPrefsInt
import org.futo.inputmethod.latin.uix.settings.UserSetting
import org.futo.inputmethod.latin.uix.settings.UserSettingsMenu
import kotlin.math.roundToInt

object KoreanIMESettings {
    val menu = UserSettingsMenu(
        title = R.string.korean_settings_title,
        navPath = "ime/ko",
        registerNavPath = true,
        settings = listOf(
            UserSetting(
                name = R.string.korean_settings_repeated_key_interval,
                subtitle = R.string.korean_settings_repeated_key_interval_subtitle,
            ) {
                val resources = LocalResources.current
                SettingSliderSharedPrefsInt(
                    title = stringResource(R.string.korean_settings_repeated_key_interval),
                    subtitle = stringResource(R.string.korean_settings_repeated_key_interval_subtitle),
                    key = Settings.PREF_KOREAN_REPEATED_KEY_TIMEOUT,
                    default = 200,
                    range = 50.0f..500.0f,
                    hardRange = 25.0f..1000.0f,
                    transform = { it.roundToInt() },
                    indicator = { resources.getString(R.string.abbreviation_unit_milliseconds, "$it") },
                    steps = 44
                )
            }
        )
    )
}
