package uk.gov.hmrc.proxibility

import org.joda.time.DateTime
import Filesystem._

object ReportWriter extends Logging {


  def createAccessibilityReport(runStamp: String, interceptedHtmlPage: InterceptedHtmlPageMessage): Unit = {

    val htmlFile = withFileWriter("proxibility-reports/"+runStamp, interceptedHtmlPage.hash+"-page.html") { writer =>
      writer.write(interceptedHtmlPage.body)
    }

    val output = AccessibilityTool.runAccessibilityReport(htmlFile)

    val reportFile = withFileWriter("proxibility-reports/"+runStamp, interceptedHtmlPage.hash+"-report.html") { writer =>
      writer.write {
        s"""<pre>$output</pre>"""
      }
    }

    val framesFile = withFileWriter("proxibility-reports/"+runStamp, interceptedHtmlPage.hash+"-frames.html") { writer =>
      writer.write {
        s"""<frameset rows="50%,50%">
          |  <frame src="${interceptedHtmlPage.hash}-page.html" name="page">
          |  <frame src="${interceptedHtmlPage.hash}-report.html" name="report">
          |</frameset>
        """.stripMargin
      }
    }

    logger.info(s"Wrote report file $reportFile")
  }
  
  
  def createReportWrapper(pages: Set[InterceptedHtmlPageMessage], runStamp: String, runTime: DateTime): Unit = {
    
    val head =
      s"""<html>
        |  <head>
        |    <title>Accessibility Report - $runTime</title>
        |  </head>
        |  <body>
        |    <h1>Accessibility Report - $runTime</h1>
        |    <hr>
        |    <ul>""".stripMargin
    
    val body = pages map { p =>
      s"""<li><a href="${p.hash}-frames.html" target="report">${p.uri}</a></li>"""
    } mkString("\n")
    
    val foot =
      """    </ul>
        |  </body>
        |</html>""".stripMargin
    
    withFileWriter("proxibility-reports/"+runStamp, "nav.html") { writer =>
      writer.write( Seq(head, body, foot).mkString("\n") )
    }
    
    val frames =
      """<frameset cols="25%,*">
        |  <frame src="nav.html">
        |  <frame name="report">
        |</frameset>
      """.stripMargin

    withFileWriter("proxibility-reports/"+runStamp, "report.html") { writer =>
      writer.write( frames )
    }
  }
}
