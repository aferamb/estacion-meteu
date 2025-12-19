package Utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import java.time.Instant;
import java.util.Date;

/**
 * Minimal JWT utility used by the server to issue and verify tokens for API clients.
 * Note: In production keep the SECRET outside the source (env/config/keystore).
 */
public class JwtUtil {

    // replace with environment var or configuration in production
    private static final String SECRET = "replace-with-a-strong-secret";
    private static final Algorithm ALG = Algorithm.HMAC256(SECRET);
    private static final JWTVerifier VERIFIER = JWT.require(ALG).build();

    // token validity in seconds
    private static final long TTL_SECONDS = 60 * 60 * 24; // 24 hours

    public static String generateToken(String username) {
        Instant now = Instant.now();
        Date iat = Date.from(now);
        Date exp = Date.from(now.plusSeconds(TTL_SECONDS));
        return JWT.create()
                .withSubject(username)
                .withIssuedAt(iat)
                .withExpiresAt(exp)
                .sign(ALG);
    }

    public static String validateTokenAndGetSubject(String token) {
        try {
            DecodedJWT jwt = VERIFIER.verify(token);
            return jwt.getSubject();
        } catch (Exception e) {
            return null;
        }
    }
}
