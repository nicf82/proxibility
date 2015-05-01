package uk.gov.hmrc.proxibility


import java.io.File

import scala.sys.process._

object AccessibilityTool extends Logging {

  def runAccessibilityReport(htmlFile: File): String = {
    Process(Seq("phantomjs", "audit.js", htmlFile.getAbsolutePath), new File("src/main/resources")).!!
  }
}
