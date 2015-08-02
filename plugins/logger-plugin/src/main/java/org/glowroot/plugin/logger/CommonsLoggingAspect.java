/*
 * Copyright 2014-2015 the original author or authors.
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
package org.glowroot.plugin.logger;

import javax.annotation.Nullable;

import org.glowroot.plugin.api.ErrorMessage;
import org.glowroot.plugin.api.MessageSupplier;
import org.glowroot.plugin.api.PluginServices;
import org.glowroot.plugin.api.TimerName;
import org.glowroot.plugin.api.TraceEntry;
import org.glowroot.plugin.api.weaving.BindMethodName;
import org.glowroot.plugin.api.weaving.BindParameter;
import org.glowroot.plugin.api.weaving.BindTraveler;
import org.glowroot.plugin.api.weaving.IsEnabled;
import org.glowroot.plugin.api.weaving.OnAfter;
import org.glowroot.plugin.api.weaving.OnBefore;
import org.glowroot.plugin.api.weaving.Pointcut;

public class CommonsLoggingAspect {

    private static final String TIMER_NAME = "logging";

    private static final PluginServices pluginServices = PluginServices.get("logger");

    @Pointcut(className = "org.apache.commons.logging.Log", methodName = "warn|error|fatal",
            methodParameterTypes = {"java.lang.Object"}, timerName = TIMER_NAME)
    public static class LogAdvice {
        private static final TimerName timerName =
                pluginServices.getTimerName(LogAdvice.class);
        @IsEnabled
        public static boolean isEnabled() {
            return !LoggerPlugin.inAdvice() && pluginServices.isEnabled();
        }
        @OnBefore
        public static TraceEntry onBefore(@BindParameter @Nullable Object message,
                @BindMethodName String methodName) {
            LoggerPlugin.inAdvice(true);
            String messageText = String.valueOf(message);
            if (LoggerPlugin.markTraceAsError(methodName.equals("warn"), false)) {
                pluginServices.setTransactionError(ErrorMessage.from(messageText));
            }
            return pluginServices.startTraceEntry(
                    MessageSupplier.from("log {}: {}", methodName, messageText), timerName);
        }
        @OnAfter
        public static void onAfter(@BindTraveler TraceEntry traceEntry,
                @BindParameter @Nullable Object message) {
            LoggerPlugin.inAdvice(false);
            traceEntry.endWithError(ErrorMessage.from(String.valueOf(message)));
        }
    }

    @Pointcut(className = "org.apache.commons.logging.Log", methodName = "warn|error|fatal",
            methodParameterTypes = {"java.lang.Object", "java.lang.Throwable"},
            timerName = TIMER_NAME)
    public static class LogWithThrowableAdvice {
        private static final TimerName timerName =
                pluginServices.getTimerName(LogWithThrowableAdvice.class);
        @IsEnabled
        public static boolean isEnabled() {
            return !LoggerPlugin.inAdvice() && pluginServices.isEnabled();
        }
        @OnBefore
        public static TraceEntry onBefore(@BindParameter @Nullable Object message,
                @BindParameter @Nullable Throwable t,
                @BindMethodName String methodName) {
            LoggerPlugin.inAdvice(true);
            String messageText = String.valueOf(message);
            if (LoggerPlugin.markTraceAsError(methodName.equals("warn"), t != null)) {
                pluginServices.setTransactionError(ErrorMessage.from(messageText, t));
            }
            return pluginServices.startTraceEntry(
                    MessageSupplier.from("log {}: {}", methodName, messageText), timerName);
        }
        @OnAfter
        public static void onAfter(@BindParameter @Nullable Object message,
                @BindParameter @Nullable Throwable t, @BindTraveler TraceEntry traceEntry) {
            LoggerPlugin.inAdvice(false);
            if (t == null) {
                traceEntry.endWithError(ErrorMessage.from(String.valueOf(message)));
            } else {
                traceEntry.endWithError(ErrorMessage.from(t.getMessage(), t));
            }
        }
    }
}
