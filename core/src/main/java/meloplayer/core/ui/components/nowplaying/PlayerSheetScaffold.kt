package meloplayer.core.ui.components.nowplaying

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemColors
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.window.core.layout.WindowWidthSizeClass

@Composable
fun PlayerSheetScaffold(
    sheetState: PlayerSheetState,
    fullPlayerContent: @Composable ColumnScope.() -> Unit,
    miniPlayerContent: @Composable (Boolean) -> Unit,
    navigationSuiteItems: NavigationSuiteScope.() -> Unit,
    modifier: Modifier = Modifier,
    isLayoutNavigationBar: Boolean =
        PlayerSheetScaffoldDefaults.calculateIsNavBarType(currentWindowAdaptiveInfo()),
    navigationSuiteColors: NavigationSuiteColors = NavigationSuiteDefaults.colors(),
    containerColor: Color = PlayerSheetScaffoldDefaults.containerColor,
    contentColor: Color = PlayerSheetScaffoldDefaults.contentColor,
    content: @Composable () -> Unit,
) {
    Surface(modifier = modifier, color = containerColor, contentColor = contentColor) {
        PlayerSheetScaffoldLayout(
            navigationSuite = {
                NavigationSuite(
                    isLayoutNavigationBar = isLayoutNavigationBar,
                    colors = navigationSuiteColors,
                    content = navigationSuiteItems
                )
            },
            isLayoutNavBar = isLayoutNavigationBar,
            miniPlayerContent = miniPlayerContent,
            fullPlayerContent = fullPlayerContent,
            sheetState = sheetState,
            content = {
                Box(
                    Modifier.consumeWindowInsets(
                        if (isLayoutNavigationBar) {
                            NavigationBarDefaults.windowInsets.only(WindowInsetsSides.Bottom)
                        } else {
                            NavigationRailDefaults.windowInsets.only(WindowInsetsSides.Start)
                        }
                    )
                ) {
                    content()
                }
            }
        )
    }
}

@Composable
fun NavigationSuite(
    modifier: Modifier = Modifier,
    isLayoutNavigationBar: Boolean,
    colors: NavigationSuiteColors = NavigationSuiteDefaults.colors(),
    content: NavigationSuiteScope.() -> Unit
) {
    val scope by rememberStateOfItems(content)
    // Define defaultItemColors here since we can't set NavigationSuiteDefaults.itemColors() as a
    // default for the colors param of the NavigationSuiteScope.item non-composable function.
    val defaultItemColors = NavigationSuiteDefaults.itemColors()
    if (isLayoutNavigationBar) {
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .then(modifier),
            containerColor = colors.navigationBarContainerColor,
            contentColor = colors.navigationBarContentColor
        ) {
            scope.itemList.forEach {
                NavigationBarItem(
                    modifier = it.modifier,
                    selected = it.selected,
                    onClick = it.onClick,
                    icon = { NavigationItemIcon(icon = it.icon, badge = it.badge) },
                    enabled = it.enabled,
                    label = it.label,
                    alwaysShowLabel = it.alwaysShowLabel,
                    colors =
                    it.colors?.navigationBarItemColors
                        ?: defaultItemColors.navigationBarItemColors,
                    interactionSource = it.interactionSource
                )
            }
        }
    } else {
        NavigationRail(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Start))
                .verticalScroll(rememberScrollState())
                .then(modifier),
            containerColor = colors.navigationRailContainerColor,
            contentColor = colors.navigationRailContentColor
        ) {
            scope.itemList.forEach {
                NavigationRailItem(
                    modifier = it.modifier,
                    selected = it.selected,
                    onClick = it.onClick,
                    icon = { NavigationItemIcon(icon = it.icon, badge = it.badge) },
                    enabled = it.enabled,
                    label = it.label,
                    alwaysShowLabel = it.alwaysShowLabel,
                    colors =
                    it.colors?.navigationRailItemColors
                        ?: defaultItemColors.navigationRailItemColors,
                    interactionSource = it.interactionSource
                )
            }
        }
    }
}


@Composable
private fun NavigationItemIcon(
    icon: @Composable () -> Unit,
    badge: (@Composable () -> Unit)? = null,
) {
    if (badge != null) {
        BadgedBox(badge = { badge.invoke() }) { icon() }
    } else {
        icon()
    }
}


object PlayerSheetScaffoldDefaults {

    fun calculateIsNavBarType(adaptiveInfo: WindowAdaptiveInfo): Boolean {
        return with(adaptiveInfo) {
            when {
                windowPosture.isTabletop -> true

                windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED ||
                        windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.MEDIUM -> false

                else -> true
            }
        }
    }

    val containerColor: Color
        @Composable get() = MaterialTheme.colorScheme.background

    val contentColor: Color
        @Composable get() = MaterialTheme.colorScheme.onBackground
}


sealed interface NavigationSuiteScope {
    fun item(
        selected: Boolean,
        onClick: () -> Unit,
        icon: @Composable () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        label: @Composable (() -> Unit)? = null,
        alwaysShowLabel: Boolean = true,
        badge: (@Composable () -> Unit)? = null,
        colors: NavigationSuiteItemColors? = null,
        interactionSource: MutableInteractionSource
    )
}

class NavigationSuiteItemColors(
    val navigationBarItemColors: NavigationBarItemColors,
    val navigationRailItemColors: NavigationRailItemColors
)

class NavigationSuiteColors
internal constructor(
    val navigationBarContainerColor: Color,
    val navigationBarContentColor: Color,
    val navigationRailContainerColor: Color,
    val navigationRailContentColor: Color,
    val navigationDrawerContainerColor: Color,
    val navigationDrawerContentColor: Color
)

object NavigationSuiteDefaults {

    @Composable
    fun colors(
        navigationBarContainerColor: Color = NavigationBarDefaults.containerColor,
        navigationBarContentColor: Color = contentColorFor(navigationBarContainerColor),
        navigationRailContainerColor: Color = NavigationRailDefaults.ContainerColor,
        navigationRailContentColor: Color = contentColorFor(navigationRailContainerColor),
        navigationDrawerContainerColor: Color = DrawerDefaults.standardContainerColor,
        navigationDrawerContentColor: Color = contentColorFor(navigationDrawerContainerColor),
    ): NavigationSuiteColors =
        NavigationSuiteColors(
            navigationBarContainerColor = navigationBarContainerColor,
            navigationBarContentColor = navigationBarContentColor,
            navigationRailContainerColor = navigationRailContainerColor,
            navigationRailContentColor = navigationRailContentColor,
            navigationDrawerContainerColor = navigationDrawerContainerColor,
            navigationDrawerContentColor = navigationDrawerContentColor
        )

    @Composable
    fun itemColors(
        navigationBarItemColors: NavigationBarItemColors = NavigationBarItemDefaults.colors(),
        navigationRailItemColors: NavigationRailItemColors = NavigationRailItemDefaults.colors(),
    ): NavigationSuiteItemColors =
        NavigationSuiteItemColors(
            navigationBarItemColors = navigationBarItemColors,
            navigationRailItemColors = navigationRailItemColors
        )
}

@Composable
private fun rememberStateOfItems(
    content: NavigationSuiteScope.() -> Unit
): State<NavigationSuiteItemProvider> {
    val latestContent = rememberUpdatedState(content)
    return remember { derivedStateOf { NavigationSuiteScopeImpl().apply(latestContent.value) } }
}

private interface NavigationSuiteItemProvider {
    val itemsCount: Int
    val itemList: MutableVector<NavigationSuiteItem>
}

private class NavigationSuiteItem(
    val selected: Boolean,
    val onClick: () -> Unit,
    val icon: @Composable () -> Unit,
    val modifier: Modifier,
    val enabled: Boolean,
    val label: @Composable (() -> Unit)?,
    val alwaysShowLabel: Boolean,
    val badge: (@Composable () -> Unit)?,
    val colors: NavigationSuiteItemColors?,
    val interactionSource: MutableInteractionSource
)

private class NavigationSuiteScopeImpl : NavigationSuiteScope, NavigationSuiteItemProvider {
    override fun item(
        selected: Boolean,
        onClick: () -> Unit,
        icon: @Composable () -> Unit,
        modifier: Modifier,
        enabled: Boolean,
        label: @Composable (() -> Unit)?,
        alwaysShowLabel: Boolean,
        badge: (@Composable () -> Unit)?,
        colors: NavigationSuiteItemColors?,
        interactionSource: MutableInteractionSource
    ) {
        itemList.add(
            NavigationSuiteItem(
                selected = selected,
                onClick = onClick,
                icon = icon,
                modifier = modifier,
                enabled = enabled,
                label = label,
                alwaysShowLabel = alwaysShowLabel,
                badge = badge,
                colors = colors,
                interactionSource = interactionSource
            )
        )
    }

    override val itemList: MutableVector<NavigationSuiteItem> = mutableVectorOf()
    override val itemsCount: Int
        get() = itemList.size
}