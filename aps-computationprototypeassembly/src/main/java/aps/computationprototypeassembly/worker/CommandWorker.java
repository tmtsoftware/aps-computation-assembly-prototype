package aps.computationprototypeassembly.worker;


import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import aps.computationprototypeassembly.commands.WorkerCommand;
import csw.framework.models.JCswContext;
import csw.params.commands.CommandResponse;
import csw.params.commands.Result;
import aps.computationprototypeassembly.AlgorithmLibrary;

public class CommandWorker {

    public static Behavior<WorkerCommand> create(JCswContext cswCtx) {

        return Behaviors.receive(WorkerCommand.class)
                .onMessage(WorkerCommand.class,
                        command -> execute(cswCtx, command))
                .build();
    }

    private static Behavior<WorkerCommand> execute(
            JCswContext cswCtx,
            WorkerCommand command) {

        try {
            AlgorithmLibrary library = new AlgorithmLibrary(cswCtx);

            double t1 = System.nanoTime();
            Result result = command.execute(library);
            double t2 = System.nanoTime();

            cswCtx.loggerFactory()
                    .getLogger(CommandWorker.class)
                    .info("Finished in " + (t2 - t1) / 1_000_000.0 + " ms");

            cswCtx.commandResponseManager()
                    .updateCommand(
                            new CommandResponse.Completed(
                                    command.runId(),
                                    result));

        } catch (Exception e) {
            cswCtx.commandResponseManager()
                    .updateCommand(
                            new CommandResponse.Error(
                                    command.runId(),
                                    e.getMessage()));
        }

        return Behaviors.same();
    }
}
