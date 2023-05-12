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

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.LongVector;
import jdk.incubator.vector.Vector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;
import org.openjdk.jmh.annotations.CompilerControl;

import static org.weakref.eval.kernel.MemorySegmentKernel.parseDate;

public class FilterRowArraysNoNullsVector
{
    private static final int MIN_DATE = parseDate("1994-01-01");
    private static final int MAX_DATE = parseDate("1995-01-01");

    private static final VectorSpecies<Byte> BYTE_SPECIES = ByteVector.SPECIES_PREFERRED;
    private static final VectorSpecies<Integer> INT_SPECIES = IntVector.SPECIES_PREFERRED;
    private static final VectorSpecies<Long> LONG_SPECIES = LongVector.SPECIES_PREFERRED;

    private static final VectorMask<Byte> OUTPUT_MASK;

    static {
        boolean[] flags = new boolean[BYTE_SPECIES.length()];
        flags[0] = true;
        flags[1] = true;
        OUTPUT_MASK = BYTE_SPECIES.loadMask(flags, 0);
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public static void evaluate(
            int count,
            byte[] selectedPositions,
            int[] shipDate,
            long[] discount,
            long[] quantity,
            byte[] result)
    {
        int i = 0;

        int bound = count - BYTE_SPECIES.length() * 2;
        for (; i < bound; i += BYTE_SPECIES.length()) {
            var positionsVector = ByteVector.fromArray(BYTE_SPECIES, selectedPositions, i);

            for (int p = 0; p < BYTE_SPECIES.length() / INT_SPECIES.length(); p++) {
                var shipDateVector = IntVector.fromArray(INT_SPECIES, shipDate, i + p * INT_SPECIES.length());

                Vector<Integer> intVector = positionsVector.castShape(INT_SPECIES, p)
                        .compare(VectorOperators.EQ, 0).not()
                        .and(shipDateVector.compare(VectorOperators.GE, MIN_DATE))
                        .and(shipDateVector.compare(VectorOperators.LT, MAX_DATE))
                        .toVector();

                for (int q = 0; q < INT_SPECIES.length() / LONG_SPECIES.length(); q++) {
                    int offset = i + p * INT_SPECIES.length() + q * LONG_SPECIES.length();

                    var discountVector = LongVector.fromArray(LONG_SPECIES, discount, offset);
                    var quantityVector = LongVector.fromArray(LONG_SPECIES, discount, offset);

                    VectorMask<Long> longVector = intVector.castShape(LONG_SPECIES, q)
                            .compare(VectorOperators.EQ, 0).not()
                            .and(discountVector.compare(VectorOperators.GE, 5L))
                            .and(discountVector.compare(VectorOperators.LE, 7L))
                            .and(quantityVector.compare(VectorOperators.LT, 2400L));

                    longVector.toVector().castShape(BYTE_SPECIES, 0)
                            .reinterpretAsBytes()
//                            .intoArray(result, offset, OUTPUT_MASK);
                            .intoArray(result, offset);
                }
            }
        }

        for (; i < count; i++) {
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

    public static void main(String[] args)
    {
        evaluate(10000, new byte[10000], new int[10000], new long[10000], new long[10000], new byte[10000]);
    }
}
