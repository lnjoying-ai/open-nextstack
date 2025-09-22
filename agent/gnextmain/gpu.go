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
	"bufio"
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"os/exec"
	"regexp"
	"strings"

	"github.com/labstack/echo"
	"github.com/spf13/cobra"
	"go.uber.org/zap"
)

type GPU struct {
	DevicePci   string   `json:"device_pci"`
	DeviceName  string   `json:"device_name"`
	VmUuid      string   `json:"vm"`
	IBPci       string   `json:"ib_pci"`
	BroPci      string   `json:"bro_pci"`
	OtherPcis   []string `json:"other"`
	DeviceId    string   `json:"device_id"`
	PartitionId string   `json:"partition_id"`
	AGENTREST
	PHASEINFO
}

type GpuFlags struct {
	uuid      string
	abnormal  bool
	vmUuid    string
	partition string
	num       uint
	ib        bool
}

type GpuAttachReq struct {
	VmUuid string `json:"vm"`
}

type GpuGetRsp struct {
	Status      string   `json:"status"`
	DevicePci   string   `json:"device_pci"`
	DeviceName  string   `json:"device_name"`
	VmUuid      string   `json:"vm"`
	IBPci       string   `json:"ib_pci"`
	BroPci      string   `json:"bro_pci"`
	OtherPcis   []string `json:"other"`
	DeviceId    string   `json:"device_id"`
	PartitionId string   `json:"partition_id"`
	AgentUuid   string   `json:"agent"`
	AGENTREST
	PHASEINFO
}

type GpuActiveRsp struct {
	Status      string   `json:"status"`
	PartitionId string   `json:"partition_id"`
	GpuUUids    []string `json:"gpu_uuids"`
}

type GpuDeactiveRsp struct {
	Status      string   `json:"status"`
	PartitionId string   `json:"partition_id"`
	GpuUUids    []string `json:"gpu_uuids"`
}

type GpuActiveReq struct {
	Num         uint   `json:"num"`
	PartitionId string `json:"partition_id"`
	IB          bool   `json:"ib"`
}

type GpuDeactiveReq struct {
	PartitionId string `json:"partition_id"`
}

type Partition struct {
	Id       string   `json:"id"`
	IsActive bool     `json:"is_active"`
	GpuIds   []string `json:"gpus"`
	Gpus     []*GPU   `json:"-"`
}

type FMPartition struct {
	Partitions map[string]*Partition `json:"partitions"`
}

var gpuFlags GpuFlags

func LoadGPU(gpu_uuid string) (gpu *GPU, err error) {
	gpu = &GPU{AGENTREST: AGENTREST{UUIDINFO: UUIDINFO{Uuid: gpu_uuid}, AgentUuid: G.config.Uuid, ETCDINFO: ETCDINFO{Type: "gpu"}}}
	path := gpu.GetKey()
	var ret RES
	if ret, err = LoadRes(path, gpu); err != nil {
		return nil, err
	}
	if ret != nil {
		gpu, ok := ret.(*GPU)
		if ok {
			return gpu, nil
		}
	}
	return nil, nil
}

func LoadGPU2(gpu_uuid string, agentUuid string) (gpu *GPU, err error) {
	gpu = &GPU{AGENTREST: AGENTREST{UUIDINFO: UUIDINFO{Uuid: gpu_uuid}, AgentUuid: agentUuid, ETCDINFO: ETCDINFO{Type: "gpu"}}}
	path := gpu.GetKey()
	var ret RES
	if ret, err = LoadRes(path, gpu); err != nil {
		return nil, err
	}
	if ret != nil {
		gpu, ok := ret.(*GPU)
		if ok {
			return gpu, nil
		}
	}
	return nil, nil
}

func LoadGPU3(rests RESTS, deviceId string) (gpu *GPU, err error) {
	for _, rest := range rests["gpu"] {
		gpu, ok := rest.(*GPU)
		if !ok {
			continue
		}
		if gpu.AgentUuid != G.config.Uuid {
			continue
		}
		if gpu.DeviceId != deviceId {
			continue
		}
		if gpu.VmUuid == "" && !gpu.NotReady() {
			return gpu, nil
		}
	}
	return nil, nil
}

func (o *GPU) VerifyUuid() error {
	return AssignUuid("gpu", o)
}

func (gpu *GPU) Attach(vm *VM) (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()
	return AttachGPU(vm, gpu)
}

func (gpu *GPU) Detach(vm *VM) (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()
	return DetachGPU(vm, gpu)
}

func (gpu *GPU) AttachingGpuTask(vm *VM) {
	var err error
	success := false
	if err := gpu.Attach(vm); err != nil {
		G.logger.Error("cannot attach gpu", zap.Error(err))
		goto UPDATE
	}
	success = true
UPDATE:
	for {
		vm.PhaseStop(success)
		gpu.PhaseStop(success)
		var done bool
		if done, err = Save(nil, []RES{gpu, vm}); err != nil {
			G.logger.Error("cannot save reses", zap.Error(err))
			goto UPDATE
		}
		if done {
			return
		}
	}
}

func (gpu *GPU) DetachingGpuTask(vm *VM) {
	var err error
	success := false
	if err = gpu.Detach(vm); err != nil {
		G.logger.Error("cannot detach gpu", zap.Error(err))
		goto UPDATE
	}
	success = true
UPDATE:
	for {
		if success {
			vm.Gpus = Remove(vm.Gpus, gpu.Uuid)
			gpu.VmUuid = ""
		}
		vm.PhaseStop(success)
		gpu.PhaseStop(success)
		var done bool
		if done, err = Save(nil, []RES{gpu, vm}); err != nil {
			G.logger.Error("cannot save reses", zap.Error(err))
			goto UPDATE
		}
		if done {
			return
		}
	}
}

func IsEndpointPic(config string) bool {
	cmd := fmt.Sprintf("hexdump -n 16 %s|head -n 1", config)
	output, _ := exec.Command("sh", "-c", cmd).Output()
	line := strings.Split(string(output), "\n")[0]
	return strings.HasSuffix(line, "0")
}

func LoadHostdevs() (hostDevs map[string]map[string]string, err error) {

	gpuDeviceids := map[string]map[string]string{
		"10de:2684": {"name": "NVIDIA GeForce RTX 4090"},
		"10de:26ba": {"name": "NVIDIA L20"},
		"10de:2330": {"name": "NVIDIA H100"},
		"10de:2324": {"name": "NVIDIA H800"},
		"10de:1c82": {"name": "NVIDIA GeForce GTX 1050 Ti"},
		"10de:128b": {"name": "NVIDIA GeForce GT 710"},
	}

	ibDeviceids := map[string]map[string]string{"15b3:1021": {"name": "Mellanox ConnectX-7"}}
	hostDevs = map[string]map[string]string{}
	ibDevs := map[string]map[string]string{}

	groups, _ := getSubdirectories("/sys/kernel/iommu_groups")
	for _, group := range groups {

		devices, _ := getSubfiles("/sys/kernel/iommu_groups/" + group + "/devices")
		for _, device := range devices {

			device = strings.Trim(device, "\"")

			cmd := "lspci -nnks " + device
			output, err := ExecuteCmdOutput(G.config.CmdTimeout, cmd)
			if err != nil {
				return nil, err
			}

			result := strings.Split(string(output), "\n")[0]
			vendorDevice := regexp.MustCompile(`\[(\w+:\w+)\]`).FindStringSubmatch(result)[1]
			if _, ok := gpuDeviceids[vendorDevice]; ok {

				hostDev := map[string]string{
					"device_id":   vendorDevice,
					"device_pci":  device,
					"device_name": gpuDeviceids[vendorDevice]["name"],
					"others":      "",
				}

				for _, _device := range devices {
					config := "/sys/kernel/iommu_groups/" + group + "/devices/" + _device + "/config"
					if _device != device && IsEndpointPic(config) {
						if hostDev["others"] == "" {
							hostDev["others"] = _device
						} else {
							hostDev["others"] = hostDev["others"] + "," + _device
						}
					}
				}
				hostDevs[device] = hostDev
			} else if _, ok := ibDeviceids[vendorDevice]; vendorDevice != "" && ok {

				hostDev := map[string]string{
					"device_id":   vendorDevice,
					"device_pci":  device,
					"device_name": ibDeviceids[vendorDevice]["name"],
				}
				ibDevs[device] = hostDev
			}
		}
	}

	cmd := "find /sys/devices/ -type f -name device"
	output, err := ExecuteCmdOutput(G.config.CmdTimeout, cmd)
	if err != nil {
		return nil, err
	}
	result := strings.Split(string(output), "\n")
	for _, line := range result {
		line = strings.TrimSpace(line)
		if line == "" {
			continue
		}
		parts := strings.Split(line, "/")
		top := parts[3]
		pci := parts[len(parts)-2]

		if _, ok := hostDevs[pci]; ok {
			hostDevs[pci]["top"] = top
		}
		if _, ok := ibDevs[pci]; ok {
			ibDevs[pci]["top"] = top
		}
	}

	for pci, gpu := range hostDevs {
		if _, ok := gpu["bro"]; ok {
			continue
		}
		for _pci, _gpu := range hostDevs {
			if pci == _pci {
				continue
			}
			if gpu["top"] == _gpu["top"] {
				gpu["bro"] = _pci
				_gpu["bro"] = pci
				break
			}
		}
	}

	for _, ib := range ibDevs {
		for _, gpu := range hostDevs {
			if _, ok := gpu["ib"]; ok {
				continue
			}
			if gpu["top"] != ib["top"] {
				continue
			}
			gpu["ib"] = ib["device_pci"]
			break
		}
	}
	return hostDevs, nil
}

func IsNewGPU(rests RESTS, device_pci string) bool {
	for _, rest := range rests["gpu"] {
		gpu, ok := rest.(*GPU)
		if !ok {
			continue
		}
		if gpu.AgentUuid != G.config.Uuid {
			continue
		}
		if gpu.DevicePci == device_pci {
			return false
		}
	}
	return true
}

func IsRemovedGPU(device_pci string, hostDevs map[string]map[string]string) bool {
	if _, ok := hostDevs[device_pci]; !ok {
		return true
	}
	return false
}

func LoadPartitions() (*FMPartition, error) {
	cmd := fmt.Sprintf("%s/bin/fm -o list -i %s", G.config.AgentHome, G.config.FmIp)
	output, err := ExecuteCmdOutput(G.config.CmdTimeout, cmd)
	if err != nil {
		return nil, err
	}
	type JsonPartitions struct {
		Partitions []Partition `json:"partitions"`
	}
	jsonPartitions := &JsonPartitions{}
	err = json.Unmarshal(output, jsonPartitions)
	if err != nil {
		return nil, err
	}
	fmPartitions := &FMPartition{Partitions: make(map[string]*Partition)}
	for _, partition := range jsonPartitions.Partitions {
		fmPartitions.Partitions[partition.Id] = &partition
		partition.Gpus = []*GPU{}
	}
	return fmPartitions, nil
}

func ParseIDSFile(idsFile string) (map[string]string, error) {
	ids := make(map[string]string)
	if ok, _ := FileExists(idsFile); !ok {
		return nil, fmt.Errorf("gpu ids file not found")
	}
	f, err := os.Open(idsFile)
	if err != nil {
		return nil, err
	}
	defer f.Close()
	reader := bufio.NewReader(f)
	for {
		line, err := reader.ReadString('\n')
		if err != nil {
			if err.Error() == "EOF" {
				break
			}
		}
		parts := strings.Split(line, ": ")
		if len(parts) != 2 {
			continue
		}
		parts[0] = strings.TrimSpace(parts[0])
		parts[1] = strings.TrimSpace(parts[1])
		ids[parts[0]] = parts[1]
	}
	return ids, nil
}

func LoadGpus() error {
	hostDevs, err := LoadHostdevs()
	if err != nil {
		return err
	}

	rests := LoadRests("gpu")
	for pci, hostDev := range hostDevs {
		if IsNewGPU(rests, pci) {
			for {
				gpu := &GPU{AGENTREST: AGENTREST{AgentUuid: G.config.Uuid, ETCDINFO: ETCDINFO{Type: "gpu"}}}
				gpu.DevicePci = hostDev["device_pci"]
				gpu.DeviceId = hostDev["device_id"]
				gpu.DeviceName = hostDev["device_name"]
				if hostDev["others"] != "" {
					gpu.OtherPcis = strings.Split(hostDev["others"], ",")
				} else {
					gpu.OtherPcis = []string{}
				}
				gpu.IBPci = hostDev["ib"]
				gpu.BroPci = hostDev["bro"]
				if err := gpu.VerifyUuid(); err != nil {
					break
				}
				_ = gpu.PhaseStart(PhaseTypeAdd)
				gpu.PhaseStop(true)
				var done bool
				done, err = Save(nil, []RES{gpu})
				if err != nil {
					break
				}
				if done {
					break
				}
			}
		}
	}

	for _, rest := range rests["gpu"] {
		gpu, ok := rest.(*GPU)
		if !ok {
			continue
		}
		if gpu.AgentUuid != G.config.Uuid {
			continue
		}
		if IsRemovedGPU(gpu.DevicePci, hostDevs) {
			gpu.IsAdded = false
		}
	}

	if G.config.FmIp != "" {
		gpuIdMap := make(map[string]*GPU)

		ids, err := ParseIDSFile(G.config.AgentHome + "/.gpu_ids")
		if err != nil {
			return err
		}
		for pci, id := range ids {
			for _, rest := range rests["gpu"] {
				gpu, ok := rest.(*GPU)
				if !ok {
					continue
				}
				if gpu.AgentUuid != G.config.Uuid {
					continue
				}
				if gpu.DevicePci == pci {
					gpuIdMap[id] = gpu
					break
				}
			}
		}

		if gpuIdMap["1"] != nil && gpuIdMap["3"] != nil && gpuIdMap["2"] != nil && gpuIdMap["4"] != nil {
			g1 := gpuIdMap["1"]
			g3 := gpuIdMap["3"]
			g2 := gpuIdMap["2"]
			g4 := gpuIdMap["4"]
			if g1.IBPci == "" && g3.IBPci == "" && g2.IBPci != "" && g4.IBPci != "" {
				g1.IBPci = g2.IBPci
				g2.IBPci = ""
			}
		}
		if gpuIdMap["5"] != nil && gpuIdMap["7"] != nil && gpuIdMap["6"] != nil && gpuIdMap["8"] != nil {
			g5 := gpuIdMap["5"]
			g7 := gpuIdMap["7"]
			g6 := gpuIdMap["6"]
			g8 := gpuIdMap["8"]
			if g5.IBPci == "" && g7.IBPci == "" && g6.IBPci != "" && g8.IBPci != "" {
				g5.IBPci = g6.IBPci
				g6.IBPci = ""
			}
		}
		G.fmPartitions, err = LoadPartitions()
		if err != nil {
			return err
		}
		for _, partition := range G.fmPartitions.Partitions {
			for _, id := range partition.GpuIds {
				if gpuIdMap[id] != nil {
					if !Contains(partition.Gpus, gpuIdMap[id]) {
						partition.Gpus = append(partition.Gpus, gpuIdMap[id])
					}
				}
			}
		}
	}
	return nil
}

func GpuActive(num uint, releaseId string, ib bool) (gpuActiveRsp *GpuActiveRsp, err error) {
	orders := map[uint][]string{1: {"7", "9", "8", "10", "11", "13", "12", "14"}, 2: {"3", "4", "5", "6"}, 4: {"1", "2"}, 8: {"0"}}
	order := orders[num]
	G.sysLock.Lock()
	defer G.sysLock.Unlock()
	if G.fmPartitions == nil {
		return nil, fmt.Errorf("no fabric partitions")
	}
	for {
		reses := []RES{}

		if releaseId != "" {
			if _, ok := G.fmPartitions.Partitions[releaseId]; !ok {
				return nil, fmt.Errorf("partition %s not found", releaseId)
			}
			partition := G.fmPartitions.Partitions[releaseId]
			if !partition.IsActive {
				G.logger.Warn("partition not active", zap.String("id", releaseId))
			}
			for _, gpu := range partition.Gpus {
				gpu.PartitionId = ""
				reses = append(reses, gpu)
			}
			partition.IsActive = false
		}
		var partition *Partition
		var partitionId string

		for _, id := range order {
			if _, ok := G.fmPartitions.Partitions[id]; !ok {
				G.logger.Warn("partition not found", zap.String("id", id))
				continue
			}
			partition = G.fmPartitions.Partitions[id]
			if partition.IsActive {
				continue
			}

			all_free := true

			has_ib := false
			for _, gpu := range partition.Gpus {
				if gpu.IBPci != "" {
					has_ib = true
				}
				if gpu.PartitionId != "" {
					if gpu.PartitionId == id {
						G.logger.Warn("gpu already in partition", zap.String("gpu", gpu.Uuid), zap.String("partition", id))
						gpu.PartitionId = ""
					}
				}
				if gpu.PartitionId != "" {
					all_free = false
					break
				}
			}
			if !all_free {
				continue
			}
			if !has_ib && ib {
				continue
			}
			partitionId = id
			partition.IsActive = true
			break
		}
		if partitionId == "" {
			return nil, fmt.Errorf("no available partition")
		}
		if releaseId != "" {
			cmd := fmt.Sprintf("%s/bin/fm -o deactivate -i %s -p %s", G.config.AgentHome, G.config.FmIp, releaseId)
			err = ExecuteCmdRun(G.config.CmdTimeout, cmd)
			if err != nil {
				return nil, err
			}
		}
		cmd := fmt.Sprintf("%s/bin/fm -o activate -i %s -p %s", G.config.AgentHome, G.config.FmIp, partitionId)
		err = ExecuteCmdRun(G.config.CmdTimeout, cmd)
		if err != nil {
			return nil, err
		}
		for _, gpu := range partition.Gpus {
			gpu.PartitionId = partitionId
			if !Contains(reses, RES(gpu)) {
				reses = append(reses, gpu)
			}
		}
		Save(nil, reses)
		var done bool
		done, err = Save(nil, reses)
		if err != nil {
			return nil, err
		}
		if done {
			gpuActiveRsp = &GpuActiveRsp{Status: "ok", PartitionId: partitionId, GpuUUids: []string{}}
			for _, gpu := range partition.Gpus {
				gpuActiveRsp.GpuUUids = append(gpuActiveRsp.GpuUUids, gpu.Uuid)
			}
			break
		}
	}
	return gpuActiveRsp, nil
}

func GpuDeactive(partitionId string) (gpuDeactiveRsp *GpuDeactiveRsp, err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()
	if G.fmPartitions == nil {
		return nil, fmt.Errorf("no fabric partitions")
	}
	if _, ok := G.fmPartitions.Partitions[partitionId]; !ok {
		return nil, fmt.Errorf("partition %s not found", partitionId)
	}
	partition := G.fmPartitions.Partitions[partitionId]
	if !partition.IsActive {
		return nil, fmt.Errorf("partition %s is not active", partitionId)
	}
	for {
		reses := []RES{}
		for _, gpu := range partition.Gpus {
			gpu.PartitionId = ""
			reses = append(reses, gpu)
		}
		partition.IsActive = false
		cmd := fmt.Sprintf("%s/bin/fm -o deactivate -i %s -p %s", G.config.AgentHome, G.config.FmIp, partitionId)
		err := ExecuteCmdRun(G.config.CmdTimeout, cmd)
		if err != nil {
			return nil, err
		}
		var done bool
		done, err = Save(nil, reses)
		if err != nil {
			return nil, err
		}
		if done {
			gpuDeactiveRsp = &GpuDeactiveRsp{Status: "ok", PartitionId: partitionId, GpuUUids: []string{}}
			for _, gpu := range partition.Gpus {
				gpuDeactiveRsp.GpuUUids = append(gpuDeactiveRsp.GpuUUids, gpu.Uuid)
			}
			break
		}
	}
	return gpuDeactiveRsp, nil
}

func GpuCmdParser(command *cobra.Command) {
	var err error
	var gpuCmd = &cobra.Command{
		Use:   "gpu",
		Short: "VM management",
		Run: func(cmd *cobra.Command, args []string) {
			fmt.Println("please specify a subcommand for gpu management. use -h for help.")
		},
	}
	var gpuGetCmd = &cobra.Command{
		Use:   "get",
		Short: "Get a GPU",
		Run:   GpuGetHandle,
	}
	gpuGetCmd.Flags().StringVarP(&gpuFlags.uuid, "uuid", "U", "", "set the UUID of the GPU")
	if err = gpuGetCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var gpuListCmd = &cobra.Command{
		Use:   "list",
		Short: "List all GPUs",
		Run:   GpuListHandle,
	}
	gpuListCmd.Flags().BoolVar(&gpuFlags.abnormal, "abnormal", false, "list abnormal GPUs")

	var gpuAttachCmd = &cobra.Command{
		Use:   "attach",
		Short: "Attach a gpu to a VM",
		Run:   GpuAttachHandle,
	}
	gpuAttachCmd.Flags().StringVarP(&gpuFlags.uuid, "uuid", "U", "", "set the UUID of the GPU")
	if err = gpuAttachCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	gpuAttachCmd.Flags().StringVar(&gpuFlags.vmUuid, "vm", "", "set the UUID of the VM")
	if err = gpuAttachCmd.MarkFlagRequired("vm"); err != nil {
		panic(err)
	}
	var gpuDetachCmd = &cobra.Command{
		Use:   "detach",
		Short: "Detach a gpu from a VM",
		Run:   GpuDetachHandle,
	}
	gpuDetachCmd.Flags().StringVarP(&gpuFlags.uuid, "uuid", "U", "", "set the UUID of the GPU")
	if err = gpuDetachCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var gpuActiveCmd = &cobra.Command{
		Use:   "active",
		Short: "Active a GPU partition",
		Run:   GpuActiveHandle,
	}
	gpuActiveCmd.Flags().StringVarP(&gpuFlags.partition, "partition", "I", "", "partition ID to deactive first")
	gpuActiveCmd.Flags().UintVarP(&gpuFlags.num, "num", "N", 1, "number of GPUs to active")
	gpuActiveCmd.Flags().BoolVar(&gpuFlags.ib, "ib", false, "need IB card")
	var gpuDeactiveCmd = &cobra.Command{
		Use:   "deactive",
		Short: "Deactive a GPU partition",
		Run:   GpuDeactiveHandle,
	}
	gpuDeactiveCmd.Flags().StringVarP(&gpuFlags.partition, "partition", "I", "", "partition ID to deactive")
	if err = gpuDeactiveCmd.MarkFlagRequired("partition"); err != nil {
		panic(err)
	}
	gpuCmd.AddCommand(gpuGetCmd, gpuListCmd, gpuAttachCmd, gpuDetachCmd, gpuActiveCmd, gpuDeactiveCmd)
	command.AddCommand(gpuCmd)
}

func GpuGetHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/gpus/%s", G.Host, G.Port, gpuFlags.uuid)
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

func GpuListHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/gpus", G.Host, G.Port)
	data := map[string]interface{}{}
	if gpuFlags.abnormal {
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

func GpuAttachHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/gpus/%s/attach", G.Host, G.Port, gpuFlags.uuid)
	data := map[string]interface{}{"vm": gpuFlags.vmUuid}
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

func GpuDetachHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/gpus/%s/detach", G.Host, G.Port, gpuFlags.uuid)
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

func GpuActiveHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/gpus/active", G.Host, G.Port)
	data := map[string]interface{}{"num": gpuFlags.num, "ib": gpuFlags.ib, "partition_id": gpuFlags.partition}
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

func GpuDeactiveHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/gpus/deactive", G.Host, G.Port)
	data := map[string]interface{}{"partition_id": gpuFlags.partition}
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

func GpuAppSetup() {
	G.echoServer.GET("/v1/gpus", AppGpuList)
	G.echoServer.GET("/v1/gpus/:uuid", AppGpuGet)
	G.echoServer.PUT("/v1/gpus/:uuid/attach", AppGpuAttach)
	G.echoServer.PUT("/v1/gpus/:uuid/detach", AppGpuDetach)
	G.echoServer.PUT("/v1/gpus/active", AppGpuActive)
	G.echoServer.PUT("/v1/gpus/deactive", AppGpuDeactive)
}

func AppGpuList(c echo.Context) (err error) {
	G.logger.Debug("=========AppGpuList==========")
	q := AppListReq{}
	if err = c.Bind(&q); err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
	}
	gpuUuids := []string{}
	rests := LoadRests("gpu")
	for _, rest := range rests["gpu"] {
		gpu, ok := rest.(*GPU)
		if !ok {
			continue
		}
		if gpu.AgentUuid != G.config.Uuid {
			continue
		}
		if q.Abnormal {
			if gpu.NotReady() {
				gpuUuids = append(gpuUuids, gpu.Uuid)
			}
		} else {
			gpuUuids = append(gpuUuids, gpu.Uuid)
		}
	}
	jsonMap := make(map[string]interface{})
	jsonMap["status"] = "ok"
	jsonMap["gpu_count"] = len(gpuUuids)
	jsonMap["gpus"] = gpuUuids
	return c.JSON(http.StatusOK, jsonMap)
}

func AppGpuGet(c echo.Context) (err error) {
	G.logger.Debug("=========AppGpuGet==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("GPU UUID is required."))
	}
	var gpu *GPU
	if gpu, err = LoadGPU(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if gpu == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("GPU not found.."))
	}
	rsp := GpuGetRsp{
		Status:      "ok",
		DevicePci:   gpu.DevicePci,
		DeviceName:  gpu.DeviceName,
		VmUuid:      gpu.VmUuid,
		IBPci:       gpu.IBPci,
		BroPci:      gpu.BroPci,
		OtherPcis:   gpu.OtherPcis,
		DeviceId:    gpu.DeviceId,
		PartitionId: gpu.PartitionId,
		AgentUuid:   gpu.AgentUuid,
		AGENTREST:   gpu.AGENTREST,
		PHASEINFO:   gpu.PHASEINFO}
	return c.JSON(http.StatusOK, &rsp)
}

func AppGpuAttach(c echo.Context) (err error) {
	G.logger.Debug("=========AppGpuAttach==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("GPU UUID is required."))
	}
	var gpu *GPU
	if gpu, err = LoadGPU(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if gpu == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("GPU not found.."))
	}
	q := GpuAttachReq{}
	if err = c.Bind(&q); err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
	}
	vm, err := LoadVM(q.VmUuid)
	if err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if vm == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Virtual machine not found."))
	}
	for {
		if gpu.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("GPU is not ready."))
		}
		if gpu.VmUuid != "" && gpu.VmUuid != vm.Uuid {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("GPU is in use."))
		}
		if vm.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is not ready."))
		}
		if vm.IsSuspend() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is suspended."))
		}
		if len(vm.Gpus) > 7 {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine has 8 attached GPUs."))
		}
		if err = vm.PhaseStart(PhaseTypeGPU); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		if err = gpu.PhaseStart(PhaseTypeAttach); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		if !Contains(vm.Gpus, gpu.Uuid) {
			vm.Gpus = append(vm.Gpus, gpu.Uuid)
		}
		gpu.VmUuid = vm.Uuid
		var done bool
		done, err = Save(nil, []RES{gpu, vm})
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	go gpu.AttachingGpuTask(vm)
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(gpu.Uuid))
}

func AppGpuDetach(c echo.Context) (err error) {
	G.logger.Debug("=========AppGpuDetach==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("GPU UUID is required."))
	}
	var gpu *GPU
	if gpu, err = LoadGPU(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if gpu == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("GPU not found.."))
	}
	if gpu.VmUuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("GPU is not in use."))
	}
	vm, err := LoadVM(gpu.VmUuid)
	if err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if vm == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Virtual machine not found."))
	}
	for {
		if gpu.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("GPU is not added."))
		}
		if gpu.VmUuid == "" {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("GPU is not in use."))
		}
		if vm.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is not added."))
		}
		if vm.IsSuspend() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is suspended."))
		}
		if err = vm.PhaseStart(PhaseTypeGPU); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		if err = gpu.PhaseStart(PhaseTypeDetach); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		var done bool
		done, err = Save(nil, []RES{gpu})
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	go gpu.DetachingGpuTask(vm)
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(gpu.Uuid))
}

func AppGpuActive(c echo.Context) (err error) {
	G.logger.Debug("=========AppGpuActive==========")
	q := GpuActiveReq{}
	if err = c.Bind(&q); err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
	}
	if q.Num != 1 && q.Num != 2 && q.Num != 4 && q.Num != 8 {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Invalid number of GPUs."))
	}

	gpuActiveRsp, err := GpuActive(q.Num, q.PartitionId, q.IB)
	if err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	return c.JSON(http.StatusOK, gpuActiveRsp)
}

func AppGpuDeactive(c echo.Context) (err error) {
	G.logger.Debug("=========AppGpuDeactive==========")
	q := GpuDeactiveReq{}
	if err = c.Bind(&q); err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
	}
	if q.PartitionId == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Partition ID is required."))
	}
	gpuDeactiveRsp, err := GpuDeactive(q.PartitionId)
	if err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	return c.JSON(http.StatusOK, gpuDeactiveRsp)
}
