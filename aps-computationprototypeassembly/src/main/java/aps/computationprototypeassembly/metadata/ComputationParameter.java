package aps.computationprototypeassembly.metadata;

public class ComputationParameter {

    public enum Source { CONFIGURATION, RESULTS }
    public enum Direction { INPUT, OUTPUT }

    public final String name;          // formal Fortran name
    public final Class<?> type;        // int, float, float[], etc.
    public final int[] dimensions;     // scalar = {}, 1D = {n}, 2D = {n,m}
    public final Source source;        // where to read input from
    public final Direction direction;  // INPUT or OUTPUT

    public ComputationParameter (
            String name,
            Class<?> type,
            int[] dimensions,
            Source source,
            Direction direction
    ) {
        this.name = name;
        this.type = type;
        this.dimensions = dimensions;
        this.source = source;
        this.direction = direction;
    }


}
