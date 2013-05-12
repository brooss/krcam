#include <jni.h>
#include <cstdio>
#include <cstring>
#include <cpu-features.h>
extern "C" {
#define VPX_CODEC_DISABLE_COMPAT 1
#include "vpx/vpx_encoder.h"
#include "vpx/vp8cx.h"
#define interface (vpx_codec_vp8_cx())

#include "krcam.h"


//TODO: Need this or android_getCpuFeatures doesn't link. Why?
jboolean Java_ws_websca_krcam_MainActivity_getCpuArmNeon( JNIEnv* env,
		jobject thiz )
{
	if(android_getCpuFamily()!=ANDROID_CPU_FAMILY_ARM)
		return JNI_FALSE;
	if ((android_getCpuFeatures() & ANDROID_CPU_ARM_FEATURE_NEON) != 0)
		return JNI_TRUE;
	else
		return JNI_FALSE;
}


static void mem_put_le16(char *mem, unsigned int val) {
	mem[0] = val;
	mem[1] = val>>8;
}

static void mem_put_le32(char *mem, unsigned int val) {
	mem[0] = val;
	mem[1] = val>>8;
	mem[2] = val>>16;
	mem[3] = val>>24;
}

static void write_ivf_file_header(FILE *outfile,
		const vpx_codec_enc_cfg_t *cfg,
		int frame_cnt) {
	char header[32];

	if(cfg->g_pass != VPX_RC_ONE_PASS && cfg->g_pass != VPX_RC_LAST_PASS)
		return;
	header[0] = 'D';
	header[1] = 'K';
	header[2] = 'I';
	header[3] = 'F';
	mem_put_le16(header+4,  0);                   /* version */
	mem_put_le16(header+6,  32);                  /* headersize */
	mem_put_le32(header+8,  fourcc);              /* headersize */
	mem_put_le16(header+12, cfg->g_w);            /* width */
	mem_put_le16(header+14, cfg->g_h);            /* height */
	mem_put_le32(header+16, cfg->g_timebase.den); /* rate */
	mem_put_le32(header+20, cfg->g_timebase.num); /* scale */
	mem_put_le32(header+24, frame_cnt);           /* length */
	mem_put_le32(header+28, 0);                   /* unused */

	(void) fwrite(header, 1, 32, outfile);
}

static void write_ivf_frame_header(FILE *outfile,
		const vpx_codec_cx_pkt_t *pkt)
{
	char             header[12];
	vpx_codec_pts_t  pts;

	if(pkt->kind != VPX_CODEC_CX_FRAME_PKT)
		return;

	pts = pkt->data.frame.pts;
	mem_put_le32(header, pkt->data.frame.sz);
	mem_put_le32(header+4, pts&0xFFFFFFFF);
	mem_put_le32(header+8, pts >> 32);

	(void) fwrite(header, 1, 12, outfile);
}

JNIEXPORT jstring JNICALL Java_ws_websca_krcam_MainActivity_vpxOpen( JNIEnv* env, jobject thiz, jstring path, jint w, jint h, jint threads )
{
	vpx_codec_err_t      res;

	int width = w;
	int height = h;
	const char *nativeString = env->GetStringUTFChars(path, 0);
	if(!(outfile = fopen(nativeString, "wb")))
		return env->NewStringUTF("Failed to open ivf for writing");
	env->ReleaseStringUTFChars(path, nativeString);

	if(!vpx_img_alloc(&raw, VPX_IMG_FMT_I420, width, height, 1))
		return env->NewStringUTF("Failed to allocate image");


	res = vpx_codec_enc_config_default(interface, &cfg, 0);
	if(res) {
		return env->NewStringUTF( vpx_codec_err_to_string(res));
	}

	cfg.rc_target_bitrate = width * height * cfg.rc_target_bitrate	/ cfg.g_w / cfg.g_h;
	cfg.g_w = width;
	cfg.g_h = height;
	cfg.g_threads=threads;

	if(vpx_codec_enc_init(&codec, interface, &cfg, 0))
		return env->NewStringUTF("Failed to initialize encoder");

	if(threads==2)
		vpx_codec_control(&codec, VP8E_SET_TOKEN_PARTITIONS, VP8_ONE_TOKENPARTITION);
	else if(threads>2)
		vpx_codec_control(&codec, VP8E_SET_TOKEN_PARTITIONS, VP8_TWO_TOKENPARTITION);

	write_ivf_file_header(outfile, &cfg, 0);

	return env->NewStringUTF("vpx ok");
}

JNIEXPORT jstring JNICALL Java_ws_websca_krcam_MainActivity_vpxNextFrame( JNIEnv* env, jobject thiz, jbyteArray input, jint w, jint h )
{
	vpx_codec_iter_t iter = NULL;
	const vpx_codec_cx_pkt_t *pkt;
	int                  flags = 0;
	char r[3];

	int frame_avail = 1;

	jbyte* bufferPtr = env->GetByteArrayElements(input, 0);
	jsize lengthOfArray = env->GetArrayLength(input);

	memcpy(raw.planes[0], bufferPtr, w*h);
	for(int x=0; x<(w*h)/2; x+=2) {
		raw.planes[1][x/2]=bufferPtr[(w*h)+(x+1)];
		raw.planes[2][x/2]=bufferPtr[(w*h)+(x)];
	}

	env->ReleaseByteArrayElements(input, bufferPtr, 0);

	if(vpx_codec_encode(&codec, frame_avail? &raw : NULL, frame_cnt, 1, flags, VPX_DL_REALTIME))
		return  env->NewStringUTF("Failed to encode frame\n");

	while( (pkt = vpx_codec_get_cx_data(&codec, &iter)) ) {

		switch(pkt->kind) {
		case VPX_CODEC_CX_FRAME_PKT:                                  //
			write_ivf_frame_header(outfile, pkt);                     //
			(void) fwrite(pkt->data.frame.buf, 1, pkt->data.frame.sz, //
					outfile);                                   //
			break;                                                    //
		default:
			break;
		}
		sprintf(r, pkt->kind == VPX_CODEC_CX_FRAME_PKT
		       && (pkt->data.frame.flags & VPX_FRAME_IS_KEY)? "K":".");
		//fflush(stdout);
	}
	frame_cnt++;
	return env->NewStringUTF(r);
}
}
