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
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;
import org.openjdk.jmh.annotations.CompilerControl;

import static org.weakref.eval.kernel.MemorySegmentKernel.parseDate;

public class FilterRowArraysNoNullsVector2
{
    private static final int MIN_DATE = parseDate("1994-01-01");
    private static final int MAX_DATE = parseDate("1995-01-01");

    private static final VectorSpecies<Byte> BYTE_SPECIES = ByteVector.SPECIES_PREFERRED;
    private static final VectorSpecies<Integer> INT_SPECIES = IntVector.SPECIES_PREFERRED;
    private static final VectorSpecies<Long> LONG_SPECIES = LongVector.SPECIES_PREFERRED;

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public static void evaluate(
            int count,
            byte[] selectedPositions,
            int[] shipDate,
            long[] discount,
            long[] quantity,
            byte[] result,
            boolean[] mask1,
            boolean[] mask2)
    {
        int bound = Math.min(Math.min(INT_SPECIES.loopBound(count), LONG_SPECIES.loopBound(count)), BYTE_SPECIES.loopBound(count));

        for (int i = 0; i < bound; i += INT_SPECIES.length()) {
            var shipDateVector = IntVector.fromArray(INT_SPECIES, shipDate, i);

            shipDateVector.compare(VectorOperators.GE, MIN_DATE)
                    .and(shipDateVector.compare(VectorOperators.LT, MAX_DATE))
                    .intoArray(mask1, i);
        }

        for (int i = 0; i < bound; i += LONG_SPECIES.length()) {
            var discountVector = LongVector.fromArray(LONG_SPECIES, discount, i);
            var quantityVector = LongVector.fromArray(LONG_SPECIES, discount, i);

            discountVector.compare(VectorOperators.GE, 5L)
                    .and(discountVector.compare(VectorOperators.LE, 7L))
                    .and(quantityVector.compare(VectorOperators.LT, 2400L))
                    .intoArray(mask2, i);
        }

        for (int i = 0; i < bound; i += BYTE_SPECIES.length()) {
            ByteVector.fromArray(BYTE_SPECIES, selectedPositions, i)
                    .compare(VectorOperators.EQ, 0).not()
                    .and(BYTE_SPECIES.loadMask(mask1, i))
                    .and(BYTE_SPECIES.loadMask(mask2, i))
                    .toVector()
                    .reinterpretAsBytes()
                    .intoArray(result, i);
        }

        for (int i = bound; i < count; i++) {
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
        evaluate(
                10000,
                new byte[10000],
                new int[10000],
                new long[10000], 
                new long[10000],
                new byte[10000],
                new boolean[10000],
                new boolean[10000]);
    }
}
