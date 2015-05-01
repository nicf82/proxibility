package uk.gov.hmrc.proxibility

import org.apache.log4j.Logger

trait Logging {
  val logger = Logger.getLogger(getClass)
}
