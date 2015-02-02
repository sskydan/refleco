package stickerboard

import testbase.UnitSpec
import org.parboiled.errors.{ErrorUtils, ParsingException}
import org.parboiled.errors.ParserRuntimeException
import com.github.nscala_time.time.Imports._
import Sticker._
import scalaz.Scalaz._
import scalaz.Semigroup

class TestSticker extends UnitSpec {

	val now = Some(DateTime.now)
	val lastYear = Some(DateTime.lastYear)
	val yesterday = Some(DateTime.yesterday)

  val google = Alias("google", Seq("goog","google inc"), Some("googleFact"), Some("googleDBP"))
  val yahoo = Alias("yahoo", Seq("y!","yhoo"), Some("yahooFact"), Some("yahooDBP"))
  val valve = Alias("valve", Seq("volvo"), Some("valveFact"), Some("valveDBP"))
  val icefrog = Alias("icefrog", Seq("frozen amphibian", "icefraud", "gaben"))
  
  val r10k = Alias("10-K", Seq(), Some("report"))
  
  "Basic sticker ops" should "work" in {
    val f1 = Sticker(r10k, Map(), now)
    val f2 = Sticker(r10k, Map(), lastYear)
    val f3 = Sticker(r10k, Map(), yesterday)
    
    val c1 = Sticker(google, Map("financials" -> MergingSet(f1)))
    val c2 = Sticker(yahoo, Map())
    val c3 = Sticker(valve, Map("financials" -> MergingSet(f1, f2)))
    val c4 = Sticker(icefrog, Map("financials" -> MergingSet(f2, f3)))
    
    val root = Sticker(Alias("root"), Map("company" -> MergingSet(c1,c2,c3,c4)))
    
    // get
    assert(c1.get("financials") == Set(f1))
    assert(c3.get("financials") == Set(f1,f2))
    
    // ++
    assert(c3 ++ Map("company" -> MergingSet(c1,c2)) hardEquals Sticker(valve, Map("financials" -> MergingSet(f1,f2), "company" -> MergingSet(c1,c2))))
    assert(c3 ++ Map("financials" -> MergingSet(f3)) hardEquals Sticker(valve, Map("financials" -> MergingSet(f1,f2,f3))))
    assert(c3 ++ Map() hardEquals c3)
    
    // +
    assert(c1 + ("financials", c2) hardEquals Sticker(google, Map("financials" -> MergingSet(f1,c2))))
    assert(c2 + ("financials", c2) hardEquals Sticker(yahoo, Map("financials" -> MergingSet(c2))))
    
    // lookup \
    var testOp = root \ ("company" -> "google")
    assert(testOp.get hardEquals c1)
    testOp = root \ Seq("company" -> "valve", "financials" -> "10-K")
    assert(testOp.get hardEquals f1)
    testOp = c3 \ ("financials" -> lastYear.get)
    assert(testOp.get hardEquals f2)
    // FIXME why is the type ascription rilly necessary here?
    testOp = root \ (Seq("company" -> "icefrog", "financials" -> yesterday.get): Seq[Clue])
    assert(testOp.get hardEquals f3)
    testOp = root \ ("company" -> "none")
    assert(testOp == None)
    
    // append \+
    assert(c2 \+ (Nil, "company", c4) hardEquals Sticker(yahoo, Map("company" -> MergingSet(c4))))
    
    var test = c3 \+ ("financials" -> r10k.id, "company", c4) 
    val newF1 = Sticker(r10k, Map("company" -> MergingSet(c4)), now)
    val newF2 = Sticker(r10k, Map("company" -> MergingSet(c4)), lastYear)
    var expected = Sticker(valve, Map("financials" -> MergingSet(newF1, f2))) 
    var expected2 = Sticker(valve, Map("financials" -> MergingSet(f1, newF2)))
    assert((test hardEquals expected) || (test hardEquals expected2))
    
    test = root \+ (List[Clue]("company" -> "valve", "financials" -> "10-K"), "company", c4) 
    expected = 
      Sticker(Alias("root"), Map("company" -> MergingSet(
        c1,
        c2, 
        Sticker(valve, Map("financials" -> MergingSet(newF1, f2))),
        c4)))
    assert(test hardEquals expected)
    assert(root \+ ("company" -> "none", "financials", f2) hardEquals root)
  }
  
  "Sticker merging" should "be applied in conflicts" in {
    val f1 = Sticker(r10k, Map("1" -> MergingSet()), now)
    val f2 = Sticker(r10k, Map("2" -> MergingSet(f1)), yesterday)
    val f3 = Sticker(r10k, Map("2" -> MergingSet(f2)), yesterday)
    val fM = Sticker(r10k, Map("2" -> MergingSet(f1,f2)), yesterday)
    
    val google1 = Alias("google inc.", Seq("g"), None, Some("g"))
    val google2 = Alias("google inc.", Seq("g"), Some("gg"), Some("gg"))
    val googleM = Alias("google inc.", Seq("g"), Some("gg"), Some("gg"))
    
    val c0 = Sticker(yahoo)
    val c1 = Sticker(google1, Map("financials" -> MergingSet(f1, f3), "x" -> MergingSet(c0)))
    val c2 = Sticker(google2, Map("financials" -> MergingSet(f2)))
    val merged = Sticker(googleM, Map("financials" -> MergingSet(f1, fM), "x" -> MergingSet(c0)))
    
    var test = c2 |+| c1
    var expected = merged
    assert(test hardEquals expected)
    
    val root = Sticker(Alias("root"), Map("company" -> MergingSet(c0, c1)))
    
    test = root + ("company", c2)
    expected = Sticker(Alias("root"), Map("company" -> MergingSet(c0, merged)))
    assert(test hardEquals expected)
  }
}


