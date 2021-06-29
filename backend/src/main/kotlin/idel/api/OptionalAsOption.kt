package idel.api

import arrow.core.None
import arrow.core.Option
import java.util.*

fun <T> Optional<T>.asOption() : Option<T> {
    return if (this.isEmpty) {
        None
    } else {
        Option.fromNullable(this.get())
    }
}