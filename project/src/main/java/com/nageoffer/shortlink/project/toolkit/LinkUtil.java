package com.nageoffer.shortlink.project.toolkit;


import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.nageoffer.shortlink.project.common.enums.NetworkTypeEnum;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Date;
import java.util.Optional;

import static com.nageoffer.shortlink.project.common.constant.ShortLinkConstant.DEFAULT_CACHE_VALID_TIME;

/**
 * 短链接工具类
 */
public class LinkUtil {

    /**
     * 获取短链接缓存有效时间
     * @param validDate 有效期时间
     * @return 有效期时间戳
     */
    public static long getLinkCacheValidTime(Date validDate){
        return Optional.ofNullable(validDate)
                .map(each-> DateUtil.between(new Date(), each, DateUnit.MS))
                .orElse(DEFAULT_CACHE_VALID_TIME);
    }

    /**
     * 获取用户真实ip地址
     * @param request 请求
     * @return ip地址
     */
    public static String getActualIp(HttpServletRequest request){
        String ipAddress = request.getHeader("X-Forwarded-For");

        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }

        return ipAddress;

    }

    /**
     * 获取用户访问操作系统
     *
     * @param request 请求
     * @return 访问操作系统
     */
    public static String getOs(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent.toLowerCase().contains("windows")) {
            return "Windows";
        } else if (userAgent.toLowerCase().contains("mac")) {
            return "Mac OS";
        } else if (userAgent.toLowerCase().contains("linux")) {
            return "Linux";
        } else if (userAgent.toLowerCase().contains("unix")) {
            return "Unix";
        } else if (userAgent.toLowerCase().contains("android")) {
            return "Android";
        } else if (userAgent.toLowerCase().contains("iphone")) {
            return "iOS";
        } else {
            return "Unknown";
        }
    }

    /**
     * 获取用户访问浏览器
     *
     * @param request 请求
     * @return 访问浏览器
     */
    public static String getBrowser(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent.toLowerCase().contains("edg")) {
            return "Microsoft Edge";
        } else if (userAgent.toLowerCase().contains("chrome")) {
            return "Google Chrome";
        } else if (userAgent.toLowerCase().contains("firefox")) {
            return "Mozilla Firefox";
        } else if (userAgent.toLowerCase().contains("safari")) {
            return "Apple Safari";
        } else if (userAgent.toLowerCase().contains("opera")) {
            return "Opera";
        } else if (userAgent.toLowerCase().contains("msie") || userAgent.toLowerCase().contains("trident")) {
            return "Internet Explorer";
        } else {
            return "Unknown";
        }
    }

    /**
     * 获取用户访问设备
     *
     * @param request 请求
     * @return 访问设备
     */
    public static String getDevice(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent.toLowerCase().contains("mobile")) {
            return "Mobile";
        }
        return "PC";
    }

    /**
     * 获取用户访问网络类型（WiFi / 流量数据 / 有线 / 未知）
     * <p>
     * 说明：服务端无法 100% 可靠地区分 WiFi 与流量，该信息只存在于用户设备上，
     * 因此按两级策略获取：
     * <ol>
     *   <li>优先读取前端通过 Network Information API 上报的准确值
     *       （请求头 X-Network-Type 或请求参数 networkType）；</li>
     *   <li>未上报时做弱推断：移动设备（手机/平板）WiFi 与流量无法区分，返回 unknown；
     *       非移动设备（PC 等）几乎不存在蜂窝流量，按 wifi 处理。</li>
     * </ol>
     *
     * @param request 请求
     * @return 网络类型：wifi / cellular / ethernet / unknown
     */
    public static String getNetwork(HttpServletRequest request) {
        // 1. 优先读取前端上报的准确网络类型
        String networkType = request.getHeader("X-Network-Type");
        if (StrUtil.isNotBlank(networkType)) {
            return NetworkTypeEnum.of(networkType).getType();
        }
        networkType = request.getParameter("networkType");
        if (StrUtil.isNotBlank(networkType)) {
            return NetworkTypeEnum.of(networkType).getType();
        }

        // 2. 兜底弱推断：只能区分设备类型，无法真正区分 WiFi 与流量
        String userAgent = request.getHeader("User-Agent");
        if (StrUtil.isBlank(userAgent)) {
            return NetworkTypeEnum.UNKNOWN.getType();
        }
        String ua = userAgent.toLowerCase();
        boolean mobileDevice = ua.contains("iphone") || ua.contains("ipad")
                || ua.contains("android") || ua.contains("mobile");
        if (mobileDevice) {
            // 手机/平板既可能是 WiFi 也可能是流量，服务端无法区分
            return NetworkTypeEnum.UNKNOWN.getType();
        }
        // PC 端基本不存在蜂窝流量，统一按 WiFi 处理
        return NetworkTypeEnum.WIFI.getType();
    }
}
