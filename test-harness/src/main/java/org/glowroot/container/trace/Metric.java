/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glowroot.container.trace;

import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Longs;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.glowroot.common.ObjectMappers.nullToEmpty;
import static org.glowroot.container.common.ObjectMappers.checkRequiredProperty;

/**
 * @author Trask Stalnaker
 * @since 0.5
 */
@Immutable
public class Metric {

    static final Ordering<Metric> orderingByTotal = new Ordering<Metric>() {
        @Override
        public int compare(@Nullable Metric left, @Nullable Metric right) {
            checkNotNull(left);
            checkNotNull(right);
            return Longs.compare(left.total, right.total);
        }
    };

    private final String name;
    private final long total;
    private final long min;
    private final long max;
    private final long count;
    private final boolean active;
    private final boolean minActive;
    private final boolean maxActive;

    private final ImmutableList<Metric> nestedMetrics;

    private Metric(String name, long total, long min, long max, long count, boolean active,
            boolean minActive, boolean maxActive, List<Metric> nestedMetrics) {
        this.name = name;
        this.total = total;
        this.min = min;
        this.max = max;
        this.count = count;
        this.active = active;
        this.minActive = minActive;
        this.maxActive = maxActive;
        this.nestedMetrics = ImmutableList.copyOf(nestedMetrics);
    }

    public String getName() {
        return name;
    }

    public long getTotal() {
        return total;
    }

    public long getMin() {
        return min;
    }

    public long getMax() {
        return max;
    }

    public long getCount() {
        return count;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isMinActive() {
        return minActive;
    }

    public boolean isMaxActive() {
        return maxActive;
    }

    public ImmutableList<Metric> getNestedMetrics() {
        return getStableAndOrderedMetrics();
    }

    // the glowroot weaving metric is a bit unpredictable since tests are often run inside the
    // same GlowrootContainer for test speed, so test order affects whether any classes are
    // woven during the test or not
    // it's easiest to just ignore this metric completely
    //
    // suppress warning for checker framework issue #312
    @SuppressWarnings("type.argument.type.incompatible")
    private ImmutableList<Metric> getStableAndOrderedMetrics() {
        List<Metric> stableMetrics = Lists.newArrayList(nestedMetrics);
        for (Iterator<Metric> i = stableMetrics.iterator(); i.hasNext();) {
            if ("glowroot weaving".equals(i.next().getName())) {
                i.remove();
            }
        }
        return ImmutableList.copyOf(Metric.orderingByTotal.reverse().sortedCopy(stableMetrics));
    }

    @JsonIgnore
    public List<String> getNestedMetricNames() {
        List<String> stableMetricNames = Lists.newArrayList();
        for (Metric stableMetric : getStableAndOrderedMetrics()) {
            stableMetricNames.add(stableMetric.getName());
        }
        return stableMetricNames;
    }

    /*@Pure*/
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("name", name)
                .add("total", total)
                .add("min", min)
                .add("max", max)
                .add("count", count)
                .add("active", active)
                .add("minActive", minActive)
                .add("maxActive", maxActive)
                .add("nestedMetrics", nestedMetrics)
                .toString();
    }

    @JsonCreator
    static Metric readValue(
            @JsonProperty("name") @Nullable String name,
            @JsonProperty("total") @Nullable Long total,
            @JsonProperty("min") @Nullable Long min,
            @JsonProperty("max") @Nullable Long max,
            @JsonProperty("count") @Nullable Long count,
            @JsonProperty("active") @Nullable Boolean active,
            @JsonProperty("minActive") @Nullable Boolean minActive,
            @JsonProperty("maxActive") @Nullable Boolean maxActive,
            @JsonProperty("nestedMetrics") @Nullable List<Metric> nestedMetrics)
            throws JsonMappingException {
        checkRequiredProperty(name, "name");
        checkRequiredProperty(total, "total");
        checkRequiredProperty(min, "min");
        checkRequiredProperty(max, "max");
        checkRequiredProperty(count, "count");
        checkRequiredProperty(active, "active");
        checkRequiredProperty(minActive, "minActive");
        checkRequiredProperty(maxActive, "maxActive");
        return new Metric(name, total, min, max, count, active, minActive, maxActive,
                nullToEmpty(nestedMetrics));
    }
}
