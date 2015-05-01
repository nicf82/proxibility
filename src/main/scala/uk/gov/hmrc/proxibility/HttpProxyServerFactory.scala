package uk.gov.hmrc.proxibility

import org.apache.log4j.Logger
import org.littleshoot.proxy._
import org.littleshoot.proxy.impl.DefaultHttpProxyServer

/**
 * Created by nic on 29/04/2015.
 */
object HttpProxyServerFactory {
  
  val logger = Logger.getLogger(getClass)
  
  def buildHtmlInterceptingProxy(port: Int, handler: InterceptedHtmlPageMessage => Unit): HttpProxyServerBootstrap = {
    DefaultHttpProxyServer.bootstrap()
      .withPort(port)
      .withFiltersSource(new HtmlInterceptingHttpFiltersSourceAdapter(handler))
  }
}
