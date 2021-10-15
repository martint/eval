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

public class ColumnarMethodHandles
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
                    ColumnarMethodHandles.class,
                    "greaterThanOrEqual",
                    MethodType.methodType(boolean.class, long.class, long.class));

            LONG_LESS_THAN_OR_EQUAL = MethodHandles.lookup().findStatic(
                    ColumnarMethodHandles.class,
                    "lessThanOrEqual",
                    MethodType.methodType(boolean.class, long.class, long.class));

            LONG_LESS_THAN = MethodHandles.lookup().findStatic(
                    ColumnarMethodHandles.class,
                    "lessThan",
                    MethodType.methodType(boolean.class, long.class, long.class));

            BYTES_LESS_THAN = MethodHandles.lookup().findStatic(
                    ColumnarMethodHandles.class,
                    "lessThan",
                    MethodType.methodType(boolean.class, byte[].class, int.class, int.class, byte[].class));

            BYTES_GREATER_THAN_OR_EQUAL = MethodHandles.lookup().findStatic(
                    ColumnarMethodHandles.class,
                    "greaterThanOrEqual",
                    MethodType.methodType(boolean.class, byte[].class, int.class, int.class, byte[].class));

            MethodHandle applyLong = MethodHandles.lookup().findStatic(
                    ColumnarMethodHandles.class,
                    "apply",
                    MethodType.methodType(int.class, MethodHandle.class, int.class, int[].class, long[].class, long.class));

            LONG_VECTOR_GREATER_THAN_OR_EQUAL = applyLong.bindTo(LONG_GREATER_THAN_OR_EQUAL);
            LONG_VECTOR_LESS_THAN_OR_EQUAL = applyLong.bindTo(LONG_LESS_THAN_OR_EQUAL);
            LONG_VECTOR_LESS_THAN = applyLong.bindTo(LONG_LESS_THAN);

            MethodHandle applyBytes = MethodHandles.lookup().findStatic(
                    ColumnarMethodHandles.class,
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

        count = (int) LONG_VECTOR_GREATER_THAN_OR_EQUAL.invokeExact(count, tempActivePositions, discount, 5L);
        count = (int) LONG_VECTOR_LESS_THAN_OR_EQUAL.invokeExact(count, tempActivePositions, discount, 7L);
        count = (int) LONG_VECTOR_LESS_THAN.invokeExact(count, tempActivePositions, quantity, 24L);

        count = (int) BYTES_VECTOR_GREATER_THAN_OR_EQUAL.invokeExact(count, tempActivePositions, shipDate, shipDatePositions, MIN_SHIP_DATE_BYTES);
        count = (int) BYTES_VECTOR_LESS_THAN.invokeExact(count, tempActivePositions, shipDate, shipDatePositions, MAX_SHIP_DATE_BYTES);

        product(count, tempActivePositions, result, discount, extendedPrice);
    }

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

        evaluate(
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
}
