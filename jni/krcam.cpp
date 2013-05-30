#include "krcam.h"
extern "C" {

JNIEXPORT jlong JNICALL Java_ws_websca_krcam_KrCamService_krStreamCreate( JNIEnv* env, jobject thiz, jstring path, jint w, jint h, jint videoBitrate, jboolean useAudio, jint audioSampleRate, jint audioQuality, jboolean networkStream, jboolean saveLocally )
{
	kr_cam_t *cam = (kr_cam_t*)calloc (1, sizeof(kr_cam_t));
	char *nativeString = (char*)env->GetStringUTFChars(path, 0);
	cam->params = init_params(nativeString, w, h, useAudio, audioSampleRate, audioQuality);
	env->ReleaseStringUTFChars(path, nativeString);

	if(saveLocally)
		cam->stream = kr_mkv_create_file (cam->params->filename);
	if(networkStream)
		cam->stream = kr_mkv_create_stream (cam->params->host, cam->params->port, cam->params->mount, cam->params->password);

	if(!vpx_img_alloc(&cam->raw, VPX_IMG_FMT_I420, cam->params->width, cam->params->height, 1))
		return (long)NULL;

	vpx_codec_err_t res;
	vpx_codec_enc_cfg_t cfg;
	res = vpx_codec_enc_config_default(interface, &cfg, 0);
	if(res) {
		NULL;
	}

	int threads = 1;
	cfg.rc_target_bitrate = videoBitrate;
	cfg.g_w = cam->params->width;
	cfg.g_h = cam->params->height;
	cfg.g_threads = 1;

	if(vpx_codec_enc_init(&cam->codec, interface, &cfg, 0))
		return (long)NULL;

	if(threads==2)
		vpx_codec_control(&cam->codec, VP8E_SET_TOKEN_PARTITIONS, VP8_ONE_TOKENPARTITION);
	else if(threads>2)
		vpx_codec_control(&cam->codec, VP8E_SET_TOKEN_PARTITIONS, VP8_TWO_TOKENPARTITION);

	cam->video_track_id = kr_mkv_add_video_track (cam->stream, VP8, 1000,	1, cam->params->width, cam->params->height);

	if(cam->params->use_audio) {
		cam->vorbis = krad_vorbis_encoder_create (cam->params->channels, cam->params->sample_rate, cam->params->audio_quality);
		cam->audio_track_id = kr_mkv_add_audio_track(cam->stream, VORBIS, cam->params->sample_rate, cam->params->channels, cam->vorbis->hdrdata, 3 + cam->vorbis->header.sz[0] + cam->vorbis->header.sz[1] + cam->vorbis->header.sz[2]);
		cam->audio_ring = krad_ringbuffer_create (500000);
	}

	return (long)cam;
}

JNIEXPORT jboolean JNICALL Java_ws_websca_krcam_KrCamService_krAudioCallback( JNIEnv* env, jobject thiz, jlong p, jbyteArray buffer, jint size )
{
	kr_cam_t *cam=(kr_cam_t*)p;
	if(!cam->params->use_audio || cam==NULL || cam->audio_ring==NULL)
		return false;
	float samples[8192];
	jbyte* bufferPtr = env->GetByteArrayElements(buffer, 0);
	int16_to_float (samples, (char *)bufferPtr + (0 * 2),	size/2, 2);
	env->ReleaseByteArrayElements(buffer, bufferPtr, 0);
	krad_ringbuffer_write (cam->audio_ring,	(char *)samples, (size/2) * 4);
	return true;
}

JNIEXPORT jboolean JNICALL Java_ws_websca_krcam_KrCamService_krStreamDestroy( JNIEnv* env, jobject thiz, jlong p )
{
	kr_cam_t *cam=(kr_cam_t*)p;
	if(cam->params->use_audio) {
		krad_ringbuffer_free (cam->audio_ring);
		cam->audio_ring=NULL;
		krad_vorbis_encoder_destroy (&cam->vorbis);
	}
	if(cam->stream!=NULL)
		kr_mkv_destroy (&cam->stream);
	cam->stream=NULL;
	free_params(cam->params);
	free(cam);
	return true;
}

JNIEXPORT jstring JNICALL Java_ws_websca_krcam_KrCamService_krAddVideo( JNIEnv* env, jobject thiz, jlong p, jbyteArray input, jint tc )
{
	kr_cam_t *cam = (kr_cam_t*)p;
	vpx_codec_iter_t iter = NULL;
	const vpx_codec_cx_pkt_t *pkt;
	int                  flags = 0;
	char r[3];

	int frame_avail = 1;

	jbyte* bufferPtr = env->GetByteArrayElements(input, 0);
	jsize lengthOfArray = env->GetArrayLength(input);

	cam->raw.planes[0] = (uint8_t *)bufferPtr;
	deinterleave ((uint8_t *)bufferPtr + (cam->params->width*cam->params->height), cam->raw.planes[2], cam->raw.planes[1], (cam->params->width*cam->params->height) / 2);

	if(vpx_codec_encode(&cam->codec, frame_avail? &cam->raw : NULL, cam->frame_count, 1, flags, VPX_DL_REALTIME))
		return  env->NewStringUTF("Failed to encode frame\n");

	env->ReleaseByteArrayElements(input, bufferPtr, 0);

	int kf=false;
	while( (pkt = vpx_codec_get_cx_data(&cam->codec, &iter)) ) {

		switch(pkt->kind) {
		case VPX_CODEC_CX_FRAME_PKT:
			kf = (pkt->data.frame.flags & VPX_FRAME_IS_KEY)? true:false;
			kr_mkv_add_video_tc (cam->stream, cam->video_track_id, (unsigned char *)pkt->data.frame.buf, pkt->data.frame.sz, kf, tc);
			break;
		default:
			break;
		}
		sprintf(r, pkt->kind == VPX_CODEC_CX_FRAME_PKT && (pkt->data.frame.flags & VPX_FRAME_IS_KEY)? "K":".");
	}
	cam->frame_count++;
	if(cam->params->use_audio)
		kr_cam_run_audio(cam);

	return env->NewStringUTF(r);
}

static void deinterleave(const uint8_t *srcAB, uint8_t *dstA, uint8_t *dstB, size_t srcABLength)
{
	if (android_getCpuFeatures() & ANDROID_CPU_ARM_FEATURE_NEON)
		deinterleave_nv21_to_i420_neon(srcAB, dstA, dstB, srcABLength);
	else
		deinterleave_nv21_to_i420(srcAB, dstA, dstB, srcABLength);
	return;
}

static void free_params(kr_cam_params_t* params)
{
	free(params->host);
	free(params->mount);
	free(params->password);
	free(params->filename);
	free(params);
}

static kr_cam_params_t* init_params(char *path, int w, int h, bool use_audio, int audioSampleRate, int audioQuality)
{
	kr_cam_params_t *params;
	params = (kr_cam_params_t*)calloc (1, sizeof(kr_cam_params_t));

	params->host = (char*)malloc(256);
	params->mount = (char*)malloc(256);
	params->password = (char*)malloc(256);
	params->filename = (char*)malloc(512);
	int32_t port;

	snprintf (params->host, 256, "%s", "europa.kradradio.com");
	snprintf (params->mount, 256, "/krcam_%"PRIu64".webm", krad_unixtime());
	snprintf (params->password, 256, "%s", "firefox");

	snprintf (params->filename, 512,"%s/%s_%"PRIu64".webm", path, "cam", krad_unixtime ());

	params->port=8008;
	params->width=w;
	params->height=h;

	//audio
	params->use_audio=use_audio;
	params->channels=1;
	params->sample_rate=audioSampleRate;
	params->audio_quality=((float)audioQuality)/10.0;

	return params;
}

void kr_cam_run_audio (kr_cam_t *cam) {

	kr_medium_t *amedium;
	kr_codeme_t *acodeme;
	uint32_t c;
	int32_t ret;

	ret = 0;

	int frames = 0;

	amedium = kr_medium_kludge_create ();
	acodeme = kr_codeme_kludge_create ();

	while (krad_ringbuffer_read_space (cam->audio_ring) >= 1024 * 4) {

		krad_ringbuffer_read (cam->audio_ring, (char *)amedium->a.samples[0], 1024 * 4);

		amedium->a.count = 1024;
		amedium->a.channels = 1;
		ret = kr_vorbis_encode (cam->vorbis, acodeme, amedium);
		if (ret == 1) {
			kr_mkv_add_audio (cam->stream, cam->audio_track_id, acodeme->data, acodeme->sz, acodeme->count);
			int muxdelay = 0;
			while (1) {
				ret = kr_vorbis_encode (cam->vorbis, acodeme, NULL);
				if (ret == 1) {
					kr_mkv_add_audio (cam->stream, cam->audio_track_id, acodeme->data, acodeme->sz, acodeme->count);
				} else {
					break;
				}
			}
		}
	}
	kr_medium_kludge_destroy (&amedium);
	kr_codeme_kludge_destroy (&acodeme);
}
}
