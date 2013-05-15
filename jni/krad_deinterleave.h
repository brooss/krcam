#ifndef KRAD_CODEC_HEADER_H
#define KRAD_CODEC_HEADER_H

#include <stdint.h>
#include <stdlib.h>
//#if defined __ARM_NEON__
#include <arm_neon.h>
//#endif

static void deinterleave_nv21_to_i420(const uint8_t *srcAB, uint8_t *dstA, uint8_t *dstB, size_t srcABLength);
static void deinterleave_nv21_to_i420_neon(const uint8_t *srcAB, uint8_t *dstA, uint8_t *dstB, size_t srcABLength);

#endif
