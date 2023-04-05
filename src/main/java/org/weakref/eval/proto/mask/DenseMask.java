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
package org.weakref.eval.proto.mask;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorSpecies;

import java.util.function.IntConsumer;

public record DenseMask(boolean[] mask)
{
    private static final VectorSpecies<Byte> BYTE_SPECIES = ByteVector.SPECIES_PREFERRED;
    
    public boolean get(int position)
    {
        return mask[position];
    }

    public void forEach(IntConsumer action)
    {
        for (int i = 0; i < mask.length; i++) {
            if (mask[i]) {
                action.accept(i);
            }
        }
    }

    public DenseVectorized toVectorized()
    {
        byte[] vector = new byte[mask.length];
        for (int i = 0; i < mask.length; i++) {
            vector[i] = (byte) (mask[i] ? 1 : 0);
        }

        return new DenseVectorized(vector);
    }

    public SparseMask toSparse()
    {
        int[] positions = new int[mask.length];
        int count = 0;
        for (int i = 0; i < mask.length; i++) {
            if (mask[i]) {
                positions[count] = i;
                count++;
            }
        }

        return new SparseMask(positions, count);
    }
}
