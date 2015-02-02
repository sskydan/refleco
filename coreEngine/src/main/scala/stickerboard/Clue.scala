package stickerboard

import org.joda.time.DateTime

/** aka step / edge / path of length 1
 *  TODO rework this to allow custom filter behavior
 */
case class Clue(rt: String, key: Option[String] = None, date: Option[DateTime] = None) {
  
  private def keyFilter(s: Sticker) = key match {
    case Some(k) if k == s.alias.id => true
    case None => true
    case _ => false
  }
  
  private def dateFilter(s: Sticker) = date match {
    case date:Some[DateTime] if date == s.birthday => true
    case None => true
    case _ => false
  }
  
  def apply(s: Sticker): Set[Sticker] = s get rt filter dateFilter filter keyFilter
}
