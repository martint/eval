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
import org.weakref.eval.core.Columnar;
import org.weakref.eval.core.ColumnarActivePositionsNoNulls;
import org.weakref.eval.core.ColumnarActivePositionsNoNullsInPlace;
import org.weakref.eval.core.ColumnarAdaptive;
import org.weakref.eval.core.ColumnarMaskAndPositions;
import org.weakref.eval.core.ColumnarMethodHandles;
import org.weakref.eval.core.ColumnarNoNulls;
import org.weakref.eval.core.ColumnarNoNullsBM;
import org.weakref.eval.core.ColumnarVector;
import org.weakref.eval.core.Naive;
import org.weakref.eval.core.Row;
import org.weakref.eval.core.RowNoNulls;

import java.io.IOException;
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
public class BenchmarkEval
{
    @Benchmark
    public Object[] naive(TpchData data)
    {
        int count = Naive.evaluate(
                data.positions,
                data.shipDate,
                data.shipDatePositions,
                data.shipDateNull,
                data.discount,
                data.discountNull,
                data.quantity,
                data.quantityNull,
                data.extendedPrice,
                data.extendedPriceNull,
                data.result,
                data.resultNull);

        return new Object[] {data.result, data.resultNull, count};
    }

    @Benchmark
    public Object[] row(TpchData data)
    {
        Row.evaluate(
                data.positions,
                data.shipDate,
                data.shipDatePositions,
                data.shipDateNullByte,
                data.discount,
                data.discountNullByte,
                data.quantity,
                data.quantityNullByte,
                data.extendedPrice,
                data.extendedPriceNullByte,
                data.result,
                data.resultMaskByte,
                data.resultNullByte);

        return new Object[] {data.result, data.resultNullByte, data.resultMaskByte};
    }

    @Benchmark
    public Object[] rowNoNulls(TpchData data)
    {
        RowNoNulls.evaluate(
                data.positions,
                data.shipDate,
                data.shipDatePositions,
                data.shipDateNullByte,
                data.discount,
                data.discountNullByte,
                data.quantity,
                data.quantityNullByte,
                data.extendedPrice,
                data.extendedPriceNullByte,
                data.result,
                data.resultMaskByte,
                data.resultNullByte);

        return new Object[] {data.result, data.resultNullByte, data.resultMaskByte};
    }

    @Benchmark
    public Object[] columnar(TpchData data)
    {
        Columnar.evaluate(
                data.positions,
                data.inputMask,
                data.shipDate,
                data.shipDatePositions,
                data.shipDateNullByte,
                data.discount,
                data.discountNullByte,
                data.quantity,
                data.quantityNullByte,
                data.extendedPrice,
                data.extendedPriceNullByte,
                data.result,
                data.resultMaskByte,
                data.resultNullByte);

        return new Object[] {data.result, data.resultNullByte, data.resultMaskByte};
    }

    @Benchmark
    public Object[] columnarNoNulls(TpchData data)
    {
        ColumnarNoNulls.evaluate(
                data.positions,
                data.inputMask,
                data.shipDate,
                data.shipDatePositions,
                data.discount,
                data.quantity,
                data.extendedPrice,
                data.result,
                data.resultMaskByte
        );

        return new Object[] {data.result, data.resultNullByte, data.resultMaskByte};
    }

    @Benchmark
    public Object[] columnarNoNullsBM(TpchData data)
    {
        ColumnarNoNullsBM.evaluate(
                data.positions,
                data.inputMaskBM,
                data.shipDate,
                data.shipDatePositions,
                data.discount,
                data.quantity,
                data.extendedPrice,
                data.result,
                data.resultMaskBM
        );

        return new Object[] {data.result, data.resultNullByte, data.resultMaskByte};
    }

    @Benchmark
    public Object[] columnarVector(TpchData data)
    {
        ColumnarVector.evaluate(
                data.positions,
                data.inputMask,
                data.shipDate,
                data.shipDatePositions,
                data.shipDateNullByte,
                data.discount,
                data.discountNullByte,
                data.quantity,
                data.quantityNullByte,
                data.extendedPrice,
                data.extendedPriceNullByte,
                data.result,
                data.resultMaskByte,
                data.resultNullByte);

        return new Object[] {data.result, data.resultNullByte, data.resultMaskByte};
    }

    @Benchmark
    public Object[] columnarActivePositionsNoNulls(TpchData data)
    {
        ColumnarActivePositionsNoNulls.evaluate(
                data.positions,
                data.inputPositions,
                data.shipDate,
                data.shipDatePositions,
                data.discount,
                data.quantity,
                data.extendedPrice,
                data.result,
                data.tempPositions1,
                data.tempPositions2);

        return new Object[] {data.result, data.resultNullByte, data.tempPositions2};
    }

    @Benchmark
    public Object[] columnarActivePositionsNoNullsInPlace(TpchData data)
    {
        ColumnarActivePositionsNoNullsInPlace.evaluate(
                data.positions,
                data.inputPositions,
                data.shipDate,
                data.shipDatePositions,
                data.discount,
                data.quantity,
                data.extendedPrice,
                data.result,
                data.tempPositions1);

        return new Object[] {data.result, data.resultNullByte, data.tempPositions1};
    }

    @Benchmark
    public Object[] columnarMethodHandles(TpchData data)
            throws Throwable
    {
        ColumnarMethodHandles.evaluate(
                data.positions,
                data.inputPositions,
                data.shipDate,
                data.shipDatePositions,
                data.discount,
                data.quantity,
                data.extendedPrice,
                data.result,
                data.tempPositions1);

        return new Object[] {data.result, data.resultNullByte, data.tempPositions1};
    }

    @Benchmark
    public Object[] columnarAdaptive(TpchData data)
            throws Throwable
    {
        ColumnarAdaptive.evaluate(
                data.positions,
                data.inputPositions,
                data.shipDate,
                data.shipDatePositions,
                data.discount,
                data.quantity,
                data.extendedPrice,
                data.result,
                data.tempPositions1);

        return new Object[] {data.result, data.resultNullByte, data.tempPositions1};
    }

    @Benchmark
    public Object[] columnarMasksAndPositions(TpchData data)
            throws Throwable
    {
        ColumnarMaskAndPositions.evaluate(
                data.positions,
                data.inputPositions,
                data.shipDate,
                data.shipDatePositions,
                data.discount,
                data.quantity,
                data.extendedPrice,
                data.result,
                data.tempPositions1,
                data.resultMaskByte);

        return new Object[] {data.result, data.resultNullByte, data.tempPositions1};
    }

//    @Test
    public void test()
            throws IOException
    {
        TpchData data = new TpchData();
        data.initialize();

        columnarNoNullsBM(data);
    }

    public static void main(String[] args)
            throws RunnerException
    {
        BenchmarkRunner.benchmark(BenchmarkEval.class);
    }
}
