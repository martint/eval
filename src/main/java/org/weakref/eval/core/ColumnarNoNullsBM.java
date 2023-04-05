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

public class ColumnarNoNullsBM
{
    private static final byte[] MIN_SHIP_DATE_BYTES = "1994-01-01".getBytes();
    private static final byte[] MAX_SHIP_DATE_BYTES = "1995-01-01".getBytes();

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public static void evaluate(
            int count,
            long[] inputMask,
            byte[] shipDate,
            int[] shipDatePositions,
            long[] discount,
            long[] quantity,
            long[] extendedPrice,
            long[] result,
            long[] resultMask)
    {
        greaterOrEqual(count, inputMask, resultMask, discount, 5);
        lessOrEqual(count, resultMask, resultMask, discount, 7);
        less(count, resultMask, resultMask, quantity, 24);
        greaterOrEqual(count, resultMask, resultMask, shipDate, shipDatePositions, MIN_SHIP_DATE_BYTES);
        less(count, resultMask, resultMask, shipDate, shipDatePositions, MAX_SHIP_DATE_BYTES);

        product(count, resultMask, result, discount, extendedPrice);
    }

    private static void greaterOrEqual(int count, long[] inputMask, long[] outputMask, long[] values, long value)
    {
        for (int i = 0; i < count; i += 64) {
            long output = 0;
            long word = inputMask[i / 64];

            while (word != 0) {
                int offset = Long.numberOfTrailingZeros(word);
                int index = i + offset;

                if (index >= count) {
                    break;
                }

                output |= (values[index] >= value ? 1L : 0L) << offset;
                word &= word - 1;             
            }

            outputMask[i / 64] = output;
        }
    }

    private static void lessOrEqual(int count, long[] inputMask, long[] outputMask, long[] values, long value)
    {
        for (int i = 0; i < count; i += 64) {
            long output = 0;
            long word = inputMask[i / 64];

            while (word != 0) {
                int offset = Long.numberOfTrailingZeros(word);
                int index = i + offset;

                if (index >= count) {
                    break;
                }


                output |= (values[index] <= value ? 1L : 0L) << offset;
                word &= word - 1;
            }

            outputMask[i / 64] = output;
        }
    }

    private static void less(int count, long[] inputMask, long[] outputMask, long[] values, long value)
    {
        for (int i = 0; i < count; i += 64) {
            long output = 0;
            long word = inputMask[i / 64];

            while (word != 0) {
                int offset = Long.numberOfTrailingZeros(word);
                int index = i + offset;

                if (index >= count) {
                    break;
                }


                output |= (values[index] < value ? 1L : 0L) << offset;
                word &= word - 1;
            }

            outputMask[i / 64] = output;
        }
    }

    private static void greaterOrEqual(int count, long[] inputMask, long[] outputMask, byte[] values, int[] offsets, byte[] value)
    {
        for (int i = 0; i < count; i += 64) {
            long output = 0;
            long word = inputMask[i / 64];

            while (word != 0) {
                int offset = Long.numberOfTrailingZeros(word);
                int index = i + offset;

                if (index >= count) {
                    break;
                }


                output |= (Arrays.compare(values, offsets[index], offsets[index + 1], value, 0, value.length) >= 0 ? 1L : 0L) << offset;
                word &= word - 1;
            }

            outputMask[i / 64] = output;
        }
    }

    private static void less(int count, long[] inputMask, long[] outputMask, byte[] values, int[] offsets, byte[] value)
    {
        for (int i = 0; i < count; i += 64) {
            long output = 0;
            long word = inputMask[i / 64];

            while (word != 0) {
                int offset = Long.numberOfTrailingZeros(word);
                int index = i + offset;

                if (index >= count) {
                    break;
                }


                output |= (Arrays.compare(values, offsets[index], offsets[index + 1], value, 0, value.length) < 0 ? 1L : 0L) << offset;
                word &= word - 1;
            }

            outputMask[i / 64] = output;
        }
    }

    private static void product(int count, long[] inputMask, long[] result, long[] a, long[] b)
    {
        for (int i = 0; i < count; i += 64) {
            long word = inputMask[i / 64];

            while (word != 0) {
                int offset = Long.numberOfTrailingZeros(word);
                int index = i + offset;

                if (index >= count) {
                    break;
                }


                result[index] = a[i] * b[i];
                word &= word - 1;
            }
        }
    }
}
