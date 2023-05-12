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
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.RunnerException;
import org.weakref.eval.core.FilterRowArraysNoNulls;
import org.weakref.eval.core.FilterRowArraysNoNullsVector;
import org.weakref.eval.core.FilterRowMsNoNulls;
import org.weakref.eval.core.FilterRowNativeNoNulls;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 1, jvmArgsAppend = {
        "--add-modules=jdk.incubator.vector",
        "-XX:+UnlockDiagnosticVMOptions",
        "-XX:CompileCommand=print,*core*.*",
        "-XX:PrintAssemblyOptions=intel"})
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
public class BenchmarkFilter
{
//    @Benchmark
    public byte[] array(TpchData data)
    {
        FilterRowArraysNoNulls.evaluate(
                data.positions,
                data.inputMask,
                data.parsedShipDate,
                data.discount,
                data.quantity,
                data.resultMaskByte);

        return data.resultMaskByte;
    }

//    @Benchmark
    public MemorySegment heapMemorySegment(HeapMemorySegments data)
    {
        FilterRowMsNoNulls.evaluate(
                data.positions,
                data.inputMask,
                data.parsedShipDate,
                data.discount,
                data.quantity,
                data.resultMask);

        return data.resultMask;
    }

//    @Benchmark
    public MemorySegment nativeMemorySegment(NativeMemorySegments data)
    {
        FilterRowMsNoNulls.evaluate(
                data.positions,
                data.inputMask,
                data.parsedShipDate,
                data.discount,
                data.quantity,
                data.resultMask);

        return data.resultMask;
    }

//    @Benchmark
    public MemorySegment unsafeMemorySegment(UnsafeMemorySegments data)
    {
        FilterRowMsNoNulls.evaluate(
                data.positions,
                data.inputMask,
                data.parsedShipDate,
                data.discount,
                data.quantity,
                data.resultMask);

        return data.resultMask;
    }

//    @Benchmark
    public MemorySegment nativeFilter(NativeMemorySegments data)
    {
        FilterRowNativeNoNulls.evaluate(
                data.positions,
                data.inputMask,
                data.parsedShipDate,
                data.discount,
                data.quantity,
                data.resultMask);

        return data.resultMask;
    }

    @Benchmark
    public byte[] vector(TpchData data)
    {
        FilterRowArraysNoNullsVector.evaluate(
                data.positions,
                data.inputMask,
                data.parsedShipDate,
                data.discount,
                data.quantity,
                data.resultMaskByte);

        return data.resultMaskByte;
    }

    public static void main(String[] args)
            throws RunnerException
    {
        BenchmarkRunner.benchmark(BenchmarkFilter.class);
    }
}
