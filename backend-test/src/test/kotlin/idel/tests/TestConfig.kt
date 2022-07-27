package idel.tests

import com.typesafe.config.ConfigFactory
import mu.KotlinLogging

object TestConfig {
   private val log = KotlinLogging.logger {}
   private val cfg = ConfigFactory
      .load()
      .also {log.info {it.toString()}}
      .getConfig("application").also {log.info {it.toString()}}

   val backendUrl = cfg.getString("saedi.url")!!
   val fakeSmtpUrl = cfg.getString("fakesmtp.url")!!

}