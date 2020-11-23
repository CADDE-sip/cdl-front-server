/*
    UserContextクラス

    COPYRIGHT FUJITSU LIMITED 2021
*/

package com.fujitsu.cdlfrontserver.usermanager;

import java.io.Serializable;
import java.util.Set;

import com.fujitsu.cdlfrontserver.api.ApiException;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;

public class UserContext implements User, Serializable {

    private static final long serialVersionUID = 1L;
    private String name;
    private Set<String> roles;
    private String account;
    private String affiliation;
    private Enrollment enrollment;
    private String mspId;
    private String org;
    private String hashSecret;

    /**
     * @author Nishimaki
     */
    protected UserContext() {
    }

    /**
     * @param sessionId セッションID。認証したユーザごとに発行される
     * @return セッションIDに紐づいたUserContextのインスタンス
     */
    public static UserContext convertSessionId(String sessionId) throws ApiException {
        UserContext context = SessionId.getUserContext(sessionId);
        return context;
    }

    /**
     * @param enrollment Fabric CAでenrollしたインスタンス
     */
    protected void setEnrollment(Enrollment enrollment) {
        this.enrollment = enrollment;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOrg(String org) {
        this.org = org;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    public void setMspId(String mspId) {
        this.mspId = mspId;
    }

    public void setHashSecret(String hashSecret) {
        this.hashSecret = hashSecret;
    }

    /**
     * @return 組織名
     */
    protected String getOrg() {
        return org;
    }

    public String getHashSecret() {
        return hashSecret;
    }

    /**
     * @return String
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * @return Set
     */
    @Override
    public Set<String> getRoles() {
        return roles;
    }

    /**
     * @return String
     */
    @Override
    public String getAccount() {
        return account;
    }

    /**
     * @return String
     */
    @Override
    public String getAffiliation() {
        return affiliation;
    }

    /**
     * @return Enrollment
     */
    @Override
    public Enrollment getEnrollment() {
        return enrollment;
    }

    /**
     * @return String
     */
    @Override
    public String getMspId() {
        return mspId;
    }
}
