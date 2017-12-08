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

public class SCUTCard extends JinLongCard {
    private Exception NotSCUTCard, NoBoiledWaterZone, NoOwnerInfoZone;
    public static byte[] GET_CARD_INFO1 = {0x00, (byte) 0xB0, (byte) 0x95, 0x00, 0x00};
    public static byte[] GET_CARD_INFO2 = {0x00, (byte) 0xB0, (byte) 0x96, 0x00, 0x00};
    public static byte[] GET_OWNER_INFO = {0x00, (byte) 0xB0, (byte) 0x97, 0x00, 0x00};
    public static byte[] GET_BOILED_WATER = {0x00, (byte) 0xB2, 0x01, 0x34, 0x00};

    public SCUTCard(TextView mVerboseInfo, Context mContext) {
        super(mVerboseInfo, mContext);
        //this.verbose = commonCard.verbose;
        NotSCUTCard = new Exception(mContext.getString(R.string.no1002));
        NoBoiledWaterZone = new Exception(mContext.getString(R.string.no1002_rec_0x6));
        NoOwnerInfoZone = new Exception(mContext.getString(R.string.no1002_bin_0x17));
    }

    // 以下为从卡中读取数据操作

    public byte[] fetchBoiledWaterInfo(IsoDep iso) throws Exception {
        byte[] t = sendAPDU(iso, GET_BOILED_WATER);
        if (t.length < 3) throw NoBoiledWaterZone;
        return t;
    }

    // 以下为从数据中解析相关信息的方法


    public float getBoiledWaterBalance(byte[] t) {
        return (float) (b2i(t[0x13]) * 0x1000000 + b2i(t[0x12]) * 0x10000 + b2i(t[0x11]) * 0x100 + b2i(t[0x10])) / 100.0f;
    }


    public String getOwnerHospitalNumber(byte[] t) {
        byte[] ownerHos = Arrays.copyOfRange(t, 0x33, getNullCharEndPosition(t, 0x33, 0x10));
        return new String(ownerHos);
    }


    // 汇总信息
    public String getGeneralInfo(IsoDep iso) throws Exception {
        byte[] t;
        int cardNum = 0;
        String expiredDate = "";
        float balance = 0, bwBalance = 0;
        String nameGBK = "", ownerNumber = "", ownerIdCardNum = "", ownerHosNum = "";
        try {
            t = selectAID(iso);
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
            t = fetchBoiledWaterInfo(iso);
            bwBalance = getBoiledWaterBalance(t);
        } catch (Exception ex) {
            // 不喝开水也无妨
        }
        // 获取持卡人基本信息
        try {
            t = fetchOwnerInfo(iso);
            nameGBK = getOwnerName(t);
            ownerNumber = getOwnerNumber(t);
            ownerIdCardNum = getOwnerIdCardNumber(t);
            ownerHosNum = getOwnerHospitalNumber(t);
        } catch (Exception ex) {
            throw ex;
        }
        // 汇总成html
        String h = "<h1><font color=\"#ff8000\"><big>" + nameGBK + " - 华工</big></font></h1>";
        h += "<h3>电子现金 余额：" + balance + "</h3>";
        h += "<h3>直饮水控 余额：" + bwBalance + "</h3>";
        h += "<h5>学号：" + ownerNumber + "</h5>";
        h += "<h5>卡号：" + cardNum + "， 有效期至" + expiredDate + "</h5>";
        h += "<h5>身份证号：" + ownerIdCardNum + "</h5>";
        h += "<h5>医院电脑号：" + ownerHosNum + "</h5>";
        return h;
    }
}
