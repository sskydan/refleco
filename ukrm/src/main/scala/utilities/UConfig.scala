package utilities

import java.io.File
import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config

trait UConfig {
  // Setup env config
  private val overrides = ConfigFactory.parseFile(new File("ukrmConfig.conf"))
  val config:Config = ConfigFactory.load(overrides).getConfig("ukrm")
}