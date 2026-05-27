package com.sw.api.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.SignatureAlgorithm;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JwtService.class);
    private final java.util.Set<String> localBlacklist = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private final StringRedisTemplate redisTemplate;

    // Esta llave secreta la pondremos en el application.properties enseguida
    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public String generarToken(UserDetails userDetails) {
        return generarToken(new HashMap<>(), userDetails);
    }

    public String generarToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .setClaims(extraClaims) 
                .setSubject(userDetails.getUsername()) 
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public void blacklistToken(String token) {
        Date expiration = extractExpiration(token);
        long remainingTime = expiration.getTime() - System.currentTimeMillis();
        
        if (remainingTime > 0) {
            try {
                redisTemplate.opsForValue().set(token, "blacklisted", remainingTime, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                logger.warn("⚠️ Redis no está disponible. Guardando token en memoria local: {}", e.getMessage());
                localBlacklist.add(token);
            }
        }
    }

    public boolean isTokenBlacklisted(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(token));
        } catch (Exception e) {
            logger.warn("⚠️ Redis no está disponible para verificar el blacklist. Usando respaldo en memoria local: {}", e.getMessage());
            return localBlacklist.contains(token);
        }
    }
}
