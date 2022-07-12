package idel.infrastructure.repositories.psql

import arrow.core.Either
import idel.domain.NotImplemented

interface HasUnimplemented {
    val className: String
        get() = this::class.simpleName!!

    fun notImplemented(method: String) = Either.Left(NotImplemented(className, method))

}