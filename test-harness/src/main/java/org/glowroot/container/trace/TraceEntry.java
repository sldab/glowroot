/*
 * Copyright 2013-2015 the original author or authors.
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

import java.util.List;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

import static org.glowroot.container.common.ObjectMappers.checkNotNullItems;
import static org.glowroot.container.common.ObjectMappers.nullToFalse;

public class TraceEntry {

    private final long offset;
    private final long duration;
    private final boolean active;
    private final int nestingLevel;
    // message is null for entries created via PluginServices.addErrorEntry()
    private final @Nullable Message message;
    private final @Nullable ErrorMessage error;
    private final @Nullable ImmutableList<String> stackTrace;
    private final boolean limitExceededMarker;
    private final boolean limitExtendedMarker;

    private TraceEntry(long offset, long duration, boolean active, int nestingLevel,
            @Nullable Message message, @Nullable ErrorMessage error,
            @Nullable List<String> stackTrace, boolean limitExceededMarker,
            boolean limitExtendedMarker) {
        this.offset = offset;
        this.duration = duration;
        this.active = active;
        this.nestingLevel = nestingLevel;
        this.message = message;
        this.error = error;
        this.stackTrace = stackTrace == null ? null : ImmutableList.copyOf(stackTrace);
        this.limitExceededMarker = limitExceededMarker;
        this.limitExtendedMarker = limitExtendedMarker;
    }

    public long getOffset() {
        if (limitExceededMarker) {
            throw new IllegalStateException("Limit exceeded marker has no offset,"
                    + " check isLimitExceededMarker() first");
        }
        if (limitExtendedMarker) {
            throw new IllegalStateException("Limit extended marker has no offset,"
                    + " check isLimitExtendedMarker() first");
        }
        return offset;
    }

    public long getDuration() {
        if (limitExceededMarker) {
            throw new IllegalStateException("Limit exceeded marker has no duration,"
                    + " check isLimitExceededMarker() first");
        }
        if (limitExtendedMarker) {
            throw new IllegalStateException("Limit extended marker has no duration,"
                    + " check isLimitExtendedMarker() first");
        }
        return duration;
    }

    public boolean isActive() {
        if (limitExceededMarker) {
            throw new IllegalStateException("Limit exceeded marker is neither active nor inactive,"
                    + " check isLimitExceededMarker() first");
        }
        if (limitExtendedMarker) {
            throw new IllegalStateException("Limit extended marker is neither active nor inactive,"
                    + " check isLimitExtendedMarker() first");
        }
        return active;
    }

    public int getNestingLevel() {
        if (limitExceededMarker) {
            throw new IllegalStateException("Limit exceeded marker has no nesting level,"
                    + " check isLimitExceededMarker() first");
        }
        if (limitExtendedMarker) {
            throw new IllegalStateException("Limit extended marker has no nesting level,"
                    + " check isLimitExtendedMarker() first");
        }
        return nestingLevel;
    }

    public @Nullable Message getMessage() {
        return message;
    }

    public @Nullable ErrorMessage getError() {
        return error;
    }

    public @Nullable ImmutableList<String> getStackTrace() {
        return stackTrace;
    }

    public boolean isLimitExceededMarker() {
        return limitExceededMarker;
    }

    public boolean isLimitExtendedMarker() {
        return limitExtendedMarker;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("offset", offset)
                .add("duration", duration)
                .add("active", active)
                .add("nestingLevel", nestingLevel)
                .add("message", message)
                .add("error", error)
                .add("stackTrace", stackTrace)
                .add("limitExceededMarker", limitExceededMarker)
                .add("limitExtendedMarker", limitExtendedMarker)
                .toString();
    }

    @JsonCreator
    static TraceEntry readValue(
            @JsonProperty("offset") @Nullable Long offset,
            @JsonProperty("duration") @Nullable Long duration,
            @JsonProperty("active") @Nullable Boolean active,
            @JsonProperty("nestingLevel") @Nullable Integer nestingLevel,
            @JsonProperty("message") @Nullable Message message,
            @JsonProperty("error") @Nullable ErrorMessage error,
            @JsonProperty("stackTrace") @Nullable List</*@Nullable*/String> uncheckedStackTrace,
            @JsonProperty("limitExceededMarker") @Nullable Boolean limitExceededMarker,
            @JsonProperty("limitExtendedMarker") @Nullable Boolean limitExtendedMarker)
                    throws JsonMappingException {
        List<String> stackTrace = checkNotNullItems(uncheckedStackTrace, "stackTrace");
        return new TraceEntry(nullToZero(offset), nullToZero(duration), nullToFalse(active),
                nullToZero(nestingLevel), message, error, stackTrace,
                nullToFalse(limitExceededMarker), nullToFalse(limitExtendedMarker));
    }

    private static long nullToZero(@Nullable Long value) {
        return value == null ? 0 : value;
    }

    private static int nullToZero(@Nullable Integer value) {
        return value == null ? 0 : value;
    }
}
