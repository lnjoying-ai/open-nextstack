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
	"net/http"

	"github.com/labstack/echo"
	"github.com/spf13/cobra"
	"go.uber.org/zap"
)

type CP struct {
	Parent    string `json:"parent"`
	IsCurrent bool   `json:"current"`
	VmUuid    string `json:"vm"`

	PoolUuid string   `json:"pool"`
	Snaps    []string `json:"snaps"`
	AGENTREST
	PHASEINFO
}

type CpFlags struct {
	uuid     string
	abnormal bool
}

type CpAddReq struct {
}

type CpGetRsp struct {
	Status    string   `json:"status"`
	Parent    string   `json:"parent"`
	IsCurrent bool     `json:"current"`
	VmUuid    string   `json:"vm"`
	AgentUuid string   `json:"agent"`
	PoolUuid  string   `json:"pool"`
	Snaps     []string `json:"snaps"`
	AGENTREST
	PHASEINFO
}

var cpFlags CpFlags

func LoadCP(cp_uuid string) (cp *CP, err error) {
	cp = &CP{AGENTREST: AGENTREST{UUIDINFO: UUIDINFO{Uuid: cp_uuid}, AgentUuid: G.config.Uuid, ETCDINFO: ETCDINFO{Type: "cp"}}}
	path := cp.GetKey()
	var ret RES
	if ret, err = LoadRes(path, cp); err != nil {
		return nil, err
	}
	if ret != nil {
		cp, ok := ret.(*CP)
		if ok {
			return cp, nil
		}
	}
	return nil, nil
}

func LoadCP2(cp_uuid string, agentUuid string) (cp *CP, err error) {
	cp = &CP{AGENTREST: AGENTREST{UUIDINFO: UUIDINFO{Uuid: cp_uuid}, AgentUuid: agentUuid, ETCDINFO: ETCDINFO{Type: "cp"}}}
	path := cp.GetKey()
	var ret RES
	if ret, err = LoadRes(path, cp); err != nil {
		return nil, err
	}
	if ret != nil {
		cp, ok := ret.(*CP)
		if ok {
			return cp, nil
		}
	}
	return nil, nil
}

func (cp *CP) Add(vm *VM, pool *Pool, vols []*Vol, snaps []*Snap) (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()
	if pool.Stype != "fs" {
		return fmt.Errorf("pool %s is not fs", pool.Uuid)
	}
	var volUuids []string
	var snapUuids []string
	for _, vol := range vols {
		volUuids = append(volUuids, vol.Uuid)
	}
	for _, snap := range snaps {
		snapUuids = append(snapUuids, snap.Uuid)
	}
	if err = CreateVolsSnap(volUuids, snapUuids, vm.Uuid, pool.ParaMap["DIR"], cp.Uuid); err != nil {
		return err
	}
	return nil
}

func (cp *CP) Del(vm *VM, pool *Pool, vols []*Vol, snaps []*Snap) (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()
	if pool.Stype != "fs" {
		return fmt.Errorf("pool %s is not fs", pool.Uuid)
	}
	snapUuids := []string{}
	mergeTos := map[string][]string{}
	backings := []string{}
	for i, snap := range snaps {
		vol := vols[i]
		snapUuids = append(snapUuids, snap.Uuid)
		mergeTo := []string{}
		if snap.IsCurrent {
			mergeTo = append(mergeTo, vol.Uuid)
		}
		for _, snapUuid := range vol.Snaps {
			if snapUuid == snap.Uuid {
				continue
			}
			_snap, err := LoadSnap(snapUuid)
			if err != nil {
				return err
			}
			if _snap == nil {
				continue
			}
			if _snap.Parent == snap.Uuid {
				mergeTo = append(mergeTo, _snap.Uuid)
			}
		}
		mergeTos[snap.Uuid] = mergeTo
		backings = append(backings, snap.Parent)
	}
	if err = DeleteVolsSnap(snapUuids, mergeTos, backings, vm.Uuid, pool.ParaMap["DIR"], cp.Uuid); err != nil {
		return err
	}
	return nil
}

func (cp *CP) Switch(vm *VM, pool *Pool, vols []*Vol, snaps []*Snap, gpus []*GPU) (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()
	if pool.Stype != "fs" {
		return fmt.Errorf("pool %s is not fs", pool.Uuid)
	}
	snapUuids := []string{}
	volUuids := []string{}
	for _, vol := range vols {
		volUuids = append(volUuids, vol.Uuid)
	}
	for _, snap := range snaps {
		snapUuids = append(snapUuids, snap.Uuid)
	}
	if err = SwitchVolsSnap(snapUuids, volUuids, vm.Uuid, pool.ParaMap["DIR"], cp.Uuid, gpus); err != nil {
		return err
	}
	return nil
}

func (cp *CP) Restore(vm *VM, pool *Pool) (err error) {
	return nil
}

func (cp *CP) AddingCpTask(vm *VM, pool *Pool, vols []*Vol, snaps []*Snap) {
	err := cp.Add(vm, pool, vols, snaps)
	if err != nil {
		G.logger.Error("cannot add cp", zap.Error(err))
		for {
			reses := []RES{cp, vm}
			cp.PhaseStop(false)
			vm.PhaseStop(false)
			for _, vol := range vols {
				vol.PhaseStop(false)
				reses = append(reses, vol)
			}
			for _, snap := range snaps {
				snap.PhaseStop(false)
				reses = append(reses, snap)
			}
			var done bool
			done, err = Save(nil, reses)
			if err != nil {
				G.logger.Error("cannot save cp", zap.Error(err))
				return
			}
			if done {
				return
			}
		}
	} else {
		for {
			reses := []RES{cp}
			for _, cpUuid := range vm.Cps {
				if cpUuid == cp.Uuid {
					continue
				}
				_cp, err := LoadCP(cpUuid)
				if err != nil {
					G.logger.Error("Cannot load checkpoint.", zap.Error(err))
					return
				}
				if _cp == nil {
					continue
				}
				if _cp.IsCurrent {
					_cp.IsCurrent = false
					cp.Parent = _cp.Uuid
					reses = append(reses, _cp)
					break
				}
			}
			cp.IsCurrent = true
			cp.PhaseStop(true)
			vm.PhaseStop(true)
			reses = append(reses, vm)
			for i, vol := range vols {
				vol.PhaseStop(true)
				reses = append(reses, vol)
				snap := snaps[i]
				snap.PhaseStop(true)
				reses = append(reses, snap)
				for _, snapUuid := range vol.Snaps {
					if snapUuid == snap.Uuid {
						continue
					}
					_snap, err := LoadSnap(snapUuid)
					if err != nil {
						G.logger.Error("Cannot load snapshot.", zap.Error(err))
						return
					}
					if _snap == nil {
						continue
					}
					if _snap.IsCurrent {
						_snap.IsCurrent = false
						snap.Parent = _snap.Uuid
						reses = append(reses, _snap)
						break
					}
				}
				if snap.Parent == "" {
					snap.Parent = vol.ImgName
				}
				snap.IsCurrent = true
			}
			var done bool
			done, err = Save(nil, reses)
			if err != nil {
				G.logger.Error("cannot save cp", zap.Error(err))
				return
			}
			if done {
				break
			}
		}
	}
}

func (cp *CP) DeletingCpTask(vm *VM, pool *Pool, vols []*Vol, snaps []*Snap) {
	err := cp.Del(vm, pool, vols, snaps)
	if err != nil {
		G.logger.Error("cannot delete cp", zap.Error(err))
		for {
			reses := []RES{cp, vm}
			cp.PhaseStop(false)
			vm.PhaseStop(false)
			for _, vol := range vols {
				vol.PhaseStop(false)
				reses = append(reses, vol)
			}
			for _, snap := range snaps {
				snap.PhaseStop(false)
				reses = append(reses, snap)
			}
			var done bool
			done, err = Save(nil, reses)
			if err != nil {
				G.logger.Error("cannot save cp", zap.Error(err))
				return
			}
			if done {
				return
			}
		}
	} else {
		for {
			reses := []RES{vm}
			for _, cpUuid := range vm.Cps {
				if cpUuid == cp.Uuid {
					continue
				}
				_cp, err := LoadCP(cpUuid)
				if err != nil {
					G.logger.Error("Cannot load checkpoint.", zap.Error(err))
					return
				}
				if _cp == nil {
					continue
				}
				if cp.Parent == _cp.Uuid {
					if cp.IsCurrent {
						_cp.IsCurrent = true
					}
					reses = append(reses, _cp)
				} else if _cp.Parent == cp.Uuid {
					_cp.Parent = cp.Parent
					reses = append(reses, _cp)
				}
			}
			vm.Cps = Remove(vm.Cps, cp.Uuid)
			vm.PhaseStop(true)
			reses_todel := []RES{cp}
			for i, vol := range vols {
				vol.PhaseStop(true)
				reses = append(reses, vol)
				snap := snaps[i]
				reses_todel = append(reses_todel, snap)
				for _, snapUuid := range vol.Snaps {
					if snapUuid == snap.Uuid {
						continue
					}
					_snap, err := LoadSnap(snapUuid)
					if err != nil {
						G.logger.Error("Cannot load snapshot.", zap.Error(err))
						return
					}
					if _snap == nil {
						continue
					}
					if snap.Parent == _snap.Uuid {
						if snap.IsCurrent {
							_snap.IsCurrent = true
						}
						reses = append(reses, _snap)
					} else if _snap.Parent == snap.Uuid {
						_snap.Parent = snap.Parent
						reses = append(reses, _snap)
					}
				}
				vol.Snaps = Remove(vol.Snaps, snap.Uuid)
			}
			var done bool
			done, err = Save(reses_todel, reses)
			if err != nil {
				G.logger.Error("cannot save cp", zap.Error(err))
				return
			}
			if done {
				break
			}
		}
	}
}

func (cp *CP) SwitchingCpTask(vm *VM, pool *Pool, vols []*Vol, snaps []*Snap, gpus []*GPU) {
	err := cp.Switch(vm, pool, vols, snaps, gpus)
	if err != nil {
		G.logger.Error("cannot switch cp", zap.Error(err))
		for {
			cp.PhaseStop(false)
			vm.PhaseStop(false)
			reses := []RES{cp, vm}
			for _, vol := range vols {
				vol.PhaseStop(false)
				reses = append(reses, vol)
			}
			for _, snap := range snaps {
				snap.PhaseStop(false)
				reses = append(reses, snap)
			}
			var done bool
			done, err = Save(nil, reses)
			if err != nil {
				G.logger.Error("cannot save cp", zap.Error(err))
				return
			}
			if done {
				return
			}
		}
	} else {
		for {
			reses := []RES{cp}
			for _, cpUuid := range vm.Cps {
				if cpUuid == cp.Uuid {
					continue
				}
				_cp, err := LoadCP(cpUuid)
				if err != nil {
					G.logger.Error("Cannot load checkpoint.", zap.Error(err))
					return
				}
				if _cp == nil {
					continue
				}
				if _cp.IsCurrent {
					_cp.IsCurrent = false
					reses = append(reses, _cp)
					break
				}
			}
			cp.IsCurrent = true
			cp.PhaseStop(true)
			vm.PhaseStop(true)
			reses = append(reses, vm)
			for i, vol := range vols {
				vol.PhaseStop(true)
				reses = append(reses, vol)
				snap := snaps[i]
				snap.PhaseStop(true)
				reses = append(reses, snap)
				for _, snapUuid := range vol.Snaps {
					if snapUuid == snap.Uuid {
						continue
					}
					_snap, err := LoadSnap(snapUuid)
					if err != nil {
						G.logger.Error("Cannot load snapshot.", zap.Error(err))
						return
					}
					if _snap == nil {
						continue
					}
					if _snap.IsCurrent {
						_snap.IsCurrent = false
						reses = append(reses, _snap)
						break
					}
				}
				snap.IsCurrent = true
			}
			var done bool
			done, err = Save(nil, reses)
			if err != nil {
				G.logger.Error("cannot save cp", zap.Error(err))
				return
			}
			if done {
				break
			}
		}
	}
}

func (cp *CP) VerifyUuid() error {
	return AssignUuid("cp", cp)
}

func CpCmdParser(command *cobra.Command) {
	var err error
	var cpCmd = &cobra.Command{
		Use:   "cp",
		Short: "VM checkpoint management",
		Run: func(cmd *cobra.Command, args []string) {
			fmt.Println("please specify a subcommand for vm checkpoint management. use -h for help.")
		},
	}
	var cpAddCmd = &cobra.Command{
		Use:   "add",
		Short: "Add a CP",
		Run:   CpAddHandle,
	}
	cpAddCmd.Flags().StringVarP(&cpFlags.uuid, "uuid", "U", "", "set the UUID of the VM")
	if err = cpAddCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var cpGetCmd = &cobra.Command{
		Use:   "get",
		Short: "Get a CP",
		Run:   CpGetHandle,
	}
	cpGetCmd.Flags().StringVarP(&cpFlags.uuid, "uuid", "U", "", "set the UUID of the CP")
	if err = cpGetCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var cpDelCmd = &cobra.Command{
		Use:   "del",
		Short: "Delete a CP",
		Run:   CpDelHandle,
	}
	cpDelCmd.Flags().StringVarP(&cpFlags.uuid, "uuid", "U", "", "set the UUID of the CP")
	if err = cpDelCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var cpListCmd = &cobra.Command{
		Use:   "list",
		Short: "List all Cps",
		Run:   CpListHandle,
	}
	cpListCmd.Flags().BoolVar(&cpFlags.abnormal, "abnormal", false, "list abnormal CPs")
	cpListCmd.Flags().StringVarP(&cpFlags.uuid, "uuid", "U", "", "set the UUID of the VM")
	if err = cpGetCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var cpSwitchCmd = &cobra.Command{
		Use:   "switch",
		Short: "Switch to a CP",
		Run:   CpSwitchHandle,
	}
	cpSwitchCmd.Flags().StringVarP(&cpFlags.uuid, "uuid", "U", "", "set the UUID of the CP")
	if err = cpSwitchCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	cpCmd.AddCommand(cpAddCmd, cpGetCmd, cpDelCmd, cpListCmd, cpSwitchCmd)
	command.AddCommand(cpCmd)
}

func CpAddHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/vms/%s/cps", G.Host, G.Port, cpFlags.uuid)
	req, _ := http.NewRequest("POST", url, nil)
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

func CpGetHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/cps/%s", G.Host, G.Port, cpFlags.uuid)
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

func CpDelHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/cps/%s", G.Host, G.Port, cpFlags.uuid)
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

func CpSwitchHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/cps/%s/switch", G.Host, G.Port, cpFlags.uuid)
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

func CpListHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/vms/%s/cps", G.Host, G.Port, cpFlags.uuid)
	data := map[string]interface{}{}
	if cpFlags.abnormal {
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

func CpAppSetup() {
	G.echoServer.POST("/v1/vms/:uuid/cps", AppCpAdd)
	G.echoServer.GET("/v1/vms/:uuid/cps", AppCpList)
	G.echoServer.GET("/v1/cps/:uuid", AppCpGet)
	G.echoServer.DELETE("/v1/cps/:uuid", AppCpDel)
	G.echoServer.PUT("/v1/cps/:uuid/switch", AppCpSwitch)
}

func AppCpAdd(c echo.Context) (err error) {
	G.logger.Debug("=========AppCpAdd==========")
	var cp *CP
	var pool *Pool
	var vm *VM
	var vols []*Vol
	var snaps []*Snap
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine UUID is required."))
	}
	if vm, err = LoadVM(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load virtual machine."))
	}
	if vm == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Virtual machine not found."))
	}
	if pool, err = LoadPool(vm.PoolUuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load pool."))
	}
	if pool == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Pool not found."))
	}
	for _, volUuid := range vm.Vols {
		var vol *Vol
		if vol, err = LoadVol(volUuid); err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load volume."))
		} else if vol == nil {
			return c.JSON(http.StatusNotFound, NewAppErrorRsp("Volume not found."))
		}
		vols = append(vols, vol)
	}
	for {
		if vm.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is not ready."))
		}
		if vm.IsSuspend() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is suspended."))
		}
		if pool.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Pool is not added."))
		}
		cp = &CP{VmUuid: vm.Uuid, PoolUuid: vm.PoolUuid, AGENTREST: AGENTREST{AgentUuid: G.config.Uuid, ETCDINFO: ETCDINFO{Type: "cp"}}}
		_ = cp.PhaseStart(PhaseTypeAdd)
		if err = cp.VerifyUuid(); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		reses := []RES{cp, vm}
		for _, vol := range vols {
			if vol.NotReady() {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Volume is not ready."))
			}
			if err = vol.PhaseStart(PhaseTypeSnap); err != nil {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
			}

			snap := &Snap{VolUuid: vol.Uuid, PoolUuid: vol.PoolUuid, CpUuid: cp.Uuid, AGENTREST: AGENTREST{AgentUuid: G.config.Uuid, ETCDINFO: ETCDINFO{Type: "snap"}}}
			_ = snap.PhaseStart(PhaseTypeAdd)
			if err = snap.VerifyUuid(); err != nil {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
			}
			vol.Snaps = append(vol.Snaps, snap.Uuid)
			snaps = append(snaps, snap)
			cp.Snaps = append(cp.Snaps, snap.Uuid)
			reses = append(reses, vol, snap)
		}

		if err = vm.PhaseStart(PhaseTypeCP); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}

		vm.Cps = append(vm.Cps, cp.Uuid)
		var done bool
		done, err = Save(nil, reses)
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	go cp.AddingCpTask(vm, pool, vols, snaps)
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(cp.Uuid))
}

func AppCpList(c echo.Context) (err error) {
	G.logger.Debug("=========AppCpList==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine UUID is required."))
	}
	var vm *VM
	if vm, err = LoadVM(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load virtual machine."))
	}
	if vm == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Virtual machine not found."))
	}
	q := AppListReq{}
	if err = c.Bind(&q); err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
	}
	cpUuids := make([]string, 0, len(vm.Cps))
	rests := LoadRests("cp")
	for _, cpUuid := range vm.Cps {
		c := &CP{AGENTREST: AGENTREST{UUIDINFO: UUIDINFO{Uuid: cpUuid}, AgentUuid: G.config.Uuid, ETCDINFO: ETCDINFO{Type: "cp"}}}
		if rest := GetRest(rests, "cp", c.GetKey()); rest != nil {
			cp, ok := rest.(*CP)
			if !ok {
				continue
			}
			if q.Abnormal {
				if cp.NotReady() {
					cpUuids = append(cpUuids, cp.Uuid)
				}
			} else {
				cpUuids = append(cpUuids, cp.Uuid)
			}
		}
	}
	jsonMap := make(map[string]interface {
	})
	jsonMap["status"] = "ok"
	jsonMap["cp_count"] = len(cpUuids)
	jsonMap["cps"] = cpUuids
	return c.JSON(http.StatusOK, jsonMap)
}

func AppCpGet(c echo.Context) (err error) {
	G.logger.Debug("=========AppCpGet==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Checkpoint UUID is required."))
	}
	var cp *CP
	if cp, err = LoadCP(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if cp == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Checkpoint not found."))
	}
	rsp := CpGetRsp{
		Status:    "ok",
		Parent:    cp.Parent,
		IsCurrent: cp.IsCurrent,
		VmUuid:    cp.VmUuid,
		AgentUuid: cp.AgentUuid,
		PoolUuid:  cp.PoolUuid,
		Snaps:     cp.Snaps,
		AGENTREST: cp.AGENTREST,
		PHASEINFO: cp.PHASEINFO}
	return c.JSON(http.StatusOK, &rsp)
}

func AppCpDel(c echo.Context) (err error) {
	G.logger.Debug("=========AppCpDel==========")
	var cp *CP
	var pool *Pool
	var vm *VM
	var vols []*Vol
	var snaps []*Snap
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Checkpoint UUID is required."))
	}
	if cp, err = LoadCP(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load checkpoint."))
	}
	if cp == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Checkpoint not found."))
	}
	for _, snapUuid := range cp.Snaps {
		var snap *Snap
		if snap, err = LoadSnap(snapUuid); err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load snapshot."))
		} else if snap == nil {
			return c.JSON(http.StatusNotFound, NewAppErrorRsp("Snapshot not found."))
		}
		snaps = append(snaps, snap)
		var vol *Vol
		if vol, err = LoadVol(snap.VolUuid); err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load volume."))
		} else if vol == nil {
			return c.JSON(http.StatusNotFound, NewAppErrorRsp("Volume not found."))
		}
		vols = append(vols, vol)
	}
	if vm, err = LoadVM(cp.VmUuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load virtual machine."))
	}
	if vm == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Virtual machine not found."))
	}
	if pool, err = LoadPool(vm.PoolUuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load pool."))
	}
	if pool == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Pool not found."))
	}
	for {
		reses := []RES{cp, vm}
		if vm.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is not ready."))
		}
		if vm.IsSuspend() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is suspended."))
		}
		if pool.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Pool is not ready."))
		}

		if err = vm.PhaseStart(PhaseTypeCP); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		if err = cp.PhaseStart(PhaseTypeDel); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		for _, vol := range vols {
			if vol.NotReady() {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Volume is not ready."))
			}
			if err = vol.PhaseStart(PhaseTypeSnap); err != nil {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
			}
			reses = append(reses, vol)
		}
		for _, snap := range snaps {
			if err = snap.PhaseStart(PhaseTypeDel); err != nil {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
			}
			reses = append(reses, snap)
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
	go cp.DeletingCpTask(vm, pool, vols, snaps)
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(cp.Uuid))
}

func AppCpSwitch(c echo.Context) (err error) {
	G.logger.Debug("=========AppCpSwitch==========")
	var cp *CP
	var pool *Pool
	var vm *VM
	var vols []*Vol
	var snaps []*Snap
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Checkpoint UUID is required."))
	}
	if cp, err = LoadCP(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load checkpoint."))
	}
	if cp == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Checkpoint not found."))
	}
	for _, snapUuid := range cp.Snaps {
		var snap *Snap
		if snap, err = LoadSnap(snapUuid); err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load snapshot."))
		} else if snap == nil {
			return c.JSON(http.StatusNotFound, NewAppErrorRsp("Snapshot not found."))
		}
		snaps = append(snaps, snap)
		var vol *Vol
		if vol, err = LoadVol(snap.VolUuid); err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load volume."))
		} else if vol == nil {
			return c.JSON(http.StatusNotFound, NewAppErrorRsp("Volume not found."))
		}
		vols = append(vols, vol)
	}
	if vm, err = LoadVM(cp.VmUuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load virtual machine."))
	}
	if vm == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Virtual machine not found."))
	}
	if pool, err = LoadPool(vm.PoolUuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load pool."))
	}
	if pool == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Pool not found."))
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
		if vm.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is not ready."))
		}
		if vm.IsSuspend() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is suspended."))
		}
		if pool.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Pool is not ready."))
		}

		if err = vm.PhaseStart(PhaseTypeCP); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		if err = cp.PhaseStart(PhaseTypeSwitch); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		reses := []RES{cp, vm}
		for _, vol := range vols {
			if vol.NotReady() {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Volume is not ready."))
			}
			if err = vol.PhaseStart(PhaseTypeSnap); err != nil {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
			}
			reses = append(reses, vol)
		}
		for _, snap := range snaps {
			if snap.NotReady() {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Snapshot is not ready."))
			}
			if err = snap.PhaseStart(PhaseTypeSwitch); err != nil {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
			}
			reses = append(reses, snap)
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
	go cp.SwitchingCpTask(vm, pool, vols, snaps, gpus)
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(cp.Uuid))
}
