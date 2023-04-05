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
import org.weakref.eval.proto.mask.DenseMask;
import org.weakref.eval.proto.mask.Masks;
import org.weakref.eval.proto.mask.SparseMask;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.weakref.eval.benchmark.BenchmarkRunner.benchmark;

@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1, jvmArgsAppend = {
        "--enable-preview",
        "--add-modules=jdk.incubator.vector",
        "-XX:+UnlockDiagnosticVMOptions",
//        "-XX:CompileCommand=print,*vector*.*",
//        "-XX:PrintAssemblyOptions=intel"
})
@Warmup(iterations = 10, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
public class BenchmarkDenseToSparse
{
    private static final int POSITIONS = 1024;
    private static final VectorSpecies<Integer> INT_SPECIES = IntVector.SPECIES_PREFERRED;
    private static final int[] OFFSETS = new int[INT_SPECIES.length()];
    private static final IntVector INDICES;
    private DenseMask dense;

    static {
        for (int i = 0; i < OFFSETS.length; i++) {
            OFFSETS[i] = i;
        }
        INDICES = IntVector.fromArray(INT_SPECIES, OFFSETS, 0);
    }

    private int[] sparse = new int[POSITIONS];

    @Param({"0", "0.1", "0.3", "0.5", "0.8", "0.9", "1"})
//    @Param({"0.5"})
    public double selectivity = 0.5;

    @Setup
    public void setup()
            throws IOException
    {
        dense = Masks.randomDenseMask(POSITIONS, selectivity);
    }

    @Benchmark
    public SparseMask withAllocation()
    {
        return dense.toSparse();
    }

    @Benchmark
    public SparseMask conditional()
    {
        boolean[] dense = this.dense.mask();
        
        int count = 0;
        for (int i = 0; i < dense.length; i++) {
            if (dense[i]) {
                sparse[count] = i;
                count++;
            }
        }

        return new SparseMask(sparse, count);
    }

    @Benchmark
    public SparseMask branchless()
    {
        boolean[] dense = this.dense.mask();

        int output = 0;
        for (int i = 0; i < dense.length; i++) {
            sparse[output] = i;
            output += dense[i] ? 1 : 0;
        }

        return new SparseMask(sparse, output);
    }

//    @Benchmark
    public SparseMask vector()
    {
        boolean[] dense = this.dense.mask();

        int length = dense.length;

        int output = 0;

        int i = 0;
        for (; i < INT_SPECIES.loopBound(length); i += INT_SPECIES.length()) {
            VectorMask<Integer> mask = VectorMask.fromArray(INT_SPECIES, dense, i);
            INDICES.add(i)
                    .compress(mask)
                    .intoArray(sparse, output);

            output += mask.trueCount();
        }

        for (; i < length; i++) {
            sparse[output] = i;
            output += dense[i] ? 1 : 0;
        }

        return new SparseMask(sparse, output);
    }

    public static void main(String[] args)
            throws RunnerException, IOException
    {
        BenchmarkDenseToSparse bench = new BenchmarkDenseToSparse();
//        bench.setup();
//        bench.vector();
        benchmark(BenchmarkDenseToSparse.class);
    }
}
