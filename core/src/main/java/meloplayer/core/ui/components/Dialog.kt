package meloplayer.core.ui.components

import android.view.Gravity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider


@Composable
fun BaseDialog(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    properties: DialogProperties = DialogProperties(
        dismissOnClickOutside = true,
        usePlatformDefaultWidth = false
    ),
    onDismissRequest: () -> Unit,
    padding: PaddingValues = BaseDialogDefaults.contentPadding,
    minWidth: Dp = 280.dp,
    paneTitle: String = "Dialog",
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        (LocalView.current.parent as? DialogWindowProvider)?.window?.run {
            setDimAmount(BaseDialogDefaults.dimAmount)
            setGravity(Gravity.BOTTOM)
        }
        Box(
            modifier = modifier
                .widthIn(min = minWidth)
                .padding(BaseDialogDefaults.dialogMargins)
                .semantics { this.paneTitle = paneTitle }
        ) {
            Column(
                modifier = Modifier
                    .clip(BaseDialogDefaults.shape)
                    .shadow(elevation = BaseDialogDefaults.elevation)
                    .background(
                        color = backgroundColor,
                        shape = BaseDialogDefaults.shape
                    )
                    .padding(padding),
            ) {
                content()
            }
        }
    }
}

@Composable
fun AlertDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    title: (@Composable () -> Unit)? = null,
    body: (@Composable () -> Unit)? = null,
    buttonBar: (@Composable () -> Unit)? = null
) {
    BaseDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest
    ) {
        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            ProvideTextStyle(MaterialTheme.typography.titleLarge) {
                title?.let { it() }
            }
            if (title != null && body != null) {
                Spacer(
                    modifier = Modifier
                        .height(8.dp)
                )
            }
            ProvideTextStyle(MaterialTheme.typography.bodyLarge) {
                body?.let { it() }
            }
            if (buttonBar != null) {
                Spacer(
                    modifier = Modifier
                        .height(26.dp)
                )
            }
            Row {
                buttonBar?.let { it() }
            }
        }
    }
}

object BaseDialogDefaults {

    val contentPadding = PaddingValues(
        all = 24.dp
    )

    val dialogMargins = PaddingValues(
        bottom = 12.dp,
        start = 12.dp,
        end = 12.dp
    )

    val shape = RoundedCornerShape(
        size = 26.dp
    )

    const val dimAmount = 0.65F

    val elevation = 1.dp

}