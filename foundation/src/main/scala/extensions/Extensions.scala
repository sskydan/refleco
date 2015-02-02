package extensions

import scala.concurrent._
import java.util.concurrent.{ ExecutorCompletionService, Executors }
import spray.json.JsValue
import ImplicitConversions._
import utilities.JsonUtil._
import scala.reflect.ClassTag

/** TODO function composition enrichment to simulate if-else (fallback) behavior with short-circuiting
 */
object Extensions {
  import utilities.JsonUtil._
  
  val cpus = Runtime.getRuntime().availableProcessors()
	val fixedPool = Executors.newFixedThreadPool((cpus+1) * 2)

  class IteratorPimp[A](it: Iterator[A]) {

    /** Non-strict first-done-first-out parallel map implementation. Iterator backed by an ExecutorCompletionService
     *  @note Not threadsafe
     *  Could not find a similar solution using low-level scala-isms (not acknowledging
     *    the hacky Twitter util.Future), and didn't want to configure actors. Paid for it.
     *  TODO better way to get length?
     *  @note producer must always outpace consumer in current simplistic implementation
     *  @note see https://groups.google.com/forum/#!msg/scala-user/q2NVdE6MAGE/KnutOq3iT3IJ
     */
    def mapp[B](f: A => B)(implicit ev: ExecutionContext) = {
      val ec = new ExecutorCompletionService[B](fixedPool)

      var len = 0
      Future {
        it foreach (x => { len = len + 1; ec submit f(x) })
      }

      new Iterator[B] {
        def next() =
          if (hasNext) {
            len = len - 1
            ec.take().get
          } else Iterator.empty.next()

        def hasNext = len > 0
      }
    }
  }

  implicit def itPimped[T](it: Iterator[T]) = new IteratorPimp(it)

}
