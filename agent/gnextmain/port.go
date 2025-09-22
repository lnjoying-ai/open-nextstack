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
	"encoding/json"
	"fmt"
	"io"
	"net"
	"net/http"
	"strings"

	"github.com/labstack/echo"
	"github.com/spf13/cobra"
	"go.uber.org/zap"
)

type Port struct {
	AgentUuid  string              `json:"agent"`
	Floating   *Floating           `json:"floating"`
	Ip         string              `json:"ip"`
	Vip        bool                `json:"vip"`
	Mac        string              `json:"mac"`
	Mask       string              `json:"mask"`
	Gateway    string              `json:"gateway"`
	Dns        string              `json:"dns"`
	Speed      uint32              `json:"speed"`
	Static     bool                `json:"static"`
	Purpose    string              `json:"purpose"`
	Sgs        []string            `json:"sgs" etcd:"sgs"`
	SubnetUuid string              `json:"subnet"`
	ByUuid     string              `json:"by"`
	LocalRes   map[string][]string `json:"local_res"`
	AgentIp    string              `json:"agent_ip"`
	AgentPort  uint32              `json:"agent_port"`
	Ofport     string              `json:"-"`
	Nic        string              `json:"-"`
	Vlanid     string              `json:"-"`
	NID        string              `json:"-"`
	Cidr       string              `json:"-"`
	SHAREREST
	PHASEINFO
}

type Statistics struct {
	BytesOfMonth uint64 `json:"bytes_of_month"`

	BytesOfMeter uint64 `json:"bytes_of_meter"`

	LastTime int64 `json:"last_time"`
}

type PORTSTAT struct {
	Stat *Statistics `json:"stat"`
	SHAREREST
}

type Floating struct {
	Eip        string            `json:"eip"`
	ProtoPorts PROTO2PORTRANGEST `json:"mapping"`
	MappingStr string            `json:"-"`
}

type PortFlags struct {
	uuid      string
	ip        string
	eip       string
	vip       bool
	static    bool
	speed     uint32
	agentUuid string
	purpose   string
	abnormal  bool
	mapping   string
	sg        []string
}

type PortAddReq struct {
	AgentUuid string `json:"agent"`
	Ip        string `json:"ip"`
	Vip       bool   `json:"vip"`
	Static    bool   `json:"static"`
	Speed     uint32 `json:"speed"`
	Purpose   string `json:"purpose"`
}

type PortBindReq struct {
	Eip     string `json:"eip"`
	Mapping string `json:"mapping"`
}

type PortApplyReq struct {
	Sgs []string `json:"sgs"`
}

type PortGetRsp struct {
	Status     string    `json:"status"`
	AgentUuid  string    `json:"agent"`
	Floating   *Floating `json:"floating"`
	Ip         string    `json:"ip"`
	Vip        bool      `json:"vip"`
	Mac        string    `json:"mac"`
	Mask       string    `json:"mask"`
	Gateway    string    `json:"gateway"`
	Dns        string    `json:"dns"`
	Speed      uint32    `json:"speed"`
	Static     bool      `json:"static"`
	Purpose    string    `json:"purpose"`
	Sgs        []string  `json:"sgs" etcd:"sgs"`
	SubnetUuid string    `json:"subnet"`
	ByUuid     string    `json:"by"`
	Bytes      uint64    `json:"bytes"`
	SHAREREST
	PHASEINFO
}

var portFlags = &PortFlags{}

func LoadPort(portUuid string) (port *Port, err error) {
	port = &Port{SHAREREST: SHAREREST{UUIDINFO: UUIDINFO{Uuid: portUuid}, ETCDINFO: ETCDINFO{Type: "port"}}}
	path := port.GetKey()
	var ret RES
	if ret, err = LoadRes(path, port); err != nil {
		return nil, err
	}
	if ret != nil {
		port, ok := ret.(*Port)
		if ok {
			return port, nil
		}
	}
	return nil, nil
}

func LoadPortStat(portUuid string) (portStat *PORTSTAT, err error) {
	portStat = &PORTSTAT{SHAREREST: SHAREREST{UUIDINFO: UUIDINFO{Uuid: portUuid}, ETCDINFO: ETCDINFO{Type: "portstat"}}}
	path := portStat.GetKey()
	var ret RES
	if ret, err = LoadRes(path, portStat); err != nil {
		return nil, err
	}
	if ret != nil {
		portStat, ok := ret.(*PORTSTAT)
		if ok {
			return portStat, nil
		}
	}
	return nil, nil
}

func (port *Port) Add(subnet *Subnet, vpc *VPC, agent *Agent, vpcLocalRes []string) (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()

	if _, ok := G.deployedRest[port.Uuid]; ok {
		return nil
	}
	if err = port.PhoneHomeSet(agent, vpcLocalRes[0], "", 0); err != nil {
		G.logger.Warn("failed to set phonehome", zap.Error(err))
		return err
	}
	G.deployedRest[port.Uuid] = struct{}{}
	RegisterPortMonitor(port, subnet, vpc, agent, vpcLocalRes)
	return nil
}

func (port *Port) Del(subnet *Subnet, vpc *VPC, agent *Agent, vpcLocalRes []string) (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()

	if _, ok := G.deployedRest[port.Uuid]; !ok {
		return nil
	}
	if vpcLocalRes != nil {
		if err = port.PhoneHomeUnset(agent, vpcLocalRes[0]); err != nil {
			G.logger.Warn("failed to unset phonehome", zap.Error(err))
			return err
		}
	}
	delete(G.deployedRest, port.Uuid)
	if _, ok := G.ctxes[port.Uuid]; ok {
		G.ctxes[port.Uuid] <- struct{}{}
	}
	return nil
}

func (port *Port) Bind(subnet *Subnet, vpc *VPC, agent *Agent, vpcLocalRes []string) (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()
	if port.Floating == nil {
		return nil
	}

	l3Mode := agent.ConfigData.L3Mode
	if err = port.IpForwardingSet(vpcLocalRes[1], vpcLocalRes[0], l3Mode); err != nil {
		G.logger.Warn("failed to set ip forwarding", zap.Error(err))
		return err
	}
	shouldConfigure := false
	if port.Floating.ProtoPorts["ip"] != nil {

		if (l3Mode && port.AgentUuid == G.config.Uuid) || (!l3Mode && G.config.L3Mode && G.config.
			MasterL3) {
			shouldConfigure = true
		}
	} else if G.config.L3Mode && G.config.MasterL3 {
		shouldConfigure = true
	}
	if shouldConfigure {
		if err = OvsWanPortBind(port.Floating.Eip, vpcLocalRes[0]); err != nil {
			G.logger.Warn("failed to bind wan port", zap.Error(err))
			return err
		}
		if err = osArping(port.Floating.Eip, G.config.WanNic); err != nil {
			return err
		}
	}
	return nil
}

func (port *Port) UnBind(subnet *Subnet, vpc *VPC, agent *Agent, vpcLocalRes []string) (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()
	if port.Floating == nil {
		return nil
	}
	l3Mode := agent.ConfigData.L3Mode
	if err = port.IpForwardingUnset(vpcLocalRes[1], vpcLocalRes[0], l3Mode); err != nil {
		G.logger.Warn("failed to set ip forwarding", zap.Error(err))
		return err
	}
	shouldConfigure := false
	if port.Floating.ProtoPorts["ip"] != nil {
		if (l3Mode && port.AgentUuid == G.config.Uuid) || (!l3Mode && G.config.L3Mode && G.config.MasterL3) {
			shouldConfigure = true
		}
	} else if G.config.L3Mode && G.config.MasterL3 {
		shouldConfigure = true
	}
	if shouldConfigure {
		if err = OvsWanPortUnbind(port.Floating.Eip, vpcLocalRes[0]); err != nil {
			G.logger.Warn("failed to unbind wan port", zap.Error(err))
		}
	}
	return nil
}

func (port *Port) Restore(subnet *Subnet, vpc *VPC, agent *Agent, vpcLocalRes []string) (err error) {
	if _, err = port.AssignLocalRes(true); err != nil {
		return nil
	}
	if err = port.Add(subnet, vpc, agent, vpcLocalRes); err != nil {
		G.logger.Error("cannot add port", zap.Error(err))
		return err
	}
	if err = port.Bind(subnet, vpc, agent, vpcLocalRes); err != nil {
		G.logger.Error("cannot bind port", zap.Error(err))
		return err
	}
	return nil
}

func (port *Port) PhoneHomeSet(agent *Agent, vlanid string, oldAgentIp string, oldAgentPort uint32) (err error) {

	if !G.config.L3Mode || port.Purpose != "kvm" {
		return nil
	}
	ns := GetNsName(vlanid)
	if oldAgentIp != "" && oldAgentPort != 0 {
		cmd := fmt.Sprintf("iptables -t nat -D PREROUTING -p tcp -s %s -d 169.254.169.254 --dport 80 -j DNAT --to-destination %s:%d", port.Ip, oldAgentIp, oldAgentPort)
		if err = osIptablesExec(ns, cmd); err != nil {
			G.logger.Warn("failed to delete iptables rule", zap.Error(err))
			return err
		}
	}
	cmd := fmt.Sprintf("iptables -t nat -A PREROUTING -p tcp -s %s -d 169.254.169.254 --dport 80 -j DNAT --to-destination %s:%d", port.Ip, agent.ConfigData.AgentIp, agent.ConfigData.AgentPort)
	return osIptablesExec(ns, cmd)
}

func (port *Port) PhoneHomeUnset(agent *Agent, vlanid string) (err error) {
	if !G.config.L3Mode || port.Purpose != "kvm" {
		return nil
	}
	ns := GetNsName(vlanid)
	cmd := fmt.Sprintf("iptables -t nat -D PREROUTING -p tcp -s %s -d 169.254.169.254 --dport 80 -j DNAT --to-destination %s:%d", port.Ip, agent.ConfigData.AgentIp, agent.ConfigData.AgentPort)
	return osIptablesExec(ns, cmd)
}

func (port *Port) IpForwardingSet(lanNic string, vlanid string, l3Mode bool) (err error) {
	if port.Floating == nil {
		return nil
	}
	ns := GetNsName(vlanid)
	wanVpcNsNic := GetWanVpNscNic(vlanid)
	lanVpcNsNic := GetLanVpcNsNic(vlanid)
	if port.Floating.ProtoPorts["ip"] != nil {
		if (l3Mode && port.AgentUuid == G.config.Uuid) || (!l3Mode && G.config.L3Mode && G.config.MasterL3) {
			if err = osDnatAdd(wanVpcNsNic, "ip", port.Floating.Eip, 0, 0, port.Ip, ns); err != nil {
				G.logger.Warn("failed to add dnat rule", zap.Error(err))
				return err
			}

			if err = osSnatAdd2(port.Ip, wanVpcNsNic, port.Floating.Eip, lanVpcNsNic, ns); err != nil {
				G.logger.Warn("failed to add snat rule", zap.Error(err))
				return err
			}
		}
	} else if G.config.L3Mode && G.config.MasterL3 {
		for proto, ports := range port.Floating.ProtoPorts {
			for _, p := range ports {
				if err = osDnatAdd(wanVpcNsNic, proto, port.Floating.Eip, p[0], p[1], port.Ip, ns); err != nil {
					G.logger.Warn("failed to add dnat rule", zap.Error(err))
					return err
				}
			}
		}
	}
	return nil
}

func (port *Port) IpForwardingUnset(lanNic string, vlanid string, l3Mode bool) (err error) {
	if port.Floating == nil {
		return nil
	}
	ns := GetNsName(vlanid)
	wanVpcNsNic := GetWanVpNscNic(vlanid)
	lanVpcNsNic := GetLanVpcNsNic(vlanid)

	if port.Floating.ProtoPorts["ip"] != nil {
		if (l3Mode && port.AgentUuid == G.config.Uuid) || (!l3Mode && G.config.L3Mode && G.config.MasterL3) {
			if err = osDnatDel(wanVpcNsNic, "ip", port.Floating.Eip, 0, 0, port.Ip, ns); err != nil {
				G.logger.Warn("failed to delete dnat rule", zap.Error(err))
				return err
			}

			if err = osSnatDel2(port.Ip, wanVpcNsNic, port.Floating.Eip, lanVpcNsNic, ns); err != nil {
				G.logger.Warn("failed to delete snat rule", zap.Error(err))
				return err
			}
		}
	} else if G.config.L3Mode && G.config.MasterL3 {
		for proto, ports := range port.Floating.ProtoPorts {
			for _, p := range ports {
				if err = osDnatDel(wanVpcNsNic, proto, port.Floating.Eip, p[0], p[1], port.Ip, ns); err != nil {
					G.logger.Warn("failed to delete dnat rule", zap.Error(err))
					return err
				}
			}
		}
	}
	return nil
}

func (port *Port) AddingPortTask(subnet *Subnet, vpc *VPC, agent *Agent, vpcLocalRes []string) {
	var err error
	success := false
	if _, err = port.AssignLocalRes(false); err != nil {
		G.logger.Error("cannot assign local res", zap.Error(err))
		goto UPDATE
	}
	if err = port.Add(subnet, vpc, agent, vpcLocalRes); err != nil {
		G.logger.Error("cannot add port", zap.Error(err))
		goto UPDATE
	}
	success = true
UPDATE:
	for {
		if success {
			port.AgentIp = agent.ConfigData.AgentIp
			port.AgentPort = agent.ConfigData.AgentPort
		}
		port.PhaseStop(success)
		var done bool
		done, err = Save(nil, []RES{port})
		if err != nil {
			G.logger.Error("cannot save port", zap.Error(err))
			return
		}
		if done {
			break
		}
	}
	if success {
		NoticeDnsmasq(vpcLocalRes[0])
	}
}

func (port *Port) DeletingPortTask(subnet *Subnet, vpc *VPC, agent *Agent, vpcLocalRes []string) {
	var err error
	hostsg, _ := LoadHostSG(port.AgentUuid)
	macs, _ := LoadMacs()
	if err = port.Del(subnet, vpc, agent, vpcLocalRes); err != nil {
		G.logger.Error("cannot delete port", zap.Error(err))
		goto UPDATE
	}
	_ = port.UnAssignLocalRes(G.config.Uuid)
	for {
		reses := []RES{subnet, macs}
		reses2del := []RES{port}
		for _, sgUuid := range port.Sgs {
			var sg *SG
			if sg, err = LoadSG(sgUuid); err != nil {
				continue
			}
			if sg != nil {
				sg.Ports = Remove(sg.Ports, port.Uuid)
				sg.Addrs[vpc.NID] = Remove(sg.Addrs[vpc.NID], port.Ip)
				if len(sg.Addrs[vpc.NID]) == 0 {
					delete(sg.Addrs, vpc.NID)
				}
				reses = append(reses, sg)
			}
		}
		delete(macs.MacsInfo, port.Mac)
		delete(subnet.Ports, port.Uuid)
		hostSgs := HostSgs2Update(port.Sgs)
		port.Sgs = []string{}
		port.UpdateHostSG(hostsg)
		reses = append(reses, hostsg)
		for agentUuid, hostsg := range hostSgs {
			if agentUuid != port.AgentUuid {
				reses = append(reses, hostsg)
			}
		}
		portStat, _ := LoadPortStat(port.Uuid)
		if portStat != nil {
			reses2del = append(reses2del, portStat)
		}
		var done bool
		done, err = Save(reses2del, reses)
		if err != nil {
			G.logger.Error("cannot save data", zap.Error(err))
			return
		}
		if done {
			if vpcLocalRes != nil {
				NoticeDnsmasq(vpcLocalRes[0])
			}
			return
		}
	}
UPDATE:
	if _, err = UpdatePhaseStop(port, false); err != nil {
		G.logger.Error("cannot update port phase", zap.Error(err))
	}
}

func (port *Port) BindingPortTask(subnet *Subnet, vpc *VPC, agent *Agent, vpcLocalRes []string) {
	success := false
	err := port.Bind(subnet, vpc, agent, vpcLocalRes)
	if err != nil {
		G.logger.Error("cannot bind port", zap.Error(err))
		goto UPDATE
	}

	success = true
UPDATE:
	if _, err = UpdatePhaseStop(port, success); err != nil {
		G.logger.Error("cannot update port phase", zap.Error(err))
	}
}

func (port *Port) UnbindPortTask(subnet *Subnet, vpc *VPC, agent *Agent, vpcLocalRes []string) {
	err := port.UnBind(subnet, vpc, agent, vpcLocalRes)
	if err != nil {
		G.logger.Error("cannot unbind port", zap.Error(err))
		if _, err = UpdatePhaseStop(port, false); err != nil {
			G.logger.Error("cannot update port phase", zap.Error(err))
		}
	} else {
		eips, _ := LoadEips()
		for {
			delete(eips.EipsInfo[port.Floating.Eip].Mapping, port.Uuid)
			if len(eips.EipsInfo[port.Floating.Eip].Mapping) == 0 {
				delete(eips.EipsInfo, port.Floating.Eip)
			}
			port.Floating = nil
			port.PhaseStop(true)
			var done bool
			done, err = Save(nil, []RES{port, eips})
			if err != nil {
				G.logger.Error("cannot save port", zap.Error(err))
				return
			}
			if done {
				break
			}
		}
	}
}

func (port *Port) VerifyUuid() error {
	return AssignUuid("port", port)
}

func (port *Port) VerifyPortMac(macs *MACS) (err error) {
	return AssignMac(macs, port)
}

func (port *Port) VerifyPortIp(subnet *Subnet) (err error) {
	ips, _, mask := IpFromCidr(subnet.Cidr, []int{1, -2})
	port.Gateway = ips[0].String()
	port.Dns = ips[1].String()
	port.Mask = mask
	if port.Ip == "" {
		for {
			ip := GenIp(subnet.Cidr)
			ipStr := ip.String()
			var isUsing bool
			for uuid, _ip := range subnet.Ports {
				if _ip == ipStr && uuid != port.Uuid {
					isUsing = true
					break
				}
			}
			if isUsing {
				continue
			}
			port.Ip = ipStr
			subnet.Ports[port.Uuid] = ipStr
			return nil
		}
	} else {
		if net.ParseIP(port.Ip) == nil {
			return fmt.Errorf("invalid port ip")
		}
		for uuid, _ip := range subnet.Ports {
			if _ip == port.Ip && uuid != port.Uuid {
				return fmt.Errorf("ip address already exists")
			}
		}
		subnet.Ports[port.Uuid] = port.Ip
		return nil
	}
}

func (port *Port) VerifyPortFloating(eips *EIPS, vpc *VPC) (err error) {
	if port.Floating != nil {
		if net.ParseIP(port.Floating.Eip) == nil {
			return fmt.Errorf("invalid eip")
		}
		if port.Floating.MappingStr != "" {
			mapping := PROTO2PORTRANGEST{}

			protocols := strings.Split(port.Floating.MappingStr, ",")
			for _, protocol := range protocols {
				parts := strings.Split(protocol, ":")
				if parts[0] != "tcp" && parts[0] != "udp" && parts[0] != "ip" {
					continue
				}
				if len(parts) > 1 {
					ports := strings.Split(parts[1], "#")
					for _, port := range ports {
						var s uint32
						var t uint32
						ps := strings.Split(port, "-")
						if len(ps) > 1 {
							s, err = AtoU32(ps[0])
							if err != nil {
								continue
							}
							t, err = AtoU32(ps[1])
							if err != nil {
								continue
							}
						} else {
							s, err = AtoU32(port)
							if err != nil {
								continue
							}
							t = s
						}
						if s > 65535 || t > 65535 {
							continue
						}
						for _, p := range mapping[parts[0]] {
							if p[0] == s {
								continue
							}
						}

						mapping[parts[0]] = append(mapping[parts[0]], [2]uint32{s, t})
					}
				} else {

					mapping[parts[0]] = [][2]uint32{{0, 0}}
				}
			}

			if _, ok := mapping["ip"]; ok {
				port.Floating.ProtoPorts["ip"] = [][2]uint32{{0, 0}}
			} else {
				for proto, ports := range mapping {
					if proto != "tcp" && proto != "udp" {
						continue
					}

					for _, p := range ports {
						if p[0] == 0 {
							ports = [][2]uint32{{0, 0}}
							break
						}
					}
					port.Floating.ProtoPorts[proto] = ports
				}
			}
		}
	}

	mac := G.config.WanNicMac
	if port.Floating.ProtoPorts["ip"] != nil {
		agent := LoadAgent(port.AgentUuid)
		if agent == nil {
			return fmt.Errorf("agent does not exist")
		}
		mac = agent.ConfigData.WanNicMac
	}

	if eips.EipsInfo[port.Floating.Eip] == nil {
		eips.EipsInfo[port.Floating.Eip] = &EIPINFOT{
			VpcUuid: vpc.Uuid,
			Mac:     mac,
			Mapping: PORT2PROTOST{
				port.Uuid: port.Floating.ProtoPorts,
			},
		}
		return nil
	}

	if eips.EipsInfo[port.Floating.Eip].VpcUuid != vpc.Uuid {
		return fmt.Errorf("eip already exists")
	}

	for _portUuid, _protoPorts := range eips.EipsInfo[port.Floating.Eip].Mapping {
		if _portUuid == port.Uuid {
			continue
		}
		if ConflictPorts(port.Floating.ProtoPorts, _protoPorts) {
			return fmt.Errorf("eip already exists")
		}
	}
	eips.EipsInfo[port.Floating.Eip].Mapping[port.Uuid] = port.Floating.ProtoPorts
	return nil
}

func (port *Port) AssignLocalRes(retrieveOnly bool) (localRes []string, err error) {
	if !G.config.L3Mode {
		for _, v := range port.LocalRes {
			if v[len(v)-1] == "1" {
				return v, nil
			}
		}
		return nil, fmt.Errorf("no local res")
	}

	if _, ok := port.LocalRes[G.config.Uuid]; ok {
		return port.LocalRes[G.config.Uuid], nil
	}

	if retrieveOnly {
		return nil, fmt.Errorf("no local res")
	}
	ports, _ := LoadPorts()
	for {
		masterL3 := "1"
		if !G.config.MasterL3 {
			masterL3 = "0"
		}
		port.LocalRes[G.config.Uuid] = []string{masterL3}
		if ports.PortsInfo[G.config.Uuid] == nil {
			ports.PortsInfo[G.config.Uuid] = make(map[string]string)
		}
		ports.PortsInfo[G.config.Uuid][port.Uuid] = port.SubnetUuid
		var done bool
		var err error
		done, err = Save(nil, []RES{port, ports})
		if err != nil {
			return nil, err
		}
		if done {
			break
		}
	}
	return port.LocalRes[G.config.Uuid], nil
}

func (port *Port) UnAssignLocalRes(agentUuid string) (err error) {
	if _, ok := port.LocalRes[agentUuid]; !ok {
		return nil
	}
	ports, _ := LoadPorts()
	for {
		delete(ports.PortsInfo[agentUuid], port.Uuid)
		delete(port.LocalRes, agentUuid)
		var done bool
		var err error
		done, err = Save(nil, []RES{port, ports})
		if err != nil {
			return err
		}
		if done {
			break
		}
	}
	return nil
}

func (port *Port) UpdateHostSG(hostSG *HOSTSGT) {
	if len(port.Sgs) == 0 {
		delete(hostSG.Ports, port.Uuid)
	}
}

func PortCmdParser(command *cobra.Command) {
	var err error
	var portCmd = &cobra.Command{
		Use:   "port",
		Short: "Port management",
		Run: func(cmd *cobra.Command, args []string) {
			fmt.Println("please specify a subcommand for port management. use -h for help.")
		},
	}
	var portAddCmd = &cobra.Command{
		Use:   "add",
		Short: "Add a Port",
		Run:   PortAddHandle,
	}
	portAddCmd.Flags().StringVarP(&portFlags.uuid, "uuid", "U", "", "set the UUID of the Subnet")
	if err = portAddCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	portAddCmd.Flags().StringVar(&portFlags.ip, "ip", "", "set the IP address of the port")
	portAddCmd.Flags().BoolVar(&portFlags.vip, "vip", false, "set the VIP mode of the port")
	portAddCmd.Flags().StringVar(&portFlags.purpose, "purpose", "kvm", "set the purpose of the port")
	portAddCmd.Flags().StringVarP(&portFlags.agentUuid, "agent", "A", "", "set the agent UUID of the port")
	portAddCmd.Flags().BoolVar(&portFlags.static, "static", false, "whether the IP of the port is static")
	portAddCmd.Flags().Uint32Var(&portFlags.speed, "speed", 0, "Kbps of the port")
	if err = portAddCmd.MarkFlagRequired("agent"); err != nil {
		panic(err)
	}
	var portGetCmd = &cobra.Command{
		Use:   "get",
		Short: "Get a Port",
		Run:   PortGetHandle,
	}
	portGetCmd.Flags().StringVarP(&portFlags.uuid, "uuid", "U", "", "set the UUID of the port")
	if err = portGetCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var portDelCmd = &cobra.Command{
		Use:   "del",
		Short: "Delete a Port",
		Run:   PortDelHandle,
	}
	portDelCmd.Flags().StringVarP(&portFlags.uuid, "uuid", "U", "", "set the UUID of the port")
	if err = portDelCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var portListCmd = &cobra.Command{
		Use:   "list",
		Short: "List all Ports",
		Run:   PortListHandle,
	}
	portListCmd.Flags().BoolVar(&portFlags.abnormal, "abnormal", false, "list abnormal ports")
	portListCmd.Flags().StringVarP(&portFlags.uuid, "uuid", "U", "", "set the UUID of the Subnet")
	if err = portListCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var portBindCmd = &cobra.Command{
		Use:   "bind",
		Short: "Bind an EIP to the port",
		Run:   PortBindHandle,
	}
	portBindCmd.Flags().StringVarP(&portFlags.uuid, "uuid", "U", "", "set the UUID of the port")
	if err = portBindCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	portBindCmd.Flags().StringVar(&portFlags.eip, "eip", "", "set the EIP of the port")
	if err = portBindCmd.MarkFlagRequired("eip"); err != nil {
		panic(err)
	}
	portBindCmd.Flags().StringVar(&portFlags.mapping, "mapping", "", "set the EIP mapping of the port")
	if err = portBindCmd.MarkFlagRequired("mapping"); err != nil {
		panic(err)
	}
	var portUnbindCmd = &cobra.Command{
		Use:   "unbind",
		Short: "Unbind an EIP from the port",
		Run:   PortUnbindHandle,
	}
	portUnbindCmd.Flags().StringVarP(&portFlags.uuid, "uuid", "U", "", "set the UUID of the port")
	if err = portUnbindCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var portApplyCmd = &cobra.Command{
		Use:   "apply",
		Short: "Apply a SG to the port",
		Run:   PortApplyHandle,
	}
	portApplyCmd.Flags().StringVarP(&portFlags.uuid, "uuid", "U", "", "set the UUID of the port")
	if err = portApplyCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	portApplyCmd.Flags().StringSliceVar(&portFlags.sg, "sg", nil, "set the UUID of the SG")
	portCmd.AddCommand(portAddCmd, portDelCmd, portBindCmd, portUnbindCmd, portApplyCmd, portGetCmd, portListCmd)
	command.AddCommand(portCmd)
}

func PortAddHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/subnets/%s/ports", G.Host, G.Port, portFlags.uuid)
	data := map[string]interface{}{"ip": portFlags.ip, "vip": portFlags.vip, "purpose": portFlags.purpose, "agent": portFlags.agentUuid, "static": portFlags.static, "speed": portFlags.speed}
	jsonBytes, _ := json.Marshal(data)
	req, _ := http.NewRequest("POST", url, bytes.NewBuffer(jsonBytes))
	req.Header.Set("Content-Type", "application/json")
	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		fmt.Println("error:", err)
		return
	}
	defer resp.Body.Close()
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		fmt.Println("error:", err)
		return
	}
	fmt.Println(string(body))
}

func PortGetHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/ports/%s", G.Host, G.Port, portFlags.uuid)
	resp, err := http.Get(url)
	if err != nil {
		fmt.Println("error:", err)
		return
	}
	defer resp.Body.Close()
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		fmt.Println("error:", err)
		return
	}
	fmt.Println(string(body))
}

func PortDelHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/ports/%s", G.Host, G.Port, portFlags.uuid)
	req, _ := http.NewRequest("DELETE", url, nil)
	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		fmt.Println("error:", err)
		return
	}
	defer resp.Body.Close()
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		fmt.Println("error:", err)
		return
	}
	fmt.Println(string(body))
}

func PortListHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/subnets/%s/ports", G.Host, G.Port, portFlags.uuid)
	data := map[string]interface{}{}
	if portFlags.abnormal {
		data["abnormal"] = true
	}
	jsonBytes, _ := json.Marshal(data)
	req, _ := http.NewRequest("GET", url, bytes.NewBuffer(jsonBytes))
	req.Header.Set("Content-Type", "application/json")
	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		fmt.Println("error:", err)
		return
	}
	defer resp.Body.Close()
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		fmt.Println("error:", err)
		return
	}
	fmt.Println(string(body))
}

func PortBindHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/ports/%s/bind", G.Host, G.Port, portFlags.uuid)
	data := map[string]interface{}{"eip": portFlags.eip, "mapping": portFlags.mapping}
	jsonBytes, _ := json.Marshal(data)
	req, _ := http.NewRequest("PUT", url, bytes.NewBuffer(jsonBytes))
	req.Header.Set("Content-Type", "application/json")
	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		fmt.Println("error:", err)
		return
	}
	defer resp.Body.Close()
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		fmt.Println("error:", err)
		return
	}
	fmt.Println(string(body))
}

func PortUnbindHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/ports/%s/unbind", G.Host, G.Port, portFlags.uuid)
	req, _ := http.NewRequest("PUT", url, nil)
	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		fmt.Println("error:", err)
		return
	}
	defer resp.Body.Close()
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		fmt.Println("error:", err)
		return
	}
	fmt.Println(string(body))
}

func PortApplyHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/ports/%s/apply", G.Host, G.Port, portFlags.uuid)
	data := map[string]interface{}{"sgs": portFlags.sg}
	jsonBytes, _ := json.Marshal(data)
	req, _ := http.NewRequest("PUT", url, bytes.NewBuffer(jsonBytes))
	req.Header.Set("Content-Type", "application/json")
	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		fmt.Println("error:", err)
		return
	}
	defer resp.Body.Close()
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		fmt.Println("error:", err)
		return
	}
	fmt.Println(string(body))
}

func PortAppSetup() {
	if !G.config.L3Mode {
		return
	}
	G.echoServer.POST("/v1/subnets/:uuid/ports", AppPortAdd)
	G.echoServer.DELETE("/v1/ports/:uuid", AppPortDel)
	G.echoServer.PUT("/v1/ports/:uuid/bind", AppPortBind)
	G.echoServer.PUT("/v1/ports/:uuid/unbind", AppPortUnbind)
	G.echoServer.PUT("/v1/ports/:uuid/apply", AppPortApply)
	G.echoServer.GET("/v1/subnets/:uuid/ports", AppPortList)
	G.echoServer.GET("/v1/ports/:uuid", AppPortGet)
}

func AppPortAdd(c echo.Context) (err error) {
	G.logger.Debug("=========AppPortAdd==========")
	if !G.config.MasterL3 {
		return c.JSON(http.StatusForbidden, NewAppErrorRsp("This node is not a master L3 node."))
	}
	var port *Port
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Subnet UUID is required."))
	}
	q := &PortAddReq{}
	if err = c.Bind(q); err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Cannot parse the request."))
	}
	agent := LoadAgent(q.AgentUuid)
	if agent == nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load agent."))
	}
	subnet, err := LoadSubnet(uuid)
	if err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load subnet."))
	}
	if subnet == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Subnet not found."))
	}
	vpc, err := LoadVPC(subnet.VpcUuid)
	if err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load VPC."))
	}
	if vpc == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("VPC not ready."))
	}
	var vpcLocalRes []string
	vpcLocalRes, err = vpc.AssignLocalRes(true)
	if err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	macs, _ := LoadMacs()
	for {
		if subnet.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Subnet is not added."))
		}
		port = &Port{Sgs: []string{}, LocalRes: map[string][]string{}, AgentUuid: q.AgentUuid, SHAREREST: SHAREREST{ETCDINFO: ETCDINFO{Type: "port"}}}
		port.Ip = q.Ip
		port.Vip = q.Vip
		port.SubnetUuid = subnet.Uuid
		port.Purpose = q.Purpose
		port.Static = q.Static
		port.Speed = q.Speed
		_ = port.PhaseStart(PhaseTypeAdd)
		if err = port.VerifyUuid(); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		if err = port.VerifyPortIp(subnet); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		if err = port.VerifyPortMac(macs); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		portStat := &PORTSTAT{SHAREREST: SHAREREST{UUIDINFO: UUIDINFO{Uuid: port.Uuid}, ETCDINFO: ETCDINFO{Type: "portstat"}}, Stat: &Statistics{}}
		var done bool
		done, err = Save(nil, []RES{port, subnet, macs, portStat})
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	go port.AddingPortTask(subnet, vpc, agent, vpcLocalRes)
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(port.Uuid))
}

func AppPortList(c echo.Context) (err error) {
	G.logger.Debug("=========AppPortList==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Subnet UUID is required."))
	}
	var subnet *Subnet
	if subnet, err = LoadSubnet(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load subnet."))
	}
	if subnet == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Subnet not found."))
	}
	q := AppListReq{}
	if err = c.Bind(&q); err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
	}
	portUuids := []string{}
	rests := LoadRests("port")
	for portUuid := range subnet.Ports {
		p := &Port{SHAREREST: SHAREREST{UUIDINFO: UUIDINFO{Uuid: portUuid}, ETCDINFO: ETCDINFO{Type: "port"}}}
		if rest := GetRest(rests, "port", p.GetKey()); rest != nil {
			port, ok := rest.(*Port)
			if !ok {
				continue
			}
			if q.Abnormal {
				if port.NotReady() {
					portUuids = append(portUuids, port.Uuid)
				}
			} else {
				portUuids = append(portUuids, port.Uuid)
			}
		}
	}
	jsonMap := make(map[string]interface{})
	jsonMap["status"] = "ok"
	jsonMap["port_count"] = len(portUuids)
	jsonMap["ports"] = portUuids
	return c.JSON(http.StatusOK, jsonMap)
}

func AppPortGet(c echo.Context) (err error) {
	G.logger.Debug("=========AppPortGet==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Port UUID is required."))
	}
	var port *Port
	if port, err = LoadPort(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if port == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Port not found."))
	}
	var portstat *PORTSTAT
	if portstat, err = LoadPortStat(port.Uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	var bytes uint64
	if portstat != nil {
		bytes = portstat.Stat.BytesOfMonth
	}
	rsp := PortGetRsp{
		Status:     "ok",
		AgentUuid:  port.AgentUuid,
		Floating:   port.Floating,
		Ip:         port.Ip,
		Vip:        port.Vip,
		Mac:        port.Mac,
		Mask:       port.Mask,
		Gateway:    port.Gateway,
		Dns:        port.Dns,
		Speed:      port.Speed,
		Static:     port.Static,
		Purpose:    port.Purpose,
		Sgs:        port.Sgs,
		SubnetUuid: port.SubnetUuid,
		ByUuid:     port.ByUuid,
		Bytes:      bytes,
		SHAREREST:  port.SHAREREST,
		PHASEINFO:  port.PHASEINFO}
	return c.JSON(http.StatusOK, &rsp)
}

func AppPortDel(c echo.Context) (err error) {
	G.logger.Debug("=========AppPortDel==========")
	if !G.config.MasterL3 {
		return c.JSON(http.StatusForbidden, NewAppErrorRsp("This node is not a master L3 node."))
	}
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Port UUID is required."))
	}
	var port *Port
	if port, err = LoadPort(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if port == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Port not found."))
	}
	agent := LoadAgent(port.AgentUuid)
	if agent == nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load agent."))
	}
	var subnet *Subnet
	if subnet, err = LoadSubnet(port.SubnetUuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load subnet."))
	}
	if subnet == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Subnet not found."))
	}
	var vpc *VPC
	if vpc, err = LoadVPC(subnet.VpcUuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load VPC."))
	}
	if vpc == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("VPC not ready."))
	}
	var vpcLocalRes []string
	vpcLocalRes, _ = vpc.AssignLocalRes(true)
	sgs := []*SG{}
	for _, sgUuid := range port.Sgs {
		var sg *SG
		sg, err = LoadSG(sgUuid)
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load security group."))
		}
		if sg == nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load security group."))
		}
		sgs = append(sgs, sg)
	}
	for {
		reses := []RES{port}
		if port.Floating != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("The port is still bound to an Elastic IP."))
		}
		if port.ByUuid != "" {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("The port is in use."))
		}
		for _, sg := range sgs {
			sg.Ports = Remove(sg.Ports, port.Uuid)
			reses = append(reses, sg)
		}
		if err = port.PhaseStart(PhaseTypeDel); err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		var done bool
		done, err = Save(nil, reses)
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	go port.DeletingPortTask(subnet, vpc, agent, vpcLocalRes)
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(port.Uuid))
}

func AppPortBind(c echo.Context) (err error) {
	G.logger.Debug("=========AppPortBind==========")
	if !G.config.MasterL3 {
		return c.JSON(http.StatusForbidden, NewAppErrorRsp("This node is not a master L3 node."))
	}
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Port UUID is required."))
	}
	q := &PortBindReq{}
	if err = c.Bind(q); err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Cannot parse the request."))
	}
	if q.Eip == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Elastic IP (EIP) is required."))
	}
	if q.Mapping == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Elastic IP mapping is required."))
	}
	var port *Port
	if port, err = LoadPort(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if port == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Port not found."))
	}
	agent := LoadAgent(port.AgentUuid)
	if agent == nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load agent."))
	}
	var subnet *Subnet
	if subnet, err = LoadSubnet(port.SubnetUuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load subnet."))
	}
	if subnet == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Subnet not found."))
	}
	var vpc *VPC
	if vpc, err = LoadVPC(subnet.VpcUuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load VPC."))
	}
	if vpc == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("VPC not ready."))
	}
	var vpcLocalRes []string
	vpcLocalRes, err = vpc.AssignLocalRes(true)
	if err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	eips, _ := LoadEips()
	for {
		if port.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Port is not ready."))
		}
		if port.Floating != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("The port is already bound to an Elastic IP."))
		}
		port.Floating = &Floating{Eip: q.Eip, MappingStr: q.Mapping, ProtoPorts: PROTO2PORTRANGEST{}}
		if err = port.VerifyPortFloating(eips, vpc); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		if err = port.PhaseStart(PhaseTypeBind); err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		var done bool
		done, err = Save(nil, []RES{port, eips})
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	go port.BindingPortTask(subnet, vpc, agent, vpcLocalRes)
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(port.Uuid))
}

func AppPortUnbind(c echo.Context) (err error) {
	G.logger.Debug("=========AppPortUnbind==========")
	if !G.config.MasterL3 {
		return c.JSON(http.StatusForbidden, NewAppErrorRsp("This node is not a master L3 node."))
	}
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Port UUID is required."))
	}
	var port *Port
	if port, err = LoadPort(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if port == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Port not found."))
	}
	agent := LoadAgent(port.AgentUuid)
	if agent == nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load agent."))
	}
	var subnet *Subnet
	if subnet, err = LoadSubnet(port.SubnetUuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load subnet."))
	}
	if subnet == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Subnet not found."))
	}
	var vpc *VPC
	if vpc, err = LoadVPC(subnet.VpcUuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load VPC."))
	}
	if vpc == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("VPC not ready."))
	}
	var vpcLocalRes []string
	vpcLocalRes, err = vpc.AssignLocalRes(true)
	if err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	for {
		if port.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Port is not ready."))
		}
		if port.Floating == nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("The port is not bound to an Elastic IP."))
		}
		if err = port.PhaseStart(PhaseTypeUnbind); err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		var done bool
		done, err = Save(nil, []RES{port})
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	go port.UnbindPortTask(subnet, vpc, agent, vpcLocalRes)
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(port.Uuid))
}

func AppPortApply(c echo.Context) (err error) {
	G.logger.Debug("=========AppPortApply==========")
	if !G.config.MasterL3 {
		return c.JSON(http.StatusForbidden, NewAppErrorRsp("This node is not a master L3 node."))
	}
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Port UUID is required."))
	}
	q := &PortApplyReq{}
	if err = c.Bind(q); err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Cannot parse the request."))
	}
	var port *Port
	if port, err = LoadPort(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if port == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Port not found."))
	}
	var subnet *Subnet
	if subnet, err = LoadSubnet(port.SubnetUuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load subnet."))
	}
	if subnet == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Subnet not found."))
	}
	var vpc *VPC
	if vpc, err = LoadVPC(subnet.VpcUuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load VPC."))
	}
	if vpc == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("VPC not found."))
	}
	var ofport string
	if port.ByUuid != "" {
		ofport = GetPortOfport(port.Uuid, port.AgentUuid, port.Purpose, port.ByUuid)
	}
	nid := vpc.NID

	oldSgs := map[string]*SG{}
	_oldSgs := []*SG{}
	for _, sgUuid := range port.Sgs {
		if _, ok := oldSgs[sgUuid]; !ok {

			if Contains(q.Sgs, sgUuid) {
				continue
			}
			sg, err := LoadSG(sgUuid)
			if err != nil {
				return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load security group."))
			}
			oldSgs[sgUuid] = sg
			_oldSgs = append(_oldSgs, sg)
		}
	}

	newSgs := map[string]*SG{}
	_newSgs := []*SG{}
	for _, sqUuid := range q.Sgs {
		if _, ok := newSgs[sqUuid]; !ok {
			var sg *SG
			if sg, err = LoadSG(sqUuid); err != nil {
				return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load security group."))
			}
			newSgs[sqUuid] = sg
			_newSgs = append(_newSgs, sg)
		}
	}
	hostsg, _ := LoadHostSG(port.AgentUuid)
	for {
		if port.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Port is not ready."))
		}
		var reses []RES
		if ofport != "" {
			hostsg.Ports[port.Uuid] = ofport
		}
		reses = []RES{hostsg, port}
		sgUuidMap := map[string]struct{}{}
		for _, sg := range _oldSgs {
			sg.Ports = Remove(sg.Ports, port.Uuid)
			sg.Addrs[nid] = Remove(sg.Addrs[nid], port.Ip)
			if len(sg.Addrs[nid]) == 0 {
				delete(sg.Addrs, nid)
			}
			reses = append(reses, sg)
			sgUuidMap[sg.Uuid] = struct{}{}
		}
		port.Sgs = []string{}
		for _, sg := range _newSgs {
			if sg.Addrs[nid] == nil {
				sg.Addrs[nid] = []string{}
			}
			if !Contains(sg.Addrs[nid], port.Ip) {
				sg.Addrs[nid] = append(sg.Addrs[nid], port.Ip)
			}
			if sg.Ports == nil {
				sg.Ports = []string{}
			}
			if !Contains(sg.Ports, port.Uuid) {
				sg.Ports = append(sg.Ports, port.Uuid)
			}
			if !Contains[RES](reses, sg) {
				reses = append(reses, sg)
			}
			sgUuidMap[sg.Uuid] = struct{}{}
			port.Sgs = append(port.Sgs, sg.Uuid)
		}
		port.PhaseStop(true)

		port.UpdateHostSG(hostsg)
		sgUuids := GetKeys(sgUuidMap)

		hostSgs := HostSgs2Update(sgUuids)
		for agentUuid, hostSG := range hostSgs {
			if agentUuid != port.AgentUuid && hostSG != nil {
				reses = append(reses, hostSG)
			}
		}
		var done bool
		done, err = Save(nil, reses)
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	return c.JSON(http.StatusOK, AppOKRsp(port.Uuid))
}
