package net.zjy.cardinfo;

/**
 * Created by ZJY on 2017/12/3.
 */

public class Schools {
    private static String[][] all = {
        {"SCUTCARD00000001", "华南理工大学"}, {"SYSUCARD00000001", "中山大学"}
    };

    public static String getSchoolName(String cardType) {
        for (int i = 0; i < all.length; i++) {
            if (all[i][0] == cardType) return all[i][1];
        }
        return "野鸡大学";
    }
}
