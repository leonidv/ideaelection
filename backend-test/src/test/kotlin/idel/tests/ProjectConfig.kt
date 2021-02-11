package idel.tests

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.listeners.Listener
import io.kotest.extensions.allure.AllureTestReporter

object ProjectConfig : AbstractProjectConfig() {

    override fun listeners() = listOf(AllureTestReporter())

    override val testNameRemoveWhitespace = true
}