package net.zjy.cardinfo;

import android.content.Context;
import android.nfc.tech.IsoDep;
import android.widget.TextView;

import java.nio.charset.Charset;
import java.util.Arrays;

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

    // 选择PAY.TICL，但不需要其中的任何信息
    public void fetchBalance(IsoDep iso) throws Exception {
        byte[] t;
        try {
            t = selectFile(iso, "PAY.TICL");
            if (t.length < 3 && t[0] != (byte) 0x90 && t[1] != 0) throw new Exception("不是羊城通！");
        } catch (Exception ex) {
            throw ex;
        }
    }

    public byte[] fetchCardInfo(IsoDep iso) throws Exception {
        byte[] t;
        try {
            t = selectAID(iso);
            t = readBinary(iso, (byte) 0x15);
            return t;
        } catch (Exception ex) {
            throw ex;
        }
    }

    public byte[] fetchStudentCardInfo(IsoDep iso) throws Exception {
        byte[] t;
        try {
            t = readBinary(iso, (byte) 0x16);
            return t;
        } catch (Exception ex) {
            throw ex;
        }
    }

    // 获取卡号
    public String getCardNum(byte[] t) {
        byte[] c = Arrays.copyOfRange(t, 0xb, 0x10);
        return h2d(c[0]) + "" + h2d(c[1]) + "" + h2d(c[2]) + "" + h2d(c[3]) + "" + h2d(c[4]);
    }

    // 获取学生卡姓名
    public String getStudentName(byte[] t) {
        byte[] name = Arrays.copyOfRange(t, 0x0, getNullCharEndPosition(t, 0, 0x10));
        return new String(name, Charset.forName("GBK"));
    }

    // 获取学生卡学籍号
    public String getStudentStatusId(byte[] t) {
        byte[] id = Arrays.copyOfRange(t, 0x24, getNullCharEndPosition(t, 0x24, 26));
        return new String(id);
    }

    // 获取学生卡生效日期（传入的SFI为0x16）
    public String getStudentBeginDate(byte[] t) {
        byte[] from = Arrays.copyOfRange(t, 0x20, 0x24);
        String v = h2d(from[0]) + "" + h2d(from[1]) + "." + h2d(from[2]) + "." + h2d(from[3]);
        return v;
    }

    // 获取学生卡失效日期（传入的SFI为0x15）
    public String getStudentFinalDate(byte[] t) {
        byte[] from = Arrays.copyOfRange(t, 0x3c, 0x40);
        String v = h2d(from[0]) + "" + h2d(from[1]) + "." + h2d(from[2]) + "." + h2d(from[3]);
        return v;
    }
    // 获取卡片生效日期
    public String getValidBeginDate(byte[] t) {
        byte[] from = Arrays.copyOfRange(t, 0x17, 0x1b);
        return h2d(from[0]) + "" + h2d(from[1]) + "." + h2d(from[2]) + "." + h2d(from[3]);
    }

    // 获取卡片失效日期
    public String getValidFinalDate(byte[] t) {
        byte[] to = Arrays.copyOfRange(t, 0x1b, 0x1f);
        return h2d(to[0]) + "" + h2d(to[1]) + "." + h2d(to[2]) + "." + h2d(to[3]);
    }

    // 重写CommonCard中获取余额的方法
    public float getBalance(IsoDep iso) throws Exception {
        fetchBalance(iso);
        return super.getBalance(iso);
    }

    // 汇总信息
    public String getGeneralInfo(IsoDep iso) throws Exception {
        byte[] t; boolean isStudentCard = false;
        String h = "<h1><font color=\"#ff8000\"><big>羊城通";// - \" + Schools.getSchoolName(cardType) +  +
        String validDate = "", cardNum = "", stuName = "", stuStatusId = "", stuValidBeginDate = "", stuValidFinalDate = "";
        float balance = 0;
        try {
            // 获取基本卡片信息（SFI: 0x15）
            t = fetchCardInfo(iso);
            if (t[0x3c] != 0) {
                isStudentCard = true;
                h +="（学生卡）";
                stuValidFinalDate = getStudentFinalDate(t);
            }
            h += "</big></font></h1>";
            cardNum = getCardNum(t);
            validDate = getValidBeginDate(t) + " - " + getValidFinalDate(t);

            // 获取学生卡相关信息
            if (isStudentCard) {
                t = fetchStudentCardInfo(iso);
                stuName = getStudentName(t);
                stuStatusId = getStudentStatusId(t);
                stuValidBeginDate = getStudentBeginDate(t);
            }

            // 获取余额
            balance = getBalance(iso);
        } catch (Exception ex) {
            throw ex;
        }
        h += "<h3>余额：" + balance + "</h3>";
        h += "<h5>卡号：" + cardNum + "</h5>";
        h += "<h5>卡片有效期：" + validDate + "</h5>";
        if (isStudentCard) {
            h += "<h5>学生卡姓名：" + stuName + "</h5>";
            h += "<h5>学生学籍号：" + stuStatusId + "</h5>";
            h += "<h5>学生卡有效期：" + stuValidBeginDate + " - " + stuValidFinalDate + "</h5>";
        }
        return h;
    }
}
