package com.fujitsu.cdlfrontserver.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import com.fujitsu.cdlfrontserver.common.Utils;
import com.fujitsu.cdlfrontserver.api.Config;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class GetAuthenticationToken {

    public static int execute(String clientId, String clientSecret, String ID_Token) throws IOException {

        // 認証APIのURLをコンフィグファイルから設定をする
        String portalURL = Config.get().portalUrl();

        // JSON文字列
        String json = "{\"access_token\": \"" + ID_Token + "\"}";

	// 認証パラメータの設定
	String prm = clientId + ":" + clientSecret;
        Charset charset = StandardCharsets.UTF_8;
        byte[] b64 = Base64.getUrlEncoder().withoutPadding()
            .encode(prm.getBytes(charset));
        String prm64 = new String(b64, charset);

        // URL に対して openConnection メソッドを呼び出すし、接続オブジェクトを生成
        URL url = new URL(portalURL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // HTTPのメソッドをPOSTに設定
        conn.setRequestMethod("POST");

        // リクエストボディへの書き込みを許可
        conn.setDoInput(true);

        // レスポンスボディの取得を許可
        conn.setDoOutput(true);

        // リクエストヘッダを指定
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("Authorization", "Basic " + prm64 + "==");

        // 接続を確立する
        conn.connect();

        // HttpURLConnectionからOutputStreamを取得し、json文字列を書き込む
        PrintStream ps = new PrintStream(conn.getOutputStream());
        ps.print(json);
        ps.close();

        // レスポンスを受け取る
        int responseCode = conn.getResponseCode();

        // 正常終了時以外はユーザ確認、登録処理を行わずresponceCodeを返す
        if (responseCode != 200) {
            return responseCode;
        }

        //HttpURLConnectionからInputStreamを取得し、読み出す
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

        StringBuilder sb = new StringBuilder();
        String line;

        while ((line = br.readLine()) != null) {
            sb.append(line);
        }

        // InputStreamを閉じる
        br.close();

        // jsonをmap化し、値のみをString化する処理
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("nashorn");
        String tokenResult = null;

        try {
            // JavaScriptの実行
            Object obj = engine.eval(String.format("(%s)", sb));
            // リフレクションでScriptObjectMirrorクラスの取得
            Class scriptClass = Class.forName("jdk.nashorn.api.scripting.ScriptObjectMirror");
            // リフレクションでキーセットを取得
            Object[] keys = ((java.util.Set)obj.getClass().getMethod("keySet").invoke(obj)).toArray();
            // リフレクションでgetメソッドを取得
            Method method_get = obj.getClass().getMethod("get", Class.forName("java.lang.Object"));

            Map<String, String> map = new HashMap<>();
            for(Object key : keys) {
                Object val = method_get.invoke(obj, key);
                map.put(key.toString(), val.toString());
            }

	    // mapの値のみを格納
            tokenResult = map.get("user_id");

            // ユーザーの存在を確認し、居なければ登録する
            Utils.isValidAuthInfo(tokenResult);

        } catch(Exception e) {
            e.printStackTrace();
        }

        // 認証処理のresponceCodeを返す
        return responseCode;
    }
}
