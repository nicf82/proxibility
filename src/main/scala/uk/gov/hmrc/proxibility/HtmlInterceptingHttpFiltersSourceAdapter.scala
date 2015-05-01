package uk.gov.hmrc.proxibility

import java.util.regex.Pattern

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http._
import io.netty.util.CharsetUtil
import org.apache.commons.codec.digest.DigestUtils
import org.littleshoot.proxy.{HttpFiltersAdapter, HttpFilters, HttpFiltersSourceAdapter}

import scala.collection.mutable
import scala.collection.JavaConversions._

class HtmlInterceptingHttpFiltersSourceAdapter(interceptHandler: InterceptedHtmlPageMessage => Unit) extends HttpFiltersSourceAdapter {
  
  case class RequestData(contentType: String, bodyAccumulator: mutable.StringBuilder)
  
  val buffers = mutable.Map[HttpRequest, RequestData]()
  
  override def filterRequest(originalRequest: HttpRequest, ctx: ChannelHandlerContext): HttpFilters = {
    
    new HttpFiltersAdapter(originalRequest) {

      //Handles chunks of an http response, we need to re-build the entire response by intercepting the chunks in this order:
      // HttpResponse (Contains response headers only)
      // HttpContent(s) (May be 0 or more)
      // LastHttpContent (Signifies the end of the response)
      //We re-assemble the response by building up a buffer in the buffers map, keyed on the original request
      //associated with the response
      override def responsePre(httpObject: HttpObject): HttpObject = {
        
        httpObject match {
          case response: HttpResponse =>
            extractContentType(response).filter(_.toLowerCase.indexOf("text/html") >= 0) map { ct =>
              buffers(originalRequest) = RequestData(ct, mutable.StringBuilder.newBuilder)
            }
            
          case content: LastHttpContent =>
            buffers.get(originalRequest) map { rd =>
              rd.bodyAccumulator.append(content.content().toString(CharsetUtil.UTF_8))
              buffers -= originalRequest
              val body = addingBaseTag(rd.bodyAccumulator.toString(), originalRequest.getUri)
              val hash = DigestUtils.md5Hex(body)
              interceptHandler( InterceptedHtmlPageMessage(originalRequest.getUri,
                originalRequest.getMethod.name, rd.contentType, body, hash) )
            }
            
          case content: HttpContent =>
            buffers.get(originalRequest).map { rd =>
              rd.bodyAccumulator.append(content.content().toString(CharsetUtil.UTF_8))
              buffers(originalRequest) = rd
            }
            
          case _ =>
        }

        httpObject
      }
    }
  }

  private def extractContentType(response: HttpResponse): Option[String] =
    response.headers().entries().filter(_.getKey.toLowerCase.indexOf("content-type") >= 0).map(_.getValue).headOption

  private def addingBaseTag(html: String, uri: String) = {
    Pattern.compile( """<head>""", Pattern.CASE_INSENSITIVE)
      .matcher(html)
      .replaceFirst( """<head><base href="""" + uri + """">""")
  }
}