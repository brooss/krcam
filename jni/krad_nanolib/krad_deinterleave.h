#ifndef KRAD_DEINTERLEAVE_HEADER_H
#define KRAD_DEINTERLEAVE_HEADER_H

#include <stdint.h>
#include <stdlib.h>


void deinterleave_nv21_to_i420(const uint8_t *srcAB, uint8_t *dstA, uint8_t *dstB, size_t srcABLength);
void deinterleave_nv21_to_i420_neon(const uint8_t *srcAB, uint8_t *dstA, uint8_t *dstB, size_t srcABLength);

#endif
