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
package org.weakref.eval.proto.vector;

public final class Int128Vector
{
    private final long[] values;

    public Int128Vector(long[] values)
    {
        this.values = values;
    }

    public long high(int position)
    {
        return values[position * 2 + 1];
    }

    public long low(int position)
    {
        return values[position * 2];
    }

    public void set(int position, long low, long high)
    {
        values[position * 2] = low;
        values[position * 2 + 1] = high;
    }

    public int size()
    {
        return values.length / 2;
    }
}
