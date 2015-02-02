package utilities

import java.io.File
import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config

trait CEConfig {
  // Setup env config
  private val overrides = ConfigFactory.parseFile(new File("coreConfig.conf"))
  val config:Config = ConfigFactory.load(overrides).getConfig("coreEngine")
}