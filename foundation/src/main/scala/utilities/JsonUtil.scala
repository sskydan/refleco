package utilities

import scala.reflect.ClassTag
import spray.json._
import scala.reflect.runtime.universe._
import extensions.DefaultsTo

/** object with json utility methods
 */
object JsonUtil {

  implicit def jsvalPimped(js: JsValue) = new JsValuePimp(js)
  
  /** Tries to parse the provided object as Json
   * TODO there is a difference between JsonParser and .toJson. Investigate
   * TODO error handing
   * @param s the value to transform
   * @return JsValue instance representing the object
   */
  def toJson(s: Any): JsValue = JsonParser(s.toString())

	class JsValuePimp(val root: JsValue) {
    private implicit def jsvalPimped(js: JsValue) = new JsValuePimp(js)

    /** Recursively looks for a given key; returns all matches. Also, it will recurse into
     *   nodes that have already matched
     * TODO is there a way to get CanBuildFrom for existential types?
     * @param <T> optional type of result you are looking for. Upper bounded by JsValue
     * @param key element name that you're looking for
     * @return set of the unordered results
     */
    def \\~[T <: JsValue : ClassTag](key: String)(implicit ev: T DefaultsTo JsValue): Set[T] = root match {
      case JsObject(fields) =>
        fields.collect{case (k, v:T) if k==key => v}.toSet ++
        fields.values.flatMap(_.\\~[T](key))
      case JsArray(array) => array.flatMap(_.\\~[T](key)).toSet
      case _ => Set()
    }
    
    /** Recursively looks for all given keys; returns all matches. Also, it will recurse into
     *   nodes that have already matched
     * @param <T> optional type of result you are looking for. Upper bounded by JsValue
     * @param keys element name(s) that you're looking for
     * @return set of the unordered results
     */
    def \\~[T <: JsValue : ClassTag](keys: String*)(implicit ev: T DefaultsTo JsValue): Set[T] =
      keys.flatMap(\\~[T]).toSet
    
    /** Recursively looks for a given key; returns the first match. Also, it will recurse into
     *   nodes that have already matched.
     * Will throw an error if no match has been found
     * FIXME stop looking once a result has been found
     * @param <T> optional type of result you are looking for. Upper bounded by JsValue
     * @param key element name that you're looking for
     * @return the first result
     */
    def \\[T <: JsValue : ClassTag](key: String)(implicit ev: T DefaultsTo JsValue): T = 
      \\~[T](key).head
    
    /** Recursively looks for all given keys; returns the first match. Also, it will recurse into
     *   nodes that have already matched
     * Will throw an error if no match has been found
     * @param <T> optional type of result you are looking for. Upper bounded by JsValue
     * @param root root to start at
     * @param keys element name(s) that you're looking for
     * @return set of the unordered results
     */
    def \\[T <: JsValue : ClassTag](keys: String*)(implicit ev: T DefaultsTo JsValue): T =
      \\~[T](keys:_*).head
    
    /** Recursively return all objects containing a particular object (key-value pair)
     * @param key key to match
     * @param value value to match for the provided key
     * @return set of the unordered results
     */
    def childrenWith[T <: JsValue](key: String, value: T): Set[JsObject] = root match {
      case obj@JsObject(fields) =>
        fields.collect{case (k, v) if k==key && v==value => obj}.toSet ++
        fields.values.flatMap(_ childrenWith (key, value))
      case JsArray(array) => array.flatMap(_ childrenWith (key, value)).toSet
      case _ => Set()
    }
  
    /** TODO generalize
     */
    private def mapUpdate(map: Map[String,JsValue], key: String, value: JsValue): Map[String,JsValue] =
      if (map contains key)
        map updated (key, map(key) ++ value)
      else 
        map + (key -> value)
    
    /** method to join together two jsvalue instances, assuming they have identical structures
     *  @param other the JsValue to compose with
     *  @return the union of the jsValues
     */
    def ++[T <: JsValue](other: T): JsValue = (root, other) match {
      case (JsObject(fields), JsObject(ofields)) => 
        val newFields = ofields.foldLeft(fields) {
          case (map, (k,v)) => mapUpdate(map, k, v)
        }
        JsObject(newFields)
        
      case (JsArray(fields), JsArray(ofields)) =>
        JsArray(fields ++ ofields)
        
      case (any, JsNull) => any
      case (any, oany) => oany
    }
    
    /** filter this node's children according to any of the provided fields
     *  @note will return the entire object if no filters are given
     *  @param paths a sequence of dot-separated paths representing the desired fields
     *  @return the pruned JsValue
     */
    def filterAll(paths: Seq[String]): JsValue =
      if (paths.isEmpty) root
      else paths map filter reduce (_ ++ _)
    
    /** Filter this node's children, keeping only nodes which match the provided pathname 
     *    returns an empty object if no match was found
     * FIXME usecase for lenses?
     * @param path the dot-separated path to follow
     * @return the filtered node
     */
    def filter(path: String): JsValue = filter(path.split('.').toList)
    
    private def filter(path: List[String]): JsValue = (root, path) match {
      // path is at the end
      case (JsObject(fields), h::Nil) => 
        JsObject(fields filterKeys (_ == h))
      
      // path needs to go deeper 
      case (JsObject(fields), h::tail) => 
        JsObject(
          fields collect {
            case (k,v) if k == h => k -> (v filter tail)
          } 
        )
      case (JsArray(fields), path) => 
        JsArray(
          fields map (_ filter path) filter (_ != JsNull)
        )
      
      // not matchable
      case _ => JsNull
    }
  }
}
