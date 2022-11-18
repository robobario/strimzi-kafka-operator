package io.strimzi.operator.cluster.operator.resource.roller;

public class RollCommand {

    public enum Command {
        DELAY,
        PROCESS_CONTEXT,
        END_ROLL
    }

    private final Command command;
    private final Context contextToActOn;

    public RollCommand(Command command, Context contextToActOn) {
        this.command = command;
        this.contextToActOn = contextToActOn;
    }

    public Command getCommand() {
        return command;
    }

    public Context getContextToActOn() {
        return contextToActOn;
    }
}

