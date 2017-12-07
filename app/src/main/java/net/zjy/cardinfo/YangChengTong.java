package net.zjy.cardinfo;

import android.content.Context;
import android.nfc.tech.IsoDep;
import android.widget.TextView;

/**
 * Created by ZJY on 2017/12/3.
 */

public class YangChengTong extends CommonCard {
    public YangChengTong(TextView mTextView, Context mContext) {
        super(mTextView, mContext);
    }

    public byte[] selectAID(IsoDep iso) {
        byte[] t;
        try {
            t = selectFile(iso, "PAY.APPY");
            if (t.length < 3 && t[0] != (byte) 0x90 && t[1] != 0) throw new Exception("不是羊城通卡！");
            return t;
        } catch (Exception ex) {
            return null;
        }
    }
}
