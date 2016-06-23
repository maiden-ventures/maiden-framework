package maiden.common

import java.io._
import scala.io.{Source, Codec}

object FileReader {

  def read(name: String) =
    Source.fromFile(name)(Codec.UTF8).mkString

  def filesAt(f: File): Array[File] = {
    if (f.isDirectory) {
      f.listFiles flatMap filesAt
    } else {
      Array(f)
    }
  }

  def filesAtWithExtension(dir: String, ext: String): Array[File] = {
    filesAt(new File(dir)).filter( f =>
      f.getPath.toLowerCase.endsWith(ext.toLowerCase)
    )
  }

  def readAll(directory: String, keyFunc: (String) => String, matchFunc: (String) => Boolean) = {
    val dir = new File(directory)
    val files = filesAt(dir).filter(f => matchFunc(f.toString))
    files.par.map(f =>
      keyFunc(f.toString) -> {
        try {
          Source.fromFile(f)(Codec.UTF8).mkString
        } catch {
          case e: Exception => "ERROR WITH " + f
        }
      }
    ).toMap
  }

  def readAllCompressed(directory: String, keyFunc: (String) => String) = {
    val dir = new File(directory)
    val files = filesAt(dir)
    files.par.map(f =>
      keyFunc(f.toString) -> Source.fromFile(f).mkString
                                      .replace('\n', ' ')
                                      .replace('\t', ' ')
                                      .replace("  ", " ")
    ).toMap
  }
}


object FileWriter {


	def write(data: Array[Byte], fileName: String) {
    val out = Some(new FileOutputStream(fileName))
		out match {
			case Some(o) => try {
				o.write(data)
      } catch {
        case e: Exception => throw(new Exception("Unable to write file"))
			} finally {
        o.close
      }
			case _ => throw(new Exception("Unable to write file"))
		}
	}

  def write(s: String, fileName: String) {
    val bytes = s.getBytes
    write(bytes, fileName)
  }
}
