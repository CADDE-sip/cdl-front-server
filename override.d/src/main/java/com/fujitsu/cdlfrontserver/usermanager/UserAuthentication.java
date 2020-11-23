/*
    UserAuthenticationクラス

    COPYRIGHT FUJITSU LIMITED 2021
*/

package com.fujitsu.cdlfrontserver.usermanager;

import java.io.File;
import java.util.Properties;

import com.fujitsu.cdlfrontserver.api.Config;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;

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
}
