package aps.computationclient

import java.net.InetAddress
import org.apache.pekko.actor.typed.{ActorSystem, Behavior, SpawnProtocol}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.util.Timeout

import csw.command.api.scaladsl.CommandService
import csw.command.client.CommandServiceFactory
import csw.location.api.models.ComponentType.Assembly
import csw.location.api.models.{ComponentId, Connection, HttpLocation, TrackingEvent, LocationRemoved, LocationUpdated}
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.logging.client.scaladsl.{GenericLoggerFactory, LoggingSystemFactory}
import csw.params.commands.{CommandName, Setup}
import csw.params.core.generics.{Key, KeyType}
import csw.prefix.models.Prefix
import csw.location.api.models.Connection.HttpConnection
import scala.concurrent.duration._
import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

object ComputationClient extends App {

  // ===============================
  // Actor system & logging
  // ===============================
  implicit val system: ActorSystem[SpawnProtocol.Command] =
    ActorSystem(SpawnProtocol(), "ComputationClient")
  implicit val ec: ExecutionContextExecutor = system.executionContext
  implicit val timeout: Timeout             = Timeout(5.seconds)

  val host = InetAddress.getLocalHost.getHostName
  LoggingSystemFactory.start("ComputationClientApp", "0.1", host, system)
  val log = GenericLoggerFactory.getLogger

  log.info("Starting ComputationClient")

  // ===============================
  // Location Service client
  // ===============================
  val locationService = HttpLocationServiceFactory.makeLocalClient

  val assemblyId = ComponentId(Prefix("APS.computationPrototypeAssembly"), Assembly)

  val connection = HttpConnection(assemblyId)

  // Resolve the assembly
  locationService.resolve(connection, 5.seconds).onComplete {
    case Success(Some(httpLoc: HttpLocation)) =>
      log.info(s"Resolved assembly at ${httpLoc.uri}")
      sendCommand(httpLoc)

    case Success(Some(other)) =>
      log.warn(s"Resolved a non-HTTP location: $other")

    case Success(None) =>
      log.error("Assembly not found in Location Service")

    case Failure(ex) =>
      log.error("Failed to resolve assembly", Map.empty, ex)
  }

  // ===============================
  // Send command
  // ===============================
  private def sendCommand(loc: HttpLocation): Unit = {
    log.info(s"Sending command to ${loc.uri}")

    val assembly: CommandService = CommandServiceFactory.make(loc)(system)

    val axisKey: Key[Char]  = KeyType.CharKey.make("axis")
    val countsKey: Key[Int] = KeyType.IntKey.make("counts")

    val setup =
      Setup(Prefix("csw.test.client"), CommandName("setRelTarget"), None)
        .add(axisKey.set('A'))
        .add(countsKey.set(2))

    assembly.submit(setup).onComplete {
      case Success(resp) =>
        log.info(s"✅ Response received from assembly: $resp")

      case Failure(ex) =>
        log.error("Command submission failed", Map.empty, ex)
    }
  }
}
