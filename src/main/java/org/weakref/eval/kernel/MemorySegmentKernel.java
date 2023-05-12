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
package org.weakref.eval.kernel;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.openjdk.jmh.annotations.CompilerControl;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.time.LocalDate;
import java.util.OptionalInt;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.toIntExact;

public class MemorySegmentKernel
{
    private static final int MIN_DATE = parseDate("1994-01-01");
    private static final int MAX_DATE = parseDate("1995-01-01");

    private final int count;
    private final MemorySegment selectedPositions;
    private final MemorySegment shipdate;
    private final MemorySegment discount;
    private final MemorySegment quantity;
    private final MemorySegment result;

    public MemorySegmentKernel(
            int count,
            MemorySegment selectedPositions,
            MemorySegment shipdate,
            MemorySegment discount,
            MemorySegment quantity,
            MemorySegment result)
    {
        this.count = count;
        this.selectedPositions = selectedPositions;
        this.shipdate = shipdate;
        this.discount = discount;
        this.quantity = quantity;
        this.result = result;
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void process()
    {
        for (int k = 0; k < count; ++k) {
//            result.set(ValueLayout.JAVA_BOOLEAN, k,
//                    selectedPositions.get(ValueLayout.JAVA_BOOLEAN, k));

            int shipdateValue = shipdate.getAtIndex(ValueLayout.JAVA_INT_UNALIGNED, k);
            long discountValue = discount.getAtIndex(ValueLayout.JAVA_LONG_UNALIGNED, k);

            result.set(ValueLayout.JAVA_BOOLEAN, k,
                    selectedPositions.get(ValueLayout.JAVA_BOOLEAN, k) &
                            shipdateValue >= MIN_DATE &
                            shipdateValue < MAX_DATE &
                            discountValue >= 5 &
                            discountValue <= 7 &
                            quantity.getAtIndex(ValueLayout.JAVA_LONG_UNALIGNED, k) < 2400);
        }
    }

    public MemorySegment result()
    {
        return result;
    }

    private static final DateTimeFormatter DATE_FORMATTER = ISODateTimeFormat.date().withZoneUTC();
    
    public static int parseDate(String value)
    {
        // Note: update DomainTranslator.Visitor.createVarcharCastToDateComparisonExtractionResult whenever varchar->date conversion (CAST) behavior changes.

        // in order to follow the standard, we should validate the value:
        // - the required format is 'YYYY-MM-DD'
        // - all components should be unsigned numbers
        // https://github.com/trinodb/trino/issues/10677

        OptionalInt days = parseIfIso8601DateFormat(value);
        if (days.isPresent()) {
            return days.getAsInt();
        }
        return toIntExact(TimeUnit.MILLISECONDS.toDays(DATE_FORMATTER.parseMillis(value)));
    }

    static OptionalInt parseIfIso8601DateFormat(String value)
    {
        if (value.length() != 10 || value.charAt(4) != '-' || value.charAt(7) != '-') {
            return OptionalInt.empty();
        }

        OptionalInt year = parseIntSimple(value, 0, 4);
        if (year.isEmpty()) {
            return OptionalInt.empty();
        }

        OptionalInt month = parseIntSimple(value, 5, 2);
        if (month.isEmpty()) {
            return OptionalInt.empty();
        }

        OptionalInt day = parseIntSimple(value, 8, 2);
        if (day.isEmpty()) {
            return OptionalInt.empty();
        }

        LocalDate date = LocalDate.of(year.getAsInt(), month.getAsInt(), day.getAsInt());
        return OptionalInt.of(toIntExact(date.toEpochDay()));
    }

    private static OptionalInt parseIntSimple(String input, int offset, int length)
    {
        checkArgument(length > 0, "Invalid length %s", length);

        int result = 0;
        for (int i = 0; i < length; i++) {
            int digit = input.charAt(offset + i) - '0';
            if (digit < 0 || digit > 9) {
                return OptionalInt.empty();
            }
            result = result * 10 + digit;
        }
        return OptionalInt.of(result);
    }
}
