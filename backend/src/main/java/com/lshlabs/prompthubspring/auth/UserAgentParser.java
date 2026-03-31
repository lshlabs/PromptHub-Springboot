package com.lshlabs.prompthubspring.auth;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class UserAgentParser {

    private UserAgentParser() {
    }

    static ParsedUserAgent parse(String rawUserAgent) {
        if (rawUserAgent == null || rawUserAgent.isBlank()) {
            return new ParsedUserAgent(rawUserAgent, null, null, null);
        }

        String ua = rawUserAgent.trim();
        String lower = ua.toLowerCase(Locale.ROOT);

        String device;
        if (isBot(lower)) {
            device = "Bot";
        } else if (isTablet(lower)) {
            device = detectTabletFamily(ua);
            if (device == null) {
                device = "Tablet";
            }
        } else if (isMobile(lower)) {
            device = detectMobileFamily(ua);
            if (device == null) {
                device = "Mobile";
            }
        } else if (isPc(lower)) {
            device = detectPcFamily(ua);
            if (device == null) {
                device = "PC";
            }
        } else {
            device = "Unknown";
        }

        String browser = buildBrowser(ua, lower);
        String os = buildOs(ua, lower);
        return new ParsedUserAgent(rawUserAgent, device, browser, os);
    }

    private static boolean isBot(String lower) {
        return lower.contains("bot") || lower.contains("spider") || lower.contains("crawler")
                || lower.contains("slurp") || lower.contains("bingpreview");
    }

    private static boolean isTablet(String lower) {
        return lower.contains("ipad")
                || lower.contains("tablet")
                || (lower.contains("android") && !lower.contains("mobile"));
    }

    private static boolean isMobile(String lower) {
        return lower.contains("mobile")
                || lower.contains("iphone")
                || lower.contains("ipod")
                || lower.contains("android")
                || lower.contains("phone");
    }

    private static boolean isPc(String lower) {
        return lower.contains("windows") || lower.contains("macintosh")
                || lower.contains("linux") || lower.contains("x11");
    }

    private static String detectMobileFamily(String ua) {
        if (containsIgnoreCase(ua, "iPhone")) return "iPhone";
        if (containsIgnoreCase(ua, "Pixel")) return "Pixel";
        if (containsIgnoreCase(ua, "SM-")) return "Samsung";
        if (containsIgnoreCase(ua, "Android")) return "Android";
        return null;
    }

    private static String detectTabletFamily(String ua) {
        if (containsIgnoreCase(ua, "iPad")) return "iPad";
        if (containsIgnoreCase(ua, "Android")) return "Android Tablet";
        return null;
    }

    private static String detectPcFamily(String ua) {
        if (containsIgnoreCase(ua, "Windows")) return "Windows PC";
        if (containsIgnoreCase(ua, "Macintosh")) return "Mac";
        if (containsIgnoreCase(ua, "Linux")) return "Linux PC";
        return null;
    }

    private static String buildBrowser(String ua, String lower) {
        if (lower.contains("edg/")) return withVersion("Edge", extractVersion(ua, "Edg/([\\d.]+)"));
        if (lower.contains("chrome/") && !lower.contains("edg/")) {
            return withVersion("Chrome", extractVersion(ua, "Chrome/([\\d.]+)"));
        }
        if (lower.contains("firefox/")) return withVersion("Firefox", extractVersion(ua, "Firefox/([\\d.]+)"));
        if (lower.contains("safari/") && lower.contains("version/")) {
            return withVersion("Safari", extractVersion(ua, "Version/([\\d.]+)"));
        }
        return "Unknown";
    }

    private static String buildOs(String ua, String lower) {
        if (lower.contains("windows nt")) {
            String nt = extractVersion(ua, "Windows NT ([\\d.]+)");
            return withVersion("Windows", mapWindowsVersion(nt));
        }
        if (lower.contains("android")) {
            return withVersion("Android", extractVersion(ua, "Android ([\\d.]+)"));
        }
        if (lower.contains("iphone os") || lower.contains("cpu os")) {
            String ios = extractVersion(ua, "(?:iPhone OS|CPU OS) ([\\d_]+)");
            return withVersion("iOS", ios == null ? null : ios.replace('_', '.'));
        }
        if (lower.contains("mac os x")) {
            String mac = extractVersion(ua, "Mac OS X ([\\d_]+)");
            return withVersion("Mac OS X", mac == null ? null : mac.replace('_', '.'));
        }
        if (lower.contains("linux")) {
            return "Linux";
        }
        return "Unknown";
    }

    private static String mapWindowsVersion(String nt) {
        if (nt == null) return null;
        return switch (nt) {
            case "10.0" -> "10";
            case "6.3" -> "8.1";
            case "6.2" -> "8";
            case "6.1" -> "7";
            default -> nt;
        };
    }

    private static String withVersion(String family, String version) {
        if (version == null || version.isBlank()) {
            return family;
        }
        return family + " " + version;
    }

    private static String extractVersion(String source, String pattern) {
        Matcher matcher = Pattern.compile(pattern).matcher(source);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static boolean containsIgnoreCase(String source, String needle) {
        return source.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    record ParsedUserAgent(String rawUserAgent, String device, String browser, String os) {
    }
}
