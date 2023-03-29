/*
    UserAuthenticationクラス

    COPYRIGHT FUJITSU LIMITED 2021
*/

package com.fujitsu.cdlfrontserver.usermanager;

import java.io.File;
import java.util.Properties;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Properties;

import com.fujitsu.cdlfrontserver.common.Utils;
import com.fujitsu.cdlfrontserver.api.Config;
import com.fujitsu.cdlfrontserver.api.ApiException;
import com.fujitsu.cdlfrontserver.model.CDLUserInfo;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.HFCAIdentity;

import org.hyperledger.fabric_ca.sdk.Attribute;

public class UserAuthentication {

    // 証明書（バイナリ）指定時のプロパティキー
    private static final String PEM_BYTES = "pemBytes";

    // 証明書パス指定時のプロパティキー
    private static final String PEM_PATH = "pemFile";

    // 証明書のホスト名チェックのプロパティキー
    private static final String PEM_HOSTNAME_CHECK= "allowAllHostNames";

    /**
     * @author Nishimaki
     */
    private UserAuthentication() {
    }

    /**
     * @param name   ユーザ指定のユーザID
     * @param secret ユーザ指定のパスワード
     * @return セッションID
     */
    public static String enroll(String name, String secret) {
        String caUrl = Config.get().cdlHLFCAURL();
        byte[] pemCert = Config.get().cdlHLFCAPem();
        String pemPath = Config.get().cdlHLFCAPemPath();
        String sessionId;
        Properties caProperties = null;
        HFCAClient caClient;
        Enrollment enrollment;
        CryptoSuite cryptoSuite;

        try {
            caProperties = new Properties();

            // pemPathが設定されている場合、証明書パス（絶対パス）を設定
            if (pemPath != null && pemPath.length() > 0) {
                // 証明書パスを絶対パスで指定
                caProperties.put(PEM_PATH, new File(pemPath).getAbsolutePath());
                // ホスト名チェックは常に許可
                caProperties.setProperty(PEM_HOSTNAME_CHECK, "true");
            }

            // pemCertが設定されている場合、証明書バイナリを設定
            if (pemCert != null && pemCert.length > 0) {
                // 証明書(バイナリ）を指定
                caProperties.put(PEM_BYTES, pemCert);
                // ホスト名チェックは常に許可
                caProperties.setProperty(PEM_HOSTNAME_CHECK, "true");
            }

            cryptoSuite = CryptoSuite.Factory.getCryptoSuite();
            caClient = HFCAClient.createNewInstance(caUrl, caProperties);
            caClient.setCryptoSuite(cryptoSuite);

            // Enroll user
            // CDLのユーザ登録API以外からもユーザ登録がある想定での暫定処置
            // 本来は下記のようにハッシュ化する（生のパスワードで扱わない）
            // enrollment = caClient.enroll(name, DigestUtils.sha256Hex(secret));
            enrollment = caClient.enroll(name, secret);
	    System.out.println("Enrollment: " + enrollment.toString());
            System.out.println("Cert: " + enrollment.getCert());
            System.out.println("Private Key: " + enrollment.getKey());

            // セッションIDを発行
            UserContext context = new UserContext();
            context.setName(name);
            // CDLのユーザ登録API以外からもユーザ登録がある想定での暫定処置
            // 本来は下記のようにハッシュ化する（生のパスワードで扱わない）
            // context.setHashSecret(DigestUtils.sha256Hex(secret));
            context.setHashSecret(secret);
            context.setEnrollment(enrollment);
            context.setOrg(Config.get().cdlHlfOrganization());
            context.setAffiliation(Config.get().cdlRole());
            context.setMspId(Config.get().cdlHlfMSPId());

            sessionId = SessionId.generate(context);

            System.out.println(sessionId);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return sessionId;
    }

    /**
     * ユーザーID情報をもとにenrollする
     * - ユーザが未登録の場合は、ユーザを登録する
     *
     * @param userid ユーザーID
     * @return セッションID
     */
    public static String enrollWithCloudIDToken(String userid) {

        String sessionId = null;
        UserContext adminContext = null;

        try {
            // Fabric adminユーザでenroll
            Certificate.setAdminSignatureKey();

            adminContext = new UserContext();
            adminContext.setEnrollment(Certificate.getAdminEnrollment());

            // ユーザの有無をチェック(取得できれなければ例外スロー）
            UserManager.getUser(adminContext, userid);

            // ユーザが登録済みであれば、enroll
            // メモリ上でに認証情報が展開される
            sessionId = UserAuthentication.enroll(userid, Config.get().registerUserPassword());
            if (sessionId == null) {
                // ユーザIDとパスワードが誤っていることはありえないが
                // 万が一誤っていた場合はnullを返す
                System.out.println("Enrolling failed.");
                return null;
            }
            return sessionId;
        } catch (ApiException apierr) {
            // 利用者取得に失敗した場合
            // ユーザ登録
            try {
                CDLUserInfo userInfo = new CDLUserInfo();
                userInfo.setCdluserid(userid);
                userInfo.setCdlpassword(Config.get().registerUserPassword());
                userInfo.setCdlorganization(Config.get().registerUserOrg());
                userInfo.setCdlrole(Config.get().registerUserRole());
                UserManager.registUser(adminContext, userInfo);
                System.out.println("Creatting user succeed. userid=" + userid);
            } catch (Exception registerr) {
                // ユーザ登録に失敗した場合は実行不能
                registerr.printStackTrace();
                return null;
            }

            // enroll
            // (メモリ上でに認証情報が展開される)
            try {
                sessionId = UserAuthentication.enroll(userid, Config.get().registerUserPassword());
                if (sessionId == null) {
                    // ユーザIDとパスワードが誤っていることはありえないが
                    // 万が一誤っていた場合はnullを返す
                    System.out.println("Enrolling failed.");
                    return null;
                }
            } catch (Exception enrollerr) {
                // enrollに失敗した場合は実行不能
                enrollerr.printStackTrace();
                return null;
            }
            return sessionId;
        } catch (Exception e) {
            // 例外が発生した場合は例外をスロー
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ロール一覧取得
     *
     * @param identity HFCAIdentityオブジェクト
     * @return hf.Registrar.Roles属性のみ
     */
    private static Set<String> getRoles(HFCAIdentity identity) {
        Set<String> attrs = new HashSet<String>();
        Collection<Attribute> attributes = identity.getAttributes();
        if (attributes != null && !attributes.isEmpty()) {
            for (Iterator ite = attributes.iterator(); ite.hasNext();) {
                Attribute attr = (Attribute) ite.next();
                // hf.Registrar.Roles属性のみ設定
                if("hf.Registrar.Roles".equals(attr.getName())) {
                    attrs.add(attr.getValue());
                    break;
                }
            }
        }
        return attrs;
    }

}
