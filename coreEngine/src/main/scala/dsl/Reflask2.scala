//package dsl
//
//import org.parboiled.scala._
//import org.parboiled.errors.{ ErrorUtils, ParsingException }
//import scala.reflect.ClassTag
//import scala.reflect.runtime.universe._
//
///** These case classes form the nodes of the AST.
// */
//sealed abstract class AstNode
//
//case class QuestionNode(qtype: AttributeNode, qname: Option[AttributeNode], fns: Seq[FunctionNode[_ <: ValueNode]]) extends AstNode
//case class FunctionNode[T <: ValueNode](fn: FN[T], args: T) extends AstNode
//
///** for generic function "values"
// */
//sealed abstract class ValueNode {
//  type VALTYPE
//  val value: VALTYPE
//}
//case class AttributeNode(value: String) extends ValueNode { type VALTYPE = String }
//case class INumberNode(value: Int) extends ValueNode { type VALTYPE = Int }
//case class RNumberNode(value: BigDecimal) extends ValueNode { type VALTYPE = BigDecimal }
//case class ArgsNode(key: AttributeNode, setVal: ValueNode) extends ValueNode {
//  type VALTYPE = (AttributeNode, ValueNode)
//  override val value = (key, setVal)
//}
//
///** class representing our "function" abstraction, ie, some kind of (parametrized) transformation
// */
//sealed abstract class FN[-ARGS <: ValueNode: ClassTag](val name: String) extends AstNode {
//  def typecheck[T <: ValueNode : ClassTag](args: T) = args match {
//    case _: ARGS => true
//    case _ => false
//  }
//}
//case object SORTBY extends FN[AttributeNode]("sortby")
//case object LIM extends FN[INumberNode]("lim")
//case object FILTER_FIELD extends FN[AttributeNode]("filter")
//case object FILTER_VAL extends FN[ArgsNode]("filter")
//object FN {
//  val all = Seq(SORTBY, LIM, FILTER_FIELD, FILTER_VAL)
//  def getByName(name: String): Seq[FN[_ <: ValueNode]] = all filter (_.name == name)
//  def exists(name: String) = !getByName(name).isEmpty
//
//  /** from a function name and set of arguments, tries to construct an instance of a matching function
//   *  @param name The function's name
//   *  @param args The set of arguments being applied
//   */
//  def reify[T <: ValueNode: ClassTag](name: String, args: T): Option[FN[T]] =
//    getByName(name) collectFirst {
//      case fn: FN[T] if fn typecheck args => fn
//    }
//}
//
///** grammar definitions
// *  TODO value classes
// *  TODO trailing spaces match zeroOrMore rules
// *  TODO completely uninformative custom action failures
// */
//object Reflask extends Parser {
//
//  // root rule
//  def Root = rule {
//    QueryType ~ WhiteSpace ~
//    optional(WrappedWord) ~ WhiteSpace ~
//    zeroOrMore(Function) ~ WhiteSpace ~~> QuestionNode ~ 
//    EOI
//  }
//  def QueryType = rule { Word ~~~? (w => Seq("company, entity") contains w.value) }
//
//  // function rules
//  def Function = rule {
//    FName ~ Value ~~~? (FN.reify(_, _) != None) ~~> (fnConstruct(_, _))
//  }
//  def fnConstruct[T <: ValueNode: ClassTag](fn: String, args: T): FunctionNode[T] =
//    FunctionNode(FN.reify(fn, args).get, args)
//
//  // word and word-group rules
//  def Phrase = rule { Word | WrappedWord }
//  def FName: Rule1[String] = rule { Word ~~> (w => w.value) ~~~? FN.exists }
//
//  // generic value rules
//  def Value: Rule1[ValueNode] = rule {
//    WhiteSpace ~ "(" ~ (Args | SimpleValue) ~ ")" |
//    SimpleValue
//  }
//  def Args = rule { Phrase ~ SimpleValue ~~> ArgsNode }
//  def SimpleValue = rule {
//    Phrase |
//    Real ~> (i => RNumberNode(BigDecimal(i.trim))) |
//    Integer ~> (i => INumberNode(i.trim.toInt))
//  }
//
//  def Word = rule { WhiteSpace ~ oneOrMore("a" - "z" | "A" - "Z" | ".,:;!?'-=+&/") ~> AttributeNode }
//  def WrappedWord = rule { WhiteSpace ~ "\"" ~ oneOrMore(NormalChar) ~> AttributeNode ~ "\"" }
//  def Character = rule { EscapedChar | NormalChar }
//  def EscapedChar = rule { "\\" ~ (anyOf("\"\\/bfnrt") | Unicode) }
//  def NormalChar = rule { !anyOf("\"\\") ~ ANY }
//  def WhiteSpace: Rule0 = rule { zeroOrMore(anyOf(" \n\r\t\f")) }
//
//  def Real = rule { Integer ~ Frac }
//  def Integer = rule { WhiteSpace ~ optional("-") ~ (("1" - "9") ~ Digits | Digit) }
//  def Frac = rule { "." ~ Digits }
//  def Digits = rule { oneOrMore(Digit) }
//  def Digit = rule { "0" - "9" }
//
//  def Unicode = rule { "u" ~ HexDigit ~ HexDigit ~ HexDigit ~ HexDigit }
//  def HexDigit = rule { "0" - "9" | "a" - "f" | "A" - "Z" }
//  def Exp = rule { ignoreCase("e") ~ optional(anyOf("+-")) ~ Digits }
//
//  /** We redefine the default string-to-rule conversion to also match trailing whitespace if the string ends with
//   *  a blank, this keeps the rules free from most whitespace matching clutter
//   */
//  override implicit def toRule(string: String) =
//    if (string endsWith " ") str(string.trim) ~ WhiteSpace
//    else str(string)
//
//  def parseQuestion(question: String): QuestionNode = {
//    /* other parse runners:  https://github.com/sirthias/parboiled/wiki/Parse-Error-Handling
//     *   BasicParseRunner - simple, no error handling
//     *   ReportingParseRunner - properly reports first error
//     *   TracingParseRunner - for debugging 
//     *   RecoveringParserRunner - intelligent recovery, to be tested
//     */
//    val parsingResult = ReportingParseRunner(Root) run question
//
//    parsingResult.result match {
//      case Some(astRoot) => astRoot
//      case None => throw new ParsingException("Invalid question source:\n"+ErrorUtils.printParseErrors(parsingResult))
//    }
//  }
//}

