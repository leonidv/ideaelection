package idel.tests.infrastructure

import arrow.core.Option
import arrow.core.getOrElse

fun <T> Option<T>.getOrThrow(fieldName : String) : T {
   return  this.getOrElse {throw IllegalStateException("can't get $fieldName")}
}