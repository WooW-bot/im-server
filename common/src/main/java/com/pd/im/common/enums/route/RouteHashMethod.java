package com.pd.im.common.enums.route;

/**
 * @author Parker
 * @date 12/8/25
 */
public enum RouteHashMethod {
    /**
     * TreeMap
     */
    TREE(1, "com.pd.im.common.route.algorithm.hash.TreeMapConsistentHash"),

    /**
     * 自定义map
     */
    CUSTOMER(2, "com.pd.im.common.route.algorithm.hash.xxxx"),

    ;


    private int code;
    private String clazz;

    /**
     * 不能用 默认的 enumType b= enumType.values()[i]; 因为本枚举是类形式封装
     *
     * @param ordinal
     * @return
     */
    public static RouteHashMethod getHandler(int ordinal) {
        for (int i = 0; i < RouteHashMethod.values().length; i++) {
            if (RouteHashMethod.values()[i].getCode() == ordinal) {
                return RouteHashMethod.values()[i];
            }
        }
        return null;
    }

    RouteHashMethod(int code, String clazz) {
        this.code = code;
        this.clazz = clazz;
    }

    public String getClazz() {
        return clazz;
    }

    public int getCode() {
        return code;
    }
}
