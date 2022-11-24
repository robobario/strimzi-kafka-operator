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

/**
 * <p>A temporary KafkaRoller implementation to enable us to safely re-implement KafkaRoller</p>
 * <p>The roller will:</p>
 * <pre>
 * 1. invoke the roll on the new implementation
 * 2. invoke the roll on the deprecated implementation (regardless of the outcome of the new roll)
 * </pre>
 * <p>
 * This means we can incrementally build the new implementation because the deprecated roll should
 * eventually become a no-op. The new roller should have brought the cluster up to stable.
 * </p>
 */
class MigrationKafkaRoller implements KafkaRoller {

    private static final ReconciliationLogger LOGGER = ReconciliationLogger.create(MigrationKafkaRoller.class);

    private final Reconciliation reconciliation;
    private final KafkaRoller newRoller;
    private final KafkaRoller deprecatedRoller;

    public MigrationKafkaRoller(Reconciliation reconciliation, KafkaRoller newRoller, KafkaRoller deprecatedRoller) {
        this.reconciliation = reconciliation;
        this.newRoller = newRoller;
        this.deprecatedRoller = deprecatedRoller;
    }

    @Override
    public Future<Void> rollingRestart(Function<Pod, RestartReasons> podNeedsRestart) {
        try {
            Future<Void> future = newRoller.rollingRestart(podNeedsRestart);
            return future.compose(ignored -> {
                LOGGER.debugCr(reconciliation, "new roller completed successfully, invoking deprecated roll");
                return deprecatedRoller.rollingRestart(podNeedsRestart);
            }, failure -> {
                LOGGER.errorCr(reconciliation, "new roller future completed exceptionally, invoking deprecated roll", failure);
                return deprecatedRoller.rollingRestart(podNeedsRestart);
            });
        } catch (Exception e) {
            LOGGER.errorCr(reconciliation, "Failure while composing roll future");
            return Future.failedFuture(e);
        }
    }

}
