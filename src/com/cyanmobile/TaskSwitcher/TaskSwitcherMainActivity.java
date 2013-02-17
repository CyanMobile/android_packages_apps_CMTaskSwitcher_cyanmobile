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
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.RemoteException;
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
    private ActivityManager am;
    private PackageManager pm;
    private static String myfilename = "applicationThumbnail.png";
    private TextView mTitle;
    private TextView noApps;
    private ImageView mIcons;
    private ImageView mKillButton;
    private ImageView myImageViews;
    ListView appsLV;
    List<App> appsList = new ArrayList<App>();
    AppsArrayAdapter adapter;
    private static int NUM_BUTTONS = 8;
    private static int MAX_RECENT_TASKS = NUM_BUTTONS * 2;
    private boolean mHidden = false;
    private boolean mHiding = false;
    private Handler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().getDecorView().setBackgroundColor(0x80000000);
        setContentView(R.layout.main);

        if (appsLV == null) {
            appsLV = (ListView)findViewById(R.id.apps_list_view);
            noApps = (TextView) findViewById(R.id.no_bg_app_bt);
            mKillButton = (ImageView) findViewById(R.id.kill_button);
	    mKillButton.setOnClickListener(mKillListener);
	    appsLV.setOnItemClickListener(this);
	    registerForContextMenu(appsLV);
            pm = this.getPackageManager();
            am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
            mHandler = new Handler();

            refresh();

	    adapter = new AppsArrayAdapter(getApplicationContext(), R.layout.appview, appsList);	
	    appsLV.setAdapter(adapter);
        }
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
        int ids = app.ids;

        switch (item.getItemId()) {
            case R.id.cmenu_end_app:
            	killApp(pkgName, ids);
         	Toast.makeText(getApplicationContext(), "Task removed!", Toast.LENGTH_SHORT).show();
         	refresh();
                return true;
            case R.id.cmenu_app_info:
            	getAppInfo(pkgName);
            	return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private View.OnClickListener mKillListener = new View.OnClickListener() {
        public void onClick(View v) {
            new EndAllTask().execute(appsList);
        }
    };

    public void killApp(String pkgName, int ids) {
        am.removeTask(ids, ActivityManager.REMOVE_TASK_KILL_PROCESS);
    }

    public void getAppInfo(String pkgName) {
	Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
	intent.addCategory(Intent.CATEGORY_DEFAULT);
	intent.setData(Uri.parse("package:"+pkgName));
	startActivity(intent);
        finish();
    }

    private void updateRecentTasks() {
    	
   	ActivityInfo homeInfo = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).resolveActivityInfo(pm, 0);

        List<ActivityManager.RecentTaskInfo> recentTasks =
                        am.getRecentTasks(MAX_RECENT_TASKS, ActivityManager.RECENT_IGNORE_UNAVAILABLE);

        int numTasks = recentTasks.size();
    	appsList.clear();
        for (int i = 0, index = 0; i < numTasks && (index < NUM_BUTTONS); ++i) {
            final ActivityManager.RecentTaskInfo info = recentTasks.get(i);

            Intent intent = new Intent(info.baseIntent);
            if (info.origActivity != null) {
                intent.setComponent(info.origActivity);
            }

            // Skip the current home activity.
            if (homeInfo != null
                && homeInfo.packageName.equals(intent.getComponent().getPackageName())
                && homeInfo.name.equals(intent.getComponent().getClassName())) {
                continue;
            }

            intent.setFlags((intent.getFlags()&~Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    | Intent.FLAG_ACTIVITY_NEW_TASK);
            final ResolveInfo resolveInfo = pm.resolveActivity(intent, 0);
            if (resolveInfo != null) {
                final ActivityInfo activityInfo = resolveInfo.activityInfo;
                final String title = activityInfo.loadLabel(pm).toString();
                Drawable icon = activityInfo.loadIcon(pm);
                final String myPackageName = activityInfo.packageName;
                int ids = info.persistentId;

                if (ids != -1 && title != null && title.length() > 0 && myPackageName != null && icon != null) {
                    String filelocation = "data/data/"+ myPackageName +"/files";
                    File file = new File(filelocation , myfilename);
                    Bitmap bmp = null;
                    if (file.exists()) {
                        bmp = BitmapFactory.decodeFile(filelocation +"/"+ myfilename);
                    } else {
                        continue;
                    }
                    if (bmp != null){
                        App app  = new App(title, myPackageName, ids, intent, icon, bmp);
                        appsList.add(app);
                    }
                    ++index;
                }
            }
        }
    }

    private final Runnable mRefreshRunnable = new Runnable() {
        public void run() {
 	    updateRecentTasks();
 	    adapter.notifyDataSetChanged();
            showRecents(appsList.size() > 0);
        }
    };
 
    private void showRecents(boolean show) {
    	if(show) {
	   appsLV.setVisibility(View.VISIBLE);
	   noApps.setVisibility(View.GONE);
           mKillButton.setVisibility(View.VISIBLE);
	} else {
	   noApps.setVisibility(View.VISIBLE);
	   appsLV.setVisibility(View.GONE);
           mKillButton.setVisibility(View.GONE);
	}
    }

    private class EndAllTask extends AsyncTask<List<App>, Integer, Long> {
         protected Long doInBackground(List<App>... names) {
	     for(App a : names[0]) {
	    	 killApp(a.pkgName, a.ids);
	     }
	     return null;
	 }

	 protected void onProgressUpdate(Integer... progress) {}

	 protected void onPostExecute(Long result) {
	     Toast.makeText(getApplicationContext(), "All apps remove!", Toast.LENGTH_SHORT).show();
	     refresh();
	 }
    }

    @Override
    public boolean onCreateThumbnail(Bitmap bitmap,Canvas canvas){
    	return false;
    }

    @Override
    public void finish() {
        moveTaskToBack(true);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        mHidden = !mHidden;
        if (mHidden) {
            mHiding = true;
            moveTaskToBack(true);
        } else {
            mHiding = false;
        }
        super.onNewIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        if (!mHiding && appsLV != null) {
            mHandler.removeCallbacks(mRefreshRunnable);
            mHandler.postDelayed(mRefreshRunnable, 50);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        refresh();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = (Intent)view.getTag();
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY
                            | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
            try {
                this.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.w("Recent", "Unable to launch recent task", e);
            }
        }
        finish();
    }
}



