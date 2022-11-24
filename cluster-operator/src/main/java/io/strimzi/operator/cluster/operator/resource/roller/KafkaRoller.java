/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.operator.resource.roller;

import io.fabric8.kubernetes.api.model.Pod;
import io.strimzi.operator.cluster.model.RestartReasons;
import io.vertx.core.Future;

import java.util.function.Function;

public interface KafkaRoller {
    Future<Void> rollingRestart(Function<Pod, RestartReasons> podNeedsRestart);
}
