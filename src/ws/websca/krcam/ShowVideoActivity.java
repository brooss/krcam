package ws.websca.krcam;
import android.app.Activity;
import android.content.Intent;

public class ShowVideoActivity extends Activity {
	protected void onResume() {
		super.onResume();
		Intent intent = new Intent(KrCamService.SHOWPREVIEW);
		intent.putExtra("show", true);
		sendBroadcast(intent);
	}
	
	protected void onStop() {
		super.onStop();
		Intent intent = new Intent(KrCamService.SHOWPREVIEW);
		intent.putExtra("show", false);
		sendBroadcast(intent);
	}
}