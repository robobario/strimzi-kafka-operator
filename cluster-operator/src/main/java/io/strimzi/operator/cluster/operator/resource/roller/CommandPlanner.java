package io.strimzi.operator.cluster.operator.resource.roller;

import java.util.Collection;

public interface CommandPlanner {

    RollCommand planNextCommand(Collection<Context> contexts);
}
