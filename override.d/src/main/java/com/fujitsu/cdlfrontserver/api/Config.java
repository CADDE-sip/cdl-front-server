/*
    Configクラス

    COPYRIGHT FUJITSU LIMITED 2021
*/

package com.fujitsu.cdlfrontserver.api;

import org.hyperledger.fabric.sdk.NetworkConfig;
import org.hyperledger.fabric.sdk.exception.NetworkConfigurationException;

import javax.json.JsonObject;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Config {

    private LogMsg log = LogMsg.getInstance();

    private static final Config Instance = new Config();

    public static Config get() {
        return Instance;
    }

    private String _channelName = "cdlchannel";
    private String _cdlChainCodeName = "cdl-chaincode";

    private String _cdlHlfNetworkConfigPath = "connection.yaml";

    private String _cdlHlfOrganization = "org01";
    private String _cdlHlfMSPId = "org01MSP";

    private String _cdlRole = "access_admin,roleA,roleB";

    private String _cdlHlfCAAdminAccount = "admin";
    private String _cdlHlfCAAdminPassword = "fjpassword1";

    private byte[] _cdlHLFCAPem = null;
    private String _cdlHLFCAPemPath = null;
    private String _cdlHLFCAHost = "ca.org01.cdl.com";
    private String _cdlHLFCAURL = "http://ca02.common.fujitsu.com:10011";

    private String _idTokenSecretKey = "7bf089bf9aa47b393e748b5f5502cc7e35b0a4826d93ce8456f7e7cc70df73ca";
    private long _idTokenExpireTime = 600000;
    // IDトークン・JWSデジタル署名の発行者名
    private String _tokenIssuer = "fujitsu";

    // 管理者権限のロール
    private String _adminRole = "user_admin";

    // 認証バイパスモード
    private Boolean _enableBypassUserAuthentication = Boolean.TRUE;

    // 認証認可機能を使用するかのフラグ
    private Boolean _enableCeatificationAuthentication = Boolean.TRUE;

    // 認証認可機能で使用するclient_id、Secret
    private String _clientId = "provenance";
    private String _clientSecret = "iUQTo16IQysrWXvO1X2lGajOHYk21L46";

    // 認証APIのURL
    private String _portalUrl = "https://auth01.saas.data-linkage.jp/cadde/api/v4/token/introspect";

    // 管理用セッションID取得で使用するユーザーID、パスワード
    private String _specialUserId = "specialuser";
    private String _specialPassword = "specialpass";

    // 管理用セッションID取得成功時の戻り値
    private String _specialSessionId = "F7C89BFA22C8320715D4413B57A25D3A";

    // ユーザー登録時に使用する固定値
    private String _registerUserOrg = "org1";
    private String _registerUserRole = "role1";
    private String _registerUserPassword = "password1";

    // cdleventtypeによるイベント重複チェック
    // 環境変数定義を省略した場合は、従来仕様通り、イベント登録を行う
    private String[] _cdlEventTypeDuplicateCheck = {};

    private String[] _cdlEventTypeAddPreviouseventsCheck = {};
    private String[] _cdlEventTypeUpdateCheck = {};
    private String[] _cdlEventTypePublishCheck = {};
    

    private Config() {
        String env;

        env = System.getenv( "CHANNEL_NAME" );
        if ( env != null && !env.equals( "" ) ) {
            this._channelName = env;
        }
        
	// 認証機能で使用するclientId、Secret
        env = System.getenv( "CLIENT_ID" );
        if ( env != null && !env.equals( "" ) ) {
            this._clientId = env;
        }

	env = System.getenv( "CLIENT_SECRET" );
        if ( env != null && !env.equals( "" ) ) {
            this._clientSecret = env;
        }

        env = System.getenv( "CDL_CHAINCODE_NAME" );
        if ( env != null && !env.equals( "" ) ) {
            this._cdlChainCodeName = env;
        }

        env = System.getenv( "CDL_HLF_NETWORKCONFIGPATH" );
        if ( env != null && !env.equals( "" ) ) {
            this._cdlHlfNetworkConfigPath = env;
        }

        env = System.getenv( "CDL_IDTOKEN_SECRET_KEY" );
        if ( env != null && !env.equals( "" ) ) {
            this._idTokenSecretKey = env;
        }

        // トークン発行者名
        env = System.getenv( "CDL_TOKEN_ISSUER" );
        if ( env != null && !env.equals( "" ) ) {
            this._tokenIssuer = env;
        }

        // 管理者権限のロール
        env = System.getenv( "CDL_ADMIN_ROLE" );
        if ( env != null && !env.equals( "" ) ) {
            this._adminRole = env;
        }

        env = System.getenv( "CDL_IDTOKEN_EXPIRETIME" );
        if ( env != null && !env.equals( "" ) ) {
            this._idTokenExpireTime = Long.parseLong( env );
        }

        // 認証バイパスモード
        env = System.getenv( "CDL_ENABLE_BYPASS_USER_AUTHENTICATION" );
        if ( env != null && !env.equals( "" ) ) {
            this._enableBypassUserAuthentication = Boolean.valueOf( env );
        }

        // 認証認可機能使用の判定
        env = System.getenv( "CDL_ENABLE_CERTIFICATION_AUTHENTICATION" );
        if ( env != null && !env.equals( "" ) ) {
            this._enableCeatificationAuthentication = Boolean.valueOf( env );
        }

        // 認証APIのURL
        env = System.getenv( "PORTAL_URL" );
        if ( env != null && !env.equals( "" ) ) {
            this._portalUrl = env;
        }

        // 管理用セッションID取得で使用するユーザーID、パスワード
        env = System.getenv( "SPECIAL_USER_ID" );
        if ( env != null && !env.equals( "" ) ) {
            this._specialUserId = env;
        }

        env = System.getenv( "SPECIAL_PASSWORD" );
        if ( env != null && !env.equals( "" ) ) {
            this._specialPassword = env;
        }

        // 管理用セッションID取得成功時の戻り値
        env = System.getenv( "SPECIAL_SESSION_ID" );
        if ( env != null && !env.equals( "" ) ) {
            this._specialSessionId = env;
        } 

        // ユーザー登録時に使用する固定値
        env = System.getenv( "REGISTER_USER_ORG" );
        if ( env != null && !env.equals( "" ) ) {
            this._registerUserOrg = env;
        }

        env = System.getenv( "REGISTER_USER_ROLE" );
        if ( env != null && !env.equals( "" ) ) {
            this._registerUserRole = env;
        }

        env = System.getenv( "REGISTER_USER_PASSWORD" );
        if ( env != null && !env.equals( "" ) ) {
            this._registerUserPassword = env;
        }

        env = System.getenv("CDL_EVENT_TYPE_DUPLICATE_CHECK");
        if (env != null && !env.equals("")) {
            this._cdlEventTypeDuplicateCheck = env.split(",");
        }

        env = System.getenv("CDL_EVENT_TYPE_ADD_PREVIOUSEVENTS_CHECK");
        if (env != null && !env.equals("")) {
            this._cdlEventTypeAddPreviouseventsCheck = env.split(",");
        }

        env = System.getenv("CDL_EVENT_TYPE_UPDATE_CHECK");
        if (env != null && !env.equals("")) {
            this._cdlEventTypeUpdateCheck = env.split(",");
        }

        env = System.getenv("CDL_EVENT_TYPE_PUBLISH_CHECK");
        if (env != null && !env.equals("")) {
            this._cdlEventTypePublishCheck = env.split(",");
        }

        // connection.yamlから必要な情報を取得
        try {
            File configFile = new File(this._cdlHlfNetworkConfigPath);
            NetworkConfig networkConfig = NetworkConfig.fromYamlFile(configFile);

            // クライアントの組織名を取得
            NetworkConfig.OrgInfo clientOrg = networkConfig.getClientOrganization();
            if(clientOrg != null) {
                this._cdlHlfOrganization = clientOrg.getName();
            }

            // クライアント所属の組織に対応するMSPIDを取得
            // あわせて、CA情報も取得
            // connection.yamlに指定したユーザは管理ユーザであり
            // かつ認証バイバスモードの場合、固定ユーザとしても利用する
            NetworkConfig.OrgInfo orgInfo = networkConfig.getOrganizationInfo(this._cdlHlfOrganization);
            if(orgInfo != null) {
                this._cdlHlfMSPId = orgInfo.getMspId();

                List<NetworkConfig.CAInfo> caInfoList = orgInfo.getCertificateAuthorities();
                // 各組織に必ずCAは1つの前提
                NetworkConfig.CAInfo caInfo = caInfoList.get(0);
                if(caInfo != null) {
                    this._cdlHLFCAURL = caInfo.getUrl();
                    this._cdlHLFCAHost = caInfo.getName();
                    // PEMの読み込み (優先順：fabric sdk同様、パス指定⇒埋め込み型)
                    this._cdlHLFCAPemPath = (String)caInfo.getProperties().get("pemFile"); //パス指定
                    if(this._cdlHLFCAPemPath == null) {
                        this._cdlHLFCAPem = (byte[])caInfo.getProperties().get("pemBytes"); //埋め込み型
                    }
                    Collection<NetworkConfig.UserInfo> userInfoCol = caInfo.getRegistrars();
                    // 各組織に必ずユーザは1人の想定
                    for (Iterator userIter = userInfoCol.iterator(); userIter.hasNext(); ) {
                        NetworkConfig.UserInfo userInfo = (NetworkConfig.UserInfo) userIter.next();
                        this._cdlHlfCAAdminAccount = userInfo.getName();
                        this._cdlHlfCAAdminPassword = userInfo.getEnrollSecret();
                    }
                }
            }
        } catch (IOException err) {
            // 例外が発生した時点で処理は続行不能だが
            // スタックトレースを出力し、そのまま継続
            // (Configクラスとしては処理続行を判断しない）
            log.error("Could not open file. " + err.getMessage());
        } catch (NetworkConfigurationException err) {
            // 例外が発生した時点で処理は続行不能だが
            // スタックトレースを出力し、そのまま継続
            // (Configクラスとしては処理続行を判断しない）
            log.error("Could not open file. " + err.getMessage());
        }

        log.notice( "CDL Configurations :" );
        log.notice( "CHANNEL_NAME = " + this._channelName );
        log.notice( "CLIENT_ID = " + this._clientId );
        log.notice( "CLIENT_SECRET = " + this._clientSecret );
        log.notice( "CDL_CHAINCODE_NAME = " + this._cdlChainCodeName );
        log.notice( "CDL_HLF_NETWORKCONFIGPATH = " + this._cdlHlfNetworkConfigPath );
        log.notice( "CDL_HLF_ORGANIZATION = " + this._cdlHlfOrganization );
        log.notice( "CDL_HLF_MSPID = " + this._cdlHlfMSPId );
        log.notice( "CDL_HLF_CA_ADMIN_ACCOUNT = " + this._cdlHlfCAAdminAccount );
        log.notice( "CDL_HLF_CA_ADMIN_PASSWORD = " + this._cdlHlfCAAdminPassword );
        log.notice( "CDL_HLF_CA_PEMFILE SIZE = " + (this._cdlHLFCAPem == null ? 0 : this._cdlHLFCAPem.length) );
        log.notice( "CDL_HLF_CA_PEMFILE PATH = " + this._cdlHLFCAPemPath );
        log.notice( "CDL_HLF_CA_HOST = " + this._cdlHLFCAHost );
        log.notice( "CDL_HLF_CA_URL = " + this._cdlHLFCAURL );

        log.notice( "CDL_ADMIN_ROLE = " + this._adminRole );

        log.notice( "CDL_IDTOKEN_SECERT_KEY = " + this._idTokenSecretKey );
        log.notice( "CDL_IDTOKEN_EXPIRETIME = " + this._idTokenExpireTime );
        log.notice( "CDL_TOKEN_ISSUER = " + this._tokenIssuer );

        log.notice( "CDL_ENABLE_BYPASS_USER_AUTHENTICATION = " + this._enableBypassUserAuthentication );
        log.notice( "CDL_ENABLE_CERTIFICATION_AUTHENTICATION = " + this._enableCeatificationAuthentication );

        log.notice( "PORTAL_URL = " + this._portalUrl );
        log.notice( "SPECIAL_USER_ID = " + this._specialUserId );
        log.notice( "SPECIAL_PASSWORD = " + this._specialPassword );
        log.notice( "SPECIAL_SESSION_ID = " + this._specialSessionId );

        log.notice( "REGISTER_USER_ORG = " + this._registerUserOrg );
        log.notice( "REGISTER_USER_ROLE = " + this._registerUserRole );
        log.notice( "REGISTER_USER_PASSWORD = " + this._registerUserPassword );

        log.notice("CDL_EVENT_TYPE_DUPLICATE_CHECK = " + Arrays.toString(this._cdlEventTypeDuplicateCheck));

        log.notice("CDL_EVENT_TYPE_ADD_PREVIOUSEVENTS_CHECK = " + Arrays.toString(this._cdlEventTypeAddPreviouseventsCheck));
        log.notice("CDL_EVENT_TYPE_UPDATE_CHECK = " + Arrays.toString(this._cdlEventTypeUpdateCheck));
        log.notice("CDL_EVENT_TYPE_PUBLISH_CHECK = " + Arrays.toString(this._cdlEventTypePublishCheck));
    }

    public String channelName() {
        return _channelName;
    }
    
    public String clientId() {
        return _clientId;
    }

    public String clientSecret() {
        return _clientSecret;
    }

    public String cdlChainCodeName() {
        return _cdlChainCodeName;
    }

    public String cdlHlfNetworkConfigPath() {
        return _cdlHlfNetworkConfigPath;
    }

    public String cdlHlfOrganization() {
        return _cdlHlfOrganization;
    }

    public String cdlHlfMSPId() {
        return _cdlHlfMSPId;
    }

    public String cdlRole() {
        return _cdlRole;
    }

    public String cdlHlfCAAdminAccount() {
        return _cdlHlfCAAdminAccount;
    }

    public String cdlHlfCAAdminPassword() {
        return _cdlHlfCAAdminPassword;
    }

    public byte[] cdlHLFCAPem() {
        return _cdlHLFCAPem;
    }

    public String cdlHLFCAPemPath() {
        return _cdlHLFCAPemPath;
    }

    public String cdlHLFCAHost() {
        return _cdlHLFCAHost;
    }

    public String cdlHLFCAURL() {
        return _cdlHLFCAURL;
    }

    public String idTokenSecretKey() {
        return _idTokenSecretKey;
    }

    public long idTokenExpireTime() {
        return _idTokenExpireTime;
    }

    /**
     * @return _tokenIssuer
     */
    public String tokenIssuer() {
        return _tokenIssuer;
    }

    /**
     * @return _adminRole
     */
    public String adminRole() {
        return _adminRole;
    }

    /**
     * @return _enableBypassUserAuthentication
     */
    public Boolean enableBypassUserAuthentication() {
        return _enableBypassUserAuthentication;
    }

    /**
     * @return _enableBypassUserAuthentication
     */
    public Boolean enableCeatificationAuthentication() {
        return _enableCeatificationAuthentication;
    }

    /**
     * @return _portalUrl
     */
    public String portalUrl() {
        return _portalUrl;
    }

    /**
     * @return _specialUserId
     */
    public String specialUserId() {
        return _specialUserId;
    }

    /**
     * @return _specialPassword
     */
    public String specialPassword() {
        return _specialPassword;
    }

    /**
     * @return _specialSessionId
     */
    public String specialSessionId() {
        return _specialSessionId;
    }

    /**
     * @return _registerUserOrg
     */
    public String registerUserOrg() {
        return _registerUserOrg;
    }

    /**
     * @return _registerUserRole
     */
    public String registerUserRole() {
        return _registerUserRole;
    }

    /**
     * @return _registerUserPassword
     */
    public String registerUserPassword() {
        return _registerUserPassword;
    }

    /**
     * @return _cdlEventTypeDuplicateCheck
     */
    public String[] cdlEventTypeDuplicateCheck() {
        return _cdlEventTypeDuplicateCheck;
    }

    /**
     * @return _cdlEventTypeAddPreviouseventsCheck
     */
    public String[] cdlEventTypeAddPreviouseventsCheck() {
        return _cdlEventTypeAddPreviouseventsCheck;
    }

    /**
     * @return _cdlEventTypeUpdateCheck
     */
    public String[] cdlEventTypeUpdateCheck() {
        return _cdlEventTypeUpdateCheck;
    }

    /**
     * @return _cdlEventTypePublisgCheck
     */
    public String[] cdlEventTypePublisgCheck() {
        return _cdlEventTypePublishCheck;
    }
}

