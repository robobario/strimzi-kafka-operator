package io.strimzi.operator.cluster.operator.resource.roller;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

public class CommandPlannerImpl implements CommandPlanner {
    @Override
    public RollCommand planNextCommand(Collection<Context> contexts) {
        Optional<Context> nextToActOn = contexts.stream().min(contextPriority());
        if(nextToActOn.isEmpty()){
            throw new IllegalStateException("No context selected to act on");
        }
        Context nextContext = nextToActOn.get();
        var state = nextContext.state();
        if (state.isHealthy()) {
            // Even the worst state is healthy => We're done
            return new RollCommand(RollCommand.Command.END_ROLL, null);
        } else if (state == State.STALE_BLOCKED) {
            return new RollCommand(RollCommand.Command.DELAY, null);
        } else {
            return new RollCommand(RollCommand.Command.PROCESS_CONTEXT, nextContext);
        }
    }

    private static Comparator<Context> contextPriority() {
        return Comparator.comparing((Context ctx) -> ctx.state().priority())
                .thenComparing(Context::brokerId);
    }
}
