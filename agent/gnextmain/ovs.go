// Copyright 2024 The GNEXT Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package gnextmain

import (
	"bytes"
	"fmt"
	"os"
	"regexp"
	"sort"
	"strconv"
	"strings"
	"text/template"

	"go.uber.org/zap"
)

func OvsBridgeAdd(bridge string) error {
	err := OvsBridgeDel(bridge)
	if err != nil {
		return err
	}
	cmd := fmt.Sprintf("ovs-vsctl --no-syslog add-br %s", bridge)
	if err = ExecuteCmdRun(G.config.CmdTimeout, cmd); err != nil {
		return err
	}
	cmd = fmt.Sprintf("ovs-vsctl set bridge %s protocols=OpenFlow10,OpenFlow11,OpenFlow13", bridge)
	return ExecuteCmdRun(G.config.CmdTimeout, cmd)
}

func OvsBridgeDel(bridge string) error {
	if bridge == "" {
		return nil
	}
	cmd := fmt.Sprintf("ovs-vsctl --no-syslog --if-exist del-br %s", bridge)
	return ExecuteCmdRun(G.config.CmdTimeout, cmd)
}

func OvsBridgeNicExists(bridge string, nic string) (bool, error) {
	cmd := fmt.Sprintf("ovs-vsctl list-ports %s", bridge)
	out, err := ExecuteCmdOutput(G.config.CmdTimeout, cmd)
	if err != nil {
		return false, err
	}
	for _, port := range bytes.Split(out, []byte("\n")) {
		if string(port) == nic {
			return true, nil
		}
	}
	return false, nil
}

func OvsNicAdd(bridge string, nic string, trunkMode bool, ofport string, vlanid string) error {
	var cmd string
	if trunkMode {
		cmd = fmt.Sprintf("ovs-vsctl --no-syslog -- --may-exist add-port %s %s vlan_mode=trunk", bridge, nic)
	} else if ofport != "" && vlanid != "" {
		cmd = fmt.Sprintf("ovs-vsctl --no-syslog --may-exist add-port %s %s tag=%s -- set Interface %s ofport_request=%s", bridge, nic, vlanid, nic, ofport)
	} else {
		cmd = fmt.Sprintf("ovs-vsctl --no-syslog -- --may-exist add-port %s %s", bridge, nic)
	}
	return ExecuteCmdRun(G.config.CmdTimeout, cmd)
}

func OvsNoFlood(bridge string, nic string) error {
	cmd := fmt.Sprintf("ovs-ofctl mod-port %s %s no-flood", bridge, nic)
	return ExecuteCmdRun(G.config.CmdTimeout, cmd)
}

func OvsVpcNicAdd(bridge string, nic string, vlanid string) error {
	cmd := fmt.Sprintf("ovs-vsctl --no-syslog -- --may-exist add-port %s %s tag=%s", bridge, nic, vlanid)
	return ExecuteCmdRun(G.config.CmdTimeout, cmd)
}

func OvsMonNicAdd(bridge string, nic string) error {
	cmd := fmt.Sprintf("ovs-vsctl --no-syslog -- --may-exist add-port %s %s", bridge, nic)
	return ExecuteCmdRun(G.config.CmdTimeout, cmd)
}

func OvsNicDel(bridge string, nic string) error {
	cmd := fmt.Sprintf("ovs-vsctl --no-syslog --if-exist del-port %s %s", bridge, nic)
	return ExecuteCmdRun(G.config.CmdTimeout, cmd)
}

func OvsFlowsLoad(bridge string, flows string) (err error) {
	var f *os.File
	if f, err = os.CreateTemp("", "*.flows"); err != nil {
		G.logger.Fatal("create temp file failed", zap.Error(err))
	}
	defer func() {
		f.Close()
		os.Remove(f.Name())
	}()
	if _, err = f.WriteString(flows); err != nil {
		return err
	}
	cmd := fmt.Sprintf("ovs-ofctl add-flows %s %s", bridge, f.Name())
	if err = ExecuteCmdRun(G.config.CmdTimeout, cmd); err != nil {
		return err
	}
	return nil
}

func OvsWanBridgeFlowInit() error {
	initWanFlows := "" +
		"add table=0,priority=200,in_port=monwportb,icmp,icmp_type=8,actions=output:{{.WAN_NIC}}\n" +
		fmt.Sprintf("add table=0,priority=200,in_port={{.WAN_NIC}},icmp,icmp_type=0,nw_src=%s,actions=ct,output:monwportb\n", G.config.WanTestIp) +

		"add table=0,priority=100,in_port={{.WAN_NIC}},arp,arp_tpa={{.DEFAULT_EIP}},arp_op=1,actions=move:NXM_OF_ETH_SRC[]->NXM_OF_ETH_DST[],set_field:{{.WAN_MAC}}->eth_src,load:0x2->NXM_OF_ARP_OP[],move:NXM_NX_ARP_SHA[]->NXM_NX_ARP_THA[],set_field:{{.WAN_MAC}}->arp_sha,move:NXM_OF_ARP_SPA[]->NXM_OF_ARP_TPA[],set_field:{{.DEFAULT_EIP}}->arp_spa,output:in_port\n" +

		"add table=0,priority=90,in_port={{.WAN_NIC}},ip,nw_dst={{.DEFAULT_EIP}},ct_state=-trk,actions=ct(table=20)\n" +
		"add table=0,priority=0,actions=drop\n" +

		"add table=10,priority=100,ip,ct_state=+new,actions=ct(commit,nat(src={{.DEFAULT_EIP}})),output:{{.WAN_NIC}}\n" +
		"add table=10,priority=100,ip,ct_state=+est,actions=ct(nat),output:{{.WAN_NIC}}\n" +
		"add table=10,priority=0,actions=drop\n" +
		"add table=20,priority=0,actions=drop\n" +

		"add table=30,priority=200,ip,nw_dst={{.WAN_GW_IP}},actions=drop\n" +
		"add table=30,priority=0,action=resubmit(,40)\n" +
		"add table=40,priority=0,actions=output:{{.WAN_NIC}}\n"

	tmpl, err := template.New("INITWANFLOWS").Parse(initWanFlows)
	if err != nil {
		G.logger.Fatal("parse template failed", zap.Error(err))
	}

	data := map[string]string{
		"DEFAULT_EIP": G.config.DefaultEip,
		"WAN_MAC":     G.config.WanNicMac,
		"WAN_NIC":     G.config.WanNic,
		"WAN_GW_IP":   G.config.WanGwIp,
		"WAN_GW_MAC":  G.config.WanGwMac,
	}
	if G.config.EipGwIp != "" {
		initWanFlows += "add table=30,priority=200,ip,nw_dst={{.EIP_GW_IP}},actions=drop\n"
		data["EIP_GW_IP"] = G.config.EipGwIp
	}
	var resultBuffer bytes.Buffer

	if err = tmpl.Execute(&resultBuffer, data); err != nil {
		G.logger.Fatal("parse template failed", zap.Error(err))
	}

	return OvsFlowsLoad(G.config.WanBridge, resultBuffer.String())
}

func OvsWanInit() (err error) {
	if !G.config.L3Mode {
		return nil
	}

	if err = OvsBridgeAdd(G.config.WanBridge); err != nil {
		return err
	}

	if err = osNicAddrFlush(G.config.WanNic); err != nil {
		return err
	}
	if err = osNicUp(G.config.WanNic, ""); err != nil {
		return err
	}

	if err = OvsNicAdd(G.config.WanBridge, G.config.WanNic, false, "", ""); err != nil {
		return err
	}

	monWanNics := MonVethPair(false)
	monWanNic := monWanNics[0]
	monWanBrNic := monWanNics[1]
	if osNicExists(monWanNic, "") {
		if err = osNicDel(monWanNic, ""); err != nil {
			G.logger.Error("cannot delete nic", zap.Error(err))
			return err
		}
	}
	if err = osVethAdd(monWanNic, monWanBrNic); err != nil {
		G.logger.Error("cannot add veth", zap.Error(err))
		return err
	}
	if err = OvsMonNicAdd(G.config.WanBridge, monWanBrNic); err != nil {
		G.logger.Error("cannot add monitor nic", zap.Error(err))
		return err
	}
	if err = osNicUp(monWanNic, ""); err != nil {
		G.logger.Error("cannot up nic", zap.Error(err))
		return err
	}
	if err = osNicUp(monWanBrNic, ""); err != nil {
		G.logger.Error("cannot up nic", zap.Error(err))
		return err
	}

	return OvsWanBridgeFlowInit()
}

func OvsLanInit() (err error) {
	if err = OvsBridgeAdd(G.config.LanBridge); err != nil {
		return err
	}
	if err = osNicUp(G.config.LanNic, ""); err != nil {
		return err
	}
	if err = OvsNicAdd(G.config.LanBridge, G.config.LanNic, true, "", ""); err != nil {
		return err
	}
	monLanNics := MonVethPair(true)
	monLanNic := monLanNics[0]
	monLanBrNic := monLanNics[1]
	if osNicExists(monLanNic, "") {
		if err = osNicDel(monLanNic, ""); err != nil {
			G.logger.Error("cannot delete nic", zap.Error(err))
			return err
		}
	}
	if err = osVethAdd(monLanNic, monLanBrNic); err != nil {
		G.logger.Error("cannot add veth", zap.Error(err))
		return err
	}
	if err = OvsMonNicAdd(G.config.LanBridge, monLanBrNic); err != nil {
		G.logger.Error("cannot add monitor nic", zap.Error(err))
		return err
	}
	if err = osNicUp(monLanNic, ""); err != nil {
		G.logger.Error("cannot up nic", zap.Error(err))
		return err
	}
	if err = osNicUp(monLanBrNic, ""); err != nil {
		G.logger.Error("cannot up nic", zap.Error(err))
		return err
	}
	if G.config.VmMode {
		initLanFlows := "" +

			fmt.Sprintf("add table=0,priority=200,in_port=%s,udp,udp_src=0xfffe,ip_src=0.0.0.0,actions=strip_vlan,output:monlportb\n", G.config.LanNic) +
			"add table=0,priority=190,in_port=monlportb,action=drop\n" +
			"add table=0,priority=0,actions=resubmit(,10)\n" +

			"add table=10,priority=0,actions=NORMAL\n" +

			"add table=71,priority=110,ct_state=+trk,actions=ct_clear,resubmit(,71)\n" +

			"add table=71,priority=80,udp,tp_src=68,tp_dst=67,actions=resubmit(,73)\n" +

			"add table=71,priority=70,udp,tp_src=67,tp_dst=68,actions=drop\n" +

			"add table=71,priority=60,tcp,nw_dst=169.254.169.254,tp_dst=80,actions=resubmit(,73)\n" +

			"add table=71,priority=10,actions=drop\n" +
			"add table=72,priority=50,ct_state=+inv+trk,actions=drop\n" +
			"add table=72,priority=50,ct_mark=0x1,actions=drop\n" +
			"add table=72,priority=50,ct_state=+est-rel+rpl,ct_mark=0,actions=resubmit(,74)\n" +
			"add table=72,priority=50,ct_state=-new-est+rel-inv,ct_mark=0,actions=resubmit(,74)\n" +

			"add table=72,priority=0,,actions=drop\n" +
			"add table=73,priority=90,ct_state=+new-est,ip,actions=ct(commit,zone=NXM_NX_REG6[0..15]),resubmit(,74)\n" +
			"add table=73,priority=80,actions=resubmit(,74)\n" +
			"add table=75,priority=0,actions=resubmit(,72)\n" +
			"add table=81,priority=100,arp,actions=strip_vlan,resubmit(,84)\n" +
			"add table=81,priority=90,udp,tp_src=67,tp_dst=68,actions=strip_vlan,resubmit(,84)\n" +
			"add table=81,priority=90,tcp,nw_src=169.254.169.254,tp_src=80,actions=strip_vlan,resubmit(,84)\n" +
			"add table=81,priority=70,ct_state=-trk,ip,actions=ct(table=85,zone=NXM_NX_REG6[0..15])\n" +
			"add table=81,priority=60,ct_state=+trk,actions=resubmit(,85)\n" +
			"add table=81,priority=0,actions=drop\n" +
			"add table=82,priority=100,ct_state=+inv+trk,actions=drop\n" +
			"add table=82,priority=100,ct_mark=0x1,actions=drop\n" +
			"add table=82,priority=50,ct_state=+est-rel+rpl,ct_mark=0,actions=strip_vlan,resubmit(,84)\n" +
			"add table=82,priority=50,ct_state=-new-est+rel-inv,ct_mark=0,actions=strip_vlan,resubmit(,84)\n" +

			"add table=82,priority=0,actions=drop\n" +
			"add table=85,priority=0,actions=pop_vlan,resubmit(,82)\n" +

			"add table=74,priority=110,ct_state=+trk,ct_label[0..47]=0x0,actions=resubmit(,76)\n" +

			"add table=74,priority=100,ct_state=+trk,actions=move:NXM_NX_CT_LABEL[0..47]->NXM_OF_ETH_DST[],resubmit(,76)\n" +
			"add table=74,priority=0,actions=resubmit(,76)\n" +
			"add table=84,priority=0,actions=output:NXM_NX_REG5[]\n" +

			"add table=76,priority=0,actions=NORMAL\n"
		if err = OvsFlowsLoad(G.config.LanBridge, initLanFlows); err != nil {
			return err
		}

		cmd := fmt.Sprintf("ovs-ofctl -O OpenFlow13 add-flow %s table=0,priority=200,in_port=monlportb,udp,udp_src=0xfffe,actions=push_vlan:0x8100,move:NXM_OF_UDP_DST[0..11]->NXM_OF_VLAN_TCI[0..11],output:%s", G.config.LanBridge, G.config.LanNic)
		if err = ExecuteCmdRun(G.config.CmdTimeout, cmd); err != nil {
			return err
		}
	}
	if G.config.L3Mode {

		if strings.Contains(G.config.WanNic, ".") {
			parts := strings.Split(G.config.WanNic, ".")
			if len(parts) == 2 && parts[0] == G.config.LanNic {

				cmd := fmt.Sprintf("ovs-ofctl add-flow %s table=0,priority=999,in_port=%s,dl_vlan=%s,actions=drop", G.config.LanBridge, parts[0], parts[1])
				if err = ExecuteCmdRun(G.config.CmdTimeout, cmd); err != nil {
					return err
				}
			}
		}
	}
	return nil
}

func OvsWanVpcAdd(wanVpcNic string, wanVpcNsNic string, wanVpcNicIp string, wanVpcNsNicIp string, vlanid string) (err error) {
	if err = OvsNicAdd(G.config.WanBridge, wanVpcNic, false, "", ""); err != nil {
		return nil
	}
	ns := GetNsName(vlanid)
	wanVpcNicMac, _ := osNicMac(wanVpcNic, "")
	wanVpcNsNicMac, _ := osNicMac(wanVpcNsNic, ns)
	addVpcWanFlows := "" +

		"add table=0,priority=105,in_port={{.WAN_VPC_NIC}},arp,arp_tpa={{.WAN_VPC_NIC_IP}},arp_op=1,actions=move:NXM_OF_ETH_SRC[]->NXM_OF_ETH_DST[],set_field:{{.WAN_VPC_NIC_MAC}}->eth_src,load:0x2->NXM_OF_ARP_OP[],move:NXM_NX_ARP_SHA[]->NXM_NX_ARP_THA[],set_field:{{.WAN_VPC_NIC_MAC}}->arp_sha,move:NXM_OF_ARP_SPA[]->NXM_OF_ARP_TPA[],set_field:{{.WAN_VPC_NIC_IP}}->arp_spa,output:in_port\n" +

		"add table=0,priority=104,in_port={{.WAN_VPC_NIC}},icmp,nw_dst={{.WAN_VPC_NIC_IP}},icmp_type=8,icmp_code=0 actions=move:NXM_OF_IP_SRC[]->NXM_OF_IP_DST[],set_field:{{.WAN_VPC_NIC_IP}}->ip_src,move:NXM_OF_ETH_SRC[]->NXM_OF_ETH_DST[],set_field:{{.WAN_VPC_NIC_MAC}}->eth_src,set_field:0->icmp_type,output:in_port\n" +

		"add table=0,priority=90,in_port={{.WAN_VPC_NIC}},ip,nw_src={{.WAN_VPC_NS_NIC_IP}},ct_state=-trk,actions=set_field:{{.WAN_MAC}}->eth_src,set_field:{{.GW_MAC}}->eth_dst,ct(table=10)\n" +

		"add table=20,priority=100,ip,ct_state=+est,ct_nw_src={{.WAN_VPC_NS_NIC_IP}},actions=set_field:{{.WAN_VPC_NS_NIC_MAC}}->eth_dst,ct(nat),output:{{.WAN_VPC_NIC}}\n"

	var tmpl *template.Template
	if tmpl, err = template.New("ADDVPCWANFLOWS").Parse(addVpcWanFlows); err != nil {
		return nil
	}

	data := map[string]string{
		"WAN_VPC_NIC":        wanVpcNic,
		"WAN_VPC_NIC_IP":     wanVpcNicIp,
		"WAN_VPC_NS_NIC_IP":  wanVpcNsNicIp,
		"WAN_VPC_NIC_MAC":    wanVpcNicMac,
		"WAN_VPC_NS_NIC_MAC": wanVpcNsNicMac,
		"WAN_MAC":            G.config.WanNicMac,
		"GW_MAC":             G.config.WanGwMac,
	}
	var resultBuffer bytes.Buffer

	if err = tmpl.Execute(&resultBuffer, data); err != nil {
		G.logger.Fatal("parse template failed", zap.Error(err))
	}

	return OvsFlowsLoad(G.config.WanBridge, resultBuffer.String())
}

func OvsGetOfport(bridge string, nic string) (ofport string, err error) {
	cmd := fmt.Sprintf("ovs-vsctl get Interface %s ofport", nic)
	var out []byte
	if out, err = ExecuteCmdOutput(G.config.CmdTimeout, cmd); err != nil {
		return "", err
	}
	return strings.TrimSpace(string(out)), nil
}

func OvsWanVpcDel(wanVpcNic string, wanVpcNicIp string, wanVpcNsNicIp string) (err error) {
	var ofport string
	var delVpcWanFlows string
	if ofport, err = OvsGetOfport(G.config.WanBridge, wanVpcNic); err != nil {
		G.logger.Error("ovsgetofport failed", zap.Error(err))
		delVpcWanFlows = "delete table=20,ip,ct_state=+est,ct_nw_src={{.WAN_VPC_NS_NIC_IP}}\n"
	} else {
		delVpcWanFlows = "" +
			"delete table=0,in_port={{.WAN_VPC_NIC_OFPORT}},arp,arp_tpa={{.WAN_VPC_NIC_IP}},arp_op=1\n" +
			"delete table=0,in_port={{.WAN_VPC_NIC_OFPORT}},icmp,nw_dst={{.WAN_VPC_NIC_IP}},icmp_type=8,icmp_code=0\n" +
			"delete table=0,in_port={{.WAN_VPC_NIC_OFPORT}},ip,nw_src={{.WAN_VPC_NS_NIC_IP}},ct_state=-trk\n" +
			"delete table=20,ip,ct_state=+est,ct_nw_src={{.WAN_VPC_NS_NIC_IP}}\n"
	}

	var tmpl *template.Template
	if tmpl, err = template.New("DELVPCWANFLOWS").Parse(delVpcWanFlows); err != nil {
		return nil
	}

	data := map[string]string{
		"WAN_VPC_NIC_OFPORT": ofport,
		"WAN_VPC_NIC_IP":     wanVpcNicIp,
		"WAN_VPC_NS_NIC_IP":  wanVpcNsNicIp,
	}
	var resultBuffer bytes.Buffer

	if err = tmpl.Execute(&resultBuffer, data); err != nil {
		G.logger.Fatal("parse template failed", zap.Error(err))
	}

	if err = OvsFlowsLoad(G.config.WanBridge, resultBuffer.String()); err != nil {
		return err
	}

	return OvsNicDel(G.config.WanBridge, wanVpcNic)
}

func OvsDvrSubnetAdd(routeIP string, dnsIP string, lanNic string, vlanid string) (err error) {
	lanVpcNics := LanVethPair(vlanid)
	lanVpcNic := lanVpcNics[0]
	addDvrSubnetLanFlows := "" +

		"table=0,priority=140,arp,arp_tpa={{.DNS_IP}},arp_op=1,dl_vlan={{.VLANID}},in_port={{.LAN_NIC}},action=drop\n" +

		"table=0,priority=130,arp,arp_tpa={{.ROUTE_IP}},arp_op=1,dl_vlan={{.VLANID}},in_port={{.LAN_NIC}},action=drop\n" +

		"table=71,priority=106,arp,arp_tpa={{.DNS_IP}},arp_op=1,reg6={{.VLANID}},action=output:{{.LAN_VPC_NIC}}\n" +

		"table=71,priority=105,arp,arp_tpa={{.ROUTE_IP}},arp_op=1,reg6={{.VLANID}},action=output:{{.LAN_VPC_NIC}}\n"

	var tmpl *template.Template
	if tmpl, err = template.New("ADDDVRSUBNETLANFLOWS").Parse(addDvrSubnetLanFlows); err != nil {
		G.logger.Fatal("parse template failed", zap.Error(err))
	}

	data := map[string]string{
		"ROUTE_IP":    routeIP,
		"DNS_IP":      dnsIP,
		"VLANID":      vlanid,
		"LAN_NIC":     lanNic,
		"LAN_VPC_NIC": lanVpcNic,
	}
	var resultBuffer bytes.Buffer

	if err = tmpl.Execute(&resultBuffer, data); err != nil {
		G.logger.Fatal("parse template failed", zap.Error(err))
	}

	return OvsFlowsLoad(G.config.LanBridge, resultBuffer.String())
}

func OvsDvrSubnetDel(routeIP string, dnsIP string, lanNic string, vlanid string) (err error) {
	delDvrSubnetLanFlows := "" +
		"delete table=0,arp,arp_tpa={{.DNS_IP}},arp_op=1,dl_vlan={{.VLANID}},in_port={{.LAN_NIC}}\n" +
		"delete table=0,arp,arp_tpa={{.ROUTE_IP}},arp_op=1,dl_vlan={{.VLANID}},in_port={{.LAN_NIC}}\n" +
		"delete table=71,arp,arp_tpa={{.DNS_IP}},arp_op=1,reg6={{.VLANID}}\n" +
		"delete table=71,arp,arp_tpa={{.ROUTE_IP}},arp_op=1,reg6={{.VLANID}}\n"

	var tmpl *template.Template
	if tmpl, err = template.New("DELDVRSUBNETLANFLOWS").Parse(delDvrSubnetLanFlows); err != nil {
		G.logger.Fatal("parse template failed", zap.Error(err))
	}

	data := map[string]string{
		"ROUTE_IP": routeIP,
		"DNS_IP":   dnsIP,
		"VLANID":   vlanid,
		"LAN_NIC":  lanNic,
	}
	var resultBuffer bytes.Buffer

	if err = tmpl.Execute(&resultBuffer, data); err != nil {
		G.logger.Fatal("parse template failed", zap.Error(err))
	}

	return OvsFlowsLoad(G.config.LanBridge, resultBuffer.String())
}

func OvsWanPortBind(eip string, vlanid string) (err error) {
	ns := GetNsName(vlanid)
	vethNics := WanVethPair(vlanid)
	wanVpcNic := vethNics[0]
	wanVpcNsNic := vethNics[1]
	wanVpcNsNicMac, _ := osNicMac(wanVpcNsNic, ns)
	portBindWanFlows := "" +

		"add table=0,priority=100,in_port={{.WAN_NIC}},arp,arp_tpa={{.EIP}},arp_op=1,actions=move:NXM_OF_ETH_SRC[]->NXM_OF_ETH_DST[],set_field:{{.WAN_MAC}}->eth_src,load:0x2->NXM_OF_ARP_OP[]," +
		"move:NXM_NX_ARP_SHA[]->NXM_NX_ARP_THA[],set_field:{{.WAN_MAC}}->arp_sha,move:NXM_OF_ARP_SPA[]->NXM_OF_ARP_TPA[],set_field:{{.EIP}}->arp_spa,output:in_port\n" +

		"add table=0,priority=90,in_port={{.WAN_NIC}},ip,nw_dst={{.EIP}},actions=set_field:{{.WAN_VPC_NS_NIC_MAC}}->eth_dst,output:{{.WAN_VPC_NIC}}\n" +

		"add table=0,priority=90,in_port={{.WAN_VPC_NIC}},ip,nw_src={{.EIP}},actions=set_field:{{.WAN_MAC}}->eth_src,set_field:{{.GW_MAC}}->eth_dst,resubmit(,30)\n" +

		"add table=30,priority=200,ip,nw_dst={{.EIP}},actions=set_field:{{.WAN_VPC_NS_NIC_MAC}}->eth_dst,output:{{.WAN_VPC_NIC}}\n"

	gwMac := G.config.WanGwMac
	if G.config.EipGwMac != "" {
		gwMac = G.config.EipGwMac
	}

	var tmpl *template.Template
	if tmpl, err = template.New("PORTBINDWANFLOWS").Parse(portBindWanFlows); err != nil {
		G.logger.Fatal("parse template failed", zap.Error(err))
	}

	data := map[string]string{
		"WAN_NIC":            G.config.WanNic,
		"WAN_MAC":            G.config.WanNicMac,
		"WAN_VPC_NIC":        wanVpcNic,
		"WAN_VPC_NS_NIC_MAC": wanVpcNsNicMac,
		"GW_MAC":             gwMac,
		"EIP":                eip,
	}
	var resultBuffer bytes.Buffer

	if err = tmpl.Execute(&resultBuffer, data); err != nil {
		G.logger.Fatal("parse template failed", zap.Error(err))
	}

	return OvsFlowsLoad(G.config.WanBridge, resultBuffer.String())
}

func OvsWanPortUnbind(eip string, vlanid string) (err error) {
	vethNics := WanVethPair(vlanid)
	wanVpcNic := vethNics[0]
	portUnbindWanFlows := "" +
		"delete table=0,in_port={{.WAN_NIC}},arp,arp_tpa={{.EIP}},arp_op=1\n" +
		"delete table=0,in_port={{.WAN_NIC}},ip,nw_dst={{.EIP}}\n" +
		"delete table=0,in_port={{.WAN_VPC_NIC}},ip,nw_src={{.EIP}}\n" +
		"delete table=30,ip,nw_dst={{.EIP}}\n"

	var tmpl *template.Template
	if tmpl, err = template.New("PORTUNBINDWANFLOWS").Parse(portUnbindWanFlows); err != nil {
		G.logger.Fatal("parse template failed", zap.Error(err))
	}

	data := map[string]string{
		"WAN_NIC":     G.config.WanNic,
		"WAN_VPC_NIC": wanVpcNic,
		"EIP":         eip,
	}
	var resultBuffer bytes.Buffer

	if err = tmpl.Execute(&resultBuffer, data); err != nil {
		G.logger.Fatal("parse template failed", zap.Error(err))
	}

	return OvsFlowsLoad(G.config.WanBridge, resultBuffer.String())
}

func OvsVmAdd(nic string, vlanid string, mac string, ip string, ofport string, speed uint32, cidr string) (err error) {

	if speed == 0 {
		speed = G.config.DefaultSpeed
	}
	cmd := fmt.Sprintf("ovs-ofctl -O OpenFlow13 dump-meters %s|grep meter=%s", G.config.LanBridge, ofport)
	if err = ExecuteShCmdRun(G.config.CmdTimeout, cmd); err != nil {
		cmd = fmt.Sprintf("ovs-ofctl -O OpenFlow13 add-meter %s meter=%s,kbps,burst,stats,bands=type=drop,rate=%d,burst_size=%d", G.config.LanBridge, ofport, speed, speed/2)
		if err = ExecuteCmdRun(G.config.CmdTimeout, cmd); err != nil {
			return err
		}
	}

	cmd = fmt.Sprintf("ovs-ofctl -O OpenFlow13 add-flow %s table=71,priority=55,ip,reg5=%s,in_port=%s,dl_src=%s,nw_src=%s,nw_dst=%s,actions=ct(table=75,zone=NXM_NX_REG6[0..15])", G.config.LanBridge, ofport, nic, mac, ip, cidr)
	if err = ExecuteCmdRun(G.config.CmdTimeout, cmd); err != nil {
		return err
	}

	cmd = fmt.Sprintf("ovs-ofctl -O OpenFlow13 add-flow %s table=71,priority=54,ip,reg5=%s,in_port=%s,dl_src=%s,nw_src=%s,actions=meter:%s,ct(table=75,zone=NXM_NX_REG6[0..15])", G.config.LanBridge, ofport, nic, mac, ip, ofport)
	if err = ExecuteCmdRun(G.config.CmdTimeout, cmd); err != nil {
		return err
	}

	cmd = fmt.Sprintf("ovs-ofctl -O OpenFlow13 add-flow %s table=84,priority=55,ip,dl_dst=%s,nw_src=%s,actions=output:%s", G.config.LanBridge, mac, cidr, ofport)
	if err = ExecuteCmdRun(G.config.CmdTimeout, cmd); err != nil {
		return err
	}

	cmd = fmt.Sprintf("ovs-ofctl -O OpenFlow13 add-flow %s table=84,priority=54,ip,dl_dst=%s,actions=meter:%s,output:%s", G.config.LanBridge, mac, ofport, ofport)
	if err = ExecuteCmdRun(G.config.CmdTimeout, cmd); err != nil {
		return err
	}
	VmAddLanFlows := "" +

		"add table=10,priority=115,arp,arp_tpa={{.IP}},arp_op=1,dl_vlan={{.VLANID}},actions=move:NXM_OF_ETH_SRC[]->NXM_OF_ETH_DST[],set_field:{{.MAC}}->eth_src,load:0x2->NXM_OF_ARP_OP[],move:NXM_NX_ARP_SHA[]->NXM_NX_ARP_THA[],set_field:{{.MAC}}->arp_sha,move:NXM_OF_ARP_SPA[]->NXM_OF_ARP_TPA[],set_field:{{.IP}}->arp_spa,output:in_port\n" +

		"add table=10,priority=114,arp,arp_tpa={{.IP}},arp_op=1,reg6={{.VLANID}},actions=move:NXM_OF_ETH_SRC[]->NXM_OF_ETH_DST[],set_field:{{.MAC}}->eth_src,load:0x2->NXM_OF_ARP_OP[],move:NXM_NX_ARP_SHA[]->NXM_NX_ARP_THA[],set_field:{{.MAC}}->arp_sha,move:NXM_OF_ARP_SPA[]->NXM_OF_ARP_TPA[],set_field:{{.IP}}->arp_spa,output:in_port\n" +

		"add table=10,priority=100,in_port=\"{{.VNET}}\",actions=,load:{{.OFPORT}}->NXM_NX_REG5[],load:{{.VLANID}}->NXM_NX_REG6[],resubmit(,71)\n" +

		"add table=10,priority=90,dl_dst={{.MAC}},actions=load:{{.OFPORT}}->NXM_NX_REG5[],load:{{.VLANID}}->NXM_NX_REG6[],resubmit(,81)\n" +

		"add table=71,priority=100,arp,arp_tpa={{.IP}},arp_op=1,reg6={{.VLANID}},actions=move:NXM_OF_ETH_SRC[]->NXM_OF_ETH_DST[],set_field:{{.MAC}}->eth_src,load:0x2->NXM_OF_ARP_OP[],move:NXM_NX_ARP_SHA[]->NXM_NX_ARP_THA[],set_field:{{.MAC}}->arp_sha,move:NXM_OF_ARP_SPA[]->NXM_OF_ARP_TPA[],set_field:{{.IP}}->arp_spa,output:in_port\n" +

		"add table=71,priority=95,arp,reg5={{.OFPORT}},in_port=\"{{.VNET}}\",dl_src={{.MAC}},arp_spa={{.IP}},actions=resubmit(,74)\n" +

		"add table=71,priority=50,ip,reg5={{.OFPORT}},in_port=\"{{.VNET}}\",dl_src={{.MAC}},nw_src={{.IP}},actions=ct(table=75,zone=NXM_NX_REG6[0..15])\n" +

		"add table=73,priority=100,reg6={{.VLANID}},dl_dst={{.MAC}},actions=load:{{.OFPORT}}->NXM_NX_REG5[],resubmit(,81)\n" +

		"add table=76,priority=100,dl_dst={{.MAC}},actions=output:{{.OFPORT}}\n"
	var tmpl *template.Template
	if tmpl, err = template.New("VMADDLANFLOWS").Parse(VmAddLanFlows); err != nil {
		G.logger.Fatal("parse template failed", zap.Error(err))
	}

	data := map[string]string{
		"VNET":   nic,
		"OFPORT": ofport,
		"VLANID": vlanid,
		"MAC":    mac,
		"IP":     ip,
	}
	var resultBuffer bytes.Buffer
	if err = tmpl.Execute(&resultBuffer, data); err != nil {
		G.logger.Fatal("parse template failed", zap.Error(err))
	}
	return OvsFlowsLoad(G.config.LanBridge, resultBuffer.String())
}

func OvsVmDel(nic string, vlanid string, mac string, ip string, ofport string, speed uint32, cidr string) (err error) {
	if speed != 0 && cidr != "" {
		cmd := fmt.Sprintf("ovs-ofctl -O OpenFlow13 del-flows %s table=84,ip,dl_dst=%s", G.config.LanBridge, mac)
		if err = ExecuteCmdRun(G.config.CmdTimeout, cmd); err != nil {
			return err
		}
		cmd = fmt.Sprintf("ovs-ofctl -O OpenFlow13 del-flows %s table=71,ip,reg5=%s", G.config.LanBridge, ofport)
		if err = ExecuteCmdRun(G.config.CmdTimeout, cmd); err != nil {
			return err
		}
		cmd = fmt.Sprintf("ovs-ofctl -O OpenFlow13 del-meter %s meter=%s", G.config.LanBridge, ofport)
		if err = ExecuteCmdRun(G.config.CmdTimeout, cmd); err != nil {
			return err
		}
	}
	VmDelLanFlows := "" +
		"delete table=10,arp,arp_tpa={{.IP}},arp_op=1,dl_vlan={{.VLANID}}\n" +
		"delete table=10,arp,arp_tpa={{.IP}},arp_op=1,reg6={{.VLANID}}\n" +
		"delete table=10,in_port={{.OFPORT}}\n" +
		"delete table=10,dl_dst={{.MAC}}\n" +
		"delete table=71,arp,arp_tpa={{.IP}},arp_op=1,reg6={{.VLANID}}\n" +
		"delete table=71,in_port={{.OFPORT}}\n" +
		"delete table=73,dl_dst={{.MAC}}\n" +
		"delete table=76,dl_dst={{.MAC}}\n"
	var tmpl *template.Template
	if tmpl, err = template.New("VMDELLANFLOWS").Parse(VmDelLanFlows); err != nil {
		G.logger.Fatal("parse template failed", zap.Error(err))
	}

	data := map[string]string{
		"VNET":   nic,
		"OFPORT": ofport,
		"VLANID": vlanid,
		"MAC":    mac,
		"IP":     ip,
	}
	var resultBuffer bytes.Buffer
	if err = tmpl.Execute(&resultBuffer, data); err != nil {
		G.logger.Fatal("parse template failed", zap.Error(err))
	}
	return OvsFlowsLoad(G.config.LanBridge, resultBuffer.String())
}

func GetPortOfport(portUuid string, agentUuid string, purpose string, byUuid string) (ofport string) {
	var nfs *Nfs
	var vm *VM
	if purpose == "kvm" {
		vm, _ = LoadVM4(byUuid, agentUuid)
		if vm == nil {
			return ""
		}
		var vmLocalRes []string
		vmLocalRes, _ = vm.AssignLocalRes(true)
		if vmLocalRes == nil {
			return ""
		}
		for i, uuid := range vm.Ports {
			if uuid == portUuid {
				return vmLocalRes[i*2+2]
			}
		}
		return ""
	}
	if purpose == "nfs" {
		nfs, _ = LoadNfs2(byUuid, agentUuid)
		if nfs == nil {
			return ""
		}
		var nfsLocalRes []string
		nfsLocalRes, _ = nfs.AssignLocalRes(true)
		if nfsLocalRes == nil {
			return ""
		}
		for i, uuid := range nfs.Ports {
			if uuid == portUuid {
				return nfsLocalRes[i*2+1]
			}
		}
		return ""
	}
	return ""
}

func GenerateSgFlows(hostSG *HOSTSGT) string {
	if hostSG == nil {
		return ""
	}
	flows := []string{
		"delete table=75",
		"delete table=85",
		"add table=75,priority=0,actions=resubmit(,72)",
		"add table=85,priority=0,actions=resubmit(,82)",
	}
	ruleid := 1
	priorities := map[string]uint32{}

	keys := make([]string, 0, len(hostSG.Ports))
	for key := range hostSG.Ports {
		keys = append(keys, key)
	}
	sort.Strings(keys)
	for _, portUuid := range keys {
		ofport := hostSG.Ports[portUuid]
		if ofport == "" {
			continue
		}
		port, err := LoadPort(portUuid)
		if err != nil || port == nil || port.Sgs == nil {
			continue
		}

		if port.ByUuid == "" {
			continue
		}

		var subnet *Subnet
		var vpc *VPC
		subnet, err = LoadSubnet(port.SubnetUuid)
		if err != nil {
			continue
		}
		vpc, err = LoadVPC(subnet.VpcUuid)
		if err != nil {
			continue
		}
		var vpcLocalRes []string
		if vpcLocalRes, err = vpc.AssignLocalRes(true); err != nil {
			continue
		}
		vlanid := vpcLocalRes[0]
		if priorities[vlanid] == 0 {
			priorities[vlanid] = 10000
		}

		for _, sgUuid := range port.Sgs {
			sg, err := LoadSG(sgUuid)
			if err != nil || sg == nil || sg.Rules == nil || sg.Ports == nil {
				continue
			}

			for _, rule := range sg.Rules {

				any_addr := rule.Addr == "all" || rule.Addr == "0.0.0.0/0"
				any_protocol := rule.Protocol == "all"
				priority := priorities[vlanid]
				var table uint32
				var field, est_accept_action, new_accept_action string
				if rule.Direction == "in" {
					table = 85
					field = "nw_src"
					est_accept_action = fmt.Sprintf("strip_vlan,output:%s", ofport)

					new_accept_action = fmt.Sprintf("ct(commit,zone=NXM_NX_REG6[0..15],exec(move:NXM_OF_ETH_SRC[]->NXM_NX_CT_LABEL[0..47])),strip_vlan,output:%s", ofport)
				} else {
					table = 75
					field = "nw_dst"
					est_accept_action = "resubmit(,73)"
					new_accept_action = "resubmit(,73)"
				}
				if !any_addr {

					addrs := GetRuleAddrs(sg, rule, vpc.NID)
					for _, _ip := range addrs {
						flows = append(flows, fmt.Sprintf("add table=%d,priority=%d,ct_state=+est-rel-rpl,reg5=%s,reg6=%s,ip,%s=%s actions=conjunction(%d,1/2)", table, priority, ofport, vlanid, field, _ip, ruleid))
						flows = append(flows, fmt.Sprintf("add table=%d,priority=%d,ct_state=+new-est,reg5=%s,reg6=%s,ip,%s=%s actions=conjunction(%d,1/2)", table, priority, ofport, vlanid, field, _ip, ruleid+1))
					}
				}
				if !any_protocol {

					protocol := rule.Protocol
					switch protocol {
					case "tcp", "udp":
						_ports := rule.Ports
						if _ports != "all" {
							parts := strings.Split(_ports, "+")
							for _, part := range parts {
								_parts := strings.Split(part, "-")
								masks := MaskRange(_parts[0], _parts[1])
								for _, mask := range masks {
									flows = append(flows, fmt.Sprintf("add table=%d,priority=%d,ct_state=+est-rel-rpl,reg5=%s,reg6=%s,tp_dst=%s,%s actions=conjunction(%d,2/2)", table, priority, ofport, vlanid, mask, protocol, ruleid))
									flows = append(flows, fmt.Sprintf("add table=%d,priority=%d,ct_state=+new-est,reg5=%s,reg6=%s,tp_dst=%s,%s actions=conjunction(%d,2/2)", table, priority, ofport, vlanid, mask, protocol, ruleid+1))
								}
							}
						} else {
							flows = append(flows, fmt.Sprintf("add table=%d,priority=%d,ct_state=+est-rel-rpl,reg5=%s,reg6=%s,%s actions=conjunction(%d,2/2)", table, priority, ofport, vlanid, protocol, ruleid))
							flows = append(flows, fmt.Sprintf("add table=%d,priority=%d,ct_state=+new-est,reg5=%s,reg6=%s,%s actions=conjunction(%d,2/2)", table, priority, ofport, vlanid, protocol, ruleid+1))
						}
					case "icmp":
						_ports := rule.Ports
						if _ports != "all" {
							parts := strings.Split(_ports, "+")
							for _, part := range parts {
								_parts := strings.Split(part, "-")
								var icmp_type, icmp_code string
								if len(_parts) > 1 {
									icmp_type = _parts[0]
									icmp_code = _parts[1]
								} else {
									icmp_type = _parts[0]
									icmp_code = ""
								}
								if icmp_code != "" {
									flows = append(flows, fmt.Sprintf("add table=%d,priority=%d,ct_state=+est-rel-rpl,reg5=%s,reg6=%s,icmp,icmp_type=%s,icmp_code=%s actions=conjunction(%d,2/2)", table, priority, ofport, vlanid, icmp_type, icmp_code, ruleid))
									flows = append(flows, fmt.Sprintf("add table=%d,priority=%d,ct_state=+new-est,reg5=%s,reg6=%s,icmp,icmp_type=%s,icmp_code=%s actions=conjunction(%d,2/2)", table, priority, ofport, vlanid, icmp_type, icmp_code, ruleid+1))
								} else {
									flows = append(flows, fmt.Sprintf("add table=%d,priority=%d,ct_state=+est-rel-rpl,reg5=%s,reg6=%s,icmp,icmp_type=%s actions=conjunction(%d,2/2)", table, priority, ofport, vlanid, icmp_type, ruleid))
									flows = append(flows, fmt.Sprintf("add table=%d,priority=%d,ct_state=+new-est,reg5=%s,reg6=%s,icmp,icmp_type=%s actions=conjunction(%d,2/2)", table, priority, ofport, vlanid, icmp_type, ruleid+1))
								}
							}
						} else {
							flows = append(flows, fmt.Sprintf("add table=%d,priority=%d,ct_state=+est-rel-rpl,reg5=%s,reg6=%s,icmp actions=conjunction(%d,2/2)", table, priority, ofport, vlanid, ruleid))
							flows = append(flows, fmt.Sprintf("add table=%d,priority=%d,ct_state=+new-est,reg5=%s,reg6=%s,icmp actions=conjunction(%d,2/2)", table, priority, ofport, vlanid, ruleid+1))
						}
					}
				}
				if any_addr && !any_protocol {
					flows = append(flows, fmt.Sprintf("add table=%d,priority=%d,ct_state=+est-rel-rpl,reg5=%s,reg6=%s,ip actions=conjunction(%d,1/2)", table, priority, ofport, vlanid, ruleid))
					flows = append(flows, fmt.Sprintf("add table=%d,priority=%d,ct_state=+new-est,reg5=%s,reg6=%s,ip actions=conjunction(%d,1/2)", table, priority, ofport, vlanid, ruleid+1))
				} else if !any_addr && any_protocol {
					flows = append(flows, fmt.Sprintf("add table=%d, priority=%d,ct_state=+est-rel-rpl,reg5=%s,reg6=%s,ip actions=conjunction(%d,2/2)", table, priority, ofport, vlanid, ruleid))
					flows = append(flows, fmt.Sprintf("add table=%d, priority=%d,ct_state=+new-est,reg5=%s,reg6=%s,ip actions=conjunction(%d,2/2)", table, priority, ofport, vlanid, ruleid+1))
				}
				if rule.Action == "accept" {
					if any_addr && any_protocol {
						flows = append(flows, fmt.Sprintf("add table=%d,priority=%d,ct_state=+est-rel-rpl,reg5=%s,reg6=%s,ip actions=%s", table, priority, ofport, vlanid, est_accept_action))
						flows = append(flows, fmt.Sprintf("add table=%d,priority=%d,ct_state=+new-est,reg5=%s,reg6=%s,ip actions=%s", table, priority, ofport, vlanid, new_accept_action))
					} else {
						flows = append(flows, fmt.Sprintf("add table=%d, priority=%d,conj_id=%d,ct_state=+est-rel-rpl,reg5=%s,reg6=%s,ip actions=%s", table, priority, ruleid, ofport, vlanid, est_accept_action))
						flows = append(flows, fmt.Sprintf("add table=%d, priority=%d,conj_id=%d,ct_state=+new-est,reg5=%s,reg6=%s,ip actions=%s", table, priority, ruleid+1, ofport, vlanid, new_accept_action))
					}
				} else {
					if any_addr && any_protocol {
						flows = append(flows, fmt.Sprintf("add table=%d, priority=%d,ct_state=+est-rel-rpl,reg5=%s,reg6=%s,ip actions=drop", table, priority, ofport, vlanid))
						flows = append(flows, fmt.Sprintf("add table=%d, priority=%d,ct_state=+new-est,reg5=%s,reg6=%s,ip actions=drop", table, priority, ofport, vlanid))
					} else {
						flows = append(flows, fmt.Sprintf("add table=%d, priority=%d,conj_id=%d,ct_state=+est-rel-rpl,reg5=%s,reg6=%s,ip actions=drop", table, priority, ruleid, ofport, vlanid))
						flows = append(flows, fmt.Sprintf("add table=%d, priority=%d,conj_id=%d,ct_state=+new-est,reg5=%s,reg6=%s,ip actions=drop", table, priority, ruleid+1, ofport, vlanid))
					}
				}
				if !(any_addr && any_protocol) {
					ruleid += 2
				}
				priorities[vlanid] = priorities[vlanid] - 1
				if priorities[vlanid] == 0 {
					break
				}
			}
		}
	}
	flow_str := strings.Join(flows, "\n")
	return flow_str
}

func GenerateEipFlows(eips *EIPS) string {
	flows := []string{
		"delete table=40",
		"add table=40,priority=0,actions=output:" + G.config.WanNic,
	}
	for eip, vpcMapping := range eips.EipsInfo {
		mac := vpcMapping.Mac
		flows = append(flows, fmt.Sprintf("add table=40,priority=199,ip,nw_dst=%s,actions=mod_dl_dst:%s,output:%s", eip, mac, G.config.WanNic))
	}
	return strings.Join(flows, "\n")
}

func GetMetersStat(bridge string) (stats map[string]uint64, err error) {
	stats = make(map[string]uint64)
	cmd := fmt.Sprintf("ovs-ofctl -O OpenFlow13 meter-stats %s", bridge)
	out, err := ExecuteCmdOutput(G.config.CmdTimeout, cmd)
	if err != nil {
		return nil, err
	}
	content := string(out)

	re := regexp.MustCompile(`(?s)meter:(\d+).*?byte_in_count:(\d+).*?byte_count:(\d+)`)
	for _, match := range re.FindAllStringSubmatch(content, -1) {
		meter := match[1]
		byte_in_count, _ := strconv.ParseUint(match[2], 10, 64)
		byte_count, _ := strconv.ParseUint(match[3], 10, 64)
		stats[meter] = byte_in_count - byte_count
	}
	return stats, nil
}
