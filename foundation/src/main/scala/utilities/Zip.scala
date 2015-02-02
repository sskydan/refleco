package utilities

import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.GZIPInputStream;
import net.lingala.zip4j.core.ZipFile
import net.lingala.zip4j.exception.ZipException

/**
 * Zip utilities
 * TODO make more scala-y
 */
object Zip {
  
  def unzipGzip(input:String, output:String) = {
    var buffer = Array.fill[Byte](1024)(0)
 
    try {
      println(input)
    	val gzis = new GZIPInputStream(new FileInputStream(input))
    	val out = new FileOutputStream(output)
 
    	var len = gzis.read(buffer)
    	while (len > 0) {
    		out.write(buffer, 0, len)
    		len = gzis.read(buffer)
    	}
 
    	gzis.close()
    	out.close()
 
    } catch {
      case ex:IOException => ex.printStackTrace()
    }
  }
  
  def unzipZip(input:String, output:String) = {
   try {
     val zipFile = new ZipFile(input);
//     if (zipFile.isEncrypted()) {
//        zipFile.setPassword(password);
//     }
     
     zipFile.extractAll(output);
    } catch {
      case ex:ZipException => ex.printStackTrace()
    }
  }
  
}
