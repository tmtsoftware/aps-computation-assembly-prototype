package aps.computationprototypeassembly;

import csw.config.client.javadsl.JConfigClientFactory;
import csw.config.api.javadsl.IConfigClientService;

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

import csw.config.api.ConfigData;
import com.typesafe.config.Config;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.List;
import csw.config.models.ConfigFileInfo;

import aps.computationprototypeassembly.worker.CommandWorker;
import aps.computationprototypeassembly.commands.*;

public class JComputationprototypeassemblyHandlers extends JComponentHandlers {

    private final JCswContext cswCtx;
    private final ILogger log;
    private final ActorContext<TopLevelActorMessage> actorContext;
    private final ActorRef<WorkerCommand> workerActor;

    private IConfigClientService configClient;

    public JComputationprototypeassemblyHandlers(ActorContext<TopLevelActorMessage> ctx, JCswContext cswCtx) {
        super(ctx, cswCtx);
        this.cswCtx = cswCtx;
        this.log = cswCtx.loggerFactory().getLogger(getClass());
        this.actorContext = ctx;

        System.loadLibrary("peas");

        configClient = cswCtx.configClientService();


        // Spawn the new CommandWorker actor
        this.workerActor = actorContext.spawn(
                CommandWorker.create(cswCtx),
                "CommandWorker"
        );
    }

    // ================== Component Lifecycle Hooks ==================

    @Override
    public void initialize() {
        IConfigClientService configClient = cswCtx.configClientService();

        HoconReader hoconReader = new HoconReader();

        String[] filenames = {
                // ── 15 APS DB generator output files ───────────────────────────────────────────────────
                "APS_DB_FS_6_rectangular_array",
                "APS_DB_FS_6_rings_global_xy_scaled",
                "APS_DB_FS_6_rings_local_xy_unscaled",
                "APS_DB_M1CS_modes",
                "APS_DB_PH_2_periph_subaps_vs_segment",
                "APS_DB_PH_params_2_subap",
                "APS_DB_PH_triangles",
                "APS_DB_PH_closure_triples",
                "APS_DB_emult_sing_values",
                "APS_DB_given_1_subaps_find_corres_2",
                "APS_DB_given_2_subaps_find_corres_1",
                "APS_DB_optimal_FI_freqs_8192_6_rings",
                "APS_DB_ref_def_tmt_8192_FS_6",
                "APS_DB_segment_colors",
                "APS_DB_segment_sensor_numbers_2_subap",
                // ── 6 APS DB generator input files likely to be used in fortran ─────────────────────────
                "APS_DB_actuators_xy",
                "APS_DB_seg_ctrs",
                "APS_DB_sensor_data",
                "APS_DB_ungapped_vertices",
                "APS_DB_m1cs_sensor_numbering",
                "APS_DB_Amatrix_Leff"
        };
        for (String filename : filenames) {
            Path filePath = Paths.get("tmt/aps/db/" + filename + ".conf");
            try {
                Optional<ConfigData> maybeConfigData = configClient.getActive(filePath).get();
                Config config = maybeConfigData
                        .orElseThrow(() -> new RuntimeException("Config file not found: " + filePath))
                        .toJConfigObject(actorContext.getSystem())
                        .get();
                hoconReader.readFile(config, filename);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Failed to load: " + filePath, e);
            }
        }
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
                workerActor.tell(new ExecuteTtOffsetsToActs(runId, controlCommand));
                return new CommandResponse.Started(runId);
            }

            case "decomposeActs" -> {
                workerActor.tell(new ExecuteDecomposeActs(runId, controlCommand));
                return new CommandResponse.Started(runId);
            }

            default -> {
                return new CommandResponse.Error(runId, "Unsupported command");
            }
        }
    }
}