package meloplayer.app.parts.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.skydoves.colorpicker.compose.AlphaTile
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import meloplayer.app.R
import meloplayer.app.prefs.PreferenceManager
import meloplayer.core.ui.AppTheme
import meloplayer.core.ui.ColorSchemeType
import meloplayer.core.ui.DarkThemeType
import meloplayer.core.ui.ThemeConfig
import meloplayer.core.ui.components.base.AlertDialog
import meloplayer.core.ui.components.base.GenericPreference
import meloplayer.core.ui.components.base.ListPreference
import meloplayer.core.ui.components.base.PreferenceGroupHeader
import meloplayer.core.ui.components.base.SwitchPreference


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onGoBack: () -> Unit
) {

    val followSystemColor = PreferenceManager.followSystemColors.asState()
    val seedColor = PreferenceManager.colorSchemeSeed.asState()
    val theme = PreferenceManager.themeConfig.asState()
    val darkThemeType = PreferenceManager.darkThemeType.asState()


    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.settings),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onGoBack, modifier = Modifier.testTag("go_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.go_back_screen)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
    ) { innerPadding ->
        val screenHeight = LocalConfiguration.current.screenHeightDp
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .heightIn(min = screenHeight.dp)
        ) {
            PreferenceGroupHeader(title = stringResource(id = R.string.look_and_feel))
            Spacer(modifier = Modifier.height(8.dp))

            val themeValues = remember {
                ThemeConfig.entries.toList()
            }
            ListPreference(
                title = stringResource(id = R.string.app_theme),
                items = themeValues,
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Palette, contentDescription = null)
                },
                selectedItemIndex = themeValues.indexOf(theme.value),
                onItemSelection = { PreferenceManager.themeConfig.setValue(themeValues[it]) },
                itemToDescription = { themeIndex ->
                    stringResource(
                        id = when (themeValues[themeIndex]) {
                            ThemeConfig.FollowSystem -> R.string.follow_system
                            ThemeConfig.Light -> R.string.light
                            ThemeConfig.Dark -> R.string.dark
                        }
                    )
                }
            )
            //Spacer(modifier = Modifier.height(8.dp))
            SwitchPreference(
                title = stringResource(R.string.pure_black_background),
                isChecked = darkThemeType.value == DarkThemeType.Black,
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Contrast, contentDescription = null)
                },
                onCheckedChange = { checked ->
                    PreferenceManager.darkThemeType.setValue(if (checked) DarkThemeType.Black else DarkThemeType.Dark)
                }
            )

            //Spacer(modifier = Modifier.height(8.dp))
            SwitchPreference(
                title = stringResource(R.string.follow_system_colors),
                isChecked = followSystemColor.value,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.FormatColorFill,
                        contentDescription = null
                    )
                },
                onCheckedChange = {
                    PreferenceManager.followSystemColors.setValue(it)
                }
            )
            AnimatedVisibility(visible = !followSystemColor.value) {
                //Spacer(modifier = Modifier.height(8.dp))
                val isColorPickerDialogShowing = remember {
                    mutableStateOf(false)
                }
                GenericPreference(
                    title = stringResource(R.string.custom_color_scheme_seed),
                    summary = stringResource(id = R.string.custom_color_scheme_seed_summary),
                    onClick = {
                        isColorPickerDialogShowing.value = true
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Colorize,
                            contentDescription = null
                        )
                    },
                    trailingContent = {
                        Surface(
                            modifier = Modifier
                                .background(
                                    color = seedColor.value,
                                    shape = CircleShape
                                )
                                .size(24.dp),
                            color = seedColor.value,
                            shape = CircleShape,
                            content = {}
                        )
                    }
                )

                if (isColorPickerDialogShowing.value) {
                    val pickedColor = remember {
                        mutableStateOf(PreferenceManager.colorSchemeSeed.value)
                    }
                    val colorController = rememberColorPickerController()
                    AppTheme(
                        colorSchemeType = if (followSystemColor.value) ColorSchemeType.Dynamic else ColorSchemeType.WithSeed(
                            pickedColor.value
                        ),
                        themeConfig = theme.value,
                        darkThemeType = darkThemeType.value
                    ) {
                        AlertDialog(
                            onDismissRequest = { isColorPickerDialogShowing.value = false },
                            title = {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = stringResource(id = R.string.custom_color_scheme_seed))
                                    AlphaTile(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape),
                                        controller = colorController
                                    )
                                }
                            },
                            body = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    HsvColorPicker(
                                        modifier = Modifier.height(450.dp),
                                        controller = colorController,
                                        initialColor = pickedColor.value,
                                        onColorChanged = { envelope ->
                                            pickedColor.value = envelope.color
                                        }
                                    )
                                }
                            },
                            buttonBar = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = {
                                        isColorPickerDialogShowing.value = false
                                    }) {
                                        Text(text = stringResource(id = R.string.cancel))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TextButton(onClick = {
                                        PreferenceManager.colorSchemeSeed.setValue(pickedColor.value)
                                        isColorPickerDialogShowing.value = false
                                    }) {
                                        Text(text = stringResource(id = R.string.ok))
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

}