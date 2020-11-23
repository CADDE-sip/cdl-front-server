/*
    HLFGatewayInfoクラス

    COPYRIGHT FUJITSU LIMITED 2021
*/

package com.fujitsu.cdlfrontserver.usermanager;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.fujitsu.cdlfrontserver.api.Config;
import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Wallet;

public class HLFGatewayInfo {

    private static Path NetworkConfigPath = Paths.get(Config.get().cdlHlfNetworkConfigPath());

    private Gateway.Builder _gatewayBuilder;
    private Gateway _gateway;
    private Network _network;
    private Contract _contract;

    private HLFGatewayInfo() {
    }

    public HLFGatewayInfo(Wallet wallet, String username) throws Exception {
        this._gatewayBuilder = Gateway.createBuilder();
        this._gatewayBuilder.identity(wallet, username).networkConfig(NetworkConfigPath).discovery(false);

        // create a gateway connection
        this._gateway = this._gatewayBuilder.connect();

        // get the network and contract
        this._network = this._gateway.getNetwork(Config.get().channelName());
        this._contract = this._network.getContract(Config.get().cdlChainCodeName());
    }

    /**
     * @return _gatewayBuilder
     */
    public Gateway.Builder getGatewayBuilder() {
        return _gatewayBuilder;
    }

    /**
     * @param gatewayBuilder セットする gatewayBuilder
     */
    public void set_builder(Gateway.Builder gatewayBuilder) {
        this._gatewayBuilder = gatewayBuilder;
    }

    /**
     * @return _gateway
     */
    public Gateway getGateway() {
        return _gateway;
    }

    /**
     * @param gateway セットする _gateway
     */
    public void setGateway(Gateway gateway) {
        this._gateway = gateway;
    }

    /**
     * @return _network
     */
    public Network getNetwork() {
        return _network;
    }

    /**
     * @param network セットする _network
     */
    public void setNetwork(Network network) {
        this._network = network;
    }

    /**
     * @return _contract
     */
    public Contract getContract() {
        return _contract;
    }

    /**
     * @param contract セットする _contract
     */
    public void setContract(Contract contract) {
        this._contract = contract;
    }
}
