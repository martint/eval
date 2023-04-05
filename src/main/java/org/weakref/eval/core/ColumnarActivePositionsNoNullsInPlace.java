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

import java.io.IOException;
import java.util.Arrays;

public class ColumnarActivePositionsNoNullsInPlace
{
    private static final byte[] MIN_SHIP_DATE_BYTES = "1994-01-01".getBytes();
    private static final byte[] MAX_SHIP_DATE_BYTES = "1995-01-01".getBytes();

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
    {
        System.arraycopy(inputPositions, 0, tempActivePositions, 0, count);
        
        count = greaterOrEqual(count, tempActivePositions, discount, 5);
        count = lessOrEqual(count, tempActivePositions, discount, 7);
        count = less(count, tempActivePositions, quantity, 24);
        count = less(count, tempActivePositions, shipDate, shipDatePositions, MAX_SHIP_DATE_BYTES);
        count = greaterOrEqual(count, tempActivePositions, shipDate, shipDatePositions, MIN_SHIP_DATE_BYTES);

        product(count, tempActivePositions, result, discount, extendedPrice);
    }

    private static int greaterOrEqual(int count, int[] activePositions, long[] values, long value)
    {
        int output = 0;
        for (int input = 0; input < count; input++) {
            int position = activePositions[input];
            activePositions[output] = position;
            output += values[position] >= value ? 1 : 0;
        }

        return output;
    }

    private static int lessOrEqual(int count, int[] activePositions, long[] values, long value)
    {
        int output = 0;
        for (int input = 0; input < count; input++) {
            int position = activePositions[input];
            activePositions[output] = position;
            output += values[position] <= value ? 1 : 0;
        }

        return output;
    }

    private static int less(int count, int[] activePositions, long[] values, long value)
    {
        int output = 0;
        for (int input = 0; input < count; input++) {
            int position = activePositions[input];
            activePositions[output] = position;
            output += values[position] < value ? 1 : 0;
        }

        return output;
    }

    private static int greaterOrEqual(int count, int[] activePositions, byte[] values, int[] offsets, byte[] value)
    {
        int output = 0;
        for (int input = 0; input < count; input++) {
            int position = activePositions[input];
            activePositions[output] = position;
            output += (Arrays.compare(values, offsets[position], offsets[position + 1], value, 0, value.length) >= 0) ? 1 : 0;
        }

        return output;
    }

    private static int less(int count, int[] activePositions, byte[] values, int[] offsets, byte[] value)
    {
        int output = 0;
        for (int input = 0; input < count; input++) {
            int position = activePositions[input];
            activePositions[output] = position;
            output += (Arrays.compare(values, offsets[position], offsets[position + 1], value, 0, value.length) < 0) ? 1 : 0;
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

    public static void main(String[] args)
            throws IOException
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
                new int[data.positions]
        );
    }
}
