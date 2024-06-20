package meloplayer.core.prefs

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class PreferenceMock<T>(
    override val defaultValue: T
) : Preference<T> {
    private var _flow = MutableStateFlow(defaultValue)
    override fun setValue(value: T) {
        _flow.update { value }
    }

    override val key: String = ""
    override val observableValue: MutableStateFlow<T> = _flow
}