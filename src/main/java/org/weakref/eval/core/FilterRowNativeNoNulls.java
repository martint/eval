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

import org.openjdk.jmh.annotations.CompilerControl;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentScope;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.nio.file.Paths;

import static com.google.common.base.Throwables.throwIfUnchecked;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static org.weakref.eval.kernel.MemorySegmentKernel.parseDate;

public class FilterRowNativeNoNulls
{
    public static final Linker LINKER = Linker.nativeLinker();
    public static final SymbolLookup LOOKUP = SymbolLookup.libraryLookup(Paths.get("src", "main", "resources", Native.resourceName("filter")), SegmentScope.global());
    
    private static final MethodHandle FILTER = LINKER.downcallHandle(
            LOOKUP.find("filter").get(),
            FunctionDescriptor.ofVoid(
                    JAVA_INT, // count
                    ADDRESS, // selectedPositions
                    ADDRESS, // shipDate
                    ADDRESS, // discount
                    ADDRESS, // quantity
                    ADDRESS, // result
                    JAVA_INT, // minDate
                    JAVA_INT, // maxDate
                    JAVA_LONG, // minDiscount
                    JAVA_LONG, // maxDiscount
                    JAVA_LONG // maxQuantity
            ));
    
    private static final int MIN_DATE = parseDate("1994-01-01");
    private static final int MAX_DATE = parseDate("1995-01-01");

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public static void evaluate(
            int count,
            MemorySegment selectedPositions,
            MemorySegment shipDate,
            MemorySegment discount,
            MemorySegment quantity,
            MemorySegment result)
    {
        try {
            FILTER.invokeExact(
                    count,
                    selectedPositions,
                    shipDate,
                    discount,
                    quantity,
                    result,
                    MIN_DATE,
                    MAX_DATE,
                    5L,
                    7L,
                    2400L);
        }
        catch (Throwable e) {
            throwIfUnchecked(e);
            throw new RuntimeException(e);
        }
    }
}
