package dbdriver.elasticsearch

import com.typesafe.scalalogging.StrictLogging
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.node.Node
import org.elasticsearch.node.NodeBuilder.nodeBuilder
import org.elasticsearch.search.sort.SortOrder
import spray.json._
import utilities.LibConfig

/** methods responsible for initializing and starting an elasticsearch server
 */
trait ESBase extends LibConfig with StrictLogging {
  val MAIN_INDEX = "finbase"
  val DICT_INDEX = "dicts"
  val COMPANY = "company"
  val R10K = "10-K"
  val R10Q = "10-Q"
  val ANA = "analytics"
  val INV = "investments"
  
  val ALL_INDICES = List(MAIN_INDEX, DICT_INDEX)
  val CHILD_TYPES = List(R10K, R10Q, ANA, INV)
  
  val parentMapping = (config getString "parentMapping").parseJson.toString()
  val childMapping = (config getString "childMapping").parseJson.toString()

  // Clusters are composed of nodes; nodes can be either data-carrying or simply router-style
  private lazy val node = nodeBuilder node ()

  // client is our gateway to the node
  private[elasticsearch] lazy val client = {
    val c = node client()
    c.admin.cluster.prepareHealth().setWaitForYellowStatus.execute().actionGet
    c
  }

  // Make sure our mappings are set up
  initializeMappings()

  /** Initialize an elasticsearch node
   */
  def init(): Unit = client
  def shutdown() = if (node != null) node close ()

  /** Setup some initial mapping configurationa
   *  - Set CHILD_TYPES as children documents to the COMPANY doctype
   *  - mark company children facts as nested documents
   *  - mark children facts as nested documents
   *  FIXME non-hardcode "company" in the childMapping
   *  TODO properly mark all fact-children as "nested" types
   */
  def initializeMappings() = {
    // prepare the dict index
    val dIndexExists = client.admin().indices().prepareExists(DICT_INDEX).execute().actionGet().isExists()
    if (!dIndexExists) client.admin().indices().prepareCreate(DICT_INDEX).execute().actionGet()
    
    // prepare the main index
    val indexExists = client.admin().indices().prepareExists(MAIN_INDEX).execute().actionGet().isExists()
    if (!indexExists) client.admin().indices().prepareCreate(MAIN_INDEX).execute().actionGet()

    val clusterState = client.admin().cluster().prepareState().execute().actionGet()
    val mappings = clusterState.getState().metaData().index(MAIN_INDEX).mappings()

    def putMapping(name: String) = if (!mappings.containsKey(name))
      client.admin.indices
        .preparePutMapping(MAIN_INDEX)
        .setType(name)
        .setSource(if (CHILD_TYPES contains name) childMapping else parentMapping)
        .execute.actionGet()

    (COMPANY :: CHILD_TYPES) map putMapping
  }

}
