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
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.RunnerException;

import java.util.concurrent.TimeUnit;

import static org.weakref.eval.benchmark.BenchmarkRunner.benchmark;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1, jvmArgsAppend = {
        "--add-modules=jdk.incubator.vector",
        "-XX:+UnlockDiagnosticVMOptions",
        "-XX:CompileCommand=print,*core*.*",
        "-XX:PrintAssemblyOptions=intel"})
@Warmup(iterations = 10)
@Measurement(iterations = 10)
public class BenchmarkFlatVsCompactMask
{
    @State(Scope.Thread)
    public static class Positions
    {
        @Param({"1000", "2000", "3000", "4000", "5000", "6000", "7000", "8000", "9000", "10000"})
        public int positions;
    }

    @Benchmark
    public Object flat(TpchData data, Positions positions)
    {
        test(positions.positions, data.inputMask, data.resultMaskByte, data.discountNullByte);

        return data.resultMaskByte;
    }

    @Benchmark
    public Object compact(TpchData data, Positions positions)
    {
        test(positions.positions, data.inputPositions, data.tempPositions1, data.discountNullByte);

        return data.tempPositions1;
    }

    private static void test(int positions, byte[] inputMask, byte[] outputMask, byte[] nulls)
    {
        for (int i = 0; i < positions; i++) {
            int selected = inputMask[i] & (nulls[i] == 1 ? 1 : 0);
            outputMask[i] = (byte) selected;
        }
    }

    private static int test(int positions, int[] inputPositions, int[] outputPositions, byte[] nulls)
    {
        int output = 0;
        for (int i = 0; i < positions; i++) {
            int position = inputPositions[i];
            outputPositions[output] = position;
            output += (nulls[position] == 1) ? 1 : 0;
        }

        return output;
    }

    public static void main(String[] args)
            throws RunnerException
    {
        benchmark(BenchmarkFlatVsCompactMask.class);
    }
}
