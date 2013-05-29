package ws.websca.krcam;

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
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {

	private static final int SAMPLERATE = 11025;
	//private static final int SAMPLERATE = 44100;
	//private static final int SAMPLERATE = 48000;
	
	private Camera camera;

	private TextView textView;

	private int videoWidth = Integer.MAX_VALUE;
	private int videoHeight = Integer.MAX_VALUE;
	private int videoBitrate=1000;
	private EditText bitrateEditText;
	private UpdateUiReceiver receiver;
	private Button showVideoButton;
	private Button startStreamingButton;
	private Button stopStreamingButton;
	

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
		
		IntentFilter filter;
		filter = new IntentFilter(KrCamService.UPDATEUI);
		receiver = new UpdateUiReceiver();
		registerReceiver(receiver, filter);
		
		startStreamingButton.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				start();
				startStreamingButton.setEnabled(false);
				stopStreamingButton.setEnabled(true);
				showVideoButton.setEnabled(true);
 			}
 		});
		stopStreamingButton.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				Intent i = new Intent(MainActivity.this, KrCamService.class);
				stopService(i);
				startStreamingButton.setEnabled(true);
				stopStreamingButton.setEnabled(false);
				showVideoButton.setEnabled(false);
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
		i.putExtra("audioSampleRate", SAMPLERATE);
		startService(i);

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
			camera.release();
			showBitrateDialog();
		}
	}

	private void start() {
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
	}

	public class UpdateUiReceiver extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent) {
			final String uiString = intent.getStringExtra("uistring");
			MainActivity.this.runOnUiThread(new Runnable() {
				public void run() {
					textView.setText(uiString);
					startStreamingButton.setEnabled(false);
					stopStreamingButton.setEnabled(true);
					showVideoButton.setEnabled(true);
				}
			});
		}
	}
	

}
