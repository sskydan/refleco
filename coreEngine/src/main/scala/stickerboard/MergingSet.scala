package stickerboard

import scalaz.Scalaz._
import scalaz.Semigroup

/** FIXME efficiency
 *  FIXME initial parameter should be priavte
 *  TODO cleanup
 */
case class MergingSet[A : Semigroup](i: A*) extends Set[A] {
  val backing = i.foldLeft(Set[A]())(conc)

  def conc(it: Set[A], elem: A): Set[A] = {
	  val toInsert = it find (_ == elem) map (_ |+| elem) getOrElse elem
			  
	  it - elem + toInsert
  }

  def contains(key: A): Boolean = backing contains key
  def iterator: Iterator[A] = backing.iterator
  def +(elem: A): MergingSet[A] = conc(backing, elem)
  def -(elem: A): MergingSet[A] = backing - elem
  override def empty: MergingSet[A] = MergingSet()
}

object MergingSet {
  /** TODO to monad instead?
   */
  implicit def ms2semigroup[A: Semigroup]: Semigroup[MergingSet[A]] = new Semigroup[MergingSet[A]] {
    def append(left: MergingSet[A], right: => MergingSet[A]): MergingSet[A] =
      left.foldLeft(right)(_ + _)
  }
  
  implicit def set2ms[A : Semigroup](set: Set[A]): MergingSet[A] = 
    MergingSet(set.toSeq: _*)
}