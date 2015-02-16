package dsl.dlx

import scala.language.existentials

object QLMatrix {
  
  def constructStreaming(cols: Seq[Seq[Int]], names: Seq[String]) = {
    
  }
  
  def constructSimple[T <: QuadNodeIntf[T]](cols: Seq[Seq[Int]], names: Seq[String], nodeBuilder: QuadHeader => T) = {
    val root = new QuadHeader("root")
    
    // link the columns (vertically)
    val matrix = (cols zip names) map { case (col, name) =>
      val header = new QuadHeader(name)
      val contents = col map (_ -> nodeBuilder(header))
      
      contents.foldLeft[S forSome {type S <: QLList[S]}](header){
        case (l, (1, r)) =>
          l.dn = r
          r.up = l
          header.size = header.size + 1
          r
        case (l, _) => l
      }
      val last = contents.reverse find (_._1 == 1) map (_._2: S forSome {type S <: QLList[S]}) getOrElse header
      header.up = last
      last.dn = header
      
      contents
    }
    
    // link the rows
    matrix.transpose foreach { row =>
      row.tail.foldLeft(row.head._2){
        case (l, (1, r)) =>
          l.r = r
          r.l = l
          r
        case (l, _) => l
      }
      val first = row.find(_._1 == 1).get._2
      val last = row.reverse.find(_._1 == 1).get._2
      first.l = last
      last.r = first
      val x = first.l
      val y = last.r
    }
    
    // link the special headers row
    matrix.transpose.head.foldLeft(root){
      case (l, (_, r)) =>
        l.r = r.c
        r.c.l = l
        r.c
    }
    val last = matrix.transpose.head.last._2.c
    root.l = last
    last.r = root
    
    root
  }
  
}

