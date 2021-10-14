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

import java.util.Arrays;

public class Columnar
{
    private static final byte[] MIN_SHIP_DATE_BYTES = "1994-01-01".getBytes();
    private static final byte[] MAX_SHIP_DATE_BYTES = "1995-01-01".getBytes();

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public static void evaluate(
            int count,
            byte[] inputMask,
            byte[] shipDate,
            int[] shipDatePositions,
            byte[] shipDateNull,
            long[] discount,
            byte[] discountNull,
            long[] quantity,
            byte[] quantityNull,
            long[] extendedPrice,
            byte[] extendedPriceNull,
            long[] result,
            byte[] resultMask,
            byte[] resultNull)
    {
        isNotNull(count, inputMask, resultMask, discountNull);
        isNotNull(count, resultMask, resultMask, shipDateNull);
        isNotNull(count, resultMask, resultMask, quantityNull);
        greaterOrEqual(count, resultMask, resultMask, discount, 5);
        lessOrEqual(count, resultMask, resultMask, discount, 7);
        less(count, resultMask, resultMask, quantity, 24);
        greaterOrEqual(count, resultMask, resultMask, shipDate, shipDatePositions, MIN_SHIP_DATE_BYTES);
        less(count, resultMask, resultMask, shipDate, shipDatePositions, MAX_SHIP_DATE_BYTES);

        isNull(count, resultMask, resultNull, discountNull);
        isNull(count, resultMask, resultNull, extendedPriceNull);
        product(count, resultMask, resultNull, result, discount, extendedPrice);
    }

    private static void isNotNull(int count, byte[] inputMask, byte[] outputMask, byte[] nulls)
    {
        for (int i = 0; i < count; i++) {
            int selected = inputMask[i] & (nulls[i] == 0 ? 1 : 0);
            outputMask[i] = (byte) selected;
        }
    }

    private static void isNull(int count, byte[] inputMask, byte[] outputMask, byte[] nulls)
    {
        for (int i = 0; i < count; i++) {
            int selected = inputMask[i] & (nulls[i] == 1 ? 1 : 0);
            outputMask[i] = (byte) selected;
        }
    }

    private static void greaterOrEqual(int count, byte[] inputMask, byte[] outputMask, long[] values, long value)
    {
        for (int i = 0; i < count; i++) {
            int selected = inputMask[i] & (values[i] >= value ? 1 : 0);
            outputMask[i] = (byte) selected;
        }
    }

    private static void lessOrEqual(int count, byte[] inputMask, byte[] outputMask, long[] values, long value)
    {
        for (int i = 0; i < count; i++) {
            int selected = inputMask[i] & (values[i] <= value ? 1 : 0);
            outputMask[i] = (byte) selected;
        }
    }

    private static void less(int count, byte[] inputMask, byte[] outputMask, long[] values, long value)
    {
        for (int i = 0; i < count; i++) {
            int selected = inputMask[i] & (values[i] < value ? 1 : 0);
            outputMask[i] = (byte) selected;
        }
    }

    private static void greaterOrEqual(int count, byte[] inputMask, byte[] outputMask, byte[] values, int[] offsets, byte[] value)
    {
        for (int i = 0; i < count; i++) {
            int selected = (inputMask[i] == 1 && Arrays.compare(values, offsets[i], offsets[i + 1], value, 0, value.length) >= 0) ? 1 : 0;
            outputMask[i] = (byte) selected;
        }
    }

    private static void less(int count, byte[] inputMask, byte[] outputMask, byte[] values, int[] offsets, byte[] value)
    {
        for (int i = 0; i < count; i++) {
            int selected = (inputMask[i] == 1 && Arrays.compare(values, offsets[i], offsets[i + 1], value, 0, value.length) < 0) ? 1 : 0;
            outputMask[i] = (byte) selected;
        }
    }

    private static void product(int count, byte[] inputMask, byte[] nulls, long[] result, long[] a, long[] b)
    {
        for (int i = 0; i < count; i++) {
            if (inputMask[i] == 1 & nulls[i] == 0) {
                result[i] = a[i] * b[i];
            }
        }
    }
}
