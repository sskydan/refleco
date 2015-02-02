package dsl

import testbase.UnitSpec

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
  
  "QLMatrix matrix construction" should "generate from dense matrix" in {
    val testM = Seq(
      Seq(1,1,0,0,0,0),
      Seq(0,0,0,0,1,1),
      Seq(0,0,0,1,1,0),
      Seq(1,1,1,0,0,0),
      Seq(0,0,1,1,0,0),
      Seq(0,0,0,1,1,0),
      Seq(1,0,1,0,1,1)
    ).transpose
    val testMNames = List("A","B","C","D","E","F")
    val test = QLMatrix.construct(testM, testMNames)
    val root = test.root 
    
    assert(root.traverseN(_.r){ case header:QuadHeader => header.name } == testMNames)
    assert(root.traverseN(_.r){ case header:QuadHeader => header.size } == List(3,2,3,3,4,2))
    assert(root.r.r.dn.r.dn.r.dn.l.up.r.r.up.up.l.c.r.r.r == root)
  }
  
  "DLX search" should "find correct solution" in {
    val test = QLMatrix.construct(matrix, matrixNames)
    val expected = List(List(List("A", "D"), List("B", "G"), List("C", "E", "F")))
    
    assert(expected == DLX(test).solutions)
  }
  
}