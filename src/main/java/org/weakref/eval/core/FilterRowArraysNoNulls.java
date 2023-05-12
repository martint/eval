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

import static org.weakref.eval.kernel.MemorySegmentKernel.parseDate;

public class FilterRowArraysNoNulls
{
    private static final int MIN_DATE = parseDate("1994-01-01");
    private static final int MAX_DATE = parseDate("1995-01-01");

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public static void evaluate(
            int count,
            byte[] selectedPositions,
            int[] shipDate,
            long[] discount,
            long[] quantity,
            byte[] result)
    {
        for (int i = 0; i < count; i++) {
            int shipdateValue = shipDate[i];
            long discountValue = discount[i];

            boolean value = selectedPositions[i] != 0 &
                    shipdateValue >= MIN_DATE &
                    shipdateValue < MAX_DATE &
                    discountValue >= 5 &
                    discountValue <= 7 &
                    quantity[i] < 2400;

            result[i] = (byte) (value ? 1 : 0);
        }
    }
}
