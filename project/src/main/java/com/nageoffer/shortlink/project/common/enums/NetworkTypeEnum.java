package com.nageoffer.shortlink.project.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 用户访问网络类型枚举
 */
@RequiredArgsConstructor
public enum NetworkTypeEnum {

    /**
     * WiFi 网络
     */
    WIFI("wifi"),

    /**
     * 移动蜂窝数据（流量）
     */
    CELLULAR("cellular"),

    /**
     * 有线以太网
     */
    ETHERNET("ethernet"),

    /**
     * 未知 / 无法判断
     */
    UNKNOWN("unknown");

    @Getter
    private final String type;

    /**
     * 根据字符串解析网络类型枚举，无法匹配时返回 UNKNOWN
     *
     * @param type 网络类型字符串，如 "wifi"、"cellular"
     * @return 对应的网络类型枚举
     */
    public static NetworkTypeEnum of(String type) {
        for (NetworkTypeEnum value : values()) {
            if (value.type.equalsIgnoreCase(type)) {
                return value;
            }
        }
        return UNKNOWN;
    }
}
