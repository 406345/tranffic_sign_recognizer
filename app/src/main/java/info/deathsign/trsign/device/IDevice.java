package info.deathsign.trsign.device;

import android.content.Context;

import java.io.InputStream;

public interface IDevice {

    boolean init(Context context);
    boolean connect(String ip);
    VideoCodecInfo getCodecInfo();
    void disconnect();
    InputStream getVideoSegment();

}
