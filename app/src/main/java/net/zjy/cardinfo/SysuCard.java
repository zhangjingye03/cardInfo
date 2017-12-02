package net.zjy.cardinfo;

import android.content.Context;
import android.nfc.tech.IsoDep;
import android.widget.TextView;

/**
 * Created by ZJY on 2017/12/3.
 */

public class SysuCard extends ScutCard {
    private Exception NotSYSUCard, NoOwnerInfoZone;
    public static byte[] GET_CARD_INFO1 = {0x00, (byte) 0xB0, (byte) 0x95, 0x00, 0x00};
    public static byte[] GET_CARD_INFO2 = {0x00, (byte) 0xB0, (byte) 0x96, 0x00, 0x00};
    public static byte[] GET_OWNER_INFO = {0x00, (byte) 0xB0, (byte) 0x97, 0x00, 0x00};
    public static byte[] GET_BOILED_WATER = {0x00, (byte) 0xB2, 0x01, 0x34, 0x00};

    public SysuCard(TextView mVerboseInfo, Context mContext, CommonCard commonCard) {
        super(mVerboseInfo, mContext, commonCard);
        this.verbose = commonCard.verbose;
        NotSYSUCard = new Exception(mContext.getString(R.string.no1002));
        NoOwnerInfoZone = new Exception(mContext.getString(R.string.no1002_bin_0x17));
    }

    // 汇总信息
    public String getGeneralInfo(IsoDep iso) throws Exception {
        byte[] t;
        int cardNum = 0;
        String expiredDate = "";
        float balance = 0;
        String nameGBK = "", ownerNumber = "", ownerIdCardNum = "";
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
        // 获取持卡人基本信息
        try {
            t = get_1002_bin_0x17(iso);
            nameGBK = getOwnerName(t);
            ownerNumber = getOwnerNumber(t);
            ownerIdCardNum = getOwnerIdCardNumber(t);
        } catch (Exception ex) {
            throw ex;
        }
        // 汇总成html
        String h = "<h1><font color=\"#ff8000\"><big>" + nameGBK + " - 中山大学</big></font></h1>";
        h += "<h3>电子现金 余额：" + balance + "</h3>";
        h += "<h5>学号：" + ownerNumber + "</h5>";
        h += "<h5>卡号：" + cardNum + "， 有效期至" + expiredDate + "</h5>";
        h += "<h5>身份证号：" + ownerIdCardNum + "</h5>";
        return h;
    }
}
