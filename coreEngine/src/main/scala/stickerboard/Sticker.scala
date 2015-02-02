package stickerboard

import scala.concurrent.Future
import com.github.nscala_time.time.Imports._
import scalaz.Scalaz._
import scalaz.Semigroup
import Sticker._
import scalaz.Monad
import scala.language.higherKinds
import java.io.Externalizable
import java.io.ObjectOutput
import java.io.ObjectInput
import facts.Fact
import facts.FactNone

/** TODO make rels a multimap?
 */
case class Sticker(
  alias: Alias, 
  rels: Map[String, MergingSet[Sticker]] = Map(),
  birthday: Option[DateTime] = None
) {
  
  // lookup and chained lookup operations
  def get(rt: String) = rels(rt)
  private def follow(clue: Clue): Set[Sticker] = clue(this)
  private def followFirst(clue: Clue): Option[Sticker] = follow(clue).headOption
  def \(clues: Seq[Clue]): Option[Sticker] = collapse(clues)(_ followFirst _)
  def \~(clues: Seq[Clue]): MergingSet[Sticker] = collapse(clues)(_ follow _)
  def \(clue: Clue): Option[Sticker] = \(Seq(clue))
  def \~(clue: Clue): MergingSet[Sticker] = \~(Seq(clue))
  
  // insert and union style operations on rels
  def ++(newItems: RelationMap) = Sticker(alias, rels |+| newItems, birthday)
  def +(rt: String, stickers: MergingSet[Sticker]) = this ++ Map(rt -> stickers)
  def +(rt: String, stickers: Sticker) = this ++ Map(rt -> MergingSet(stickers))

  /** deep update operation
   *  TODO does insert new rel types??
   */
  def \+(chain: List[Clue], newRt: String, newSticker: Sticker): Sticker = chain match {
    case clue::tail =>
      followFirst(clue) map { nextSticker =>
        val updatedChild = nextSticker \+ (tail, newRt, newSticker)
        
        this + (clue.rt, updatedChild)
      } getOrElse this
      
    case Nil => this + (newRt, newSticker)
  }
  def \+(clue: Clue, newRt: String, newSticker: Sticker): Sticker = \+ (List(clue), newRt, newSticker)
    
  /** collapse/fold a path on the board using a provided function
   *  @param chain The path to follow
   *  @param fn Function to apply during fold
   */
  def collapse[B[Sticker] : Monad](chain: Seq[Clue])(fn: (Sticker,Clue) => B[Sticker]): B[Sticker] =
    chain.foldLeft (Monad[B] point this) {
      case (node, nextClue) => node flatMap (fn(_, nextClue))
    }
  
  /** TODO alias checking should accept if any of the fields match
   */
  override def equals(any: Any) = any match {
    case Sticker(otherAlias,_,otherBirthday) =>
      alias.id == otherAlias.id && birthday == otherBirthday 
    case _ => false
  }
  /** TODO full-ass this
   */
  def hardEquals(right: Sticker): Boolean = 
    alias == right.alias && 
    birthday == right.birthday && 
    rels.size == right.rels.size &&
    (rels map {
      case (k,v) if (right.rels contains k) && right.rels(k).size == v.size =>
        v.flatMap(s => right.rels(k).find(_ hardEquals s)).size == v.size
      case _ => false
    }).foldLeft(true)(_ && _)
  
  def prettyPrint(depth: Int = 0): String = {
    val indent = " " * depth
    indent + s"$alias - $birthday" + rels.foldLeft("\n"){ case (str, (rel, stickers)) => str +
      indent + s"-->$rel" + stickers.foldLeft("\n")(
        _ + _.prettyPrint(depth + 4)
      )
    }
  }
}
  
object Sticker {
  type RelationMap = Map[String, MergingSet[Sticker]]
  
  // FIXME you 'avin a laugh
  case class SSticker(alias:Alias, rels:Map[String, Set[SSticker]] = Map(), birthday:Option[DateTime])
  implicit def s2ss(s:Sticker): SSticker = {
    val newRels = s.rels map { case (k,v) => k -> (v map s2ss).toSet}
    SSticker(s.alias, newRels, s.birthday)
  }
  implicit def ss2s(ss:SSticker): Sticker = {
    val newRels = ss.rels map { case (k,v) => k -> MergingSet((v map ss2s).toSeq:_*)}
    Sticker(ss.alias, newRels, ss.birthday)
  } 

  
  /** FIXME NOT commutative -- can't use in reduce 
   */
  implicit def stickergroup: Semigroup[Sticker] = new Semigroup[Sticker] {
    def append(left: Sticker, right: => Sticker): Sticker = 
      Sticker(
        left.alias |+| right.alias,
        left.rels |+| right.rels,
        left.birthday orElse right.birthday
      )
  }
  
  /** why not a default?
   */
  implicit def setAsMonad: Monad[Set] = new Monad[Set] {
    def point[A](a: => A): Set[A] = Set(a)
    def bind[A, B](fa: Set[A])(f: A => Set[B]): Set[B] = fa flatMap f
  }
  
  implicit def s2ms[A:Semigroup](s:Set[A]): MergingSet[A] = MergingSet(s.toSeq:_*)
  implicit def ms2s[A:Semigroup](ms:MergingSet[A]): Set[A] = ms.toSet
  
  implicit def ktuple2clue(tup: Tuple2[String,String]): Clue = Clue(tup._1, Some(tup._2))
  implicit def dtuple2clue(tup: Tuple2[String,DateTime]): Clue = Clue(tup._1, None, Some(tup._2))
  implicit def m2clue[T[_], S <% Clue](m: T[S]): T[Clue] = m
  
  implicit def s2fact(s: Sticker): Fact = {
    val flatRels = s.rels.toList flatMap { case (k,v) => v map (k -> _) }
    val children = flatRels map { case (k,v) => Fact(v.alias.id, k, FactNone, v.alias.id)}
    
    Fact(s.alias.id, "refleco:entity", FactNone, s.alias.id, 0, children)
  }
}


