package utilities

import java.io.File
import testbase.UnitSpec

class TestFSUtil extends UnitSpec {

  "Filereader" should "read file correctly" in {
    val sample = "Just some sample text.\n\n\t Wonderful stuff"
    val f = new File("FSUtil_Test.txt")  
     
    val pw = new java.io.PrintWriter(f)
    try pw.write(sample) finally pw.close()
    
    val read = FSUtil.readFile(f)
    
    assert(sample == read)
    
    f.delete()
  }
  
}