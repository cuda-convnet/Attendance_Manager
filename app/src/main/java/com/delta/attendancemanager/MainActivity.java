package com.delta.attendancemanager;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.os.AsyncTaskCompat;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity {
    Context applicationContext=MainActivity.this;
    public static final String URL="https://61d8e2a4.ngrok.com";
    public static final String GOOGLE_PROJ_ID="275730371821";
    String regId="";
    public static final String REG_ID="REG-ID";
    public static final String RNO="rno";
    static boolean wrong=false;

    GoogleCloudMessaging gcmObj;
    MySqlAdapter handler;
    String usernme;
    String pass;
    static boolean isfirst;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Bundle b = getIntent().getExtras();
        if (b != null) {
            boolean g = b.getBoolean("wrong");
            if (g)
                wrong = true;

        }
        SharedPreferences prefs = getSharedPreferences("user",
                Context.MODE_PRIVATE);
        String rollno = prefs.getString(RNO, "default");

        if (!rollno.equals("default") && b==null) {                               // to ensure its not activated when logging out
            Intent i = new Intent(MainActivity.this, Userhome.class);
            i.putExtra("rno", rollno);
            startActivity(i);
            finish();

    }
//       InitialHandShake("110114070");
//        try {
//            regId = gcmObj
//                    .register(GOOGLE_PROJ_ID);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        handler=new MySqlAdapter(this,null);
        if(handler.get_days()==null){
            isfirst=true;
        }
        else
            isfirst=false;
        final EditText username = (EditText) findViewById(R.id.username);
        final EditText password = (EditText) findViewById(R.id.passwordm);
        if(wrong){
            YoYo.with(Techniques.Tada).duration(700).playOn(username);
            YoYo.with(Techniques.Tada).duration(700).playOn(password);
        }
        Button loginbutton = (Button) findViewById(R.id.login);
        Button crswitch = (Button) findViewById(R.id.crmodeswitch);

        loginbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (username.getText().length() == 0) {
                    Toast.makeText(MainActivity.this, "Enter a Roll.No", Toast.LENGTH_SHORT).show();
                } else if (password.getText().length() == 0) {
                    Toast.makeText(MainActivity.this, "Enter a password", Toast.LENGTH_SHORT).show();
                } else {
                    String user=username.getText().toString();
                    usernme=username.getText().toString();
                    pass=password.getText().toString();
                    Log.d("TAG", user + pass);
                    Authenticate a = new Authenticate();
                    a.execute(usernme, pass);

                }
            }
        });

        crswitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              startActivity(new Intent(applicationContext,CRLogin.class));
            }
        });

    }

    private void InitialHandShake(String user) {
        registerInBackground(user);


    }
    private void registerInBackground(final String rollnumber) {
        new AsyncTask<Void, Void, String>() {
            JSONParser jp;
            @Override
            protected String doInBackground(Void... params) {
                jp=new JSONParser();
                String msg = "";
                try {
                    if (gcmObj == null) {
                        gcmObj = GoogleCloudMessaging
                                .getInstance(applicationContext);
                        Log.i("came here","dgyc");
                    }
                    regId = gcmObj
                            .register(GOOGLE_PROJ_ID);
                    msg = "Registration ID :" + regId;
                    Log.d("fbj",regId);

                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                }
//                List<NameValuePair> aut=new ArrayList<>();
                JSONObject js=new JSONObject();


                try {
                    js.put("rollnumber",rollnumber);
                    js.put("regno", regId);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                JSONObject jd=jp.makeHttpRequest(URL+"/register","POST",js);
//                Log.i("Json",js.toString());
                try {
                    int success=jd.getInt("Signed Up");
                    if(success!=1){
                        wrongpassword();
                    }
                } catch (JSONException e) {
                    msg = "Error :" + e.getMessage();
                }
                return msg;
            }

          //  @TargetApi(Build.VERSION_CODES.GINGERBREAD)
            @Override
            protected void onPostExecute(String msg) {
                if (!TextUtils.isEmpty(regId)) {
                    // Store RegId created by GCM Server in SharedPref
                    storeRegIdinSharedPref(applicationContext, regId, rollnumber);
                    Toast.makeText(
                            applicationContext,
                            "Registered with GCM Server successfully.\n\n"
                                    + msg, Toast.LENGTH_SHORT).show();
                    SharedPreferences share=getSharedPreferences("user",Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor=share.edit();
                    editor.putString("rno", usernme);
                    editor.commit();
                    Intent i = new Intent(MainActivity.this, Userhome.class);
                    i.putExtra("rno", usernme);
                    startActivity(i);
                    finish();
                } else {
                    Toast.makeText(
                            applicationContext,
                            "Reg ID Creation Failed.\n\nEither you haven't enabled Internet or GCM server is busy right now. Make sure you enabled Internet and try registering again after some time."
                                    + msg, Toast.LENGTH_LONG).show();
                }

            }
        }.execute(null, null, null);
    }
    private void storeRegIdinSharedPref(Context context, String regId,
                                        String rollnumber) {
        SharedPreferences prefs = getSharedPreferences("user",
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(REG_ID, regId);
        editor.putString(RNO, rollnumber);
        editor.commit();


    }


    class Authenticate extends AsyncTask<String,Void,Boolean>{
        final String TAG = "JsonParser.java";



        @Override
        protected Boolean doInBackground(String... params) {
            JSONParser jp=new JSONParser();

            try {
               JSONObject js=new JSONObject();

                js.put("username",params[0]);
                js.put("password", params[1]);
                JSONObject jd=jp.makeHttpRequest(URL+"/login","POST",js);
                Log.i(TAG,js.toString());
                int success=jd.getInt("logged_in");
                jp=null;
                js=null;
                return success==1;                                                //authentication
            }  catch (Exception e) {
                e.printStackTrace();

            }


            return false;
        }

      //  @TargetApi(Build.VERSION_CODES.GINGERBREAD)
        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if(aBoolean){
                if(isfirst)
                    InitialHandShake(usernme);
                else {
                    SharedPreferences share=getSharedPreferences("user",Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor=share.edit();
                    editor.putString(RNO, usernme);
                    editor.commit();
                    Intent i = new Intent(MainActivity.this, Userhome.class);
                    i.putExtra("rno", usernme);

                   // startActivity(i);
//                    finish();
                }
            }
            else{
                wrongpassword();
            }
        }
    }

    private void wrongpassword() {
        Intent i =new Intent(MainActivity.this,MainActivity.class);
        i.putExtra("wrong",true);
        startActivity(i);  //TODO: enhance with textview "Wrong password"

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
