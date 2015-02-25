package dlx

import testbase.UnitSpec
import DLX._

class TestDLX extends UnitSpec {

	/* R  A B C D E F G
	 * 1  0 0 1 0 1 1 0
	 * 2  1 0 0 1 0 0 1
	 * 3  0 1 1 0 0 1 0
	 * 4  1 0 0 1 0 0 0
	 * 5  0 1 0 0 0 0 1
	 * 6  0 0 0 1 1 0 1
	 */
  val matrix =
    Seq(
      Seq(0,1,0,1,0,0),
      Seq(0,0,1,0,1,0),
      Seq(1,0,1,0,0,0),
      Seq(0,1,0,1,0,1),
      Seq(1,0,0,0,0,1),
      Seq(1,0,1,0,0,0),
      Seq(0,1,0,0,1,1)
    )
  val matrixNames = Seq("A","B","C","D","E","F","G")
  
  val constructorMatrix = Seq(
    Seq(1,1,0,0,0,0),
    Seq(0,0,0,0,1,1),
    Seq(0,0,0,1,1,0),
    Seq(1,1,1,0,0,0),
    Seq(0,0,1,1,0,0),
    Seq(0,0,0,1,1,0),
    Seq(1,0,1,0,1,1)
  ).transpose
  val constructorMatrixNames = List("A","B","C","D","E","F")
  
  "QLMatrix simple matrix" should "generate from dense matrix" in {
    val test = QLMatrix(constructorMatrix, constructorMatrixNames, new QuadNode(_))
    val root = test.root
    
    assert(root.traverseRem((h:QuadHeader) => h.r)(_.name) == constructorMatrixNames)
    assert(root.traverseRem((_:QuadHeader).r)(_.size) == List(3,2,3,3,4,2))
    assert(root.r.r.dn.r.dn.r.dn.l.up.r.r.up.up.l.c.r.r.r == root)
  }
  
  "QLMatrix simple matrix" should "generate from sparse matrix" in {
    val testM = Seq(
      Seq("A","B"),
      Seq("E","F"),
      Seq("D","E"),
      Seq("A","B","C"),
      Seq("C","D"),
      Seq("D","E"),
      Seq("A","C","E","F")
    )
    val test = QLMatrix.fromSparse(testM, constructorMatrixNames, new QuadNode(_))
    val root = test.root
    
    assert(root.traverseRem((h:QuadHeader) => h.r)(_.name) == constructorMatrixNames)
    assert(root.traverseRem((_:QuadHeader).r)(_.size) == List(3,2,3,3,4,2))
    assert(root.r.r.dn.r.dn.r.dn.l.up.r.r.up.up.l.c.r.r.r == root)
  }
  
  "DLX search" should "find correct solution" in {
    val test = QLMatrix(matrix, matrixNames, new QuadNode(_))
    val expected = List(List(List("A", "D"), List("B", "G"), List("C", "E", "F")))
    
    assert(expected == test.solve())
  }
  
  "DLX search" should "find correct solution with custom nodes" in {
    class DummyQN(c:QuadHeader) extends UntestedQuadNode[String, DummyQN](c) {
      val execute: () => String = () => "complete"
      val evaluate: String => Boolean = _ == "complete"
    }

    val test = QLMatrix(matrix, matrixNames, new DummyQN(_))
    val expected = List(List(List("A", "D"), List("B", "G"), List("C", "E", "F")))
    
    assert(expected == test.solve())
  }
}