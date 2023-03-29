// Utils.java COPYRIGHT Fujitsu Limited 2021 and FUJITSU LABORATORIES LTD. 2021
package com.fujitsu.cdlfrontserver.common;

import com.fujitsu.cdlfrontserver.api.ApiException;
import com.fujitsu.cdlfrontserver.api.Config;
import com.fujitsu.cdlfrontserver.usermanager.HLFGatewayManager;
import com.fujitsu.cdlfrontserver.usermanager.UserAuthentication;
import com.fujitsu.cdlfrontserver.usermanager.UserContext;

import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;

import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

public class Utils {

    // CAClientオブジェクト
    private static HFCAClient caClient = null;

    /**
     * 文字列のnull、空チェック
     *
     * @param str  対象の文字列
     * @return チェック結果（null or 空文字列ならばtrue）
     */
    public static boolean isEmpty(String str){
        return str == null || str.isEmpty();
    }

    /**
     * 認証情報のチェック
     *   ユーザ未登録であれば、ユーザ登録後にenrollする
     *
     * @param cdluserId ユーザーID
     * @throws ApiException null ,空, トークン期限切れ
     */
    public static void isValidAuthInfo(String cdluserid) throws ApiException {
        String sessionId = UserAuthentication.enrollWithCloudIDToken(cdluserid);
        if(sessionId == null) {
            // enrollに失敗したら、例外をスロー
            throw new ApiException(Response.Status.UNAUTHORIZED.getStatusCode(),
                    "illegal access token");
        }
        return;
    }

    /**
     * HLFCAClientオブジェクトを作成する
     * 既にHLFCAClientオブジェクトが生成済みの場合は、生成済みのものを返す
     *
     * @return HFCAClient
     * @throws Exception
     */
    public static HFCAClient createCAClient() throws Exception {
        if(caClient != null) {
            return caClient;
        }

        Properties caProperties = null;
        String caUrl = Config.get().cdlHLFCAURL();
        byte[] pemCert = Config.get().cdlHLFCAPem();

        // pemFile環境変数が設定されている場合、証明書情報を設定する
        if (pemCert != null && pemCert.length > 0) {
            caProperties = new Properties();
            // 証明書データ指定
            caProperties.put("pemBytes", pemCert);
            // ホスト名チェックは常に許可
            caProperties.setProperty("allowAllHostNames", "true");
        }

        CryptoSuite cryptoSuite = CryptoSuite.Factory.getCryptoSuite();
        caClient = HFCAClient.createNewInstance(caUrl, caProperties);
        caClient.setCryptoSuite(cryptoSuite);
        return caClient;
    }
}
