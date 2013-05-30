package ws.websca.krcam;

import java.io.IOException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

public class KrCamService  extends Service implements PreviewCallback, Callback, Runnable {

	public static final String SHOWPREVIEW = "ws.websca.krcam.show_preview";
	public static final String UPDATEUI = "ws.websca.krcam.update_ui";
	private Camera camera=null;
	private byte[] previewBuffer;
	private SurfaceView surfaceView;
	private int videoWidth;
	private int videoHeight;
	private int videoBitrate;
	private int audioSampleRate;
	private byte[] previewBuffer2;
	private byte[] previewBuffer3;
	private byte[] previewBuffer4;
	private byte[] previewBuffer5;
	private long startMs=Long.MIN_VALUE;
	private long FPSstartMs=Long.MIN_VALUE;
	private int frame;
	private Long krCamStream=null;
	private AudioRecord ar=null;
	private KrCamServiceReceiver receiver;
	private String formatString;
	private int audioQuality;
	private boolean streamFile;
	private boolean localFile;
	private int cameraNumber=0;
	private int micSource;
	
	
	static {
		System.loadLibrary("krcam");
	}
	
	public native long krStreamCreate(String path, int w, int h, int videoBitrate, boolean useAudio, int audioSampleRate, int audioQuality, boolean networkStream, boolean saveLocal);
	public native String krAddVideo(long cam, byte input[], int tc);
	public native boolean krAudioCallback(long cam, byte buffer[], int size);
	public native boolean krStreamDestroy(long cam);

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
    public void onDestroy(){
    	super.onDestroy();
    	camera.stopPreview();
    	camera.release();
		WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
		wm.removeView(surfaceView);
		if(ar!=null) 
			ar.release();
		ar=null;
		krStreamDestroy(krCamStream);
		krCamStream=null;
		this.setNotifaction(false);
		unregisterReceiver(receiver);
    }

	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, startId, startId);
		this.setNotifaction(true);
		Log.e("KrCamService", "onStartCommand");
		
		IntentFilter filter;
		filter = new IntentFilter(KrCamService.SHOWPREVIEW);
		receiver = new KrCamServiceReceiver();
		registerReceiver(receiver, filter);
		
		cameraNumber = intent.getIntExtra("cameraNumber", 0);
		micSource = intent.getIntExtra("micSource", MediaRecorder.AudioSource.CAMCORDER);
		videoWidth = intent.getIntExtra("videoWidth", -1);
		videoHeight = intent.getIntExtra("videoHeight", -1);
		videoBitrate = intent.getIntExtra("videoBitrate", -1);
		audioSampleRate = intent.getIntExtra("audioSampleRate", 44100);
		audioQuality = intent.getIntExtra("audioQuality", 3);
		streamFile = intent.getBooleanExtra("stream", false);
		localFile = intent.getBooleanExtra("local", true);
		
		createSurface(1,1);

		return START_STICKY;
	}

	@Override
	public void onPreviewFrame(byte[] buffer, Camera arg1) {
		if(startMs<=0)
			startMs=SystemClock.elapsedRealtime();
		if(FPSstartMs<0) {
			FPSstartMs  = SystemClock.elapsedRealtime();
			frame=0;
		}
		frame++;
		if(krCamStream!=null)
			krAddVideo(krCamStream, buffer, (int) (SystemClock.elapsedRealtime()-startMs));
		camera.addCallbackBuffer(buffer);
		if(SystemClock.elapsedRealtime()>=FPSstartMs+1000) {
			FPSstartMs=SystemClock.elapsedRealtime();
			Intent intent = new Intent(KrCamService.UPDATEUI);
			intent.putExtra("uistring", formatString+" @"+frame+"FPS"+" "+videoBitrate+"kb/s");
			sendBroadcast(intent);
			frame=0;
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int arg1, int arg2, int arg3) {
		if(camera != null)
			camera.release();
		camera = Camera.open(cameraNumber);
		Parameters p = camera.getParameters();
		p.setPreviewSize(videoWidth, videoHeight);
		camera.setParameters(p);
		camera.setPreviewCallbackWithBuffer(this);
		int bufferSize = videoHeight*videoWidth+((videoHeight*videoWidth)/2);
		previewBuffer = new byte[bufferSize];
		previewBuffer2 = new byte[bufferSize];
		previewBuffer3 = new byte[bufferSize];
		previewBuffer4 = new byte[bufferSize];
		previewBuffer5 = new byte[bufferSize];
		camera.addCallbackBuffer(previewBuffer);
		camera.addCallbackBuffer(previewBuffer2);
		camera.addCallbackBuffer(previewBuffer3);
		camera.addCallbackBuffer(previewBuffer4);
		camera.addCallbackBuffer(previewBuffer5);
		
		try {
			camera.setPreviewDisplay(holder);
		} catch (IOException e) {
			
		}
		camera.startPreview();
		if(krCamStream==null) {
			krCamStream = krStreamCreate(Environment.getExternalStorageDirectory()+"/", videoWidth, videoHeight, videoBitrate, micSource >=0 ? true : false, audioSampleRate, audioQuality, streamFile, localFile);
			formatString = ""+videoWidth+"x"+videoHeight+" NV21 ";

			if(micSource>=0) {
				int min = AudioRecord.getMinBufferSize(audioSampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
				try{
					ar = new AudioRecord(micSource, audioSampleRate, AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT, min);
					ar.startRecording();
					Thread t = new Thread(this);
					t.start();
				} catch(Exception e) {
					
				}
			}
		}
	}
	public void surfaceCreated(SurfaceHolder arg0) {}
	public void surfaceDestroyed(SurfaceHolder arg0) {}
	
	public void run() {
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
		int min = AudioRecord.getMinBufferSize(audioSampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
		byte audioBuffer[] = new byte[min/2];
		while(krCamStream!=null && ar!=null) {
			if(ar!=null)
				ar.read(audioBuffer, 0, min/2);
			if(krCamStream!=null)
				krAudioCallback(krCamStream, audioBuffer, audioBuffer.length);
		}
	}
	
	public void createSurface(int w, int h) {
		WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
		LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
		            WindowManager.LayoutParams.WRAP_CONTENT,
		            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
		            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
		            PixelFormat.OPAQUE); 
		if(w!=-1 && h!=-1) {
			params.width=w;
			params.height=h;
		}
		surfaceView = new SurfaceView(this);
		surfaceView.getHolder().addCallback(this);
		surfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		surfaceView.setZOrderOnTop(true);
		DisplayMetrics dm = new DisplayMetrics();
		wm.getDefaultDisplay().getMetrics(dm);
		wm.addView(surfaceView, params);
	}
	
	public void setNotifaction(boolean show) {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		if(show) {
			NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
			        .setSmallIcon(android.R.drawable.ic_media_play)
			        .setContentTitle("krcam")
			        .setContentText("Streaming!");
			mBuilder.setProgress(0, 0, true);
			Intent resultIntent = new Intent(this, MainActivity.class);
			PendingIntent resultPendingIntent;
			resultPendingIntent =  PendingIntent.getActivity(this, 0, resultIntent, 0);
			mBuilder.setContentIntent(resultPendingIntent);
			
			Notification n = mBuilder.build();
			n.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
			notificationManager.notify(0, n);
		}
		else {
			notificationManager.cancel(0);
		}
	}
	
	public class KrCamServiceReceiver extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent) {
			WindowManager wm = (WindowManager) KrCamService.this.getSystemService(Context.WINDOW_SERVICE);
			wm.removeView(surfaceView);
			surfaceView.removeCallbacks(KrCamService.this);
			
			boolean show = intent.getBooleanExtra("show", false);
			if(show)
				createSurface(-1, -1);
			else
				createSurface(1, 1);
		}
	}
}
