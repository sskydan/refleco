package extensions

import scala.concurrent._
import java.util.concurrent.{ ExecutorCompletionService, Executors }
import spray.json.JsValue
import ImplicitConversions._
import utilities.JsonUtil._
import scala.reflect.ClassTag
import scala.collection.Traversable

/** TODO function composition enrichment to simulate if-else (fallback) behavior with short-circuiting
 */
object Extensions {
  import utilities.JsonUtil._
  
  val cpus = Runtime.getRuntime().availableProcessors()
	val fixedPool = Executors.newFixedThreadPool((cpus+1) * 2)

  implicit class IteratorPimp[A](it: Iterator[A]) {

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

  implicit class SeqPimp[T](xs: Seq[T]) {
    
    /** apply a sequence of functions, starting at every position in the object sequence, and
     *    count the number of successful subsets found this way  
     */
    def countSlicesLike(fns: Seq[T => Boolean]): Int = {
		  val maxSliceSize = Seq(xs.size - 1, xs.size - fns.size).min
		  val slices = (0 to maxSliceSize) map (xs view (_, xs.size))
			  
		  slices map { slice =>
			  (fns zip slice).foldLeft(true){ case (b, (fn, elem)) => b && fn(elem)}
		  } count (_ == true)
    }
    
    /** produce the cartesian product of a matrix (2d seq)
     *  FIXME make lazy
     */
    def cartesianProduct[B](implicit asTraversable: (T) => Traversable[B]): Seq[Seq[B]] =
      xs.foldLeft(Seq(Seq.empty[B])) { 
        (ll, rr) => for (l <- ll; r <- asTraversable(rr)) yield l :+ r 
      }
  }
  
  implicit class ListPimp[T](l: List[T]) {
    
    /** split the list on the provided predicate, and filter out all the empty results
     *  @param p the function to use as the filter
     *  @return a list of subsets
     */
    def splitFilter(p: T => Boolean): List[List[T]] = l match {
       case Nil => Nil
       case x::xs if p(x) => (xs dropWhile p).splitFilter(p)  
       case _ => l span (!p(_)) match { case (a,b) => a :: b.splitFilter(p) }
    }
  }
  
}
