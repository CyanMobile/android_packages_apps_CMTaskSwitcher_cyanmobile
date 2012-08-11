package com.cyanmobile.TaskSwitcher;

import com.cyanmobile.TaskSwitcher.R;
import android.app.Activity;
import android.os.Bundle;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.Canvas;
import static android.util.Log.d;
import java.util.List;
import java.io.File;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class TaskSwitcherMainActivity extends Activity implements OnClickListener {
	private static String LOG_TAG = "ATaskSwitcherMainActivity";
	private ActivityManager am;
	private PackageManager pm;
	private static String myfilename = "applicationThumbnail.png";
	private LinearLayout ll;
	private ViewGroup axx;
        private TextView[] mTitle;
        private ImageView[] mIcons;
	private ImageView[] myImageViews;

	private static int NUM_BUTTONS = 8;
        private static int MAX_RECENT_TASKS = NUM_BUTTONS * 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        pm = this.getPackageManager();
        am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        ll = (LinearLayout) findViewById(R.id.ll);
    }
    
    @Override
    public void onResume(){
    	super.onResume();
    	
        mTitle = new TextView[NUM_BUTTONS];
    	myImageViews = new ImageView[NUM_BUTTONS];
        mIcons = new ImageView[NUM_BUTTONS];

    	ll.removeAllViews();
    	
    	Intent homeIntent = new Intent(Intent.ACTION_MAIN);
    	homeIntent.addCategory(Intent.CATEGORY_HOME);
    	ActivityInfo homeInfo = homeIntent.resolveActivityInfo(pm, 0);

        List<ActivityManager.RecentTaskInfo> recentTasks =
                        am.getRecentTasks(MAX_RECENT_TASKS, ActivityManager.RECENT_IGNORE_UNAVAILABLE);

        int index = 0;
        int numTasks = recentTasks.size();
        for (int i = 0; i < numTasks && (index < NUM_BUTTONS); ++i) {
            final ActivityManager.RecentTaskInfo info = recentTasks.get(i);

            Intent intent = new Intent(info.baseIntent);
            if (info.origActivity != null) {
                intent.setComponent(info.origActivity);
            }

            // Skip the current home activity.
            if (homeInfo != null) {
                if (homeInfo.packageName.equals(
                        intent.getComponent().getPackageName())
                        && homeInfo.name.equals(
                                intent.getComponent().getClassName())) {
                    continue;
                }
            }

            intent.setFlags((intent.getFlags()&~Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    | Intent.FLAG_ACTIVITY_NEW_TASK);
            final ResolveInfo resolveInfo = pm.resolveActivity(intent, 0);
            if (resolveInfo != null) {
                final ActivityInfo activityInfo = resolveInfo.activityInfo;
                final String title = activityInfo.loadLabel(pm).toString();
                Drawable icon = activityInfo.loadIcon(pm);
                final String myPackageName = activityInfo.packageName;

                if (title != null && title.length() > 0 && myPackageName != null && icon != null) {
                	
                	String filelocation = "data/data/"+ myPackageName +"/files";
                	
                	File file = new File(filelocation , myfilename);
                	Bitmap b = null;
                	if (file.exists()) {
                		d(LOG_TAG, "file exists");
                		b = BitmapFactory.decodeFile(filelocation +"/"+ myfilename);
                	} else {
                		d(LOG_TAG, "file doesn't exist: "+ filelocation);
                		continue;
                	}
                	if (b != null){
                		d(LOG_TAG, "	Bitmap found: "+ b);
                                axx = (RelativeLayout) View.inflate(this, R.layout.appview, null);
        	        	ImageView iv = new ImageView(this);
                                iv = (ImageView) axx.findViewById(R.id.iVScreenshot);
        	        	iv.setImageBitmap(b);
                                TextView thv = new TextView(this);
                                thv = (TextView) axx.findViewById(R.id.tVName);
                                thv.setText(title);
                                mTitle[index]=thv;
                                ImageView tv = new ImageView(this);
                                tv = (ImageView) axx.findViewById(R.id.iVIcon);
                                tv.setImageDrawable(icon);
                                mIcons[index]=tv;
        	        	ll.addView(axx);
        	        	iv.setTag(intent);
        	        	iv.setOnClickListener(this);
        	        	myImageViews[index]=iv;
        	        	++index;
                       }
                }
            }
        }
        if (index == 0) {
            axx = (RelativeLayout) View.inflate(this, R.layout.appview, null);
            ImageView iv = new ImageView(this);
            iv = (ImageView) axx.findViewById(R.id.iVScreenshot);
            iv.setVisibility(View.GONE);
            TextView thv = new TextView(this);
            thv = (TextView) axx.findViewById(R.id.tVName);
            thv.setText("No Recent Apps");
            ImageView tv = new ImageView(this);
            tv = (ImageView) axx.findViewById(R.id.iVIcon);
            tv.setVisibility(View.GONE);
            ll.addView(axx);
        }
    }
    
    @Override
    public boolean onCreateThumbnail(Bitmap bitmap,Canvas canvas){
    	return false;
    }

    @Override
    public void onClick(View v) {

           for (ImageView b: myImageViews){
             if (b != null && b == v) {
                // prepare a launch intent and send it
                Intent intent = (Intent)b.getTag();
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
                    try {
                        this.startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Log.w("Recent", "Unable to launch recent task", e);
                    }
                }
                break;
             }
           }
        finish();
    }
}



