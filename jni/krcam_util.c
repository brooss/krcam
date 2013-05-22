#include "krcam_util.h"
void write_ivf_file_header(FILE *outfile, const vpx_codec_enc_cfg_t *cfg, int frame_cnt) {
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

void write_ivf_frame_header(FILE *outfile, const vpx_codec_cx_pkt_t *pkt)
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

//hacked from gstreamer (lgpl)
void fill_frame(int frame_number, int width, int height, vpx_image_t *img) {
	size_t to_read;

	int t = frame_number;
	int w = width, h = height;
	int i;
	int j;
	int xreset = -(w / 2);
	int yreset = -(h / 2);
	int x, y;
	int accum_kx;
	int accum_kxt;
	int accum_ky;
	int accum_kyt;
	int accum_kxy;
	int kt;
	int kt2;
	int ky2;
	int delta_kxt = 0 * t;
	int delta_kxy;
	int scale_kxy = 0xffff / (w / 2);
	int scale_kx2 = 0xffff / w;
	int vky=0;
	int vkt=10;
	int vkxy=0;
	int vkyt=0;
	int vky2=10;
	int vk0=0;
	int vkx=0;
	int vkx2=10;
	int vkt2=0;
	  /* optimised version, with original code shown in comments */
	  accum_ky = 0;
	  accum_kyt = 0;
	  kt = vkt * t;
	  kt2 = vkt2 * t * t;
	  for (j = 0, y = yreset; j < h; j++, y++) {
	    accum_kx = 0;
	    accum_kxt = 0;
	    accum_ky += vky;
	    accum_kyt += vkyt * t;
	    delta_kxy = vkxy * y * scale_kxy;
	    accum_kxy = delta_kxy * xreset;
	    ky2 = (vky2 * y * y) / h;
	    for (i = 0, x = xreset; i < w; i++, x++) {

	      /* zero order */
	      int phase = vk0;

	      /* first order */
	      accum_kx += vkx;
	      /* phase = phase + (v->kx * i) + (v->ky * j) + (v->kt * t); */
	      phase = phase + accum_kx + accum_ky + kt;

	      /* cross term */
	      accum_kxt += delta_kxt;
	      accum_kxy += delta_kxy;
	      /* phase = phase + (v->kxt * i * t) + (v->kyt * j * t); */
	      phase = phase + accum_kxt + accum_kyt;

	      /* phase = phase + (v->kxy * x * y) / (w/2); */
	      /* phase = phase + accum_kxy / (w/2); */
	      phase = phase + (accum_kxy >> 16);

	      /*second order */
	      /*normalise x/y terms to rate of change of phase at the picture edge */
	      /*phase = phase + ((v->kx2 * x * x)/w) + ((v->ky2 * y * y)/h) + ((v->kt2 * t * t)>>1); */
	      phase = phase + ((vkx2 * x * x * scale_kx2) >> 16) + ky2 + (kt2 >> 1);
			img->planes[0][(j*w)+i] = sine_table[phase & 0xff];
			img->planes[1][((j*w)+i)/2] = sine_table[phase & 0xff];
		}
	}
}

void int16_to_float (float *dst, char *src, uint32_t nsamples, uint32_t src_skip)
{
	const float scaling = 1.0/32767.0f;
	while (nsamples--) {
		*dst = (*((short *) src)) * scaling;
		dst++;
		src += src_skip;
	}
}

