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
package org.weakref.eval.proto.function;

import org.weakref.eval.proto.vector.Int128Vector;
import org.weakref.eval.proto.vector.Int128VectorView;
import org.weakref.eval.proto.mask.Selection;

public class AddDecimalDecimalFunction
{
    public void add(Int128Vector left, Int128Vector right, Selection selection, Int128Vector output)
    {
        Int128VectorView view = new Int128VectorView(output);

        selection.stream().forEach(i -> {
            view.position(i);
            add(left, i, right, i, view);
        });
    }

    // TODO: view or vector+position ?
    public void add(Int128Vector left, int leftPosition, Int128Vector right, int rightPosition, Int128VectorView output)
    {
        long leftLow = left.low(leftPosition);
        long leftHigh = left.high(leftPosition);

        long rightLow = right.low(rightPosition);
        long rightHigh = right.high(rightPosition);

        long resultLow = leftLow + rightLow;
        long resultHigh = leftHigh + rightHigh;
        // TODO: carry

        output.set(resultLow, resultHigh);
    }
}
