package ws.websca.krcam;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

public class MainActivity extends Activity {

	private static final int DEFAULTVIDEOBITRATE = 1000;
	protected static final int DEFAULTAUDIOQUALITY = 3;

	private Camera camera;
	private TextView textView;
	private int videoWidth = Integer.MAX_VALUE;
	private int videoHeight = Integer.MAX_VALUE;
	private int videoBitrate=DEFAULTVIDEOBITRATE;
	private int cameraNumber;
	private int audioSamplerate=48000;
	private int audioQuality;
	private UpdateUiReceiver receiver;
	private Button showVideoButton;
	private Button startStreamingButton;
	private Button stopStreamingButton;
	private Spinner micSpinner;
	private Spinner micSamplerateSpinner;
	private Spinner cameraSpinner;
	private Spinner cameraResSpinner;
	private SpinnerData[] samplerateArray;
	private Parameters cameraParams=null;
	private EditText videoBitrateEditText;
	private EditText audioQualityEditText;
	private RadioButton streamRadio;
	private RadioButton localRadio;
	private RadioButton bothRadio;
	private boolean streamFile;
	private boolean localFile;
	protected int micSource;

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

		unregisterReceiver(MainActivity.this.receiver);

	}

	protected void onPause() {
		super.onPause();
	}

	protected void onResume() {
		super.onResume();
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);	
		textView = (TextView)findViewById(R.id.textView1);
		showVideoButton = (Button)findViewById(R.id.showVideo);
		startStreamingButton = (Button)findViewById(R.id.startStreaming);
		stopStreamingButton = (Button)findViewById(R.id.stopStreaming);
		micSpinner = (Spinner)findViewById(R.id.micSpinner);
		micSamplerateSpinner = (Spinner)findViewById(R.id.micSamplerateSpinner);
		cameraSpinner = (Spinner)findViewById(R.id.cameraSpinner);
		cameraResSpinner = (Spinner)findViewById(R.id.cameraResSpinner);
		videoBitrateEditText = (EditText)findViewById(R.id.videoBitrateEditText);
		audioQualityEditText = (EditText)findViewById(R.id.audioQualityEditText);
		streamRadio = (RadioButton)findViewById(R.id.streamRadio);
		localRadio = (RadioButton)findViewById(R.id.localRadio);
		bothRadio = (RadioButton)findViewById(R.id.bothRadio);

		populateSpinners();

		IntentFilter filter;
		filter = new IntentFilter(KrCamService.UPDATEUI);
		receiver = new UpdateUiReceiver();
		registerReceiver(receiver, filter);

		startStreamingButton.setOnClickListener(new OnClickListener() {

			public void onClick(View arg0) {
				setUiStateRecording(true);
				cameraNumber=cameraSpinner.getSelectedItemPosition()-1;
				if(micSpinner.getSelectedItemPosition()==0)
					micSource=-1;
				else
					micSource=MediaRecorder.AudioSource.CAMCORDER;
				videoWidth=cameraParams.getSupportedPreviewSizes().get(cameraResSpinner.getSelectedItemPosition()).width;
				videoHeight=cameraParams.getSupportedPreviewSizes().get(cameraResSpinner.getSelectedItemPosition()).height;
				audioSamplerate = ((SpinnerData)micSamplerateSpinner.getSelectedItem()).getValue();
				try{
					videoBitrate = Integer.parseInt(videoBitrateEditText.getText().toString());
					if(videoBitrate<10 || videoBitrate > 1000000) {
						videoBitrate=DEFAULTVIDEOBITRATE;
					}
				}
				catch(java.lang.NumberFormatException e) {
					videoBitrate=DEFAULTVIDEOBITRATE;
				}
				videoBitrateEditText.setText(""+videoBitrate);

				try{
					audioQuality = Integer.parseInt(audioQualityEditText.getText().toString());
					if(audioQuality<0 || audioQuality > 10) {
						audioQuality=DEFAULTAUDIOQUALITY;
					}
				}
				catch(java.lang.NumberFormatException e) {
					audioQuality=DEFAULTAUDIOQUALITY;
				}
				audioQualityEditText.setText(""+audioQuality);

				localFile = localRadio.isChecked();
				streamFile = streamRadio.isChecked();
				if(bothRadio.isChecked()) {
					localFile=true;
					streamFile=true;
				}

				startVideo();
			}
		});
		stopStreamingButton.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				Intent i = new Intent(MainActivity.this, KrCamService.class);
				stopService(i);
				setUiStateRecording(false);
				textView.setText("");
			}
		});
		showVideoButton.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				Intent intent = new Intent(MainActivity.this, ShowVideoActivity.class);
				startActivity(intent);
			}
		});

	}

	protected void onStart() {
		super.onStart();
	}

	private void startVideo() {

		Intent i = new Intent(this, KrCamService.class);
		i.putExtra("cameraNumber", cameraNumber);
		i.putExtra("micSource", micSource);
		i.putExtra("videoWidth", videoWidth);
		i.putExtra("videoHeight", videoHeight);
		i.putExtra("videoBitrate", videoBitrate);
		i.putExtra("audioSampleRate", audioSamplerate);
		i.putExtra("audioQuality", audioQuality);
		i.putExtra("stream", streamFile);
		i.putExtra("local", localFile);
		startService(i);
	}

	private void checkSamplerates() {
		//TODO: check samplerates
		samplerateArray = new SpinnerData[3];
		samplerateArray[0] = new SpinnerData("48000hz", 48000);
		samplerateArray[1] = new SpinnerData("44100hz", 44100);
		samplerateArray[2] = new SpinnerData("11025hz", 11025);
	}

	private void populateSpinners() {
		List<String> list = new ArrayList<String>();
		list.add("None");
		list.add("Default Mic");
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		micSpinner.setAdapter(dataAdapter);

		checkSamplerates();
		ArrayAdapter<SpinnerData> spinnerDataAdapter = new ArrayAdapter<SpinnerData>(this, android.R.layout.simple_spinner_item, samplerateArray);
		spinnerDataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		micSamplerateSpinner.setAdapter(spinnerDataAdapter);

		list = new ArrayList<String>();
		list.add("None");
		CameraInfo i = new CameraInfo();
		for(int x=0; x<Camera.getNumberOfCameras(); x++) {
			Camera.getCameraInfo(x, i);
			if(i.facing==CameraInfo.CAMERA_FACING_BACK)
				list.add("Back Camera");
			else if(i.facing==CameraInfo.CAMERA_FACING_FRONT)
				list.add("Front Camera");
		}

		dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		cameraSpinner.setAdapter(dataAdapter);

		cameraSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> arg0, View arg1,	int arg2, long arg3) {
				cameraNumber = cameraSpinner.getSelectedItemPosition()-1;
				cameraParams = null;
				MainActivity.this.populateCameraRes();
			}
			public void onNothingSelected(AdapterView<?> arg0) {}
		});

		micSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> arg0, View arg1,	int arg2, long arg3) {
				if(micSpinner.getSelectedItemPosition()==0) {
					micSamplerateSpinner.setEnabled(false);
					audioQualityEditText.setEnabled(false);
				}
				else {
					micSamplerateSpinner.setEnabled(true);
					audioQualityEditText.setEnabled(true);
				}
			}
			public void onNothingSelected(AdapterView<?> arg0) {}
		});
	}
	public void populateCameraRes() {
		List<String> list = new ArrayList<String>();
		if(cameraNumber==-1) {
			cameraResSpinner.setEnabled(false);
			videoBitrateEditText.setEnabled(false);
			//TODO: Make KrCamSerive work with no video
			startStreamingButton.setEnabled(false);
		}
		else {
			cameraResSpinner.setEnabled(true);
			videoBitrateEditText.setEnabled(true);
			startStreamingButton.setEnabled(true);
			if(cameraParams==null) {
				//TODO: Camera.open is slow. Do something smart if it fails.
				camera = Camera.open(cameraNumber);
				cameraParams = camera.getParameters();
				camera.release();
			}
			list = new ArrayList<String>();
			for(int x=0; x<cameraParams.getSupportedPreviewSizes().size(); x++) {
				list.add(new String(""+cameraParams.getSupportedPreviewSizes().get(x).width+"x"+cameraParams.getSupportedPreviewSizes().get(x).height));
				cameraResSpinner.setSelection(0);
			}
			ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
			dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			cameraResSpinner.setAdapter(dataAdapter);
		}
	}

	public void setUiStateRecording(boolean rec) {
		startStreamingButton.setEnabled(!rec);
		stopStreamingButton.setEnabled(rec);
		showVideoButton.setEnabled(rec);
		cameraSpinner.setEnabled(!rec);
		cameraResSpinner.setEnabled(!rec);
		micSpinner.setEnabled(!rec);
		micSamplerateSpinner.setEnabled(!rec);
		videoBitrateEditText.setEnabled(!rec);
		audioQualityEditText.setEnabled(!rec);
		localRadio.setEnabled(!rec);
		streamRadio.setEnabled(!rec);
		bothRadio.setEnabled(!rec);
	}

	public class UpdateUiReceiver extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent) {
			final String uiString = intent.getStringExtra("uistring");
			MainActivity.this.runOnUiThread(new Runnable() {
				public void run() {
					textView.setText(uiString);
					setUiStateRecording(true);
				}
			});
		}
	}

	class SpinnerData {
		public SpinnerData( String spinnerText, int value ) {
			this.spinnerText = spinnerText;
			this.value = value;
		}

		public String getSpinnerText() {
			return spinnerText;
		}

		public int getValue() {
			return value;
		}

		public String toString() {
			return spinnerText;
		}

		String spinnerText;
		int value;
	}
}
