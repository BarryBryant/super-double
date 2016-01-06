package barqsoft.footballscores.widget;

/**
 * Created by Joopk on 1/4/2016.
 */

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.lang.annotation.Target;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import barqsoft.footballscores.DatabaseContract;
import barqsoft.footballscores.R;
import barqsoft.footballscores.Utilies;
import barqsoft.footballscores.ViewHolder;

/**
 * RemoteViewsService controlling the data being shown in the scrollable weather detail widget
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class DetailWidgetRemoteViewsService extends RemoteViewsService {
    public final String LOG_TAG = DetailWidgetRemoteViewsService.class.getSimpleName();

    // these indices must match the projection
    public static final int COL_HOME = 3;
    public static final int COL_AWAY = 4;
    public static final int COL_HOME_GOALS = 6;
    public static final int COL_AWAY_GOALS = 7;
    public static final int COL_DATE = 1;
    public static final int COL_LEAGUE = 5;
    public static final int COL_MATCHDAY = 9;
    public static final int COL_ID = 8;
    public static final int COL_MATCHTIME = 2;

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor data = null;

            @Override
            public void onCreate() {
                // Nothing to do
            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }
                // This method is called by the app hosting the widget (e.g., the launcher)
                // However, our ContentProvider is not exported so it doesn't have access to the
                // data. Therefore we need to clear (and finally restore) the calling identity so
                // that calls use our process and permission
                String[] dateArg = new String[1];
                final long identityToken = Binder.clearCallingIdentity();
                Date widgetDate = new Date(System.currentTimeMillis());
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                dateArg[0] = dateFormat.format(widgetDate);
                Uri footballUri = DatabaseContract.scores_table.buildScoreWithDate();
                data = getContentResolver().query(footballUri,
                        null,
                        null,
                        dateArg,
                        null);
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }


                RemoteViews views = new RemoteViews(getPackageName(),
                        R.layout.widget_detail_list_item);

                String homeTeam = data.getString(COL_HOME);
                String awayTeam = data.getString(COL_AWAY);
                int homeGoals = data.getInt(COL_HOME_GOALS);
                int awayGoals = data.getInt(COL_AWAY_GOALS);
                views.setTextViewText(R.id.widget_home_name, homeTeam);
                views.setTextViewText(R.id.widget_away_name, awayTeam);
                views.setTextViewText(R.id.widget_score_textview, Utilies.getScores(homeGoals, awayGoals));
                views.setTextViewText(R.id.widget_date_textview, data.getString(COL_MATCHTIME));
                views.setImageViewResource(R.id.widget_home_crest, Utilies.getTeamCrestByTeamName(
                        data.getString(COL_HOME)));
                views.setImageViewResource(R.id.widget_away_crest,Utilies.getTeamCrestByTeamName(
                        data.getString(COL_AWAY)));


                String description;
                if(homeGoals==-1 || awayGoals == -1){
                    description = getString(R.string.a11y_game_info_no_scores, homeTeam, awayTeam);
                }else {
                    description = getString(R.string.a11y_game_info, homeTeam,
                            awayTeam, homeGoals, awayGoals);
                }


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    setRemoteContentDescription(views, description);
                }


                return views;
            }

            @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
            private void setRemoteContentDescription(RemoteViews views, String description) {
                views.setContentDescription(R.id.widget_icon, description);
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_detail_list_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (data.moveToPosition(position))
                    return data.getLong(COL_ID);
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
