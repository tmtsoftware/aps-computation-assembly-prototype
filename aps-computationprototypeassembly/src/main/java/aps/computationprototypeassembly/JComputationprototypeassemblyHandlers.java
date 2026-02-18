package aps.computationprototypeassembly;

import org.apache.pekko.actor.typed.javadsl.ActorContext;
import csw.command.client.messages.TopLevelActorMessage;
import csw.framework.javadsl.JComponentHandlers;
import csw.framework.models.JCswContext;
import csw.location.api.models.TrackingEvent;
import csw.logging.api.javadsl.ILogger;
import csw.params.commands.CommandResponse;
import csw.params.commands.ControlCommand;
import csw.params.commands.Result;
import csw.time.core.models.UTCTime;
import csw.params.core.models.Id;

import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import csw.params.commands.CommandIssue;
import csw.params.commands.Setup;
import csw.params.core.generics.Key;
import csw.params.core.generics.Parameter;
import csw.params.javadsl.JKeyType;

import csw.params.core.models.ArrayData;

import org.tmt.aps.peas.lang.interop.JcolorStep;
import org.tmt.aps.peas.lang.interop.RetVal;

import java.util.concurrent.CompletableFuture;
import java.util.Arrays;
import java.util.List;

/**
 * Domain specific logic should be written in below handlers.
 * This handlers gets invoked when component receives messages/commands from other component/entity.
 * For example, if one component sends Submit(Setup(args)) command to Computationprototypehcd,
 * This will be first validated in the supervisor and then forwarded to Component TLA which first invokes validateCommand hook
 * and if validation is successful, then onSubmit hook gets invoked.
 * You can find more information on this here : https://tmtsoftware.github.io/csw/commons/framework.html
 */
public class JComputationprototypeassemblyHandlers extends JComponentHandlers {

    private final JCswContext cswCtx;
    private final ILogger log;

    private final ActorContext<TopLevelActorMessage> actorContext;
    private final ActorRef<WorkerCommand> workerActor;


    JComputationprototypeassemblyHandlers(ActorContext<TopLevelActorMessage> ctx, JCswContext cswCtx) {
        super(ctx, cswCtx);
        this.cswCtx = cswCtx;
        this.log = cswCtx.loggerFactory().getLogger(getClass());
        this.actorContext = ctx;

        System.loadLibrary("peas");

        this.workerActor = createWorkerActor();
    }

    // ================= Worker Actor =================

    private interface WorkerCommand { }

    private static final class ExecuteColorStep implements WorkerCommand {
        private final Id runId;
        private final int stepCount;
        private final float stepSizeNm;

        private ExecuteColorStep(Id runId, int stepCount, float stepSizeNm) {
            this.runId = runId;
            this.stepCount = stepCount;
            this.stepSizeNm = stepSizeNm;
        }
    }
    private static final class ExecuteTtOffsetsToActs implements WorkerCommand {
        private final Id runId;
        private ExecuteTtOffsetsToActs(Id runId) {
            this.runId = runId;
        }
    }

    private ActorRef<WorkerCommand> createWorkerActor() {
        return actorContext.spawn(
                Behaviors.receiveMessage(msg -> {

                    if (msg instanceof ExecuteColorStep exec) {

                        log.info("Worker: Starting colorStep computation");

                        try {
                            AlgorithmLibrary algorithmLibrary = new AlgorithmLibrary();

                            double t1 = System.nanoTime();
                            float[][] colorsteps =
                                    algorithmLibrary.colorStep(exec.stepCount, exec.stepSizeNm);
                            double t2 = System.nanoTime();

                            int rows = colorsteps.length;
                            int cols = colorsteps[0].length;
                            float[] flattened = new float[rows * cols];

                            int index = 0;
                            for (int r = 0; r < rows; r++) {
                                for (int c = 0; c < cols; c++) {
                                    flattened[index++] = colorsteps[r][c];
                                }
                            }

                            Float[] boxed = new Float[flattened.length];
                            for (int i = 0; i < flattened.length; i++) {
                                boxed[i] = flattened[i];
                            }

                            ArrayData<Float> arrayData = ArrayData.fromArray(boxed);
                            var floatArrayKey = JKeyType.FloatArrayKey().make("flattened");
                            Parameter<ArrayData<Float>> param = floatArrayKey.set(arrayData);

                            Result result = new Result().add(param);

                            CommandResponse.Completed response =
                                    new CommandResponse.Completed(exec.runId, result);

                            log.info("Worker: Computation finished in "
                                    + (t2 - t1) / 1_000_000.0 + " ms");

                            cswCtx.commandResponseManager().updateCommand(response);

                        } catch (Exception e) {
                            log.error("Worker error: " + e.getMessage());
                            cswCtx.commandResponseManager()
                                    .updateCommand(new CommandResponse.Error(exec.runId, e.getMessage()));
                        }

                    } else if (msg instanceof ExecuteTtOffsetsToActs exec) {

                        log.info("Worker: Starting ttOffsetsToActs computation");

                        try {
                            AlgorithmLibrary algorithmLibrary = new AlgorithmLibrary();
                            Configuration configuration = new Configuration();
                            Results results = new Results();

                            double t1 = System.nanoTime();
                            algorithmLibrary.ttOffsetsToActs(configuration, results);
                            double t2 = System.nanoTime();

                            Result result = new Result();

                            CommandResponse.Completed response =
                                    new CommandResponse.Completed(exec.runId, result);

                            log.info("Worker: Computation finished in "
                                    + (t2 - t1) / 1_000_000.0 + " ms");

                            cswCtx.commandResponseManager().updateCommand(response);

                        } catch (Exception e) {
                            log.error("Worker error: " + e.getMessage());
                            cswCtx.commandResponseManager()
                                    .updateCommand(new CommandResponse.Error(exec.runId, e.getMessage()));
                        }
                    }

                    // ✅ This must be outside the if/else chain
                    return Behaviors.same();
                }),
                "CommandWorker"
        );
    }



    @Override
    public void initialize() {


    }

    @Override
    public void onShutdown() {
    }

    @Override
    public void onLocationTrackingEvent(TrackingEvent trackingEvent) {

    }

    @Override
    public CommandResponse.ValidateCommandResponse validateCommand(Id runId, ControlCommand controlCommand) {

        String commandName = controlCommand.commandName().name();

        if (commandName.equals("colorStep")) {
            return new CommandResponse.Accepted(runId);
        }
        if (commandName.equals("ttOffsetsToActs")) {
            return new CommandResponse.Accepted(runId);
        }

        return new CommandResponse.Invalid(
                runId,
                new CommandIssue.UnsupportedCommandIssue("Unsupported command: " + commandName)
        );
    }

    @Override
    public CommandResponse.SubmitResponse onSubmit(Id runId, ControlCommand controlCommand) {

        log.info("Handling command: " + controlCommand.commandName());

        if (controlCommand instanceof Setup setup) {

            String commandName = controlCommand.commandName().name();

            if (commandName.equals("colorStep")) {
                Key<Integer> stepCountKey =
                        csw.params.javadsl.JKeyType.IntKey().make("stepCount");

                Key<Float> stepSizeKey =
                        csw.params.javadsl.JKeyType.FloatKey().make("stepSizeMicrons");

                var stepSizeParam = setup.jGet(stepSizeKey);
                var stepCountParam = setup.jGet(stepCountKey);

                if (stepCountParam.isEmpty() || stepSizeParam.isEmpty()) {
                    return new CommandResponse.Error(runId,
                            "Missing required parameters: stepCount or stepSizeMicrons");
                }

                int stepCount = stepCountParam.get().head();
                float stepSizeMicrons = stepSizeParam.get().head();
                float stepSizeNm = stepSizeMicrons * 1000.0f;

                log.info("Received parameters: stepCount=" + stepCount +
                        ", stepSizeMicrons=" + stepSizeMicrons);

                workerActor.tell(
                        new ExecuteColorStep(runId, stepCount, stepSizeNm)
                );

                return new CommandResponse.Started(runId);

            } else if (commandName.equals("ttOffsetsToActs")) {

                workerActor.tell(new ExecuteTtOffsetsToActs(runId));

                return new CommandResponse.Started(runId);
            }

        }
        return new CommandResponse.Error(runId, "Only Setup supported");
    }

    @Override
    public void onOneway(Id runId, ControlCommand controlCommand) {

    }

    @Override
    public void onGoOffline() {

    }

    @Override
    public void onGoOnline() {

    }

    @Override
    public void onDiagnosticMode(UTCTime startTime,String hint){

    }

    @Override
    public void onOperationsMode(){

    }
}
