package dsl

import testbase.UnitSpec
import org.parboiled.errors.{ErrorUtils, ParsingException}
import org.parboiled.errors.ParserRuntimeException

class TestReflask extends UnitSpec {
  val ctype = StringNode("company")
  val atype = StringNode("entity")
  
  val name = Some(StringNode("pooh"))
  val att = StringNode("story")
  val rel = StringNode("eeyore's birthday")
//  val intval = INumberNode(2)
  val numval = RNumberNode(4.2)
  val numval2 = RNumberNode(-5)
//  val numval3 = RNumberNode(41111111.22222)
  
  val filterEQ = FunctionNode(EQ, numval)
  val filterLT = FunctionNode(LT, numval)
  val filterGTEQ = FunctionNode(GTEQ, numval2)
  
  val selectorAtt = AttributeSelectorNode(att, Nil)
  val selectorAttEq = AttributeSelectorNode(att, Seq(filterEQ))
  val selectorAttLtGtEq = AttributeSelectorNode(att, Seq(filterLT, filterGTEQ))
  val selectorRel = RelationSelectorNode(rel)
  
  val path = PathNode(name, None)
  val pathAtt = PathNode(name, Some(selectorAtt))
  val pathAttEq = PathNode(name, Some(selectorAttEq))
  val pathAttLtGtEq = PathNode(name, Some(selectorAttLtGtEq))
  val pathRel = PathNode(name, Some(selectorRel))
  val noneAttEq = PathNode(None, Some(selectorAttEq))
  val noneAttLtGtEq = PathNode(None, Some(selectorAttLtGtEq))
  val noneRel = PathNode(None, Some(selectorRel))
    
  "DSL parser" should "accept simple question" in {
    val queries = Map(
      """company "pooh" """ -> QuestionNode(ctype, None, Seq(path)),
      "company" -> QuestionNode(ctype, None, Nil),
      "entity" -> QuestionNode(atype, None, Nil),
      """entity "pooh"""" -> QuestionNode(atype, None, Seq(path))
    )
    assertAll(queries)
  }
  
  "DSL parser" should "accept correct attribute selector" in {
    val queries = Map(
      """company "pooh"@story""" -> QuestionNode(ctype, None, Seq(pathAtt)),
      """company "pooh"@"story" """ -> QuestionNode(ctype, None, Seq(pathAtt)),
      """company "pooh"@"story"==4.2 """ -> QuestionNode(ctype, None, Seq(pathAttEq)),
//      """company "pooh"@story==4.2 """ -> QuestionNode(ctype, Seq(pathAttEq)),
      """company "pooh"@"story"<4.2>=-5 """ -> QuestionNode(ctype, None, Seq(pathAttLtGtEq))
    )
    assertAll(queries)
  }
  
  "DSL parser" should "accept correct relation selector" in {
    val queries = Map(
      """company "pooh"."eeyore's birthday" """ -> QuestionNode(ctype, None, Seq(pathRel))
    )
    assertAll(queries)
  }
  
  "DSL parser" should "accept multiple selectors" in {
    val queries = Map(
      """company "pooh"@"story"<4.2>=-5 "pooh"@story""" -> QuestionNode(ctype, None, Seq(pathAttLtGtEq, pathAtt)),
      """company "pooh"@"story"==4.2 "pooh"."eeyore's birthday"""" -> QuestionNode(ctype, None, Seq(pathAttEq, pathRel))
    )
    assertAll(queries)
  }

  "DSL parser" should "accept wildcard path roots" in {
    val queries = Map(
      """company *@"story"<4.2>=-5 "pooh" """ -> QuestionNode(ctype, None, Seq(noneAttLtGtEq, path)),
      """company *@"story"==4.2 *."eeyore's birthday"""" -> QuestionNode(ctype, None, Seq(noneAttEq, noneRel))
    )
    assertAll(queries)
  }
  
  "DSL parser" should "accept size requests" in {
    val queries = Map(
      """company 5 *@"story"<4.2>=-5 "pooh" """ -> QuestionNode(ctype, Some(INumberNode(5)), Seq(noneAttLtGtEq, path)),
      """company 100 *@"story"==4.2 *."eeyore's birthday"""" -> QuestionNode(ctype, Some(INumberNode(100)), Seq(noneAttEq, noneRel))
    )
    assertAll(queries)
  }
  
//  "DSL parser" should "reject wrong structures" in {
//    val queries = Seq(
//      """company pooh""",
//      """company "lim" 2.3""",
//      """lim 2""",
//      """company 14 lim 2"""
//    )
//    assertExceptionAll(queries)
//  }
//  
//  "DSL parser" should "reject incorrect function arguments" in {
//    val queries = Seq(
//      """company "pooh" filter story "eeyore's birthday"""",
//      """company "pooh" filter "story" sortby "story" "story"""",
//      """company "pooh" lim story""",
//      """company lim 2.3"""
//    )
//    assertExceptionAll(queries)
//  }
//   
//  "DSL parser" should "reject unknown functions" in {
//    val queries = Seq(
//      """companyX "pooh"""",
//      """company "pooh" filterr story"""
//    )
//    assertExceptionAll(queries)
//  }
  
  def assertAll(m:Map[String, AstNode]) = for {
      query <- m.keys
      expected = m(query)
      result = Reflask.parseQuestion(query)
    } yield assert(expected == result)
  
  def assertExceptionAll(queries:Seq[String]) = 
    queries foreach (q => intercept[ParsingException](Reflask.parseQuestion(q)))
}