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

import org.weakref.eval.proto.vector.Int64Vector;
import org.weakref.eval.proto.mask.Selection;

public class MultiplyBigintBigintFunction
{
    public void multiply(Int64Vector left, Int64Vector right, Selection selection, Int64Vector output)
    {
        selection.stream().forEach(i -> output.set(i, multiply(left.get(i), right.get(i))));
    }

    public void multiply(Int64Vector left, long right, Selection selection, Int64Vector output)
    {
        selection.stream().forEach(i -> output.set(i, multiply(left.get(i), right)));
    }

    public void multiply(long left, Int64Vector right, Selection selection, Int64Vector output)
    {
        selection.stream().forEach(i -> output.set(i, multiply(left, right.get(i))));
    }

    // TODO: need variant for dictionary indirection?

    public long multiply(long left, long right)
    {
        return left * right;
    }
}
