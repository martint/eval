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

import com.google.common.primitives.Booleans;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorSpecies;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.RunnerException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.weakref.eval.benchmark.BenchmarkRunner.benchmark;

@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 1, jvmArgsAppend = {
        "--enable-preview",
        "--add-modules=jdk.incubator.vector",
        "-XX:+UnlockDiagnosticVMOptions",
//        "-XX:CompileCommand=print,*vector*.*",
//        "-XX:PrintAssemblyOptions=intel"
})
@Warmup(iterations = 10, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
public class BenchmarkProjections
{
    private static final int POSITIONS = 1024;
    private static final VectorSpecies<Integer> INT_SPECIES = IntVector.SPECIES_PREFERRED;
    private boolean[] mask;

    private int[] result;
    private int[] value1;
    private int[] value2;

    @Param({"0", "25", "50", "75", "100"})
    public int selectivity = 50;

    @Setup
    public void setup()
            throws IOException
    {
        mask = new boolean[POSITIONS];

        int positives = selectivity * mask.length / 100;
        Arrays.fill(mask, 0, positives, true);
        Arrays.fill(mask, positives, mask.length, false);
        Collections.shuffle(Booleans.asList(mask));

        result = new int[POSITIONS];
        value1 = new int[POSITIONS];
        value2 = new int[POSITIONS];

        for (int i = 0; i < POSITIONS; i++) {
            value1[i] = ThreadLocalRandom.current().nextInt();
            value2[i] = ThreadLocalRandom.current().nextInt();
        }
    }

    @Benchmark
    public Object conditional()
    {
        boolean[] mask = this.mask;
        int[] value1 = this.value1;
        int[] value2 = this.value2;
        int[] result = this.result;

        for (int i = 0; i < result.length; i++) {
            if (mask[i]) {
                result[i] = value1[i] * value2[i];
            }
        }

        return result;
    }

    @Benchmark
    public Object unconditional()
    {
        int[] value1 = this.value1;
        int[] value2 = this.value2;
        int[] result = this.result;

        for (int i = 0; i < result.length; i++) {
            result[i] = value1[i] * value2[i];
        }

        return result;
    }

    @Benchmark
    public Object vectorUnconditional()
    {
        int length = mask.length;

        int i = 0;
        for (; i < INT_SPECIES.loopBound(length); i += INT_SPECIES.length()) {
            IntVector a = IntVector.fromArray(INT_SPECIES, value1, i);
            IntVector b = IntVector.fromArray(INT_SPECIES, value2, i);
            a.mul(b).intoArray(result, i);
        }

        for (; i < length; i++) {
            result[i] = value1[i] * value2[i];
        }

        return result;
    }

    @Benchmark
    public Object vectorConditional()
    {
        int length = mask.length;

        int i = 0;
        for (; i < INT_SPECIES.loopBound(length); i += INT_SPECIES.length()) {
            IntVector a = IntVector.fromArray(INT_SPECIES, value1, i);
            IntVector b = IntVector.fromArray(INT_SPECIES, value2, i);
            VectorMask<Integer> mask = VectorMask.fromArray(INT_SPECIES, this.mask, i);
            a.mul(b).intoArray(result, i, mask);
        }

        for (; i < length; i++) {
            if (mask[i]) {
                result[i] = value1[i] * value2[i];
            }
        }

        return result;
    }

    public static void main(String[] args)
            throws RunnerException, IOException
    {
        BenchmarkProjections bench = new BenchmarkProjections();
//        bench.selectivity = 100;
//        bench.setup();
//        bench.vector();
        benchmark(BenchmarkProjections.class);
    }
}
