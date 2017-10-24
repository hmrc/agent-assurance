/*
 * Copyright 2017 HM Revenue & Customs
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

import java.net.{InetSocketAddress, URL}
import java.util.concurrent.TimeUnit.{MILLISECONDS, SECONDS}
import javax.inject.{Inject, Provider, Singleton}

import com.codahale.metrics.graphite.{Graphite, GraphiteReporter}
import com.codahale.metrics.{MetricFilter, SharedMetricRegistries}
import com.google.inject.AbstractModule
import com.google.inject.name.Names
import org.slf4j.MDC
import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Environment, Logger, Mode}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HttpGet
import uk.gov.hmrc.play.audit.http.config.{AuditingConfig, BaseUri, Consumer}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import wiring.WSVerbs

import scala.concurrent.{ExecutionContext, Future}

class Module(val environment: Environment, val configuration: Configuration) extends AbstractModule with ServicesConfig {

  def configure(): Unit = {
    lazy val appName = configuration.getString("appName").get
    lazy val loggerDateFormat: Option[String] = configuration.getString("logger.json.dateformat")

    Logger.info(s"Starting microservice : $appName : in mode : ${environment.mode}")
    MDC.put("appName", appName)
    loggerDateFormat.foreach(str => MDC.put("logger.json.dateformat", str))
    bind(classOf[AuditConnector]).to(classOf[MicroserviceAuditConnector])
    bind(classOf[GraphiteStartUp]).asEagerSingleton()
    bind(classOf[AuthConnector]).to(classOf[MicroserviceAuthConnector])
    bind(classOf[HttpGet]).toInstance(new WSVerbs()(configuration))
    bindBaseUrl("des")
    bindBaseUrl("government-gateway")
    bindProperty("des.environment", "des.environment")
    bindProperty("des.authorizationToken", "des.authorization-token")
    bindIntProperty("minimumIRPAYEClients", "minimumIRPAYEClients")
    bindIntProperty("minimumIRSAClients", "minimumIRSAClients")
  }

  private def bindBaseUrl(serviceName: String) =
    bind(classOf[URL]).annotatedWith(Names.named(s"$serviceName-baseUrl")).toProvider(new BaseUrlProvider(serviceName))

  private class BaseUrlProvider(serviceName: String) extends Provider[URL] {
    override lazy val get = new URL(baseUrl(serviceName))
  }

  private def bindProperty(objectName: String, propertyName: String) =
    bind(classOf[String]).annotatedWith(Names.named(objectName)).toProvider(new PropertyProvider(propertyName))

  private def bindIntProperty(objectName: String, propertyName: String) =
    bind(classOf[Int]).annotatedWith(Names.named(objectName)).toProvider(new PropertyIntProvider(propertyName))

  private class PropertyIntProvider(confKey: String) extends Provider[Int] {
    override lazy val get = getConfInt(confKey, throw new IllegalStateException(s"No value found for configuration property $confKey"))
  }

  private class PropertyProvider(confKey: String) extends Provider[String] {
    override lazy val get = getConfString(confKey, throw new IllegalStateException(s"No value found for configuration property $confKey"))
  }
}

@Singleton
class GraphiteStartUp @Inject()(val configuration: Configuration,
                                val environment: Environment,
                                lifecycle: ApplicationLifecycle,
                                implicit val ec: ExecutionContext) extends ServicesConfig {

  val metricsPluginEnabled: Boolean = getConfBool("metrics.enabled", default = false)
  val graphitePublisherEnabled: Boolean = getConfBool("microservice.metrics.graphite.enabled", default = false)

  if (metricsPluginEnabled && graphitePublisherEnabled) {
    val graphite = new Graphite(new InetSocketAddress(
      getConfString("microservice.metrics.graphite.host", "graphite"),
      getConfInt("microservice.metrics.graphite.port", 2003)))

    val prefix: String = getConfString("microservice.metrics.graphite.prefix", s"play.${configuration.getString("appName").get}")

    val registryName: String = getConfString("metrics.name", "default")

    val reporter: GraphiteReporter = GraphiteReporter.forRegistry(
      SharedMetricRegistries.getOrCreate(registryName))
      .prefixedWith(s"$prefix.${java.net.InetAddress.getLocalHost.getHostName}")
      .convertRatesTo(SECONDS)
      .convertDurationsTo(MILLISECONDS)
      .filter(MetricFilter.ALL)
      .build(graphite)

    Logger.info("Graphite metrics enabled, starting the reporter")
    reporter.start(getConfInt("microservice.metrics.graphite.interval", 10).toLong, SECONDS)

    lifecycle.addStopHook { () =>
      Future successful reporter.stop()
    }
  } else {
    Logger.warn(s"Graphite metrics disabled, plugin = $metricsPluginEnabled and publisher = $graphitePublisherEnabled")
  }

}

@Singleton
class MicroserviceAuditConnector @Inject()(val configuration: Configuration) extends AuditConnector {

  override def auditingConfig: AuditingConfig = configuration.getConfig("auditing") map { auditing =>
    val enabled = auditing.getBoolean("enabled").getOrElse(false)
    AuditingConfig(
      enabled = enabled,
      consumer = Some(auditing.getConfig("consumer").map { consumer =>
        Consumer(
          baseUri = consumer.getConfig("baseUri").map { uri =>
            BaseUri(
              host = uri.getString("host").getOrElse(throw new Exception("Missing consumer host for auditing")),
              port = uri.getInt("port").getOrElse(throw new Exception("Missing consumer port for auditing")),
              protocol = uri.getString("protocol").getOrElse("http")
            )
          }.getOrElse(throw new Exception("Missing consumer baseUri for auditing"))
        )
      }.getOrElse(throw new Exception("Missing consumer configuration for auditing")))
    )
  } getOrElse {
    AuditingConfig(consumer = None, enabled = false)
  }
}

trait ServicesConfig {

  def environment: Environment

  def configuration: Configuration

  val env: String = if (environment.mode == Mode.Test) "Test"
  else configuration.getString("run.mode").getOrElse("Dev")

  private val rootServices = "microservice.services"
  private val envServices = s"$env.microservice.services"
  private val playServices = s"govuk-tax.$env.services"

  private val defaultProtocol: String =
    configuration.getString(s"$rootServices.protocol")
      .getOrElse(configuration.getString(s"$envServices.protocol")
        .getOrElse("http"))

  def baseUrl(serviceName: String) = {
    val protocol = getConfString(s"$serviceName.protocol", defaultProtocol)
    val host = getConfString(s"$serviceName.host", throw new RuntimeException(s"Could not find config $serviceName.host"))
    val port = getConfInt(s"$serviceName.port", throw new RuntimeException(s"Could not find config $serviceName.port"))
    s"$protocol://$host:$port"
  }

  private def keys(confKey: String) = Seq(
    s"$rootServices.$confKey",
    s"$envServices.$confKey",
    confKey,
    s"$env.$confKey",
    s"$playServices.$confKey"
  )

  private def read[A](confKey: String)(f: String => Option[A]): Option[A] =
    keys(confKey).foldLeft[Option[A]](None)((a,k) => a.orElse(f(k)))

  def getConfString(confKey: String, default: => String): String =
    read[String](confKey)(configuration.getString(_)).getOrElse(default)

  def getConfInt(confKey: String, default: => Int): Int =
    read[Int](confKey)(configuration.getInt(_)).getOrElse(default)

  def getConfBool(confKey: String, default: => Boolean): Boolean =
    read[Boolean](confKey)(configuration.getBoolean(_)).getOrElse(default)

}