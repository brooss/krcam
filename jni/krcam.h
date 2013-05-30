#ifndef KRCAM_H_
#define KRCAM_H_

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
#include "krad_nanolib/krad_vorbis.h"
#include "krad_nanolib/krad_deinterleave.h"
#include "krcam_util.h"

typedef struct kr_cam_St kr_cam_t;
typedef struct kr_cam_params_St kr_cam_params_t;

struct kr_cam_params_St {
	uint32_t width;
	uint32_t height;
	//uint32_t video_bitrate;
	bool use_audio;
	float audio_quality;
	uint32_t sample_rate;
	uint32_t channels;
	char *filename;
	char *host;
	int32_t port;
	char *mount;
	char *password;
};

struct kr_cam_St {
	kr_cam_params_t *params;
	kr_mkv_t *stream;
	vpx_image_t raw;
	vpx_codec_ctx_t codec;
	uint32_t video_track_id;
	uint32_t audio_track_id;
	uint32_t frame_count;
	krad_vorbis_t *vorbis;
	//kr_vpx_t *vpx;
	krad_ringbuffer_t *audio_ring;
	//uint64_t total_samples;
};

static void deinterleave(const uint8_t *srcAB, uint8_t *dstA, uint8_t *dstB, size_t srcABLength);
static void deinterleave_no_neon(const uint8_t *srcAB, uint8_t *dstA, uint8_t *dstB, size_t srcABLength);
static void deinterleave_neon(const uint8_t *srcAB, uint8_t *dstA, uint8_t *dstB, size_t srcABLength);
static kr_cam_params_t* init_params(char *path, int w, int h, bool use_audio, int audioSampleRate, int audioQuality);
static void free_params(kr_cam_params_t* params);
static void kr_cam_run_audio (kr_cam_t *cam);
}
#endif
