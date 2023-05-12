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
package org.weakref.eval.benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.RunnerException;
import org.weakref.eval.kernel.MemorySegmentKernel;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentScope;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 1, jvmArgsAppend = {
        "--add-modules=jdk.incubator.vector",
        "-XX:+UnlockDiagnosticVMOptions",
        "-XX:CompileCommand=print,*MemorySegmentKernel*.*",
        "-XX:PrintAssemblyOptions=intel"})
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@State(Scope.Thread)
public class BenchmarkKernel
{
    private MemorySegmentKernel kernel;

    @Setup
    public void setup()
            throws IOException
    {
        TpchData data = new TpchData();
        data.initialize();

        MemorySegment selectedPositions = allocate(data.positions, 1);
        selectedPositions.copyFrom(MemorySegment.ofArray(data.inputMask));

        MemorySegment shipdate = allocate(data.positions, 4);
        shipdate.copyFrom(MemorySegment.ofArray(data.parsedShipDate));

        MemorySegment discount = allocate(data.positions, 8);
        discount.copyFrom(MemorySegment.ofArray(data.discount));

        MemorySegment quantity = allocate(data.positions, 8);
        quantity.copyFrom(MemorySegment.ofArray(data.quantity));

        MemorySegment result = allocate(data.positions, 1);

        kernel = new MemorySegmentKernel(
                data.positions,
                selectedPositions,
                shipdate,
                discount,
                quantity,
                result
        );
    }

    @Benchmark
    public MemorySegment simple()
    {
        kernel.process();
        return kernel.result();
    }

    private static MemorySegment allocate(int count, int entrySize)
    {
        return MemorySegment.allocateNative(count * entrySize, SegmentScope.global());
    }


    public static void main(String[] args)
            throws RunnerException
    {
        BenchmarkRunner.benchmark(BenchmarkKernel.class);
    }
}
