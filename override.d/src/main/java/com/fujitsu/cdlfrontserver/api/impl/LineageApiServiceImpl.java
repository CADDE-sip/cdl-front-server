/*
    LineageApiServiceImplクラス

    COPYRIGHT FUJITSU LIMITED 2021
*/

package com.fujitsu.cdlfrontserver.api.impl;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import javax.validation.constraints.NotNull;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.ContractException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fujitsu.cdlfrontserver.api.ApiException;
import com.fujitsu.cdlfrontserver.api.ApiResponseMessage;
import com.fujitsu.cdlfrontserver.api.LineageApiService;
import com.fujitsu.cdlfrontserver.api.LogLevel;
import com.fujitsu.cdlfrontserver.api.LogMsg;
import com.fujitsu.cdlfrontserver.api.NotFoundException;
import com.fujitsu.cdlfrontserver.model.CDLEvent;
import com.fujitsu.cdlfrontserver.usermanager.HLFGatewayManager;

import com.fujitsu.cdlfrontserver.api.Config;
import com.fujitsu.cdlfrontserver.api.GetAuthenticationToken;
import java.io.IOException;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", date = "2020-12-01T20:30:14.057+09:00[Asia/Tokyo]")
public class LineageApiServiceImpl extends LineageApiService {

    /**
     * 探索イベントクラス
     * イベントIDと深さを保持
     */
    class SearchEvent {
        public SearchEvent(String eventId) {
            this.eventId = eventId;
            this.depth = 0;
        }

        public SearchEvent(String eventId, int depth) {
            this.eventId = eventId;
            this.depth = depth;
        }

        public String getEventId() {
            return eventId;
        }

        public void setEventId(String eventId) {
            this.eventId = eventId;
        }

        public int getDepth() {
            return depth;
        }

        public void setDepth(int depth) {
            this.depth = depth;
        }

        private String eventId = null;
        private int depth = 0;
    }

    private LogMsg log = LogMsg.getInstance();

    private static int MAX_LINEAGE = 10000;

    // directionパラメタ
    private static enum DIRECTION_PARAM {FORWARD, BACKWARD, BOTH};

    ObjectMapper mapper = new ObjectMapper();

    @Override
    public Response lineage(String cdleventid, String Authorization, String ID_Token, String direction, Integer depth,
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

                        case  500:
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
                "start, cdleventid='" + cdleventid + "', direction=" + direction + ", depth=" + depth.intValue()));

        // directionパラメタチェック
        DIRECTION_PARAM directionEnum = DIRECTION_PARAM.BACKWARD;
        try {
            directionEnum = DIRECTION_PARAM.valueOf(direction);
        } catch (IllegalArgumentException e) {
            String logmsg_direction = log.buildLogMsg(LogLevel.ERROR, "The specified direction is wrong. " + direction);
            return Response.status(Status.BAD_REQUEST.getStatusCode())
                    .entity(new ApiResponseMessage(ApiResponseMessage.ERROR, logmsg_direction))
                    .build();
        }
        // depthパラメタチェック
        if(depth != null && depth < -1) {
            String logmsg_depth = log.buildLogMsg(LogLevel.ERROR, "The specified depth is wrong. " + depth);
            return Response.status(Status.BAD_REQUEST.getStatusCode())
                    .entity(new ApiResponseMessage(ApiResponseMessage.ERROR, logmsg_depth))
                    .build();
        }
        // depthが-1の場合はすべてのものが対象なので最大値を設定しておく
        if (depth < 0) {
            depth = Integer.MAX_VALUE;
        }

        try {
            Contract cdlChainCode = HLFGatewayManager.getInsntance().getContract(null);

            // 応答イベントリスト（文字列）lineageレスポンス用 CDLEvent リストは、JSON 文字列として管理する
            StringBuilder events = null;
            // 後方探索キュー
            Deque<SearchEvent> backwardSearchEvents = new ArrayDeque<>();
            // 後方検索イベントIDセット（重複検知用）
            Set<String> backwardEventIds = new HashSet<>();
            // 前方探索キュー
            Deque<SearchEvent> forwardSearchEvents = new ArrayDeque<>();
            // 前方検索イベントIDセット（重複検知用）
            Set<String> forwardEventIds = new HashSet<>();
            if (directionEnum == DIRECTION_PARAM.FORWARD) {
                forwardSearchEvents.add(new SearchEvent(cdleventid));
                forwardEventIds.add(cdleventid);
            } else {
                // BACKWARDとBOTHは後方探索から実行
                backwardSearchEvents.add(new SearchEvent(cdleventid));
                backwardEventIds.add(cdleventid);
            }

            // 後方探索
            while (true) {
                // キューが空になったら終了
                SearchEvent searchEvent = backwardSearchEvents.poll();
                if (searchEvent == null) {
                    break;
                }
                System.out.println(log.buildLogMsg(LogLevel.DEBUG,
                        "BACKWARD SEARCH : searching id = '" + searchEvent.getEventId() + "', depth=" + searchEvent.getDepth()));

                // イベント取得（CDLEvent JSON文字列）
                byte[] chainCodeResult = cdlChainCode.evaluateTransaction("queryCDLEvent", searchEvent.getEventId());
                String chainCodeResultJsonString = new String(chainCodeResult);
                System.out.println(log.buildLogMsg(LogLevel.DEBUG,
                        "BACKWARD SEARCH : CDL ChainCode queryCDLEvent() returns JSON String '"
                                + chainCodeResultJsonString + "'"));
                if (chainCodeResultJsonString == null || chainCodeResultJsonString.equals("")) {
                    break;
                }

                // 応答イベントリストに追加
                if (events == null) {
                    events = new StringBuilder(chainCodeResultJsonString);
                } else {
                    events.insert(0, ",\n");
                    events.insert(0, chainCodeResultJsonString);
                }

                // 探索終了
                if (depth <= searchEvent.getDepth()) {
                    continue;
                }

                // 前イベントIDリストを探索対象に追加
                List<String> previousEventIdList = getPreviousEventIdList(chainCodeResultJsonString);
                for (String previousEventId : previousEventIdList) {
                    // 後方探索済みは探索不要のため追加しない
                    if (!backwardEventIds.contains(previousEventId)) {
                        backwardSearchEvents.add(new SearchEvent(previousEventId, searchEvent.getDepth() + 1));
                        backwardEventIds.add(previousEventId);
                    }
                }

                // 指定されたイベントIDでBOTHの場合は次イベントIDリストを前方探索対象に追加
                if (searchEvent.getEventId().equals(cdleventid) && directionEnum == DIRECTION_PARAM.BOTH) {
                    List<String> nextEventIdList = getNextEventIdList(chainCodeResultJsonString);
                    for (String nextEventId : nextEventIdList) {
                        forwardSearchEvents.add(new SearchEvent(nextEventId, 1));
                        forwardEventIds.add(nextEventId);
                    }
                }
            }

            // 前方探索
            while (true) {
                // キューが空になったら終了
                SearchEvent searchEvent = forwardSearchEvents.poll();
                if (searchEvent == null) {
                    break;
                }
                System.out.println(log.buildLogMsg(LogLevel.DEBUG,
                        "FORWARD SEARCH : searching id = '" + searchEvent.getEventId() + "', depth=" + searchEvent.getDepth()));

                // イベント取得
                byte[] chainCodeResult = cdlChainCode.evaluateTransaction("queryCDLEvent", searchEvent.getEventId());
                String chainCodeResultJsonString = new String(chainCodeResult);
                System.out.println(log.buildLogMsg(LogLevel.DEBUG,
                        "FORWARD SEARCH : CDL ChainCode queryCDLEvent() returns JSON String '"
                                + chainCodeResultJsonString + "'"));
                if (chainCodeResultJsonString == null || chainCodeResultJsonString.equals("")) {
                    break;
                }

                // 応答イベントリストに追加
                // 後方探索済みの場合は追加不要
                if (!backwardEventIds.contains(searchEvent.getEventId())) {
                    if (events == null) {
                        events = new StringBuilder(chainCodeResultJsonString);
                    } else {
                        events.append(",\n");
                        events.append(chainCodeResultJsonString);
                    }
                }

                // 探索終了
                if (depth <= searchEvent.getDepth()) {
                    continue;
                }

                // 次イベントIDリストを探索対象に追加
                List<String> nextEventIdList = getNextEventIdList(chainCodeResultJsonString);
                
                for (String nextEventId : nextEventIdList) {
                    // 前方探索済みは探索不要のため追加しない
                    if (!forwardEventIds.contains(nextEventId)) {
                        forwardSearchEvents.add(new SearchEvent(nextEventId, searchEvent.getDepth() + 1));
                        forwardEventIds.add(nextEventId);
                    }
                }
            }

            // []で囲む
            events.insert(0, "[\n");
            events.append("\n]");

            System.out.println(log.buildLogMsg(LogLevel.DEBUG, "end, response = " + events.toString()));

            return Response.ok().type(MediaType.APPLICATION_JSON).entity(events.toString()).build();

        } catch (ContractException e) {
            // リッチクエリ検索エラーの際はContractExceptionが発生するため、404を返す
            String logmsg = log.buildLogMsg(LogLevel.ERROR, e);
            e.printStackTrace();
            return Response.status(Status.NOT_FOUND.getStatusCode())
                    .entity(new ApiResponseMessage(ApiResponseMessage.ERROR, logmsg))
                    .build();
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

    /**
     * @return _previousEventIdList
     */
    @SuppressWarnings("unchecked")
    private List<String> getPreviousEventIdList(String jsonStr) {

        List<String> _previousEventIdList = null;

        HashMap<String, Object> _cdlEventObj = null;
            try {

                // JSON文字列をObject化
                _cdlEventObj = (HashMap<String, Object>) mapper.readValue( jsonStr,
                        HashMap.class );
            } catch ( Throwable t ) {
                return null;
            }


        // 前イベントID抽出 (lineage追跡用)
        _previousEventIdList = (List<String>) _cdlEventObj.get( CDLEvent.JSON_PROPERTY_CDLPREVIOUSEVENTS );
        if ( !( _previousEventIdList instanceof List ) ) {
            // 前イベントIDが List ではない場合、無効とする
            _previousEventIdList = null;
        }

        return _previousEventIdList;
    }

    /**
     * @return _nextEventIdList
     */
    @SuppressWarnings("unchecked")
    private List<String> getNextEventIdList(String jsonStr) {

        List<String> _nextEventIdList = null;

        HashMap<String, Object> _cdlEventObj = null;
            try {

                // JSON文字列をObject化
                _cdlEventObj = (HashMap<String, Object>) mapper.readValue( jsonStr,
                        HashMap.class );
            } catch ( Throwable t ) {
                return null;
            }


        // 次イベントID抽出 (lineage追跡用)
        _nextEventIdList = (List<String>) _cdlEventObj.get( CDLEvent.JSON_PROPERTY_CDLNEXTEVENTS );
        if ( !( _nextEventIdList instanceof List ) ) {
            // 次イベントIDが List ではない場合、無効とする
            _nextEventIdList = null;
        }

        return _nextEventIdList;
    }
}
