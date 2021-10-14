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

public class RowNoNulls
{
    private static final byte[] MIN_SHIP_DATE_BYTES = "1994-01-01".getBytes();
    private static final byte[] MAX_SHIP_DATE_BYTES = "1995-01-01".getBytes();

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public static void evaluate(
            int count,
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
        Arrays.fill(resultNull, (byte) 0);
        for (int i = 0; i < count; i++) {
            boolean match =
                             discount[i] >= 5
                            & discount[i] <= 7
                            & quantity[i] < 24;

            int shipDateStart = shipDatePositions[i];
            int shipDateEnd = shipDatePositions[i + 1];

            match = match
                    && Arrays.compare(shipDate, shipDateStart, shipDateEnd, MIN_SHIP_DATE_BYTES, 0, MIN_SHIP_DATE_BYTES.length) >= 0
                    && Arrays.compare(shipDate, shipDateStart, shipDateEnd, MAX_SHIP_DATE_BYTES, 0, MAX_SHIP_DATE_BYTES.length) < 0;

            resultMask[i] = (byte) (match ? 1 : 0);
            if (match) {
                result[i] = discount[i] * extendedPrice[i];
            }
        }
    }
}
