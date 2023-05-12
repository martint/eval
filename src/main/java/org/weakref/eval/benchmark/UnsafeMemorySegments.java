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
package org.weakref.eval.benchmark;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import sun.misc.Unsafe;

import java.lang.foreign.MemorySegment;
import java.lang.reflect.Field;

@State(Scope.Thread)
public class UnsafeMemorySegments
{
    public int positions;
    public MemorySegment inputMask;

    public MemorySegment discount;
    public MemorySegment quantity;
    public MemorySegment parsedShipDate;
    public MemorySegment resultMask;

    @Setup
    public void setup(TpchData data)
    {
        this.positions = data.positions;

        this.inputMask = allocateAndCopy(data.inputMask);
        this.discount = allocateAndCopy(data.discount);
        this.quantity = allocateAndCopy(data.quantity);
        this.parsedShipDate = allocateAndCopy(data.parsedShipDate);
        this.resultMask = allocateAndCopy(data.resultMaskByte);
    }

    private MemorySegment allocateAndCopy(byte[] data)
    {
        MemorySegment result = allocateInternal(data.length);
        result.copyFrom(MemorySegment.ofArray(data));
        return result;
    }

    private MemorySegment allocateAndCopy(long[] data)
    {
        MemorySegment result = allocateInternal(data.length * Long.BYTES);
        result.copyFrom(MemorySegment.ofArray(data));
        return result;
    }

    private MemorySegment allocateAndCopy(int[] data)
    {
        MemorySegment result = allocateInternal(data.length * Integer.BYTES);
        result.copyFrom(MemorySegment.ofArray(data));
        return result;
    }

    private static MemorySegment allocateInternal(int sizeInBytes)
    {
        long address = unsafe.allocateMemory(sizeInBytes);
        return MemorySegment.ofAddress(address, sizeInBytes);
    }

    private static final Unsafe unsafe;

    static {
        try {
            // fetch theUnsafe object
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
            if (unsafe == null) {
                throw new RuntimeException("Unsafe access not available");
            }
        }
        catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
