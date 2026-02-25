package aps.computationprototypeassembly.commands;

import csw.params.commands.Result;
import csw.params.core.models.Id;
import aps.computationprototypeassembly.AlgorithmLibrary;

public interface WorkerCommand {

    Id runId();

    Result execute(AlgorithmLibrary library) throws Exception;
}
