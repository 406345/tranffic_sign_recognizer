package info.deathsign.trsign;

import android.Manifest;
import android.annotation.SuppressLint;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.pm.PackageManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.net.wifi.SupplicantState;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import info.deathsign.trsign.device.ddpai.DDPaiDevice;
import info.deathsign.trsign.device.IDevice;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);
        toggle();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

    }

    IDevice device;
    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

    private void toggle() {
        new Thread(() -> {
            WifiUtils wifiUtils = new WifiUtils(getApplicationContext());
            wifiUtils.connectWifiPws("vYou_cam", "1234567890");

            while (wifiUtils.getConnection().getSupplicantState() != SupplicantState.COMPLETED) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (device == null) {
                device = new DDPaiDevice();
                device.init(this.getApplicationContext());
                if (!device.connect(wifiUtils.getGatewayIP())) {
                    Toast.makeText(this.getApplicationContext(), "ConnectionFailed", Toast.LENGTH_LONG).show();
                    return;
                }
                byte[] buffer = new byte[10240];
                String sdCard = Environment.getExternalStorageState();
                if (sdCard.equals(Environment.MEDIA_MOUNTED)){
                    Toast.makeText(this,"获得授权",Toast.LENGTH_LONG).show();
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && this.checkSelfPermission(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    this.requestPermissions(new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    }, 1);
                }

                File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),"record.mp4");
                FileOutputStream fis = null;
                try {
                    fis = new FileOutputStream(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                while (true) {
                    try {
                        if (device.getVideoSegment().available() > 0) {
                            while (device.getVideoSegment().available() > 0) {
                                int reads = device.getVideoSegment().read(buffer);
                                fis.write(buffer, 0, reads);
                            }
                        } else {
                            SystemClock.sleep(10);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

//                long startMs = System.currentTimeMillis();
//                byte[] buffer = new byte[10240];
//
//                MediaFormat format = MediaFormat.createVideoFormat("video/avc", device.getCodecInfo().width, device.getCodecInfo().height);
//                format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
//                format.setInteger(MediaFormat.KEY_BIT_RATE, device.getCodecInfo().bitRate);
//                format.setInteger(MediaFormat.KEY_FRAME_RATE, device.getCodecInfo().frmRate);
////                format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
//                format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);
//                format.setInteger(MediaFormat.KEY_COMPLEXITY, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);
//
//                MediaCodec codec = null;
//                try {
//                    codec = MediaCodec.createEncoderByType("video/avc");
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                SurfaceView sv = this.findViewById(R.id.surfaceView);
//                codec.configure(format, sv.getHolder().getSurface(), null, MediaCodec.CONFIGURE_FLAG_ENCODE);
//
//                codec.start();
//                ByteBuffer inputBuffer;
//
//                for (; ; ) {
//                    int inputBufferId = codec.dequeueInputBuffer(0);
//                    if (inputBufferId >= 0) {
//                        inputBuffer = codec.getInputBuffer(inputBufferId);
//                        inputBuffer.clear();
//
//                        try {
//                            int total = 0;
//                            while( device.getVideoSegment().available()>0 && total <= 1024*1024*2){
//                                int reads = device.getVideoSegment().read(buffer);
//                                total += reads;
//                                inputBuffer.put(buffer, 0, reads);
//                            }
//                            codec.queueInputBuffer(inputBufferId, 0,total, 0, 0);
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    } else {
//                        SystemClock.sleep(50);
//                    }
//
//                    SystemClock.sleep(50);
//                    Log.d("DDDDDDDDDDD", String.format("%d",bufferInfo.presentationTimeUs));
//                    int outputBufferId = codec.dequeueOutputBuffer(bufferInfo, 0);
//                    if (outputBufferId >= 0) {
//
//                        while (bufferInfo.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
//                            try {
//                                Thread.sleep(100);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                        }
//
////                        ByteBuffer outputBuffer = codec.getOutputBuffer(outputBufferId);
////                        MediaFormat bufferFormat = codec.getOutputFormat(outputBufferId); // option A
//                        codec.releaseOutputBuffer(outputBufferId, (bufferInfo.size != 0));
//                    } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
//                        // Subsequent data will conform to new format.
//                        // Can ignore if using getOutputFormat(outputBufferId)
////                        codec.getOutputFormat(); // option B
//                    }
//                    System.gc();
//                }

//                codec.stop();
//                codec.release();

            }
        }).start();
    }


}