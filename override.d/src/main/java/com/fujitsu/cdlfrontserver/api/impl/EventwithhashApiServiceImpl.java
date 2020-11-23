/*
    EventwithhashApiServiceImplクラス

    COPYRIGHT FUJITSU LIMITED 2021
*/

package com.fujitsu.cdlfrontserver.api.impl;

import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.hyperledger.fabric.gateway.Contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fujitsu.cdlfrontserver.api.ApiException;
import com.fujitsu.cdlfrontserver.api.ApiResponseMessage;
import com.fujitsu.cdlfrontserver.api.CDLEventJsonStringBuilder;
import com.fujitsu.cdlfrontserver.api.Config;
import com.fujitsu.cdlfrontserver.api.EventwithhashApiService;
import com.fujitsu.cdlfrontserver.api.LogLevel;
import com.fujitsu.cdlfrontserver.api.LogMsg;
import com.fujitsu.cdlfrontserver.api.NotFoundException;
import com.fujitsu.cdlfrontserver.model.CDLDataTag;
import com.fujitsu.cdlfrontserver.model.CDLEvent;
import com.fujitsu.cdlfrontserver.model.CDLEventResponse;
import com.fujitsu.cdlfrontserver.usermanager.HLFGatewayManager;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", date = "2020-12-08T10:35:27.256+09:00[Asia/Tokyo]")
public class EventwithhashApiServiceImpl extends EventwithhashApiService {

    private static final String SHA_NAME = "SHA-256";

    private LogMsg log = LogMsg.getInstance();

    // リッチクエリ条件文
    // ・cdluriとcdlsha256hashのAND条件で検索
    // ・fieldsでcdleventidのみ結果検索を絞り込み
    private String SELECTOR = "{\"selector\":{\"dataprovider\":\"PROVIDER\","+
                                            //"\"datauser\":\"USER\","+ //datauserも一致判定対象とする場合はコメントアウトを外す
                                            "\"cdldatatags\":{\"$elemMatch\":" +
                                            "{\"cdluri\":{\"$eq\":\"URI\"},\"cdlsha256hash\":{\"$eq\":\"HASH\"}}}}," +
                                            "\"fields\":[\"cdleventid\",\"cdleventtype\"]}";

    // バージョン指定
    private static final String DATAMODEL_VERSION = "2.0";

    public Response eventwithhash(CDLEvent request,
            List<FormDataBodyPart> upfileBodypart, SecurityContext securityContext) throws NotFoundException {

        System.out.println(log.buildLogMsg(LogLevel.DEBUG,
                "start, CDLEvent = " + (request == null ? "null" : request.toString())));

        if (request == null) {
            String logmsg = log.buildLogMsg(LogLevel.ERROR, "requested CDLEvent is null");
            return Response.status(500)
                    .entity(new ApiResponseMessage(ApiResponseMessage.ERROR, "Internal Server Error : " + logmsg))
                    .build();
        }


        // バージョンチェック
        // バージョン指定必須、かつ指定バージョンであること
        String version = (String)request.get(CDLEvent.JSON_PROPERTY_CDLDATAMODELVERSION);
        if(version == null || false == DATAMODEL_VERSION.equals(version)) {
            String logmsg_version = log.buildLogMsg(LogLevel.ERROR, "The specified cdldatamodelversion is wrong. " + version);
            return Response.status(Status.BAD_REQUEST.getStatusCode())
                    .entity(new ApiResponseMessage(ApiResponseMessage.ERROR,  logmsg_version))
                    .build();
        }

        // cdleventtypeチェック
        // cdleventtype指定必須
        String cdleventtype = (String)request.get(CDLEvent.JSON_PROPERTY_CDLEVENTTYPE);
        if(cdleventtype == null) {
            String logmsg_eventtype = log.buildLogMsg(LogLevel.ERROR, "The specified cdleventtype is null. ");
            return Response.status(Status.BAD_REQUEST.getStatusCode())
                    .entity(new ApiResponseMessage(ApiResponseMessage.ERROR,  logmsg_eventtype))
                    .build();
        }

        try {
            Contract cdlChainCode = HLFGatewayManager.getInsntance().getContract(null);

            // 全コンテンツのハッシュ値取得
            if (upfileBodypart != null) {

                byte[] hashByteArray = null;
                MessageDigest messageDigest = null;
                messageDigest = MessageDigest.getInstance(SHA_NAME);

                // cdldatatags がなければ、生成する
                List<Map<String, String>> sha256recordList;
                if (request.containsKey(CDLEvent.JSON_PROPERTY_CDLDATATAGS)) {
                    Object obj = request.get(CDLEvent.JSON_PROPERTY_CDLDATATAGS);
                    // List かどうかチェック
                    if (!(obj instanceof List)) {
                        throw new ApiException(500, log.buildLogMsg(LogLevel.ERROR,
                                "CDLEvent Validation Error : cdldatatags is not List "));
                    }
                    sha256recordList = (List<Map<String, String>>) obj;
                } else {
                    sha256recordList = new ArrayList<Map<String, String>>();
                    request.put(CDLEvent.JSON_PROPERTY_CDLDATATAGS, sha256recordList);
                }

                int sha256recordIndex = 0;
                for (FormDataBodyPart formBody : upfileBodypart) {

                    InputStream tempInputStream = formBody.getValueAs(InputStream.class);
                    DigestInputStream digestStream = new DigestInputStream(tempInputStream, messageDigest);
                    while (digestStream.read() != -1) {
                    }

                    // ハッシュ値の計算
                    hashByteArray = messageDigest.digest();

                    // ハッシュ値（byte）を文字列に変換し返却
                    StringBuilder hashStringBuf = new StringBuilder();
                    for (byte hashByte : hashByteArray) {
                        String hexString = String.format("%02x", hashByte);
                        hashStringBuf.append(hexString);
                    }
                    System.out.println(log.buildLogMsg(LogLevel.DEBUG,
                            "Content-Disposition: " + formBody.getContentDisposition().toString()));
                    System.out.println(
                            log.buildLogMsg(LogLevel.DEBUG, "Content-Type: " + formBody.getMediaType().toString()));
                    String name = formBody.getContentDisposition().getFileName();
                    if (name == null) {
                        name = formBody.getName();
                    }
                    System.out.println(
                            log.buildLogMsg(LogLevel.DEBUG, "SHA256 (" + name + ") = " + hashStringBuf.toString()));
                    try {
                        String value = formBody.getEntityAs(String.class);
                        if (value.length() < 1024) {
                            // 1Kバイト以内であれば、中身をログに出力
                            System.out.println(log.buildLogMsg(LogLevel.DEBUG, "value :\n" + value));
                        }
                    } catch (Throwable t) {
                        ;
                    }

                    // CDLEventに追記
                    HashMap<String, String> sha256recordMap;
                    if (sha256recordIndex < sha256recordList.size()) {
                        sha256recordMap = (HashMap<String, String>) sha256recordList.get(sha256recordIndex);
                    } else {
                        sha256recordMap = new HashMap<String, String>();
                        sha256recordList.add(sha256recordMap);
                    }

                    sha256recordMap.put(CDLDataTag.JSON_PROPERTY_CDLSHA256HASH, hashStringBuf.toString());
                    sha256recordIndex++;
                }
            }

            // cdleventtypeが環境変数に定義されているものと一致する場合は重複チェックを行う
            // なお、リクエストにcdleventtypeが未定義の場合、イベント登録を行う
            boolean duplicateCheck = false;
            for (String eventType : Config.get().cdlEventTypeDuplicateCheck()) {
                System.out.println(log.buildLogMsg(LogLevel.DEBUG,
                            "duplicateCheck eventType = '" + eventType + "'"));
                System.out.println(log.buildLogMsg(LogLevel.DEBUG,
                            "request eventType = '" + request.get(CDLEvent.JSON_PROPERTY_CDLEVENTTYPE) + "'"));
                if (eventType.equals(request.get(CDLEvent.JSON_PROPERTY_CDLEVENTTYPE))) {
                    duplicateCheck = true;
                    break;
                }
            }

            // リッチクエリ検索
            // 以下の条件をすべて満たした場合、リッチクエリ検索を行い、検索ヒットした場合、そのeventidを返す
            //  ・cdleventtypeが環境変数に定義されているものと一致
            //  ・cdldatatagの数が１つ
            //  　(cdldatatagが空または複数の場合は、従来の仕様通り、イベント登録を行う）
            List<Map<String, String>> datatag = (List<Map<String, String>>) request.get(CDLEvent.JSON_PROPERTY_CDLDATATAGS);
            if (duplicateCheck && datatag.size() == 1) {
                String cdluri = datatag.get(0).get(CDLDataTag.JSON_PROPERTY_CDLURI);
                String cdlsha256hash = datatag.get(0).get(CDLDataTag.JSON_PROPERTY_CDLSHA256HASH);
                List<String> previouseventsList = (List<String>) request.get(CDLEvent.JSON_PROPERTY_CDLPREVIOUSEVENTS);
                String dataprovider = (String) request.get("dataprovider");
                //String datauser = (String) request.get("datauser"); //datauserも一致判定対象とする場合はコメントアウトを外す
                List<CDLEvent> ResultCDLEventList = new ArrayList<>();

                // cdleventtypeが環境変数に定義されているものと一致するし、かつcdlpreviouseventsが空の場合はcdlpreviouseventsの追加を行う
                boolean addPreviouseventsCheck = false;
                for (String eventType : Config.get().cdlEventTypeAddPreviouseventsCheck()) {
                    System.out.println(log.buildLogMsg(LogLevel.DEBUG,
                                "duplicateCheck eventType = '" + eventType + "'"));
                    System.out.println(log.buildLogMsg(LogLevel.DEBUG,
                                "request eventType = '" + request.get(CDLEvent.JSON_PROPERTY_CDLEVENTTYPE) + "'"));
                    // if (eventType.equals(request.get(CDLEvent.JSON_PROPERTY_CDLEVENTTYPE)) && previouseventsList.size()==0) {
                    if (eventType.equals(request.get(CDLEvent.JSON_PROPERTY_CDLEVENTTYPE)) && (previouseventsList == null || previouseventsList.size()==0)) {
                        addPreviouseventsCheck = true;
                        break;
                    }
                }

                if (cdluri != null && !cdluri.equals("") && cdlsha256hash != null && !cdlsha256hash.equals("")
                    && dataprovider != null && !dataprovider.equals("") 
                    //&& datauser != null && !datauser.equals("") //datauserも一致判定対象とする場合はコメントアウトを外す
                    ) {

                    // cdluriとcdlsha256hashが一致するCDLEventのcdleventidを取得する
                    String queryString = SELECTOR.replace("URI", cdluri).replace("HASH", cdlsha256hash)
                                                    //.replace("USER", datauser) //datauserも一致判定対象とする場合はコメントアウトを外す
                                                    .replace("PROVIDER", dataprovider);

                    System.out.println(log.buildLogMsg(LogLevel.DEBUG,
                                "call chaincode queryCDLEventByRichQuery() queryString = '" + queryString + "'"));

                    byte[] chainCodeResult = cdlChainCode.evaluateTransaction("queryCDLEventByRichQuery", queryString);
                    String chainCodeResultJsonString = new String(chainCodeResult);

                    System.out.println(log.buildLogMsg(LogLevel.DEBUG,
                                "chainCodeResultJsonString = '" + chainCodeResultJsonString + "'"));

                    // 空でない場合、取得したcdleventidを応答レスポンスで返す
                    // それ以外は、イベント登録を行う
                    if (chainCodeResultJsonString != null && !chainCodeResultJsonString.equals("")
                            && !chainCodeResultJsonString.equals("[]") && !chainCodeResultJsonString.equals("[\n\n]")
                            && !chainCodeResultJsonString.equals("[\n]")) {

                        // チェーンコード・リッチクエリのレスポンスから、先頭'[\n'、末尾'\n]' を削除し、改行で分割
                        String[] ResultJsonStrings = chainCodeResultJsonString.substring(2, chainCodeResultJsonString.length() - 2).split(",\n");
                        
                        for (String ResultJsonString : ResultJsonStrings){

                            ObjectMapper mapper = new ObjectMapper();
                            CDLEvent ResultCDLEvent = mapper.readValue(ResultJsonString ,CDLEvent.class);
                        
                            String eventid_str = (String) ResultCDLEvent.get(CDLEvent.JSON_PROPERTY_CDLEVENTID);
                            String eventtype_str = (String) ResultCDLEvent.get(CDLEvent.JSON_PROPERTY_CDLEVENTTYPE);
                            System.out.println("cdleventid:"+eventid_str);
                            System.out.println("cdleventtype: "+eventtype_str);

                            ResultCDLEventList.add(ResultCDLEvent);                            
                        }
                    }

                    // データ重複イベントに更新タイプのイベントがあるかチェックを行う
                    boolean updateCheck = false;
                    String addcdlEventId = "";
                    for (String eventType : Config.get().cdlEventTypeUpdateCheck()) {
                        for (CDLEvent ResultEvent: ResultCDLEventList){
                            System.out.println(log.buildLogMsg(LogLevel.DEBUG,
                                    "updateCheck eventType = '" + eventType + "'"));
                            System.out.println(log.buildLogMsg(LogLevel.DEBUG,
                                    "detected eventType = '" + ResultEvent.get(CDLEvent.JSON_PROPERTY_CDLEVENTTYPE) + "'"));
                            if (eventType.equals(ResultEvent.get(CDLEvent.JSON_PROPERTY_CDLEVENTTYPE))) {
                                updateCheck = true;
                                addcdlEventId = (String) ResultEvent.get(CDLEvent.JSON_PROPERTY_CDLEVENTID);
                                break;
                            }
                        }
                    }

                    // データ重複イベントにカタログ作成タイプ（CreateやPublish）のイベントがあるかチェックを行う
                    boolean publishCheck = false;
                    for (String eventType : Config.get().cdlEventTypePublisgCheck()) {
                        for (CDLEvent ResultEvent: ResultCDLEventList){
                            System.out.println(log.buildLogMsg(LogLevel.DEBUG,
                                    "publishCheck eventType = '" + eventType + "'"));
                            System.out.println(log.buildLogMsg(LogLevel.DEBUG,
                                    "detected eventType = '" + ResultEvent.get(CDLEvent.JSON_PROPERTY_CDLEVENTTYPE) + "'"));
                            if (eventType.equals(ResultEvent.get(CDLEvent.JSON_PROPERTY_CDLEVENTTYPE))) {
                                publishCheck = true;
                                break;
                            }
                        }
                    }

                    //同一データの更新履歴があり、かつcdlpreviouseventsが空、かつ登録中の履歴イベントがカタログ作成タイプである場合
                    //cdlpreviouseventsに更新履歴のcdleventidを追加する
                    //それ以外で、かつ同一ユーザ、データのイベントが存在した場合は、取得したcdleventidを応答レスポンスで返す
                    if ( updateCheck && addPreviouseventsCheck && !publishCheck){
                        List<String> setpreviouseventsList = new ArrayList<>();
                        setpreviouseventsList.add(addcdlEventId);

                        System.out.println(log.buildLogMsg(LogLevel.DEBUG, "end, add previousevents = " + setpreviouseventsList));

                        request.put(CDLEvent.JSON_PROPERTY_CDLPREVIOUSEVENTS, setpreviouseventsList);
                    }else if (ResultCDLEventList.size()!=0){
                        
                        //重複チェックの結果から、更新履歴を除外する
                        //複数のイベントが検出されることを想定した、カンマ区切りで表示するための処理
                        String cdlEventIdString = "";
                        for (CDLEvent ResultEvent: ResultCDLEventList){
                            for (String eventType : Config.get().cdlEventTypeUpdateCheck()) {
                                if (!eventType.equals(ResultEvent.get(CDLEvent.JSON_PROPERTY_CDLEVENTTYPE))){
                                    if (cdlEventIdString != ""){
                                        cdlEventIdString = cdlEventIdString + ", ";
                                    }
                                    cdlEventIdString = cdlEventIdString + ResultEvent.get(CDLEvent.JSON_PROPERTY_CDLEVENTID);
                                }
                            }
                            
                        }
                        String cdlEventId = cdlEventIdString;

                        CDLEventResponse cdlEventResponse = new CDLEventResponse();
                        cdlEventResponse.setCdleventid(cdlEventId);

                        ObjectMapper mapper = new ObjectMapper();
                        String cdlEventResponseJsonString = mapper.writeValueAsString(cdlEventResponse);

                        System.out.println(log.buildLogMsg(LogLevel.DEBUG, "end, response = " + cdlEventResponseJsonString));

                        return Response.ok().type(MediaType.APPLICATION_JSON).entity(cdlEventResponseJsonString).build();
                    }
                }
            }

            // CDLEvent の整形
            CDLEventJsonStringBuilder cdlEventJsonStringBuilder = new CDLEventJsonStringBuilder(cdlChainCode);
            String jsonString = cdlEventJsonStringBuilder.build(request);

            // CDLEvent を HLF に書き込み
            String cdlEventId = cdlEventJsonStringBuilder.getCDLEventId();
            System.out.println(log.buildLogMsg(LogLevel.DEBUG,
                    "write to Block-Chain, Key = '" + cdlEventId + "', JSON = " + jsonString));
            String previousEvents = cdlEventJsonStringBuilder.getPreviousEvents();
            cdlChainCode.submitTransaction("registUpdateCDLEvent", cdlEventId, jsonString, previousEvents);

            CDLEventResponse cdlEventResponse = new CDLEventResponse();
            cdlEventResponse.setCdleventid(cdlEventId);

            ObjectMapper mapper = new ObjectMapper();
            String cdlEventResponseJsonString = mapper.writeValueAsString(cdlEventResponse);

            System.out.println(log.buildLogMsg(LogLevel.DEBUG, "end, response = " + cdlEventResponseJsonString));

            return Response.ok().type(MediaType.APPLICATION_JSON).entity(cdlEventResponseJsonString).build();

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
