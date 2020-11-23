/*
    HLFGatewayManagerクラス

    COPYRIGHT FUJITSU LIMITED 2021
*/

package com.fujitsu.cdlfrontserver.usermanager;

import java.util.HashMap;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fujitsu.cdlfrontserver.api.Config;
import com.fujitsu.cdlfrontserver.api.LogLevel;
import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.Identities;
import org.hyperledger.fabric.gateway.Identity;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;

import com.fujitsu.cdlfrontserver.api.ApiException;
import com.fujitsu.cdlfrontserver.api.LogMsg;

public class HLFGatewayManager {

    private LogMsg log = new LogMsg();

    private static HLFGatewayManager SingletonInstnace = new HLFGatewayManager();

    // メモリ管理Walletを一つだけ生成
    private Wallet _wallet = Wallets.newInMemoryWallet();

    // セッションID → HLF接続情報管理マップ
    private HashMap<String, HLFGatewayInfo> _sessionIDHLFGatewayInfoMap = new HashMap<String, HLFGatewayInfo>();

    // 認証バイパスモード用セッションID
    private String _bypassUserAuthenticationSessionID = null;

    // シングルトンインスタンス取得
    public static HLFGatewayManager getInsntance() {
        return SingletonInstnace;
    }

    // コンストラクタ不可視
    private HLFGatewayManager() {
        try {
            if (Config.get().enableBypassUserAuthentication()) {
                getHLFGatewayInfo(null);
            }
        } catch (Throwable t) {
            ;
        }
    }

    /**
     * sessionId -> HLFコントラクト 変換
     *
     * @param sessionId
     * @return HLFコントラクト(org.hyperledger.fabric.gateway.Contract)オブジェクト
     * @throws Exception
     */
    public Contract getContract(String sessionId) throws Exception {
        return getHLFGatewayInfo(sessionId).getContract();
    }

    /**
     * @return _wallet
     */
    private Wallet getWallet() {
        return _wallet;
    }

    private HLFGatewayInfo getHLFGatewayInfo(String sessionId) throws Exception {

        if (sessionId == null || sessionId.isEmpty()) {
            if (!Config.get().enableBypassUserAuthentication()) {
                // 認証バイパスモードでなければ、BAD_REQUESTで返す
                throw new ApiException(Status.BAD_REQUEST.getStatusCode(), "session id is empty");
            }

            // 認証バイパスモード → バイパス用ユーザアカウントのセッションIDを生成
            if (getBypassUserAuthenticationSessionID() == null) {
                setBypassUserAuthenticationSessionID(UserAuthentication.enroll(Config.get().cdlHlfCAAdminAccount(),
                        Config.get().cdlHlfCAAdminPassword()));

                if (getBypassUserAuthenticationSessionID() == null) {
                    // 認証失敗時はInternal Server Error で返す
                    throw new ApiException(Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                            "Bypass User Authentication Mode is enabled. but user-id or password is wrong");

                }
            }

            sessionId = getBypassUserAuthenticationSessionID();
        }

        HLFGatewayInfo hlfgatewayinfo = _sessionIDHLFGatewayInfoMap.get(sessionId);

        // 既に HLF に接続済み → 登録済の接続情報を返す
        if (hlfgatewayinfo != null) {
            return hlfgatewayinfo;
        }

        // セッションIDから、ユーザIDを抽出
        UserContext userContext = SessionId.getUserContext(sessionId);

        if (userContext == null) {
            throw new ApiException(Status.BAD_REQUEST.getStatusCode(), "unknown session id '" + sessionId + "'");
        }

        String username = userContext.getName();

        if (username == null || username.isEmpty()) {
            throw new ApiException(Status.BAD_REQUEST.getStatusCode(),
                    "specified session id '" + sessionId + "' is broken? (username is empty)");
        }

        // Wallet にユーザーアクセス情報未登録時は、新たにWalletにユーザー情報を格納
        if (getWallet().get(username) == null) {
            System.out.println(
                    log.buildLogMsg(LogLevel.NOTICE, "User ID '" + username + "' not found in wallet. registering..."));

            Identity userIdentity = Identities.newX509Identity(userContext.getMspId(), userContext.getEnrollment());
            getWallet().put(username, userIdentity);

            System.out.print(log.buildLogMsg(LogLevel.NOTICE,
                    "User ID '" + username + "' put into wallet. registered users are..."));
            for (String user : getWallet().list()) {
                System.out.print(" '" + user + "'");
            }
            System.out.println("");
        }

        // HLFGatewayInfo 新規生成
        HLFGatewayInfo newHlfGatewayInfo = new HLFGatewayInfo(getWallet(), username);
        _sessionIDHLFGatewayInfoMap.put(sessionId, newHlfGatewayInfo);

        // 新規生成した HLFGatewayInfo
        return newHlfGatewayInfo;
    }

    /**
     * @return _bypassUserAuthenticationSessionID
     */
    public String getBypassUserAuthenticationSessionID() {
        return this._bypassUserAuthenticationSessionID;
    }

    /**
     * @param bypassUserAuthenticationSessionID セットする
     *                                          bypassUserAuthenticationSessionID
     */
    private void setBypassUserAuthenticationSessionID(String bypassUserAuthenticationSessionID) {
        this._bypassUserAuthenticationSessionID = bypassUserAuthenticationSessionID;
    }
}
