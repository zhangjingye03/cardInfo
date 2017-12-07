package net.zjy.cardinfo;

import android.content.Context;
import android.nfc.tech.IsoDep;
import android.text.Html;
import android.widget.TextView;

import java.util.Arrays;

/**
 * Created by ZJY on 2017/11/24.
 */

public class CommonCard {
    public static String verbose; protected TextView mVerboseInfo; protected Context mContext;
    public Exception No1002, NoPurseRecord;
    public static byte[] GET_BALANCE = {(byte) 0x80, 0x5C, 0x00, 0x02, 0x04};

    public static int hex2Dec(byte in) {
        if (in < 0) return (in & 0xFF) / 16 * 10 + (in & 0xFF) % 16;
        return in / 16 * 10 + in % 16;
    }

    public static int h2d(byte in) {
        return hex2Dec(in);
    }

    public CommonCard(TextView mVerboseInfo, Context mContext) {
        //verbose = "";
        this.mVerboseInfo = mVerboseInfo; this.mContext = mContext;
        No1002 = new Exception(mContext.getString(R.string.no1002));
        NoPurseRecord = new Exception(mContext.getString(R.string.no_purse_rec));
    }

    public static int byte2Int(byte b) {
        return b & 0xFF;
    }

    public static int b2i(byte b) {
        return byte2Int(b);
    }

    public static char byte2Char(byte b) {
        if (b < 0xA) return (char) (b - 0 + '0');
        return (char) (b - 0xA + 'A');
    }

    public static String byte2Str(byte[] b) {
        return byte2Str(b, (char) 0);
    }

    public static String byte2Str(byte[] b, char split) {
        String tmp = "";
        for (int i = 0; i < b.length; i++) {
            byte former = 0, latter = 0;
            if (b[i] >= 0) {
                latter = (byte) (b[i] % 0x10);
                former = (byte) (b[i] / 0x10);
            } else {
                latter = (byte) ((b[i] - (byte)0x80) % 0x10);
                former = (byte) (((b[i] - (byte)0x80) / 0x10) + 0x8);
            }
            tmp += byte2Char(former);
            tmp += byte2Char(latter);
            if (split != 0) tmp += split;
        }
        return tmp;
    }

    public int getNullCharEndPosition(byte[] t, int start, int limit) {
        int i = start;
        for (; i < start + limit; i++) {
            if (t[i] == 0x0 || t[i] == 0xFF) break;
        }
        return i + 1;
    }

    public void addVerbose(String msg, String color) {
        verbose += "<br><font color=\"" + color + "\">" + msg + "</font>";

        mVerboseInfo.setText(Html.fromHtml(verbose));
        mVerboseInfo.append("\n");
    }

    public byte[] sendAPDU(IsoDep iso, byte[] to_send) {
        byte r[];
        try {
            addVerbose("[SEND]  " + byte2Str(to_send, ' '), "#0000ff");
            r = iso.transceive(to_send);
            addVerbose("[RECV]  " + byte2Str(r, ' '), "#00ff00");
        } catch (Exception ex) {
            addVerbose(ex.toString(), "#604f69");
            r = new byte[1]; //XXX
        }
        return r;
    }

    public byte[] selectFile(IsoDep iso, byte i1, byte i2) {
        try {
            byte[] s = {0x00, (byte) 0xA4, 0x00, 0x00, 0x02, i1, i2, 0x00};
            byte[] t = sendAPDU(iso, s);
            //if (t.length < 3) throw new Exception(mContext.getString(R.string.no_such_file) + i1 + " " + i2);
            return t;
        } catch (Exception ex) {
            return null;
        }
    }

    public byte[] selectFile(IsoDep iso, String identifier) {
        try {
            if (identifier.length() > 256) throw new Exception(mContext.getString(R.string.fi_too_long));
            byte[] s = {0x00, (byte) 0xA4, 0x04, 0x00, (byte) identifier.length()/* Lc field*/};
            byte[] ss = new byte[6 + identifier.length()];
            for (int i = 0; i < 5; i++) {
                ss[i] = s[i]; // 把基础指令拷过来
            }
            byte[] iden = identifier.getBytes();
            for (int i = 0; i < iden.length; i++) {
                ss[5 + i] = iden[i]; // 拷贝AID
            }
            ss[5 + iden.length] = 0; // Le Filed
            byte[] t = sendAPDU(iso, ss);
            //if (t.length < 3) throw new Exception(mContext.getString(R.string.no_such_file) + identifier);
            return t;
        } catch (Exception ex) {
            return null;
        }
    }

    public byte[] readBinary(IsoDep iso, byte sfi) throws Exception {
        try {
            byte[] s = {0x00, (byte) 0xb0, (byte) (0x80 + sfi), 0x00, 0x00};
            byte[] t = sendAPDU(iso, s);
            if (t.length < 3) throw new Exception(mContext.getString(R.string.no_such_rec) + sfi);
            return t;
        } catch (Exception ex) {
            throw ex;
        }
    }

    public byte[] readRecord(IsoDep iso, byte sfi) throws Exception {
        try {
            byte[] s = {0x00, (byte) 0xb2, (byte) ((sfi << 3) + 4), 0x00, 0x00};
            byte[] t = sendAPDU(iso, s);
            if (t.length < 3) throw new Exception(mContext.getString(R.string.no_such_rec) + sfi);
            return t;
        } catch (Exception ex) {
            throw ex;
        }
    }

    public float getBalance(IsoDep iso) throws Exception {
        try {
            byte[] t = sendAPDU(iso, GET_BALANCE);
            if (t.length < 3) throw NoPurseRecord;
            return (float) (b2i(t[0]) * 0x1000000 + b2i(t[1]) * 0x10000 + b2i(t[2]) * 0x100 + b2i(t[3])) / 100.0f;
        } catch (Exception ex) {
            throw ex;
        }
    }

    public byte[] selectAID(IsoDep iso) {
        return null;
    }

    public String getGeneralInfo(IsoDep iso) throws Exception {
        return null;
    }
}
