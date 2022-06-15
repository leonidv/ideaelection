package idel.tests

import io.kotest.core.config.AbstractProjectConfig

object ProjectConfig : AbstractProjectConfig() {
    override val testNameRemoveWhitespace = true
}