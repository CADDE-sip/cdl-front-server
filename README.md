# 本ドキュメントについて
　本ドキュメントは、来歴管理モジュールの利用方法に関するガイドラインです。<br>
  したがって、分野間データ連携基盤(CADDE)やコネクタの基本事項についての説明は割愛しています。<br>
  ※CADDEは情報・システム研究機構（国立情報学研究所）の商標です。<br>

# 分野間データ連携基盤: 来歴管理モジュール

## システム全体構成図
分野間データ連携基盤全体のシステム構成を下記に示します。
![Alt text](doc/png/system.png?raw=true "Title") <BR>
本手順で構築する構成を下記に示します。
![Alt text](doc/png/build_env.png?raw=true "Title")

## 前提条件
- 来歴管理モジュールの前提条件を示します。

  - 来歴管理モジュールは、利用者コネクタメイン、提供者コネクタメインと連携して利用します。
  - 来歴管理に関わる通信路と通信路の安全性は、適宜、来歴管理の外で利用者と提供者が準備するものとします。
  - 来歴管理モジュールと来歴管理エージェントとの間に、HTTP Proxyが存在しないことを前提とします。
  - 本ソフトウェアの品質は商用に耐えうるものではありません。動作で発生した結果について一切の責任を負いません。

- Linux 上での動作を前提とします。

  - Docker、Docker Compose、make、jq, curl が事前インストールされていることを前提とします。
  - 対応する Docker Version : 20.10
  - 対応する OS : 上記Dockerがサポートする OS

- 本手順では、作業フォルダとして以下を前提に記載します。
    -  /opt/sip2020-cdl/

- ブロックチェーン環境は、Hyperledger Fabric で台帳同期を実現します。
  - 本手順では、Hyperledger団体 が公開するOSSである「minifabric」 の利用を前提とします。<BR>
https://github.com/hyperledger-labs/minifabric
  - ブロックチェーン環境の構成は以下の通り。
    -  台帳同期機構（3組織分）
    -  ブロック生成機構（3組織分）
    -  来歴管理用認証局（3組織分）
<br><br>

# 事前準備
作業フォルダの環境変数を設定します。
```bash
export SIP_WORKDIR=/opt/sip2021-cdl
```
**sudoコマンドを使用する場合は、環境変数の値を取得するために-Eオプションをつけて各コマンドを実行すること。**

構築環境からインターネットへのアクセスにプロキシが必要な場合は、下記のように環境変数を適宜設定します。
```bash
export http_proxy={プロキシの接続先URL}
export https_proxy={プロキシの接続先URL}
export JAVA_OPTS='-Djdk.http.auth.tunneling.disabledSchemes="" -Dhttp.proxyHost={プロキシの接続先ホスト} -Dhttp.proxyPort={プロキシの接続先ポート} -Dhttps.proxyHost={プロキシの接続先ホスト} -Dhttps.proxyPort={プロキシの接続先ポート} -Dhttp.nonProxyHosts=127.0.0.1\|{ホストOSのIPアドレス}'
```
構築に必要となる資材を入手します。
```bash
mkdir -p ${SIP_WORKDIR}
cd ${SIP_WORKDIR}

#スマートコントラクト取得
git clone https://github.com/CADDE-sip/cdl-chaincode-go.git ${SIP_WORKDIR}/cdl-chaincode-go

#minifabric取得
mkdir -p ${SIP_WORKDIR}/minifabric && curl -sL https://tinyurl.com/yxa2q6yr --output ${SIP_WORKDIR}/minifabric/minifab && chmod +x ${SIP_WORKDIR}/minifabric/minifab

#来歴管理モジュール取得
git clone https://github.com/CADDE-sip/cdl-front-server.git ${SIP_WORKDIR}/cdl-front-server
```
# A.ブロックチェーン環境の構築手順
## minifabric の構築

1. 作業ディレクトリに移動
   ```bash
   cd ${SIP_WORKDIR}/minifabric
   ```

2. スマートコントラクトのビルドモジュールのコピー<BR>
ビルドしたスマートコントラクトのモジュールをminifabricの所定ディレクトリにコピーします。
   ```bash
   mkdir -p ${SIP_WORKDIR}/minifabric/vars/chaincode/cdl-chaincode/go
   cp -rf ${SIP_WORKDIR}/cdl-chaincode-go/* ${SIP_WORKDIR}/minifabric/vars/chaincode/cdl-chaincode/go
   ```

3. spec.yaml作成<BR>
minifabricの実行に必要な設定ファイルを ${SIP_WORKDIR}/minifabric/spec.yaml に作成します。 <BR>
定義内容は下記例の通り。

   ```yaml
   #  ${SIP_WORKDIR}/minifabric/spec.yaml の内容
   
   fabric:
     cas:
      - "ca.org01.cdl.com"
      - "ca.org02.cdl.com"
      - "ca.org03.cdl.com"
     peers:
      - "epcp01.org01.cdl.com"
      - "epcp02.org02.cdl.com"
      - "epcp03.org03.cdl.com"
     orderers:
      - "os01.common.cdl.com"
      - "os02.common.cdl.com"
      - "os03.common.cdl.com"
     settings:
       ca:
         FABRIC_LOGGING_SPEC: INFO
       peer:
         FABRIC_LOGGING_SPEC: INFO
       orderer:
         FABRIC_LOGGING_SPEC: INFO
     netname: cdl_network
     container_options: "--restart=always --log-opt max-size=14m --log-opt max-file=3"
   ```

4. ブロックチェーン起動

   ```bash
   ./minifab up -i 2.2.1 -s couchdb -c cdlchannel -n cdl-chaincode -l go -e 7050 -o org01.cdl.com -p ''
   
   ./minifab invoke -p '"init"'
   ```
   下記のコンテナが起動していることを確認します。<BR>
   ```bash
   docker ps --format "{{.Names}} {{.Status}}"
   
   dev-epcp01.org01.cdl.com-cdl-chaincode_1.0-15a1e68bf3ba536c45b874d3caebd58bdb35a415c1b09a500ff9a45350752385 Up 6 minutes
   dev-epcp02.org02.cdl.com-cdl-chaincode_1.0-15a1e68bf3ba536c45b874d3caebd58bdb35a415c1b09a500ff9a45350752385 Up 6 minutes
   dev-epcp03.org03.cdl.com-cdl-chaincode_1.0-15a1e68bf3ba536c45b874d3caebd58bdb35a415c1b09a500ff9a45350752385 Up 6 minutes
   cdl_network Up 8 minutes
   ca.org03.cdl.com Up 8 minutes
   ca.org02.cdl.com Up 8 minutes
   ca.org01.cdl.com Up 8 minutes
   os03.common.cdl.com Up 8 minutes
   os02.common.cdl.com Up 8 minutes
   os01.common.cdl.com Up 8 minutes
   epcp03.org03.cdl.com Up 8 minutes
   epcp02.org02.cdl.com Up 8 minutes
   epcp01.org01.cdl.com Up 8 minutes
   epcp03.org03.cdl.com.couchdb Up 8 minutes
   epcp02.org02.cdl.com.couchdb Up 8 minutes
   epcp01.org01.cdl.com.couchdb Up 8 minutes
   ```
   ブロックチェーン構築に失敗した場合は、<BR>
   下記コマンドを実行して環境をクリーンアップした後、手順「2. スマートコントラクトのビルドモジュールのコピー」から再度実施してください。<BR>
   ```bash
   ./minifab cleanup
   ```
   詳細は、下記のminifabricマニュアルを参照してください。<BR>
   https://github.com/hyperledger-labs/minifabric/tree/main/docs

5. トランザクション数変更手順（オプション）<BR>
本手順は、性能向上を目的とした手順であり、必須ではありません。<BR>
本手順を省力したい場合、「B.来歴管理モジュールの構築手順」を実施します。<BR><BR>
最新のブロック情報を取得します。
   ```bash
   ./minifab channelquery
   ```
   ブロック情報取得時、標準出力として「Channel configuration file」に表示されたファイル名を設定します。 <BR>
   また、環境情報を更新するためのブロック情報を作成します。
   ```bash
   configFile=./vars/cdlchannel_config.json
   
   jq ".channel_group.groups.Orderer.values.BatchSize.value.max_message_count" ${configFile}
   10
   
   jq ".channel_group.groups.Orderer.values.BatchSize.value.max_message_count=20" ${configFile} > ${configFile}_new
   
   mv  ${configFile}_new ${configFile}
   ```
   更新用ブロック情報をブロックチェーン環境に対して送信し、環境情報を変更します。
   ```bash
   ./minifab channelsign,channelupdate
   
   rm ${configFile}
   ```
   環境情報の反映結果を確認します。
   ```bash
   ./minifab channelquery
   
   jq ".channel_group.groups.Orderer.values.BatchSize.value.max_message_count" ${configFile}
   20
   ```

# B.来歴管理モジュールの構築手順

1. 作業ディレクトリ移動
   ```bash
   cd ${SIP_WORKDIR}/cdl-front-server
   ```

2. ブロックチェーン環境への接続情報ファイルのコピー<BR>
ブロックチェーン環境への接続情報ファイルを所定ディレクトリにコピーします。
   ```bash
   cp ${SIP_WORKDIR}/minifabric/vars/profiles/cdlchannel_connection_for_javasdk.yaml ${SIP_WORKDIR}/cdl-front-server/connection.yaml
   ```

3. 来歴管理モジュール起動

   ```bash
   make all
   ```

4. 来歴管理モジュール確認 <BR>
下記コンテナが起動していることを確認します。
   ```bash
   docker ps --format "{{.Names}} {{.Status}}" | grep cdlfrontserver
   cdlfrontserver Up About a minute
   ```

# 来歴管理モジュールの利用
来歴管理 機能設計書のAPI仕様に準じます。<BR>
下記のコマンド実行例は、登録済の全履歴の検索方法を示します。

   ```bash
   curl -v -X POST http://localhost:3000/v2/searchevents -H "Content-Type:application/json" -d '{"selector": {"cdldatamodelversion":"2.0"}}'
   ```

# 停止手順
## 来歴管理モジュールの停止
```bash
cd ${SIP_WORKDIR}/cdl-front-server
make stop
```

## ブロックチェーン環境の停止
```bash
cd ${SIP_WORKDIR}/minifabric
./minifab cleanup
```
# LICENSE
[MIT](./LICENSE.md)

# 謝辞
本研究の一部は、内閣府総合科学技術・イノベーション会議の「SIP/ビッグデータ・AIを活用したサイバー空間基盤技術」（管理法人：国立研究開発法人新エネルギー・産業技術総合開発機構）によって実施されました。
