/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.operator.resource.roller;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.vertx.core.Future;

/**
 * Encapsulates the rolling algorithm in {@link #roll()}.
 */
public class KR2 {

    private final int numPods;
    final Observer observer;

    final CommandPlanner planner;
    final Processor processor;
    private final List<Context> contexts;

    public KR2(int numPods, Observer observer, Processor processor) {
        this.numPods = numPods;
        this.observer = observer;
        this.processor = processor;
        this.planner = new CommandPlannerImpl();
        this.contexts = IntStream.range(0, numPods).boxed()
                .map(Context::new)
                .collect(Collectors.toList());
    }

    Future<Void> roll() {
        while (true) {
            if (!processOne()) break;
        }
        // TODO close Admin client
        return null;
    }

    boolean processOne() {
        RollCommand command = observeAndPlanNextCommand();
        if (command.getCommand() == RollCommand.Command.END_ROLL) {
            return false;
        } else if (command.getCommand() == RollCommand.Command.PROCESS_CONTEXT){
            process(command.getContextToActOn());
        } else {
            throw new IllegalArgumentException("unknown roll command " + command.getCommand());
        }
        return true;
    }

    RollCommand observeAndPlanNextCommand() {
        var observations = observer.observe(contexts);
        contexts.forEach(c -> c.classify(observations.get(c.brokerId())));
        return planner.planNextCommand(contexts);
    }

    private boolean process(Context context) {
        switch (context.state()) {
            case STALE_RESTARTABLE:
            case UNHEALTHY:
                processor.deletePod(context.brokerId());
                processor.awaitStable(context.brokerId()); // TODO should this actually be a method of Observer?
                return true;
            case STALE_RECONFIGGABLE:
                processor.reconfigure(context.brokerId());
                processor.awaitStable(context.brokerId()); // TODO should this actually be a method of Observer? Do we need stable, or merely healthy?
                return true;
            case RESTARTED:
            case RECOVERY:
            case SYNCING:
                processor.awaitStable(context.brokerId()); // TODO should this actually be a method of Observer? Do we need stable, or merely healthy?
                return false;
            case STALE_BLOCKED:
            case UNKNOWN:
            case STABLE:
            default:
                throw new IllegalStateException("Unexpected state " + context);
        }
    }


}
