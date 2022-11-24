/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.operator.resource.roller;

import io.fabric8.kubernetes.api.model.Pod;
import io.strimzi.operator.cluster.model.RestartReasons;
import io.strimzi.operator.common.Reconciliation;
import io.strimzi.operator.common.ReconciliationLogger;
import io.vertx.core.Future;

import java.util.function.Function;

class StateMachineKafkaRoller implements KafkaRoller {
    private static final ReconciliationLogger LOGGER = ReconciliationLogger.create(StateMachineKafkaRoller.class);

    private final Reconciliation reconciliation;

    public StateMachineKafkaRoller(Reconciliation reconciliation) {
        this.reconciliation = reconciliation;
    }

    @Override
    public Future<Void> rollingRestart(Function<Pod, RestartReasons> podNeedsRestart) {
        LOGGER.debugCr(reconciliation, "No-op");
        return Future.succeededFuture();
    }
}
