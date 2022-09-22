/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.common;

import io.fabric8.kubernetes.api.model.LabelSelector;
import io.strimzi.operator.common.metrics.OperatorMetricsHolder;
import io.strimzi.operator.common.model.NamespaceAndName;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Abstraction of an operator which is driven by resources of a given {@link #kind()}.
 *
 * {@link #reconcile(Reconciliation)} triggers the asynchronous reconciliation of a named resource.
 * Reconciliation of a given resource may be triggered either by a Kubernetes watch event (see {@link OperatorWatcher}) or
 * on a regular schedule.
 * {@link #reconcileAll(String, String, Handler)} triggers reconciliation of all the resources that the operator consumes.
 * An operator instance is not bound to a particular namespace. Rather the namespace is passed as a parameter.
 */
public interface Operator {

    Comparator<NamespaceAndName> BY_NAMESPACE_THEN_NAME = Comparator.comparing(NamespaceAndName::getNamespace).thenComparing(NamespaceAndName::getName);


    /**
     * The Kubernetes kind of the resource "consumed" by this operator
     * @return The kind.
     */
    String kind();

    /**
     * Returns the operator metrics holder which is used to hold the operator metrics
     *
     * @return  Metrics holder instance
     */
    OperatorMetricsHolder metrics();

    /**
     * Reconcile the resource identified by the given reconciliation.
     * @param reconciliation The resource.
     * @return A Future is completed once the resource has been reconciled.
     */
    Future<Void> reconcile(Reconciliation reconciliation);

    private Future<Void> delayedReconcile(Reconciliation reconciliation, long delayMillis) {
        if (delayMillis == 0) {
            return reconcile(reconciliation);
        }
        final Promise<Void> promise = Promise.promise();
        Vertx.vertx().setTimer(delayMillis, id -> {
            try {
                reconcile(reconciliation).onComplete(voidAsyncResult -> {
                    if (voidAsyncResult.failed()) {
                        promise.fail(voidAsyncResult.cause());
                    } else {
                        promise.complete();
                    }
                });
            } catch (Exception e) {
                promise.fail(e);
            }
        });
        return promise.future();
    }

    /**
     * Triggers the asynchronous reconciliation of all resources which this operator consumes.
     * The resources to reconcile are identified by {@link #allResourceNames(String)}.
     * @param trigger The cause of this reconciliation (for logging).
     * @param namespace The namespace to reconcile, or {@code *} to reconcile across all namespaces.
     * @param handler Handler called on completion.
     */
    default void reconcileAll(String trigger, String namespace, Handler<AsyncResult<Void>> handler) {
        reconcileAll(trigger, namespace, handler, 0);
    }

    /**
     * Triggers the asynchronous reconciliation of all resources which this operator consumes.
     * The resources to reconcile are identified by {@link #allResourceNames(String)}.
     * @param trigger The cause of this reconciliation (for logging).
     * @param namespace The namespace to reconcile, or {@code *} to reconcile across all namespaces.
     * @param handler Handler called on completion.
     * @param spreadOverMillis spread reconciliation of resources evenly over this duration.
     */
    default void reconcileAll(String trigger, String namespace, Handler<AsyncResult<Void>> handler, long spreadOverMillis) {
        allResourceNames(namespace).onComplete(ar -> {
            if (ar.succeeded()) {
                reconcileThese(trigger, ar.result(), namespace, handler, spreadOverMillis);
                metrics().periodicReconciliationsCounter(namespace).increment();
            } else {
                handler.handle(ar.map((Void) null));
            }
        });
    }

    default void reconcileThese(String trigger, Set<NamespaceAndName> desiredNames, String namespace, Handler<AsyncResult<Void>> handler) {
        reconcileThese(trigger, desiredNames, namespace, handler, 0);
    }

    default void reconcileThese(String trigger, Set<NamespaceAndName> desiredNames, String namespace, Handler<AsyncResult<Void>> handler, long spreadOverMillis) {
        if (namespace.equals("*")) {
            metrics().resetResourceAndPausedResourceCounters();
        } else {
            metrics().resourceCounter(namespace).set(0);
            metrics().pausedResourceCounter(namespace).set(0);
        }

        if (desiredNames.size() > 0) {
            @SuppressWarnings({ "rawtypes" }) // Has to use Raw type because of the CompositeFuture
            List<Future> futures = new ArrayList<>();
            final List<NamespaceAndName> ordered = desiredNames.stream().sorted(BY_NAMESPACE_THEN_NAME).collect(Collectors.toList());
            final int size = ordered.size();
            final long delayPerItem = spreadOverMillis / size;
            for (int i = 0; i < size; i++) {
                final NamespaceAndName resourceRef = ordered.get(i);
                metrics().resourceCounter(resourceRef.getNamespace()).getAndIncrement();
                Reconciliation reconciliation = new Reconciliation(trigger, kind(), resourceRef.getNamespace(), resourceRef.getName());
                futures.add(delayedReconcile(reconciliation, i * delayPerItem));
            }
            CompositeFuture.join(futures).map((Void) null).onComplete(handler);
        } else {
            handler.handle(Future.succeededFuture());
        }
    }

    /**
     * Returns a future which completes with the names of all the resources to be reconciled by
     * {@link #reconcileAll(String, String, Handler)}.
     *
     * @param namespace The namespace
     * @return The set of resource names
     */
    Future<Set<NamespaceAndName>> allResourceNames(String namespace);

    /**
     * A selector for narrowing the resources which this operator instance consumes to those whose labels match this selector.
     * @return A selector.
     */
    default Optional<LabelSelector> selector() {
        return Optional.empty();
    }
}
