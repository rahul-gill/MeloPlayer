package meloplayer.core.ui.components.base

import androidx.compose.ui.tooling.preview.Preview

@Preview(
    name = "small font",
    group = "font scales",
    fontScale = 0.5f,
    showBackground = true,
    showSystemUi = true
)
@Preview(
    name = "large font",
    group = "font scales",
    fontScale = 2f,
    showBackground = true,
    showSystemUi = true
)
annotation class FontScalePreviews

@Preview(
    name = "mobile device",
    group = "devices",
    showSystemUi = true,
    device = "spec:id=reference_phone,shape=Normal,width=411,height=891,unit=dp,dpi=420"
)
@Preview(
    name = "tablet device",
    group = "devices",
    showBackground = true,
    showSystemUi = true,
    device = "spec:id=reference_tablet,shape=Normal,width=1280,height=800,unit=dp,dpi=240"
)
@Preview(
    name = "desktop device",
    group = "devices",
    showBackground = true,
    showSystemUi = true,
    device = "spec:id=reference_desktop,shape=Normal,width=1920,height=1080,unit=dp,dpi=160"
)
annotation class DevicesPreviews
