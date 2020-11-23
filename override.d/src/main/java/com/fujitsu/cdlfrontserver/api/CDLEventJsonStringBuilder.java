/*
    CDLEventJsonStringBuilderクラス

    COPYRIGHT FUJITSU LIMITED 2021
*/

package com.fujitsu.cdlfrontserver.api;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.Response.Status;

import org.erdtman.jcs.JsonCanonicalizer;
import org.hyperledger.fabric.gateway.Contract;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fujitsu.cdlfrontserver.model.CDLEvent;
import com.fujitsu.cdlfrontserver.model.CDLDataTag;

public class CDLEventJsonStringBuilder {

    private LogMsg log = LogMsg.getInstance();
    private static final ObjectMapper mapper = new ObjectMapper();
    
    private static ArrayList<String> EmptyCdlPreviousEvents = new ArrayList<String>();
    private static ArrayList<String> EmptyCdlNextEvents = new ArrayList<String>();
    private Contract _hlfContract; // HLFコントラクターオブジェクト

    // private HashMap<String, Object> _originalReqBodyMap;
    private HashMap<String, Object> _reqCDLEventAndGlobalTagMap = new HashMap<String, Object>();

    // 次イベントIDを設定した前イベントリスト
    private String _previousEvents = null;

    private CDLEventJsonStringBuilder() {
    }

    public CDLEventJsonStringBuilder(Contract hlfContract) {
        this._hlfContract = hlfContract;
    }

    public String build(Map<String, Object> requestBody) throws Exception {

        log.debug("start");

        boolean cdltagscopeIsSpecified = false;
        boolean globalTagIsSpecified = false;
        boolean localTagIsSpecified = false;

        // リクエストのJSON文字列からオブジェクト化された LinkedHashMap をコピー
        for (Map.Entry<String, Object> entry : requestBody.entrySet()) {
            String k = entry.getKey();
            Object v = entry.getValue();

            // _originalReqBodyMap.put(k, v);

            _reqCDLEventAndGlobalTagMap.put(k, v);
        }


        //
        // JSONオブジェクト(LinkedHashMap) から CDLEvent オブジェクト生成
        //

        // イベントID取得
        String reqCdlEventId = (String) _reqCDLEventAndGlobalTagMap.get(CDLEvent.JSON_PROPERTY_CDLEVENTID);

        // イベントID省略時生成
        if (reqCdlEventId == null || reqCdlEventId.isEmpty()) {
            reqCdlEventId = UUID.randomUUID().toString();
            _reqCDLEventAndGlobalTagMap.put(CDLEvent.JSON_PROPERTY_CDLEVENTID, reqCdlEventId);
        } else {
            // 二重登録チェック
            // Boolean で直接受け取ることはできず、byte[] で受け取り String に変換するしかない。'true' か 'false' で返ってくる
            byte[] chainCodeResult = this._hlfContract.evaluateTransaction("keyExists", reqCdlEventId);
            String chainCodeResultBooleanString = new String(chainCodeResult);

            boolean keyAlreadyExists = chainCodeResultBooleanString.equals("true");

            if (keyAlreadyExists) {
                throw new ApiException(Status.BAD_REQUEST.getStatusCode(),
                        Status.BAD_REQUEST.getReasonPhrase() + " : specified " + CDLEvent.JSON_PROPERTY_CDLEVENTID
                                + " '" + reqCdlEventId + "' already exists");
            }
        }

        // イベントタイプ
        String reqCdlEventType = (String) _reqCDLEventAndGlobalTagMap.get(CDLEvent.JSON_PROPERTY_CDLEVENTTYPE);
        if (reqCdlEventType == null) {
            reqCdlEventType = "";
            _reqCDLEventAndGlobalTagMap.put(CDLEvent.JSON_PROPERTY_CDLEVENTTYPE, reqCdlEventType);
        }

        // 組織名 → 未指定時は 環境変数 CDL_HLF_ORGANIZATION の値を設定
        String reqCdlOrganization = (String) _reqCDLEventAndGlobalTagMap.get(CDLEvent.JSON_PROPERTY_CDLORGANIZATION);
        if (reqCdlOrganization == null) {
            reqCdlOrganization = Config.get().cdlHlfOrganization();
            _reqCDLEventAndGlobalTagMap.put(CDLEvent.JSON_PROPERTY_CDLORGANIZATION, reqCdlOrganization);
        }

        // タイムスタンプ
        String reqCdlTimeStamp = (String) _reqCDLEventAndGlobalTagMap.get(CDLEvent.JSON_PROPERTY_CDLTIMESTAMP);
        if (reqCdlTimeStamp == null) {
            // 現在時刻を設定
            OffsetDateTime nowOffsetTime = OffsetDateTime.now();
            reqCdlTimeStamp = nowOffsetTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            _reqCDLEventAndGlobalTagMap.put(CDLEvent.JSON_PROPERTY_CDLTIMESTAMP, reqCdlTimeStamp);
        }

        // CDLデータモデルバージョン
        String reqCdlDataModelVersion = (String) _reqCDLEventAndGlobalTagMap
                .get(CDLEvent.JSON_PROPERTY_CDLDATAMODELVERSION);
        if (reqCdlDataModelVersion == null) {
            // CDLデータモデルバージョンを設定
            _reqCDLEventAndGlobalTagMap.put(CDLEvent.JSON_PROPERTY_CDLDATAMODELVERSION, "2.0");
        }

        // 前イベントID存在チェック (キー自体がなければ空ARRAYを挿入)
        List<String> reqCdlPreviousEvents = (List<String>) _reqCDLEventAndGlobalTagMap
                .get(CDLEvent.JSON_PROPERTY_CDLPREVIOUSEVENTS);
        
        List<CDLEvent> previousEventList = null;

        if (reqCdlPreviousEvents != null) {
            // (JSON記述が List<String> でなければ、エラーで受付られないはず)

            // {"cdleventid":"<イベントID>"}のリストを生成
            List<Map<String, String>> orList = new ArrayList<>();
            for (String previousEventId : reqCdlPreviousEvents) {
                Map<String, String> map = new HashMap<>();
                map.put(CDLEvent.JSON_PROPERTY_CDLEVENTID, previousEventId);
                orList.add(map);
            }

            // クエリ生成
            Map<String, List<Map<String, String>>> selectorMap = new HashMap<>();
            selectorMap.put("$or", orList);
            Map<String, Object> queryMap = new HashMap<>();
            queryMap.put("selector", selectorMap);
            String queryString = mapper.writeValueAsString(queryMap);
            System.out.println(log.buildLogMsg(LogLevel.DEBUG,
                    "call chaincode queryCDLEventByRichQuery() queryString = '" + queryString + "'"));
            
            // 前イベントリスト取得
            byte[] chainCodeResult = this._hlfContract.evaluateTransaction("queryCDLEventByRichQuery", queryString);
            String chainCodeResultJsonString = new String(chainCodeResult);

            System.out.println(log.buildLogMsg(LogLevel.DEBUG,
                    "call chaincode queryCDLEventByRichQuery() response = '" + chainCodeResultJsonString + "'"));

            Map<String, CDLEvent> previousEventMap = new HashMap<>();
            if (!chainCodeResultJsonString.equals("") && !chainCodeResultJsonString.equals("[]") &&
                    !chainCodeResultJsonString.equals("[\n\n]")) {
                // チェーンコード・リッチクエリのレスポンスから、先頭'[\n'、末尾'\n]' を削除し、改行で分割
                String[] events = chainCodeResultJsonString.substring(2, chainCodeResultJsonString.length() - 2).split(",\n");

                for (String eventString : events) {
                    System.out.println(log.buildLogMsg(LogLevel.DEBUG, "eventString = '" + eventString + "'"));
                    
                    CDLEvent event = mapper.readValue(eventString, CDLEvent.class);
                    String eventid = (String)event.get(CDLEvent.JSON_PROPERTY_CDLEVENTID);
                        
                    System.out.println(log.buildLogMsg(LogLevel.DEBUG, "event = '" + event + "'"));

                    // 前イベントマップに追加
                    previousEventMap.put(eventid, event);
                }
            }

            // 前イベント存在チェック
            previousEventList = new ArrayList<>();
            for (String previousEventId : reqCdlPreviousEvents) {
                CDLEvent previousEvent = previousEventMap.get(previousEventId);
                System.out.println(log.buildLogMsg(LogLevel.DEBUG, "previousEvent = '" + previousEvent + "'"));
                if (previousEvent == null) {
                    // 指定された前イベントIDが存在しない場合
                    throw new ApiException(Status.BAD_REQUEST.getStatusCode(),
                            Status.BAD_REQUEST.getReasonPhrase() + " : specified "
                                    + CDLEvent.JSON_PROPERTY_CDLPREVIOUSEVENTS + " '" + previousEventId
                                    + "' does not exist");
                }
                previousEventList.add(previousEvent);
            }

        } else {
            // キー cdlpreviousevents 自体が指定されていない場合は、空ARRAYを追加する
            _reqCDLEventAndGlobalTagMap.put(CDLEvent.JSON_PROPERTY_CDLPREVIOUSEVENTS, EmptyCdlPreviousEvents);
            previousEventList = new ArrayList<>();
        }

        //次イベント追加用に新しく前イベントマップを生成
        Map<String, String> previousEventMap = new HashMap<>();

        // 前イベントの次イベントIDを設定
        for (CDLEvent previousEvent : previousEventList) {
            System.out.println(log.buildLogMsg(LogLevel.DEBUG, "previousEvent = '" + previousEvent + "'"));
            String previousEventId = (String)previousEvent.get(CDLEvent.JSON_PROPERTY_CDLEVENTID);

            // 次イベントID追加
            List nextEventList = (List)previousEvent.get(CDLEvent.JSON_PROPERTY_CDLNEXTEVENTS);
            nextEventList.add(reqCdlEventId);
            previousEvent.put(CDLEvent.JSON_PROPERTY_CDLNEXTEVENTS,nextEventList);

            // JSON文字列化
            String jsonString = mapper.writeValueAsString(previousEvent);
            JsonCanonicalizer canonicalizer = new JsonCanonicalizer(jsonString);
            String canonicalizedJson = canonicalizer.getEncodedString();

            // 前イベントマップに追加
            previousEventMap.put(previousEventId, canonicalizedJson);

        }
        
        //resistUpdateEventsで使用する前イベント情報
        _previousEvents = mapper.writeValueAsString(previousEventMap);
        log.debug("_previousEvents = " + _previousEvents);

        // 次イベントIDリスト
        _reqCDLEventAndGlobalTagMap.put(CDLEvent.JSON_PROPERTY_CDLNEXTEVENTS, EmptyCdlNextEvents);

        log.debug("CDLEvent.toString() = " + _reqCDLEventAndGlobalTagMap.toString());

        // JSON文字列化
        String jsonString = mapper.writeValueAsString(_reqCDLEventAndGlobalTagMap);

        // JSON正規化
        JsonCanonicalizer jc = new JsonCanonicalizer(jsonString);
        String canonicalizedJson = jc.getEncodedString();

        log.debug("end, canonicalizedJson = " + canonicalizedJson);

        return canonicalizedJson;
    }

    public HashMap<String, Object> getCDLEventObj() {
        return _reqCDLEventAndGlobalTagMap;
    }

    public String getCDLEventId() {
        return (String) _reqCDLEventAndGlobalTagMap.get(CDLEvent.JSON_PROPERTY_CDLEVENTID);
    }

    public String getPreviousEvents() {
        return _previousEvents;
    }

}
