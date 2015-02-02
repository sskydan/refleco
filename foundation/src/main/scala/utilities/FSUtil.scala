package utilities

import java.nio.file.Files
import java.nio.charset.Charset
import java.io.File
import java.nio.file.Paths
import java.nio.ByteBuffer

/** Filesystem utilities
 */
object FSUtil {

  /** Generates a stream of all files which are children of the provided file
   *  @note Streams are memoized by default, if the head of the stream is kept around
   *  TODO test
   *  @param base a File representing the folder to start at
   *  @return a Stream representing all the files found
   */
  def getFileTree(base: File): Stream[File] =
    if (base.isDirectory) base.listFiles().toStream flatMap(getFileTree)
    else base #:: Stream.empty

  /** Read a file from the filesystem wholly
   *  TODO is this best?
   *  @param file the file to read
   *  @return a String holding the entire contents of the file
   */
  def readFile(file: File): String = {
    val encoded = Files.readAllBytes(Paths.get(file.getPath()))
    Charset.defaultCharset().decode(ByteBuffer.wrap(encoded)).toString()
  }

}