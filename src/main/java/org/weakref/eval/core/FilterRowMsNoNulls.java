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

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static org.weakref.eval.kernel.MemorySegmentKernel.parseDate;

public class FilterRowMsNoNulls
{
    private static final int MIN_DATE = parseDate("1994-01-01");
    private static final int MAX_DATE = parseDate("1995-01-01");

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public static void evaluate(
            int count,
            MemorySegment selectedPositions,
            MemorySegment shipDate,
            MemorySegment discount,
            MemorySegment quantity,
            MemorySegment result)
    {
        for (int i = 0; i < count; i++) {
            int shipdateValue = shipDate.getAtIndex(ValueLayout.JAVA_INT_UNALIGNED, i);
            long discountValue = discount.getAtIndex(ValueLayout.JAVA_LONG_UNALIGNED, i);

            result.set(ValueLayout.JAVA_BOOLEAN, i,
                    selectedPositions.get(ValueLayout.JAVA_BOOLEAN, i) &
                            shipdateValue >= MIN_DATE &
                            shipdateValue < MAX_DATE &
                            discountValue >= 5 &
                            discountValue <= 7 &
                            quantity.getAtIndex(ValueLayout.JAVA_LONG_UNALIGNED, i) < 2400);
        }
    }
}
