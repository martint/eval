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

public class Carry
{
    public static void main(String[] args)
    {
        int a = Integer.MAX_VALUE;
        int b = Integer.MAX_VALUE;
        int c = Integer.MAX_VALUE;

        add(a, b, c);
    }

    public static void add(int a, int b, int c)
    {
        int r = (a + b);

        int ca = r ^ a;
        int cb = r ^ b;
        int cc = r ^ c;
        int carryRaw = ca & cb ;
        int carry = carryRaw >>> 31;

        System.out.println(r);
        System.out.println(Integer.toBinaryString(carryRaw));
        System.out.println(Integer.toBinaryString(carry));

        
    }
}
