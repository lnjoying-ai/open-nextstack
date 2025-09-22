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
	"math/rand/v2"
	"net"
	"net/http"
	"strings"

	"github.com/labstack/echo"
	"github.com/spf13/cobra"
	"go.uber.org/zap"
)

type Nfs struct {
	Name     string              `json:"name"`
	Size     uint32              `json:"size"`
	Ports    []string            `json:"ports"`
	LocalRes map[string][]string `json:"local_res"`
	AGENTREST
	PHASEINFO
}

type NfsFlags struct {
	name     string
	size     uint32
	uuid     string
	port     []string
	abnormal bool
}

type NfsAddReq struct {
	Name  string   `json:"name"`
	Size  uint32   `json:"size"`
	Ports []string `json:"ports"`
}

type NfsGetRsp struct {
	Status string   `json:"status"`
	Name   string   `json:"name"`
	Size   uint32   `json:"size"`
	Ports  []string `json:"ports"`
	AGENTREST
	PHASEINFO
}

var nfsFlags NfsFlags

func LoadNfs(nfs_uuid string) (nfs *Nfs, err error) {
	nfs = &Nfs{AGENTREST: AGENTREST{UUIDINFO: UUIDINFO{Uuid: nfs_uuid}, AgentUuid: G.config.Uuid, ETCDINFO: ETCDINFO{Type: "nfs"}}}
	path := nfs.GetKey()
	var ret RES
	if ret, err = LoadRes(path, nfs); err != nil {
		return nil, err
	}
	if ret != nil {
		nfs, ok := ret.(*Nfs)
		if ok {
			return nfs, nil
		}
	}
	return nil, nil
}

func LoadNfs2(nfs_uuid string, agentUuid string) (nfs *Nfs, err error) {
	nfs = &Nfs{AGENTREST: AGENTREST{UUIDINFO: UUIDINFO{Uuid: nfs_uuid}, AgentUuid: agentUuid, ETCDINFO: ETCDINFO{Type: "nfs"}}}
	path := nfs.GetKey()
	var ret RES
	if ret, err = LoadRes(path, nfs); err != nil {
		return nil, err
	}
	if ret != nil {
		nfs, ok := ret.(*Nfs)
		if ok {
			return nfs, nil
		}
	}
	return nil, nil
}

func (nfs *Nfs) Add(ports []*Port, nfsLocalRes []string) (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()

	if _, ok := G.deployedRest[nfs.Uuid]; ok {
		G.logger.Error("nfs already deployed", zap.String("nfs", nfs.Uuid))
		return nil
	}
	if err = CreateDockerVolume(nfs.Uuid, nfs.Size); err != nil {
		G.logger.Error("cannot create docker volume", zap.Error(err))
		return err
	}
	containerParams := fmt.Sprintf("--privileged -v %s:%s -e SHARED_DIRECTORY=%s", nfs.Uuid, nfs.Name, nfs.Name)
	if err = StartContainer(nfs.Uuid, containerParams, "itsthenetwork/nfs-server-alpine:latest", ""); err != nil {
		G.logger.Error("cannot start container", zap.Error(err))
		return err
	}
	if err = EnableContainerNetns(nfs.Uuid); err != nil {
		G.logger.Error("cannot enable container netns", zap.Error(err))
		return err
	}
	for _, port := range ports {
		ip := net.ParseIP(port.Mask).To4()
		mask := net.IPMask(ip)
		prefixlen, _ := mask.Size()
		prefix := fmt.Sprintf("%d", prefixlen)
		if err = ConnectContainerNetns(nfs.Uuid, port.Nic, port.Mac, port.Ip, prefix, port.Vlanid, port.Ofport, port.Gateway); err != nil {
			G.logger.Error("cannot connect container netns", zap.Error(err))
			return err
		}
		if err = OvsVmAdd(port.Nic, port.Vlanid, port.Mac, port.Ip, port.Ofport, port.Speed, port.Cidr); err != nil {
			G.logger.Error("cannot add ovs for the nfs", zap.Error(err))
			return err
		}
	}
	if err = DisableContainerNetns(nfs.Uuid); err != nil {
		G.logger.Error("cannot disable container netns", zap.Error(err))
		return err
	}
	G.deployedRest[nfs.Uuid] = struct{}{}
	return nil
}

func (nfs *Nfs) Del(ports []*Port) (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()

	if _, ok := G.deployedRest[nfs.Uuid]; !ok {
		return nil
	}
	if err = DisableContainerNetns(nfs.Uuid); err != nil {
		G.logger.Error("cannot disable container netns", zap.Error(err))
		return err
	}
	if err = DeleteContainer(nfs.Uuid); err != nil {
		G.logger.Error("cannot delete container", zap.Error(err))
		return err
	}
	for _, port := range ports {
		if port.Nic == "" || port.Ofport == "" || port.Vlanid == "" {
			continue
		}
		if err = OvsVmDel(port.Nic, port.Vlanid, port.Mac, port.Ip, port.Ofport, port.Speed, port.Cidr); err != nil {
			G.logger.Error("cannot delete ovs vm", zap.Error(err))
			return err
		}
		if err = OvsNicDel(G.config.LanBridge, port.Nic); err != nil {
			G.logger.Error("cannot delete ovs nic", zap.Error(err))
			return err
		}
		if osNicExists(port.Nic, "") {
			if err = osNicDel(port.Nic, ""); err != nil {
				G.logger.Error("cannot delete nic", zap.Error(err))
				return err
			}
		}
	}
	if err = DeleteDockerVolume(nfs.Uuid); err != nil {
		G.logger.Error("cannot delete docker volume", zap.Error(err))
		return err
	}
	delete(G.deployedRest, nfs.Uuid)
	return nil
}

func (nfs *Nfs) Restore() (err error) {
	var nfsLocalRes []string

	if nfsLocalRes, err = nfs.AssignLocalRes(true); err != nil {
		return nil
	}
	var ports []*Port
	for i, portUuid := range nfs.Ports {
		var port *Port
		if port, err = LoadPort(portUuid); err != nil {
			G.logger.Error("cannot load port", zap.Error(err))
			return err
		}
		if port == nil {
			return fmt.Errorf("port not found")
		}
		var vpcLocalRes []string
		var subnet *Subnet
		var vpc *VPC
		if subnet, err = LoadSubnet(port.SubnetUuid); err != nil {
			G.logger.Error("Cannot load subnet.", zap.Error(err))
			return err
		}
		if vpc, err = LoadVPC(subnet.VpcUuid); err != nil {
			G.logger.Error("Cannot load VPC.", zap.Error(err))
			return err
		}
		if vpcLocalRes, err = vpc.AssignLocalRes(true); err != nil {
			G.logger.Error("cannot assign local res", zap.Error(err))
			return err
		}
		port.Nic = nfsLocalRes[i*2]
		port.Ofport = nfsLocalRes[i*2+1]
		port.Vlanid = vpcLocalRes[0]
		ports = append(ports, port)
	}
	if err = nfs.Add(ports, nfsLocalRes); err != nil {
		G.logger.Error("cannot add nfs", zap.Error(err))
		return err
	}
	return nil
}

func (nfs *Nfs) AddingNfsTask(ports []*Port, hostSG *HOSTSGT) {
	var err error
	success := false
	var nfsLocalRes []string
	agent := LoadAgent(G.config.Uuid)
	if agent == nil {
		G.logger.Error("Cannot load agent.", zap.Error(err))
		goto UPDATE
	}
	if nfsLocalRes, err = nfs.AssignLocalRes(false); err != nil {
		G.logger.Error("cannot assign local res", zap.Error(err))
		goto UPDATE
	}
	for i, port := range ports {
		var vpcLocalRes []string
		var subnet *Subnet
		var vpc *VPC
		if subnet, err = LoadSubnet(port.SubnetUuid); err != nil {
			G.logger.Error("Cannot load subnet.", zap.Error(err))
			goto UPDATE
		}
		if vpc, err = LoadVPC(subnet.VpcUuid); err != nil {
			G.logger.Error("Cannot load VPC.", zap.Error(err))
			goto UPDATE
		}
		if vpcLocalRes, err = vpc.AssignLocalRes(false); err != nil {
			G.logger.Error("cannot assign local res", zap.Error(err))
			goto UPDATE
		}
		if _, err = subnet.AssignLocalRes(false); err != nil {
			G.logger.Error("cannot assign local res", zap.Error(err))
			goto UPDATE
		}
		if _, err = port.AssignLocalRes(false); err != nil {
			G.logger.Error("cannot assign local res", zap.Error(err))
			goto UPDATE
		}
		if err = vpc.Restore(); err != nil {
			G.logger.Error("cannot restore vpc", zap.Error(err))
			goto UPDATE
		}
		port.Nic = nfsLocalRes[i*2]
		port.Ofport = nfsLocalRes[i*2+1]
		port.Vlanid = vpcLocalRes[0]
	}
	if err = nfs.Add(ports, nfsLocalRes); err != nil {
		G.logger.Error("cannot add nfs", zap.Error(err))
		goto UPDATE
	}
	success = true
UPDATE:
	for {
		reses := []RES{nfs, hostSG}
		var done bool
		nfs.PhaseStop(success)
		for i, port := range ports {
			if len(port.Sgs) > 0 {
				hostSG.Ports[port.Uuid] = nfsLocalRes[i*2+1]
			}
			port.PhaseStop(success)
			reses = append(reses, port)
		}
		done, err = Save(nil, reses)
		if err != nil {
			G.logger.Error("cannot save reses", zap.Error(err))
			return
		}
		if done {
			break
		}
	}
	if success {
		NoticeSG()
	}
}

func (nfs *Nfs) DeletingNfsTask(ports []*Port, hostSG *HOSTSGT) {
	var err error
	success := false
	var agent *Agent
	if err = nfs.Del(ports); err != nil {
		G.logger.Error("cannot delete nfs", zap.Error(err))
		goto UPDATE
	}
	_ = nfs.UnAssignLocalRes()
	agent = LoadAgent(G.config.Uuid)
	if agent == nil {
		G.logger.Error("Cannot load agent.", zap.Error(err))
		goto UPDATE
	}
	for _, port := range ports {
		var subnet *Subnet
		var vpc *VPC
		var vpcLocalRes []string
		if subnet, err = LoadSubnet(port.SubnetUuid); err != nil {
			continue
		}
		if vpc, err = LoadVPC(subnet.VpcUuid); err != nil {
			continue
		}
		if vpcLocalRes, err = vpc.AssignLocalRes(true); err != nil {
			continue
		}
		if G.config.L3Mode && !G.config.MasterL3 {
			if err = port.UnBind(subnet, vpc, agent, vpcLocalRes); err != nil {
				continue
			}
			if err = port.Del(subnet, vpc, agent, vpcLocalRes); err != nil {
				continue
			}
			if err = port.UnAssignLocalRes(G.config.Uuid); err != nil {
				continue
			}
			if !subnet.HasLocalPort() {
				if err = subnet.Del(vpc, vpcLocalRes); err != nil {
					continue
				}
				if err = subnet.UnAssignLocalRes(G.config.Uuid); err != nil {
					continue
				}
			}
			if !vpc.HasLocalSubnet() {
				if err = vpc.Del(); err != nil {
					continue
				}
				if err = vpc.UnAssignLocalRes(G.config.Uuid); err != nil {
					continue
				}
			}
		}

	}
	success = true
UPDATE:
	for {
		todel := []RES{}
		reses := []RES{}
		if success {
			todel = append(todel, nfs)
		} else {
			nfs.PhaseStop(success)
			reses = append(reses, nfs)
		}
		if hostSG != nil {
			reses = append(reses, hostSG)
		}
		for _, port := range ports {
			if hostSG != nil {
				delete(hostSG.Ports, port.Uuid)
			}
			port.ByUuid = ""
			port.PhaseStop(success)
			reses = append(reses, port)
		}
		var done bool
		if done, err = Save(todel, reses); err != nil {
			G.logger.Error("cannot save reses", zap.Error(err))
			return
		}
		if done {
			NoticeSG()
			return
		}
	}
}

func (nfs *Nfs) VerifyUuid() error {
	return AssignUuid("nfs", nfs)
}

func (nfs *Nfs) AssignLocalRes(retrieveOnly bool) ([]string, error) {

	if _, ok := nfs.LocalRes[nfs.AgentUuid]; ok {
		return nfs.LocalRes[nfs.AgentUuid], nil
	}

	if retrieveOnly {
		return nil, fmt.Errorf("no local res")
	}
	nics, _ := LoadNics()
	ofports, _ := LoadOfports()
	for {
		nfs.LocalRes[nfs.AgentUuid] = []string{}
		for _, portUuid := range nfs.Ports {
			var nic string
			var ofport string
			for {
				nic = GenNic()
				if nics.NicsInfo[nfs.AgentUuid] == nil {
					nics.NicsInfo[nfs.AgentUuid] = make(map[string]string)
				}
				if _, ok := nics.NicsInfo[nfs.AgentUuid][nic]; !ok {
					nics.NicsInfo[nfs.AgentUuid][nic] = portUuid
					break
				}
			}
			for {
				o := rand.Uint32N(65299-10) + 10
				ofport = fmt.Sprintf("%d", o)
				if ofports.OfportsInfo[nfs.AgentUuid] == nil {
					ofports.OfportsInfo[nfs.AgentUuid] = make(map[string]string)
				}
				if _, ok := ofports.OfportsInfo[nfs.AgentUuid][ofport]; !ok {
					ofports.OfportsInfo[nfs.AgentUuid][ofport] = portUuid
					break
				}
			}
			nfs.LocalRes[nfs.AgentUuid] = append(nfs.LocalRes[nfs.AgentUuid], nic, ofport)
		}
		var done bool
		var err error
		done, err = Save(nil, []RES{nfs, nics, ofports})
		if err != nil {
			return nil, err
		}
		if done {
			break
		}
	}
	return nfs.LocalRes[nfs.AgentUuid], nil
}

func (nfs *Nfs) UnAssignLocalRes() (err error) {
	if _, ok := nfs.LocalRes[nfs.AgentUuid]; !ok {
		return nil
	}
	nics, _ := LoadNics()
	ofports, _ := LoadOfports()
	for {
		for i := 0; i < len(nfs.LocalRes[nfs.AgentUuid]); i += 2 {
			nic := nfs.LocalRes[nfs.AgentUuid][i]
			ofport := nfs.LocalRes[nfs.AgentUuid][i+1]
			delete(nics.NicsInfo[nfs.AgentUuid], nic)
			delete(ofports.OfportsInfo[nfs.AgentUuid], ofport)
		}
		delete(nfs.LocalRes, nfs.AgentUuid)
		var done bool
		var err error
		done, err = Save(nil, []RES{nfs, nics, ofports})
		if err != nil {
			return err
		}
		if done {
			break
		}
	}
	return nil
}

func NfsCmdParser(command *cobra.Command) {
	var err error
	var nfsCmd = &cobra.Command{
		Use:   "nfs",
		Short: "NFS management",
		Run: func(cmd *cobra.Command, args []string) {
			fmt.Println("please specify a subcommand for nfs management. use -h for help.")
		},
	}
	var nfsAddCmd = &cobra.Command{
		Use:   "add",
		Short: "Add a NFS",
		Run:   NfsAddHandle,
	}
	nfsAddCmd.Flags().StringVar(&nfsFlags.name, "name", "", "set the name of the NFS")
	if err = nfsAddCmd.MarkFlagRequired("name"); err != nil {
		panic(err)
	}
	nfsAddCmd.Flags().Uint32Var(&nfsFlags.size, "size", 0, "set the size of the NFS")
	if err = nfsAddCmd.MarkFlagRequired("size"); err != nil {
		panic(err)
	}
	nfsAddCmd.Flags().StringSliceVar(&nfsFlags.port, "port", nil, "set the ports of the NFS")
	if err = nfsAddCmd.MarkFlagRequired("port"); err != nil {
		panic(err)
	}
	var nfsGetCmd = &cobra.Command{
		Use:   "get",
		Short: "Get a NFS",
		Run:   NfsGetHandle,
	}
	nfsGetCmd.Flags().StringVarP(&nfsFlags.uuid, "uuid", "U", "", "set the UUID of the NFS")
	if err = nfsGetCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var nfsDelCmd = &cobra.Command{
		Use:   "del",
		Short: "Delete a NFS",
		Run:   NfsDelHandle,
	}
	nfsDelCmd.Flags().StringVarP(&nfsFlags.uuid, "uuid", "U", "", "set the UUID of the NFS")
	if err = nfsDelCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var nfsListCmd = &cobra.Command{
		Use:   "list",
		Short: "List all NFSes",
		Run:   NfsListHandle,
	}
	nfsListCmd.Flags().BoolVar(&nfsFlags.abnormal, "abnormal", false, "list abnormal nfses")
	nfsListCmd.Flags().StringVarP(&nfsFlags.uuid, "uuid", "U", "", "set the UUID of the VPC")
	if err = nfsGetCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	nfsCmd.AddCommand(nfsAddCmd, nfsGetCmd, nfsDelCmd, nfsListCmd)
	command.AddCommand(nfsCmd)
}

func NfsAddHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/nfses", G.Host, G.Port)
	data := map[string]interface{}{"name": nfsFlags.name, "size": nfsFlags.size, "ports": nfsFlags.port}
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

func NfsGetHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/nfses/%s", G.Host, G.Port, nfsFlags.uuid)
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

func NfsDelHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/nfses/%s", G.Host, G.Port, nfsFlags.uuid)
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

func NfsListHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/nfses", G.Host, G.Port)
	data := map[string]interface{}{}
	if nfsFlags.abnormal {
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

func NfsAppSetup() {
	G.echoServer.POST("/v1/nfses", AppNfsAdd)
	G.echoServer.GET("/v1/nfses", AppNfsList)
	G.echoServer.GET("/v1/nfses/:uuid", AppNfsGet)
	G.echoServer.DELETE("/v1/nfses/:uuid", AppNfsDel)
}

func AppNfsAdd(c echo.Context) (err error) {
	G.logger.Debug("=========AppNfsAdd==========")
	q := NfsAddReq{}
	if err = c.Bind(&q); err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Cannot parse the request."))
	}
	if !strings.HasPrefix(q.Name, "/") {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("NFS name must start with ‘/’."))
	}
	var ports []*Port
	for _, portUuid := range q.Ports {
		var port *Port
		if port, err = LoadPort(portUuid); err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if port == nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Port not found."))
		}
		ports = append(ports, port)
	}
	if len(ports) == 0 {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("No port specified."))
	}
	hostsg, _ := LoadHostSG(G.config.Uuid)
	var nfs *Nfs
	for {
		nfs = &Nfs{Ports: q.Ports, Name: q.Name, Size: q.Size, LocalRes: make(map[string][]string), AGENTREST: AGENTREST{AgentUuid: G.config.Uuid, ETCDINFO: ETCDINFO{Type: "nfs"}}}
		_ = nfs.PhaseStart(PhaseTypeAdd)
		if err = nfs.VerifyUuid(); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		reses := []RES{nfs, hostsg}
		for _, port := range ports {
			if port.NotReady() {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Port is not ready."))
			}
			if port.AgentUuid != G.config.Uuid {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Port is not assigned to this host."))
			}
			if port.ByUuid != "" {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Port is in use."))
			}
			if err = port.PhaseStart(PhaseTypeAttach); err != nil {
				return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
			}
			port.ByUuid = nfs.Uuid
			reses = append(reses, port)
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
	go nfs.AddingNfsTask(ports, hostsg)
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(nfs.Uuid))
}

func AppNfsList(c echo.Context) (err error) {
	G.logger.Debug("=========AppNfsList==========")
	q := AppListReq{}
	if err = c.Bind(&q); err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
	}
	nfsUuids := []string{}
	rests := LoadRests("nfs")
	for _, rest := range rests["nfs"] {
		nfs, ok := rest.(*Nfs)
		if !ok {
			continue
		}
		if nfs.AgentUuid != G.config.Uuid {
			continue
		}
		if q.Abnormal {
			if nfs.NotReady() {
				nfsUuids = append(nfsUuids, nfs.Uuid)
			}
		} else {
			nfsUuids = append(nfsUuids, nfs.Uuid)
		}
	}
	jsonMap := make(map[string]interface{})
	jsonMap["status"] = "ok"
	jsonMap["nfs_count"] = len(nfsUuids)
	jsonMap["nfses"] = nfsUuids
	return c.JSON(http.StatusOK, jsonMap)
}

func AppNfsGet(c echo.Context) (err error) {
	G.logger.Debug("=========AppNfsGet==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("NFS UUID is required."))
	}
	var nfs *Nfs
	if nfs, err = LoadNfs(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if nfs == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("NFS not found."))
	}
	rsp := NfsGetRsp{
		Status:    "ok",
		Name:      nfs.Name,
		Size:      nfs.Size,
		Ports:     nfs.Ports,
		AGENTREST: nfs.AGENTREST,
		PHASEINFO: nfs.PHASEINFO}
	return c.JSON(http.StatusOK, &rsp)
}

func AppNfsDel(c echo.Context) (err error) {
	G.logger.Debug("=========AppNfsDel==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("NFS UUID is required."))
	}
	var nfs *Nfs
	if nfs, err = LoadNfs(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if nfs == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("NFS not found."))
	}
	var nfsLocalRes []string
	nfsLocalRes, _ = nfs.AssignLocalRes(true)
	var ports []*Port
	for i, portUuid := range nfs.Ports {
		var port *Port
		var subnet *Subnet
		var vpc *VPC
		if port, err = LoadPort(portUuid); err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if port == nil {
			return c.JSON(http.StatusNotFound, NewAppErrorRsp("Port not found."))
		}
		if subnet, err = LoadSubnet(port.SubnetUuid); err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if subnet == nil {
			return c.JSON(http.StatusNotFound, NewAppErrorRsp("Subnet not found."))
		}
		if vpc, err = LoadVPC(subnet.VpcUuid); err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if vpc == nil {
			return c.JSON(http.StatusNotFound, NewAppErrorRsp("VPC not ready."))
		}
		var vpcLocalRes []string
		vpcLocalRes, _ = vpc.AssignLocalRes(true)
		if nfsLocalRes != nil {
			port.Nic = nfsLocalRes[i*2]
			port.Ofport = nfsLocalRes[i*2+1]
		}
		if vpcLocalRes != nil {
			port.Vlanid = vpcLocalRes[0]
		}
		ports = append(ports, port)
	}
	for {
		reses := []RES{nfs}
		if err = nfs.PhaseStart(PhaseTypeDel); err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		for _, port := range ports {
			if err = port.PhaseStart(PhaseTypeDetach); err != nil {
				return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
			}
			reses = append(reses, port)
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
	hostsg, _ := LoadHostSG(nfs.AgentUuid)
	go nfs.DeletingNfsTask(ports, hostsg)
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(nfs.Uuid))
}
