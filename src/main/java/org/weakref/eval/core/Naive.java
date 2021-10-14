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

public class Naive
{
    private static final byte[] MIN_SHIP_DATE_BYTES = "1994-01-01".getBytes();
    private static final byte[] MAX_SHIP_DATE_BYTES = "1995-01-01".getBytes();

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public static int evaluate(
            int count,
            byte[] shipDate,
            int[] shipDatePositions,
            boolean[] shipDateNull,
            long[] discount,
            boolean[] discountNull,
            long[] quantity,
            boolean[] quantityNull,
            long[] extendedPrice,
            boolean[] extendedPriceNull,
            long[] result,
            boolean[] resultNull)
    {
        int output = 0;
        for (int i = 0; i < count; i++) {
            int shipDateStart = shipDatePositions[i];
            int shipDateEnd = shipDatePositions[i + 1];

            boolean match = !discountNull[i]
                    && !quantityNull[i]
                    && !shipDateNull[i]
                    && discount[i] >= 5
                    && discount[i] <= 7
                    && quantity[i] < 24
                    && Arrays.compare(shipDate, shipDateStart, shipDateEnd, MIN_SHIP_DATE_BYTES, 0, MIN_SHIP_DATE_BYTES.length) >= 0
                    && Arrays.compare(shipDate, shipDateStart, shipDateEnd, MAX_SHIP_DATE_BYTES, 0, MAX_SHIP_DATE_BYTES.length) < 0;

            if (match) {
                boolean outputNull = discountNull[i] || extendedPriceNull[i];

                resultNull[output] = outputNull;
                if (!outputNull) {
                    result[output] = discount[i] * extendedPrice[i];
                }

                output++;
            }
        }

        return output;
    }
}
