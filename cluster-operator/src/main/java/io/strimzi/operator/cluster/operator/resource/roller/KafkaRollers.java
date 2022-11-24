/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.operator.resource.roller;

import io.fabric8.kubernetes.api.model.Secret;
import io.strimzi.operator.cluster.model.KafkaVersion;
import io.strimzi.operator.cluster.operator.resource.events.KubernetesRestartEventPublisher;
import io.strimzi.operator.common.AdminClientProvider;
import io.strimzi.operator.common.BackOff;
import io.strimzi.operator.common.Reconciliation;
import io.strimzi.operator.common.operator.resource.PodOperator;
import io.vertx.core.Vertx;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class KafkaRollers {

    @SuppressWarnings({"ParameterNumber", "deprecation"})
    public static KafkaRoller migrationRoller(Reconciliation reconciliation, Vertx vertx, PodOperator podOperations,
                                              long pollingIntervalMs, long operationTimeoutMs, Supplier<BackOff> backOffSupplier, List<String> podList,
                                              Secret clusterCaCertSecret, Secret coKeySecret,
                                              AdminClientProvider adminClientProvider,
                                              Function<Integer, String> kafkaConfigProvider, String kafkaLogging, KafkaVersion kafkaVersion, boolean allowReconfiguration, KubernetesRestartEventPublisher eventsPublisher) {
        QueuingKafkaRoller deprecatedRoller = new QueuingKafkaRoller(reconciliation, vertx, podOperations, pollingIntervalMs, operationTimeoutMs, backOffSupplier, podList, clusterCaCertSecret, coKeySecret, adminClientProvider, kafkaConfigProvider, kafkaLogging, kafkaVersion, allowReconfiguration, eventsPublisher);
        StateMachineKafkaRoller newRoller = new StateMachineKafkaRoller(reconciliation);
        return new MigrationKafkaRoller(reconciliation, newRoller, deprecatedRoller);
    }

}
