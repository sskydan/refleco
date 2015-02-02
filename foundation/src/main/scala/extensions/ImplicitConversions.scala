package extensions

import java.util.concurrent.Callable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

/** USE WITH CARE
 *  TODO why can't it have an explicit return type??
 */
object ImplicitConversions {

  implicit def arrayToList[A](a: Array[A]) = a.toList
  implicit def iteratorToList[A](a: Iterator[A]) = a.toList

  // TODO follow up
  //   http://scala-programming-language.1934581.n4.nabble.com/map-turns-Map-into-Iterable-why-td3178709.html
  implicit def iterableToSeq[A](a: Iterable[A]): Seq[A] = a.toSeq

  // TODO test
  //  implicit def anyToOption[A](a:A):Option[A] = Some(a)

  implicit def fnToCallable[F](a: => F) = new Callable[F] { def call() = a }

  implicit def fToFuture[T](f: java.util.concurrent.Future[T])(implicit ec: ExecutionContext) = Future{ f.get() }
}