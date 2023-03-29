// EnrollApiServiceImpl.java COPYRIGHT Fujitsu Limited 2022
package com.fujitsu.cdlfrontserver.api.impl;

import com.fujitsu.cdlfrontserver.api.ApiResponseMessage;
import com.fujitsu.cdlfrontserver.api.Config;
import com.fujitsu.cdlfrontserver.api.LogLevel;
import com.fujitsu.cdlfrontserver.api.LogMsg;
import com.fujitsu.cdlfrontserver.api.ApiResponseMessage;
import com.fujitsu.cdlfrontserver.api.NotFoundException;
import com.fujitsu.cdlfrontserver.api.EnrollApiService;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import javax.validation.constraints.NotNull;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", date = "2020-12-01T20:30:14.057+09:00[Asia/Tokyo]")
public class EnrollApiServiceImpl extends EnrollApiService {

    // ログ
    private LogMsg log = LogMsg.getInstance();
    
    /**
     * 管理用セッションID取得
     * - userid, xCDLAuth をコンフィグの管理用設定値と比較し、
     * - 正しい場合はコンフィグから管理用セッションIDを返す。
     *
     * @param userid 認証するユーザーID
     * @param ID_Token ヘッダーのIDトークン
     * @param xCDLAuth 認証するユーザーのパスワード
     * @param securityContext セキュリティコンテキスト
     * @return 応答レスポンス
     * @throws NotFoundException ユーザが見つかない
     */
    @Override
    public Response enroll(String ID_Token, String userid, String xCDLAuth,
                           SecurityContext securityContext) throws NotFoundException {
    	
        // 引数のユーザーIDとパスワードをコンフィグの値と比較する
        if (userid.equals(Config.get().specialUserId()) && xCDLAuth.equals(Config.get().specialPassword())){

            // レスポンス200を返し、成功時の戻り値を表示させる
            return Response.ok()
                    .entity(new ApiResponseMessage(ApiResponseMessage.OK, "Special Session Id : " + Config.get().specialSessionId()))
                    .build();
        }
        // IDとパスワードが異なる場合
        return Response.status(400)
                    .entity(new ApiResponseMessage(ApiResponseMessage.ERROR, "different id or password"))
                    .build();
    }
}

