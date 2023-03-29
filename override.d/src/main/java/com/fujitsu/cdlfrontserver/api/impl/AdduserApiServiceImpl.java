// AdduserApiServiceImpl.java COPYRIGHT Fujitsu Limited 2022
package com.fujitsu.cdlfrontserver.api.impl;

import com.fujitsu.cdlfrontserver.api.ApiException;
import com.fujitsu.cdlfrontserver.api.ApiResponseMessage;
import com.fujitsu.cdlfrontserver.api.LogLevel;
import com.fujitsu.cdlfrontserver.api.LogMsg;
import com.fujitsu.cdlfrontserver.api.NotFoundException;
import com.fujitsu.cdlfrontserver.api.AdduserApiService;
import com.fujitsu.cdlfrontserver.common.Utils;
import com.fujitsu.cdlfrontserver.model.CDLUserInfo;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import javax.validation.constraints.NotNull;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", date = "2020-12-01T20:30:14.057+09:00[Asia/Tokyo]")
public class AdduserApiServiceImpl extends AdduserApiService {

    // ログ
    private LogMsg log = LogMsg.getInstance();
    
    /**
     * ユーザ登録
     * - user_adminロールを保有するユーザが操作した場合、自組織のユーザのみ作成可能
     *
     * @param ID_Token ヘッダーのIDトークン
     * @param xCDLSessionId セッション情報
     * @param requestBody リクエスト情報
     * @param securityContext セキュリティコンテキスト
     * @return 応答レスポンス
     * @throws NotFoundException ユーザが見つかない
     */
    @Override
    public Response adduser(String ID_Token, String xCDLSessionId,
                            CDLUserInfo requestBody, SecurityContext securityContext) throws NotFoundException {
    	
        try {            
            // リクエストボディのユーザーIDを登録する
            Utils.isValidAuthInfo(requestBody.getCdluserid());
            return Response.ok().build();

        } catch (ApiException e) {
            // ユーザ登録に失敗した場合
            return Response.status(e.getCode()).entity(e.getMessage()).build();

        } catch (Throwable t) {
            String logmsg = log.println(LogLevel.ERROR, t);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity(new ApiResponseMessage(ApiResponseMessage.ERROR, "Internal Server Error : " + logmsg))
                    .build();
        }
    }
}
