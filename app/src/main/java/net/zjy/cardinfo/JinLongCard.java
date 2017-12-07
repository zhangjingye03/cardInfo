package net.zjy.cardinfo;

import android.content.Context;
import android.nfc.tech.IsoDep;
import android.widget.TextView;

import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Created by ZJY on 2017/12/3.
 */

public class JinLongCard extends CommonCard {
    private Exception NotSYSUCard, NoOwnerInfoZone;
    public static byte[] GET_CARD_INFO1 = {0x00, (byte) 0xB0, (byte) 0x95, 0x00, 0x00};
    public static byte[] GET_CARD_INFO2 = {0x00, (byte) 0xB0, (byte) 0x96, 0x00, 0x00};
    public static byte[] GET_OWNER_INFO = {0x00, (byte) 0xB0, (byte) 0x97, 0x00, 0x00};
    public static byte[] GET_BOILED_WATER = {0x00, (byte) 0xB2, 0x01, 0x34, 0x00};

    public JinLongCard(TextView mVerboseInfo, Context mContext) {
        super(mVerboseInfo, mContext);
        //this.verbose = commonCard.verbose;
        NoOwnerInfoZone = new Exception(mContext.getString(R.string.no1002_bin_0x17));
    }

    // 以下为从卡中读取数据操作

    public byte[] fetchOwnerInfo(IsoDep iso) throws Exception {
        byte[] t = readBinary(iso, (byte) 0x17);
        if (t.length < 3) throw NoOwnerInfoZone;
        return t;
    }

    public byte[] selectAID(IsoDep iso) {
        byte[] t;
        try {
            t = selectFile(iso, (byte) 0x10, (byte) 0x02);
            if (t.length < 3 && t[0] != (byte) 0x90 && t[1] != 0) throw new Exception("没有1002文件区域！");
            return t;
        } catch (Exception ex) {
            //addVerbose(mContext.getString(R.string.no1002), "#ff0000"); // 没有1002文件区域
            return null;
        }
    }

    // 以下为从数据中解析相关信息的方法

    public String getCardType(byte[] zone_1002) {
        byte[] t = Arrays.copyOfRange(zone_1002, 0x04, 0x14);
        return new String(t);
    }

    public int getCardNum(byte[] zone_1002) {
        byte[] cardNum = Arrays.copyOfRange(zone_1002, 0x1d, 0x21);
        return b2i(cardNum[3]) * 0x1000000 + b2i(cardNum[2]) * 0x10000 + b2i(cardNum[1]) * 0x100 + b2i(cardNum[0]);
    }

    public String getCardExpiredDate(byte[] zone_1002) {
        byte[] expDate = Arrays.copyOfRange(zone_1002, 0x21, 0x25);
        return h2d(expDate[0]) + "" + h2d(expDate[1]) + "." + h2d(expDate[2]) + "." + h2d(expDate[3]);
    }

    public String getOwnerName(byte[] t) {
        byte[] name = Arrays.copyOfRange(t, 0x0, getNullCharEndPosition(t, 0, 0x10));
        return new String(name, Charset.forName("GBK"));
    }

    public String getOwnerNumber(byte[] t) {
        byte[] ownerNum = Arrays.copyOfRange(t, 0x1f, getNullCharEndPosition(t, 0x1f, 0x10));
        return new String(ownerNum);
    }

    public String getOwnerIdCardNumber(byte[] t) {
        byte[] ownerIdCardNum = Arrays.copyOfRange(t, 0x47, getNullCharEndPosition(t, 0x47, 18));
        return new String(ownerIdCardNum);
    }

    // 汇总信息
    public String getGeneralInfo(IsoDep iso) throws Exception {
        byte[] t;
        int cardNum = 0;
        String expiredDate = "";
        float balance = 0;
        String cardType = "", nameGBK = "", ownerNumber = "", ownerIdCardNum = "";
        try {
            t = selectAID(iso);
            cardNum = getCardNum(t);
            expiredDate = getCardExpiredDate(t);
            cardType = getCardType(t);
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
            ownerIdCardNum = getOwnerIdCardNumber(t);
        } catch (Exception ex) {
            throw ex;
        }
        // 汇总成html
        String h = "<h1><font color=\"#ff8000\"><big>" + nameGBK + " - " + Schools.getSchoolName(cardType) + "</big></font></h1>";
        h += "<h3>电子现金 余额：" + balance + "</h3>";
        h += "<h5>学号：" + ownerNumber + "</h5>";
        h += "<h5>卡号：" + cardNum + "， 有效期至" + expiredDate + "</h5>";
        h += "<h5>身份证号：" + ownerIdCardNum + "</h5>";
        return h;
    }
}
