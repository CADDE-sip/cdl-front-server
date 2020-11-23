/*
    IdTokenクラス

    COPYRIGHT FUJITSU LIMITED 2021
*/

package com.fujitsu.cdlfrontserver.usermanager;

import java.util.Date;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fujitsu.cdlfrontserver.api.ApiException;
import com.fujitsu.cdlfrontserver.api.Config;
import com.fujitsu.cdlfrontserver.api.LogMsg;

public class IdToken {

    private static LogMsg log = LogMsg.getInstance();

    /**
     * @author Nishimaki
     */
    private IdToken() {
    }

    /**
     * @param sessionId セッションID。認証したユーザごとに発行される
     * @return JWT形式のIDトークン
     */
    public static String convertSessionId( String givenSessionId ) throws ApiException {

        String sessionId;
        // 認証バイパスモードチェック & セッションID設定
        if ( givenSessionId == null && Config.get().enableBypassUserAuthentication() ) {
            sessionId = HLFGatewayManager.getInsntance().getBypassUserAuthenticationSessionID();
            log.notice( "Bypass User Authentication Mode is enabled : session ID = " + sessionId );
        } else {
            sessionId = givenSessionId;
        }

        UserContext context = SessionId.getUserContext( sessionId );
        String token = "";
        System.out.println( "IDトークン生成" );
        System.out.println( "user: " + context.getName() );
        System.out.println( "secret: " + context.getHashSecret() );
        try {
            Date issuedAt = new Date();
            Date expireTime = new Date();
            expireTime.setTime( expireTime.getTime() + Config.get().idTokenExpireTime() );

            Algorithm algorithm = Algorithm.HMAC256( Config.get().idTokenSecretKey() );
            token = JWT.create().withClaim( "cdluserid", context.getName() )
                    .withClaim( "cdlorganization", context.getOrg() ).withClaim( "cdlrole", context.getAffiliation() )
                    .withIssuedAt( issuedAt ).withIssuer( "fujitsu" ).sign( algorithm );
            System.out.println( token );
        } catch ( JWTCreationException exception ) {
            exception.printStackTrace();
        }
        return token;
    }

    /**
     * @param token  JWT形式のIDトークン
     * @param secret IDトークンに使用した共通鍵
     * @return {@code true}：:正当なIDトークンと判定。{@code false}：不正なIDトークンと判定
     */
    public static boolean verify( String token, String secret ) {
        System.out.println( "IDトークン検証" );
        try {
            Algorithm algorithm = Algorithm.HMAC256( secret );
            JWTVerifier verifier = JWT.require( algorithm ).withIssuer( "fujitsu" ).build(); // Reusable verifier
                                                                                             // instance
            DecodedJWT jwt = verifier.verify( token );

            System.out.println( "JWT cdluserid: " + jwt.getClaim( "cdluserid" ).asString() );
            System.out.println( "JWT cdlorganization: " + jwt.getClaim( "cdlorganization" ).asString() );
            System.out.println( "JWT cdlrole: " + jwt.getClaim( "cdlrole" ).asString() );
            System.out.println( "JWT iss: " + jwt.getIssuedAt() );
        } catch ( JWTVerificationException exception ) {
            exception.printStackTrace();
            return false;
        }
        return true;
    }
}
