/*
 * Copyright Squircle CE contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blacksquircle.ui.feature.settings.ui.about

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.blacksquircle.ui.core.extensions.daggerViewModel
import com.blacksquircle.ui.ds.PreviewBackground
import com.blacksquircle.ui.ds.extensions.adaptiveIconPainterResource
import com.blacksquircle.ui.ds.preference.Preference
import com.blacksquircle.ui.ds.preference.PreferenceGroup
import com.blacksquircle.ui.ds.scaffold.ScaffoldSuite
import com.blacksquircle.ui.ds.toolbar.Toolbar
import com.blacksquircle.ui.feature.settings.BuildConfig
import com.blacksquircle.ui.feature.settings.R
import com.blacksquircle.ui.feature.settings.data.applicationName
import com.blacksquircle.ui.feature.settings.data.versionName
import com.blacksquircle.ui.feature.settings.internal.SettingsComponent
import com.blacksquircle.ui.ds.R as UiR

private const val PRIVACY_POLICY_URL =
    "https://github.com/lincomax/QuirkEdit/blob/master/PRIVACY-POLICY.md"
private const val PROJECT_URL = "https://github.com/lincomax/QuirkEdit"

@Composable
internal fun AboutHeaderScreen(
    viewModel: AboutHeaderViewModel = daggerViewModel { context ->
        val component = SettingsComponent.buildOrGet(context)
        AboutHeaderViewModel.Factory().also(component::inject)
    }
) {
    val context = LocalContext.current
    AboutHeaderScreen(
        onBackClicked = viewModel::onBackClicked,
        onVersionClicked = {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = PROJECT_URL.toUri()
            }
            context.startActivity(intent)
        },
        onPrivacyClicked = {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = PRIVACY_POLICY_URL.toUri()
            }
            context.startActivity(intent)
        },
    )
}

@Composable
private fun AboutHeaderScreen(
    onBackClicked: () -> Unit = {},
    onVersionClicked: () -> Unit = {},
    onPrivacyClicked: () -> Unit = {},
) {
    ScaffoldSuite(
        topBar = {
            Toolbar(
                title = stringResource(R.string.settings_header_about_title),
                navigationIcon = UiR.drawable.ic_back,
                onNavigationClicked = onBackClicked,
            )
        },
        modifier = Modifier.imePadding()
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
        ) {
            PreferenceGroup(
                title = stringResource(R.string.settings_category_about)
            )
            Preference(
                title = LocalContext.current.applicationName,
                subtitle = stringResource(
                    R.string.settings_about_subtitle,
                    LocalContext.current.appVersionName(),
                ),
                onClick = onVersionClicked,
                leadingContent = {
                    Image(
                        painter = adaptiveIconPainterResource(UiR.mipmap.ic_launcher),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp)
                    )
                },
            )
            Preference(
                title = stringResource(R.string.settings_privacy_policy_title),
                onClick = onPrivacyClicked,
            )
        }
    }
}

private fun Context.appVersionName(): String {
    return if (BuildConfig.DEBUG) {
        this.versionName + getString(R.string.settings_about_version_debug)
    } else {
        this.versionName
    }
}

@PreviewLightDark
@Composable
private fun AboutHeaderScreenPreview() {
    PreviewBackground {
        AboutHeaderScreen()
    }
}