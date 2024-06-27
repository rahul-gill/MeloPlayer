package meloplayer.app.playbackx

typealias OnEventCallback<T> = (T) -> Unit
typealias EventUnsubscribeHandle = () -> Unit

interface EventSource<T> {
    fun subscribe(subscriber: OnEventCallback<T>): EventUnsubscribeHandle
    fun unsubscribe(subscriber: OnEventCallback<T>)
}

open class EventSourceImpl<T> : EventSource<T> {
    private val subscribers = mutableListOf<OnEventCallback<T>>()

    override fun subscribe(subscriber: OnEventCallback<T>): EventUnsubscribeHandle {
        subscribers.add(subscriber)
        return { unsubscribe(subscriber) }
    }

    override fun unsubscribe(subscriber: OnEventCallback<T>) {
        subscribers.remove(subscriber)
    }

    fun dispatchEvent(event: T) {
        subscribers.forEach { it(event) }
    }

}