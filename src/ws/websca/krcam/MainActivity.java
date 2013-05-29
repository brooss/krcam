package ws.websca.krcam;

import java.util.ArrayList;
import java.util.List;

import ws.websca.krcam.MainActivity.SpinnerData;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class MainActivity extends Activity {

	private static final int DEFAULTVIDEOBITRATE = 1000;

	private Camera camera;

	private TextView textView;

	private int videoWidth = Integer.MAX_VALUE;
	private int videoHeight = Integer.MAX_VALUE;
	private int videoBitrate=DEFAULTVIDEOBITRATE;
	private int audioSamplerate=48000;
	private EditText bitrateEditText;
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
				
		populateSpinners();
		
		IntentFilter filter;
		filter = new IntentFilter(KrCamService.UPDATEUI);
		receiver = new UpdateUiReceiver();
		registerReceiver(receiver, filter);
		
		startStreamingButton.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				setUiStateRecording(true);
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
		i.putExtra("videoWidth", videoWidth);
		i.putExtra("videoHeight", videoHeight);
		i.putExtra("videoBitrate", videoBitrate);
		i.putExtra("audioSampleRate", audioSamplerate);
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
		list.add("Default Mic");
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		micSpinner.setAdapter(dataAdapter);

		checkSamplerates();
		ArrayAdapter<SpinnerData> spinnerDataAdapter = new ArrayAdapter<SpinnerData>(this, android.R.layout.simple_spinner_item, samplerateArray);
		spinnerDataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		micSamplerateSpinner.setAdapter(spinnerDataAdapter);
		
		list = new ArrayList<String>();
		list.add("Default Camera");
		dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		cameraSpinner.setAdapter(dataAdapter);
		
		if(cameraParams==null) {
			camera = Camera.open();
			cameraParams = camera.getParameters();
			camera.release();
		}
		list = new ArrayList<String>();
		for(int x=0; x<cameraParams.getSupportedPreviewSizes().size(); x++) {
			list.add(new String(""+cameraParams.getSupportedPreviewSizes().get(x).width+"x"+cameraParams.getSupportedPreviewSizes().get(x).height));
		}
		dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		cameraResSpinner.setAdapter(dataAdapter);
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
