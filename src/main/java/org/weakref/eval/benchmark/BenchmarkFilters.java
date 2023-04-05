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
import org.openjdk.jmh.annotations.Benchmark;
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
//@Fork(value = 1, jvmArgsAppend = {
//        "--enable-preview",
//        "--add-modules=jdk.incubator.vector",
//        "-XX:+UnlockDiagnosticVMOptions",
////        "-XX:CompileCommand=print,*vector*.*",
////        "-XX:PrintAssemblyOptions=intel"
//})
@Warmup(iterations = 5, time = 5000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 2000, timeUnit = TimeUnit.MILLISECONDS)
public class BenchmarkFilters
{
    private static final int POSITIONS = 1024;
    private boolean[] denseMask;
    private int[] sparseMask;
    private int count;

    private boolean[] resultDense;
    private int[] resultSparse;
    private int[] value1;
    private int[] value2;

    //        @Param({"0", "25",  "50", "75", "100"})
    @Param({"0", "10", "20", "30", "40", "50", "60", "70", "80", "90", "100"})
    public int selectivity = 50;

    @Setup
    public void setup()
            throws IOException
    {
        denseMask = new boolean[POSITIONS];
        sparseMask = new int[POSITIONS];

        int positives = selectivity * denseMask.length / 100;
        Arrays.fill(denseMask, 0, positives, true);
        Arrays.fill(denseMask, positives, denseMask.length, false);
        Collections.shuffle(Booleans.asList(denseMask));

        count = 0;
        for (int i = 0; i < denseMask.length; i++) {
            if (denseMask[i]) {
                sparseMask[count] = i;
                count++;
            }
        }

        resultDense = new boolean[POSITIONS];
        resultSparse = new int[POSITIONS];
        value1 = new int[POSITIONS];
        value2 = new int[POSITIONS];

        for (int i = 0; i < POSITIONS; i++) {
            value1[i] = ThreadLocalRandom.current().nextInt();
            value2[i] = ThreadLocalRandom.current().nextInt();
        }
    }

    private boolean filter(int a, int b)
    {
        return a < b;
    }

    @Benchmark
    public Object conditionalIf_dense_dense()
    {
        boolean[] mask = this.denseMask;
        int[] value1 = this.value1;
        int[] value2 = this.value2;
        boolean[] result = this.resultDense;

        for (int i = 0; i < result.length; i++) {
            if (mask[i]) {
                result[i] = filter(value1[i], value2[i]);
            }
        }

        return result;
    }

    @Benchmark
    public Object unconditional_logicalAnd_dense_dense()
    {
        boolean[] mask = this.denseMask;
        int[] value1 = this.value1;
        int[] value2 = this.value2;
        boolean[] result = this.resultDense;

        for (int i = 0; i < result.length; i++) {
            result[i] = mask[i] && filter(value1[i], value2[i]);
        }

        return result;
    }

    @Benchmark
    public Object unconditional_bitwiseAnd_dense_dense()
    {
        boolean[] mask = this.denseMask;
        int[] value1 = this.value1;
        int[] value2 = this.value2;
        boolean[] result = this.resultDense;

        for (int i = 0; i < result.length; i++) {
            result[i] = mask[i] & filter(value1[i], value2[i]);
        }

        return result;
    }

    // dense vs sparse input
    // dense vs sparse output
    // in place vs new

    @Benchmark
    public Object conditional_logicalAnd_dense_sparse()
    {
        boolean[] mask = this.denseMask;
        int[] value1 = this.value1;
        int[] value2 = this.value2;
        int[] result = this.resultSparse;

        int output = 0;
        for (int i = 0; i < mask.length; i++) {
            if (mask[i] && filter(value1[i], value2[i])) {
                result[output] = i;
                output++;
            }
        }

        return new Object[] {result, output};
    }

    @Benchmark
    public Object conditional_bitwiseAnd_dense_sparse()
    {
        boolean[] mask = this.denseMask;
        int[] value1 = this.value1;
        int[] value2 = this.value2;
        int[] result = this.resultSparse;

        int output = 0;
        for (int i = 0; i < mask.length; i++) {
            if (mask[i] && filter(value1[i], value2[i])) {
                result[output] = i;
                output++;
            }
        }

        return new Object[] {result, output};
    }

    @Benchmark
    public Object branchless_bitwiseAnd_dense_sparse()
    {
        boolean[] mask = this.denseMask;
        int[] value1 = this.value1;
        int[] value2 = this.value2;
        int[] result = this.resultSparse;

        int output = 0;
        for (int i = 0; i < mask.length; i++) {
            boolean filter = mask[i] & filter(value1[i], value2[i]);
            result[output] = i;
            output += filter ? 1 : 0;
        }

        return new Object[] {result, output};
    }

    @Benchmark
    public Object branchless_sparse_sparse()
    {
        int[] mask = this.sparseMask;
        int count = this.count;
        int[] value1 = this.value1;
        int[] value2 = this.value2;
        int[] result = this.resultSparse;

        int output = 0;
        for (int i = 0; i < count; i++) {
            int position = mask[i];

            boolean filter = filter(value1[position], value2[position]);
            result[output] = i;
            output += filter ? 1 : 0;
        }

        return new Object[] {result, output};
    }

    @Benchmark
    public Object conditional_sparse_sparse()
    {
        int[] mask = this.sparseMask;
        int count = this.count;
        int[] value1 = this.value1;
        int[] value2 = this.value2;
        int[] result = this.resultSparse;

        int output = 0;
        for (int i = 0; i < count; i++) {
            int position = mask[i];

            if (filter(value1[position], value2[position])) {
                result[output] = i;
                output++;
            }
        }

        return new Object[] {result, output};
    }

    @Benchmark
    public Object branchless_sparse_dense()
    {
        int[] mask = this.sparseMask;
        int count = this.count;
        int[] value1 = this.value1;
        int[] value2 = this.value2;
        boolean[] result = this.resultDense;

        Arrays.fill(result, false);
        for (int i = 0; i < count; i++) {
            int position = mask[i];
            result[position] = filter(value1[position], value2[position]);
        }

        return result;
    }

    public static void main(String[] args)
            throws RunnerException, IOException
    {
        BenchmarkFilters bench = new BenchmarkFilters();
//        bench.selectivity = 100;
//        bench.setup();
//        bench.vector();
        benchmark(BenchmarkFilters.class);
    }
}
