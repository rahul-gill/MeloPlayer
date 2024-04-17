package meloplayer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import meloplayer.app.R
import meloplayer.core.ui.components.NavigationBarTabs
import meloplayer.core.ui.components.TabItem
import meloplayer.core.ui.components.sheet.PlayerSheetScaffold
import meloplayer.core.ui.components.sheet.rememberPlayerSheetState



enum class TabScreen {
    Dashboard, Budget, Stats, More
}

@Composable
fun RootScreen(){
    val sheetState = rememberPlayerSheetState()
    PlayerSheetScaffold(
        sheetState = sheetState,
        fullPlayerContent = {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .systemBarsPadding()
                    .background(color = MaterialTheme.colorScheme.background)
            ) {
                val queue = remember {
                    listOf(
                        R.drawable.app_icon,
                        R.drawable.app_icon_monochrome,
                        R.drawable.app_icon,
                        R.drawable.app_icon_monochrome,
                        R.drawable.app_icon,
                        R.drawable.app_icon_monochrome,
                        R.drawable.app_icon,
                        R.drawable.app_icon_monochrome,
                    )
                }
                val itemIndex = remember {
                    mutableIntStateOf(0)
                }
                NowPlayingPanel(
                    playItemAtIndex = { itemIndex.intValue = it },
                    playingQueue = queue.map { painterResource(id = it) },
                    currentItemIndex = itemIndex.intValue
                )
            }
        },
        miniPlayerContent = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .systemBarsPadding()
            ) {
                Icon(imageVector = Icons.Default.Face, contentDescription = null)
                Spacer(modifier = Modifier.size(16.dp))
                Text(text = "Something in the way", style = MaterialTheme.typography.titleMedium)
            }
        },
        navBarContent = {
            val selectedItem = remember {
                mutableStateOf(TabScreen.Dashboard)
            }
            NavigationBarTabs(
                modifier = Modifier
                    .selectableGroup()
                    .navigationBarsPadding()
            ) {
                val tabs = remember {
                    TabScreen.entries.toList()
                }

                tabs.forEach { tabScreen ->
                    TabItem(
                        onClick = {
                            selectedItem.value = tabScreen
                        },
                        selected = selectedItem.value == tabScreen,
                        text = when (tabScreen) {
                            TabScreen.Dashboard -> "For you"
                            TabScreen.Budget -> "Songs"
                            TabScreen.More -> "Albums"
                            TabScreen.Stats -> "Folders"
                        }
                    )
                }

                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "TODO",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                //.background(androidx.compose.ui.graphics.Color.Yellow)
            ) {
                Text(text = "Yellow is main content")
                val scope = rememberCoroutineScope()
                Button(onClick = { scope.launch { sheetState.expandToFullPlayer() } }) {
                    Text(text = "Expand")
                }
                Button(onClick = { scope.launch { sheetState.shrinkToMiniPlayer() } }) {
                    Text(text = "Shrink")
                }
            }
        }
    )
}