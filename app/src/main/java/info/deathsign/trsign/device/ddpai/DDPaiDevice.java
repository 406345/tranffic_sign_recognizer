package info.deathsign.trsign.device.ddpai;

import android.content.Context;
import android.renderscript.ScriptGroup;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import info.deathsign.trsign.device.IDevice;
import info.deathsign.trsign.device.VideoCodecInfo;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DDPaiDevice implements IDevice {
    private final String TAG = "TSIGNREC";
    private final String FAKE_IMEI = "444444444444444";
    private final String USERNAME = "admin";
    private final String DEVICE_NAME = "HUAWEI P1000";
    private final String PASSWORD = "admin";

    private VideoCodecInfo videoCodecInfo = new VideoCodecInfo();
    private Socket frameSocket;
    private InputStream socketInputStream;
    private String host;
    HttpHelper httpHelper = new HttpHelper();
    private Context context;

    @Override
    public boolean init(Context context) {
        this.context = context;
        return true;
    }

    @Override
    @SuppressWarnings(value = "")
    public boolean connect(String host) {
        this.host = host;
        JSONObject obj;
        String rep = "";

        rep = httpHelper.get(cmdUrl("API_GetBaseInfo"));
        Log.d(TAG, rep);

        obj = httpHelper.getJson(cmdUrl("API_RequestSessionID"));
        if (obj == null) {
            return false;
        }

        if (!isOK(obj))
            return false;

        try {
            String sid = new JSONObject(obj.getString("data")).getString("acSessionId");
            httpHelper.setSessionId(sid);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        obj = httpHelper.postJson(cmdUrl("API_RequestCertificate"), String.format(
                "{\"user\" : \"%s\", \"password\" : \"%s\", \"level\":0, \"uid\":\"" + FAKE_IMEI + "\"}",
                USERNAME,
                PASSWORD
        ));


        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat formatter = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance();
        formatter.applyLocalizedPattern("yyyyMMddHHmmss");
        obj = httpHelper.postJson(cmdUrl("API_SyncDate"), String.format(
                "{\"date\" : \"%s\", \"imei\" : \"%s\", \"time_zone\": 28800, \"format\":\"yyyy-MM-dd HH:mm:ss\", \"lang\": \"zh_CN\"}",
                formatter.format(date),
                FAKE_IMEI
        ));

        if (!isOK(obj))
            return false;

        obj = httpHelper.postJson(cmdUrl("API_GetBaseInfo"), "");
        if (!isOK(obj))
            return false;

        obj = httpHelper.postJson(cmdUrl("APP_AvCapReq"), "");
        if (!isOK(obj))
            return false;

        try {
            obj = obj.getJSONObject("data");
            videoCodecInfo.audSampleRate = obj.getInt("aud_samplerate");
            videoCodecInfo.bitRate = obj.getInt("ss_bitrat");
            videoCodecInfo.frmRate = obj.getInt("ss_frmrate");
            videoCodecInfo.width = Integer.valueOf(obj.getString("ss_pixel").split("x")[0]);
            videoCodecInfo.height = Integer.valueOf(obj.getString("ss_pixel").split("x")[1]);

        } catch (JSONException e) {
            e.printStackTrace();
        }


//        obj = httpHelper.postJson(cmdUrl("APP_AvCapSet"), "{\"stream_type\":0,\"frmrate\":30}");
//        if (!isOK(obj))
//            return false;

        obj = httpHelper.postJson(cmdUrl("API_GeneralQuery"), "{\"keys\":[{\"key\":\"image_quality\"},{\"key\":\"video_download_mode\"},{\"key\":\"tar_download_mode\"},{\"key\":\"video_codec\"},{\"key\":\"record_resolution\"},{\"key\":\"event_before_time\"},{\"key\":\"event_after_time\"},{\"key\":\"wdr_enable\"},{\"key\":\"mic_switch\"},{\"key\":\"display_mode\"},{\"key\":\"supportOneKeyReport\"},{\"key\":\"supportCloudAlbum\"},{\"key\":\"auto_upload\"},{\"key\":\"ips_mgr_switch\"},{\"key\":\"ai_algorithm_sensitivity\"},{\"key\":\"adas_type\"}]}");
        if (!isOK(obj))
            return false;

        boolean res = true;
        res = this.setLogonInfo();
        res = this.setAppLiveState();
        res = this.setLogonInfo();
        res = this.playbackLiveSwitch();

        Toast.makeText(context, "开始连接TCP流", Toast.LENGTH_SHORT).show();

        if(frameSocket!=null){
            try {
                frameSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            frameSocket = new Socket(this.host, 6200);
            socketInputStream = frameSocket.getInputStream();
//            InputStream inputStream = frameSocket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    public VideoCodecInfo getCodecInfo() {
        return videoCodecInfo;
    }

    private boolean playbackLiveSwitch() {
        JSONObject obj = httpHelper.postJson(cmdUrl("APP_PlaybackLiveSwitch"), "{\"switch\":\"live\", \"playtime\": \"\"}");
        if (!isOK(obj))
            return false;


        return true;
    }

    private boolean setAppLiveState() {
        JSONObject obj = httpHelper.postJson(cmdUrl("API_SetAppLiveState"), "{\"switch\":\"on\"}");
        if (!isOK(obj))
            return false;


        return true;
    }

    private boolean setLogonInfo() {
        SimpleDateFormat formatter = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance();
        formatter.applyPattern("yyyy-MM-dd HH:mm:ss");
        JSONObject obj = httpHelper.postJson(cmdUrl("API_SetLogonInfo"),
                String.format(
                        "{\"device_name\":\"%s\",\"imei\":\"%s\",\"logon_time\":\"%s\",\"postion\":\"Unknown\"}",
                        DEVICE_NAME,
                        FAKE_IMEI,
                        formatter.format(new Date(System.currentTimeMillis()))

                ));
        if (!isOK(obj))
            return false;

        return true;
    }

    @Override
    public void disconnect() {

    }

    @Override
    public InputStream getVideoSegment() {
        try {
            return this.frameSocket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String cmdUrl(String cmd) {
        return "http://" + this.host + "/vcam/cmd.cgi?cmd=" + cmd;
    }

    private boolean isOK(JSONObject obj) {
        if (obj.isNull("errcode")) {
            return false;
        }

        try {
            return obj.getInt("errcode") == 0;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return false;
    }


    static class HttpHelper {

        private final String TAG = "HTTP_HELPER";

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(50L, TimeUnit.SECONDS)
                .readTimeout(60L, TimeUnit.SECONDS)
                .build();

        private String sessionId = "";

        public void setSessionId(String sid) {
            this.sessionId = sid;
        }

        public JSONObject getJson(String url) {
            try {
                return new JSONObject(get(url));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        public String get(String url) {
            Request req = new Request.Builder()
                    .url(url)
                    .addHeader("sessionid", this.sessionId)
                    .build();

            return this.sendOKHttp(req);
        }

        public JSONObject postJson(String url, String data) {
            try {
                return new JSONObject(post(url, data));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        public String post(String url, String data) {
            Request req = new Request.Builder()
                    .url(url)
                    .method("POST", RequestBody.create(MediaType.parse("application/json"), data))
                    .addHeader("sessionid", this.sessionId)
                    .build();

            return this.sendOKHttp(req);
        }

        private String sendOKHttp(Request request) {
            String responseBody = "";
            Response response = null;
            try {
                response = client.newCall(request).execute();
                //int status = response.code();
                if (response.isSuccessful()) {
                    responseBody = response.body().string();
                } else {
//                logger.error("code>>: {} : okHttpUrl>>:{}  body>>:{} ", response.code(),request.url(), StringUtils.isEmpty(request.body()) ? "" : request.body().toString());
                }
            } catch (Exception e) {
                Log.d(TAG, e.getMessage());
            } finally {
                if (response != null) {
                    response.close();
                }
            }
            return responseBody;
        }
    }


}
