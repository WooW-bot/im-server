package com.pd.im.common.enums.device;

import com.pd.im.common.enums.CodeProvider;

/**
 * @author Parker
 * @date 12/4/25
 */
public enum ClientType implements CodeProvider {
    WEBAPI(0, "webapi"),
    WEB(1, "web"),
    IOS(2, "ios"),
    ANDROID(3, "android"),
    WINDOWS(4, "windows"),
    MAC(5, "mac"),
    ;

    private Integer code;
    private String info;

    ClientType(int code, String info) {
        this.code = code;
        this.info = info;
    }

    @Override
    public Integer getCode() {
        return this.code;
    }

    public String getInfo() {
        return info;
    }

    /**
     * 判断是否为同一类型客户端
     *
     * @param dtoClientType     用户当前登录端信息
     * @param channelClientType 用户之前登录端信息
     * @return 是否为同一类型客户端
     */
    public static boolean isSameClient(Integer dtoClientType, Integer channelClientType) {
        if (isPcClient(dtoClientType) && isPcClient(channelClientType)) {
            return true;
        }
        if (isMobileClient(dtoClientType) && isMobileClient(channelClientType)) {
            return true;
        }
        return false;
    }

    /**
     * 判断是否为PC端
     *
     * @param clientType 客户端类型
     * @return true-PC端(Windows/Mac)，false-其他
     */
    public static boolean isPcClient(Integer clientType) {
        return WINDOWS.getCode().equals(clientType) || MAC.getCode().equals(clientType);
    }

    /**
     * 判断是否为移动端
     *
     * @param clientType 客户端类型
     * @return true-移动端(iOS/Android)，false-其他
     */
    public static boolean isMobileClient(Integer clientType) {
        return IOS.getCode().equals(clientType) || ANDROID.getCode().equals(clientType);
    }

    /**
     * 判断是否为Web端
     *
     * @param clientType 客户端类型
     * @return true-Web端(Web/WebAPI)，false-其他
     */
    public static boolean isWebClient(Integer clientType) {
        return WEB.getCode().equals(clientType) || WEBAPI.getCode().equals(clientType);
    }
}
