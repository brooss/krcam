#include "krad_deinterleave.h"
#if defined __ARM_NEON__
#include <arm_neon.h>
#endif
void deinterleave_nv21_to_i420(const uint8_t *srcAB, uint8_t *dstA, uint8_t *dstB, size_t srcABLength)
{
    int i;
    div_t srcABLength_8 = div(srcABLength, 8);
    if (srcABLength_8.rem == 0)
    {
        typedef union
        {
            uint64_t wide;
            struct { uint8_t a1; uint8_t b1; uint8_t a2; uint8_t b2; uint8_t a3; uint8_t b3; uint8_t a4; uint8_t b4; } narrow;
        } ab8x8_t;

        uint64_t *srcAB64 = (uint64_t *)srcAB;
        int j = 0;
        for (i = 0; i < srcABLength_8.quot; i++)
        {
            ab8x8_t cursor;
            cursor.wide = srcAB64[i];
            dstA[j  ] = cursor.narrow.a1;
            dstB[j++] = cursor.narrow.b1;
            dstA[j  ] = cursor.narrow.a2;
            dstB[j++] = cursor.narrow.b2;
            dstA[j  ] = cursor.narrow.a3;
            dstB[j++] = cursor.narrow.b3;
            dstA[j  ] = cursor.narrow.a4;
            dstB[j++] = cursor.narrow.b4;
        }
        return;
    }

    // iterate 4-bytes at a time
    div_t srcABLength_4 = div(srcABLength, 4);
    if (srcABLength_4.rem == 0)
    {
        typedef union
        {
            uint32_t wide;
            struct { uint8_t a1; uint8_t b1; uint8_t a2; uint8_t b2; } narrow;
        } ab8x4_t;

        uint32_t *srcAB32 = (uint32_t *)srcAB;
        int j = 0;
        for (i = 0; i < srcABLength_4.quot; i++)
        {
            ab8x4_t cursor;
            cursor.wide = srcAB32[i];
            dstA[j  ] = cursor.narrow.a1;
            dstB[j++] = cursor.narrow.b1;
            dstA[j  ] = cursor.narrow.a2;
            dstB[j++] = cursor.narrow.b2;
        }
        return;
    }

    // iterate 2-bytes at a time
    div_t srcABLength_2 = div(srcABLength, 2);
    typedef union
    {
        uint16_t wide;
        struct { uint8_t a; uint8_t b; } narrow;
    } ab8x2_t;

    uint16_t *srcAB16 = (uint16_t *)srcAB;
    for (i = 0; i < srcABLength_2.quot; i++)
    {
        ab8x2_t cursor;
        cursor.wide = srcAB16[i];
        dstA[i] = cursor.narrow.a;
        dstB[i] = cursor.narrow.b;
    }
}

void deinterleave_nv21_to_i420_neon(const uint8_t *srcAB, uint8_t *dstA, uint8_t *dstB, size_t srcABLength)
{
#if defined __ARM_NEON__
    // attempt to use NEON intrinsics

    // iterate 32-bytes at a time
    div_t srcABLength_32 = div(srcABLength, 32);
    if (srcABLength_32.rem == 0)
    {
        while (srcABLength_32.quot --> 0)
        {
            const uint8x16x2_t ab = vld2q_u8(srcAB);
            vst1q_u8(dstA, ab.val[0]);
            vst1q_u8(dstB, ab.val[1]);
            srcAB += 32;
            dstA += 16;
            dstB += 16;
        }
        return;
    }

    // iterate 16-bytes at a time
    div_t srcABLength_16 = div(srcABLength, 16);
    if (srcABLength_16.rem == 0)
    {
        while (srcABLength_16.quot --> 0)
        {
            const uint8x8x2_t ab = vld2_u8(srcAB);
            vst1_u8(dstA, ab.val[0]);
            vst1_u8(dstB, ab.val[1]);
            srcAB += 16;
            dstA += 8;
            dstB += 8;
        }
        return;
    }
#endif

    // if the bytes were not aligned properly
    // or NEON is unavailable, fall back to
    // an optimized iteration

    // iterate 8-bytes at a time
    deinterleave_nv21_to_i420(srcAB, dstA, dstB, srcABLength);
}
