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
package org.weakref.eval.swiss;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorSpecies;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.Long.rotateLeft;
import static java.lang.Math.toIntExact;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

public class SwissVector
{
    private static final VectorSpecies<Byte> SPECIES = ByteVector.SPECIES_PREFERRED;
    private static final VarHandle LONG_HANDLE = MethodHandles.byteArrayViewVarHandle(long[].class, LITTLE_ENDIAN);
    private static final int VALUE_WIDTH = Long.BYTES;

    private final byte[] control;
    private final byte[] values;

    private final int capacity;
    private final int mask;

    private int size;
    private final int maxSize;

    public SwissVector(int maxSize)
    {
        checkArgument(maxSize > 0, "maxSize must be greater than 0");
        long expandedSize = maxSize * 8L / 7L;
        expandedSize = Math.max(SPECIES.length(), 1L << (64 - Long.numberOfLeadingZeros(expandedSize - 1)));
        checkArgument(expandedSize < (1L << 30), "Too large (" + maxSize + " expected elements with load factor 7/8)");
        capacity = (int) expandedSize;

        this.maxSize = maxSize;
        mask = capacity - 1;
        control = new byte[capacity + SPECIES.length()];
        values = new byte[toIntExact(((long) VALUE_WIDTH * capacity))];
    }

    public boolean put(long value)
    {
        checkState(size < maxSize, "Table is full");

        long hash = hash(value);

        byte hashPrefix = (byte) (hash & 0x7F | 0x80);
        int bucket = bucket((int) (hash >> 7));

        while (true) {
            ByteVector controlVector = ByteVector.fromArray(SPECIES, control, bucket);

            long matches = controlVector.eq(hashPrefix).toLong();
            while (matches != 0) {
                int index = bucket(bucket + Long.numberOfTrailingZeros(matches)) * VALUE_WIDTH;

                if ((long) LONG_HANDLE.get(values, index) == value) {
                    return true;
                }

                matches = matches & (matches - 1);
            }

            VectorMask<Byte> isEmpty = controlVector.eq((byte) 0);
            if (isEmpty.anyTrue()) {
                int emptyIndex = bucket(bucket + isEmpty.firstTrue());
                control[emptyIndex] = hashPrefix;
                if (emptyIndex < SPECIES.length()) {
                    control[emptyIndex + capacity] = hashPrefix;
                }

                int index = emptyIndex * VALUE_WIDTH;
                LONG_HANDLE.set(values, index, value);

                size++;
                return true;
            }

            bucket = bucket(bucket + SPECIES.length());
        }
    }

    public boolean find(long value)
    {
        long hash = hash(value);

        byte hashPrefix = (byte) (hash & 0x7F | 0x80);
        int bucket = bucket((int) (hash >> 7));

        while (true) {
            ByteVector controlVector = ByteVector.fromArray(SPECIES, control, bucket);

            long matches = controlVector.eq(hashPrefix).toLong();
            while (matches != 0) {
                int index = bucket(bucket + Long.numberOfTrailingZeros(matches)) * VALUE_WIDTH;

                if ((long) LONG_HANDLE.get(values, index) == value) {
                    return true;
                }

                matches = matches & (matches - 1);
            }

            VectorMask<Byte> isEmpty = controlVector.eq((byte) 0);
            if (isEmpty.anyTrue()) {
                return false;
            }

            bucket = bucket(bucket + SPECIES.length());
        }
    }

    private int bucket(int hash)
    {
        return hash & mask;
    }

    private long hash(long value)
    {
        // xxHash64 mix
        return rotateLeft(value * 0xC2B2AE3D27D4EB4FL, 31) * 0x9E3779B185EBCA87L;
    }

    public static void main(String[] args)
    {
        SwissVector table = new SwissVector(1000);
        for (int i = 0; i < 1000; ++i) {
            table.put(i);
        }

        System.out.println(table.find(999));
    }
}
