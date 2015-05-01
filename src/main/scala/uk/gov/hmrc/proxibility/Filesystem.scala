package uk.gov.hmrc.proxibility

import java.io.{File, FileWriter}

import org.apache.commons.io.FileUtils

object Filesystem {

  def withFileWriter(directory: String, filename: String)(block: FileWriter => Unit): File = {
    val dir = new File(directory)
    FileUtils.forceMkdir(dir)
    val file = new File(dir, filename)
    val out = new FileWriter(file)
    block(out)
    out.close()
    file
  }
}
