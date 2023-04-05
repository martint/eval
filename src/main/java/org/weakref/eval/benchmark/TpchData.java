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

import io.airlift.slice.BasicSliceInput;
import io.airlift.slice.SliceInput;
import io.airlift.slice.Slices;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

@State(Scope.Thread)
public class TpchData
{
    public int positions;
    public int[] inputPositions;
    public byte[] inputMask;
    public long[] inputMaskBM;

    public long[] discount;
    public boolean[] discountNull;
    public byte[] discountNullByte;

    public long[] extendedPrice;
    public boolean[] extendedPriceNull;
    public byte[] extendedPriceNullByte;

    public long[] quantity;
    public boolean[] quantityNull;
    public byte[] quantityNullByte;

    public int[] shipDatePositions;
    public byte[] shipDate;
    public boolean[] shipDateNull;
    public byte[] shipDateNullByte;

    public long[] result;
    public boolean[] resultNull;
    public byte[] resultNullByte;
    public boolean[] resultMask;
    public byte[] resultMaskByte;
    public long[] resultMaskBM;

    public int[] tempPositions1;
    public int[] tempPositions2;

    @Setup
    public void initialize()
            throws IOException
    {
        positions = 10_240;

        inputPositions = new int[positions];
        for (int i = 0; i < positions; i++) {
            inputPositions[i] = i;
        }

        inputMask = new byte[positions];
        Arrays.fill(inputMask, (byte) 1);

        inputMaskBM = new long[Math.floorDiv(positions, 64) + 1];
        Arrays.fill(inputMaskBM, 0xffffffffffffffffL);

        tempPositions1 = new int[positions];
        for (int i = 0; i < positions; i++) {
            tempPositions1[i] = i;
        }

        tempPositions2 = new int[positions];
        for (int i = 0; i < positions; i++) {
            tempPositions1[i] = i;
        }

        discount = new long[positions];
        extendedPrice = new long[positions];
        quantity = new long[positions];
        shipDatePositions = new int[positions + 1];
        shipDate = new byte[positions * 10];

        byte[] buffer = Files.readAllBytes(Paths.get("data.bin"));
        SliceInput input = new BasicSliceInput(Slices.wrappedBuffer(buffer));

        positions = input.readInt();

        discount = new long[positions];
        discountNull = new boolean[positions];
        discountNullByte = new byte[positions];
        for (int i = 0; i < discount.length; i++) {
            discount[i] = input.readLong();
        }

        extendedPrice = new long[positions];
        extendedPriceNull = new boolean[positions];
        extendedPriceNullByte = new byte[positions];
        for (int i = 0; i < extendedPrice.length; i++) {
            extendedPrice[i] = input.readLong();
        }

        quantity = new long[positions];
        quantityNull = new boolean[positions];
        quantityNullByte = new byte[positions];
        for (int i = 0; i < quantity.length; i++) {
            quantity[i] = input.readLong();
        }

        shipDatePositions = new int[positions + 1];
        shipDateNull = new boolean[positions];
        shipDateNullByte = new byte[positions];
        for (int i = 0; i < shipDatePositions.length; i++) {
            shipDatePositions[i] = input.readInt();
        }

        shipDate = new byte[shipDatePositions[positions]];
        input.read(shipDate);

        result = new long[positions];

        resultNull = new boolean[positions];
        resultNullByte = new byte[positions];

        resultMask = new boolean[positions];
        resultMaskByte = new byte[positions];
        resultMaskBM = new long[Math.floorDiv(positions, 64) + 1];
    }
}
