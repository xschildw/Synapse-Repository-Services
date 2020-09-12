package org.sagebionetworks.repo.manager.oauth;

import io.jsonwebtoken.Claims;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;

import java.util.Date;

public class ClaimsWithAuthTimeHelper {

    public static void setAuthTime(Claims claims, Date authTime) {
        claims.put(OIDCClaimName.auth_time.name(), authTime.getTime()/1000L);
    }

    public static Date getAuthTime(Claims claims) {
        return new Date(claims.get(OIDCClaimName.auth_time.name(), Long.class)*1000L);
    }

}
