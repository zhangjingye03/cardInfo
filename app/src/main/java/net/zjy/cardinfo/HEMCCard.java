package net.zjy.cardinfo;

import android.content.Context;
import android.nfc.tech.IsoDep;
import android.widget.TextView;

import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Created by ZJY on 12/7/2017.
 */

public class HEMCCard extends CommonCard {

    public HEMCCard(TextView mVerboseInfo, Context mContext) {
        super(mVerboseInfo, mContext);
    }

    public byte[] selectAID(IsoDep iso) {
        byte[] t;
        try {
            t = selectFile(iso, "DXC.PAY01");
            if (t[0] != (byte) 0x90 && t[1] != 0) throw new Exception("不是广州大学城一卡通！");
        } catch (Exception ex) {
            return null;
        }
        return t;
    }

    public byte[] fetchCardInfo(IsoDep iso) throws Exception {
        byte[] t;
        try {
            t = readBinary(iso, (byte) 0x15);
        } catch (Exception ex) {
            throw ex;
        }
        return t;
    }

    public byte[] fetchOwnerInfo(IsoDep iso) throws Exception {
        byte[] t;
        try {
            t = readBinary(iso, (byte) 0x16);
        } catch (Exception ex) {
            throw ex;
        }
        return t;
    }

    public byte[] fetchLastPurchaseInfo(IsoDep iso) throws Exception {
        byte[] t;
        try {
            t = readBinary(iso, (byte) 0x1d);
        } catch (Exception ex) {
            throw ex;
        }
        return t;
    }

    public int getCardNum(byte[] t) {
        byte[] cardNum = Arrays.copyOfRange(t, 0x38, 0x3c);
        return h2d(cardNum[0]) * 1000000 + h2d(cardNum[1]) * 10000 + h2d(cardNum[2]) * 100 + h2d(cardNum[3]);
    }

    public String getOwnerName(byte[] t) {
        byte[] name = Arrays.copyOfRange(t, 0x16, getNullCharEndPosition(t, 0x16, 0xa));
        return new String(name, Charset.forName("GBK"));
    }

    public String getOwnerNumber(byte[] t) {
        byte[] ownerNum = Arrays.copyOfRange(t, 0, getNullCharEndPosition(t, 0, 0x10));
        return new String(ownerNum);
    }

    public String getLastPurchaseDate(byte[] t) {
        byte[] lastDate = Arrays.copyOfRange(t, 0x1e, 0x22);
        return h2d(lastDate[0]) + "" + h2d(lastDate[1]) + "." + h2d(lastDate[2]) + "." + h2d(lastDate[3]);
    }

    public float getLastPurchaseValue(byte[] t) {
        byte[] lastSpent = Arrays.copyOfRange(t, 0x22, 0x26);
        return (float) (b2i(lastSpent[0]) * 0x1000000 + b2i(lastSpent[1]) * 0x10000 + b2i(lastSpent[2]) * 0x100 + b2i(lastSpent[3])) / 100.0f;
    }

    public String getSchoolId(byte[] t) {
        // 22 28
        byte[] schoolId = Arrays.copyOfRange(t, 0x2e, 0x2f);
        return new String(schoolId);
    }

    public String getSchoolId2(byte[] t) {
        byte[] schoolId = Arrays.copyOfRange(t, 0x22, 0x28);
        return new String(schoolId);
    }

    public String getGeneralInfo(IsoDep iso) throws Exception {
        byte[] t;
        int cardNum = 0;
        float balance = 0, lastPurchaseValue = 0;
        String nameGBK = "", ownerNumber = "", lastPurchaseDate = "", schoolId = "", schoolId2 = "";
        // 选择DXC.PAY01区域
        try {
            t = selectAID(iso);
            if (t == null) throw new Exception("不是广州大学城一卡通！");
        } catch (Exception ex) {
            throw ex;
        }

        // 获取电子现金余额
        try {
            balance = getBalance(iso);
        } catch (Exception ex) {
            throw ex;
        }
        // 获取持卡人基本信息
        try {
            t = fetchOwnerInfo(iso);
            nameGBK = getOwnerName(t);
            ownerNumber = getOwnerNumber(t);
            schoolId = getSchoolId(t);
            schoolId2 = getSchoolId2(t);
            cardNum = getCardNum(t);
        } catch (Exception ex) {
            throw ex;
        }
        // 获取上次消费记录
        try {
            t = fetchLastPurchaseInfo(iso);
            lastPurchaseValue = getLastPurchaseValue(t);
            lastPurchaseDate = getLastPurchaseDate(t);
        } catch (Exception ex) {
            throw ex;
        }
        // 汇总成html
        String h = "<h1><font color=\"#ff8000\"><big>" + nameGBK + " - " +
                Schools.getSchoolNameByHEMCId(schoolId) + "(" + schoolId2 + ")</big></font></h1>";
        h += "<h3>电子现金余额：" + balance + "（尚且不准）</h3>";
        h += "<h3>上次消费金额：" + lastPurchaseValue + "</h3>";
        h += "<h3>上次消费时间：" + lastPurchaseDate + "</h3>";
        h += "<h5>学号：" + ownerNumber + "</h5>";
        h += "<h5>卡号：" + cardNum + "</h5>";
        return h;
    }
}
