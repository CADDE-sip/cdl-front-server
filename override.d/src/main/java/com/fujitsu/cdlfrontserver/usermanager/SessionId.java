/*
    SessionIdクラス

    COPYRIGHT FUJITSU LIMITED 2021
*/

package com.fujitsu.cdlfrontserver.usermanager;

import java.util.Map;
import java.util.HashMap;

import com.fujitsu.cdlfrontserver.api.ApiException;
import org.apache.commons.codec.digest.DigestUtils;

import javax.ws.rs.core.Response;

public class SessionId {
    static Map<String, UserContext> sessionMap = new HashMap<String, UserContext>();

    /**
     * @param context UserContextのインスタンス。認証したユーザ情報
     * @return 新たに生成したセッションID
     */
    protected static String generate(UserContext context) {
        String sessionId = "cdl-" + DigestUtils.md5Hex(context.getEnrollment().toString() + System.currentTimeMillis());
        sessionMap.put(sessionId, context);

        return sessionId;
    }

    /**
     * @param sessionId セッションID。認証したユーザごとに発行される
     * @return セッションIDに紐づくUserContextのインスタンス
     */
    protected static UserContext getUserContext(String sessionId) throws ApiException {
        UserContext context = sessionMap.get(sessionId);

        if (context == null) {
            throw new ApiException(Response.Status.BAD_REQUEST.getStatusCode(), "unknown session id '" + sessionId + "'");
        }

        return context;
    }
}
