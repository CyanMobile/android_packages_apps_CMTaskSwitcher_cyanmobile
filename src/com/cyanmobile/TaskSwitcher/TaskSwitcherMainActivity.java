package com.cyanmobile.TaskSwitcher;

import com.cyanmobile.TaskSwitcher.R;
import android.app.Activity;
import android.os.Bundle;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.Canvas;
import android.net.Uri;
import static android.util.Log.d;
import java.util.List;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ListView;
import android.widget.Toast;

public class TaskSwitcherMainActivity extends Activity implements OnItemClickListener {
	private static String LOG_TAG = "ATaskSwitcherMainActivity";
	private ActivityManager am;
	private PackageManager pm;
	private static String myfilename = "applicationThumbnail.png";
        TextView mTitle;
        TextView noApps;
        ImageView mIcons;
	ImageView myImageViews;
        ListView appsLV;
	List<App> appsList = new ArrayList<App>();
	AppsArrayAdapter adapter;
	private static int NUM_BUTTONS = 8;
        private static int MAX_RECENT_TASKS = NUM_BUTTONS * 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        appsLV = (ListView)findViewById(R.id.apps_list_view);
        noApps = (TextView) findViewById(R.id.no_bg_app_bt);
	appsLV.setOnItemClickListener(this);
	registerForContextMenu(appsLV);
        pm = this.getPackageManager();
        am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);

        getAppsList();
        checkNoAppsRunning();

	adapter = new AppsArrayAdapter(getApplicationContext(), R.layout.appview, appsList);	
	appsLV.setAdapter(adapter);

    }

    @Override
    protected void onStart() {
        super.onStart();
 	getAppsList();
 	adapter.notifyDataSetChanged();
        checkNoAppsRunning();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
        App app =  (App) appsLV.getItemAtPosition(info.position);
        menu.setHeaderIcon(app.icon);
        menu.setHeaderTitle(app.name);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        App app =  (App) appsLV.getItemAtPosition(info.position);
        String pkgName = app.pkgName;
        
        switch (item.getItemId()) {
            case R.id.cmenu_end_app:
            	killApp(pkgName);
         		getAppsList();
         		adapter.notifyDataSetChanged();
         		Toast.makeText(getApplicationContext(), "Task removed!", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.cmenu_app_info:
            	getAppInfo(pkgName);
            	return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    public void killApp(String pkgName) {
        String filelocation = "data/data/"+ pkgName +"/files";
        File file = new File(filelocation , myfilename);
        if (file.exists()) {
            new CMDProcessor().su.runWaitFor("rm "+file);
        }
    }

    public void getAppInfo(String pkgName) {
	Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
	intent.addCategory(Intent.CATEGORY_DEFAULT);
	intent.setData(Uri.parse("package:"+pkgName));
	startActivity(intent);
    }

    public void getAppsList() {
    	
    	appsList.clear();

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
                                App app  = new App(title, myPackageName, intent, icon, b);
  		  		appsList.add(app);
                       }
                }
            }
        }
    }

    public void checkNoAppsRunning() {
    	if(appsList.size() > 0) {
	   appsLV.setVisibility(View.VISIBLE);
	   noApps.setVisibility(View.GONE);
	} else {
	   noApps.setVisibility(View.VISIBLE);
	   appsLV.setVisibility(View.GONE);
	}
    }

    @Override
    public boolean onCreateThumbnail(Bitmap bitmap,Canvas canvas){
    	return false;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = (Intent)view.getTag();
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
            try {
                this.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.w("Recent", "Unable to launch recent task", e);
            }
        }
	getAppsList();
	adapter.notifyDataSetChanged();
        checkNoAppsRunning();
    }
}



