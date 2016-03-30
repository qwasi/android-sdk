package com.qwasi.androidexample;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.qwasi.sdk.Qwasi;
import com.qwasi.sdk.QwasiConfig;
import com.qwasi.sdk.QwasiError;
import com.qwasi.sdk.QwasiMessage;

import io.hearty.witness.Reporter;
import io.hearty.witness.Witness;

public class MainActivity extends AppCompatActivity {
    Qwasi qwasi;
    //EditText log;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**
         * start of qwasi setup
         */
        final SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        String name = preferences.getString("name", null);
        String userToken = preferences.getString("userToken", null);
        qwasi = Qwasi.getInstance(getApplication());
        qwasi.qwasiWithConfig(new QwasiConfig(this));
        //log = (EditText) findViewById(R.id.log);
        final Qwasi value = qwasi;
        qwasi.useLocalNotifications = true;
        qwasi.registerDevice(preferences.getString("QwasiDeviceToken", null), name, userToken, new Qwasi.QwasiInterface() {
            @Override
            public void onSuccess(Object o) {
                preferences.edit().putString("deviceToken", qwasi.getMdeviceToken()).apply();
                preferences.edit().putBoolean("registered", true).apply();
                preferences.edit().putString("userToken", qwasi.getUserToken()).apply();
                value.subscribeToChannel("NON_AUTH_MEMBERS"); //default callback
                value.setPushEnabled(true, new Qwasi.QwasiInterface() {
                    @Override
                    public void onSuccess(Object o) {
                        preferences.edit().putString("gcm_token", o.toString()).apply();
                        value.fetchUnreadMessage();
                    }

                    @Override
                    public void onFailure(QwasiError e) {
                        Log.i("NCR Test", "Set Push Failed");
                    }
                });

            }

            @Override
            public void onFailure(QwasiError e) {
                e.printStackTrace();
            }
        });

        Witness.register(QwasiMessage.class, new Reporter() {
            @Override
            public void notifyEvent(Object o) {
                QwasiMessage message = (QwasiMessage) o;
                final String data = getResources().getString(R.string.app_name) + "\n" + //application name
                        message.malert + "\n" +
                        message.description() + "\n";
                Log.i("Qwasi Example", data);
            }
        });
        /**
         * end of Qwasi setup
         */

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onResume(){
        super.onResume();
        qwasi.fetchUnreadMessage();
    }
}
