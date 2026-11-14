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

package com.blacksquircle.ui.application

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.core.util.Consumer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.blacksquircle.ui.application.extensions.toComposeColors
import com.blacksquircle.ui.application.splash.SplashScreen
import com.blacksquircle.ui.application.update.KEY_INSTALL_UPDATE
import com.blacksquircle.ui.application.update.rememberInAppUpdate
import com.blacksquircle.ui.core.effect.ResultEffect
import com.blacksquircle.ui.core.extensions.daggerViewModel
import com.blacksquircle.ui.core.extensions.fullscreenMode
import com.blacksquircle.ui.core.extensions.showToast
import com.blacksquircle.ui.core.mvi.ViewEvent
import com.blacksquircle.ui.ds.SquircleTheme
import com.blacksquircle.ui.ds.animation.NavigationTransition
import com.blacksquircle.ui.internal.AppComponent
import com.blacksquircle.ui.navigation.api.Navigator
import com.blacksquircle.ui.navigation.api.provider.EntryProvider

@Composable
internal fun MainScreen(
    savedInstanceState: Bundle? = null,
    navigator: Navigator,
    entryProviders: Set<@JvmSuppressWildcards EntryProvider>,
    viewModel: MainViewModel = daggerViewModel { context ->
        val component = AppComponent.buildOrGet(context)
        MainViewModel.Factory().also(component::inject)
    },
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val colors = toComposeColors(viewState.colorScheme)

    SquircleTheme(colors = colors) {
        Surface(color = SquircleTheme.colors.colorBackgroundPrimary) {
            Box(Modifier.fillMaxSize()) {
                val dialogStrategy = remember { DialogSceneStrategy<NavKey>() }

                NavDisplay(
                    backStack = navigator.backStack,
                    onBack = { navigator.goBack() },
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator()
                    ),
                    entryProvider = entryProvider {
                        entryProviders.forEach { provider ->
                            with(provider) {
                                this@entryProvider.builder()
                            }
                        }
                    },
                    sceneStrategy = dialogStrategy,
                    transitionSpec = {
                        NavigationTransition.EnterTransition togetherWith NavigationTransition.ExitTransition
                    },
                    popTransitionSpec = {
                        NavigationTransition.PopEnterTransition togetherWith NavigationTransition.PopExitTransition
                    },
                    modifier = Modifier.fillMaxSize()
                )

                /** Status bar and navigation bar share one darker shade */
                val systemBarColor = lerp(
                    start = SquircleTheme.colors.colorBackgroundSecondary,
                    stop = Color.Black,
                    fraction = if (SquircleTheme.colors.isDark) 0.3f else 0.12f,
                )
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .windowInsetsTopHeight(WindowInsets.statusBars)
                        .background(systemBarColor)
                )
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .windowInsetsBottomHeight(WindowInsets.navigationBars)
                        .background(systemBarColor)
                )
            }
        }
    }

    val activity = LocalActivity.current as? ComponentActivity
    DisposableEffect(Unit) {
        val consumer = Consumer<Intent> { intent ->
            viewModel.onNewIntent(intent)
        }
        activity?.addOnNewIntentListener(consumer)
        onDispose {
            activity?.removeOnNewIntentListener(consumer)
        }
    }

    val inAppUpdate = rememberInAppUpdate()
    LaunchedEffect(Unit) {
        if (savedInstanceState == null) {
            activity?.intent?.let { intent ->
                viewModel.onNewIntent(intent)
            }
            inAppUpdate.checkForUpdates {
                viewModel.onUpdateAvailable()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.viewEvent.collect { event ->
            when (event) {
                is ViewEvent.Toast -> activity?.showToast(text = event.message)
            }
        }
    }

    ResultEffect<Unit>(KEY_INSTALL_UPDATE) {
        inAppUpdate.installUpdate()
    }

    LaunchedEffect(viewState.fullscreenMode) {
        activity?.window?.fullscreenMode(viewState.fullscreenMode)
    }

    AnimatedVisibility(
        visible = viewState.isLoading,
        enter = EnterTransition.None,
        exit = fadeOut(),
    ) {
        SquircleTheme(darkTheme = true) {
            SplashScreen()
        }
    }
}