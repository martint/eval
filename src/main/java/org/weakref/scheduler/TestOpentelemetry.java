/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.weakref.scheduler;

import io.airlift.node.NodeInfo;
import io.airlift.tracing.OpenTelemetryConfig;
import io.airlift.tracing.OpenTelemetryModule;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

public class TestOpentelemetry
{
    public static void main(String[] args)
            throws ExecutionException, InterruptedException
    {
        OpenTelemetryModule module = new OpenTelemetryModule("test", "1");
        OpenTelemetry openTelemetry = module.createOpenTelemetry(new NodeInfo("testing"), new OpenTelemetryConfig());

        Tracer tracer = openTelemetry.getTracer("test");

        try (ScopedSpan scopedSpan = ScopedSpan.scopedSpan(tracer, "t")) {
            scopedSpan.span().addEvent("start");
            Thread.sleep(100);
            scopedSpan.span().addEvent("wait");
            Thread.sleep(200);
            scopedSpan.span().addEvent("continue");
            Thread.sleep(200);
        }

        try (ScopedSpan parent = ScopedSpan.scopedSpan(tracer, "parent")) {
            parent.span().addEvent("start");

            new Thread(() -> {
                Span threadSpan = tracer.spanBuilder("thread1")
                        .setParent(Context.current().with(parent.span()))
                        .startSpan();

                Span previous = null;

                for (int i = 0; i < 10; i++) {
                    SpanBuilder builder = tracer.spanBuilder("child1")
                            .setParent(Context.current().with(threadSpan));

                    if (previous != null) {
//                        builder.addLink(previous.getSpanContext());
                    }

                    Span span = builder.startSpan();
                    try (ScopedSpan child = ScopedSpan.scopedSpan(span)) {
                        previous = child.span();
                        try {
                            Thread.sleep(ThreadLocalRandom.current().nextInt(200));
                        }
                        catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    try {
                        Thread.sleep(ThreadLocalRandom.current().nextInt(200));
                    }
                    catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                threadSpan.end();
                
            }).start();

            new Thread(() -> {
                Span threadSpan = tracer.spanBuilder("thread2")
                        .setParent(Context.current().with(parent.span()))
                        .startSpan();

                Span previous = null;

                for (int i = 0; i < 10; i++) {
                    SpanBuilder builder = tracer.spanBuilder("child2")
                            .setParent(Context.current().with(threadSpan));

                    if (previous != null) {
//                        builder.addLink(previous.getSpanContext());
                    }

                    Span span = builder.startSpan();
                    try (ScopedSpan child = ScopedSpan.scopedSpan(span)) {
                        previous = child.span();
                        try {
                            Thread.sleep(ThreadLocalRandom.current().nextInt(200));
                        }
                        catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    try {
                        Thread.sleep(ThreadLocalRandom.current().nextInt(200));
                    }
                    catch (InterruptedException e) {

                        throw new RuntimeException(e);
                    }
                }
                threadSpan.end();
            }).start();
        }

        Thread.sleep(5000);
    }
}
