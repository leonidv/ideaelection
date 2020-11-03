package idel.tests

import com.typesafe.config.ConfigFactory

object Idel {
   val URL = ConfigFactory.load().getString("idel.url")
}