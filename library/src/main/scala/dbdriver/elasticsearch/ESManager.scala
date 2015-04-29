package dbdriver.elasticsearch


import dbdriver.DataServerManager
import com.typesafe.scalalogging.StrictLogging

/** Class for initializing and managing an elasticsearch node cluster
 *  @note uses elasticsearch.yml config in src/main/resources
 */
trait ESManager 
  extends DataServerManager 
  with ESBase with ESQueryer with ESUpdater
  with StrictLogging
