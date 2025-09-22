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
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"time"

	"github.com/labstack/echo"
	"github.com/spf13/cobra"
	v3 "go.etcd.io/etcd/client/v3"
	"go.etcd.io/etcd/client/v3/concurrency"
	"sigs.k8s.io/yaml"
)

type Agent struct {
	ConfigData *Config `json:"config"`
	SHAREREST
	PHASEINFO
}

type AgentFlags struct {
	uuid     string
	abnormal bool
	local    bool
}

type AgentGetRsp struct {
	Status         string   `json:"status"`
	AgentIp        string   `json:"agent_ip"`
	AgentPort      uint32   `json:"agent_port"`
	EtcdEndpoints  []string `json:"etcd_endpoints"`
	AgentHome      string   `json:"agent_home"`
	LogFile        string   `json:"log_file"`
	LogLevel       string   `json:"log_level"`
	VmMode         bool     `json:"vm_mode"`
	FmIp           string   `json:"fm_ip"`
	LanNic         string   `json:"lan_nic"`
	LanBridge      string   `json:"lan_bridge"`
	L3Mode         bool     `json:"l3_mode"`
	WanNic         string   `json:"wan_nic"`
	WanNicMac      string   `json:"wan_mac"`
	WanBridge      string   `json:"wan_bridge"`
	VlanRange      string   `json:"vlan_range"`
	VniRange       string   `json:"vni_range"`
	EipGwMac       string   `json:"eip_gw_mac"`
	WanGwMac       string   `json:"wan_gw_mac"`
	WanGwIp        string   `json:"wan_gw_ip"`
	DefaultEip     string   `json:"default_eip"`
	DnsmasqOptions []string `json:"dnsmasq_options"`
	DnsServers     []string `json:"dns_servers"`
	HaUuid         string   `json:"ha_uuid"`
	MasterL3       bool     `json:"master_l3"`
	SHAREREST
	PHASEINFO
}

type UnReachable struct {
	From string `json:"from"`
	To   string `json:"to"`
}

type DisConsistency struct {
	ResType string `json:"res_type"`
	Desc    string `json:"desc"`
}

var agentFlags AgentFlags

func LoadAgent(agentUuid string) *Agent {
	agent := &Agent{SHAREREST: SHAREREST{UUIDINFO: UUIDINFO{Uuid: agentUuid}, ETCDINFO: ETCDINFO{Type: "agent"}}}
	rest, _ := LoadRes(agent.GetKey(), agent)
	if rest != nil {
		agent, ok := rest.(*Agent)
		if ok {
			return agent
		}
	}
	return nil
}

func AddAgent(session0 *concurrency.Session, agent0 *Agent) (session *concurrency.Session, agent *Agent, err error) {
	if agent0 != nil {
		agent = agent0
	} else {
		agent = &Agent{
			SHAREREST:  SHAREREST{UUIDINFO: UUIDINFO{Uuid: G.config.Uuid}, ETCDINFO: ETCDINFO{Type: "agent"}},
			ConfigData: G.config,
		}
	}
	_ = agent.PhaseStart(PhaseTypeAdd)
	agent.PhaseStop(true)
	data, err := yaml.Marshal(agent)
	if err != nil {
		return
	}
	if session0 != nil {
		session = session0
	} else {
		session, err = concurrency.NewSession(G.v3client, concurrency.WithTTL(30))
		if err != nil {
			return
		}
	}
	var putResp *v3.PutResponse
	putResp, err = G.v3client.Put(context.Background(), agent.GetKey(), string(data), v3.WithLease(session.Lease()))
	if err != nil {
		return
	}
	agent.SetModrev(putResp.Header.Revision)
	return session, agent, nil
}

func AgentCmdParser(command *cobra.Command) {
	var err error
	var agentCmd = &cobra.Command{
		Use:   "agent",
		Short: "Agent management",
		Run: func(cmd *cobra.Command, args []string) {
			fmt.Println("please specify a subcommand for agent management. use -h for help.")
		},
	}
	var agentGetCmd = &cobra.Command{
		Use:   "get",
		Short: "Get a Agent",
		Run:   AgentGetHandle,
	}
	agentGetCmd.Flags().StringVarP(&agentFlags.uuid, "uuid", "U", "", "set the UUID of the Agent")
	if err = agentGetCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var agentListCmd = &cobra.Command{
		Use:   "list",
		Short: "List all Agents",
		Run:   AgentListHandle,
	}
	agentListCmd.Flags().BoolVar(&agentFlags.abnormal, "abnormal", false, "list abnormal Agents")
	agentListCmd.Flags().BoolVar(&agentFlags.local, "local", false, "list the agent of the local host")
	var agentLanTestCmd = &cobra.Command{
		Use:   "lantest",
		Short: "List unreachable Agents",
		Run:   AgentLanTestHandle,
	}
	var agentWanTestCmd = &cobra.Command{
		Use:   "wantest",
		Short: "List unreachable gateways",
		Run:   AgentWanTestHandle,
	}
	var agentResTestCmd = &cobra.Command{
		Use:   "restest",
		Short: "List disconsistency resources",
		Run:   AgentResTestHandle,
	}
	agentCmd.AddCommand(agentGetCmd, agentListCmd, agentLanTestCmd, agentWanTestCmd, agentResTestCmd)
	command.AddCommand(agentCmd)
}

func AgentGetHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/agents/%s", G.Host, G.Port, agentFlags.uuid)
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

func AgentListHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/agents", G.Host, G.Port)
	data := map[string]interface{}{}
	if agentFlags.abnormal {
		data["abnormal"] = true
	}
	if agentFlags.local {
		data["local"] = true
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

func AgentLanTestHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/agents/lantest", G.Host, G.Port)
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

func AgentWanTestHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/agents/wantest", G.Host, G.Port)
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

func AgentResTestHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/agents/restest", G.Host, G.Port)
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

func AgentAppSetup() {
	G.echoServer.GET("/v1/agents", AppAgentList)
	G.echoServer.GET("/v1/agents/lantest", AppAgentLanTest)
	G.echoServer.GET("/v1/agents/wantest", AppAgentWanTest)
	G.echoServer.GET("/v1/agents/restest", AppAgentResTest)
	G.echoServer.GET("/v1/agents/:uuid", AppAgentGet)
}

func AppAgentLanTest(c echo.Context) (err error) {
	G.logger.Debug("=========AppAgentLanTest==========")
	unreachables := []UnReachable{}
	rests := LoadRests("agent")
	lantest, err := LoadLanTest()
	if err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
	}
	curr := time.Now().Unix()
	for _, rest := range rests["agent"] {
		agent0, ok := rest.(*Agent)
		if !ok {
			continue
		}
		for _, rest := range rests["agent"] {
			agent1, ok := rest.(*Agent)
			if !ok {
				continue
			}
			if agent0.ConfigData.Uuid == agent1.ConfigData.Uuid {
				continue
			}
			var t0 int64
			if lantest.LanTestInfo[agent0.ConfigData.Uuid] != nil {
				t0 = lantest.LanTestInfo[agent0.ConfigData.Uuid][agent1.ConfigData.Uuid]
			}
			if t0 == 0 || curr-t0 > 30 {
				unreachables = append(unreachables, UnReachable{From: agent1.ConfigData.Uuid, To: agent0.ConfigData.Uuid})
			}
		}
	}
	jsonMap := make(map[string]interface{})
	jsonMap["status"] = "ok"
	jsonMap["unreachable_count"] = len(unreachables)
	jsonMap["unreachables"] = unreachables
	return c.JSON(http.StatusOK, jsonMap)
}

func AppAgentWanTest(c echo.Context) (err error) {
	G.logger.Debug("=========AppAgentWanTest==========")
	unreachables := []UnReachable{}
	rests := LoadRests("agent")
	wantest, err := LoadWanTest()
	if err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
	}
	curr := time.Now().Unix()
	for _, rest := range rests["agent"] {
		agent0, ok := rest.(*Agent)
		if !ok {
			continue
		}
		if wantest.WanTestInfo[agent0.ConfigData.Uuid] != nil {
			for gw, t0 := range wantest.WanTestInfo[agent0.ConfigData.Uuid] {
				if gw != agent0.ConfigData.WanGwMac && (agent0.ConfigData.EipGwMac == "" || gw != agent0.ConfigData.EipGwMac) {
					continue
				}
				if t0 == 0 || curr-t0 > 30 {
					unreachables = append(unreachables, UnReachable{From: agent0.ConfigData.Uuid, To: gw})
				}
			}
		}
	}
	jsonMap := make(map[string]interface{})
	jsonMap["status"] = "ok"
	jsonMap["unreachable_count"] = len(unreachables)
	jsonMap["unreachables"] = unreachables
	return c.JSON(http.StatusOK, jsonMap)
}

func AppAgentResTest(c echo.Context) (err error) {
	G.logger.Debug("=========AppAgentResTest==========")
	disconsistencies := ResTest()
	jsonMap := make(map[string]interface{})
	jsonMap["status"] = "ok"
	jsonMap["disconsistency_count"] = len(disconsistencies)
	jsonMap["disconsistencies"] = disconsistencies
	return c.JSON(http.StatusOK, jsonMap)
}

func AppAgentList(c echo.Context) (err error) {
	G.logger.Debug("=========AppAgentList==========")
	q := AppListReq{}
	if err = c.Bind(&q); err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
	}
	agentUuids := []string{}
	rests := LoadRests("agent")
	for _, rest := range rests["agent"] {
		agent, ok := rest.(*Agent)
		if !ok {
			continue
		}
		if q.Abnormal {
			if agent.NotReady() {
				if !q.Local || agent.ConfigData.Uuid == G.config.Uuid {
					agentUuids = append(agentUuids, agent.ConfigData.Uuid)
				}
			}
		} else {
			if !q.Local || agent.ConfigData.Uuid == G.config.Uuid {
				agentUuids = append(agentUuids, agent.ConfigData.Uuid)
			}
		}
	}
	jsonMap := make(map[string]interface{})
	jsonMap["status"] = "ok"
	jsonMap["agent_count"] = len(agentUuids)
	jsonMap["agents"] = agentUuids
	return c.JSON(http.StatusOK, jsonMap)
}

func AppAgentGet(c echo.Context) (err error) {
	G.logger.Debug("=========AppAgentGet==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Agent UUID is required."))
	}
	agent := LoadAgent(uuid)
	if agent == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Agent not found."))
	}
	rsp := AgentGetRsp{Status: "ok",
		AgentIp:        agent.ConfigData.AgentIp,
		AgentPort:      agent.ConfigData.AgentPort,
		EtcdEndpoints:  agent.ConfigData.EtcdEndpoints,
		AgentHome:      agent.ConfigData.AgentHome,
		LogFile:        agent.ConfigData.LogFile,
		LogLevel:       agent.ConfigData.LogLevel,
		VmMode:         agent.ConfigData.VmMode,
		FmIp:           agent.ConfigData.FmIp,
		LanNic:         agent.ConfigData.LanNic,
		LanBridge:      agent.ConfigData.LanBridge,
		L3Mode:         agent.ConfigData.L3Mode,
		WanNic:         agent.ConfigData.WanNic,
		WanNicMac:      agent.ConfigData.WanNicMac,
		WanBridge:      agent.ConfigData.WanBridge,
		VlanRange:      agent.ConfigData.VlanRange,
		VniRange:       agent.ConfigData.VniRange,
		EipGwMac:       agent.ConfigData.EipGwMac,
		WanGwMac:       agent.ConfigData.WanGwMac,
		WanGwIp:        agent.ConfigData.WanGwIp,
		DefaultEip:     agent.ConfigData.DefaultEip,
		DnsmasqOptions: agent.ConfigData.DnsmasqOptions,
		DnsServers:     agent.ConfigData.DnsServers,
		HaUuid:         agent.ConfigData.HaUuid,
		MasterL3:       agent.ConfigData.MasterL3,
		SHAREREST:      agent.SHAREREST,
		PHASEINFO:      agent.PHASEINFO}
	return c.JSON(http.StatusOK, &rsp)
}

func ResTest() (consistencies []*DisConsistency) {
	rests := LoadRests("")
	var fmPartitions *FMPartition
	if G.config.FmIp != "" {

		ids, err := ParseIDSFile(G.config.AgentHome + "/.gpu_ids")
		if err != nil {
			consistencies = append(consistencies, &DisConsistency{ResType: "gpu-ids", Desc: err.Error()})
			return consistencies
		}

		cmd := fmt.Sprintf("%s/bin/fm -o list -i %s", G.config.AgentHome, G.config.FmIp)
		output, err := ExecuteCmdOutput(G.config.CmdTimeout, cmd)
		if err != nil {
			consistencies = append(consistencies, &DisConsistency{ResType: "fm-list", Desc: err.Error()})
			return consistencies
		}
		type JsonPartitions struct {
			Partitions []Partition `json:"partitions"`
		}
		jsonPartitions := &JsonPartitions{}
		err = json.Unmarshal(output, jsonPartitions)
		if err != nil {

			consistencies = append(consistencies, &DisConsistency{ResType: "fm-list-unmarshal", Desc: err.Error()})
			return consistencies
		}

		id2gpu := make(map[string]*GPU)
		for _, rest := range rests["gpu"] {
			gpu, ok := rest.(*GPU)
			if !ok {
				continue
			}
			if !gpu.IsAdded {
				consistencies = append(consistencies, &DisConsistency{ResType: "gpu-add", Desc: fmt.Sprintf("GPU %s is not added", gpu.Uuid)})
			}

			if gpu.DevicePci == "" {
				consistencies = append(consistencies, &DisConsistency{ResType: "gpu-pci", Desc: fmt.Sprintf("GPU %s pci not found", gpu.Uuid)})
			} else if ids[gpu.DevicePci] == "" {
				consistencies = append(consistencies, &DisConsistency{ResType: "gpu-pci", Desc: fmt.Sprintf("GPU %s pci %s not found in .gpu_ids", gpu.Uuid, gpu.DevicePci)})
			} else {

				id2gpu[ids[gpu.DevicePci]] = gpu
			}
		}

		if len(id2gpu) != len(rests["gpu"]) {

			consistencies = append(consistencies, &DisConsistency{ResType: "gpu-ids", Desc: fmt.Sprintf("number of gpu ids %d is not equal to number of gpus %d", len(id2gpu), len(rests["gpu"]))})
		}

		fmPartitions = &FMPartition{Partitions: make(map[string]*Partition)}
		for _, partition := range jsonPartitions.Partitions {
			partitionId := partition.Id
			fmPartitions.Partitions[partitionId] = &partition
			for _, gpuId := range partition.GpuIds {
				gpu, ok := id2gpu[gpuId]
				if !ok {

					consistencies = append(consistencies, &DisConsistency{ResType: "gpu-partition", Desc: fmt.Sprintf("GPU id %s not found in .gpu_ids", gpuId)})
					continue
				}
				if !Contains(partition.Gpus, gpu) {
					partition.Gpus = append(partition.Gpus, gpu)
					if partition.IsActive {
						if gpu.PartitionId == "" {
							consistencies = append(consistencies, &DisConsistency{ResType: "gpu-partition", Desc: fmt.Sprintf("partition %s is active, but GPU %s partition is empty", partitionId, gpu.Uuid)})
						} else if gpu.VmUuid == "" {
							consistencies = append(consistencies, &DisConsistency{ResType: "gpu-partition", Desc: fmt.Sprintf("partition %s is active, but GPU %s vm uuid is empty", partitionId, gpu.Uuid)})
						}
					}
				}
			}
		}
		for _, gpu := range id2gpu {
			if gpu.PartitionId != "" {

				if fmPartitions.Partitions[gpu.PartitionId] == nil {
					consistencies = append(consistencies, &DisConsistency{ResType: "gpu-partition", Desc: fmt.Sprintf("GPU %s partition %s not found", gpu.Uuid, gpu.PartitionId)})
				} else {

					if !fmPartitions.Partitions[gpu.PartitionId].IsActive {
						consistencies = append(consistencies, &DisConsistency{ResType: "gpu-partition", Desc: fmt.Sprintf("GPU %s partition %s is inactive", gpu.Uuid, gpu.PartitionId)})
					}
				}
			} else if gpu.VmUuid != "" {

				consistencies = append(consistencies, &DisConsistency{ResType: "gpu-partition", Desc: fmt.Sprintf("GPU %s partition is empty, but vm uuid %s is not empty", gpu.Uuid, gpu.VmUuid)})
			}
		}
	}

	for _, rest := range rests["pool"] {
		pool, ok := rest.(*Pool)
		if !ok {
			continue
		}
		if !pool.IsAdded {
			consistencies = append(consistencies, &DisConsistency{ResType: "pool-add", Desc: fmt.Sprintf("pool %s is not added", pool.Uuid)})
			continue
		}
		dir := pool.ParaMap["DIR"]
		for _, volRest := range rests["vol"] {
			vol, ok := volRest.(*Vol)
			if !ok {
				continue
			}
			if vol.PoolUuid != pool.Uuid {
				continue
			}
			if !vol.IsAdded {
				consistencies = append(consistencies, &DisConsistency{ResType: "vol-add", Desc: fmt.Sprintf("vol %s is not added", vol.Uuid)})
				continue
			}

			if !Contains(pool.Vols, vol.Uuid) {
				consistencies = append(consistencies, &DisConsistency{ResType: "vol-pool", Desc: fmt.Sprintf("vol %s not found in pool %s", vol.Uuid, pool.Uuid)})
			}

			ok, _ = FileExists(dir + "/volumes/" + vol.Uuid + ".qcow2")
			if !ok {
				consistencies = append(consistencies, &DisConsistency{ResType: "vol-file", Desc: fmt.Sprintf("vol %s file not found", vol.Uuid)})
			}
			if vol.Root {
				if vol.ImgUuid == "" || vol.ImgName == "" {
					consistencies = append(consistencies, &DisConsistency{ResType: "vol-img", Desc: fmt.Sprintf("vol %s root but img uuid or name is empty", vol.Uuid)})
				} else {
					ok, _ = FileExists(dir + "/backing/" + vol.ImgName + ".qcow2")
					if !ok {
						consistencies = append(consistencies, &DisConsistency{ResType: "vol-img", Desc: fmt.Sprintf("vol %s img file not found", vol.Uuid)})
					}
				}
			}
			for _, snapUuid := range vol.Snaps {
				var snnap *Snap
				for _, snapRest := range rests["snap"] {
					s, ok := snapRest.(*Snap)
					if !ok {
						continue
					}
					if !s.IsAdded {
						consistencies = append(consistencies, &DisConsistency{ResType: "snap-add", Desc: fmt.Sprintf("snap %s is not added", s.Uuid)})
						continue
					}
					if s.Uuid == snapUuid {
						snnap = s
						break
					}
				}
				if snnap == nil {
					consistencies = append(consistencies, &DisConsistency{ResType: "snap-vol", Desc: fmt.Sprintf("snap %s vol %s not found", snapUuid, vol.Uuid)})
				}

				ok, _ = FileExists(dir + "/volumes/" + snapUuid + ".qcow2")
				if !ok {
					consistencies = append(consistencies, &DisConsistency{ResType: "snap-file", Desc: fmt.Sprintf("snap %s file not found", snapUuid)})
				}
			}
		}

		for _, restVM := range rests["vm"] {
			vm, ok := restVM.(*VM)
			if !ok {
				continue
			}
			if !vm.IsAdded {
				consistencies = append(consistencies, &DisConsistency{ResType: "vm-add", Desc: fmt.Sprintf("vm %s is not added", vm.Uuid)})
				continue
			}
			if vm.PoolUuid != pool.Uuid {
				continue
			}
			domain, err := VirtDomainGet(vm.Uuid)
			if err != nil {
				consistencies = append(consistencies, &DisConsistency{ResType: "vm-domain", Desc: fmt.Sprintf("vm %s domain not found: %s", vm.Uuid, err.Error())})
				continue
			}
			if domain == nil {
				consistencies = append(consistencies, &DisConsistency{ResType: "vm-domain", Desc: fmt.Sprintf("vm %s domain not found", vm.Uuid)})
				continue
			}
			disks, err := VirtDomainDisks(domain)
			if err != nil {
				consistencies = append(consistencies, &DisConsistency{ResType: "vm-disks", Desc: fmt.Sprintf("vm %s disks not found: %s", vm.Uuid, err.Error())})
				continue
			}
			for _, volUuid := range vm.Vols {
				var vol *Vol
				for _, volRest := range rests["vol"] {
					v, ok := volRest.(*Vol)
					if !ok {
						continue
					}
					if !v.IsAdded {
						consistencies = append(consistencies, &DisConsistency{ResType: "vol-add", Desc: fmt.Sprintf("vol %s is not added", v.Uuid)})
						continue
					}
					if v.Uuid == volUuid {
						vol = v
						break
					}
				}
				if vol == nil {
					consistencies = append(consistencies, &DisConsistency{ResType: "vol-vm", Desc: fmt.Sprintf("vol %s vm %s not found", volUuid, vm.Uuid)})
					continue
				}
				if vol.VmUuid == "" {
					consistencies = append(consistencies, &DisConsistency{ResType: "vol-vm", Desc: fmt.Sprintf("vol %s vm is empty", volUuid)})
					continue
				}
				if vol.VmUuid != vm.Uuid {
					consistencies = append(consistencies, &DisConsistency{ResType: "vol-vm", Desc: fmt.Sprintf("vol %s vm %s not match", volUuid, vm.Uuid)})
					continue
				}
				if vol.PoolUuid != pool.Uuid {
					consistencies = append(consistencies, &DisConsistency{ResType: "vol-pool", Desc: fmt.Sprintf("vol %s pool %s not match", volUuid, pool.Uuid)})
					continue
				}
				source := fmt.Sprintf("%s/volumes/%s.qcow2", pool.ParaMap["DIR"], vol.Uuid)
				var found bool
				for _, disk := range disks {
					if disk["source"] == source {
						found = true
						break
					}
				}
				if !found {
					consistencies = append(consistencies, &DisConsistency{ResType: "vol-vm", Desc: fmt.Sprintf("vol %s vm %s not found in domain", volUuid, vm.Uuid)})
				}
			}
		}
	}
	domains, err := VirtDomains()
	if err != nil {
		consistencies = append(consistencies, &DisConsistency{ResType: "vm-domain", Desc: fmt.Sprintf("domains not found: %s", err.Error())})
	}
	for _, domain := range domains {
		var found bool
		var name string
		for _, rest := range rests["vm"] {
			vm, ok := rest.(*VM)
			if !ok {
				continue
			}

			name, err = domain.GetName()
			if err != nil {
				consistencies = append(consistencies, &DisConsistency{ResType: "vm-domain", Desc: fmt.Sprintf("vm %s domain not found: %s", vm.Uuid, err.Error())})
				continue
			}
			if name == vm.Uuid {
				found = true
				break
			}
		}
		if !found && name != "" && name != "nvsvm" {
			consistencies = append(consistencies, &DisConsistency{ResType: "vm-domain", Desc: fmt.Sprintf("domain %s not found in vm", name)})
		}
	}
	return consistencies
}
