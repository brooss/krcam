#ifndef KRCAM_UTIL_H_
#define KRCAM_UTIL_H_

#include <jni.h>
#include <cpu-features.h>
#include <stdio.h>
#include "vpx/vpx_encoder.h"
#include "vpx/vp8cx.h"

static void mem_put_le16(char *mem, unsigned int val);
static void mem_put_le32(char *mem, unsigned int val);
void write_ivf_frame_header(FILE *outfile, const vpx_codec_cx_pkt_t *pkt);
void write_ivf_file_header(FILE *outfile, const vpx_codec_enc_cfg_t *cfg, int frame_cnt);
void fill_frame(int frame_number, int width, int height, vpx_image_t *img);
void int16_to_float (float *dst, char *src, uint32_t nsamples, uint32_t src_skip);

//libvpx
#define fourcc    0x30385056

#define IVF_FILE_HDR_SZ  (32)
#define IVF_FRAME_HDR_SZ (12)

static const uint8_t sine_table[256] = {
  128, 131, 134, 137, 140, 143, 146, 149,
  152, 156, 159, 162, 165, 168, 171, 174,
  176, 179, 182, 185, 188, 191, 193, 196,
  199, 201, 204, 206, 209, 211, 213, 216,
  218, 220, 222, 224, 226, 228, 230, 232,
  234, 236, 237, 239, 240, 242, 243, 245,
  246, 247, 248, 249, 250, 251, 252, 252,
  253, 254, 254, 255, 255, 255, 255, 255,
  255, 255, 255, 255, 255, 255, 254, 254,
  253, 252, 252, 251, 250, 249, 248, 247,
  246, 245, 243, 242, 240, 239, 237, 236,
  234, 232, 230, 228, 226, 224, 222, 220,
  218, 216, 213, 211, 209, 206, 204, 201,
  199, 196, 193, 191, 188, 185, 182, 179,
  176, 174, 171, 168, 165, 162, 159, 156,
  152, 149, 146, 143, 140, 137, 134, 131,
  128, 124, 121, 118, 115, 112, 109, 106,
  103, 99, 96, 93, 90, 87, 84, 81,
  79, 76, 73, 70, 67, 64, 62, 59,
  56, 54, 51, 49, 46, 44, 42, 39,
  37, 35, 33, 31, 29, 27, 25, 23,
  21, 19, 18, 16, 15, 13, 12, 10,
  9, 8, 7, 6, 5, 4, 3, 3,
  2, 1, 1, 0, 0, 0, 0, 0,
  0, 0, 0, 0, 0, 0, 1, 1,
  2, 3, 3, 4, 5, 6, 7, 8,
  9, 10, 12, 13, 15, 16, 18, 19,
  21, 23, 25, 27, 29, 31, 33, 35,
  37, 39, 42, 44, 46, 49, 51, 54,
  56, 59, 62, 64, 67, 70, 73, 76,
  79, 81, 84, 87, 90, 93, 96, 99,
  103, 106, 109, 112, 115, 118, 121, 124
};

#endif /* KRCAM_UTIL_H_ */
