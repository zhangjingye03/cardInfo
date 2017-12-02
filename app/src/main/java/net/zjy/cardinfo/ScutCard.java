package net.zjy.cardinfo;

/**
 * Created by ZJY on 2017/11/24.
 */

import android.content.Context;
import android.nfc.tech.IsoDep;
import android.text.Html;
import android.widget.TextView;

import net.zjy.cardinfo.CommonCard;

import java.nio.charset.Charset;
import java.util.Arrays;

public class ScutCard extends CommonCard {
    private Exception NotSCUTCard, NoBoiledWaterZone, NoOwnerInfoZone;
    public static byte[] GET_CARD_INFO1 = {0x00, (byte) 0xB0, (byte) 0x95, 0x00, 0x00};
    public static byte[] GET_CARD_INFO2 = {0x00, (byte) 0xB0, (byte) 0x96, 0x00, 0x00};
    public static byte[] GET_OWNER_INFO = {0x00, (byte) 0xB0, (byte) 0x97, 0x00, 0x00};
    public static byte[] GET_BOILED_WATER = {0x00, (byte) 0xB2, 0x01, 0x34, 0x00};

    public ScutCard(TextView mVerboseInfo, Context mContext, CommonCard commonCard) {
        super(mVerboseInfo, mContext);
        this.verbose = commonCard.verbose;
        NotSCUTCard = new Exception(mContext.getString(R.string.no1002));
        NoBoiledWaterZone = new Exception(mContext.getString(R.string.no1002_rec_0x6));
        NoOwnerInfoZone = new Exception(mContext.getString(R.string.no1002_bin_0x17));
    }

    // 以下为从卡中读取数据操作

    public byte[] get_1002_rec_0x6(IsoDep iso) throws Exception {
        byte[] t = sendAPDU(iso, GET_BOILED_WATER);
        if (t.length < 3) throw NoBoiledWaterZone;
        return t;
    }

    public byte[] get_1002_bin_0x17(IsoDep iso) throws Exception {
        byte[] t = sendAPDU(iso, GET_OWNER_INFO);
        if (t.length < 3) throw NoOwnerInfoZone;
        return t;
    }

    // 以下为从数据中解析相关信息的方法

    public int getCardNum(byte[] zone_1002) {
        byte[] cardNum = Arrays.copyOfRange(zone_1002, 0x1d, 0x21);
        return b2i(cardNum[3]) * 0x1000000 + b2i(cardNum[2]) * 0x10000 + b2i(cardNum[1]) * 0x100 + b2i(cardNum[0]);
    }

    public String getCardExpiredDate(byte[] zone_1002) {
        byte[] expDate = Arrays.copyOfRange(zone_1002, 0x21, 0x25);
        return h2d(expDate[0]) + "" + h2d(expDate[1]) + "." + h2d(expDate[2]) + "." + h2d(expDate[3]);
    }

    public float getBoiledWaterBalance(byte[] t) {
        return (float) (b2i(t[0x13]) * 0x1000000 + b2i(t[0x12]) * 0x10000 + b2i(t[0x11]) * 0x100 + b2i(t[0x10])) / 100.0f;
    }

    public String getOwnerName(byte[] t) {
        byte[] name = Arrays.copyOfRange(t, 0x0, getNullCharEndPosition(t, 0, 0x10));
        return new String(name, Charset.forName("GBK"));
    }

    public String getOwnerNumber(byte[] t) {
        byte[] ownerNum = Arrays.copyOfRange(t, 0x1f, getNullCharEndPosition(t, 0x1f, 0x10));
        return new String(ownerNum);
    }

    public String getOwnerHospitalNumber(byte[] t) {
        byte[] ownerHos = Arrays.copyOfRange(t, 0x33, getNullCharEndPosition(t, 0x33, 0x10));
        return new String(ownerHos);
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
        float balance = 0, bwBalance = 0;
        String nameGBK = "", ownerNumber = "", ownerIdCardNum = "", ownerHosNum = "";
        try {
            t = select_1002(iso);
            cardNum = getCardNum(t);
            expiredDate = getCardExpiredDate(t);
        } catch (Exception ex) {
            throw ex;
        }

        // 获取电子现金余额
        try {
            balance = getBalance(iso);
        } catch (Exception ex) {
            throw ex;
        }
        // 获取直饮水控余额
        try {
            t = get_1002_rec_0x6(iso);
            bwBalance = getBoiledWaterBalance(t);
        } catch (Exception ex) {
            throw ex;
        }
        // 获取持卡人基本信息
        try {
            t = get_1002_bin_0x17(iso);
            nameGBK = getOwnerName(t);
            ownerNumber = getOwnerNumber(t);
            ownerIdCardNum = getOwnerIdCardNumber(t);
            ownerHosNum = getOwnerHospitalNumber(t);
        } catch (Exception ex) {
            throw ex;
        }
        // 汇总成html
        String h = "<h1><font color=\"#ff8000\"><big>" + nameGBK + " - 华南理工大学</big></font></h1>";
        h += "<h3>电子现金 余额：" + balance + "</h3>";
        h += "<h3>直饮水控 余额：" + bwBalance + "</h3>";
        h += "<h5>学号：" + ownerNumber + "</h5>";
        h += "<h5>卡号：" + cardNum + "， 有效期至" + expiredDate + "</h5>";
        h += "<h5>身份证号：" + ownerIdCardNum + "</h5>";
        h += "<h5>医院电脑号：" + ownerHosNum + "</h5>";
        return h;
    }
}
