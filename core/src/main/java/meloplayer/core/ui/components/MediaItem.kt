package meloplayer.core.ui.components

import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import wow.app.core.R


@Composable
fun MediaItemGridCard(
    imageModel: Any?,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    options: @Composable (Boolean, () -> Unit) -> Unit = { _, _ -> },
    onClick: () -> Unit = {},
    isSelected: Boolean? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .then(modifier),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        onClick = onClick
    ) {
        Box {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box {
                    val transition = updateTransition(isSelected, label = "selected")
                    val padding by transition.animateDp(label = "padding") { selected ->
                        if (selected == true) 10.dp else 0.dp
                    }
                    val roundedCornerShape by transition.animateDp(label = "corner") { selected ->
                        if (selected == true) 16.dp else 5.dp
                    }
                    AsyncImage(
                        imageModel ?: painterResource(R.drawable.placeholder_music),
                        null,
                        placeholder = painterResource(R.drawable.placeholder_music),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .fillMaxWidth()
                            .padding(padding)
                            .clip(RoundedCornerShape(roundedCornerShape))
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 4.dp, start = 4.dp)
                    ) {
                        if (isSelected != null) {
                            if (isSelected) {
                                val bgColor =
                                    MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    tint = MaterialTheme.colorScheme.primary,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .border(2.dp, bgColor, CircleShape)
                                        .clip(CircleShape)
                                        .background(bgColor)
                                )
                            } else {
                                Icon(
                                    Icons.Filled.RadioButtonUnchecked,
                                    tint = Color.White.copy(alpha = 0.7f),
                                    contentDescription = null,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }
                    }
//                    Box(
//                        modifier = Modifier
//                            .align(Alignment.TopEnd)
//                    ) {
//                        var showOptionsMenu by remember { mutableStateOf(false) }
//                        IconButton(
//                            modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
//                            onClick = { showOptionsMenu = !showOptionsMenu }
//                        ) {
//                            Icon(Icons.Filled.MoreVert, null)
//                            options(showOptionsMenu) {
//                                showOptionsMenu = false
//                            }
//                        }
//                    }
//                    Box(
//                        modifier = Modifier
//                            .align(Alignment.BottomStart)
//                            .padding(8.dp)
//                    ) {
//                        IconButton(
//                            modifier = Modifier
//                                .background(
//                                    MaterialTheme.colorScheme.surface,
//                                    RoundedCornerShape(12.dp)
//                                )
//                                .then(Modifier.size(36.dp)),
//                            onClick = onPlay
//                        ) {
//                            Icon(Icons.Filled.PlayArrow, null)
//                        }
//                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

}

@Composable
fun MediaItemListCard(
    modifier: Modifier = Modifier,
    imageModel: Any?,
    title: String,
    subtitle: String,
    highlightedTitle: Boolean = false,
    onHeartIconClick: (() -> Unit)? = null,
    leading: @Composable () -> Unit = {},
    onShowOptionsMenu: (() -> Unit)? = null,
    thumbnailLabel: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
    showHeartIcon: Boolean = false,
    isSelected: Boolean? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth().then(modifier),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        onClick = onClick
    ) {
        Box(modifier = Modifier.padding(12.dp, 12.dp, 4.dp, 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                leading()
                Box {
                    AsyncImage(
                        imageModel ,
                        null,
                        placeholder = painterResource(R.drawable.placeholder_music),
                        fallback = painterResource(R.drawable.placeholder_music),
                        error = painterResource(R.drawable.placeholder_music),
                        modifier = Modifier
                            .size(45.dp)
                            .clip(RoundedCornerShape(10.dp)),
                    )
                    thumbnailLabel?.let { it ->

                        Box(
                            modifier = Modifier
                                .offset(y = 8.dp)
                                .align(Alignment.BottomCenter)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(3.dp, 0.dp)
                            ) {
                                ProvideTextStyle(
                                    MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) { it() }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = when {
                                highlightedTitle -> MaterialTheme.colorScheme.primary
                                else -> LocalTextStyle.current.color
                            }
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(15.dp))

                Row {
                    if (showHeartIcon) {
                        IconButton(
                            modifier = Modifier.offset(4.dp, 0.dp),
                            onClick = {
                                onHeartIconClick?.invoke()
                            }
                        ) {
                            Icon(
                                Icons.Filled.Favorite,
                                null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    if (onShowOptionsMenu != null) {
                        IconButton(
                            onClick = onShowOptionsMenu
                        ) {
                            Icon(
                                Icons.Filled.MoreVert,
                                null,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
            }
        }
    }

}