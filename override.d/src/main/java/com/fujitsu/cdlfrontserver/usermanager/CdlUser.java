/*
    CdlUserクラス

    COPYRIGHT FUJITSU LIMITED 2021
*/

package com.fujitsu.cdlfrontserver.usermanager;

import com.fujitsu.cdlfrontserver.api.ApiException;
import org.hyperledger.fabric.sdk.User;

public class CdlUser {
    /**
     * @author Nishimaki
     */
    private String token;
    private String sessionId;
    private boolean validity;
    private User context;

    /**
     * @param id セッションID。enrollしたユーザごとに発行される
     */
    public CdlUser(String id) {
        this.sessionId = id;
    }

    /**
     * @return User
     */
    public User getUserContext() throws ApiException {
        context = UserContext.convertSessionId(sessionId);
        return context;
    }

    /**
     * @return JWT形式のIDトークン
     */
    public String getIDToken() throws ApiException {
        token = IdToken.convertSessionId(sessionId);
        return token;
    }

    /**
     * @return trueの場合はセッションIDが有効であり、ContextやIDトークンの取得が可能である。
     *         falseの場合はセッションIDが失効されているため、再度enroll処理が必要
     */
    public boolean checkValidity() {
        return validity;
    }
}
