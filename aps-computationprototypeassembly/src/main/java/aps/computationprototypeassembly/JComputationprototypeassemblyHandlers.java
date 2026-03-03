package aps.computationprototypeassembly;

import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.javadsl.ActorContext;

import csw.command.client.messages.TopLevelActorMessage;
import csw.framework.javadsl.JComponentHandlers;
import csw.framework.models.JCswContext;
import csw.location.api.models.TrackingEvent;
import csw.logging.api.javadsl.ILogger;
import csw.params.commands.*;
import csw.params.core.models.Id;
import csw.params.core.generics.Key;
import csw.params.javadsl.JKeyType;
import csw.params.core.models.ArrayData;
import csw.time.core.models.UTCTime;

import aps.computationprototypeassembly.worker.CommandWorker;
import aps.computationprototypeassembly.commands.*;

public class JComputationprototypeassemblyHandlers extends JComponentHandlers {

    private final JCswContext cswCtx;
    private final ILogger log;
    private final ActorContext<TopLevelActorMessage> actorContext;
    private final ActorRef<WorkerCommand> workerActor;

    public JComputationprototypeassemblyHandlers(ActorContext<TopLevelActorMessage> ctx, JCswContext cswCtx) {
        super(ctx, cswCtx);
        this.cswCtx = cswCtx;
        this.log = cswCtx.loggerFactory().getLogger(getClass());
        this.actorContext = ctx;

        System.loadLibrary("peas");

        // Spawn the new CommandWorker actor
        this.workerActor = actorContext.spawn(
                CommandWorker.create(cswCtx),
                "CommandWorker"
        );
    }

    // ================== Component Lifecycle Hooks ==================

    @Override
    public void initialize() {
        // Initialization logic if needed
    }

    @Override
    public void onShutdown() {
        // Cleanup logic if needed
    }

    @Override
    public void onLocationTrackingEvent(TrackingEvent trackingEvent) {
        // Handle location events if needed
    }

    @Override
    public void onGoOffline() { }

    @Override
    public void onGoOnline() { }

    @Override
    public void onDiagnosticMode(UTCTime startTime, String hint) { }

    @Override
    public void onOperationsMode() { }

    @Override
    public void onOneway(Id runId, ControlCommand controlCommand) { }

    // ================== Command Validation ==================

    @Override
    public CommandResponse.ValidateCommandResponse validateCommand(Id runId, ControlCommand controlCommand) {
        String commandName = controlCommand.commandName().name();

        switch (commandName) {
            case "colorStep", "ttOffsetsToActs", "decomposeActs":
                return new CommandResponse.Accepted(runId);
            default:
                return new CommandResponse.Invalid(
                        runId,
                        new CommandIssue.UnsupportedCommandIssue("Unsupported command: " + commandName)
                );
        }
    }

    // ================== Command Submission ==================

    @Override
    public CommandResponse.SubmitResponse onSubmit(Id runId, ControlCommand controlCommand) {

        log.info("Handling command: " + controlCommand.commandName());

        if (!(controlCommand instanceof Setup setup)) {
            return new CommandResponse.Error(runId, "Only Setup supported");
        }

        String commandName = controlCommand.commandName().name();

        switch (commandName) {

            case "colorStep" -> {


                workerActor.tell(new ExecuteColorStep(runId, controlCommand));

                return new CommandResponse.Started(runId);
            }

            case "ttOffsetsToActs" -> {
                workerActor.tell(new ExecuteTtOffsetsToActs(runId));
                return new CommandResponse.Started(runId);
            }

            case "decomposeActs" -> {
                workerActor.tell(new ExecuteDecomposeActs(runId));
                return new CommandResponse.Started(runId);
            }

            default -> {
                return new CommandResponse.Error(runId, "Unsupported command");
            }
        }
    }
}