package uk.gov.hmrc.proxibility

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.util.control.Exception._


object Runner {
  
  val interceptedPages = new QueueStream
  
  def initShutdownHandler(): Unit = {
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run() {
        println("Ctrl-c caught, shutting down...")
        interceptedPages.put(StopMessage)
        Thread.sleep(2000)
      }
    })
  }
  
  def startProcessingHtmlPages(): Unit = {
    val runTime = DateTime.now
    val runStamp = DateTimeFormat.forPattern("yyyyMMddHHmmss").print(runTime)

    //Run page interceptor in worker thread
    future {
      //Consume from interceptedPages until ctrl-c causes end of stream
      val pages = for(page <- interceptedPages) yield {
        ReportWriter.createAccessibilityReport(runStamp, page)
        page
      }
      ReportWriter.createReportWrapper(pages.toSet, runStamp, runTime)
    }
  }
  
  def main(args: Array[String]): Unit = {
    
    initShutdownHandler()
    startProcessingHtmlPages()
    
    //Run proxy server in main thread (blocking call)
    val port = allCatch.opt(args(1).toInt).getOrElse(8080)
    HttpProxyServerFactory.buildHtmlInterceptingProxy(port, interceptedPages.put).start()
  }
}
