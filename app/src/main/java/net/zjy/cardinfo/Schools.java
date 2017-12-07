package net.zjy.cardinfo;

/**
 * Created by ZJY on 2017/12/3.
 */

public class Schools {
    private static String[][] allAid = {
        {"SCUTCARD", "华工"}, {"SYSUCARD", "中大"}
    };

    private static String[][] allHEMCId = {
            {"804100", "华工本科生"}
    };

    public static String getSchoolName(String cardType) {
        cardType = cardType.substring(0, 8);
        for (int i = 0; i < allAid.length; i++) {
            if (allAid[i][0].equals(cardType)) return allAid[i][1];
        }
        return "野鸡学校";
    }

    public static String getSchoolNameByHEMCId(String schoolId) {
        for (int i = 0; i < allHEMCId.length; i++) {
            if (allHEMCId[i][0].equals(schoolId)) return allHEMCId[i][1];
        }
        return "野人";
    }

}
