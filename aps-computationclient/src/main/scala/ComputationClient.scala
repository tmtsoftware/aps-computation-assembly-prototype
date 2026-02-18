package aps.computationclient

import java.net.InetAddress
import org.apache.pekko.actor.typed.{ActorSystem, Behavior, SpawnProtocol}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.util.Timeout
import csw.command.api.scaladsl.CommandService
import csw.command.client.CommandServiceFactory
import csw.location.api.models.ComponentType.Assembly
import csw.location.api.models.{ComponentId, Connection, HttpLocation, LocationRemoved, LocationUpdated, TrackingEvent}
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.logging.client.scaladsl.{GenericLoggerFactory, LoggingSystemFactory}
import csw.params.commands.{CommandName, CommandResponse, ControlCommand, Setup}
import csw.params.core.generics.{Key, KeyType}
import csw.prefix.models.Prefix
import csw.location.api.models.Connection.HttpConnection
import csw.params.commands.CommandResponse.{Cancelled, Completed, Invalid, Locked, Started, SubmitResponse, Error}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}
import scala.concurrent.Future

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

    val stepCountKey = KeyType.IntKey.make("stepCount")
    val stepSizeKey  = KeyType.FloatKey.make("stepSizeMicrons")

    val setup =
      Setup(
        Prefix("aps.computationprototypeassembly"),
        CommandName("colorStep"),
        None
      )
        .add(stepCountKey.set(11))
        .add(stepSizeKey.set(20.0f))

    val immediateCommandF: Future[SubmitResponse] = for {
      response <- assembly.submitAndWait(setup)
    } yield response match {
      case completed: Completed =>
        log.info(s"Command completed successfully: $completed")
        completed
      case started: Started =>
        log.info(s"Command started: $started")
        started
      case invalid: Invalid =>
        log.error(s"Command invalid: $invalid")
        invalid
      case error: Error =>
        log.error(s"Command failed: $error")
        error
      case cancelled: Cancelled =>
        log.warn(s"Command cancelled: $cancelled")
        cancelled
      case locked: Locked =>
        log.warn(s"Command locked: $locked")
        locked
    }
  }
}
