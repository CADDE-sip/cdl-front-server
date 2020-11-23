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

    // 認証バイパスモード
    private Boolean _enableBypassUserAuthentication = Boolean.TRUE;

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

        env = System.getenv( "CDL_IDTOKEN_EXPIRETIME" );
        if ( env != null && !env.equals( "" ) ) {
            this._idTokenExpireTime = Long.parseLong( env );
        }

        // 認証バイパスモード
        env = System.getenv( "CDL_ENABLE_BYPASS_USER_AUTHENTICATION" );
        if ( env != null && !env.equals( "" ) ) {
            this._enableBypassUserAuthentication = Boolean.valueOf( env );
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
        log.notice( "CDL_IDTOKEN_SECERT_KEY = " + this._idTokenSecretKey );
        log.notice( "CDL_IDTOKEN_EXPIRETIME = " + this._idTokenExpireTime );

        log.notice( "CDL_ENABLE_BYPASS_USER_AUTHENTICATION = " + this._enableBypassUserAuthentication );

        log.notice("CDL_EVENT_TYPE_DUPLICATE_CHECK = " + Arrays.toString(this._cdlEventTypeDuplicateCheck));

        log.notice("CDL_EVENT_TYPE_ADD_PREVIOUSEVENTS_CHECK = " + Arrays.toString(this._cdlEventTypeAddPreviouseventsCheck));
        log.notice("CDL_EVENT_TYPE_UPDATE_CHECK = " + Arrays.toString(this._cdlEventTypeUpdateCheck));
        log.notice("CDL_EVENT_TYPE_PUBLISH_CHECK = " + Arrays.toString(this._cdlEventTypePublishCheck));
    }

    public String channelName() {
        return _channelName;
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
     * @return _enableBypassUserAuthentication
     */
    public Boolean enableBypassUserAuthentication() {
        return _enableBypassUserAuthentication;
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
