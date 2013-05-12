package ws.websca.krcam;

import java.io.IOException;
import java.util.List;



import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements Callback, PreviewCallback {

	public native String vpxOpen(String path, int w, int h, int threads);
	public native String vpxNextFrame(byte input[], int w, int h);
	public native String vpxClose(String path);
	public native void vp8init();
	private SurfaceView surfaceView;
	private Camera camera;
	private long startMs = -1;
	private int frame;
	private TextView textView;
	private byte[] previewBuffer;
	private String formatString;
	private int useWidth = Integer.MAX_VALUE;
	private int useHeight = Integer.MAX_VALUE;

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
		camera.release();
	}
	
	protected void onPause() {
		super.onPause();
		camera.release();
	}
	
	
	
	protected void onStart() {
		super.onStart();

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
			builder.setSingleChoiceItems(choiceList, selected,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,	int which)
						{
							dialog.dismiss();
							Camera.Parameters parameters = camera.getParameters(); 
							int w=parameters.getSupportedPreviewSizes().get(which).width;
							int h=parameters.getSupportedPreviewSizes().get(which).height;
							parameters.setPreviewSize(w, h);
							camera.setParameters(parameters);
							int size = h*w+((h*w)/2);
							previewBuffer = new byte[size];
							camera.addCallbackBuffer(previewBuffer);
							
							vpxOpen(Environment.getExternalStorageDirectory()+"/krcam.ivf", w, h, 1);
							useWidth=w;
							useHeight=h;
							
							formatString = ""+parameters.getPreviewSize().width+"x"+parameters.getPreviewSize().height+" NV21 ";
							try {
								camera.setPreviewDisplay(surfaceView.getHolder());
							} catch (IOException e) {
								e.printStackTrace();
							}
							camera.setPreviewCallbackWithBuffer(MainActivity.this);
							camera.startPreview();
						}
					}
					);
			AlertDialog alert = builder.create();
	        alert.show();
			surfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
			surfaceView.getHolder().addCallback(this);
	}


	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {}
	public void surfaceCreated(SurfaceHolder holder) {}
	public void surfaceDestroyed(SurfaceHolder arg0) {}

	public void onPreviewFrame(byte[] buffer, Camera arg1) {
		if(startMs<0) {
			startMs  = System.currentTimeMillis();
			frame=0;
		}
		frame++;
		Log.e("vpx", vpxNextFrame(previewBuffer, useWidth, useHeight));
		camera.addCallbackBuffer(previewBuffer);
		if(System.currentTimeMillis()>=startMs+1000) {
			startMs=System.currentTimeMillis();

	        this.runOnUiThread(new Runnable() {
	            public void run() {
					textView.setText(formatString+" @"+frame+"FPS");
	            }
	        });
			frame=0;
		}
	}

}
