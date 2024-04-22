/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test.uk.gov.hmrc.agentassurance.support

import java.net.ServerSocket
import java.net.URL

import scala.annotation.tailrec

import com.github.tomakehurst.wiremock.client.WireMock.configureFor
import com.github.tomakehurst.wiremock.client.WireMock.reset
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.WireMockServer
import org.scalatest.BeforeAndAfterAll
import org.scalatest.BeforeAndAfterEach
import org.scalatest.Suite
import play.api.Logging

case class WireMockBaseUrl(value: URL)

object WireMockSupport {
  // We have to make the wireMockPort constant per-JVM instead of constant
  // per-WireMockSupport-instance because config values containing it are
  // cached in the GGConfig object
  private lazy val wireMockPort = Port.randomAvailable
}

trait WireMockSupport extends BeforeAndAfterAll with BeforeAndAfterEach {
  me: Suite =>

  val wireMockPort: Int                                           = WireMockSupport.wireMockPort
  val wireMockHost                                                = "localhost"
  val wireMockBaseUrlAsString                                     = s"http://$wireMockHost:$wireMockPort"
  val wireMockBaseUrl                                             = new URL(wireMockBaseUrlAsString)
  protected implicit val implicitWireMockBaseUrl: WireMockBaseUrl = WireMockBaseUrl(wireMockBaseUrl)

  protected def basicWireMockConfig(): WireMockConfiguration = wireMockConfig()

  private val wireMockServer = new WireMockServer(basicWireMockConfig().port(wireMockPort))

  protected override def beforeAll(): Unit = {
    super.beforeAll()
    configureFor(wireMockHost, wireMockPort)
    wireMockServer.start()
  }

  protected override def afterAll(): Unit = {
    wireMockServer.stop()
    super.afterAll()
  }

  protected override def beforeEach(): Unit = {
    super.beforeEach()
    wireMockServer.resetAll()
    reset()
  }
}

// This class was copy-pasted from the hmrctest project, which is now deprecated.
object Port extends Logging {
  val rnd       = new scala.util.Random
  val range     = 8000 to 39999
  val usedPorts = List[Int]()

  @tailrec
  def randomAvailable: Int =
    range(rnd.nextInt(range.length)) match {
      case 8080 => randomAvailable
      case 8090 => randomAvailable
      case p: Int => {
        available(p) match {
          case false => {
            logger.debug(s"Port $p is in use, trying another")
            randomAvailable
          }
          case true => {
            logger.debug("Taking port : " + p)
            usedPorts :+ p
            p
          }
        }
      }
    }

  private def available(p: Int): Boolean = {
    var socket: ServerSocket = null
    try {
      if (!usedPorts.contains(p)) {
        socket = new ServerSocket(p)
        socket.setReuseAddress(true)
        true
      } else {
        false
      }
    } catch {
      case t: Throwable => false
    } finally {
      if (socket != null) socket.close()
    }
  }
}
