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

type Snap struct {
	Parent    string `json:"parent"`
	IsCurrent bool   `json:"current"`
	VolUuid   string `json:"vol"`
	PoolUuid  string `json:"pool"`
	CpUuid    string `json:"cp"`
	AGENTREST
	PHASEINFO
}

type SnapFlags struct {
	uuid     string
	abnormal bool
}

type SnapAddReq struct {
}

type SnapGetRsp struct {
	Status    string `json:"status"`
	Parent    string `json:"parent"`
	IsCurrent bool   `json:"current"`
	VolUuid   string `json:"vol"`
	AgentUuid string `json:"agent"`
	PoolUuid  string `json:"pool"`
	CpUuid    string `json:"cp"`
	AGENTREST
	PHASEINFO
}

var snapFlags SnapFlags

func LoadSnap(snap_uuid string) (snap *Snap, err error) {
	snap = &Snap{AGENTREST: AGENTREST{UUIDINFO: UUIDINFO{Uuid: snap_uuid}, AgentUuid: G.config.Uuid, ETCDINFO: ETCDINFO{Type: "snap"}}}
	path := snap.GetKey()
	var ret RES
	if ret, err = LoadRes(path, snap); err != nil {
		return nil, err
	}
	if ret != nil {
		snap, ok := ret.(*Snap)
		if ok {
			return snap, nil
		}
	}
	return nil, nil
}

func LoadSnap2(snap_uuid string, agentUuid string) (snap *Snap, err error) {
	snap = &Snap{AGENTREST: AGENTREST{UUIDINFO: UUIDINFO{Uuid: snap_uuid}, AgentUuid: agentUuid, ETCDINFO: ETCDINFO{Type: "snap"}}}
	path := snap.GetKey()
	var ret RES
	if ret, err = LoadRes(path, snap); err != nil {
		return nil, err
	}
	if ret != nil {
		snap, ok := ret.(*Snap)
		if ok {
			return snap, nil
		}
	}
	return nil, nil
}

func (snap *Snap) Add(vol *Vol, pool *Pool) (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()
	if pool.Stype != "fs" {
		return fmt.Errorf("pool %s is not fs", pool.Uuid)
	}
	if err = CreateVolSnap(vol.Uuid, snap.Uuid, vol.VmUuid, pool.ParaMap["DIR"]); err != nil {
		return err
	}
	return nil
}

func (snap *Snap) Del(vol *Vol, pool *Pool) (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()
	if pool.Stype != "fs" {
		return fmt.Errorf("pool %s is not fs", pool.Uuid)
	}
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
	if err = DeleteVolSnap(snap.Uuid, mergeTo, snap.Parent, vol.VmUuid, pool.ParaMap["DIR"]); err != nil {
		return err
	}
	return nil
}

func (snap *Snap) Switch(vol *Vol, pool *Pool) (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()
	if pool.Stype != "fs" {
		return fmt.Errorf("pool %s is not fs", pool.Uuid)
	}
	if err = SwitchVolSnap(snap.Uuid, vol.Uuid, vol.VmUuid, pool.ParaMap["DIR"]); err != nil {
		return err
	}
	return nil
}

func (snap *Snap) Restore(vol *Vol, pool *Pool) (err error) {
	return nil
}

func (snap *Snap) AddingSnapTask(vol *Vol, pool *Pool, vm *VM) {
	err := snap.Add(vol, pool)
	if err != nil {
		G.logger.Error("cannot add snap", zap.Error(err))
		for {
			snap.PhaseStop(false)
			vol.PhaseStop(false)
			if vm != nil {
				vm.PhaseStop(false)
			}
			var done bool
			if vm != nil {
				done, err = Save(nil, []RES{snap, vol, vm})
			} else {
				done, err = Save(nil, []RES{snap, vol})
			}
			if err != nil {
				G.logger.Error("cannot save snap", zap.Error(err))
				return
			}
			if done {
				return
			}
		}
	} else {
		for {
			reses := []RES{snap}
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
			snap.PhaseStop(true)
			vol.PhaseStop(true)
			reses = append(reses, vol)
			if vm != nil {
				vm.PhaseStop(true)
				reses = append(reses, vm)
			}
			var done bool
			done, err = Save(nil, reses)
			if err != nil {
				G.logger.Error("cannot save snap", zap.Error(err))
				return
			}
			if done {
				break
			}
		}
	}
}

func (snap *Snap) DeletingSnapTask(vol *Vol, pool *Pool, vm *VM) {
	err := snap.Del(vol, pool)
	if err != nil {
		G.logger.Error("cannot delete snap", zap.Error(err))
		for {
			snap.PhaseStop(false)
			vol.PhaseStop(false)
			if vm != nil {
				vm.PhaseStop(false)
			}
			var done bool
			if vm != nil {
				done, err = Save(nil, []RES{snap, vol, vm})
			} else {
				done, err = Save(nil, []RES{snap, vol})
			}
			if err != nil {
				G.logger.Error("cannot save snap", zap.Error(err))
				return
			}
			if done {
				return
			}
		}
	} else {
		for {
			reses := []RES{}
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
			vol.PhaseStop(true)
			reses = append(reses, vol)
			if vm != nil {
				vm.PhaseStop(true)
				reses = append(reses, vm)
			}
			var done bool
			done, err = Save([]RES{snap}, reses)
			if err != nil {
				G.logger.Error("cannot save snap", zap.Error(err))
				return
			}
			if done {
				break
			}
		}
	}
}

func (snap *Snap) SwitchingSnapTask(vol *Vol, pool *Pool, vm *VM) {
	err := snap.Switch(vol, pool)
	if err != nil {
		G.logger.Error("cannot switch snap", zap.Error(err))
		for {
			snap.PhaseStop(false)
			vol.PhaseStop(false)
			if vm != nil {
				vm.PhaseStop(false)
			}
			var done bool
			if vm != nil {
				done, err = Save(nil, []RES{snap, vol, vm})
			} else {
				done, err = Save(nil, []RES{snap, vol})
			}
			if err != nil {
				G.logger.Error("cannot save snap", zap.Error(err))
				return
			}
			if done {
				return
			}
		}
	} else {
		for {
			reses := []RES{snap}
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
			snap.PhaseStop(true)
			vol.PhaseStop(true)
			reses = append(reses, vol)
			if vm != nil {
				vm.PhaseStop(true)
				reses = append(reses, vm)
			}
			var done bool
			done, err = Save(nil, reses)
			if err != nil {
				G.logger.Error("cannot save snap", zap.Error(err))
				return
			}
			if done {
				break
			}
		}
	}
}

func (snap *Snap) VerifyUuid() error {
	return AssignUuid("snap", snap)
}

func SnapCmdParser(command *cobra.Command) {
	var err error
	var snapCmd = &cobra.Command{
		Use:   "snap",
		Short: "Volume snapshot management",
		Run: func(cmd *cobra.Command, args []string) {
			fmt.Println("please specify a subcommand for volume snapshot management. use -h for help.")
		},
	}
	var snapAddCmd = &cobra.Command{
		Use:   "add",
		Short: "Add a Snap",
		Run:   SnapAddHandle,
	}
	snapAddCmd.Flags().StringVarP(&snapFlags.uuid, "uuid", "U", "", "set the UUID of the Vol")
	if err = snapAddCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var snapGetCmd = &cobra.Command{
		Use:   "get",
		Short: "Get a Snap",
		Run:   SnapGetHandle,
	}
	snapGetCmd.Flags().StringVarP(&snapFlags.uuid, "uuid", "U", "", "set the UUID of the snap")
	if err = snapGetCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var snapDelCmd = &cobra.Command{
		Use:   "del",
		Short: "Delete a Snap",
		Run:   SnapDelHandle,
	}
	snapDelCmd.Flags().StringVarP(&snapFlags.uuid, "uuid", "U", "", "set the UUID of the snap")
	if err = snapDelCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var snapListCmd = &cobra.Command{
		Use:   "list",
		Short: "List all Snaps",
		Run:   SnapListHandle,
	}
	snapListCmd.Flags().BoolVar(&snapFlags.abnormal, "abnormal", false, "list abnormal snaps")
	snapListCmd.Flags().StringVarP(&snapFlags.uuid, "uuid", "U", "", "set the UUID of the Vol")
	if err = snapGetCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var snapSwitchCmd = &cobra.Command{
		Use:   "switch",
		Short: "Switch to a Snap",
		Run:   SnapSwitchHandle,
	}
	snapSwitchCmd.Flags().StringVarP(&snapFlags.uuid, "uuid", "U", "", "set the UUID of the snap")
	if err = snapSwitchCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	snapCmd.AddCommand(snapAddCmd, snapGetCmd, snapDelCmd, snapListCmd, snapSwitchCmd)
	command.AddCommand(snapCmd)
}

func SnapAddHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/vols/%s/snaps", G.Host, G.Port, snapFlags.uuid)
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

func SnapGetHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/snaps/%s", G.Host, G.Port, snapFlags.uuid)
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

func SnapDelHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/snaps/%s", G.Host, G.Port, snapFlags.uuid)
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

func SnapSwitchHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/snaps/%s/switch", G.Host, G.Port, snapFlags.uuid)
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

func SnapListHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/vols/%s/snaps", G.Host, G.Port, snapFlags.uuid)
	data := map[string]interface{}{}
	if snapFlags.abnormal {
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

func SnapAppSetup() {
	G.echoServer.POST("/v1/vols/:uuid/snaps", AppSnapAdd)
	G.echoServer.GET("/v1/vols/:uuid/snaps", AppSnapList)
	G.echoServer.GET("/v1/snaps/:uuid", AppSnapGet)
	G.echoServer.DELETE("/v1/snaps/:uuid", AppSnapDel)
	G.echoServer.PUT("/v1/snaps/:uuid/switch", AppSnapSwitch)
}

func AppSnapAdd(c echo.Context) (err error) {
	G.logger.Debug("=========AppSnapAdd==========")
	var snap *Snap
	var pool *Pool
	var vol *Vol
	var vm *VM
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Volume UUID is required."))
	}
	if vol, err = LoadVol(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load volume."))
	}
	if vol == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Volume not found."))
	}
	if pool, err = LoadPool(vol.PoolUuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load pool."))
	}
	if pool == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Pool not found."))
	}
	if vol.VmUuid != "" {
		if vm, err = LoadVM(vol.VmUuid); err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load virtual machine."))
		}
		if vm == nil {
			return c.JSON(http.StatusNotFound, NewAppErrorRsp("Virtual machine not found."))
		}
	}
	for {
		if vol.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Volume is not ready."))
		}
		if pool.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Pool is not ready."))
		}
		if vm != nil {
			if vm.NotReady() {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is not added."))
			}
			if vm.IsSuspend() {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is suspended."))
			}

			if err = vm.PhaseStart(PhaseTypeVol); err != nil {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
			}
		}

		if err = vol.PhaseStart(PhaseTypeSnap); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		snap = &Snap{VolUuid: vol.Uuid, PoolUuid: vol.PoolUuid, AGENTREST: AGENTREST{AgentUuid: G.config.Uuid, ETCDINFO: ETCDINFO{Type: "snap"}}}
		_ = snap.PhaseStart(PhaseTypeAdd)
		if err = snap.VerifyUuid(); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		vol.Snaps = append(vol.Snaps, snap.Uuid)
		var done bool
		done, err = Save(nil, []RES{snap, vol, vm})
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	go snap.AddingSnapTask(vol, pool, vm)
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(snap.Uuid))
}

func AppSnapList(c echo.Context) (err error) {
	G.logger.Debug("=========AppSnapList==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Volume UUID is required."))
	}
	var vol *Vol
	if vol, err = LoadVol(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load volume."))
	}
	if vol == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Volume not found."))
	}
	q := AppListReq{}
	if err = c.Bind(&q); err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
	}
	snapUuids := []string{}
	rests := LoadRests("snap")
	for _, snapUuid := range vol.Snaps {
		s := &Snap{AGENTREST: AGENTREST{UUIDINFO: UUIDINFO{Uuid: snapUuid}, AgentUuid: G.config.Uuid, ETCDINFO: ETCDINFO{Type: "snap"}}}
		if rest := GetRest(rests, "snap", s.GetKey()); rest != nil {
			snap, ok := rest.(*Snap)
			if !ok {
				continue
			}
			if q.Abnormal {
				if snap.NotReady() {
					snapUuids = append(snapUuids, snap.Uuid)
				}
			} else {
				snapUuids = append(snapUuids, snap.Uuid)
			}
		}
	}
	jsonMap := make(map[string]interface {
	})
	jsonMap["status"] = "ok"
	jsonMap["snap_count"] = len(snapUuids)
	jsonMap["snaps"] = snapUuids
	return c.JSON(http.StatusOK, jsonMap)
}

func AppSnapGet(c echo.Context) (err error) {
	G.logger.Debug("=========AppSnapGet==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Snapshot UUID is required."))
	}
	var snap *Snap
	if snap, err = LoadSnap(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if snap == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Snapshot not found."))
	}
	rsp := SnapGetRsp{
		Status:    "ok",
		Parent:    snap.Parent,
		IsCurrent: snap.IsCurrent,
		VolUuid:   snap.VolUuid,
		AgentUuid: snap.AgentUuid,
		PoolUuid:  snap.PoolUuid,
		CpUuid:    snap.CpUuid,
		AGENTREST: snap.AGENTREST,
		PHASEINFO: snap.PHASEINFO}
	return c.JSON(http.StatusOK, &rsp)
}

func AppSnapDel(c echo.Context) (err error) {
	G.logger.Debug("=========AppSnapDel==========")
	var snap *Snap
	var pool *Pool
	var vol *Vol
	var vm *VM
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Snapshot UUID is required."))
	}
	if snap, err = LoadSnap(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load snapshot."))
	}
	if snap == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Snapshot not found."))
	}
	if vol, err = LoadVol(snap.VolUuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load volume."))
	}
	if vol == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Volume not found."))
	}
	if pool, err = LoadPool(vol.PoolUuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load pool."))
	}
	if pool == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Pool not found."))
	}
	if vol.VmUuid != "" {
		if vm, err = LoadVM(vol.VmUuid); err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load virtual machine."))
		}
		if vm == nil {
			return c.JSON(http.StatusNotFound, NewAppErrorRsp("Virtual machine not found."))
		}
	}
	for {
		if snap.CpUuid != "" {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Snapshot was created by a checkpoint."))
		}
		if vol.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Volume is not ready."))
		}
		if pool.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Pool is not ready."))
		}
		if vm != nil {
			if vm.NotReady() {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is not ready."))
			}
			if vm.IsSuspend() {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is suspended."))
			}

			if err = vm.PhaseStart(PhaseTypeVol); err != nil {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
			}
		}

		if err = vol.PhaseStart(PhaseTypeSnap); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		if err = snap.PhaseStart(PhaseTypeDel); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		var done bool
		if vm != nil {
			done, err = Save(nil, []RES{snap, vol, vm})
		} else {
			done, err = Save(nil, []RES{snap, vol})
		}
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	go snap.DeletingSnapTask(vol, pool, vm)
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(snap.Uuid))
}

func AppSnapSwitch(c echo.Context) (err error) {
	G.logger.Debug("=========AppSnapSwitch==========")
	var snap *Snap
	var pool *Pool
	var vol *Vol
	var vm *VM
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Snapshot UUID is required."))
	}
	if snap, err = LoadSnap(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load snapshot."))
	}
	if snap == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Snapshot not found."))
	}
	if vol, err = LoadVol(snap.VolUuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load volume."))
	}
	if vol == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Volume not found."))
	}
	if pool, err = LoadPool(vol.PoolUuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load pool."))
	}
	if pool == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Pool not found."))
	}
	if vol.VmUuid != "" {
		if vm, err = LoadVM(vol.VmUuid); err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load virtual machine."))
		}
		if vm == nil {
			return c.JSON(http.StatusNotFound, NewAppErrorRsp("Virtual machine not found."))
		}
	}
	for {
		if vol.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Volume is not ready."))
		}
		if pool.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Pool is not ready."))
		}
		if vm != nil {
			if vm.NotReady() {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is not ready."))
			}
			if vm.IsSuspend() {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is suspended."))
			}

			if err = vm.PhaseStart(PhaseTypeVol); err != nil {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
			}
		}

		if err = vol.PhaseStart(PhaseTypeSnap); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		if err = snap.PhaseStart(PhaseTypeSwitch); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		var done bool
		if vm != nil {
			done, err = Save(nil, []RES{snap, vol, vm})
		} else {
			done, err = Save(nil, []RES{snap, vol})
		}
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	go snap.SwitchingSnapTask(vol, pool, vm)
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(snap.Uuid))
}
