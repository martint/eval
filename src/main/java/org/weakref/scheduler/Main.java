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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.airlift.node.NodeInfo;
import io.airlift.tracing.OpenTelemetryConfig;
import io.airlift.tracing.OpenTelemetryModule;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Main
{
    private static final ListeningExecutorService SLEEPER = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());

    public static ListenableFuture<?> asyncSleep(long millis)
    {
        return SLEEPER.submit(() -> {
            Thread.sleep(millis);
            return null;
        });
    }

    public static void main(String[] args)
            throws ExecutionException, InterruptedException
    {
        OpenTelemetryModule module = new OpenTelemetryModule("main", "1");
        OpenTelemetry openTelemetry = module.createOpenTelemetry(new NodeInfo("testing"), new OpenTelemetryConfig());

        Tracer tracer = openTelemetry.getTracer("scheduler");

        Span mainSpan = tracer.spanBuilder("main").startSpan();

        Scheduler scheduler = new Scheduler(tracer);
        scheduler.start();

        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < 4; t++) {
            futures.add(scheduler.submit("thread" + t, newTask(100)));
            Thread.sleep(300);
//            futures.add(scheduler.submit("thread" + t, newTask((t + 1) * 100)));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        scheduler.threads().stream()
//                .map(task -> task.time())
                .map(task -> task.time() / 1e9)
                .sorted()
                .forEach(System.out::println);

        scheduler.close();
        SLEEPER.close();
        mainSpan.end();

        Thread.sleep(5000);
    }

    private static Task newTask(int sleep)
    {
        return (context) -> {
            long sum = 0;
            for (int loop = 0; loop < 5; loop++) {
                Thread.sleep(sleep);
                context.maybeYield();
            }
            System.out.println(sum);
        };
    }
}
