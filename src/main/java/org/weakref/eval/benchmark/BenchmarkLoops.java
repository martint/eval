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

import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.RunnerException;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1, jvmArgsAppend = {
        "--add-modules=jdk.incubator.vector",
        "-XX:+UnlockDiagnosticVMOptions",
        "-XX:CompileCommand=print,*core*.*",
        "-XX:PrintAssemblyOptions=intel"})
@Warmup(iterations = 10)
@Measurement(iterations = 10)
public class BenchmarkLoops
{
    public int selectionVector(int count, int[] inputPositions, int[] outputPositions, boolean[] matches)
    {
        int output = 0;
        for (int input = 0; input < count; input++) {
            int position = inputPositions[input];
            outputPositions[output] = position;
            output += matches[position] ? 1 : 0;
        }

        return output;
    }

    public int selectionVectorInPlace(int count, int[] positions, boolean[] matches)
    {
        int output = 0;
        for (int input = 0; input < count; input++) {
            int position = positions[input];
            positions[output] = position;
            output += matches[position] ? 1 : 0;
        }

        return output;
    }

    public void byteMask(int count, byte[] mask, boolean[] matches)
    {
        for (int i = 0; i < count; i++) {
            int match = matches[i] ? 1 : 0;
            mask[i] = (byte) (mask[i] & match);
        }
    }

    public void booleanMask(int count, boolean[] mask, boolean[] matches)
    {
        for (int i = 0; i < count; i++) {
            boolean selected = mask[i] & matches[i];
            mask[i] = selected;
        }
    }

    public void longMask(int count, long[] mask, boolean[] matches)
    {
//        for (int i = 0; i < count; i += 64) {
//            long output = 0;
//            long word = mask[i / 64];
//
//            while (word != 0) {
//                int offset = Long.numberOfTrailingZeros(word);
//                int index = i + offset;
//
//                output |= (matches[index] ? 1L : 0L) << offset;
//                word &= word - 1;
//            }
//
//            mask[i / 64] = output;
//        }

        for (int k = 0; k < mask.length; k++) {
            long word = mask[k];
            long output = 0;
            while (word != 0) {
                int r = Long.numberOfTrailingZeros(word);
                int index = k * 64 + r;
                output = output | ((matches[index] ? 1L : 0L) << r);

//                long t = word & -word;
//                word = word ^ t;
                word = word & (word - 1);
            }
            mask[k] = output;
        }
    }

    @Benchmark
    public int[] selectionVector(Data data)
    {
        selectionVector(data.count, data.positions, data.outputPositions, data.matches);
        return data.outputPositions;
    }

    @Benchmark
    public int[] selectionVectorInPlace(Data data)
    {
        selectionVectorInPlace(data.count, data.positions, data.matches);
        return data.positions;
    }

    @Benchmark
    public byte[] byteMask(Data data)
    {
        byteMask(data.count, data.byteMask, data.matches);
        return data.byteMask;
    }

    @Benchmark
    public boolean[] booleanMask(Data data)
    {
        booleanMask(data.count, data.booleanMask, data.matches);
        return data.booleanMask;
    }

    @Benchmark
    public long[] longMask(Data data)
    {
        longMask(data.count, data.longMask, data.matches);
        return data.longMask;
    }

    @State(Scope.Thread)
    public static class Data
    {
        private int count = 10240;
        private int[] positions = new int[count];
        private int[] outputPositions = new int[count];
        private boolean[] booleanMask = new boolean[count];
        private byte[] byteMask = new byte[count];
        private long[] longMask = new long[count / Long.SIZE];

        private boolean[] matches = new boolean[count];

        @Param(value = {"0.1", "0.2", "0.3", "0.4", "0.5", "0.6", "0.7", "0.8", "0.9"})
        public double selectivity = 0.2;

        @Setup
        public void setup()
        {
            for (int i = 0; i < count; i++) {
                matches[i] = true; //ThreadLocalRandom.current().nextDouble() < selectivity;
            }

            for (int i = 0; i < count; i++) {
                booleanMask[i] = ThreadLocalRandom.current().nextDouble() < selectivity;
            }

            for (int i = 0; i < count; i++) {
                byteMask[i] = (byte) (booleanMask[i] ? 1 : 0);
            }

            for (int i = 0; i < count; i += Long.SIZE) {
                long word = 0;
                for (int j = 0; j < Long.SIZE; j++) {
                    word = word | (((long) byteMask[i + j]) << 63 - j);
                }
                longMask[i / Long.SIZE] = word;
            }

//            Arrays.fill(booleanMask, true);
//            Arrays.fill(byteMask, (byte) 1);
//            Arrays.fill(longMask, -1);

            int index = 0;
            for (int i = 0; i < count; i++) {
                positions[index] = i;
                index += booleanMask[i] ? 1 : 0;
            }
        }
    }

    @Test
    public void test()
    {
        Data data = new Data();
        data.setup();

        longMask(data);
    }

    public static void main(String[] args)
            throws RunnerException
    {
        BenchmarkRunner.benchmark(BenchmarkLoops.class);
    }
}
