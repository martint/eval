// gcc -c -Wall -Werror -O3 -march=native filter.c
// gcc -shared filter.o -o filter.so

void filter(
    int count,
    int* selectedPositions,
    int* shipDate,
    int* discount,
    int* quantity,
    char* result,
    int minDate,
    int maxDate,
    long minDiscount,
    long maxDiscount,
    long maxQuantity)
{
    for (int i = 0; i < count; ++i) {
        result[i] = shipDate[i] >= minDate &
                    shipDate[i] < maxDate &
                    discount[i] >= minDiscount &
                    discount[i] < maxDiscount &
                    quantity[i] < maxQuantity;
    }
}
