package idel.api

import arrow.core.Option
import java.util.*

fun <T> Optional<T>.asOption() : Option<T> {
    return if (this.isEmpty) { Option.empty() } else { Option.just(this.get()) }
}