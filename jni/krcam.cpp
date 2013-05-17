#include "krcam.h"
extern "C" {
//TODO: Need this or android_getCpuFeatures doesn't link. Why?
JNIEXPORT jboolean Java_ws_websca_krcam_MainActivity_getCpuArmNeon( JNIEnv* env,
		jobject thiz )
{
	if(android_getCpuFamily()!=ANDROID_CPU_FAMILY_ARM)
		return JNI_FALSE;
	if ((android_getCpuFeatures() & ANDROID_CPU_ARM_FEATURE_NEON) != 0)
		return JNI_TRUE;
	else
		return JNI_FALSE;
}

JNIEXPORT jstring JNICALL Java_ws_websca_krcam_MainActivity_vpxOpen( JNIEnv* env, jobject thiz, jstring path, jint w, jint h, jint threads )
{
	vpx_codec_err_t res;
	int width = w;
	int height = h;

	char host[256];
	char mount[256];
	char password[256];
	int32_t port;

	snprintf (host, sizeof(host), "%s", "europa.kradradio.com");
	snprintf (mount, sizeof(mount), "/krcam_%"PRIu64".webm", krad_unixtime());
	snprintf (password, sizeof(password), "%s", "firefox");

	port = 8008;
	//kr_stream = kr_mkv_create_stream (host, port, mount, password);

	char *nativeString = (char*)env->GetStringUTFChars(path, 0);
	kr_stream = kr_mkv_create_file ((char *)nativeString);

	/*if(!(outfile = fopen(nativeString, "wb")))
		return env->NewStringUTF("Failed to open ivf for writing");*/
	env->ReleaseStringUTFChars(path, nativeString);

	if(!vpx_img_alloc(&raw, VPX_IMG_FMT_I420, width, height, 1))
		return env->NewStringUTF("Failed to allocate image");


	res = vpx_codec_enc_config_default(interface, &cfg, 0);
	if(res) {
		return env->NewStringUTF( vpx_codec_err_to_string(res));
	}

	cfg.rc_target_bitrate = 1000;
	cfg.g_w = width;
	cfg.g_h = height;
	cfg.g_threads = 4;

	if(vpx_codec_enc_init(&codec, interface, &cfg, 0))
		return env->NewStringUTF("Failed to initialize encoder");

	if(threads==2)
		vpx_codec_control(&codec, VP8E_SET_TOKEN_PARTITIONS, VP8_ONE_TOKENPARTITION);
	else if(threads>2)
		vpx_codec_control(&codec, VP8E_SET_TOKEN_PARTITIONS, VP8_TWO_TOKENPARTITION);

	//write_ivf_file_header(outfile, &cfg, 0);

	video_track = kr_mkv_add_video_track (kr_stream, VP8, 25, 1, w, h);

	return env->NewStringUTF("vpx ok");
}

JNIEXPORT jstring JNICALL Java_ws_websca_krcam_MainActivity_vpxNextFrame( JNIEnv* env, jobject thiz, jbyteArray input, jint w, jint h, jint tc )
{
	vpx_codec_iter_t iter = NULL;
	const vpx_codec_cx_pkt_t *pkt;
	int                  flags = 0;
	char r[3];

	int frame_avail = 1;

	jbyte* bufferPtr = env->GetByteArrayElements(input, 0);
	jsize lengthOfArray = env->GetArrayLength(input);

	raw.planes[0] = (uint8_t *)bufferPtr;
	deinterleave ((uint8_t *)bufferPtr + (w*h), raw.planes[2], raw.planes[1], (w*h) / 2);
	//fill_frame(frame_cnt, w, h, &raw);

	if(vpx_codec_encode(&codec, frame_avail? &raw : NULL, frame_cnt, 1, flags, VPX_DL_REALTIME))
		return  env->NewStringUTF("Failed to encode frame\n");

	env->ReleaseByteArrayElements(input, bufferPtr, 0);

	int kf=false;
	while( (pkt = vpx_codec_get_cx_data(&codec, &iter)) ) {

		switch(pkt->kind) {
		case VPX_CODEC_CX_FRAME_PKT:
			//write_ivf_frame_header(outfile, pkt);
			//(void) fwrite(pkt->data.frame.buf, 1, pkt->data.frame.sz, outfile);
			kf = (pkt->data.frame.flags & VPX_FRAME_IS_KEY)? true:false;
			kr_mkv_add_video_tc (kr_stream, video_track, (unsigned char *)pkt->data.frame.buf, pkt->data.frame.sz, kf, tc);
			break;
		default:
			break;
		}
		sprintf(r, pkt->kind == VPX_CODEC_CX_FRAME_PKT && (pkt->data.frame.flags & VPX_FRAME_IS_KEY)? "K":".");
	}
	frame_cnt++;
	return env->NewStringUTF(r);
}

JNIEXPORT void JNICALL Java_ws_websca_krcam_MainActivity_vpxClose( JNIEnv* env, jobject thiz)
{
	if(kr_stream!=NULL)
		kr_mkv_destroy (&kr_stream);
	kr_stream=NULL;
}

static void deinterleave(const uint8_t *srcAB, uint8_t *dstA, uint8_t *dstB, size_t srcABLength)
{
	if (android_getCpuFeatures() & ANDROID_CPU_ARM_FEATURE_NEON)
		deinterleave_nv21_to_i420_neon(srcAB, dstA, dstB, srcABLength);
	else
		deinterleave_nv21_to_i420(srcAB, dstA, dstB, srcABLength);
	return;
}
}
