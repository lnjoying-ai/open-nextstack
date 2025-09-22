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
	"math/rand"
	"net"
	"net/http"
	"strconv"
	"sync"

	"github.com/labstack/echo"
	"github.com/spf13/cobra"
	"go.uber.org/zap"
)

type VPC struct {
	Cidr     string              `json:"cidr"`
	NID      string              `json:"nid"`
	Subnets  map[string]string   `json:"subnets"`
	Hosts    map[string]string   `json:"hosts"`
	LocalRes map[string][]string `json:"local_res"`
	SHAREREST
	PHASEINFO
}

type VpcFlags struct {
	uuid     string
	cidr     string
	abnormal bool
}

type VpcAddReq struct {
	Cidr string `json:"cidr"`
}

type VpcGetRsp struct {
	Status string `json:"status"`
	Cidr   string `json:"cidr"`
	NID    string `json:"nid"`
	SHAREREST
	PHASEINFO
}

var vpcFlags VpcFlags

func LoadVPC(vpc_uuid string) (vpc *VPC, err error) {
	vpc = &VPC{SHAREREST: SHAREREST{UUIDINFO: UUIDINFO{Uuid: vpc_uuid}, ETCDINFO: ETCDINFO{Type: "vpc"}}}
	path := vpc.GetKey()
	var ret RES
	if ret, err = LoadRes(path, vpc); err != nil {
		return nil, err
	}
	if ret != nil {
		vpc, ok := ret.(*VPC)
		if ok {
			return vpc, nil
		}
	}
	return nil, nil
}

func (vpc *VPC) Add(vpcLocalRes []string) (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()

	if _, ok := G.deployedRest[vpc.Uuid]; ok {
		return nil
	}
	vlanid := vpcLocalRes[0]
	lanVpcNsNicMac := vpcLocalRes[2]
	ns := GetNsName(vlanid)
	lanVpcNics := LanVethPair(vlanid)
	lanVpcNic := lanVpcNics[0]
	lanVpcNsNic := lanVpcNics[1]

	if err = osNetnsAdd(ns); err != nil {
		G.logger.Error("cannot add netns", zap.Error(err))
		return err
	}

	if err = osNicUp("lo", ns); err != nil {
		G.logger.Error("cannot up lo", zap.Error(err))
		return err
	}

	if osNicExists(lanVpcNic, "") {
		if err = osNicDel(lanVpcNic, ""); err != nil {
			G.logger.Error("cannot delete nic", zap.Error(err))
			return err
		}
	}

	if err = vpc.VpcNicAdd(lanVpcNic, lanVpcNsNic, lanVpcNsNicMac, vlanid); err != nil {
		G.logger.Error("cannot add vlan nic", zap.Error(err))
		return err
	}

	if err = osNicMacSet(lanVpcNsNic, lanVpcNsNicMac); err != nil {
		G.logger.Error("cannot set vlan nic mac", zap.Error(err))
		return err
	}

	if err = osNsNicAdd(ns, lanVpcNsNic); err != nil {
		G.logger.Error("cannot add nic to ns", zap.Error(err))
		return err
	}

	if err = osNicUp(lanVpcNsNic, ns); err != nil {
		G.logger.Error("cannot up nic", zap.Error(err))
		return err
	}

	wanVpcNics := WanVethPair(vlanid)
	wanVpcNic := wanVpcNics[0]
	wanVpcNsNic := wanVpcNics[1]
	if osNicExists(wanVpcNic, "") {
		if err = osNicDel(wanVpcNic, ""); err != nil {
			G.logger.Error("cannot delete nic", zap.Error(err))
			return err
		}
	}
	if err = osVethAdd(wanVpcNic, wanVpcNsNic); err != nil {
		G.logger.Error("cannot add veth", zap.Error(err))
		return err
	}

	if err = osNsNicAdd(ns, wanVpcNsNic); err != nil {
		G.logger.Error("cannot add nic to ns", zap.Error(err))
		return err
	}

	if err = osNicUp(wanVpcNic, ""); err != nil {
		G.logger.Error("cannot up nic", zap.Error(err))
		return err
	}
	if err = osNicUp(wanVpcNsNic, ns); err != nil {
		G.logger.Error("cannot up nic", zap.Error(err))
		return err
	}

	vpcIps := VpcIps(vlanid)
	wanVpcNicIp := vpcIps[0]
	wanVpcNicNsNicIp := vpcIps[1]
	manVpcNicIp := vpcIps[2]
	manVpcNsNicIp := vpcIps[3]
	if err = osNicIpAdd(wanVpcNsNic, wanVpcNicNsNicIp+"/24", ns); err != nil {
		G.logger.Error("cannot set nic ip", zap.Error(err))
		return err
	}

	if err = osRouteAdd("default", wanVpcNicIp, ns); err != nil {
		G.logger.Error("cannot add route", zap.Error(err))
		return err
	}

	if err = osRouteDevAdd(vpc.Cidr, lanVpcNsNic, ns); err != nil {
		G.logger.Error("cannot add route", zap.Error(err))
		return err
	}

	if err = osSnatAdd(wanVpcNsNic, ns); err != nil {
		G.logger.Error("cannot add snat", zap.Error(err))
		return err
	}
	if err = OvsWanVpcAdd(wanVpcNic, wanVpcNsNic, wanVpcNicIp, wanVpcNicNsNicIp, vlanid); err != nil {
		G.logger.Error("cannot add ovs wan vpc", zap.Error(err))
		return err
	}

	manVpcNics := ManVethPair(vlanid)
	manVpcNic := manVpcNics[0]
	manVpcNsNic := manVpcNics[1]
	if osNicExists(manVpcNic, "") {
		if err = osNicDel(manVpcNic, ""); err != nil {
			G.logger.Error("cannot delete nic", zap.Error(err))
			return err
		}
	}
	if err = osVethAdd(manVpcNic, manVpcNsNic); err != nil {
		G.logger.Error("cannot add veth", zap.Error(err))
		return err
	}

	if err = osNsNicAdd(ns, manVpcNsNic); err != nil {
		G.logger.Error("cannot add nic to ns", zap.Error(err))
		return err
	}

	if err = osNicUp(manVpcNic, ""); err != nil {
		G.logger.Error("cannot up nic", zap.Error(err))
		return err
	}
	if err = osNicUp(manVpcNsNic, ns); err != nil {
		G.logger.Error("cannot up nic", zap.Error(err))
		return err
	}

	if err = osNicIpAdd(manVpcNic, manVpcNicIp+"/24", ""); err != nil {
		G.logger.Error("cannot set nic ip", zap.Error(err))
		return err
	}
	if err = osNicIpAdd(manVpcNsNic, manVpcNsNicIp+"/24", ns); err != nil {
		G.logger.Error("cannot set nic ip", zap.Error(err))
		return err
	}

	if err = osRoute169Add(manVpcNsNic, ns, manVpcNsNicIp); err != nil {
		G.logger.Error("cannot add route", zap.Error(err))
		return err
	}

	if err = osSnatAdd(manVpcNsNic, ns); err != nil {
		G.logger.Error("cannot add snat", zap.Error(err))
		return err
	}
	if err = osSnatAdd3(manVpcNsNicIp); err != nil {
		G.logger.Error("cannot add snat", zap.Error(err))
		return err
	}

	if err = osClearMangle(ns); err != nil {
		G.logger.Error("cannot clear mangle", zap.Error(err))
		return err
	}

	if err = osDhcpAllow(ns, lanVpcNsNic); err != nil {
		G.logger.Error("cannot allow dhcp", zap.Error(err))
		return err
	}

	if err = osConntrackAllow(ns, lanVpcNsNic); err != nil {
		G.logger.Error("cannot allow conntrack", zap.Error(err))
		return err
	}

	if err = osDropInvalid(ns, lanVpcNsNic, vpc.Cidr); err != nil {
		G.logger.Error("cannot drop invalid", zap.Error(err))
		return err
	}

	if err = osAllowVpc(ns, lanVpcNsNic, vpc.Cidr); err != nil {
		G.logger.Error("cannot allow vpc", zap.Error(err))
		return err
	}

	if err = osMark169(ns, lanVpcNsNic); err != nil {
		G.logger.Error("cannot mark 169", zap.Error(err))
		return err
	}
	if err = osAllowMark169(ns, lanVpcNsNic); err != nil {
		G.logger.Error("cannot allow mark 169", zap.Error(err))
		return err
	}
	G.deployedRest[vpc.Uuid] = struct{}{}
	RegisterHostMonitor(vpc.Uuid, vpcLocalRes[0])
	return nil
}

func (vpc *VPC) Del() (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()

	if _, ok := G.deployedRest[vpc.Uuid]; !ok {
		return nil
	}
	var vpcLocalRes []string
	vpcLocalRes, _ = vpc.AssignLocalRes(true)
	if vpcLocalRes != nil {
		vlanid := vpcLocalRes[0]
		lanVpcNsNicMac := vpcLocalRes[2]
		ns := GetNsName(vlanid)
		lanVpcNics := LanVethPair(vlanid)
		lanVpcNic := lanVpcNics[0]
		vpcIps := VpcIps(vlanid)
		manVpcNsNicIp := vpcIps[3]

		if osNetnsExists(ns) {
			if err = osNetnsKillAll(ns); err != nil {
				G.logger.Error("cannot kill all processes in netns", zap.Error(err))
				return err
			}
		}
		vethManNics := ManVethPair(vlanid)
		if err = osSnatDel3(manVpcNsNicIp); err != nil {
			G.logger.Error("cannot delete snat", zap.Error(err))
			return err
		}
		if osNicExists(vethManNics[0], "") {
			if err = osNicDel(vethManNics[0], ""); err != nil {
				G.logger.Error("cannot delete nic", zap.Error(err))
				return err
			}
		}
		vethWanNics := WanVethPair(vlanid)
		wanVpcNic := vethWanNics[0]
		wanVpcNicIp := vpcIps[0]
		wanVpcNsNicIp := vpcIps[1]
		if err = OvsWanVpcDel(wanVpcNic, wanVpcNicIp, wanVpcNsNicIp); err != nil {
			return err
		}
		if osNicExists(wanVpcNic, "") {
			if err = osNicDel(wanVpcNic, ""); err != nil {
				G.logger.Error("cannot delete nic", zap.Error(err))
				return err
			}
		}
		if err = vpc.VpcNicDel(lanVpcNic, lanVpcNsNicMac, vlanid); err != nil {
			G.logger.Error("cannot delete vlan nic", zap.Error(err))
			return err
		}
		if osNetnsExists(ns) {
			if err = osNetnsDel(ns); err != nil {
				G.logger.Error("cannot delete netns", zap.Error(err))
				return err
			}
		}
	}
	delete(G.deployedRest, vpc.Uuid)
	if _, ok := G.ctxes[vpc.Uuid]; ok {
		G.ctxes[vpc.Uuid] <- struct{}{}
	}
	return nil
}

func (vpc *VPC) Restore() (err error) {
	if !G.config.L3Mode {
		return nil
	}
	var vpcLocalRes []string

	if vpcLocalRes, err = vpc.AssignLocalRes(true); err != nil {
		return nil
	}
	if err = vpc.Add(vpcLocalRes); err != nil {
		G.logger.Error("cannot add vpc", zap.Error(err))
		return err
	}
	var wg sync.WaitGroup
	for subnetUuid := range vpc.Subnets {
		var subnet *Subnet
		if subnet, err = LoadSubnet(subnetUuid); err != nil {
			continue
		}
		if !subnet.NotReady() {
			wg.Add(1)
			go func(subnet *Subnet, wg *sync.WaitGroup) {
				defer wg.Done()
				if err = subnet.Restore(vpc, vpcLocalRes); err != nil {
					G.logger.Error("cannot add subnet", zap.Error(err))
				}
			}(subnet, &wg)
		}
	}
	wg.Wait()
	return nil
}

func (vpc *VPC) VpcNicAdd(lanVpcNic string, lanVpcNsNic string, lanVpcNsNicMac string, vlanid string) (err error) {
	lanBridge := G.config.LanBridge
	if err = osVethAdd(lanVpcNic, lanVpcNsNic); err != nil {
		G.logger.Error("cannot add veth", zap.Error(err))
		return err
	}
	if err = OvsVpcNicAdd(lanBridge, lanVpcNic, vlanid); err != nil {
		G.logger.Error("cannot add vlan nic", zap.Error(err))
		return err
	}
	if err = osNicUp(lanVpcNic, ""); err != nil {
		G.logger.Error("cannot up nic", zap.Error(err))
		return err
	}
	vpcLanFlows := "" +

		fmt.Sprintf("table=0,priority=120,arp,arp_op=1,in_port=%s,action=load:%s->NXM_NX_REG6[],resubmit(,10)\n", lanVpcNic, vlanid) +

		fmt.Sprintf("table=74,priority=120,ip,dl_dst=%s,ct_label[0..47]=0x0,action=output:%s", lanVpcNsNicMac, lanVpcNic)
	if err = OvsFlowsLoad(lanBridge, vpcLanFlows); err != nil {
		G.logger.Error("cannot load flows", zap.Error(err))
		return err
	}
	return nil
}

func (vpc *VPC) VpcNicDel(lanVpcNic string, lanVpcNsNicMac string, vlanid string) (err error) {
	lanBridge := G.config.LanBridge
	vpcLanFlows := fmt.Sprintf("delete table=0,arp,arp_op=1,in_port=%s", lanVpcNic)
	vpcLanFlows += "\n" + fmt.Sprintf("delete table=74,ip,dl_dst=%s", lanVpcNsNicMac)
	if err = OvsFlowsLoad(lanBridge, vpcLanFlows); err != nil {
		G.logger.Error("cannot load flows", zap.Error(err))
		return err
	}
	if err = OvsNicDel(G.config.LanBridge, lanVpcNic); err != nil {
		G.logger.Error("cannot delete vlan nic", zap.Error(err))
		return err
	}
	if err = osNicDel(lanVpcNic, ""); err != nil {
		G.logger.Error("cannot delete nic", zap.Error(err))
		return err
	}
	return nil
}

func (vpc *VPC) AddingVpcTask() {
	var err error
	var vpcLocalRes []string
	success := false
	if vpcLocalRes, err = vpc.AssignLocalRes(false); err != nil {
		G.logger.Error("cannot assign local res", zap.Error(err))
		goto UPDATE
	}
	if err = vpc.Add(vpcLocalRes); err != nil {
		G.logger.Error("cannot add vpc", zap.Error(err))
		goto UPDATE
	}
	success = true
UPDATE:
	if _, err = UpdatePhaseStop(vpc, success); err != nil {
		G.logger.Error("cannot update vpc phase", zap.Error(err))
	}
	if success {
		NoticeDnsmasq(vpcLocalRes[0])
	}
}

func (vpc *VPC) DeletingVpcTask() {
	var err error
	nids, _ := LoadNids()
	var vlanid string
	if _, ok := vpc.LocalRes[G.config.Uuid]; ok {
		vlanid = vpc.LocalRes[G.config.Uuid][0]
	}
	if err = vpc.Del(); err != nil {
		G.logger.Error("cannot delete vpc", zap.Error(err))
		goto UPDATE
	}
	_ = vpc.UnAssignLocalRes(G.config.Uuid)
	for {
		delete(nids.NidsInfo, vpc.NID)
		var done bool
		if done, err = Save([]RES{vpc}, []RES{nids}); err != nil {
			G.logger.Error("cannot save res", zap.Error(err))
			goto UPDATE
		}
		if done {
			if vlanid != "" {
				NoticeDnsmasq(vlanid)
			}
			return
		}
	}
UPDATE:
	if _, err = UpdatePhaseStop(vpc, false); err != nil {
		G.logger.Error("cannot update vpc phase", zap.Error(err))
	}
}

func (vpc *VPC) VerifyUuid() error {
	return AssignUuid("vpc", vpc)
}

func (vpc *VPC) VerifyVpcId(nids *NIDS) error {
	for {
		var nid string
		if G.config.VniRange != "" {
			nid = strconv.FormatUint(uint64(G.config.vni_from+uint32(rand.Intn(int(G.config.vni_to-G.config.vni_from+1)))), 10)
		} else {
			nid = strconv.FormatUint(uint64(G.config.vlan_from+uint32(rand.Intn(int(G.config.vlan_to-G.config.vlan_from+1)))), 10)
		}
		if _, ok := nids.NidsInfo[nid]; ok {
			continue
		}
		nids.NidsInfo[nid] = vpc.Uuid
		vpc.NID = nid
		return nil
	}
}

func (vpc *VPC) VerifyVpcCidr() error {
	_, cidr, err := net.ParseCIDR(vpc.Cidr)
	if err != nil {
		return fmt.Errorf("vpc cidr %s format error", vpc.Cidr)
	}
	prefix, _ := cidr.Mask.Size()
	_, net10, _ := net.ParseCIDR("10.0.0.0/8")
	_, net172, _ := net.ParseCIDR("172.16.0.0/12")
	_, net192, _ := net.ParseCIDR("192.168.0.0/16")
	if prefix <= 28 && (IsSubnet(net10, cidr) && prefix >= 8 || (IsSubnet(net172, cidr) && prefix >= 12) || (IsSubnet(net192, cidr) && prefix >= 16)) {
		return nil
	}
	return fmt.Errorf("invalid vpc cidr %s", vpc.Cidr)
}

func (vpc *VPC) AssignLocalRes(retrieveOnly bool) ([]string, error) {

	if !G.config.L3Mode {
		for _, v := range vpc.LocalRes {
			if v[len(v)-1] == "1" {
				return v, nil
			}
		}
		return nil, fmt.Errorf("no local res")
	}
	if _, ok := vpc.LocalRes[G.config.Uuid]; ok {
		return vpc.LocalRes[G.config.Uuid], nil
	}

	if retrieveOnly {
		return nil, fmt.Errorf("no local res")
	}
	vlanids, _ := LoadVlanids()
	macs, _ := LoadMacs()
	for {
		var vlanid string
		var mac string
		if G.config.VniRange != "" {
			for {

				vlanid = strconv.FormatUint(uint64(G.config.vlan_from+uint32(rand.Intn(int(G.config.vlan_to-G.config.vlan_from+1)))), 10)
				if vlanids.VlanidsInfo[G.config.Uuid] == nil {
					vlanids.VlanidsInfo[G.config.Uuid] = make(map[string]string)
				}
				if _, ok := vlanids.VlanidsInfo[G.config.Uuid][vlanid]; !ok {
					vlanids.VlanidsInfo[G.config.Uuid][vlanid] = vpc.Uuid
					break
				}
			}
		} else {
			vlanid = vpc.NID
		}

		for {
			mac = GenMac("vpc")
			if _, ok := macs.MacsInfo[mac]; ok {
				continue
			}
			break
		}
		masterL3 := "1"
		if !G.config.MasterL3 {
			masterL3 = "0"
		}

		vpc.LocalRes[G.config.Uuid] = []string{vlanid, G.config.LanNic, mac, masterL3}
		var done bool
		var err error
		done, err = Save(nil, []RES{vpc, vlanids})
		if err != nil {
			return nil, err
		}
		if done {
			break
		}
	}
	return vpc.LocalRes[G.config.Uuid], nil
}

func (vpc *VPC) AssignLocalResMl3() {
	vlanids, _ := LoadVlanids()
	macs, _ := LoadMacs()
	var vlanid string
	var mac string
	if G.config.VniRange != "" {
		for {

			vlanid = strconv.FormatUint(uint64(G.config.vlan_from+uint32(rand.Intn(int(G.config.vlan_to-G.config.vlan_from+1)))), 10)
			if vlanids.VlanidsInfo[G.config.Uuid] == nil {
				vlanids.VlanidsInfo[G.config.Uuid] = make(map[string]string)
			}
			if _, ok := vlanids.VlanidsInfo[G.config.Uuid][vlanid]; !ok {
				vlanids.VlanidsInfo[G.config.Uuid][vlanid] = vpc.Uuid
				break
			}
		}
	} else {
		vlanid = vpc.NID
	}

	for {
		mac = GenMac("vpc")
		if _, ok := macs.MacsInfo[mac]; !ok {
			break
		}
	}
	masterL3 := "1"

	vpc.LocalRes[G.config.Uuid] = []string{vlanid, G.config.LanNic, mac, masterL3}
}

func (vpc *VPC) UnAssignLocalRes(agentUuid string) (err error) {
	if _, ok := vpc.LocalRes[agentUuid]; !ok {
		return nil
	}
	vlanids, _ := LoadVlanids()
	macs, _ := LoadMacs()
	for {
		if G.config.VniRange != "" {
			vlanid := vpc.LocalRes[agentUuid][0]
			delete(vlanids.VlanidsInfo[agentUuid], vlanid)
		}
		mac := vpc.LocalRes[agentUuid][1]
		delete(macs.MacsInfo, mac)
		delete(vpc.LocalRes, agentUuid)
		var done bool
		var err error
		done, err = Save(nil, []RES{vpc, vlanids, macs})
		if err != nil {
			return err
		}
		if done {

			break
		}
	}
	return nil
}

func (vpc *VPC) HasLocalSubnet() bool {
	subnets, _ := LoadSubnets()
	if subnets.SubnetsInfo[G.config.Uuid] == nil {
		return false
	}
	for _, vpcUuid := range subnets.SubnetsInfo[G.config.Uuid] {
		if vpcUuid == vpc.Uuid {
			return true
		}
	}
	return false
}

func VpcCmdParser(command *cobra.Command) {
	var err error
	var vpcCmd = &cobra.Command{
		Use:   "vpc",
		Short: "VPC management",
		Run: func(cmd *cobra.Command, args []string) {
			fmt.Println("please specify a subcommand for vpc management. use -h for help.")
		},
	}
	var vpcAddCmd = &cobra.Command{
		Use:   "add",
		Short: "Add a VPC",
		Run:   VpcAddHandle,
	}
	vpcAddCmd.Flags().StringVar(&vpcFlags.cidr, "cidr", "", "set the CIDR of the VPC")
	if err = vpcAddCmd.MarkFlagRequired("cidr"); err != nil {
		panic(err)
	}
	var vpcGetCmd = &cobra.Command{
		Use:   "get",
		Short: "Get a VPC",
		Run:   VpcGetHandle,
	}
	vpcGetCmd.Flags().StringVarP(&vpcFlags.uuid, "uuid", "U", "", "set the UUID of the VPC")
	if err = vpcGetCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var vpcDelCmd = &cobra.Command{
		Use:   "del",
		Short: "Delete a VPC",
		Run:   VpcDelHandle,
	}
	vpcDelCmd.Flags().StringVarP(&vpcFlags.uuid, "uuid", "U", "", "set the UUID of the VPC")
	if err = vpcDelCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var vpcListCmd = &cobra.Command{
		Use:   "list",
		Short: "List all VPCs",
		Run:   VpcListHandle,
	}
	vpcListCmd.Flags().BoolVar(&vpcFlags.abnormal, "abnormal", false, "list abnormal VPCs")
	vpcCmd.AddCommand(vpcAddCmd, vpcDelCmd, vpcGetCmd, vpcListCmd)
	command.AddCommand(vpcCmd)
}

func VpcAddHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/vpcs", G.Host, G.Port)
	data := map[string]interface{}{"cidr": vpcFlags.cidr}
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

func VpcGetHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/vpcs/%s", G.Host, G.Port, vpcFlags.uuid)
	req, _ := http.NewRequest("GET", url, nil)
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

func VpcDelHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/vpcs/%s", G.Host, G.Port, vpcFlags.uuid)
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

func VpcListHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/vpcs", G.Host, G.Port)
	data := map[string]interface{}{}
	if vpcFlags.abnormal {
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

func VpcAppSetup() {
	if !G.config.L3Mode {
		return
	}
	G.echoServer.POST("/v1/vpcs", AppVpcAdd)
	G.echoServer.DELETE("/v1/vpcs/:uuid", AppVpcDel)
	G.echoServer.GET("/v1/vpcs", AppVpcList)
	G.echoServer.GET("/v1/vpcs/:uuid", AppVpcGet)
}

func AppVpcAdd(c echo.Context) (err error) {
	G.logger.Debug("=========AppVpcAdd==========")
	if !G.config.MasterL3 {
		return c.JSON(http.StatusForbidden, NewAppErrorRsp("This node is not a master L3 node."))
	}
	q := VpcAddReq{}
	if err = c.Bind(&q); err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Cannot parse the request."))
	}
	var vpc *VPC
	nids, _ := LoadNids()
	for {
		vpc = &VPC{Subnets: map[string]string{}, Hosts: map[string]string{}, LocalRes: map[string][]string{}, SHAREREST: SHAREREST{ETCDINFO: ETCDINFO{Type: "vpc"}}}
		vpc.Cidr = q.Cidr
		_ = vpc.PhaseStart(PhaseTypeAdd)
		if err = vpc.VerifyUuid(); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		if err = vpc.VerifyVpcCidr(); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		if err = vpc.VerifyVpcId(nids); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		var done bool
		done, err = Save(nil, []RES{vpc, nids})
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	go vpc.AddingVpcTask()
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(vpc.Uuid))
}

func AppVpcList(c echo.Context) (err error) {
	G.logger.Debug("=========AppVpcList==========")
	q := AppListReq{}
	if err = c.Bind(&q); err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
	}
	vpcUuids := []string{}
	rests := LoadRests("vpc")
	for _, rest := range rests["vpc"] {
		vpc, ok := rest.(*VPC)
		if !ok {
			continue
		}
		if q.Abnormal {
			if vpc.NotReady() {
				vpcUuids = append(vpcUuids, vpc.Uuid)
			}
		} else {
			vpcUuids = append(vpcUuids, vpc.Uuid)
		}
	}
	jsonMap := make(map[string]interface{})
	jsonMap["status"] = "ok"
	jsonMap["vpc_count"] = len(vpcUuids)
	jsonMap["vpcs"] = vpcUuids
	return c.JSON(http.StatusOK, jsonMap)
}

func AppVpcGet(c echo.Context) (err error) {
	G.logger.Debug("=========AppVpcGet==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("VPC UUID is required."))
	}
	var vpc *VPC
	if vpc, err = LoadVPC(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if vpc == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("VPC not ready."))
	}
	rsp := VpcGetRsp{
		Status:    "ok",
		Cidr:      vpc.Cidr,
		NID:       vpc.NID,
		SHAREREST: vpc.SHAREREST,
		PHASEINFO: vpc.PHASEINFO}
	return c.JSON(http.StatusOK, &rsp)
}

func AppVpcDel(c echo.Context) (err error) {
	G.logger.Debug("=========AppVpcDel==========")
	if !G.config.MasterL3 {
		return c.JSON(http.StatusForbidden, NewAppErrorRsp("This node is not a master L3 node."))
	}
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("VPC UUID is required."))
	}
	var vpc *VPC
	if vpc, err = LoadVPC(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if vpc == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("VPC not ready."))
	}
	for {
		if len(vpc.Subnets) > 0 {
			return c.JSON(http.StatusForbidden, NewAppErrorRsp("VPC contains subnets."))
		}
		if len(vpc.Hosts) > 0 {
			return c.JSON(http.StatusForbidden, NewAppErrorRsp("VPC contains hosts."))
		}
		if err = vpc.PhaseStart(PhaseTypeDel); err != nil {
			return c.JSON(http.StatusForbidden, NewAppErrorRsp(err.Error()))
		}
		var done bool
		done, err = Save(nil, []RES{vpc})
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	go vpc.DeletingVpcTask()
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(vpc.Uuid))
}
