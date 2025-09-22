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
	"sync"

	"github.com/labstack/echo"
	"github.com/spf13/cobra"
	"go.uber.org/zap"
)

type Subnet struct {
	Cidr     string              `json:"cidr"`
	VpcUuid  string              `json:"vpc"`
	Ports    map[string]string   `json:"ports"`
	LocalRes map[string][]string `json:"local_res"`
	SHAREREST
	PHASEINFO
}

type SubnetFlags struct {
	uuid     string
	cidr     string
	abnormal bool
}

type SubnetAddReq struct {
	Cidr string `json:"cidr"`
}

type SubnetGetRsp struct {
	Status  string `json:"status"`
	Cidr    string `json:"cidr"`
	VpcUuid string `json:"vpc"`
	SHAREREST
	PHASEINFO
}

var subnetFlags SubnetFlags

func LoadSubnet(subnet_uuid string) (subnet *Subnet, err error) {
	subnet = &Subnet{SHAREREST: SHAREREST{UUIDINFO: UUIDINFO{Uuid: subnet_uuid}, ETCDINFO: ETCDINFO{Type: "subnet"}}}
	path := subnet.GetKey()
	var ret RES
	if ret, err = LoadRes(path, subnet); err != nil {
		return nil, err
	}
	if ret != nil {
		subnet, ok := ret.(*Subnet)
		if ok {
			return subnet, nil
		}
	}
	return nil, nil
}

func (subnet *Subnet) Add(vpc *VPC, vpcLocalRes []string) (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()

	if _, ok := G.deployedRest[subnet.Uuid]; ok {
		return nil
	}
	vlanid := vpcLocalRes[0]
	lanNic := vpcLocalRes[1]
	ns := GetNsName(vlanid)
	nsLanNic := GetLanVpcNsNic(vlanid)
	ips := SubnetIps(subnet.Cidr)

	if err = osNicIpAdd(nsLanNic, ips[0], ns); err != nil {
		G.logger.Error("cannot add ip address to nic", zap.Error(err))
	}

	if err = osNicIpAdd(nsLanNic, ips[1], ns); err != nil {
		G.logger.Error("cannot add ip address to nic", zap.Error(err))
		return err
	}
	if !G.config.MasterL3 {
		if err = OvsDvrSubnetAdd(ips[2], ips[3], lanNic, vlanid); err != nil {
			G.logger.Error("cannot add ovs flows to lan bridge for new subnet", zap.Error(err))
			return err
		}
	}
	G.deployedRest[subnet.Uuid] = struct{}{}
	return nil
}

func (subnet *Subnet) Del(vpc *VPC, vpcLocalRes []string) (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()

	if _, ok := G.deployedRest[subnet.Uuid]; !ok {
		return nil
	}
	if vpcLocalRes != nil {
		vlanid := vpcLocalRes[0]
		lanNic := vpcLocalRes[1]
		ns := GetNsName(vlanid)
		nsLanNic := GetLanVpcNsNic(vlanid)
		ips := SubnetIps(subnet.Cidr)
		if err = osNicIpDel(nsLanNic, ips[0], ns); err != nil {
			G.logger.Error("cannot delete ip address from nic", zap.Error(err))
		}
		if err = osNicIpDel(nsLanNic, ips[1], ns); err != nil {
			G.logger.Error("cannot delete ip address from nic", zap.Error(err))
		}
		if !G.config.MasterL3 {
			if err = OvsDvrSubnetDel(ips[2], ips[3], lanNic, vlanid); err != nil {
				G.logger.Error("cannot delete ovs flows to lan bridge for new subnet", zap.Error(err))
			}
		}
	}
	delete(G.deployedRest, subnet.Uuid)
	return nil
}

func (subnet *Subnet) Restore(vpc *VPC, vpcLocalRes []string) (err error) {
	if _, err = subnet.AssignLocalRes(true); err != nil {
		return nil
	}
	if err = subnet.Add(vpc, vpcLocalRes); err != nil {
		G.logger.Error("cannot add subnet", zap.Error(err))
		return err
	}
	var wg sync.WaitGroup
	for portUuid := range subnet.Ports {
		var port *Port
		if port, err = LoadPort(portUuid); err != nil {
			continue
		}
		if port.PhaseType == PhaseTypeAttach || port.PhaseType == PhaseTypeResume || !port.NotReady() {
			agent := LoadAgent(port.AgentUuid)
			if agent == nil {
				G.logger.Error("Cannot load agent.", zap.String("agent", port.AgentUuid))
				continue
			}
			wg.Add(1)
			go func(port *Port, subnet *Subnet, vpc *VPC, agent *Agent, vpcLocalRes []string, wg *sync.WaitGroup) {
				defer wg.Done()
				if err = port.Restore(subnet, vpc, agent, vpcLocalRes); err != nil {
					G.logger.Error("cannot add port", zap.Error(err))
				}
			}(port, subnet, vpc, agent, vpcLocalRes, &wg)
		}
	}
	wg.Wait()
	return nil
}

func (subnet *Subnet) AddingSubnetTask(vpc *VPC, vpcLocalRes []string) {
	var err error
	success := false
	if _, err = subnet.AssignLocalRes(false); err != nil {
		G.logger.Error("cannot assign local res", zap.Error(err))
		goto UPDATE
	}
	if err = subnet.Add(vpc, vpcLocalRes); err != nil {
		G.logger.Error("cannot add subnet", zap.Error(err))
		goto UPDATE
	}
	success = true
UPDATE:
	if _, err = UpdatePhaseStop(subnet, success); err != nil {
		G.logger.Error("cannot update subnet phase", zap.Error(err))
	}
	if success {
		NoticeDnsmasq(vpcLocalRes[0])
	}
}

func (subnet *Subnet) DeletingSubnetTask(vpc *VPC, vpcLocalRes []string) {
	var err error
	if err = subnet.Del(vpc, vpcLocalRes); err != nil {
		G.logger.Error("cannot delete subnet", zap.Error(err))
		goto UPDATE
	}
	_ = subnet.UnAssignLocalRes(G.config.Uuid)
	for {
		delete(vpc.Subnets, subnet.Uuid)
		var done bool
		if done, err = Save([]RES{subnet}, []RES{vpc}); err != nil {
			G.logger.Error("cannot save subnet", zap.Error(err))
			goto UPDATE
		}
		if done {
			if vpcLocalRes != nil {
				NoticeDnsmasq(vpcLocalRes[0])
			}
			return
		}
	}
UPDATE:
	if _, err = UpdatePhaseStop(subnet, false); err != nil {
		G.logger.Error("cannot update subnet phase", zap.Error(err))
	}
}

func (subnet *Subnet) VerifyUuid() error {
	return AssignUuid("subnet", subnet)
}

func (subnet *Subnet) VerifySubnetCidr(vpc *VPC) (err error) {
	var subnetCidr *net.IPNet
	var vpcCidr *net.IPNet
	_, subnetCidr, err = net.ParseCIDR(subnet.Cidr)
	if err != nil {
		return fmt.Errorf("subnet cidr %s format error", subnet.Cidr)
	}
	_, vpcCidr, err = net.ParseCIDR(vpc.Cidr)
	if err != nil {
		return fmt.Errorf("vpc cidr %s format error", vpc.Cidr)
	}
	if !IsSubnet(vpcCidr, subnetCidr) {
		return fmt.Errorf("subnet cidr %s not in vpc cidr %s", subnet.Cidr, vpc.Cidr)
	}
	for subnetUuid, cidr := range vpc.Subnets {
		var scidr *net.IPNet
		if _, scidr, err = net.ParseCIDR(cidr); err != nil {
			return fmt.Errorf("vpc subnet cidr %s format error", cidr)
		}
		if IsOverlap(scidr, subnetCidr) {
			return fmt.Errorf("subnet cidr %s overlap with vpc subnet %s", subnet.Cidr, subnetUuid)
		}
	}
	vpc.Subnets[subnet.Uuid] = subnet.Cidr
	return nil
}

func (subnet *Subnet) AssignLocalRes(retrieveOnly bool) ([]string, error) {
	if !G.config.L3Mode {
		for _, v := range subnet.LocalRes {
			if v[len(v)-1] == "1" {
				return v, nil
			}
		}
		return nil, fmt.Errorf("no local res")
	}

	if _, ok := subnet.LocalRes[G.config.Uuid]; ok {
		return subnet.LocalRes[G.config.Uuid], nil
	}

	if retrieveOnly {
		return nil, fmt.Errorf("no local res")
	}
	subnets, _ := LoadSubnets()
	for {
		masterL3 := "1"
		if !G.config.MasterL3 {
			masterL3 = "0"
		}
		subnet.LocalRes[G.config.Uuid] = []string{masterL3}
		if subnets.SubnetsInfo[G.config.Uuid] == nil {
			subnets.SubnetsInfo[G.config.Uuid] = make(map[string]string)
		}
		subnets.SubnetsInfo[G.config.Uuid][subnet.Uuid] = subnet.VpcUuid
		var done bool
		var err error
		done, err = Save(nil, []RES{subnet, subnets})
		if err != nil {
			return nil, err
		}
		if done {
			break
		}
	}
	return subnet.LocalRes[G.config.Uuid], nil
}

func (subnet *Subnet) UnAssignLocalRes(agentUuid string) (err error) {
	if _, ok := subnet.LocalRes[agentUuid]; !ok {
		return nil
	}
	subnets, _ := LoadSubnets()
	for {
		delete(subnet.LocalRes, agentUuid)
		delete(subnets.SubnetsInfo[agentUuid], subnet.Uuid)
		var done bool
		var err error
		done, err = Save(nil, []RES{subnet, subnets})
		if err != nil {
			return err
		}
		if done {
			break
		}
	}
	return nil
}

func (subnet *Subnet) HasLocalPort() bool {
	ports, _ := LoadPorts()
	if ports.PortsInfo[G.config.Uuid] == nil {
		return false
	}
	for _, subnetUuid := range ports.PortsInfo[G.config.Uuid] {
		if subnetUuid == subnet.Uuid {
			return true
		}
	}
	return false
}

func SubnetCmdParser(command *cobra.Command) {
	var err error
	var subnetCmd = &cobra.Command{
		Use:   "subnet",
		Short: "Subnet management",
		Run: func(cmd *cobra.Command, args []string) {
			fmt.Println("please specify a subcommand for subnet management. use -h for help.")
		},
	}
	var subnetAddCmd = &cobra.Command{
		Use:   "add",
		Short: "Add a Subnet",
		Run:   SubnetAddHandle,
	}
	subnetAddCmd.Flags().StringVar(&subnetFlags.cidr, "cidr", "", "set the CIDR of the subnet")
	if err = subnetAddCmd.MarkFlagRequired("cidr"); err != nil {
		panic(err)
	}
	subnetAddCmd.Flags().StringVarP(&subnetFlags.uuid, "uuid", "U", "", "set the UUID of the VPC")
	if err = subnetAddCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var subnetGetCmd = &cobra.Command{
		Use:   "get",
		Short: "Get a Subnet",
		Run:   SubnetGetHandle,
	}
	subnetGetCmd.Flags().StringVarP(&subnetFlags.uuid, "uuid", "U", "", "set the UUID of the subnet")
	if err = subnetGetCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var subnetDelCmd = &cobra.Command{
		Use:   "del",
		Short: "Delete a Subnet",
		Run:   SubnetDelHandle,
	}
	subnetDelCmd.Flags().StringVarP(&subnetFlags.uuid, "uuid", "U", "", "set the UUID of the subnet")
	if err = subnetDelCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var subnetListCmd = &cobra.Command{
		Use:   "list",
		Short: "List all Subnets",
		Run:   SubnetListHandle,
	}
	subnetListCmd.Flags().BoolVar(&subnetFlags.abnormal, "abnormal", false, "list abnormal subnets")
	subnetListCmd.Flags().StringVarP(&subnetFlags.uuid, "uuid", "U", "", "set the UUID of the VPC")
	if err = subnetGetCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	subnetCmd.AddCommand(subnetAddCmd, subnetDelCmd, subnetGetCmd, subnetListCmd)
	command.AddCommand(subnetCmd)
}

func SubnetAddHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/vpcs/%s/subnets", G.Host, G.Port, subnetFlags.uuid)
	data := map[string]interface{}{"cidr": subnetFlags.cidr}
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

func SubnetGetHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/subnets/%s", G.Host, G.Port, subnetFlags.uuid)
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

func SubnetDelHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/subnets/%s", G.Host, G.Port, subnetFlags.uuid)
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

func SubnetListHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/vpcs/%s/subnets", G.Host, G.Port, subnetFlags.uuid)
	data := map[string]interface{}{}
	if subnetFlags.abnormal {
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

func SubnetAppSetup() {
	if !G.config.L3Mode {
		return
	}
	G.echoServer.POST("/v1/vpcs/:uuid/subnets", AppSubnetAdd)
	G.echoServer.DELETE("/v1/subnets/:uuid", AppSubnetDel)
	G.echoServer.GET("/v1/vpcs/:uuid/subnets", AppSubnetList)
	G.echoServer.GET("/v1/subnets/:uuid", AppSubnetGet)
}

func AppSubnetAdd(c echo.Context) (err error) {
	G.logger.Debug("=========AppSubnetAdd==========")
	if !G.config.MasterL3 {
		return c.JSON(http.StatusForbidden, NewAppErrorRsp("This node is not a master L3 node."))
	}
	var subnet *Subnet
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("VPC UUID is required."))
	}
	vpc, err := LoadVPC(uuid)
	if err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load VPC."))
	}
	if vpc == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("VPC not ready."))
	}
	var vpcLocalRes []string
	if vpcLocalRes, err = vpc.AssignLocalRes(true); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	q := &SubnetAddReq{}
	if err = c.Bind(q); err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Cannot parse the request."))
	}
	for {
		if vpc.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("VPC is not ready."))
		}
		subnet = &Subnet{Ports: map[string]string{}, LocalRes: map[string][]string{}, SHAREREST: SHAREREST{ETCDINFO: ETCDINFO{Type: "subnet"}}}
		subnet.Cidr = q.Cidr
		subnet.VpcUuid = vpc.Uuid
		_ = subnet.PhaseStart(PhaseTypeAdd)
		if err = subnet.VerifyUuid(); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		if err = subnet.VerifySubnetCidr(vpc); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		var done bool
		done, err = Save(nil, []RES{subnet, vpc})
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	go subnet.AddingSubnetTask(vpc, vpcLocalRes)
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(subnet.Uuid))
}

func AppSubnetList(c echo.Context) (err error) {
	G.logger.Debug("=========AppSubnetList==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("VPC UUID is required."))
	}
	var vpc *VPC
	if vpc, err = LoadVPC(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load VPC."))
	}
	if vpc == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("VPC not ready."))
	}
	q := AppListReq{}
	if err = c.Bind(&q); err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
	}
	subnetUuids := []string{}
	rests := LoadRests("subnet")
	for subnetUuid := range vpc.Subnets {
		s := &Subnet{SHAREREST: SHAREREST{UUIDINFO: UUIDINFO{Uuid: subnetUuid}, ETCDINFO: ETCDINFO{Type: "subnet"}}}
		if rest := GetRest(rests, "subnet", s.GetKey()); rest != nil {
			subnet, ok := rest.(*Subnet)
			if !ok {
				continue
			}
			if q.Abnormal {
				if subnet.NotReady() {
					subnetUuids = append(subnetUuids, subnet.Uuid)
				}
			} else {
				subnetUuids = append(subnetUuids, subnet.Uuid)
			}
		}
	}
	jsonMap := make(map[string]interface {
	})
	jsonMap["status"] = "ok"
	jsonMap["subnet_count"] = len(subnetUuids)
	jsonMap["subnets"] = subnetUuids
	return c.JSON(http.StatusOK, jsonMap)
}

func AppSubnetGet(c echo.Context) (err error) {
	G.logger.Debug("=========AppSubnetGet==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Subnet UUID is required."))
	}
	var subnet *Subnet
	if subnet, err = LoadSubnet(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if subnet == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Subnet not found."))
	}
	rsp := SubnetGetRsp{
		Status:    "ok",
		Cidr:      subnet.Cidr,
		VpcUuid:   subnet.VpcUuid,
		SHAREREST: subnet.SHAREREST,
		PHASEINFO: subnet.PHASEINFO}
	return c.JSON(http.StatusOK, &rsp)
}

func AppSubnetDel(c echo.Context) (err error) {
	G.logger.Debug("=========AppSubnetDel==========")
	if !G.config.MasterL3 {
		return c.JSON(http.StatusForbidden, NewAppErrorRsp("This node is not a master L3 node."))
	}
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Subnet UUID is required."))
	}
	var subnet *Subnet
	if subnet, err = LoadSubnet(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
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
	for {
		if len(subnet.Ports) > 0 {
			return c.JSON(http.StatusForbidden, NewAppErrorRsp("Subnet has assigned ports."))
		}
		if err = subnet.PhaseStart(PhaseTypeDel); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		var done bool
		done, err = Save(nil, []RES{subnet})
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	go subnet.DeletingSubnetTask(vpc, vpcLocalRes)
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(subnet.Uuid))
}
