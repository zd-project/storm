/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.  The ASF licenses this file to you under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package org.apache.storm.metric;

import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Reservoir;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.storm.daemon.metrics.MetricsUtils;
import org.apache.storm.daemon.metrics.reporters.PreparableReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormMetricsRegistry extends MetricRegistry {
    private static final StormMetricsRegistry DEFAULT_REGISTRY = new StormMetricsRegistry();
    private static final Logger LOG = LoggerFactory.getLogger(StormMetricsRegistry.class);

    private StormMetricsRegistry() {/*Singleton pattern*/}

    public static <V> Gauge<V> registerGauge(final String name, final Gauge<V> gauge) {
        return DEFAULT_REGISTRY.register(name, gauge);
    }

    public static Histogram registerHistogram(String name) {
        return registerHistogram(name, new ExponentiallyDecayingReservoir());
    }

    public static Histogram registerHistogram(String name, Reservoir reservoir) {
        return DEFAULT_REGISTRY.register(name, new Histogram(reservoir));
    }

    public static Meter registerMeter(String name) {
        return DEFAULT_REGISTRY.register(name, new Meter());
    }

    /**
     * Start metrics reporters for the registry singleton.
     *
     * @param topoConf config that specifies reporter plugin
     */
    public static void startMetricsReporters(Map<String, Object> topoConf) {
        for (PreparableReporter reporter : MetricsUtils.getPreparableReporters(topoConf)) {
            reporter.prepare(StormMetricsRegistry.DEFAULT_REGISTRY, topoConf);
            reporter.start();
            LOG.info("Started statistics report plugin...");
        }
    }

    public static String name(String prefix, String name) {
        assert name != null;
        return StringUtils.isEmpty(prefix) ? name : prefix + ':' + name;
    }

    public static String name(Class<?> klass, String names) {
        return name(klass.getSimpleName().toLowerCase(), names);
    }

    @Override
    //This is more similar to super#getOrAdd than super#register
    public <T extends Metric> T register(final String name, T metric) throws IllegalArgumentException {
        try {
            return super.register(name, metric);
        } catch (IllegalArgumentException e) {
            @SuppressWarnings("unchecked")
            final T existing = (T) DEFAULT_REGISTRY.getMetrics().get(name);
            if (metric.getClass().isInstance(existing)) {
                LOG.warn("Metric {} has already been registered", name);
                return existing;
            }
            throw e;
        }
    }
}
