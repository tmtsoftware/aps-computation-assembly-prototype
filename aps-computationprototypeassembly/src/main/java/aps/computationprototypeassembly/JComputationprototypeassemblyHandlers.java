package aps.computationprototypeassembly;

import org.apache.pekko.actor.typed.javadsl.ActorContext;
import csw.command.client.messages.TopLevelActorMessage;
import csw.framework.javadsl.JComponentHandlers;
import csw.framework.models.JCswContext;
import csw.location.api.models.TrackingEvent;
import csw.logging.api.javadsl.ILogger;
import csw.params.commands.CommandResponse;
import csw.params.commands.ControlCommand;
import csw.time.core.models.UTCTime;
import csw.params.core.models.Id;

import org.tmt.aps.peas.lang.interop.JcolorStep;
import org.tmt.aps.peas.lang.interop.RetVal;

import java.util.concurrent.CompletableFuture;

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

    JComputationprototypeassemblyHandlers(ActorContext<TopLevelActorMessage> ctx,JCswContext cswCtx) {
        super(ctx, cswCtx);
        this.cswCtx = cswCtx;
        this.log = cswCtx.loggerFactory().getLogger(getClass());
    }

    @Override
    public void initialize() {

        log.info("Initializing computationPrototypeAssembly...");
        System.loadLibrary("peas");

        AlgorithmLibrary algorithmLibrary = new AlgorithmLibrary();

        int stepCount = 11;
        float stepSizeMicrons = 20.0f;
        float stepSizeNm = stepSizeMicrons * 1000.0f;
        RetVal retVal = new RetVal();

        log.info("computation.starting: colorStep");

        try {

            double t1 = System.nanoTime();
            float[][] colorsteps = algorithmLibrary.colorStep(stepCount, stepSizeNm);
            double t2 = System.nanoTime();

            for (int i=0; i<stepCount+1; i++){
                for (int j = 0; j < 3; j++){
                    log.info("i,j = " + i + "," + j + "   colorstep = " + colorsteps[i][j]);
                }
            }
            log.info("computation.success colorStep: time to execute = " + (t2-t1)/1000000.0 + " ms");

        } catch (Exception e) {
            log.error(e.getMessage());
        }





    }

    @Override
    public void onShutdown() {
    }

    @Override
    public void onLocationTrackingEvent(TrackingEvent trackingEvent) {

    }

    @Override
    public CommandResponse.ValidateCommandResponse validateCommand(Id runId, ControlCommand controlCommand) {
        return new CommandResponse.Accepted(runId);
    }

    @Override
    public CommandResponse.SubmitResponse onSubmit(Id runId, ControlCommand controlCommand) {
        return new CommandResponse.Completed(runId);
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
