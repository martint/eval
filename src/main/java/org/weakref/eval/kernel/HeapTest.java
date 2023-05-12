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
package org.weakref.eval.kernel;

import org.weakref.eval.benchmark.TpchData;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentScope;

public class HeapTest
{
    public static void main(String[] args)
            throws IOException
    {
        TpchData data = new TpchData();
        data.initialize();

        MemorySegment selectedPositions = allocate(data.positions, 1);
        selectedPositions.copyFrom(MemorySegment.ofArray(data.inputMask));

        MemorySegment shipdate = allocate(data.positions, 4);
        shipdate.copyFrom(MemorySegment.ofArray(data.parsedShipDate));

        MemorySegment discount = allocate(data.positions, 8);
        discount.copyFrom(MemorySegment.ofArray(data.discount));

        MemorySegment quantity = allocate(data.positions, 8);
        quantity.copyFrom(MemorySegment.ofArray(data.quantity));

        MemorySegment result = allocate(data.positions, 1);

        MemorySegmentKernel kernel = new MemorySegmentKernel(
                data.positions,
                selectedPositions,
                shipdate,
                discount,
                quantity,
                result
        );

        kernel.process();
    }

    private static MemorySegment allocate(int count, int entrySize)
    {
        return MemorySegment.allocateNative(count * entrySize, SegmentScope.global());
    }
}
