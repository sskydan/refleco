package facts

import com.github.nscala_time.time.Imports._


/** Classes for representing complex fact values.
 *   see http://stackoverflow.com/questions/3170821/abstract-types-versus-type-parameters
 *  FIXME standardize "inner value" naming, so all direct values have same key. This should allow some nice
 *   cleanup in the dependencies. The problem is ES indexing mapping key names to static types.
 *  FIXME value classes + jsonFormatX() are not playing nice
 *  @note http://bloodredsun.com/2011/06/22/doubles-financial-calculations/
 */
sealed trait FactVal {
  type Inner
  def get: Inner
}
sealed trait FactValSliced[T <: FactValSliced[T]] extends FactVal with Ordered[T] {
  val inner: FactVal
  override type Inner = inner.Inner
  override def get = inner.get
}

/** Represents an instance of a monetary-like value
 *  @param valDouble The actual monetary amount
 *  @param currency The currency symbol
 *  @param factttype Fact type switch for serialization
 */
case class FactMoney(
  valDouble: BigDecimal,
  currency: String,
  precision: Int,
  facttype: String = "monetary") extends FactVal {
  type Inner = BigDecimal
  def get = valDouble
}

/** @link http://www.sec.gov/about/forms/form13f.pdf
 */
case class Holding(
  issuer: String,
  cusip: Double,
  titleClass: String,
  marketValue: BigDecimal,
  sharesOrPrincipal: (String, BigDecimal),
  discretion: String,
  authoritySole: Double,
  authorityShared: Double,
  authorityNone: Double,
  facttype: String = "holding"
) extends FactVal {
  type Inner = BigDecimal
  def get = marketValue
}

/** Single-variable classes
 */
case class FactString(valStr: String) extends FactVal {
  type Inner = String
  def get = valStr
}
case class FactInt(valInt: Int) extends FactVal {
  type Inner = Int
  def get = valInt
}
case class FactBD(valBD: BigDecimal) extends FactVal {
  type Inner = BigDecimal
  def get = valBD
}

/** Wrapper for time-aware fact values
 *  @param startDate The date at which this value starts applying
 *  @param endDate The date at which this values stops applying
 *  @param inner The inner FactVal wrapped by this one
 */
case class Period(
  startDate: DateTime,
  endDate: DateTime,
  inner: FactVal,
  facttype: String = "period"
) extends FactValSliced[Period] with Ordered[Period] {
  implicit def compare(that: Period) =
    if (endDate < that.endDate) 1
    else if (endDate > that.endDate) -1
    else 0
}

/** Wrapper for generic sequence of factVals as FactValSliced
 */
case class Group[T <: FactVal](
  inner: T,
  facttype: String = "group"
) extends FactValSliced[Group[T]] with Ordered[Group[T]] {
  implicit def compare(that: Group[T]) = 0
}

/** Represents a fact value which is broken up into multiple facets
 *  @note this class is required atm to bypass elasticsearch key-mapping restrictions
 *  @param valList Represents the underlying collection of FactValSliced types
 *  @param factttype Fact type switch for serialization
 */
case class FactCol[T <: FactValSliced[T]](
  valList: List[T],
  facttype: String = "collection"
) extends FactVal with Seq[T] {

  type Inner = T#Inner
  def get: Inner = valList.sorted.head.get

  def iterator: Iterator[T] = valList.iterator
  def apply(idx: Int): T = valList(idx)
  def length: Int = valList.length
}

/** represents a missing factval
 */
case object FactNone extends FactVal {
  type Inner = Option[FactVal]
  def get = None
}
