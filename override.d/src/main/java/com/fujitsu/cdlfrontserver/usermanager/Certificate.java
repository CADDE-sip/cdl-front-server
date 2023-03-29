// Certificate.java COPYRIGHT Fujitsu Limited 2021 and FUJITSU LABORATORIES LTD. 2021

package com.fujitsu.cdlfrontserver.usermanager;

import java.io.IOException;
import java.io.StringReader;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Properties;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;

import com.fujitsu.cdlfrontserver.api.Config;
import com.fujitsu.cdlfrontserver.common.Utils;

public class Certificate {
    //ユーザの秘密鍵
    private ECPrivateKey userPrivateKey;
    //ユーザの公開鍵
    private ECPublicKey userPublicKey;
    //HLF管理者の秘密鍵
    private static ECPrivateKey adminPrivateKey;
    //HLF管理者の公開鍵
    private static ECPublicKey adminPublicKey;
    //HLF管理者のEnrollment
    private static Enrollment adminEnrollment;
    //PEM形式
    private static final String BEGIN_CERTIFICATE = "-----BEGIN CERTIFICATE-----\n";
    private static final String END_CERTIFICATE = "\n-----END CERTIFICATE-----";

    public Certificate(){
    }

    /**
     * ユーザ署名鍵を設定
     *
     * @param enrollment   ユーザのエンロールメントオブジェクト
     */
    public void setUserSignatureKey(Enrollment enrollment) throws Exception {
        // Enrollmentオブジェクトから、証明書・秘密鍵を取得。
        // 証明書から公開鍵を取り出す。
        userPrivateKey = (ECPrivateKey) enrollment.getKey();
        userPublicKey = getPublicKey(enrollment.getCert());
    }

    /**
     * HLF管理者署名鍵を設定
     *
     */
    public static void setAdminSignatureKey() throws Exception {
        // 設定済みかチェック
        if(adminPrivateKey != null && adminPublicKey != null){
            return;
        }

        // 管理者アカウントでエンロール
        HFCAClient caClient;

        caClient = Utils.createCAClient();
        adminEnrollment = caClient.enroll(Config.get().cdlHlfCAAdminAccount(), Config.get().cdlHlfCAAdminPassword());

        // Enrollmentオブジェクトから、証明書・秘密鍵を取得。
        // 証明書から公開鍵を取り出す。
        adminPrivateKey= (ECPrivateKey) adminEnrollment.getKey();
        adminPublicKey = getPublicKey(adminEnrollment.getCert());
    }

    /**
     * 取得したユーザ秘密鍵を返す
     *
     */
    public ECPrivateKey getUserPrivateKey() {
        return userPrivateKey;
    }

    /**
     * 取得したユーザ公開鍵を返す
     *
     */
    public ECPublicKey getUserPublicKey() {
        return userPublicKey;
    }

    /**
     * 取得したHLF管理者秘密鍵を返す
     *
     */
    public static ECPrivateKey getAdminPrivateKey() {
        return adminPrivateKey;
    }

    /**
     * 取得したHLF管理者公開鍵を返す
     *
     */
    public static ECPublicKey getAdminPublicKey() {
        return adminPublicKey;
    }

    /**
     * 取得したHLF管理者のEnrollmentを返す
     *
     */
    public static Enrollment getAdminEnrollment() {
        return adminEnrollment;
    }

    /**
     * 証明書から公開鍵を取得
     *
     * @param certStr 証明書（pem形式）の文字列
     * @return 公開鍵
     */
    private static ECPublicKey getPublicKey(String certStr) throws Exception{
        // X509証明書オブジェクトに変換
        X509Certificate cert = convertToX509(certStr);
        // オブジェクトから公開鍵を取得
        PublicKey publicKey = cert.getPublicKey();
        return (ECPublicKey) publicKey;
    }

    /**
     * X509証明書オブジェクトに変換
     *
     * @param cert 証明書（pem形式）の文字列
     * @return X509証明書オブジェクト
     */
    private static X509Certificate convertToX509(String cert) throws IOException, CertificateException {
        StringReader sr = new StringReader(cert);
        PEMParser pp = new PEMParser(sr);
        return new JcaX509CertificateConverter().getCertificate((X509CertificateHolder) pp.readObject());
    }

    /**
     * X509CertificateオブジェクトをPEM形式に変換
     *
     * @param certificate X509Certificateオブジェクト
     * @return PEM形式
     * @throws CertificateEncodingException
     */
    public static String convertToPem(X509Certificate certificate) throws CertificateEncodingException {
        Encoder encoder = Base64.getEncoder();
        byte[] derCertificate = certificate.getEncoded();
        return BEGIN_CERTIFICATE + new String(encoder.encode(derCertificate)) + END_CERTIFICATE;
    }
}

