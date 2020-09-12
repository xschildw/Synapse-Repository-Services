package org.sagebionetworks.repo.manager.oauth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class ClaimsWithAuthTimeHelperTest {

    @Test
    void testRoundTrip() {
        Date now = new Date();
        Claims claims = Jwts.claims();
        ClaimsWithAuthTimeHelper.setAuthTime(claims, now);
        // The authTime is stored in s
        assertEquals(now.getTime()/1000L*1000, ClaimsWithAuthTimeHelper.getAuthTime(claims).getTime());
    }
}