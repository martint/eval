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
package org.weakref.eval.core;

import org.openjdk.jmh.annotations.CompilerControl;
import org.weakref.eval.benchmark.TpchData;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;

import static java.lang.String.format;

public class ColumnarAdaptive
{
    private static final byte[] MIN_SHIP_DATE_BYTES = "1994-01-01".getBytes();
    private static final byte[] MAX_SHIP_DATE_BYTES = "1995-01-01".getBytes();

    public static final MethodHandle LONG_VECTOR_GREATER_THAN_OR_EQUAL;
    public static final MethodHandle LONG_VECTOR_LESS_THAN_OR_EQUAL;
    public static final MethodHandle LONG_VECTOR_LESS_THAN;
    public static final MethodHandle BYTES_VECTOR_GREATER_THAN_OR_EQUAL;
    public static final MethodHandle BYTES_VECTOR_LESS_THAN;

    public static final MethodHandle LONG_GREATER_THAN_OR_EQUAL;
    public static final MethodHandle LONG_LESS_THAN_OR_EQUAL;
    public static final MethodHandle LONG_LESS_THAN;
    public static final MethodHandle BYTES_GREATER_THAN_OR_EQUAL;
    public static final MethodHandle BYTES_LESS_THAN;

    static {
        try {
            LONG_GREATER_THAN_OR_EQUAL = MethodHandles.lookup().findStatic(
                    ColumnarAdaptive.class,
                    "greaterThanOrEqual",
                    MethodType.methodType(boolean.class, long.class, long.class));

            LONG_LESS_THAN_OR_EQUAL = MethodHandles.lookup().findStatic(
                    ColumnarAdaptive.class,
                    "lessThanOrEqual",
                    MethodType.methodType(boolean.class, long.class, long.class));

            LONG_LESS_THAN = MethodHandles.lookup().findStatic(
                    ColumnarAdaptive.class,
                    "lessThan",
                    MethodType.methodType(boolean.class, long.class, long.class));

            BYTES_LESS_THAN = MethodHandles.lookup().findStatic(
                    ColumnarAdaptive.class,
                    "lessThan",
                    MethodType.methodType(boolean.class, byte[].class, int.class, int.class, byte[].class));

            BYTES_GREATER_THAN_OR_EQUAL = MethodHandles.lookup().findStatic(
                    ColumnarAdaptive.class,
                    "greaterThanOrEqual",
                    MethodType.methodType(boolean.class, byte[].class, int.class, int.class, byte[].class));

            MethodHandle applyLong = MethodHandles.lookup().findStatic(
                    ColumnarAdaptive.class,
                    "apply",
                    MethodType.methodType(int.class, MethodHandle.class, int.class, int[].class, long[].class, long.class));

            LONG_VECTOR_GREATER_THAN_OR_EQUAL = applyLong.bindTo(LONG_GREATER_THAN_OR_EQUAL);
            LONG_VECTOR_LESS_THAN_OR_EQUAL = applyLong.bindTo(LONG_LESS_THAN_OR_EQUAL);
            LONG_VECTOR_LESS_THAN = applyLong.bindTo(LONG_LESS_THAN);

            MethodHandle applyBytes = MethodHandles.lookup().findStatic(
                    ColumnarAdaptive.class,
                    "apply",
                    MethodType.methodType(int.class, MethodHandle.class, int.class, int[].class, byte[].class, int[].class, byte[].class));

            BYTES_VECTOR_GREATER_THAN_OR_EQUAL = applyBytes.bindTo(BYTES_GREATER_THAN_OR_EQUAL);
            BYTES_VECTOR_LESS_THAN = applyBytes.bindTo(BYTES_LESS_THAN);
        }
        catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public static void evaluate(
            int count,
            int[] inputPositions,
            byte[] shipDate,
            int[] shipDatePositions,
            long[] discount,
            long[] quantity,
            long[] extendedPrice,
            long[] result,
            int[] tempActivePositions)
            throws Throwable
    {
        System.arraycopy(inputPositions, 0, tempActivePositions, 0, count);

        count = (int) LONG_VECTOR_LESS_THAN.invokeExact(count, tempActivePositions, quantity, 24L);
        count = (int) LONG_VECTOR_GREATER_THAN_OR_EQUAL.invokeExact(count, tempActivePositions, discount, 5L);
        count = (int) LONG_VECTOR_LESS_THAN_OR_EQUAL.invokeExact(count, tempActivePositions, discount, 7L);
        count = (int) BYTES_VECTOR_LESS_THAN.invokeExact(count, tempActivePositions, shipDate, shipDatePositions, MAX_SHIP_DATE_BYTES);
        count = (int) BYTES_VECTOR_GREATER_THAN_OR_EQUAL.invokeExact(count, tempActivePositions, shipDate, shipDatePositions, MIN_SHIP_DATE_BYTES);

        product(count, tempActivePositions, result, discount, extendedPrice);
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public static void evaluate2(
            int count,
            int[] inputPositions,
            byte[] shipDate,
            int[] shipDatePositions,
            long[] discount,
            long[] quantity,
            long[] extendedPrice,
            long[] result,
            int[] tempActivePositions)
            throws Throwable
    {
        int c = count;
        System.arraycopy(inputPositions, 0, tempActivePositions, 0, count);
        int newCount;
        long t0 = System.nanoTime();
        newCount = (int) LONG_VECTOR_LESS_THAN.invokeExact(count, tempActivePositions, quantity, 24L);

        C0 += count - newCount;
        count = newCount;
        count = c;

        long t1 = System.nanoTime();
        System.arraycopy(inputPositions, 0, tempActivePositions, 0, count);
        newCount = (int) LONG_VECTOR_GREATER_THAN_OR_EQUAL.invokeExact(count, tempActivePositions, discount, 5L);

        C1 += count - newCount;
        count = newCount;
        count = c;

        long t2 = System.nanoTime();
        System.arraycopy(inputPositions, 0, tempActivePositions, 0, count);
        newCount = (int) LONG_VECTOR_LESS_THAN_OR_EQUAL.invokeExact(count, tempActivePositions, discount, 7L);
        C2 += count - newCount;
//        count = newCount;
        count = c;

        long t3 = System.nanoTime();
        System.arraycopy(inputPositions, 0, tempActivePositions, 0, count);
        newCount = (int) BYTES_VECTOR_LESS_THAN.invokeExact(count, tempActivePositions, shipDate, shipDatePositions, MAX_SHIP_DATE_BYTES);
        C3 += count - newCount;
//        count = newCount;
        count = c;

        long t4 = System.nanoTime();
        System.arraycopy(inputPositions, 0, tempActivePositions, 0, count);
        newCount = (int) BYTES_VECTOR_GREATER_THAN_OR_EQUAL.invokeExact(count, tempActivePositions, shipDate, shipDatePositions, MIN_SHIP_DATE_BYTES);
        C4 += count - newCount;
//        count = newCount;
        count = c;

        long t5 = System.nanoTime();

        product(count, tempActivePositions, result, discount, extendedPrice);

        D0 += t1 - t0;
        D1 += t2 - t1;
        D2 += t3 - t2;
        D3 += t4 - t3;
        D4 += t5 - t4;
//        System.out.println(format("%d, %d, %d, %d, %d", t1 - t0, t2 - t1, t3 - t2, t4 - t3, t5 - t4));
    }

    static long D0, D1, D2, D3, D4;
    static long C0, C1, C2, C3, C4;

    private static int apply(MethodHandle function, int count, int[] activePositions, byte[] values, int[] offsets, byte[] value)
            throws Throwable
    {
        int output = 0;
        for (int input = 0; input < count; input++) {
            int position = activePositions[input];
            activePositions[output] = position;
            output += (boolean) function.invokeExact(values, offsets[position], offsets[position + 1], value) ? 1 : 0;
        }

        return output;
    }

    private static int apply(MethodHandle function, int count, int[] activePositions, long[] values, long value)
            throws Throwable
    {
        int output = 0;
        for (int input = 0; input < count; input++) {
            int position = activePositions[input];
            activePositions[output] = position;
            boolean result = (boolean) function.invokeExact(values[position], value);
            output += result ? 1 : 0;
        }

        return output;
    }

    private static void product(int count, int[] activePositions, long[] result, long[] a, long[] b)
    {
        for (int i = 0; i < count; i++) {
            int position = activePositions[i];
            result[position] = a[position] * b[position];
        }
    }

    private static boolean greaterThanOrEqual(byte[] values, int start, int end, byte[] value)
    {
        return Arrays.compare(values, start, end, value, 0, value.length) >= 0;
    }

    private static boolean lessThan(byte[] values, int start, int end, byte[] value)
    {
        return Arrays.compare(values, start, end, value, 0, value.length) < 0;
    }

    private static boolean greaterThanOrEqual(long a, long b)
    {
        return a >= b;
    }

    private static boolean lessThanOrEqual(long a, long b)
    {
        return a <= b;
    }

    private static boolean lessThan(long a, long b)
    {
        return a < b;
    }

    public static void main(String[] args)
            throws Throwable
    {
        TpchData data = new TpchData();
        data.initialize();

        for (int i = 0; i < 100_000; i++) {
            evaluate2(
                    data.positions,
                    data.inputPositions,
                    data.shipDate,
                    data.shipDatePositions,
                    data.discount,
                    data.quantity,
                    data.extendedPrice,
                    data.result,
                    data.tempPositions1);
        }

        System.out.println(format("time = %.4f, dropped = %s, per dropped row = %s", D0 / 1e9, C0, D0 * 1.0 / C0));
        System.out.println(format("time = %.4f, dropped = %s, per dropped row = %s", D1 / 1e9, C1, D1 * 1.0 / C1));
        System.out.println(format("time = %.4f, dropped = %s, per dropped row = %s", D2 / 1e9, C2, D2 * 1.0 / C2));
        System.out.println(format("time = %.4f, dropped = %s, per dropped row = %s", D3 / 1e9, C3, D3 * 1.0 / C3));
        System.out.println(format("time = %.4f, dropped = %s, per dropped row = %s", D4 / 1e9, C4, D4 * 1.0 / C4));
    }
}
