#ifndef KRCAM_UTIL_H_
#define KRCAM_UTIL_H_

#include <jni.h>
#include <cstdio>
#include <cstring>
#include <cpu-features.h>
extern "C" {
#define VPX_CODEC_DISABLE_COMPAT 1
#include "vpx/vpx_encoder.h"
#include "vpx/vp8cx.h"
#define interface (vpx_codec_vp8_cx())

#include "krad_nanolib/krad_mkv.h"
#include "krad_nanolib/krad_deinterleave.h"
#include "krcam_util.h"

//libvpx ugly globals
//FILE                *infile, *outfile;
kr_mkv_t			*kr_stream;
int					video_track;
vpx_codec_enc_cfg_t  cfg;
int                  frame_cnt = 0;
vpx_image_t          raw;
vpx_codec_ctx_t      codec;



static void deinterleave(const uint8_t *srcAB, uint8_t *dstA, uint8_t *dstB, size_t srcABLength);
static void deinterleave_no_neon(const uint8_t *srcAB, uint8_t *dstA, uint8_t *dstB, size_t srcABLength);
static void deinterleave_neon(const uint8_t *srcAB, uint8_t *dstA, uint8_t *dstB, size_t srcABLength);
}
#endif
