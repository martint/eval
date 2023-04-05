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

import org.weakref.eval.proto.vector.BooleanVector;

import java.util.Arrays;
import java.util.stream.IntStream;

public class Selection
{
    private final int[] positions;
    private final int size;

    public Selection(int[] positions, int size)
    {
        this.positions = positions;
        this.size = size;
    }

    public static Selection fromVector(BooleanVector vector)
    {
        int[] positions = new int[vector.size()];
        int output = 0;
        for (int i = 0; i < positions.length; i++) {
            if (vector.get(i)) {
                positions[output] = i;
                output++;
            }
        }

        return new Selection(positions, output);
    }

    public IntStream stream()
    {
        return Arrays.stream(positions).limit(size);
    }
}
