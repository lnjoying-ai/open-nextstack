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
	"os"
	"strings"

	"github.com/labstack/echo"
	"github.com/spf13/cobra"
	"go.uber.org/zap"
)

type VM struct {
	Vcpu          uint32              `json:"vcpu"`
	Mem           uint32              `json:"mem"`
	Bootdev       string              `json:"bootdev"`
	Vnc           string              `json:"vnc"`
	Ports         []string            `json:"ports"`
	Vols          []string            `json:"vols"`
	Gpus          []string            `json:"gpus"`
	Power         string              `json:"power"`
	Cps           []string            `json:"cps"`
	PoolUuid      string              `json:"pool"`
	CloudInitInfo *CloudInitInfoT     `json:"cloudinit"`
	LocalRes      map[string][]string `json:"local_res"`
	AGENTREST
	PHASEINFO
}

type CloudInitInfoT struct {
	Os         string `json:"os"`
	Hostname   string `json:"hostname"`
	Username   string `json:"username"`
	Password   string `json:"password"`
	Pubkey     string `json:"pubkey"`
	InstanceId string `json:"instance_id"`
	Done       bool   `json:"done"`
}

type VmFlags struct {
	uuid     string
	abnormal bool
	force    bool
	vcpu     uint32
	mem      uint32
	bootdev  string
	os       string
	hostname string
	username string
	password string
	pubkey   string
	nbddev   string
	port     []string
	vol      []string
	gpu      []string
}

type VmAddReq struct {
	Vcpu  uint32   `json:"vcpu"`
	Mem   uint32   `json:"mem"`
	Ports []string `json:"ports"`
	Vols  []string `json:"vols"`
	Gpus  []string `json:"gpus"`
}

type VmPoweroffReq struct {
	Force bool `json:"force"`
}

type VmInjectReq struct {
	Os       string `json:"os"`
	Hostname string `json:"hostname"`
	Username string `json:"username"`
	Password string `json:"password"`
	Pubkey   string `json:"pubkey"`
}

type VmInject2Req struct {
	Nbddev string `json:"nbddev"`
}

type VmEject2Req struct {
	Nbddev string `json:"nbddev"`
}

type VmGetRsp struct {
	Status        string          `json:"status"`
	Vcpu          uint32          `json:"vcpu"`
	Mem           uint32          `json:"mem"`
	Vnc           string          `json:"vnc"`
	Ports         []string        `json:"ports"`
	Vols          []string        `json:"vols"`
	Gpus          []string        `json:"gpus"`
	Power         string          `json:"power"`
	Cps           []string        `json:"cps"`
	PoolUuid      string          `json:"pool"`
	AgentUuid     string          `json:"agent"`
	CloudInitInfo *CloudInitInfoT `json:"cloudinit"`
	AGENTREST
	PHASEINFO
}

type VmModifyReq struct {
	Vcpu    uint32 `json:"vcpu"`
	Mem     uint32 `json:"mem"`
	Bootdev string `json:"bootdev"`
}

type MoveVmRtn struct {
	toDel  []RES
	toPut  []RES
	ports  []*Port
	vols   []*Vol
	vol0s  []*Vol
	gpus   []*GPU
	gpu0s  []*GPU
	snaps  []*Snap
	snap0s []*Snap
	imgs   []*Img
	img0s  []*Img
	cps    []*CP
	cp0s   []*CP
	pool   *Pool
	pool0  *Pool
	vm0    *VM
	vm     *VM
}

var vmFlags VmFlags

func LoadVM(vm_uuid string) (vm *VM, err error) {
	vm = &VM{AGENTREST: AGENTREST{UUIDINFO: UUIDINFO{Uuid: vm_uuid}, AgentUuid: G.config.Uuid, ETCDINFO: ETCDINFO{Type: "vm"}}}
	path := vm.GetKey()
	var ret RES
	if ret, err = LoadRes(path, vm); err != nil {
		return nil, err
	}
	if ret != nil {
		vm, ok := ret.(*VM)
		if ok {
			return vm, nil
		}
	}
	return nil, nil
}

func LoadVM4(vm_uuid string, agentUuid string) (vm *VM, err error) {
	vm = &VM{AGENTREST: AGENTREST{UUIDINFO: UUIDINFO{Uuid: vm_uuid}, AgentUuid: agentUuid, ETCDINFO: ETCDINFO{Type: "vm"}}}
	path := vm.GetKey()
	var ret RES
	if ret, err = LoadRes(path, vm); err != nil {
		return nil, err
	}
	if ret != nil {
		vm, ok := ret.(*VM)
		if ok {
			return vm, nil
		}
	}
	return nil, nil
}

func LoadVM2(rests RESTS, instanceId string) (vm *VM, err error) {
	for _, rest := range rests["vm"] {
		vm, ok := rest.(*VM)
		if !ok {
			continue
		}
		if vm.AgentUuid != G.config.Uuid {
			continue
		}
		if vm.CloudInitInfo != nil && vm.CloudInitInfo.InstanceId == instanceId {
			return vm, nil
		}
	}
	return nil, nil
}

func LoadVM3(vmUuid string) (vm *VM, err error) {
	rests := LoadRests("agent")
	for _, res := range rests["agent"] {
		agent, ok := res.(*Agent)
		if !ok {
			continue
		}
		vm = &VM{AGENTREST: AGENTREST{UUIDINFO: UUIDINFO{Uuid: vmUuid}, AgentUuid: agent.Uuid, ETCDINFO: ETCDINFO{Type: "vm"}}}
		path := vm.GetKey()
		var ret RES
		if ret, err = LoadRes(path, vm); err != nil {
			return nil, err
		}
		if ret != nil {
			vm, ok := ret.(*VM)
			if ok {
				return vm, nil
			}
		}
	}
	return nil, nil
}

func (o *VM) IsSuspend() bool {
	return (o.PhaseType == PhaseTypeSuspend && o.PhaseStatus == PhaseStatusSuccess) || (o.PhaseType == PhaseTypeResume && o.PhaseStatus == PhaseStatusFail) || (o.PhaseType == PhaseTypeMigrate && o.PhaseStatus == PhaseStatusFail)
}

func (vm *VM) GenVncTokenFile(pool *Pool, vncPort string) error {
	dir := pool.ParaMap["DIR"] + "/tokens/" + vm.AgentUuid
	if exists, _ := DirExists(dir); !exists {
		if err := os.MkdirAll(dir, 0755); err != nil {
			return err
		}
	}
	tokenFile := dir + "/" + vm.Uuid
	file, err := os.OpenFile(tokenFile, os.O_CREATE|os.O_WRONLY, 0644)
	if err != nil {
		return err
	}
	defer file.Close()
	_, err = file.WriteString(fmt.Sprintf("%s: 127.0.0.1:%s\n", vm.Uuid, vncPort))
	return err
}

func (vm *VM) Add(ports []*Port, vols []*Vol, pool *Pool, gpus []*GPU, vmLocalRes []string) (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()

	if _, ok := G.deployedRest[vm.Uuid]; ok {
		G.logger.Error("vm already deployed", zap.String("uuid", vm.Uuid))
		return nil
	}
	if err = vm.GenVncTokenFile(pool, vmLocalRes[0]); err != nil {
		G.logger.Error("cannot generate vnc token file", zap.Error(err))
		return err
	}
	for _, port := range ports {
		if !osNicExists(port.Nic, "") {
			if err = osTapNicAdd(port.Nic); err != nil {
				G.logger.Error("cannot add tap nic", zap.Error(err))
				return err
			}
		}
		if err = osNicUp(port.Nic, ""); err != nil {
			G.logger.Error("cannot up nic", zap.Error(err))
			return err
		}
		if err = OvsNicAdd(G.config.LanBridge, port.Nic, false, port.Ofport, port.Vlanid); err != nil {
			G.logger.Error("cannot add ovs nic", zap.Error(err))
			return err
		}
		if err = OvsNoFlood(G.config.LanBridge, port.Nic); err != nil {
			G.logger.Error("cannot set ovs no flood", zap.Error(err))
			return err
		}
		if err = OvsVmAdd(port.Nic, port.Vlanid, port.Mac, port.Ip, port.Ofport, port.Speed, port.Cidr); err != nil {
			G.logger.Error("cannot add ovs vm", zap.Error(err))
			return err
		}
		if G.config.DpuIp != "" {
			DpuAddPort(port.NID, port.Vlanid, port.Mac, port.Ip, port.Uuid)
		}
	}
	if !ExistVM(vm.Uuid) {
		if err = CreateVM(vm, ports, vols, pool, gpus, vmLocalRes[0]); err != nil {
			return err
		}
	}
	if vm.Power == "running" {
		if err = PoweronVM(vm.Uuid); err != nil {
			G.logger.Error("cannot poweron the vm", zap.Error(err))
			return err
		}
	}
	G.deployedRest[vm.Uuid] = struct{}{}
	return nil
}

func (vm *VM) Del(ports []*Port, pool *Pool) (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()

	if _, ok := G.deployedRest[vm.Uuid]; !ok {
		return nil
	}
	if err = DelVM(vm.Uuid); err != nil {
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
		if err = osNicDel(port.Nic, ""); err != nil {
			G.logger.Error("cannot delete tap nic", zap.Error(err))
			return err
		}
		if G.config.DpuIp != "" {
			DpuDelPort(port.Uuid)
		}
	}
	tokenFile := pool.ParaMap["DIR"] + "/tokens/" + vm.AgentUuid + "/" + vm.Uuid
	if ok, _ := FileExists(tokenFile); ok {
		if err = os.Remove(tokenFile); err != nil {
			return err
		}
	}
	delete(G.deployedRest, vm.Uuid)
	return nil
}

func (vm *VM) Poweron() (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()

	if _, ok := G.deployedRest[vm.Uuid]; !ok {
		return nil
	}
	return PoweronVM(vm.Uuid)
}

func (vm *VM) Poweroff(force bool) (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()

	if _, ok := G.deployedRest[vm.Uuid]; !ok {
		return nil
	}
	return PoweroffVM(vm.Uuid, force)
}

func (vm *VM) Inject() (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()

	if _, ok := G.deployedRest[vm.Uuid]; !ok {
		return nil
	}
	var nics []map[string]string
	var paras map[string]string
	var isoFile string
	paras = vm.GetCloudInitParas()
	if nics, err = vm.GetNics(); err != nil {
		G.logger.Error("cannot get nics", zap.Error(err))
		return err
	}
	if paras["DNS"] == "" && len(nics) > 0 {
		paras["DNS"] = nics[0]["DNS"]
	}
	if paras["IP"] == "" && len(nics) > 0 {
		paras["IP"] = nics[0]["IP"]
	}
	if isoFile, err = GenISO(vm.Uuid, paras, nics, vm.CloudInitInfo.InstanceId); err != nil {
		G.logger.Error("cannot generate cloud-init iso", zap.Error(err))
		return err
	}
	if err = InjectVM(vm.Uuid, isoFile); err != nil {
		G.logger.Error("cannot inject cloud-init cdrom to the vm", zap.Error(err))
		return err
	}
	return nil
}

func (vm *VM) Inject2(nbddev string) (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()

	if _, ok := G.deployedRest[vm.Uuid]; !ok {
		return nil
	}
	if err = Inject2VM(vm.Uuid, nbddev); err != nil {
		G.logger.Error("cannot inject nbd device to the vm", zap.Error(err))
		return err
	}
	return nil
}

func (vm *VM) Eject() (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()

	if _, ok := G.deployedRest[vm.Uuid]; !ok {
		return nil
	}
	if err = EjectVM(vm.Uuid); err != nil {
		G.logger.Error("cannot eject cloud-init cdrom from the vm", zap.Error(err))
		return err
	}
	return nil
}

func (vm *VM) Eject2(nbddev string) (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()

	if _, ok := G.deployedRest[vm.Uuid]; !ok {
		return nil
	}
	if err = Eject2VM(vm.Uuid, nbddev); err != nil {
		G.logger.Error("cannot eject nbd device from the vm", zap.Error(err))
		return err
	}
	return nil
}

func (vm *VM) Suspend(ports []*Port, pool *Pool) (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()

	if _, ok := G.deployedRest[vm.Uuid]; !ok {
		return nil
	}
	if pool.Stype != "fs" {
		return fmt.Errorf("pool %s is not fs", pool.Uuid)
	}
	if err = SuspendVM(vm.Uuid, pool.ParaMap["DIR"]); err != nil {
		G.logger.Error("cannot suspend the vm", zap.Error(err))
		return err
	}
	for _, port := range ports {
		if err = OvsVmDel(port.Nic, port.Vlanid, port.Mac, port.Ip, port.Ofport, port.Speed, port.Cidr); err != nil {
			G.logger.Error("cannot delete ovs vm", zap.Error(err))
			return err
		}
		if err = OvsNicDel(G.config.LanBridge, port.Nic); err != nil {
			G.logger.Error("cannot delete ovs nic", zap.Error(err))
			return err
		}
		if err = osNicDel(port.Nic, ""); err != nil {
			G.logger.Error("cannot delete tap nic", zap.Error(err))
			return err
		}
		if G.config.DpuIp != "" {
			DpuDelPort(port.Uuid)
		}
	}
	tokenFile := pool.ParaMap["DIR"] + "/tokens/" + vm.AgentUuid + "/" + vm.Uuid
	if ok, _ := FileExists(tokenFile); ok {
		if err = os.Remove(tokenFile); err != nil {
			return err
		}
	}
	delete(G.deployedRest, vm.Uuid)
	return nil
}

func (vm *VM) Resume(ports []*Port, vols []*Vol, pool *Pool, gpus []*GPU, cps []*CP, vmLocalRes []string) (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()

	if _, ok := G.deployedRest[vm.Uuid]; ok {
		G.logger.Error("vm already deployed", zap.String("uuid", vm.Uuid))
		return nil
	}
	if err = vm.GenVncTokenFile(pool, vmLocalRes[0]); err != nil {
		G.logger.Error("cannot generate vnc token file", zap.Error(err))
		return err
	}
	for _, port := range ports {
		if !osNicExists(port.Nic, "") {
			if err = osTapNicAdd(port.Nic); err != nil {
				G.logger.Error("cannot add tap nic", zap.Error(err))
				return err
			}
		}
		if err = osNicUp(port.Nic, ""); err != nil {
			G.logger.Error("cannot up nic", zap.Error(err))
			return err
		}
		if err = OvsNicAdd(G.config.LanBridge, port.Nic, false, port.Ofport, port.Vlanid); err != nil {
			G.logger.Error("cannot add ovs nic", zap.Error(err))
			return err
		}
		if err = OvsNoFlood(G.config.LanBridge, port.Nic); err != nil {
			G.logger.Error("cannot set ovs no flood", zap.Error(err))
			return err
		}
		if err = OvsVmAdd(port.Nic, port.Vlanid, port.Mac, port.Ip, port.Ofport, port.Speed, port.Cidr); err != nil {
			G.logger.Error("cannot add ovs vm", zap.Error(err))
			return err
		}
		if G.config.DpuIp != "" {
			DpuAddPort(port.NID, port.Vlanid, port.Mac, port.Ip, port.Uuid)
		}
	}
	for _, cp := range cps {
		defpath := pool.ParaMap["DIR"] + "/volumes/" + cp.Uuid + ".xml"
		if ok, _ := FileExists(defpath); ok {
			if err = FixHugepage(defpath); err != nil {
				return err
			}
			if err = FixVnc(defpath, vmLocalRes[0]); err != nil {
				return err
			}
			if err = FixCdrom(defpath); err != nil {
				return err
			}
			for _, port := range ports {
				if err = FixVnet(defpath, port.Nic, port.Mac); err != nil {
					return
				}
			}
		}
	}
	if !ExistVM(vm.Uuid) {
		mempath := pool.ParaMap["DIR"] + "/volumes/" + vm.Uuid + ".suspend"
		defpath := pool.ParaMap["DIR"] + "/volumes/" + vm.Uuid + ".xml"
		if ok, _ := FileExists(mempath); ok {
			if ok, _ := FileExists(defpath); !ok {
				defpath = ""
			} else {
				if err = FixHugepage(defpath); err != nil {
					return err
				}
				if err = FixVnc(defpath, vmLocalRes[0]); err != nil {
					return err
				}
				if err = FixCdrom(defpath); err != nil {
					return err
				}
				for _, port := range ports {
					if err = FixVnet(defpath, port.Nic, port.Mac); err != nil {
						return
					}
				}
			}
			if err = ResumeVM(vm.Uuid, mempath, defpath, gpus); err != nil {
				G.logger.Error("cannot resume the vm", zap.Error(err))
				return err
			}
		} else {
			if err = CreateVM(vm, ports, vols, pool, gpus, vmLocalRes[0]); err != nil {
				return err
			}
			if vm.Power == "running" {
				if err = PoweronVM(vm.Uuid); err != nil {
					G.logger.Error("cannot poweron the vm", zap.Error(err))
					return err
				}
			}
		}
	}
	G.deployedRest[vm.Uuid] = struct{}{}
	return nil
}

func (vm *VM) Restore() (err error) {
	var vmLocalRes []string
	if vmLocalRes, err = vm.AssignLocalRes(true); err != nil {
		return nil
	}
	vm.Vnc = vmLocalRes[0]
	var ports []*Port
	var vols []*Vol
	var pool *Pool
	var gpus []*GPU
	for i, portUuid := range vm.Ports {
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
		if subnet == nil {
			return fmt.Errorf("subnet not found")
		}
		if vpc, err = LoadVPC(subnet.VpcUuid); err != nil {
			G.logger.Error("Cannot load VPC.", zap.Error(err))
			return err
		}
		if vpc == nil {
			return fmt.Errorf("vpc not found")
		}
		if vpcLocalRes, err = vpc.AssignLocalRes(true); err != nil {
			G.logger.Error("cannot assign local res", zap.Error(err))
			return err
		}
		port.Nic = vmLocalRes[i*2+1]
		port.Ofport = vmLocalRes[i*2+2]
		port.Vlanid = vpcLocalRes[0]
		port.NID = vpc.NID
		port.Cidr = vpc.Cidr
		ports = append(ports, port)
	}
	for _, volUuid := range vm.Vols {
		var vol *Vol
		if vol, err = LoadVol(volUuid); err != nil {
			G.logger.Error("Cannot load volume.", zap.Error(err))
			return err
		}
		if vol == nil {
			return fmt.Errorf("volume not found")
		}
		vols = append(vols, vol)
	}
	if pool, err = LoadPool(vm.PoolUuid); err != nil {
		G.logger.Error("Cannot load pool.", zap.Error(err))
		return err
	}
	if pool == nil {
		return fmt.Errorf("pool not found")
	}
	for _, gpuUuid := range vm.Gpus {
		var gpu *GPU
		if gpu, err = LoadGPU(gpuUuid); err != nil {
			G.logger.Error("cannot load gpu", zap.Error(err))
			return err
		}
		if gpu == nil {
			return fmt.Errorf("GPU not found")
		}
		gpus = append(gpus, gpu)
	}
	if err = vm.Add(ports, vols, pool, gpus, vmLocalRes); err != nil {
		G.logger.Error("cannot add vpc", zap.Error(err))
		return err
	}
	return nil
}

func (vm *VM) Modify() (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()

	if _, ok := G.deployedRest[vm.Uuid]; !ok {
		return nil
	}
	if err = ModifyVM(vm.Uuid, vm.Vcpu, vm.Mem, vm.Bootdev); err != nil {
		G.logger.Error("cannot modify the vm", zap.Error(err))
		return err
	}
	return nil
}

func (vm *VM) AddingVmTask(ports []*Port, vols []*Vol, pool *Pool, gpus []*GPU, hostSG *HOSTSGT) {
	var err error
	success := false
	var vlanids []string
	var vmLocalRes []string
	agent := LoadAgent(G.config.Uuid)
	if agent == nil {
		G.logger.Error("Cannot load agent.", zap.Error(err))
		goto UPDATE
	}
	if vmLocalRes, err = vm.AssignLocalRes(false); err != nil {
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
		if vpc == nil {
			G.logger.Error("VPC not ready.", zap.String("uuid", subnet.VpcUuid))
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
		port.Nic = vmLocalRes[i*2+1]
		port.Ofport = vmLocalRes[i*2+2]
		port.Vlanid = vpcLocalRes[0]
		port.NID = vpc.NID
		port.Cidr = vpc.Cidr
		if !Contains(vlanids, port.Vlanid) {
			vlanids = append(vlanids, port.Vlanid)
		}
	}
	vm.Vnc = vmLocalRes[0]
	if err = vm.Add(ports, vols, pool, gpus, vmLocalRes); err != nil {
		G.logger.Error("cannot add vm", zap.Error(err))
		goto UPDATE
	}
	success = true
UPDATE:
	for {
		reses := []RES{vm, hostSG}
		var done bool
		vm.PhaseStop(success)
		for i, port := range ports {
			if len(port.Sgs) > 0 {
				hostSG.Ports[port.Uuid] = vmLocalRes[i*2+2]
			}
			port.PhaseStop(success)
			reses = append(reses, port)
		}
		for _, vol := range vols {
			vol.PhaseStop(success)
			reses = append(reses, vol)
		}
		for _, gpu := range gpus {
			gpu.PhaseStop(success)
			reses = append(reses, gpu)
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
		for _, vlanid := range vlanids {
			NoticeDnsmasq(vlanid)
		}
		NoticeSG()
	}
}

func (vm *VM) DeletingVmTask(ports []*Port, vols []*Vol, pool *Pool, gpus []*GPU, hostSG *HOSTSGT) {
	var err error
	var agent *Agent
	var vlanids []string
	if err = vm.Del(ports, pool); err != nil {
		G.logger.Error("cannot delete vm", zap.Error(err))
		goto UPDATE
	}
	if err = vm.UnAssignLocalRes(); err != nil {
		G.logger.Error("cannot release vm local res", zap.Error(err))
		goto UPDATE
	}
	agent = LoadAgent(G.config.Uuid)
	if agent == nil {
		G.logger.Error("Cannot load agent.", zap.Error(err))
		goto UPDATE
	}
	for _, port := range ports {
		if port.Vlanid != "" {
			if !Contains(vlanids, port.Vlanid) {
				vlanids = append(vlanids, port.Vlanid)
			}
		}
		var subnet *Subnet
		var vpc *VPC
		var vpcLocalRes []string
		if subnet, err = LoadSubnet(port.SubnetUuid); err != nil {
			continue
		}
		if subnet == nil {
			continue
		}
		if vpc, err = LoadVPC(subnet.VpcUuid); err != nil {
			continue
		}
		if vpc == nil {
			continue
		}
		if vpcLocalRes, err = vpc.AssignLocalRes(true); err != nil {
			continue
		}
		if !G.config.MasterL3 {
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
	for {
		reses := []RES{}
		if hostSG != nil {
			reses = append(reses, hostSG)
		}
		for _, vol := range vols {
			vol.VmUuid = ""
			reses = append(reses, vol)
		}
		for _, gpu := range gpus {
			gpu.VmUuid = ""
			reses = append(reses, gpu)
		}
		for _, port := range ports {
			if hostSG != nil {
				delete(hostSG.Ports, port.Uuid)
			}
			port.ByUuid = ""
			reses = append(reses, port)
		}
		var done bool
		done, err = Save([]RES{vm}, reses)
		if err != nil {
			G.logger.Error("cannot save reses", zap.Error(err))
			goto UPDATE
		}
		if done {
			for _, vlanid := range vlanids {
				NoticeDnsmasq(vlanid)
			}
			NoticeSG()
			return
		}
	}
UPDATE:
	if _, err = UpdatePhaseStop(vm, false); err != nil {
		G.logger.Error("cannot update vm phase", zap.Error(err))
	}
}

func (vm *VM) PoweringOnVmTask() {
	var err error
	success := false
	if err = vm.Poweron(); err != nil {
		G.logger.Error("cannot poweron vm", zap.Error(err))
		goto UPDATE
	}
	success = true
UPDATE:
	if _, err = UpdatePhaseStop(vm, success); err != nil {
		G.logger.Error("cannot update vm phase", zap.Error(err))
	}
}

func (vm *VM) PoweringOffVmTask(force bool) {
	var err error
	success := false
	if err = vm.Poweroff(force); err != nil {
		G.logger.Error("cannot poweron vm", zap.Error(err))
		goto UPDATE
	}
	success = true
UPDATE:
	if _, err = UpdatePhaseStop(vm, success); err != nil {
		G.logger.Error("cannot update vm phase", zap.Error(err))
	}
}

func (vm *VM) InjectingVmTask() {
	var err error
	success := false
	if err = vm.Inject(); err != nil {
		G.logger.Error("cannot inject cloud-init cdrom to the vm", zap.Error(err))
		goto UPDATE
	}
	success = true
UPDATE:
	if _, err = UpdatePhaseStop(vm, success); err != nil {
		G.logger.Error("cannot update vm phase", zap.Error(err))
	}
}

func (vm *VM) Injecting2VmTask(nbddev string) {
	var err error
	success := false
	if err = vm.Inject2(nbddev); err != nil {
		G.logger.Error("cannot inject nbd device to the vm", zap.Error(err))
		goto UPDATE
	}
	success = true
UPDATE:
	if _, err = UpdatePhaseStop(vm, success); err != nil {
		G.logger.Error("cannot update vm phase", zap.Error(err))
	}
}

func (vm *VM) EjectingVmTask() {
	var err error
	success := false
	if err = vm.Eject(); err != nil {
		G.logger.Error("cannot eject cloud-init cdrom from the vm", zap.Error(err))
		goto UPDATE
	}
	success = true
UPDATE:
	if _, err = UpdatePhaseStop(vm, success); err != nil {
		G.logger.Error("cannot update vm phase", zap.Error(err))
	}
}

func (vm *VM) Ejecting2VmTask(nbddev string) {
	var err error
	success := false
	if err = vm.Eject2(nbddev); err != nil {
		G.logger.Error("cannot eject nbd device from the vm", zap.Error(err))
		goto UPDATE
	}
	success = true
UPDATE:
	if _, err = UpdatePhaseStop(vm, success); err != nil {
		G.logger.Error("cannot update vm phase", zap.Error(err))
	}
}

func (vm *VM) SuspendingVmTask(ports []*Port, vols []*Vol, pool *Pool, hostSG *HOSTSGT) {
	var err error
	var agent *Agent
	var vlanids []string
	success := false
	if err = vm.Suspend(ports, pool); err != nil {
		G.logger.Error("cannot suspend vm", zap.Error(err))
		goto UPDATE
	}
	if vm.CloudInitInfo != nil && vm.CloudInitInfo.InstanceId != "" {
		isoFile := "/tmp/" + vm.CloudInitInfo.InstanceId + ".iso"
		if ok, _ := FileExists(isoFile); ok {
			os.Remove(isoFile)
		}
	}
	if err = vm.UnAssignLocalRes(); err != nil {
		G.logger.Error("cannot release vm local res", zap.Error(err))
		goto UPDATE
	}
	agent = LoadAgent(G.config.Uuid)
	if agent == nil {
		G.logger.Error("Cannot load agent.", zap.Error(err))
		goto UPDATE
	}
	for _, port := range ports {
		if !Contains(vlanids, port.Vlanid) {
			vlanids = append(vlanids, port.Vlanid)
		}
		var subnet *Subnet
		var vpc *VPC
		var vpcLocalRes []string
		if subnet, err = LoadSubnet(port.SubnetUuid); err != nil {
			G.logger.Error("Cannot load subnet.", zap.Error(err))
			goto UPDATE
		}
		if subnet == nil {
			G.logger.Error("Subnet not found.", zap.String("uuid", port.SubnetUuid))
			goto UPDATE
		}
		if vpc, err = LoadVPC(subnet.VpcUuid); err != nil {
			G.logger.Error("Cannot load VPC.", zap.Error(err))
			goto UPDATE
		}
		if vpc == nil {
			G.logger.Error("VPC not ready.", zap.String("uuid", subnet.VpcUuid))
			goto UPDATE
		}
		if vpcLocalRes, err = vpc.AssignLocalRes(true); err != nil {
			G.logger.Error("cannot get vpc local res", zap.Error(err))
			goto UPDATE
		}
		if !G.config.MasterL3 {
			if err = port.UnBind(subnet, vpc, agent, vpcLocalRes); err != nil {
				G.logger.Error("cannot unbind port", zap.Error(err))
				goto UPDATE
			}
			if err = port.Del(subnet, vpc, agent, vpcLocalRes); err != nil {
				G.logger.Error("cannot delete port", zap.Error(err))
				goto UPDATE
			}
			if err = port.UnAssignLocalRes(G.config.Uuid); err != nil {
				G.logger.Error("cannot release port local res", zap.Error(err))
				goto UPDATE
			}
			if !subnet.HasLocalPort() {
				if err = subnet.Del(vpc, vpcLocalRes); err != nil {
					G.logger.Error("cannot delete subnet", zap.Error(err))
					goto UPDATE
				}
				if err = subnet.UnAssignLocalRes(G.config.Uuid); err != nil {
					G.logger.Error("cannot release subnet local res", zap.Error(err))
					goto UPDATE
				}
			}
			if !vpc.HasLocalSubnet() {
				if err = vpc.Del(); err != nil {
					G.logger.Error("cannot delete vpc", zap.Error(err))
					goto UPDATE
				}
				if err = vpc.UnAssignLocalRes(G.config.Uuid); err != nil {
					G.logger.Error("cannot release vpc local res", zap.Error(err))
					goto UPDATE
				}
			}
		} else if G.config.MasterL3 && port.Floating != nil && port.Floating.ProtoPorts["ip"] != nil {
			if err = port.UnBind(subnet, vpc, agent, vpcLocalRes); err != nil {
				G.logger.Error("cannot unbind port", zap.Error(err))
				goto UPDATE
			}
		}
	}
	success = true
UPDATE:
	for {
		var done bool
		vm.PhaseStop(success)
		reses := []RES{vm}
		if success && hostSG != nil {
			reses = append(reses, hostSG)
			for _, port := range ports {
				delete(hostSG.Ports, port.Uuid)
			}
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
		for _, vlanid := range vlanids {
			NoticeDnsmasq(vlanid)
		}
		NoticeSG()
	}
}

func (vm *VM) ResumingVmTask(movRtn *MoveVmRtn, hostSG *HOSTSGT) {
	var err error
	success := false
	var vlanids []string
	var vmLocalRes []string
	eips, _ := LoadEips()
	agent := LoadAgent(G.config.Uuid)
	if agent == nil {
		G.logger.Error("Cannot load agent.", zap.Error(err))
		goto UPDATE
	}
	if vmLocalRes, err = vm.AssignLocalRes(false); err != nil {
		G.logger.Error("cannot assign local res", zap.Error(err))
		goto UPDATE
	}
	for i, port := range movRtn.ports {
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
		if vpc == nil {
			G.logger.Error("VPC not ready.", zap.String("uuid", subnet.VpcUuid))
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
		port.Nic = vmLocalRes[i*2+1]
		port.Ofport = vmLocalRes[i*2+2]
		port.Vlanid = vpcLocalRes[0]
		port.NID = vpc.NID
		port.Cidr = vpc.Cidr
		if !Contains(vlanids, port.Vlanid) {
			vlanids = append(vlanids, port.Vlanid)
		}
	}
	vm.Vnc = vmLocalRes[0]
	if err = vm.Resume(movRtn.ports, movRtn.vols, movRtn.pool, movRtn.gpus, movRtn.cps, vmLocalRes); err != nil {
		G.logger.Error("cannot add vm", zap.Error(err))
		goto UPDATE
	}
	success = true
UPDATE:
	for {
		if movRtn.pool.PhaseStatus == PhaseStatusPending && movRtn.pool.PhaseType == PhaseTypeAdd {
			movRtn.pool.PhaseStop(success)
		}
		for _, gpu0 := range movRtn.gpu0s {
			gpu0.PhaseStop(success)
			if success {
				gpu0.VmUuid = ""
			}
		}
		for _, gpu := range movRtn.gpus {
			switch gpu.PhaseType {
			case PhaseTypeAttach, PhaseTypeResume:
				gpu.PhaseStop(success)
			}
		}
		eips_changed := false
		for i, port := range movRtn.ports {
			port.PhaseStop(success)
			if success {
				port.AgentIp = agent.ConfigData.AgentIp
				port.AgentPort = agent.ConfigData.AgentPort
				if len(port.Sgs) > 0 {
					if hostSG == nil {
						hostSG = &HOSTSGT{Ports: map[string]string{}, AGENTRES: AGENTRES{AgentUuid: G.config.Uuid, ETCDINFO: ETCDINFO{Type: "hostsg"}}}
					}
					hostSG.Ports[port.Uuid] = vmLocalRes[i*2+2]
				}
				if port.Floating != nil {

					if port.Floating.ProtoPorts["ip"] != nil {
						if eips.EipsInfo[port.Floating.Eip] != nil {
							if eips.EipsInfo[port.Floating.Eip].Mac != G.config.WanNicMac {
								eips.EipsInfo[port.Floating.Eip].Mac = G.config.WanNicMac
								eips_changed = true
							}
						}
					}
				}
			}
		}
		toPut := movRtn.toPut

		if !success {
			for _, vol0 := range movRtn.vol0s {
				vol0.PhaseStop(success)
				toPut = append(toPut, vol0)
			}
			for _, snap0 := range movRtn.snap0s {
				snap0.PhaseStop(success)
				toPut = append(toPut, snap0)
			}
			for _, cp0 := range movRtn.cp0s {
				cp0.PhaseStop(success)
				toPut = append(toPut, cp0)
			}
			if movRtn.vm0 != nil {
				movRtn.vm0.PhaseStop(success)
				toPut = append(toPut, movRtn.vm0)
			}
		} else {
			if movRtn.vm0 != nil {
				if movRtn.vm0.PHASEINFO.PhaseType == PhaseTypeMigrate {
					movRtn.vm0.PhaseStop(success)
				}
			}
		}
		for _, vol := range movRtn.vols {
			vol.PhaseStop(success)
			if success && vol.Root {
				for _, img0 := range movRtn.img0s {
					if vol.ImgUuid == img0.Uuid {
						img0.Vols = Remove(img0.Vols, vol.Uuid)
					}
				}
			}
		}
		for _, img := range movRtn.imgs {
			img.PhaseStop(success)
		}
		for _, snap := range movRtn.snaps {
			snap.PhaseStop(success)
		}
		for _, cp := range movRtn.cps {
			cp.PhaseStop(success)
		}
		vm.PhaseStop(success)
		if hostSG != nil {
			toPut = append(toPut, hostSG)
		}
		if eips_changed {
			toPut = append(toPut, eips)
		}
		var done bool
		if success {
			done, err = Save(movRtn.toDel, toPut)
		} else {
			done, err = Save(nil, toPut)
		}
		if err != nil {
			G.logger.Error("cannot save reses", zap.Error(err))
			return
		}
		if done {
			break
		}
	}
	if success {
		for _, vlanid := range vlanids {
			NoticeDnsmasq(vlanid)
		}
		NoticeSG()
	}
}

func (vm *VM) ModifyingVmTask() {
	var err error
	success := false
	if err = vm.Modify(); err != nil {
		G.logger.Error("cannot modify vm", zap.Error(err))
		goto UPDATE
	}
	success = true
UPDATE:
	if _, err = UpdatePhaseStop(vm, success); err != nil {
		G.logger.Error("cannot update vm phase", zap.Error(err))
	}
}

func (vm *VM) VerifyUuid() error {
	return AssignUuid("vm", vm)
}

func (vm *VM) VerifyVcpu() error {
	if vm.Vcpu < 1 || vm.Vcpu > 256 {
		return fmt.Errorf("vcpu invalid")
	}
	return nil
}

func (vm *VM) VerifyMem() error {
	if vm.Mem < 1 || vm.Mem > 1650 {
		return fmt.Errorf("mem invalid")
	}
	return nil
}

func (vm *VM) VerifyBootdev() error {
	if vm.Bootdev != "hd" && vm.Bootdev != "cdrom" && vm.Bootdev != "network" {
		return fmt.Errorf("bootdev invalid")
	}
	return nil
}

func (vm *VM) AssignLocalRes(retrieveOnly bool) ([]string, error) {

	if _, ok := vm.LocalRes[vm.AgentUuid]; ok {
		return vm.LocalRes[vm.AgentUuid], nil
	}

	if retrieveOnly {
		return nil, fmt.Errorf("no local res")
	}
	vncs, _ := LoadVncs()
	nics, _ := LoadNics()
	ofports, _ := LoadOfports()
	for {

		var vnc string
		for {
			vnc = GenVncPort()
			if vncs.VncsInfo[vm.AgentUuid] == nil {
				vncs.VncsInfo[vm.AgentUuid] = make(map[string]string)
			}
			if _, ok := vncs.VncsInfo[vm.AgentUuid][vnc]; !ok {
				vncs.VncsInfo[vm.AgentUuid][vnc] = vm.Uuid
				break
			}
		}
		vm.LocalRes[vm.AgentUuid] = []string{vnc}
		for _, portUuid := range vm.Ports {
			var nic string
			var ofport string
			for {
				nic = GenNic()
				if nics.NicsInfo[vm.AgentUuid] == nil {
					nics.NicsInfo[vm.AgentUuid] = make(map[string]string)
				}
				if _, ok := nics.NicsInfo[vm.AgentUuid][nic]; !ok {
					nics.NicsInfo[vm.AgentUuid][nic] = portUuid
					break
				}
			}
			for {
				o := rand.Uint32N(65299-10) + 10
				ofport = fmt.Sprintf("%d", o)
				if ofports.OfportsInfo[vm.AgentUuid] == nil {
					ofports.OfportsInfo[vm.AgentUuid] = make(map[string]string)
				}
				if _, ok := ofports.OfportsInfo[vm.AgentUuid][ofport]; !ok {
					ofports.OfportsInfo[vm.AgentUuid][ofport] = portUuid
					break
				}
			}
			vm.LocalRes[vm.AgentUuid] = append(vm.LocalRes[vm.AgentUuid], nic, ofport)
		}
		var done bool
		var err error
		done, err = Save(nil, []RES{vm, vncs, nics, ofports})
		if err != nil {
			return nil, err
		}
		if done {
			break
		}
	}
	return vm.LocalRes[vm.AgentUuid], nil
}

func (vm *VM) UnAssignLocalRes() (err error) {
	if _, ok := vm.LocalRes[vm.AgentUuid]; !ok {
		return nil
	}
	vncs, _ := LoadVncs()
	nics, _ := LoadNics()
	ofports, _ := LoadOfports()
	for {
		vnc := vm.LocalRes[vm.AgentUuid][0]
		for i := 1; i < len(vm.LocalRes[vm.AgentUuid]); i += 2 {
			nic := vm.LocalRes[vm.AgentUuid][i]
			ofport := vm.LocalRes[vm.AgentUuid][i+1]
			delete(nics.NicsInfo[vm.AgentUuid], nic)
			delete(ofports.OfportsInfo[vm.AgentUuid], ofport)
		}
		delete(vncs.VncsInfo[vm.AgentUuid], vnc)
		delete(vm.LocalRes, vm.AgentUuid)
		var done bool
		var err error
		done, err = Save(nil, []RES{vm, nics, ofports, vncs})
		if err != nil {
			return err
		}
		if done {
			break
		}
	}
	return nil
}

func (vm *VM) GetNics() (nics []map[string]string, err error) {
	nics = make([]map[string]string, 0)
	for idx, portUuid := range vm.Ports {
		var port *Port
		if port, err = LoadPort(portUuid); err != nil {
			return nil, err
		}
		if port == nil {
			return nil, fmt.Errorf("port not found")
		}
		ip := net.ParseIP(port.Mask).To4()
		mask := net.IPMask(ip)
		prefixlen, _ := mask.Size()
		nic := make(map[string]string)
		nic["IDX"] = fmt.Sprintf("%d", idx)
		nic["IP"] = port.Ip
		if port.Vip {
			nic["VIP"] = "true"
		} else {
			nic["VIP"] = "false"
		}
		nic["MASK"] = port.Mask
		nic["GATEWAY"] = port.Gateway
		nic["DNS"] = port.Dns
		nic["MAC"] = port.Mac
		nic["NIC"] = port.Nic
		nic["PREFIX"] = fmt.Sprintf("%d", prefixlen)
		if port.Static {
			nic["STATIC"] = "true"
		} else {
			nic["STATIC"] = "false"
		}
		nics = append(nics, nic)
	}
	return nics, nil
}

func (vm *VM) GetCloudInitParas() (paras map[string]string) {
	paras = make(map[string]string)
	paras["INSTANCE_ID"] = vm.CloudInitInfo.InstanceId
	paras["OS"] = vm.CloudInitInfo.Os
	paras["HOSTNAME"] = vm.CloudInitInfo.Hostname
	paras["USERNAME"] = vm.CloudInitInfo.Username
	paras["PASSWORD"] = vm.CloudInitInfo.Password
	paras["SSH_PUB_KEY"] = vm.CloudInitInfo.Pubkey
	return paras
}

func (vm *VM) MovePool(rests RESTS, rtn *MoveVmRtn) (err error) {
	var pool0 *Pool
	if pool0, err = LoadPool2(vm.PoolUuid, vm.AgentUuid); err != nil {
		return err
	}
	if pool0 == nil {
		return fmt.Errorf("pool not found")
	}
	if pool0.Stype != "fs" {
		return fmt.Errorf("pool %s is not fs", pool0.Uuid)
	}
	rtn.pool0 = pool0
	rtn.toPut = append(rtn.toPut, pool0)
	var pool *Pool
	if pool, err = LoadPool3(rests, pool0.Sid); err != nil {
		return err
	}

	if pool == nil {
		if exist, _ := DirExists(pool0.ParaMap["DIR"]); !exist {
			return fmt.Errorf("pool dir not found")
		}
		pool = &Pool{
			AGENTREST: AGENTREST{AgentUuid: G.config.Uuid, ETCDINFO: ETCDINFO{Type: "pool"}},
			Sid:       pool0.Sid,
			Stype:     pool0.Stype,
			Paras:     pool0.Paras,
			ParaMap:   pool0.ParaMap,
			Vols:      []string{},
			Imgs:      make(map[string]string)}

		if err = pool.PhaseStart(PhaseTypeAdd); err != nil {
			return err
		}
		if err = pool.VerifyUuid(); err != nil {
			return fmt.Errorf("cannot verify pool uuid")
		}
	} else {

		if pool.Stype != "fs" {
			return fmt.Errorf("pool %s is not fs", pool.Uuid)
		}
		if pool.ParaMap["DIR"] != pool0.ParaMap["DIR"] {
			return fmt.Errorf("pool dir not match")
		}
	}
	rtn.pool = pool
	rtn.toPut = append(rtn.toPut, pool)
	return nil
}

func (vm *VM) MoveGPU(rests RESTS, rtn *MoveVmRtn) (err error) {
	for _, gpuUuid := range vm.Gpus {
		var gpu *GPU

		var gpu0 *GPU
		if gpu0, err = LoadGPU2(gpuUuid, vm.AgentUuid); err != nil {
			return err
		}
		if gpu0 == nil {
			return fmt.Errorf("old GPU not found")
		}

		if err = gpu0.PhaseStart(PhaseTypeDetach); err != nil {
			return err
		}
		rtn.gpu0s = append(rtn.gpu0s, gpu0)
		rtn.toPut = append(rtn.toPut, gpu0)

		if gpu, err = LoadGPU3(rests, gpu0.DeviceId); err != nil {
			return err
		}
		if gpu == nil {
			return fmt.Errorf("new GPU not found")
		}

		if err = gpu.PhaseStart(PhaseTypeAttach); err != nil {
			return err
		}
		gpu.VmUuid = vm.Uuid
		rtn.gpus = append(rtn.gpus, gpu)
		rtn.toPut = append(rtn.toPut, gpu)
	}
	return nil
}

func (vm *VM) MoveCP(rests RESTS, rtn *MoveVmRtn) (err error) {
	for _, cpUuid := range vm.Cps {
		var cp0 *CP
		if cp0, err = LoadCP2(cpUuid, vm.AgentUuid); err != nil {
			return err
		}
		if cp0 == nil {
			return fmt.Errorf("checkpoint not found")
		}
		cp := &CP{}
		*cp = *cp0
		if err = cp0.PhaseStart(PhaseTypeDel); err != nil {
			return err
		}
		rtn.cp0s = append(rtn.cp0s, cp0)
		rtn.toDel = append(rtn.toDel, cp0)
		cp.AgentUuid = G.config.Uuid
		cp.PoolUuid = rtn.pool.Uuid
		cp.Modrev = 0
		if err = cp.PhaseStart(PhaseTypeResume); err != nil {
			return err
		}
		rtn.cps = append(rtn.cps, cp)
		rtn.toPut = append(rtn.toPut, cp)
	}
	return nil
}

func (vm *VM) MovePort(rests RESTS, rtn *MoveVmRtn) (err error) {
	hostsg, _ := LoadHostSG(vm.AgentUuid)
	for _, portUuid := range vm.Ports {
		var port *Port
		if port, err = LoadPort(portUuid); err != nil {
			return err
		}
		if port == nil {
			return fmt.Errorf("port not found")
		}
		delete(hostsg.Ports, port.Uuid)
		delete(rests["port"], port.GetKey())
		port.AgentUuid = G.config.Uuid
		if err = port.PhaseStart(PhaseTypeResume); err != nil {
			return err
		}
		rtn.ports = append(rtn.ports, port)
		rtn.toPut = append(rtn.toPut, port)
	}
	rtn.toPut = append(rtn.toPut, hostsg)
	return nil
}

func (vm0 *VM) MoveVM(rests RESTS, rtn *MoveVmRtn) (err error) {
	suspended := vm0.IsSuspend()
	vm := &VM{}
	*vm = *vm0
	rtn.vm0 = vm0
	if suspended {
		if err = vm0.PhaseStart(PhaseTypeDel); err != nil {
			return err
		}
		rtn.toDel = append(rtn.toDel, vm0)
	} else {
		if err = vm0.PhaseStart(PhaseTypeMigrate); err != nil {
			return err
		}
		rtn.toPut = append(rtn.toPut, vm0)
	}
	vm.AgentUuid = G.config.Uuid
	vm.PoolUuid = rtn.pool.Uuid
	vm.Modrev = 0
	if err = vm.PhaseStart(PhaseTypeResume); err != nil {
		return err
	}
	vm.Gpus = []string{}
	for _, gpu := range rtn.gpus {
		vm.Gpus = append(vm.Gpus, gpu.Uuid)
	}
	rtn.vm = vm
	rtn.toPut = append(rtn.toPut, vm)
	return nil
}

func (vm *VM) MoveVol(rests RESTS, rtn *MoveVmRtn) (err error) {
	img2del := map[string]*Img{}
	img2put := map[string]*Img{}
	for _, volUuid := range vm.Vols {
		var vol0 *Vol

		if vol0, err = LoadVol2(volUuid, vm.AgentUuid); err != nil {
			return err
		}
		if vol0 == nil {
			return fmt.Errorf("volume not found")
		}

		if ok, _ := FileExists(rtn.pool.ParaMap["DIR"] + "/volumes/" + volUuid + ".qcow2"); !ok {
			return fmt.Errorf("volume file not found")
		}
		vol := &Vol{}
		*vol = *vol0
		if err = vol0.PhaseStart(PhaseTypeDel); err != nil {
			return err
		}
		rtn.vol0s = append(rtn.vol0s, vol0)
		rtn.toDel = append(rtn.toDel, vol0)
		rtn.pool0.Vols = Remove(rtn.pool0.Vols, vol0.Uuid)
		vol.AgentUuid = G.config.Uuid
		vol.PoolUuid = rtn.pool.Uuid
		vol.Modrev = 0
		if err = vol.PhaseStart(PhaseTypeResume); err != nil {
			return err
		}
		rtn.vols = append(rtn.vols, vol)
		rtn.toPut = append(rtn.toPut, vol)
		rtn.pool.Vols = append(rtn.pool.Vols, vol.Uuid)
		if vol0.ImgUuid != "" {

			var img0 *Img

			if img2del[vol0.ImgUuid] == nil {
				if img0, err = LoadImg2(vol0.ImgUuid, vol0.AgentUuid); err != nil {
					return err
				}
				if img0 == nil {
					return fmt.Errorf("image not found")
				}
				rtn.img0s = append(rtn.img0s, img0)
				rtn.toPut = append(rtn.toPut, img0)
				img2del[img0.Uuid] = img0
			} else {
				img0 = img2del[vol0.ImgUuid]
			}
			img0.Vols = Remove(img0.Vols, vol0.Uuid)

			var img *Img
			if img, err = LoadImg3(rests, img0.Name); err != nil {
				return err
			}

			if img == nil {
				for _, _img := range img2put {
					if _img.Name == img0.Name {
						img = _img
						break
					}
				}
			}
			if img == nil {
				if ok, _ := FileExists(rtn.pool.ParaMap["DIR"] + "/backing/" + img0.Name + ".qcow2"); !ok {
					return fmt.Errorf("img file not found")
				}
				img = &Img{
					Name:      img0.Name,
					Desc:      img0.Desc,
					Size:      img0.Size,
					AGENTREST: AGENTREST{AgentUuid: G.config.Uuid, ETCDINFO: ETCDINFO{Type: "img"}},
					PoolUuid:  rtn.pool.Uuid,
					Vols:      []string{}}
				if err = img.PhaseStart(PhaseTypeAdd); err != nil {
					return err
				}
				if err = img.VerifyUuid(); err != nil {
					return fmt.Errorf("cannot verify img uuid")
				}
			}
			img.Vols = append(img.Vols, vol.Uuid)
			vol.ImgUuid = img.Uuid
			if img2put[img.Uuid] == nil {
				rtn.imgs = append(rtn.imgs, img)
				rtn.toPut = append(rtn.toPut, img)
				rtn.pool.Imgs[img.Uuid] = img.Name
				img2put[img.Uuid] = img
			}
		}

		for _, snapUuid := range vol0.Snaps {
			var snap0 *Snap
			if snap0, err = LoadSnap2(snapUuid, vol0.AgentUuid); err != nil {
				return err
			}
			if snap0 == nil {
				return fmt.Errorf("snapshot not found")
			}
			snap := &Snap{}
			*snap = *snap0
			if err = snap0.PhaseStart(PhaseTypeDel); err != nil {
				return err
			}
			rtn.snap0s = append(rtn.snap0s, snap0)
			rtn.toDel = append(rtn.toDel, snap0)
			snap.AgentUuid = G.config.Uuid
			snap.PoolUuid = rtn.pool.Uuid
			snap.Modrev = 0
			if err = snap.PhaseStart(PhaseTypeResume); err != nil {
				return err
			}
			rtn.snaps = append(rtn.snaps, snap)
			rtn.toPut = append(rtn.toPut, snap)
		}
	}
	return nil
}
func (vm *VM) Move(rests RESTS) (rtn *MoveVmRtn, err error) {
	rtn = &MoveVmRtn{
		toDel:  []RES{},
		toPut:  []RES{},
		ports:  []*Port{},
		vols:   []*Vol{},
		vol0s:  []*Vol{},
		gpus:   []*GPU{},
		gpu0s:  []*GPU{},
		snaps:  []*Snap{},
		snap0s: []*Snap{},
		imgs:   []*Img{},
		img0s:  []*Img{},
		cps:    []*CP{},
		cp0s:   []*CP{}}
	if vm.AgentUuid != G.config.Uuid {
		if err = vm.MovePool(rests, rtn); err != nil {
			return nil, err
		}
		if err = vm.MoveGPU(rests, rtn); err != nil {
			return nil, err
		}
		if err = vm.MoveVol(rests, rtn); err != nil {
			return nil, err
		}
		if err = vm.MoveCP(rests, rtn); err != nil {
			return nil, err
		}
		if err = vm.MovePort(rests, rtn); err != nil {
			return nil, err
		}
		if err = vm.MoveVM(rests, rtn); err != nil {
			return nil, err
		}
		return rtn, nil
	}
	if err = vm.LocalMove(rests, rtn); err != nil {
		return nil, err
	}
	return rtn, nil
}

func (vm *VM) LocalMove(rests RESTS, rtn *MoveVmRtn) (err error) {
	var pool *Pool
	if pool, err = LoadPool(vm.PoolUuid); err != nil {
		return err
	}
	rtn.pool = pool
	for _, gpuUuid := range vm.Gpus {
		var gpu *GPU
		if gpu, err = LoadGPU(gpuUuid); err != nil {
			return err
		}
		if gpu == nil {
			return fmt.Errorf("GPU not found")
		}
		if err = gpu.PhaseStart(PhaseTypeResume); err != nil {
			return err
		}
		rtn.gpus = append(rtn.gpus, gpu)
		rtn.toPut = append(rtn.toPut, gpu)
	}
	imgs := map[string]struct{}{}
	for _, volUuid := range vm.Vols {
		var vol *Vol
		if vol, err = LoadVol(volUuid); err != nil {
			return err
		}
		if vol == nil {
			return fmt.Errorf("volume not found")
		}
		if err = vol.PhaseStart(PhaseTypeResume); err != nil {
			return err
		}
		rtn.vols = append(rtn.vols, vol)
		rtn.toPut = append(rtn.toPut, vol)
		if vol.ImgUuid != "" {
			if _, ok := imgs[vol.ImgUuid]; !ok {
				var img *Img
				if img, err = LoadImg(vol.ImgUuid); err != nil {
					return err
				}
				if err = img.PhaseStart(PhaseTypeResume); err != nil {
					return err
				}
				rtn.imgs = append(rtn.imgs, img)
				rtn.toPut = append(rtn.toPut, img)
				imgs[vol.ImgUuid] = struct{}{}
			}
		}
		for _, snapUuid := range vol.Snaps {
			var snap *Snap
			if snap, err = LoadSnap(snapUuid); err != nil {
				return err
			}
			if snap == nil {
				return fmt.Errorf("snapshot not found")
			}
			if err = snap.PhaseStart(PhaseTypeResume); err != nil {
				return err
			}
			rtn.snaps = append(rtn.snaps, snap)
			rtn.toPut = append(rtn.toPut, snap)
		}
	}
	for _, cpUuid := range vm.Cps {
		var cp0 *CP
		if cp0, err = LoadCP(cpUuid); err != nil {
			return err
		}
		if cp0 == nil {
			return fmt.Errorf("checkpoint not found")
		}
		if err = cp0.PhaseStart(PhaseTypeResume); err != nil {
			return err
		}
		rtn.cps = append(rtn.cps, cp0)
		rtn.toPut = append(rtn.toPut, cp0)
	}
	for _, portUuid := range vm.Ports {
		var port *Port
		if port, err = LoadPort(portUuid); err != nil {
			return err
		}
		if port == nil {
			return fmt.Errorf("port not found")
		}
		if err = port.PhaseStart(PhaseTypeResume); err != nil {
			return err
		}
		rtn.ports = append(rtn.ports, port)
		rtn.toPut = append(rtn.toPut, port)
	}
	if err = vm.PhaseStart(PhaseTypeResume); err != nil {
		return err
	}
	rtn.vm = vm
	rtn.toPut = append(rtn.toPut, vm)
	return nil
}

func (vm *VM) AllVolsLocked(rests RESTS) (locked bool, err error) {
	pool, err := LoadPool2(vm.PoolUuid, vm.AgentUuid)
	if err != nil {
		return false, err
	}
	if pool == nil {
		return false, fmt.Errorf("pool not found")
	}
	if pool.Stype != "fs" {
		return false, fmt.Errorf("pool %s is not fs", pool.Uuid)
	}
	for _, volUuid := range vm.Vols {
		volpath := pool.ParaMap["DIR"] + "/volumes/" + volUuid + ".qcow2"
		locked := isFileLocked(volpath)
		if locked {
			return true, nil
		}
	}
	return false, nil
}

func VmCmdParser(command *cobra.Command) {
	var err error
	var vmCmd = &cobra.Command{
		Use:   "vm",
		Short: "VM management",
		Run: func(cmd *cobra.Command, args []string) {
			fmt.Println("please specify a subcommand for vm management. use -h for help.")
		},
	}
	var vmAddCmd = &cobra.Command{
		Use:   "add",
		Short: "Add a VM",
		Run:   VmAddHandle,
	}
	vmAddCmd.Flags().Uint32Var(&vmFlags.vcpu, "vcpu", 0, "set the vcpu number of the VM")
	if err = vmAddCmd.MarkFlagRequired("vcpu"); err != nil {
		panic(err)
	}
	vmAddCmd.Flags().Uint32Var(&vmFlags.mem, "mem", 0, "set the memory size of the VM")
	if err = vmAddCmd.MarkFlagRequired("mem"); err != nil {
		panic(err)
	}
	vmAddCmd.Flags().StringSliceVar(&vmFlags.port, "port", nil, "set the ports of the VM")
	if err = vmAddCmd.MarkFlagRequired("port"); err != nil {
		panic(err)
	}
	vmAddCmd.Flags().StringSliceVar(&vmFlags.vol, "vol", nil, "set the volumes of the VM")
	if err = vmAddCmd.MarkFlagRequired("vol"); err != nil {
		panic(err)
	}
	vmAddCmd.Flags().StringSliceVar(&vmFlags.gpu, "gpu", nil, "set the GPU of the VM")
	var vmGetCmd = &cobra.Command{
		Use:   "get",
		Short: "Get a VM",
		Run:   VmGetHandle,
	}
	vmGetCmd.Flags().StringVarP(&vmFlags.uuid, "uuid", "U", "", "set the UUID of the VM")
	if err = vmGetCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var vmDelCmd = &cobra.Command{
		Use:   "del",
		Short: "Delete a VM",
		Run:   VmDelHandle,
	}
	vmDelCmd.Flags().StringVarP(&vmFlags.uuid, "uuid", "U", "", "set the UUID of the VM")
	if err = vmDelCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var vmListCmd = &cobra.Command{
		Use:   "list",
		Short: "List all VMs",
		Run:   VmListHandle,
	}
	vmListCmd.Flags().BoolVar(&vmFlags.abnormal, "abnormal", false, "list abnormal VMs")
	var vmPoweronCmd = &cobra.Command{
		Use:   "poweron",
		Short: "Power on a VM",
		Run:   VmPoweronHandle,
	}
	vmPoweronCmd.Flags().StringVarP(&vmFlags.uuid, "uuid", "U", "", "set the UUID of the VM")
	if err = vmPoweronCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var vmPoweroffCmd = &cobra.Command{
		Use:   "poweroff",
		Short: "Power off a VM",
		Run:   VmPoweroffHandle,
	}
	vmPoweroffCmd.Flags().BoolVar(&vmFlags.force, "force", false, "poweroff the VM forcefully")
	vmPoweroffCmd.Flags().StringVarP(&vmFlags.uuid, "uuid", "U", "", "set the UUID of the VM")
	if err = vmPoweroffCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var vmModifyCmd = &cobra.Command{
		Use:   "modify",
		Short: "Modify a VM",
		Run:   VmModifyHandle,
	}
	vmModifyCmd.Flags().StringVarP(&vmFlags.uuid, "uuid", "U", "", "set the UUID of the VM")
	if err = vmModifyCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	vmModifyCmd.Flags().Uint32Var(&vmFlags.vcpu, "vcpu", 0, "set the vcpu number of the VM")
	vmModifyCmd.Flags().Uint32Var(&vmFlags.mem, "mem", 0, "set the memory size of the VM")
	vmModifyCmd.Flags().StringVar(&vmFlags.bootdev, "bootdev", "", "set the boot dev of the VM")
	var vmInjectCmd = &cobra.Command{
		Use:   "inject",
		Short: "Inject a cloud-init cdrom to a VM",
		Run:   VmInjectHandle,
	}
	vmInjectCmd.Flags().StringVarP(&vmFlags.uuid, "uuid", "U", "", "set the UUID of the VM")
	if err = vmInjectCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	vmInjectCmd.Flags().StringVar(&vmFlags.os, "os", "linux", "set the OS of the VM")
	vmInjectCmd.Flags().StringVar(&vmFlags.hostname, "hostname", "", "set the hostname of the VM")
	if err = vmInjectCmd.MarkFlagRequired("hostname"); err != nil {
		panic(err)
	}
	vmInjectCmd.Flags().StringVar(&vmFlags.username, "username", "", "set the username of the VM")
	vmInjectCmd.Flags().StringVar(&vmFlags.password, "password", "", "set the password of the VM")
	vmInjectCmd.Flags().StringVar(&vmFlags.pubkey, "pubkey", "", "set the public key for accessing the VM")
	var vmEjectCmd = &cobra.Command{
		Use:   "eject",
		Short: "Eject the cdrom of a VM",
		Run:   VmEjectHandle,
	}
	vmEjectCmd.Flags().StringVarP(&vmFlags.uuid, "uuid", "U", "", "set the UUID of the VM")
	if err = vmEjectCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var vmInject2Cmd = &cobra.Command{
		Use:   "inject2",
		Short: "Inject a NBD dev to a VM",
		Run:   VmInject2Handle,
	}
	vmInject2Cmd.Flags().StringVarP(&vmFlags.uuid, "uuid", "U", "", "set the UUID of the VM")
	if err = vmInject2Cmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	vmInject2Cmd.Flags().StringVar(&vmFlags.nbddev, "nbddev", "", "set the NBD device to inject to the VM")
	if err = vmInject2Cmd.MarkFlagRequired("nbddev"); err != nil {
		panic(err)
	}
	var vmEject2Cmd = &cobra.Command{
		Use:   "eject2",
		Short: "Eject the nbddev of a VM",
		Run:   VmEject2Handle,
	}
	vmEject2Cmd.Flags().StringVarP(&vmFlags.uuid, "uuid", "U", "", "set the UUID of the VM")
	if err = vmEject2Cmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	vmEject2Cmd.Flags().StringVar(&vmFlags.nbddev, "nbddev", "", "set the NBD device to inject to the VM")
	if err = vmEject2Cmd.MarkFlagRequired("nbddev"); err != nil {
		panic(err)
	}

	var vmSuspendCmd = &cobra.Command{
		Use:   "suspend",
		Short: "Suspend a VM",
		Run:   VmSuspendHandle,
	}
	vmSuspendCmd.Flags().StringVarP(&vmFlags.uuid, "uuid", "U", "", "set the UUID of the VM")
	if err = vmSuspendCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var vmResumeCmd = &cobra.Command{
		Use:   "resume",
		Short: "Resume a VM",
		Run:   VmResumeHandle,
	}
	vmResumeCmd.Flags().StringVarP(&vmFlags.uuid, "uuid", "U", "", "set the UUID of the VM")
	if err = vmResumeCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	vmCmd.AddCommand(vmAddCmd, vmGetCmd, vmDelCmd, vmListCmd, vmPoweronCmd, vmPoweroffCmd, vmModifyCmd, vmInjectCmd, vmEjectCmd, vmInject2Cmd, vmEject2Cmd, vmSuspendCmd, vmResumeCmd)
	command.AddCommand(vmCmd)
}

func VmAddHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/vms", G.Host, G.Port)
	data := map[string]interface{}{"vcpu": vmFlags.vcpu, "mem": vmFlags.mem, "ports": vmFlags.port, "vols": vmFlags.vol, "gpus": vmFlags.gpu}
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

func VmGetHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/vms/%s", G.Host, G.Port, vmFlags.uuid)
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

func VmDelHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/vms/%s", G.Host, G.Port, vmFlags.uuid)
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

func VmListHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/vms", G.Host, G.Port)
	data := map[string]interface{}{}
	if vmFlags.abnormal {
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

func VmPoweronHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/vms/%s/poweron", G.Host, G.Port, vmFlags.uuid)
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

func VmPoweroffHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/vms/%s/poweroff", G.Host, G.Port, vmFlags.uuid)
	data := map[string]interface{}{}
	if vmFlags.force {
		data["force"] = true
	}
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

func VmModifyHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/vms/%s/modify", G.Host, G.Port, vmFlags.uuid)
	data := map[string]interface{}{"vcpu": vmFlags.vcpu, "mem": vmFlags.mem, "bootdev": vmFlags.bootdev}
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

func VmInjectHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/vms/%s/inject", G.Host, G.Port, vmFlags.uuid)
	data := map[string]interface{}{"os": vmFlags.os, "hostname": vmFlags.hostname, "username": vmFlags.username, "password": vmFlags.password, "pubkey": vmFlags.pubkey}
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

func VmInject2Handle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/vms/%s/inject2", G.Host, G.Port, vmFlags.uuid)
	data := map[string]interface{}{"nbddev": vmFlags.nbddev}
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

func VmEjectHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/vms/%s/eject", G.Host, G.Port, vmFlags.uuid)
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

func VmEject2Handle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/vms/%s/eject2", G.Host, G.Port, vmFlags.uuid)
	data := map[string]interface{}{"nbddev": vmFlags.nbddev}
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

func VmSuspendHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/vms/%s/suspend", G.Host, G.Port, vmFlags.uuid)
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

func VmResumeHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/vms/%s/resume", G.Host, G.Port, vmFlags.uuid)
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

func VmAppSetup() {
	G.echoServer.POST("/v1/vms", AppVmAdd)
	G.echoServer.GET("/v1/vms", AppVmList)
	G.echoServer.GET("/v1/vms/:uuid", AppVmGet)
	G.echoServer.DELETE("/v1/vms/:uuid", AppVmDel)
	G.echoServer.PUT("/v1/vms/:uuid/poweron", AppVmPoweron)
	G.echoServer.PUT("/v1/vms/:uuid/poweroff", AppVmPoweroff)
	G.echoServer.PUT("/v1/vms/:uuid/inject", AppVmInject)
	G.echoServer.PUT("/v1/vms/:uuid/inject2", AppVmInject2)
	G.echoServer.PUT("/v1/vms/:uuid/eject", AppVmEject)
	G.echoServer.PUT("/v1/vms/:uuid/eject2", AppVmEject2)
	G.echoServer.POST("/v1/phonehome/:instance_id", AppVmPhonehome)
	G.echoServer.PUT("/v1/vms/:uuid/suspend", AppVmSuspend)
	G.echoServer.PUT("/v1/vms/:uuid/resume", AppVmResume)
	G.echoServer.PUT("/v1/vms/:uuid/modify", AppVmModify)
}

func AppVmAdd(c echo.Context) (err error) {
	G.logger.Debug("=========AppVmAdd==========")
	q := VmAddReq{}
	if err = c.Bind(&q); err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Cannot parse the request."))
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
	var vols []*Vol

	var pool *Pool
	for i, volUuid := range q.Vols {
		var vol *Vol
		if vol, err = LoadVol(volUuid); err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if vol == nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Volume not found."))
		}
		if pool == nil {
			pool, err = LoadPool(vol.PoolUuid)
			if err != nil {
				return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
			}
			if pool == nil {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Pool not found."))
			}
		}
		if i == 0 && !vol.Root {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("The first volume must be a root volume."))
		}
		vols = append(vols, vol)
	}
	if len(vols) == 0 {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("No volume specified."))
	}
	var gpus []*GPU
	for _, gpuUuid := range q.Gpus {
		var gpu *GPU
		if gpu, err = LoadGPU(gpuUuid); err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if gpu == nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("GPU not found."))
		}
		if gpu.VmUuid != "" {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("gpu has been used"))
		}
		gpus = append(gpus, gpu)
	}
	var vm *VM
	for {
		vm = &VM{AGENTREST: AGENTREST{AgentUuid: G.config.Uuid, ETCDINFO: ETCDINFO{Type: "vm"}},
			Vcpu:          q.Vcpu,
			Mem:           q.Mem,
			Ports:         q.Ports,
			Vols:          q.Vols,
			Gpus:          q.Gpus,
			Cps:           []string{},
			PoolUuid:      pool.Uuid,
			CloudInitInfo: &CloudInitInfoT{},
			LocalRes:      map[string][]string{}}
		_ = vm.PhaseStart(PhaseTypeAdd)
		if err = vm.VerifyUuid(); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		if err = vm.VerifyVcpu(); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		if err = vm.VerifyMem(); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		reses := []RES{vm, hostsg}
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
			port.ByUuid = vm.Uuid
			reses = append(reses, port)
		}
		for _, vol := range vols {
			if vol.NotReady() {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Volume is not ready."))
			}
			if vol.VmUuid != "" {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Volume is in use."))
			}
			if pool.Uuid != vol.PoolUuid {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp("All volumes must be in the same pool."))
			}
			if err = vol.PhaseStart(PhaseTypeAttach); err != nil {
				return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
			}
			vol.VmUuid = vm.Uuid
			reses = append(reses, vol)
		}
		for _, gpu := range gpus {
			if gpu.NotReady() {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp("gpu is not ready"))
			}
			if gpu.VmUuid != "" {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp("gpu has been used"))
			}
			if err = gpu.PhaseStart(PhaseTypeAttach); err != nil {
				return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
			}
			gpu.VmUuid = vm.Uuid
			reses = append(reses, gpu)
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
	go vm.AddingVmTask(ports, vols, pool, gpus, hostsg)
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(vm.Uuid))
}

func AppVmList(c echo.Context) (err error) {
	G.logger.Debug("=========AppVmList==========")
	q := AppListReq{}
	if err = c.Bind(&q); err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
	}
	rests := LoadRests("vm")
	vmUuids := []string{}
	for _, rest := range rests["vm"] {
		vm, ok := rest.(*VM)
		if !ok {
			continue
		}
		if vm.AgentUuid != G.config.Uuid {
			continue
		}
		if q.Abnormal {
			if vm.NotReady() {
				vmUuids = append(vmUuids, vm.Uuid)
			}
		} else {
			vmUuids = append(vmUuids, vm.Uuid)
		}
	}
	jsonMap := make(map[string]interface{})
	jsonMap["status"] = "ok"
	jsonMap["vm_count"] = len(vmUuids)
	jsonMap["vms"] = vmUuids
	return c.JSON(http.StatusOK, jsonMap)
}

func AppVmGet(c echo.Context) (err error) {
	G.logger.Debug("=========AppVmGet==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine UUID is required."))
	}
	var vm *VM
	if vm, err = LoadVM(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if vm == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Virtual machine not found."))
	}
	rsp := VmGetRsp{
		Status:        "ok",
		Vcpu:          vm.Vcpu,
		Mem:           vm.Mem,
		Vnc:           vm.Vnc,
		Ports:         vm.Ports,
		Vols:          vm.Vols,
		Gpus:          vm.Gpus,
		Power:         vm.Power,
		Cps:           vm.Cps,
		PoolUuid:      vm.PoolUuid,
		AgentUuid:     vm.AgentUuid,
		CloudInitInfo: vm.CloudInitInfo,
		AGENTREST:     vm.AGENTREST,
		PHASEINFO:     vm.PHASEINFO}
	return c.JSON(http.StatusOK, &rsp)
}

func AppVmDel(c echo.Context) (err error) {
	G.logger.Debug("=========AppVmDel==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine UUID is required."))
	}
	var vm *VM
	if vm, err = LoadVM(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if vm == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Virtual machine not found."))
	}
	var vmLocalRes []string
	vmLocalRes, _ = vm.AssignLocalRes(true)
	var ports []*Port
	for i, portUuid := range vm.Ports {
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
		if vmLocalRes != nil {
			port.Nic = vmLocalRes[i*2+1]
			port.Ofport = vmLocalRes[i*2+2]
		}
		if vpcLocalRes != nil {
			port.Vlanid = vpcLocalRes[0]
			port.NID = vpc.NID
		}
		port.Cidr = vpc.Cidr
		ports = append(ports, port)
	}
	var pool *Pool
	if pool, err = LoadPool(vm.PoolUuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if pool == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Pool not found."))
	}
	var vols []*Vol
	for _, volUuid := range vm.Vols {
		var vol *Vol
		if vol, err = LoadVol(volUuid); err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if vol != nil {
			vols = append(vols, vol)
		}
	}
	var gpus []*GPU
	for _, gpuUuid := range vm.Gpus {
		var gpu *GPU
		if gpu, err = LoadGPU(gpuUuid); err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if gpu != nil {
			gpus = append(gpus, gpu)
		}
	}
	for {
		if err = vm.PhaseStart(PhaseTypeDel); err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		var done bool
		done, err = Save(nil, []RES{vm})
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	hostsg, _ := LoadHostSG(vm.AgentUuid)
	go vm.DeletingVmTask(ports, vols, pool, gpus, hostsg)
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(vm.Uuid))
}

func AppVmPoweron(c echo.Context) (err error) {
	G.logger.Debug("=========AppVmPoweron==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine UUID is required."))
	}
	var vm *VM
	if vm, err = LoadVM(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if vm == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Virtual machine not found."))
	}
	for {
		if vm.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is not ready."))
		}
		if vm.IsSuspend() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is suspended."))
		}
		if err = vm.PhaseStart(PhaseTypePoweron); err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		done, err := Save(nil, []RES{vm})
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	go vm.PoweringOnVmTask()
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(vm.Uuid))
}

func AppVmPoweroff(c echo.Context) (err error) {
	G.logger.Debug("=========AppVmPoweroff==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine UUID is required."))
	}
	var vm *VM
	if vm, err = LoadVM(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if vm == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Virtual machine not found."))
	}
	q := VmPoweroffReq{}
	if err = c.Bind(&q); err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
	}
	for {
		if vm.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is not ready."))
		}
		if vm.IsSuspend() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is suspended."))
		}
		if err = vm.PhaseStart(PhaseTypePoweroff); err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		done, err := Save(nil, []RES{vm})
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	go vm.PoweringOffVmTask(q.Force)
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(vm.Uuid))
}

func AppVmInject(c echo.Context) (err error) {
	G.logger.Debug("=========AppVmInject==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine UUID is required."))
	}
	var vm *VM
	if vm, err = LoadVM(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if vm == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Virtual machine not found."))
	}
	q := VmInjectReq{}
	if err = c.Bind(&q); err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
	}
	for {
		if vm.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is not ready."))
		}
		if vm.IsSuspend() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is suspended."))
		}
		if err = vm.PhaseStart(PhaseTypeInject); err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		vm.CloudInitInfo.Os = q.Os
		vm.CloudInitInfo.Hostname = q.Hostname
		vm.CloudInitInfo.Username = q.Username
		vm.CloudInitInfo.Password = q.Password
		vm.CloudInitInfo.Pubkey = q.Pubkey
		vm.CloudInitInfo.InstanceId = strings.ToUpper(RandString(16))
		vm.CloudInitInfo.Done = false
		done, err := Save(nil, []RES{vm})
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	go vm.InjectingVmTask()
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(vm.Uuid))
}

func AppVmInject2(c echo.Context) (err error) {
	G.logger.Debug("=========AppVmInject2==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine UUID is required."))
	}
	var vm *VM
	if vm, err = LoadVM(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if vm == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Virtual machine not found."))
	}
	q := VmInject2Req{}
	if err = c.Bind(&q); err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
	}
	if !strings.Contains(q.Nbddev, "nbd") {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("NBD device is invalid."))
	}
	for {
		if vm.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is not ready."))
		}
		if vm.IsSuspend() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is suspended."))
		}
		if err = vm.PhaseStart(PhaseTypeInject2); err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		done, err := Save(nil, []RES{vm})
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	go vm.Injecting2VmTask(q.Nbddev)
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(vm.Uuid))
}

func AppVmEject(c echo.Context) (err error) {
	G.logger.Debug("=========AppVmEject==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine UUID is required."))
	}
	var vm *VM
	if vm, err = LoadVM(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if vm == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Virtual machine not found."))
	}
	for {
		if vm.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is not ready."))
		}
		if vm.IsSuspend() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is suspended."))
		}
		if err = vm.PhaseStart(PhaseTypeEject); err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		done, err := Save(nil, []RES{vm})
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	go vm.EjectingVmTask()
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(vm.Uuid))
}

func AppVmEject2(c echo.Context) (err error) {
	G.logger.Debug("=========AppVmEject2==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine UUID is required."))
	}
	var vm *VM
	if vm, err = LoadVM(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if vm == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Virtual machine not found."))
	}
	q := VmEject2Req{}
	if err = c.Bind(&q); err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
	}
	if !strings.Contains(q.Nbddev, "nbd") {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("NBD device is invalid."))
	}
	for {
		if vm.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is not ready."))
		}
		if vm.IsSuspend() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is suspended."))
		}
		if err = vm.PhaseStart(PhaseTypeEject2); err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		done, err := Save(nil, []RES{vm})
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	go vm.Ejecting2VmTask(q.Nbddev)
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(vm.Uuid))
}

func AppVmPhonehome(c echo.Context) (err error) {
	G.logger.Debug("=========AppVmPhonehome==========")
	instanceId := c.Param("instance_id")
	if instanceId == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine instance ID is required."))
	}
	var vm *VM
	rests := LoadRests("vm")
	if vm, err = LoadVM2(rests, instanceId); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	for {
		vm.CloudInitInfo.Done = true
		done, err := Save(nil, []RES{vm})
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	return c.NoContent(http.StatusOK)
}

func AppVmSuspend(c echo.Context) (err error) {
	G.logger.Debug("=========AppVmSuspend==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine UUID is required."))
	}
	var vm *VM
	if vm, err = LoadVM(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if vm == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Virtual machine not found."))
	}
	var vmLocalRes []string
	if vmLocalRes, err = vm.AssignLocalRes(true); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	hostsg, _ := LoadHostSG(vm.AgentUuid)
	var ports []*Port
	for i, portUuid := range vm.Ports {
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
		if _, err = port.AssignLocalRes(true); err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if vpcLocalRes, err = vpc.AssignLocalRes(true); err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		port.Nic = vmLocalRes[i*2+1]
		port.Ofport = vmLocalRes[i*2+2]
		port.Vlanid = vpcLocalRes[0]
		port.NID = vpc.NID
		port.Cidr = vpc.Cidr
		ports = append(ports, port)
	}
	var pool *Pool
	if pool, err = LoadPool(vm.PoolUuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if pool == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Pool not found."))
	}
	var vols []*Vol
	for _, volUuid := range vm.Vols {
		var vol *Vol
		if vol, err = LoadVol(volUuid); err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if vol != nil {
			vols = append(vols, vol)
		}
	}
	for {
		if vm.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is not ready."))
		}
		if vm.IsSuspend() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is suspended."))
		}
		if err = vm.PhaseStart(PhaseTypeSuspend); err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		done, err := Save(nil, []RES{vm})
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	go vm.SuspendingVmTask(ports, vols, pool, hostsg)
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(vm.Uuid))
}

func AppVmResume(c echo.Context) (err error) {
	G.logger.Debug("=========AppVmResume==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine UUID is required."))
	}
	var vm *VM
	if vm, err = LoadVM3(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if vm == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Virtual machine not found."))
	}
	hostsg, _ := LoadHostSG(G.config.Uuid)
	var movRtn *MoveVmRtn
	rests := LoadRests("")
	for {
		if vm.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is not ready."))
		}
		if !vm.IsSuspend() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is not suspended."))
		}
		if movRtn, err = vm.Move(rests); err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}

		reses := append([]RES{}, movRtn.toPut...)
		reses = append(reses, movRtn.toDel...)
		done, err := Save(nil, reses)
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			vm = movRtn.vm
			break
		}
	}
	go vm.ResumingVmTask(movRtn, hostsg)
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(vm.Uuid))
}

func AppVmModify(c echo.Context) (err error) {
	G.logger.Debug("=========AppVmModify==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine UUID is required."))
	}
	var vm *VM
	if vm, err = LoadVM(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if vm == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Virtual machine not found."))
	}
	q := VmModifyReq{}
	if err = c.Bind(&q); err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
	}
	for {
		if vm.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is not ready."))
		}
		if vm.IsSuspend() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is suspended."))
		}
		if err = vm.PhaseStart(PhaseTypeModify); err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if q.Vcpu != 0 {
			vm.Vcpu = q.Vcpu
			if err = vm.VerifyVcpu(); err != nil {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
			}
		}
		if q.Mem != 0 {
			vm.Mem = q.Mem
			if err = vm.VerifyMem(); err != nil {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
			}
		}
		if q.Bootdev != "" {
			vm.Bootdev = q.Bootdev
			if err = vm.VerifyBootdev(); err != nil {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
			}
		}
		done, err := Save(nil, []RES{vm})
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	go vm.ModifyingVmTask()
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(vm.Uuid))
}

func (vm *VM) Migrate() {
	hostsg, _ := LoadHostSG(G.config.Uuid)
	var movRtn *MoveVmRtn
	rests := LoadRests("")
	for {

		if vm.NotReady() {
			continue
		}
		if vm.PhaseType == PhaseTypeMigrate && vm.PhaseStatus != PhaseStatusSuccess {
			continue
		}
		if vm.PhaseType == PhaseTypeSuspend && vm.PhaseStatus == PhaseStatusSuccess {
			continue
		}

		locaked, err := vm.AllVolsLocked(rests)
		if err != nil {
			continue
		}
		if locaked {
			continue
		}
		movRtn, err = vm.Move(rests)
		if err != nil {
			return
		}
		reses := append([]RES{}, movRtn.toPut...)
		reses = append(reses, movRtn.toDel...)
		done, err := Save(nil, reses)
		if err != nil {
			return
		}
		if done {
			vm = movRtn.vm
			break
		}
	}
	go vm.ResumingVmTask(movRtn, hostsg)
}

func (vm *VM) PostMigrate() (err error) {
	var agent *Agent
	var vlanids []string
	agent = LoadAgent(G.config.Uuid)
	if agent == nil {
		return fmt.Errorf("cannot load agent")
	}
	pool, err := LoadPool(vm.PoolUuid)
	if err != nil {
		return err
	}
	var vmLocalRes []string
	if vmLocalRes, err = vm.AssignLocalRes(true); err != nil {
		return err
	}
	var ports []*Port
	for i, portUuid := range vm.Ports {
		var port *Port
		if port, err = LoadPort(portUuid); err != nil {
			return err
		}
		if port == nil {
			return fmt.Errorf("port not found")
		}
		var subnet *Subnet
		var vpc *VPC
		var vpcLocalRes []string
		if subnet, err = LoadSubnet(port.SubnetUuid); err != nil {
			return err
		}
		if subnet == nil {
			return fmt.Errorf("subnet not found")
		}
		if vpc, err = LoadVPC(subnet.VpcUuid); err != nil {
			return fmt.Errorf("cannot load vpc")
		}
		if vpc == nil {
			return fmt.Errorf("vpc not found")
		}
		if vpcLocalRes, err = vpc.AssignLocalRes(true); err != nil {
			return err
		}
		if !Contains(vlanids, vpcLocalRes[0]) {
			vlanids = append(vlanids, vpcLocalRes[0])
		}
		port.Nic = vmLocalRes[i*2+1]
		port.Ofport = vmLocalRes[i*2+2]
		port.Vlanid = vpcLocalRes[0]
		port.NID = vpc.NID
		port.Cidr = vpc.Cidr
		if !G.config.MasterL3 {
			if err = port.UnBind(subnet, vpc, agent, vpcLocalRes); err != nil {
				return err
			}
			if err = port.Del(subnet, vpc, agent, vpcLocalRes); err != nil {
				return err
			}
			if err = port.UnAssignLocalRes(G.config.Uuid); err != nil {
				return err
			}
			if !subnet.HasLocalPort() {
				if err = subnet.Del(vpc, vpcLocalRes); err != nil {
					return err
				}
				if err = subnet.UnAssignLocalRes(G.config.Uuid); err != nil {
					return err
				}
			}
			if !vpc.HasLocalSubnet() {
				if err = vpc.Del(); err != nil {
					return err
				}
				if err = vpc.UnAssignLocalRes(G.config.Uuid); err != nil {
					return err
				}
			}
		} else if G.config.MasterL3 && port.Floating != nil && port.Floating.ProtoPorts["ip"] != nil {
			if err = port.UnBind(subnet, vpc, agent, vpcLocalRes); err != nil {
				return err
			}
		}
		for {
			done, err := Save([]RES{vm}, nil)
			if err != nil {
				return err
			}
			if done {
				break
			}
		}
		ports = append(ports, port)
	}
	if err = DelVM(vm.Uuid); err != nil {
		return err
	}
	for _, port := range ports {
		if port.Nic == "" || port.Ofport == "" || port.Vlanid == "" {
			continue
		}
		if err = OvsVmDel(port.Nic, port.Vlanid, port.Mac, port.Ip, port.Ofport, port.Speed, port.Cidr); err != nil {
			return err
		}
		if err = OvsNicDel(G.config.LanBridge, port.Nic); err != nil {
			return err
		}
		if err = osNicDel(port.Nic, ""); err != nil {
			return err
		}
		if G.config.DpuIp != "" {
			DpuDelPort(port.Uuid)
		}
	}
	if vm.CloudInitInfo != nil && vm.CloudInitInfo.InstanceId != "" {
		isoFile := "/tmp/" + vm.CloudInitInfo.InstanceId + ".iso"
		if ok, _ := FileExists(isoFile); ok {
			os.Remove(isoFile)
		}
	}
	tokenFile := pool.ParaMap["DIR"] + "/tokens/" + vm.AgentUuid + "/" + vm.Uuid
	if ok, _ := FileExists(tokenFile); ok {
		if err = os.Remove(tokenFile); err != nil {
			return err
		}
	}
	delete(G.deployedRest, vm.Uuid)
	for _, vlanid := range vlanids {
		G.logger.Debug("notice dnsmasq (post migrate)", zap.String("vlanid", vlanid))
		NoticeDnsmasq(vlanid)
	}
	NoticeSG()
	return nil
}
