package com.lshlabs.prompthubspring.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserAgentParserTest {

    @Test
    void parse_mobileUserAgent() {
        String ua = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) "
                + "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1";

        UserAgentParser.ParsedUserAgent parsed = UserAgentParser.parse(ua);

        assertEquals("iPhone", parsed.device());
        assertTrue(parsed.browser().startsWith("Safari"));
        assertTrue(parsed.os().startsWith("iOS"));
    }

    @Test
    void parse_pcUserAgent() {
        String ua = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

        UserAgentParser.ParsedUserAgent parsed = UserAgentParser.parse(ua);

        assertEquals("Mac", parsed.device());
        assertTrue(parsed.browser().startsWith("Chrome"));
        assertTrue(parsed.os().startsWith("Mac OS X"));
    }

    @Test
    void parse_botUserAgent() {
        String ua = "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)";

        UserAgentParser.ParsedUserAgent parsed = UserAgentParser.parse(ua);

        assertEquals("Bot", parsed.device());
    }

    @Test
    void parse_blankUserAgent_returnsNullFieldsLikeLegacyNone() {
        UserAgentParser.ParsedUserAgent parsed = UserAgentParser.parse(" ");

        assertNull(parsed.device());
        assertNull(parsed.browser());
        assertNull(parsed.os());
    }
}
