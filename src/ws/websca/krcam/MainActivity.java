package ws.websca.krcam;

import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity implements Callback, PreviewCallback, Runnable {

	//private static final int SAMPLERATE = 11025;
	private static final int SAMPLERATE = 44100;
	//private static final int SAMPLERATE = 48000;
	public native long krStreamCreate(String path, int w, int h, int videoBitrate, int audioSampleRate, boolean networkStream);
	public native String krAddVideo(long cam, byte input[], int tc);
	public native boolean krAudioCallback(long cam, byte buffer[], int size);
	public native boolean krStreamDestroy(long cam);
	private Long cam=null;
	private AudioRecord ar;
	private SurfaceView surfaceView;
	private Camera camera;
	private long FPSstartMs = -1;
	private long startMs = -1;
	private int frame;
	private TextView textView;
	private byte[] previewBuffer;
	private byte[] previewBuffer2;
	private byte[] previewBuffer3;	
	private byte[] previewBuffer4;
	private byte[] previewBuffer5;	
	private String formatString;
	private int videoWidth = Integer.MAX_VALUE;
	private int videoHeight = Integer.MAX_VALUE;
	private int videoBitrate=1000;
	private EditText bitrateEditText;

	static {
		System.loadLibrary("krcam");
	}

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	protected void onStop() {
		super.onStop();
		stop();
	}

	protected void onPause() {
		super.onPause();
	}

	protected void onResume() {
		super.onResume();
		start();
	}

	protected void onStart() {
		super.onStart();
	}

	private void stop() {
		surfaceView.getHolder().removeCallback(this);
		camera.release();
		if(cam!=null)
			krStreamDestroy(cam);
		cam=null;
		if(ar!=null)
			ar.release();
		ar=null;
		this.setNotifaction(false);
	}

	private void startVideo() {
		Camera.Parameters parameters = camera.getParameters(); 

		parameters.setPreviewSize(videoWidth, videoHeight);
		camera.setParameters(parameters);
		int size = videoHeight*videoWidth+((videoHeight*videoWidth)/2);
		previewBuffer = new byte[size];
		previewBuffer2 = new byte[size];
		previewBuffer3 = new byte[size];
		previewBuffer4 = new byte[size];
		previewBuffer5 = new byte[size];
		camera.addCallbackBuffer(previewBuffer);
		camera.addCallbackBuffer(previewBuffer2);
		camera.addCallbackBuffer(previewBuffer3);
		camera.addCallbackBuffer(previewBuffer4);
		camera.addCallbackBuffer(previewBuffer5);

		cam = krStreamCreate(Environment.getExternalStorageDirectory()+"/", videoWidth, videoHeight, videoBitrate, SAMPLERATE, false);

		formatString = ""+parameters.getPreviewSize().width+"x"+parameters.getPreviewSize().height+" NV21 ";
		try {
			camera.setPreviewDisplay(surfaceView.getHolder());
		} catch (IOException e) {
			e.printStackTrace();
		}
		camera.setPreviewCallbackWithBuffer(MainActivity.this);
		camera.startPreview();
		int min = AudioRecord.getMinBufferSize(SAMPLERATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
		ar = new AudioRecord(MediaRecorder.AudioSource.CAMCORDER, SAMPLERATE, AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT, min);
		ar.startRecording();
		Thread t = new Thread(MainActivity.this);
		t.start();
		this.setNotifaction(true);
	}
	private final class bitrateSelectedOnClickListener implements DialogInterface.OnClickListener {

		public void onClick(DialogInterface dialog,	int which) {
			
			String value = bitrateEditText.getText().toString();
			try{
				videoBitrate = Integer.parseInt(value);
				if(videoBitrate>=40 && videoBitrate <= 1000000) {
					dialog.dismiss();
					startVideo();
				}
				else
					showBitrateDialog();
			}
			catch(java.lang.NumberFormatException e) {
				showBitrateDialog();
			}
		}
	}
	private void showBitrateDialog() {

		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setTitle("Video Bitrate");
		bitrateEditText = new EditText(MainActivity.this);
		bitrateEditText.setText(""+videoBitrate);
		bitrateEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
		bitrateEditText.setSingleLine();
		builder.setView(bitrateEditText);
		builder.setPositiveButton("Ok", new bitrateSelectedOnClickListener());
		builder.setCancelable(false);
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	private final class resSelectedOnClickListener implements DialogInterface.OnClickListener {
		public void onClick(DialogInterface dialog,	int which)
		{
			Camera.Parameters parameters = camera.getParameters(); 
			videoWidth=parameters.getSupportedPreviewSizes().get(which).width;
			videoHeight=parameters.getSupportedPreviewSizes().get(which).height;
			dialog.dismiss();
			showBitrateDialog();
		}
	}

	private void start() {
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);	
		textView = (TextView)findViewById(R.id.textView1);
		surfaceView = (SurfaceView)findViewById(R.id.surfaceview);
		camera = Camera.open();
		Parameters p = camera.getParameters();

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Video Resolution");

		String choiceList[] = new String[p.getSupportedPreviewSizes().size()];
		for(int x=0; x<p.getSupportedPreviewSizes().size(); x++) {
			choiceList[x]=new String(""+p.getSupportedPreviewSizes().get(x).width+"x"+p.getSupportedPreviewSizes().get(x).height);
		}
		int selected = 0;
		builder.setSingleChoiceItems(choiceList, selected, new resSelectedOnClickListener());
		builder.setCancelable(false);
		AlertDialog alert = builder.create();
		alert.show();
		surfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		surfaceView.getHolder().addCallback(this);
	}

	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {}
	public void surfaceCreated(SurfaceHolder holder) {}
	public void surfaceDestroyed(SurfaceHolder arg0) {}

	public void onPreviewFrame(byte[] buffer, Camera arg1) {
		if(startMs<=0)
			startMs=SystemClock.elapsedRealtime();
		if(FPSstartMs<0) {
			FPSstartMs  = SystemClock.elapsedRealtime();
			frame=0;
		}
		frame++;
		if(cam!=null)
			Log.e("vpx", krAddVideo(cam, buffer, (int) (SystemClock.elapsedRealtime()-startMs)));
		camera.addCallbackBuffer(buffer);
		if(SystemClock.elapsedRealtime()>=FPSstartMs+1000) {
			FPSstartMs=SystemClock.elapsedRealtime();

			this.runOnUiThread(new Runnable() {
				public void run() {
					textView.setText(formatString+" @"+frame+"FPS"+" "+videoBitrate+"kb/s");
				}
			});
			frame=0;
		}
	}
	@Override
	public void run() {
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
		int min = AudioRecord.getMinBufferSize(SAMPLERATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
		byte audioBuffer[] = new byte[min/2];
		while(cam!=null && ar!=null) {
			if(ar!=null)
				ar.read(audioBuffer, 0, min/2);
			if(cam!=null)
				krAudioCallback(cam, audioBuffer, audioBuffer.length);
		}
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
}
