/*
COPYRIGHT Fujitsu Limited 2021
*/

package com.fujitsu.cdlfrontserver.usermanager;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Properties;

import javax.ws.rs.core.Response.Status;

import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.Attribute;
import org.hyperledger.fabric_ca.sdk.exception.AffiliationException;
import org.hyperledger.fabric_ca.sdk.exception.IdentityException;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.HFCAIdentity;
import org.hyperledger.fabric_ca.sdk.HFCAAffiliation;
import org.hyperledger.fabric.sdk.Enrollment;

import com.fujitsu.cdlfrontserver.api.ApiException;
import com.fujitsu.cdlfrontserver.api.Config;
import com.fujitsu.cdlfrontserver.common.Utils;
import com.fujitsu.cdlfrontserver.model.CDLUserInfo;

public class UserManager {
    /**
     * @author Nishimaki
     */
    private UserManager() {
    }

    /**
     * Fabric CAにユーザを登録する
     * - 操作者はadmin権限が必須
     * - 事前にaffiliationの登録が必須
     *
     * @param adminContext コンテキスト情報
     * @param request 登録するユーザ情報
     * @throws Exception 登録失敗
     */
    public static void registUser(UserContext adminContext, CDLUserInfo request) throws Exception {
        HFCAClient caClient = null;

        // ユーザ情報取得（ユーザーID以外はコンフィグファイルから設定）
        String targetUserID = request.getCdluserid();
        String targetOrg = request.getCdlorganization();
        String targetRole = request.getCdlrole();
        String targetPassword = request.getCdlpassword();
  
        HFCAIdentity userIdentity = null;
        int userReadStatus = -1;
        try {
            // ユーザ情報のnull,空チェック
            if(Utils.isEmpty(targetUserID)){
		throw new ApiException(Status.BAD_REQUEST.getStatusCode(), "cdluserid is not specified or is empty");
            }
            // 組織は空文字を許容する
            if(targetOrg == null){
                throw new ApiException(Status.BAD_REQUEST.getStatusCode(), "cdlorganization is not specified or is empty");
            }
            // ロールは空文字を許容する
            if(targetRole == null){
                throw new ApiException(Status.BAD_REQUEST.getStatusCode(), "cdlrole is not specified");
            }
            if(Utils.isEmpty(targetPassword)){
                throw new ApiException(Status.BAD_REQUEST.getStatusCode(), "cdlpassword is not specified or is empty");
            }
            caClient = Utils.createCAClient();
	    
            // admin権限で検索
            userIdentity = caClient.newHFCAIdentity(targetUserID);
	    userReadStatus = userIdentity.read(adminContext);

            // 既に登録済みの場合、登録失敗
            throw new ApiException(Status.BAD_REQUEST.getStatusCode(), "User already exists. userid=" + targetUserID);

	} catch (IdentityException err) {
            // ユーザ情報取得に失敗した場合はIdentityExceptionが発生する

            //  ユーザが未登録であれば、FabricCAに対してユーザ登録
            // 　TODO ユーザが見つからない場合、
            // 　エラーメッセージが"[Code: 404] - Error while getting user"で開始
            //   (userReadStatusは404が期待値だが、-1のままで利用不可)
            if(null != err.getMessage()
                    && err.getMessage().startsWith("[Code: 404] - Error while getting user")) {
                System.out.println("User creatting....");

                // 組織としてaffiliation設定
                userIdentity.setAffiliation(targetOrg);
                // ロール設定
                userIdentity.setAttributes(setRoles(targetRole));
                // パスワード設定
                userIdentity.setSecret(targetPassword);
                // ユーザ権限でaffliationを取得
                int affiReadStatus = -1;
                
		try {
                    HFCAAffiliation hfcaff = caClient.newHFCAAffiliation(targetOrg);
                    affiReadStatus = hfcaff.read(adminContext);
                }catch (AffiliationException affierr) {
                    // affiliation取得に失敗した場合、登録失敗
                    System.out.println("The specified organization does not exit.organization=" + targetOrg);
                    throw new ApiException(Status.BAD_REQUEST.getStatusCode(),
                            "The specified organization does not exit.organization=" + targetOrg);
                }
		
                // ユーザ登録
                int createStatus = userIdentity.create(adminContext);
                if(201 != createStatus) {
                    System.out.println("Creatting User failed.statuscode=" + createStatus);
                    throw new ApiException(Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                            "Creatting User failed.statuscode=" + createStatus);
                }
                System.out.println("Creatting User succeed");
            } else {
                // ユーザ検索時、404以外のエラーが発生した場合、予期せぬエラー
                System.out.println("Getting User failed.errmsg=" + err.getMessage());
                throw new ApiException(Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                        "Getting User failed.");
            }
        } catch (Exception e) {
            // ユーザ取得時、IdentityException以外の例外が発生した場合は例外をスロー
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Fabric CAに登録したユーザを取得する
     * - 操作者はadmin権限が必須
     *
     * @param adminContext コンテキスト情報
     * @param userid ユーザID
     * @throws Exception 登録失敗
     */
    public static void getUser(UserContext adminContext, String userid) throws Exception {
        HFCAClient caClient = null;

        HFCAIdentity userIdentity = null;
        int userReadStatus = -1;
        try {
            caClient = Utils.createCAClient();
            // ユーザ検索
            userIdentity = caClient.newHFCAIdentity(userid);
            userReadStatus = userIdentity.read(adminContext);
            
            String org = userIdentity.getAffiliation();
    
        } catch (IdentityException err) {
            // ユーザ情報取得に失敗した場合はIdentityExceptionが発生する
            //  ユーザが未登録であれば、取得失敗
            // 　TODO ユーザが見つからない場合、エラーメッセージが"[Code: 404] - Error while getting user"で開始
            //   (userReadStatusは404が期待値だが、-1のままで利用不可)
            //   TODO アクセス権限がない場合、エラーメッセージが"[Code: 403] - Error while getting user"で開始
            //   (userReadStatusは404が期待値だが、-1のままで利用不可)
            if(null != err.getMessage()
                    && err.getMessage().startsWith("[Code: 404] - Error while getting user")) {
                System.out.println("The specified user does not exist.errmsg=" + err.getMessage());
                throw new ApiException(Status.NOT_FOUND.getStatusCode(),
                        "The specified user does not exist.");
            } else if (null != err.getMessage()
                    && err.getMessage().startsWith("[Code: 403] - Error while getting user")) {
                System.out.println("Authorization error.errmsg=" + err.getMessage());
                throw new ApiException(Status.FORBIDDEN.getStatusCode(),
                        "Authorization error.");
            } else {
                    // 404・403エラー以外の時、予期せぬエラー
                    System.out.println("Getting User failed.errmsg=" + err.getMessage());
                    throw new ApiException(Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                            "Getting User failed.");
            }
        } catch (Exception e) {
            // ユーザ取得時、IdentityException以外の例外が発生した場合は例外をスロー
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * ロール情報を作成
     * Fabric特有のロール情報を付与
     *
     * @param roles ロール
     * @return Attribute
     */
    private static HashSet<Attribute> setRoles(String roles) {
        HashSet<Attribute> attrSet = new HashSet<Attribute>();
        // 管理権限の場合、affiliation権限を付与
        String[] splitRoles = roles.split(",");
        boolean adminFlag = false;
        for(String role:splitRoles) {
            if(Config.get().adminRole().equals(role)) {
                adminFlag = true;
                break;
            }
        }
        // HLFのuserロールは必須
        Attribute roleAttr = new Attribute("hf.Registrar.Roles", roles + ",user");
        Attribute revokeAttr = new Attribute("hf.Revoker", "true");
        attrSet.add(roleAttr);
        attrSet.add(revokeAttr);
        // ユーザ操作の管理権限をもつ場合、HLFのaffiliation権限を付与
        if(adminFlag) {
            Attribute affiAttr = new Attribute("hf.AffiliationMgr", "true");
            attrSet.add(affiAttr);
        }
        // ユーザ属性の設定権限を付与
        // user_adminを保有しないユーザ自身の属性変更を許可する
        Attribute attr = new Attribute("hf.Registrar.Attributes", "*");
        attrSet.add(attr);
        return attrSet;
    }
}
