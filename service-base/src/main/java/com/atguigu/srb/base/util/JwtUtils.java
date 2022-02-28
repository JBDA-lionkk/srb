package com.atguigu.srb.base.util;

import com.atguigu.common.exception.BusinessException;
import com.atguigu.common.result.ResponseEnum;
import io.jsonwebtoken.*;
import org.springframework.util.StringUtils;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.security.Key;
import java.util.Date;

public class JwtUtils {

    private static long tokenExpiration = 24 * 60 * 60 * 1000;
    private static String tokenSignKey = "A1t2g3uigu123456";

    private static Key getKeyInstance() {
        //生成key类型的秘钥
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
        byte[] bytes = DatatypeConverter.parseBase64Binary(tokenSignKey);
        return new SecretKeySpec(bytes, signatureAlgorithm.getJcaName());
    }

    public static String createToken(Long userId, String userName) {
        String token = Jwts.builder()
                .setSubject("SRB-USER")//令牌主题
                .setExpiration(new Date(System.currentTimeMillis() + tokenExpiration))//令牌过期时间
                .claim("userId", userId)//用户id
                .claim("userName", userName)//用户名
                .signWith(SignatureAlgorithm.HS512, getKeyInstance())//签名哈希  加密算法+秘钥
                .compressWith(CompressionCodecs.GZIP)
                .compact(); //组装jwt字符串
        return token;
    }

    /**
     * 判断token是否有效
     *
     * @param token
     * @return
     */
    public static boolean checkToken(String token) {
        if (StringUtils.isEmpty(token)) {
            return false;
        }
        try {
            //判断token 是否合法
            Jwts.parser().setSigningKey(getKeyInstance()).parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    public static Long getUserId(String token) {
        Claims claims = getClaims(token);
        Integer userId = (Integer) claims.get("userId");
        return userId.longValue();
    }

    public static String getUserName(String token) {
        Claims claims = getClaims(token);
        return (String) claims.get("userName");
    }

    public static void removeToken(String token) {
        //jwttoken无需删除，客户端扔掉即可。
    }

    /**
     * 校验token并返回Claims
     *
     * @param token
     * @return
     */
    private static Claims getClaims(String token) {
        if (StringUtils.isEmpty(token)) {
            // LOGIN_AUTH_ERROR(-211, "未登录"),
            throw new BusinessException(ResponseEnum.LOGIN_AUTH_ERROR);
        }
        try {
            Jws<Claims> claimsJws = Jwts.parser().setSigningKey(getKeyInstance()).parseClaimsJws(token);
            Claims claims = claimsJws.getBody();
            return claims;
        } catch (Exception e) {
            throw new BusinessException(ResponseEnum.LOGIN_AUTH_ERROR);
        }
    }
}
