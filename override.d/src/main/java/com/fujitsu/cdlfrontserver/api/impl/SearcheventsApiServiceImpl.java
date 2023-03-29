/*
    SearcheventsApiServiceImplクラス

    COPYRIGHT FUJITSU LIMITED 2021
*/

package com.fujitsu.cdlfrontserver.api.impl;

import javax.validation.constraints.NotNull;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.hyperledger.fabric.gateway.Contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fujitsu.cdlfrontserver.api.ApiException;
import com.fujitsu.cdlfrontserver.api.ApiResponseMessage;
import com.fujitsu.cdlfrontserver.api.NotFoundException;
import com.fujitsu.cdlfrontserver.api.SearcheventsApiService;
import com.fujitsu.cdlfrontserver.api.LogLevel;
import com.fujitsu.cdlfrontserver.api.LogMsg;
import com.fujitsu.cdlfrontserver.model.InlineObject;
import com.fujitsu.cdlfrontserver.usermanager.HLFGatewayManager;

import com.fujitsu.cdlfrontserver.api.Config;
import com.fujitsu.cdlfrontserver.api.GetAuthenticationToken;
import java.io.IOException;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", date = "2020-12-01T20:30:14.057+09:00[Asia/Tokyo]")
public class SearcheventsApiServiceImpl extends SearcheventsApiService {

    private LogMsg log = LogMsg.getInstance();

    @Override
    public Response searchevents(String Authorization, String ID_Token, InlineObject inlineObject,
            SecurityContext securityContext) throws NotFoundException {

        // 認証認可機能を行うかどうかをコンフィグファイルから判定
        if (Config.get().enableCeatificationAuthentication()) {
            try {
                Integer responseCode = null;

                // AuthorizationとID_Tokenがnullの場合はエラーを返す
                if (Authorization == null) {
                    if (ID_Token == null) {
                        return Response.status(Response.Status.BAD_REQUEST).entity(new ApiResponseMessage(ApiResponseMessage.ERROR,
                                 Response.Status.BAD_REQUEST.getReasonPhrase() + " : ID_Token is null")).build();
                    }
                    // Authorizationが無い場合はID_Tokenで認証を行う
                    responseCode = GetAuthenticationToken.execute(Config.get().clientId(), Config.get().clientSecret(), ID_Token);
                } else {
                    // clientId、clientSecret、ヘッダ情報のAuthorizationを認証APIに送る
                    responseCode = GetAuthenticationToken.execute(Config.get().clientId(), Config.get().clientSecret(), Authorization);
                }
                // トークン検証APIが正常終了していなかった場合、メッセージを出力し終了する
                if (responseCode != 200) {
                    switch(responseCode){
                        case 403:
                            return Response.status(Response.Status.BAD_REQUEST).entity(new ApiResponseMessage(ApiResponseMessage.ERROR,
                                 Response.Status.BAD_REQUEST.getReasonPhrase() + " : ID_Token is different")).build();

                        case 422:
                            return Response.status(Response.Status.BAD_REQUEST).entity(new ApiResponseMessage(ApiResponseMessage.ERROR,
                                 Response.Status.BAD_REQUEST.getReasonPhrase() + " : Vallidate Error")).build();

                        case 500:
                            return Response.status(Response.Status.BAD_REQUEST).entity(new ApiResponseMessage(ApiResponseMessage.ERROR,
                                 Response.Status.BAD_REQUEST.getReasonPhrase() + " : Internal Server Error")).build();

                        default:
                            throw new IOException();
                    }
                }
            } catch(IOException i) {
                log.error("An error has occurred. " + i.getMessage());
                return Response.status(Response.Status.BAD_REQUEST).entity(new ApiResponseMessage(ApiResponseMessage.ERROR,
                        Response.Status.BAD_REQUEST.getReasonPhrase() + " : connect error")).build();
            }
        }

        System.out.println(log.buildLogMsg(LogLevel.DEBUG,
                "start, inlineObject = " + (inlineObject == null ? "null" : inlineObject.toString())));

        try {
            if (inlineObject == null) {
                return Response.status(Response.Status.BAD_REQUEST).entity(new ApiResponseMessage(ApiResponseMessage.ERROR,
                         Response.Status.BAD_REQUEST.getReasonPhrase() + " : requestBody is null")).build();
            }
            if (inlineObject.getSelector() == null) {
                return Response.status(Response.Status.BAD_REQUEST).entity(new ApiResponseMessage(ApiResponseMessage.ERROR,
                        Response.Status.BAD_REQUEST.getReasonPhrase() + " : " + InlineObject.JSON_PROPERTY_SELECTOR + " is null")).build();
            }

            Contract cdlChainCode = HLFGatewayManager.getInsntance().getContract(null);

            // inlineObject を JSON文字列化する
            ObjectMapper mapper = new ObjectMapper();
            String queryString = mapper.writeValueAsString(inlineObject);

            // fields キーが指定されていない場合、JSON文字列に「,"fields":null」が含まれてしまっているので、除去する
            // で受け取ってそのまま渡したほうが検索の自由度が高い)
            queryString = queryString.replace(",\"fields\":null", "");

            System.out.println(log.buildLogMsg(LogLevel.DEBUG,
                    "call chaincode queryCDLEventByRichQuery() queryString = '" + queryString + "'"));

            byte[] chainCodeResult = cdlChainCode.evaluateTransaction("queryCDLEventByRichQuery", queryString);
            String chainCodeResultJsonString = new String(chainCodeResult);

            // 空っぽが返ってきた場合は、空リストを返す
            if (chainCodeResultJsonString == null || chainCodeResultJsonString.equals("")
                    || chainCodeResultJsonString.equals("[]") || chainCodeResultJsonString.equals("[\n\n]")) {
                System.out.println(log.buildLogMsg(LogLevel.DEBUG, "end, JSON String = [] (empty)"));
                return Response.ok().type(MediaType.APPLICATION_JSON).entity("[]").build();
            }

            System.out.println(log.buildLogMsg(LogLevel.DEBUG, "end, JSON String = " + chainCodeResultJsonString));
            return Response.ok().type(MediaType.APPLICATION_JSON).entity(chainCodeResultJsonString).build();

        } catch (ApiException e) {
            return Response.status(e.getCode()).entity(e.getMessage()).build();

        } catch (Throwable t) {
            String logmsg = log.buildLogMsg(LogLevel.ERROR, t);
            System.out.println(logmsg);
            t.printStackTrace();
            return Response.status(500)
                    .entity(new ApiResponseMessage(ApiResponseMessage.ERROR, "Internal Server Error : " + logmsg))
                    .build();
        }
    }
}
