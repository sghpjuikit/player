package sp.it.pl.audio.tagging

import javafx.concurrent.Task
import sp.it.pl.util.functional.invoke
import java.util.function.BiConsumer
import java.util.function.Consumer

@Suppress("UNCHECKED_CAST")
abstract class SuccessTask<T, O>(): Task<T>() {

    private var onEnd: BiConsumer<Boolean, T>? = null
    private var onClose: Consumer<O>? = null
    protected val sb = StringBuffer(40)

    @Suppress("LeakingThis")
    constructor(title: String): this() {
        updateTitle(title)
    }

    constructor(onEnd: BiConsumer<Boolean, T>): this() {
        setOnDone(onEnd)
    }

    constructor(title: String, onEnd: BiConsumer<Boolean, T>): this(title) {
        setOnDone(onEnd)
    }

    fun setOnDone(onEnd: BiConsumer<Boolean, T>): O {
        this.onEnd = onEnd
        return this as O
    }

    fun setOnClose(onClose: Consumer<O>): O {
        this.onClose = onClose
        return this as O
    }

    override fun succeeded() {
        super.succeeded()
        updateMessage(title+" succeeded")
        onEnd?.invoke(true, value)
        onClose?.invoke(this as O)
    }

    override fun cancelled() {
        super.cancelled()
        updateMessage(title+" cancelled")
        onEnd?.invoke(true, value)
        onClose?.invoke(this as O)
    }

    override fun failed() {
        super.failed()
        updateMessage(title+" failed")
        onEnd?.invoke(true, value)
        onClose?.invoke(this as O)
    }

    protected fun updateMessage(all: Int, done: Int, skipped: Int) {
        sb.setLength(0)
        sb.append("Completed: ")
        sb.append(done)
        sb.append(" / ")
        sb.append(all)
        sb.append(" ")
        sb.append(skipped)
        sb.append(" skipped.")
        updateMessage(sb.toString())
    }

    protected fun updateMessage(all: Int, done: Int) {
        sb.setLength(0)
        sb.append("Completed: ")
        sb.append(done)
        sb.append(" / ")
        sb.append(all)
        sb.append(".")
        updateMessage(sb.toString())
    }

}