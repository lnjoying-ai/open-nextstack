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
	"os"

	"github.com/labstack/echo"
	"github.com/spf13/cobra"
	"go.uber.org/zap"
)

type Vol struct {
	Size     uint32   `json:"size"`
	Root     bool     `json:"root"`
	ImgUuid  string   `json:"img"`
	ImgName  string   `json:"img_name"`
	OrigFile string   `json:"orig_file"`
	PoolUuid string   `json:"pool"`
	VmUuid   string   `json:"vm"`
	Snaps    []string `json:"snaps"`
	AGENTREST
	PHASEINFO
}

type VolFlags struct {
	uuid       string
	abnormal   bool
	size       uint32
	root       bool
	imgUuid    string
	origFile   string
	vmUuid     string
	outputFile string
}

type VolAddReq struct {
	Size     uint32 `json:"size"`
	Root     bool   `json:"root"`
	ImgUuid  string `json:"img"`
	OrigFile string `json:"orig_file"`
}

type VolExportReq struct {
	OutputFile string `json:"output"`
}

type VolResizeReq struct {
	Size uint32 `json:"size"`
}

type VolAttachReq struct {
	VmUuid string `json:"vm"`
}

type VolGetRsp struct {
	Status    string   `json:"status"`
	Size      uint32   `json:"size"`
	Root      bool     `json:"root"`
	ImgUuid   string   `json:"img"`
	OrigFile  string   `json:"orig_file"`
	AgentUuid string   `json:"agent"`
	PoolUuid  string   `json:"pool"`
	VmUuid    string   `json:"vm"`
	Snaps     []string `json:"snaps"`
	AGENTREST
	PHASEINFO
}

type MoveVolRtn struct {
	toDel  []RES
	toPut  []RES
	snaps  []*Snap
	snap0s []*Snap
	img    *Img
	img0   *Img
	pool   *Pool
	pool0  *Pool
	vol0   *Vol
	vol    *Vol
}

var volFlags VolFlags

func LoadVol(vol_uuid string) (vol *Vol, err error) {
	vol = &Vol{AGENTREST: AGENTREST{UUIDINFO: UUIDINFO{Uuid: vol_uuid}, AgentUuid: G.config.Uuid, ETCDINFO: ETCDINFO{Type: "vol"}}}
	path := vol.GetKey()
	var ret RES
	if ret, err = LoadRes(path, vol); err != nil {
		return nil, err
	}
	if ret != nil {
		vol, ok := ret.(*Vol)
		if ok {
			return vol, nil
		}
	}
	return nil, nil
}

func LoadVol2(vol_uuid string, agentUuid string) (vol *Vol, err error) {
	vol = &Vol{AGENTREST: AGENTREST{UUIDINFO: UUIDINFO{Uuid: vol_uuid}, AgentUuid: agentUuid, ETCDINFO: ETCDINFO{Type: "vol"}}}
	path := vol.GetKey()
	var ret RES
	if ret, err = LoadRes(path, vol); err != nil {
		return nil, err
	}
	if ret != nil {
		vol, ok := ret.(*Vol)
		if ok {
			return vol, nil
		}
	}
	return nil, nil
}

func LoadVol3(volUuid string) (vol *Vol, err error) {
	rests := LoadRests("agent")
	for _, res := range rests["agent"] {
		agent, ok := res.(*Agent)
		if !ok {
			continue
		}
		vol = &Vol{AGENTREST: AGENTREST{UUIDINFO: UUIDINFO{Uuid: volUuid}, AgentUuid: agent.Uuid, ETCDINFO: ETCDINFO{Type: "vol"}}}
		path := vol.GetKey()
		var ret RES
		if ret, err = LoadRes(path, vol); err != nil {
			return nil, err
		}
		if ret != nil {
			vol, ok := ret.(*Vol)
			if ok {
				return vol, nil
			}
		}
	}
	return nil, nil
}

func (vol *Vol) Add(pool *Pool, img *Img) (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()
	if pool.Stype != "fs" {
		return fmt.Errorf("only fs pool is supported now")
	}
	volumesDir := pool.ParaMap["DIR"] + "/volumes"
	if err := osMkdir(volumesDir); err != nil {
		return err
	}
	diskPath := pool.ParaMap["DIR"] + "/volumes/" + vol.Uuid + ".qcow2"
	if vol.OrigFile != "" {

		info, err := GetQemuImgInfo(vol.OrigFile)
		if err != nil {
			return err
		}
		if info.Format != "qcow2" {
			return fmt.Errorf("only qcow2 format is supported")
		}
		if vol.OrigFile != diskPath {
			if err = os.Rename(vol.OrigFile, diskPath); err != nil {
				return err
			}
			s := BytesToGB(info.Size)
			if s < vol.Size {
				if err = QemuImgResize(diskPath, vol.Size); err != nil {
					return err
				}
			}
		}
	} else if img != nil {

		backing := pool.ParaMap["DIR"] + "/backing/" + img.Name + ".qcow2"
		var size uint32
		if vol.Size > img.Size {
			size = vol.Size
		}
		if err = QemuImgCreate(diskPath, size, backing); err != nil {
			return err
		}
	} else {

		if err = QemuImgCreate(diskPath, vol.Size, ""); err != nil {
			return err
		}
	}
	return nil
}

func (vol *Vol) Del(pool *Pool) (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()
	if pool.Stype != "fs" {
		return fmt.Errorf("only fs pool is supported now")
	}
	diskName := pool.ParaMap["DIR"] + "/volumes/" + vol.Uuid + ".qcow2"
	if ok, _ := FileExists(diskName); !ok {
		return nil
	}
	if err = os.Remove(diskName); err != nil {
		return err
	}
	return nil
}

func (vol *Vol) Attach(pool *Pool, vm *VM) (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()
	if pool.Stype != "fs" {
		return fmt.Errorf("only fs pool is supported now")
	}
	return AttachVol(vm, pool, vol)
}

func (vol *Vol) Detach(pool *Pool, vm *VM) (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()
	if pool.Stype != "fs" {
		return fmt.Errorf("only fs pool is supported now")
	}
	return DetachVol(vm, pool, vol)
}

func (vol *Vol) Export(pool *Pool, vm *VM, outputFile string) (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()
	if pool.Stype != "fs" {
		return fmt.Errorf("only fs pool is supported now")
	}
	return ExportVol(vm, pool, vol, outputFile)
}

func (vol *Vol) Resize(pool *Pool, vm *VM, size uint32) (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()
	if pool.Stype != "fs" {
		return fmt.Errorf("only fs pool is supported now")
	}
	return ResizeVol(vm, pool, vol, size)
}

func (vol *Vol) Suspend() (err error) {
	return nil
}

func (vol *Vol) Resume() (err error) {
	return nil
}

func (vol *Vol) AddingVolTask(pool *Pool, img *Img) {
	success := false
	err := vol.Add(pool, img)
	if err != nil {
		G.logger.Error("cannot add vol", zap.Error(err))
		goto UPDATE
	}
	success = true
UPDATE:
	if _, err = UpdatePhaseStop(vol, success); err != nil {
		G.logger.Error("cannot update vol phase", zap.Error(err))
	}
}

func (vol *Vol) DeletingVolTask(pool *Pool, img *Img) {
	var err error
	if err := vol.Del(pool); err != nil {
		G.logger.Error("cannot delete vol", zap.Error(err))
		goto UPDATE
	}
	for {
		pool.Vols = Remove(pool.Vols, vol.Uuid)
		if img != nil {
			img.Vols = Remove(img.Vols, vol.Uuid)
		}

		var done bool
		if done, err = Save([]RES{vol}, []RES{pool, img}); err != nil {
			G.logger.Error("cannot save res", zap.Error(err))
			goto UPDATE
		}
		if done {
			return
		}
	}
UPDATE:
	if _, err = UpdatePhaseStop(vol, false); err != nil {
		G.logger.Error("cannot update vol phase", zap.Error(err))
		return
	}
}

func (vol *Vol) AttachingVolTask(pool *Pool, vm *VM) {
	var err error
	success := false
	if err := vol.Attach(pool, vm); err != nil {
		G.logger.Error("cannot attach vol", zap.Error(err))
		goto UPDATE
	}
	success = true
UPDATE:
	for {
		reses := []RES{vol, vm}
		var done bool
		vol.PhaseStop(success)
		vm.PhaseStop(success)
		done, err = Save(nil, reses)
		if err != nil {
			G.logger.Error("cannot save reses", zap.Error(err))
			return
		}
		if done {
			break
		}
	}
}

func (vol *Vol) DetachingVolTask(pool *Pool, vm *VM) {
	var err error
	success := false
	if err = vol.Detach(pool, vm); err != nil {
		G.logger.Error("cannot detach vol", zap.Error(err))
		goto UPDATE
	}
	success = true
UPDATE:
	for {
		reses := []RES{vol, vm}
		vm.Vols = Remove(vm.Vols, vol.Uuid)
		vol.VmUuid = ""
		vol.PhaseStop(success)
		vm.PhaseStop(success)
		var done bool
		done, err = Save(nil, reses)
		if err != nil {
			G.logger.Error("cannot save reses", zap.Error(err))
			return
		}
		if done {
			break
		}
	}
}

func (vol *Vol) ExportingVolTask(pool *Pool, vm *VM, outputFile string) {
	var err error
	success := false
	if err = vol.Export(pool, vm, outputFile); err != nil {
		G.logger.Error("cannot export vol", zap.Error(err))
		goto UPDATE
	}
	success = true
UPDATE:
	for {
		reses := []RES{vol}
		if vm != nil {
			vm.PhaseStop(success)
			reses = append(reses, vm)
		}
		vol.PhaseStop(success)
		var done bool
		done, err = Save(nil, reses)
		if err != nil {
			G.logger.Error("cannot save reses", zap.Error(err))
			return
		}
		if done {
			break
		}
	}
}

func (vol *Vol) ResizingVolTask(pool *Pool, vm *VM, size uint32) {
	var err error
	success := false
	if err = vol.Resize(pool, vm, size); err != nil {
		G.logger.Error("cannot export vol", zap.Error(err))
		goto UPDATE
	}
	success = true
UPDATE:
	for {
		reses := []RES{vol}
		if vm != nil {
			vm.PhaseStop(success)
			reses = append(reses, vm)
		}
		vol.Size += size
		vol.PhaseStop(success)
		var done bool
		done, err = Save(nil, reses)
		if err != nil {
			G.logger.Error("cannot save reses", zap.Error(err))
			return
		}
		if done {
			break
		}
	}
}

func (vol *Vol) SuspendingVolTask(pool *Pool) {
	var err error
	success := false
	if err = vol.Suspend(); err != nil {
		G.logger.Error("cannot suspend vol", zap.Error(err))
		goto UPDATE
	}
	success = true
UPDATE:
	for {
		vol.PhaseStop(success)
		var done bool
		done, err = Save(nil, []RES{vol})
		if err != nil {
			G.logger.Error("cannot save reses", zap.Error(err))
			return
		}
		if done {
			break
		}
	}
}

func (vol *Vol) ResumingVolTask(movRtn *MoveVolRtn) {
	var err error
	success := false
	if err = vol.Resume(); err != nil {
		G.logger.Error("cannot resume vol", zap.Error(err))
		goto UPDATE
	}
	success = true
UPDATE:
	for {
		if movRtn.pool.PhaseStatus == PhaseStatusPending && movRtn.pool.PhaseType == PhaseTypeAdd {
			movRtn.pool.PhaseStop(success)
		}
		toPut := movRtn.toPut
		if !success {
			movRtn.vol0.PhaseStop(success)
			toPut = append(toPut, movRtn.vol0)
			for _, snap0 := range movRtn.snap0s {
				snap0.PhaseStop(success)
				toPut = append(toPut, snap0)
			}
		}
		vol = movRtn.vol
		vol.PhaseStop(success)
		if success && vol.Root {
			img0 := movRtn.img0
			if vol.ImgUuid == img0.Uuid {
				img0.Vols = Remove(img0.Vols, vol.Uuid)
			}
		}
		img := movRtn.img
		if img != nil {
			img.PhaseStop(success)
		}
		for _, snap := range movRtn.snaps {
			snap.PhaseStop(success)
		}
		vol.PhaseStop(success)
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
}

func (vol *Vol) VerifyUuid() error {
	return AssignUuid("vol", vol)
}

func (vol *Vol) VerifyVol(pool *Pool, img *Img) error {
	const MAX_VOL_SIZE = 1024 * 20
	if vol.OrigFile != "" {
		if ok, _ := FileExists(vol.OrigFile); !ok {
			return fmt.Errorf("orig file does not exist")
		}
	} else if vol.Size <= 0 || vol.Size > MAX_VOL_SIZE {
		return fmt.Errorf("invalid sized")
	}
	if !Contains(pool.Vols, vol.Uuid) {
		pool.Vols = append(pool.Vols, vol.Uuid)
	}
	if img != nil && !Contains(img.Vols, vol.Uuid) {
		img.Vols = append(img.Vols, vol.Uuid)
	}
	return nil
}

func (o *Vol) IsSuspend() bool {
	return (o.PhaseType == PhaseTypeSuspend && o.PhaseStatus == PhaseStatusSuccess) || (o.PhaseType == PhaseTypeResume && o.PhaseStatus == PhaseStatusFail) || (o.PhaseType == PhaseTypeMigrate && o.PhaseStatus == PhaseStatusFail)
}

func (vol *Vol) MovePool(rests RESTS, rtn *MoveVolRtn) (err error) {
	var pool0 *Pool
	if pool0, err = LoadPool2(vol.PoolUuid, vol.AgentUuid); err != nil {
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

func (vol0 *Vol) MoveVol(rests RESTS, rtn *MoveVolRtn) (err error) {

	if ok, _ := FileExists(rtn.pool.ParaMap["DIR"] + "/volumes/" + vol0.Uuid + ".qcow2"); !ok {
		return fmt.Errorf("volume file not found")
	}
	vol := &Vol{}
	*vol = *vol0
	if err = vol0.PhaseStart(PhaseTypeDel); err != nil {
		return err
	}
	rtn.vol0 = vol0
	rtn.toDel = append(rtn.toDel, vol0)
	rtn.pool0.Vols = Remove(rtn.pool0.Vols, vol0.Uuid)
	vol.AgentUuid = G.config.Uuid
	vol.PoolUuid = rtn.pool.Uuid
	vol.Modrev = 0
	if err = vol.PhaseStart(PhaseTypeResume); err != nil {
		return err
	}
	rtn.vol = vol
	rtn.toPut = append(rtn.toPut, vol)
	rtn.pool.Vols = append(rtn.pool.Vols, vol.Uuid)
	if vol0.ImgUuid != "" {

		var img0 *Img
		if img0, err = LoadImg2(vol0.ImgUuid, vol0.AgentUuid); err != nil {
			return err
		}
		if img0 == nil {
			return fmt.Errorf("image not found")
		}
		rtn.img0 = img0
		rtn.toPut = append(rtn.toPut, img0)
		img0.Vols = Remove(img0.Vols, vol0.Uuid)

		var img *Img
		if img, err = LoadImg3(rests, img0.Name); err != nil {
			return err
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
		rtn.img = img
		rtn.toPut = append(rtn.toPut, img)
		rtn.pool.Imgs[img.Uuid] = img.Name
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
	return nil
}

func (vol *Vol) Move(rests RESTS) (rtn *MoveVolRtn, err error) {
	rtn = &MoveVolRtn{
		toDel:  []RES{},
		toPut:  []RES{},
		snaps:  []*Snap{},
		snap0s: []*Snap{}}
	if vol.AgentUuid != G.config.Uuid {
		if err = vol.MovePool(rests, rtn); err != nil {
			return nil, err
		}
		if err = vol.MoveVol(rests, rtn); err != nil {
			return nil, err
		}
		return rtn, nil
	}
	if err = vol.LocalMove(rests, rtn); err != nil {
		return nil, err
	}
	return rtn, nil
}

func (vol *Vol) LocalMove(rests RESTS, rtn *MoveVolRtn) (err error) {
	var pool *Pool
	if pool, err = LoadPool(vol.PoolUuid); err != nil {
		return err
	}
	rtn.pool = pool
	imgs := map[string]struct{}{}
	if vol.ImgUuid != "" {
		if _, ok := imgs[vol.ImgUuid]; !ok {
			var img *Img
			if img, err = LoadImg(vol.ImgUuid); err != nil {
				return err
			}
			if err = img.PhaseStart(PhaseTypeResume); err != nil {
				return err
			}
			rtn.img = img
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
	if err = vol.PhaseStart(PhaseTypeResume); err != nil {
		return err
	}
	rtn.vol = vol
	rtn.toPut = append(rtn.toPut, vol)
	return nil
}

func VolCmdParser(command *cobra.Command) {
	var err error
	var volCmd = &cobra.Command{
		Use:   "vol",
		Short: "Vol management",
		Run: func(cmd *cobra.Command, args []string) {
			fmt.Println("please specify a subcommand for vol management. use -h for help.")
		},
	}
	var volAddCmd = &cobra.Command{
		Use:   "add",
		Short: "Add a Vol",
		Run:   VolAddHandle,
	}
	volAddCmd.Flags().StringVarP(&volFlags.uuid, "uuid", "U", "", "set the UUID of the Pool")
	if err = volAddCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	volAddCmd.Flags().Uint32Var(&volFlags.size, "size", 0, "set the size (GB) of the Vol")
	if err = volAddCmd.MarkFlagRequired("size"); err != nil {
		panic(err)
	}
	volAddCmd.Flags().BoolVar(&volFlags.root, "root", false, "set the Vol as root disk")
	volAddCmd.Flags().StringVar(&volFlags.imgUuid, "img", "", "set the UUID of the Img for the root Vol")
	volAddCmd.Flags().StringVar(&volFlags.origFile, "orig", "", "set the existing Vol file")
	var volGetCmd = &cobra.Command{
		Use:   "get",
		Short: "Get a Vol",
		Run:   VolGetHandle,
	}
	volGetCmd.Flags().StringVarP(&volFlags.uuid, "uuid", "U", "", "set the UUID of the Vol")
	if err = volGetCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var volDelCmd = &cobra.Command{
		Use:   "del",
		Short: "Delete a Vol",
		Run:   VolDelHandle,
	}
	volDelCmd.Flags().StringVarP(&volFlags.uuid, "uuid", "U", "", "set the UUID of the Vol")
	if err = volDelCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var volListCmd = &cobra.Command{
		Use:   "list",
		Short: "List all Vols",
		Run:   VolListHandle,
	}
	volListCmd.Flags().BoolVar(&volFlags.abnormal, "abnormal", false, "list abnormal Vols")
	volListCmd.Flags().StringVarP(&volFlags.uuid, "uuid", "U", "", "set the UUID of the Vol")
	if err = volGetCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var volAttachCmd = &cobra.Command{
		Use:   "attach",
		Short: "Attach a Vol to a VM",
		Run:   VolAttachHandle,
	}
	volAttachCmd.Flags().StringVarP(&volFlags.uuid, "uuid", "U", "", "set the UUID of the Vol")
	if err = volAttachCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	volAttachCmd.Flags().StringVar(&volFlags.vmUuid, "vm", "", "set the UUID of the VM")
	if err = volAttachCmd.MarkFlagRequired("vm"); err != nil {
		panic(err)
	}
	var volDetachCmd = &cobra.Command{
		Use:   "detach",
		Short: "Detach a Vol from a VM",
		Run:   VolDetachHandle,
	}
	volDetachCmd.Flags().StringVarP(&volFlags.uuid, "uuid", "U", "", "set the UUID of the Vol")
	if err = volDetachCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var volExportCmd = &cobra.Command{
		Use:   "export",
		Short: "Export a Vol to a file",
		Run:   VolExportHandle,
	}
	volExportCmd.Flags().StringVarP(&volFlags.uuid, "uuid", "U", "", "set the UUID of the Vol")
	volExportCmd.Flags().StringVar(&volFlags.outputFile, "output", "", "set the file name of the exported Vol")
	if err = volExportCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	if err = volExportCmd.MarkFlagRequired("output"); err != nil {
		panic(err)
	}
	var volResizeCmd = &cobra.Command{
		Use:   "resize",
		Short: "Resize a Vol",
		Run:   VolResizeHandle,
	}
	volResizeCmd.Flags().StringVarP(&volFlags.uuid, "uuid", "U", "", "set the UUID of the Vol")
	volResizeCmd.Flags().Uint32Var(&volFlags.size, "size", 0, "set the increase size (GB) of the Vol")
	if err = volResizeCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	if err = volResizeCmd.MarkFlagRequired("size"); err != nil {
		panic(err)
	}
	var volSuspendCmd = &cobra.Command{
		Use:   "suspend",
		Short: "Suspend a Vol",
		Run:   VolSuspendHandle,
	}
	volSuspendCmd.Flags().StringVarP(&volFlags.uuid, "uuid", "U", "", "set the UUID of the Vol")
	if err = volSuspendCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var volResumeCmd = &cobra.Command{
		Use:   "resume",
		Short: "Resume a Vol",
		Run:   VolResumeHandle,
	}
	volResumeCmd.Flags().StringVarP(&volFlags.uuid, "uuid", "U", "", "set the UUID of the Vol")
	if err = volResumeCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	volCmd.AddCommand(volAddCmd, volGetCmd, volDelCmd, volListCmd, volAttachCmd, volDetachCmd, volExportCmd, volResizeCmd, volSuspendCmd, volResumeCmd)
	command.AddCommand(volCmd)
}

func VolAddHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/pools/%s/vols", G.Host, G.Port, volFlags.uuid)
	data := map[string]interface{}{"size": volFlags.size, "root": volFlags.root, "img": volFlags.imgUuid, "orig_file": volFlags.origFile}
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

func VolGetHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/vols/%s", G.Host, G.Port, volFlags.uuid)
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

func VolDelHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/vols/%s", G.Host, G.Port, volFlags.uuid)
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

func VolListHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/pools/%s/vols", G.Host, G.Port, volFlags.uuid)
	data := map[string]interface{}{}
	if volFlags.abnormal {
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

func VolAttachHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/vols/%s/attach", G.Host, G.Port, volFlags.uuid)
	data := map[string]interface{}{"vm": volFlags.vmUuid}
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

func VolDetachHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/vols/%s/detach", G.Host, G.Port, volFlags.uuid)
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

func VolExportHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/vols/%s/export", G.Host, G.Port, volFlags.uuid)
	data := map[string]interface{}{"output": volFlags.outputFile}
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

func VolResizeHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/vols/%s/resize", G.Host, G.Port, volFlags.uuid)
	data := map[string]interface{}{"size": volFlags.size}
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

func VolSuspendHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/vols/%s/suspend", G.Host, G.Port, volFlags.uuid)
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

func VolResumeHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/vols/%s/resume", G.Host, G.Port, volFlags.uuid)
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

func VolAppSetup() {
	G.echoServer.POST("/v1/pools/:uuid/vols", AppVolAdd)
	G.echoServer.GET("/v1/pools/:uuid/vols", AppVolList)
	G.echoServer.GET("/v1/vols/:uuid", AppVolGet)
	G.echoServer.DELETE("/v1/vols/:uuid", AppVolDel)
	G.echoServer.PUT("/v1/vols/:uuid/attach", AppVolAttach)
	G.echoServer.PUT("/v1/vols/:uuid/detach", AppVolDetach)
	G.echoServer.PUT("/v1/vols/:uuid/export", AppVolExport)
	G.echoServer.PUT("/v1/vols/:uuid/resize", AppVolResize)
	G.echoServer.PUT("/v1/vols/:uuid/suspend", AppVolSuspend)
	G.echoServer.PUT("/v1/vols/:uuid/resume", AppVolResume)
}

func AppVolAdd(c echo.Context) (err error) {
	G.logger.Debug("=========AppVolAdd==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Pool UUID is required."))
	}
	pool, err := LoadPool(uuid)
	if err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load pool."))
	}
	if pool == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Pool not found."))
	}
	q := VolAddReq{}
	if err = c.Bind(&q); err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Cannot parse the request."))
	}
	if q.ImgUuid != "" && q.OrigFile != "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Cannot set both image and original source."))
	}
	if !q.Root && q.ImgUuid != "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Cannot set an image for a data volume."))
	}
	var vol *Vol
	var img *Img
	if q.ImgUuid != "" {
		img, err = LoadImg(q.ImgUuid)
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load image."))
		}
		if img == nil {
			return c.JSON(http.StatusNotFound, NewAppErrorRsp("Image not found."))
		}
	}
	for {
		if pool.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Pool is not ready."))
		}
		if img != nil {
			if img.NotReady() {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Image is not ready."))
			}

			if img.PoolUuid != pool.Uuid {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Image is not in the pool."))
			}
		}
		vol = &Vol{Snaps: []string{}, AGENTREST: AGENTREST{AgentUuid: G.config.Uuid, ETCDINFO: ETCDINFO{Type: "vol"}}}
		vol.Size = q.Size
		vol.Root = q.Root
		vol.ImgUuid = q.ImgUuid
		vol.OrigFile = q.OrigFile
		vol.PoolUuid = uuid
		if img != nil {
			vol.ImgName = img.Name
		}
		_ = vol.PhaseStart(PhaseTypeAdd)
		if err = vol.VerifyUuid(); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		if err = vol.VerifyVol(pool, img); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		var done bool
		done, err = Save(nil, []RES{vol, pool, img})
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	go vol.AddingVolTask(pool, img)
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(vol.Uuid))
}

func AppVolList(c echo.Context) (err error) {
	G.logger.Debug("=========AppVolList==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Pool UUID is required."))
	}
	pool, err := LoadPool(uuid)
	if err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load pool."))
	}
	if pool == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Pool not found."))
	}
	q := AppListReq{}
	if err = c.Bind(&q); err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
	}
	volUuids := make([]string, 0, len(pool.Vols))
	rests := LoadRests("vol")
	for _, volUuid := range pool.Vols {
		v := &Vol{AGENTREST: AGENTREST{UUIDINFO: UUIDINFO{Uuid: volUuid}, AgentUuid: G.config.Uuid, ETCDINFO: ETCDINFO{Type: "vol"}}}
		if rest := GetRest(rests, "vol", v.GetKey()); rest != nil {
			vol, ok := rest.(*Vol)
			if !ok {
				continue
			}
			if q.Abnormal {
				if vol.NotReady() {
					volUuids = append(volUuids, vol.Uuid)
				}
			} else {
				volUuids = append(volUuids, vol.Uuid)
			}
		}
	}
	jsonMap := make(map[string]interface{})
	jsonMap["status"] = "ok"
	jsonMap["vol_count"] = len(volUuids)
	jsonMap["vols"] = volUuids
	return c.JSON(http.StatusOK, jsonMap)
}

func AppVolGet(c echo.Context) (err error) {
	G.logger.Debug("=========AppVolGet==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Volume UUID is required."))
	}
	var vol *Vol
	if vol, err = LoadVol(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if vol == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Volume not found."))
	}
	rsp := VolGetRsp{
		Status:    "ok",
		Size:      vol.Size,
		Root:      vol.Root,
		ImgUuid:   vol.ImgUuid,
		OrigFile:  vol.OrigFile,
		AgentUuid: vol.AgentUuid,
		PoolUuid:  vol.PoolUuid,
		VmUuid:    vol.VmUuid,
		Snaps:     vol.Snaps,
		AGENTREST: vol.AGENTREST,
		PHASEINFO: vol.PHASEINFO}
	return c.JSON(http.StatusOK, &rsp)
}

func AppVolDel(c echo.Context) (err error) {
	G.logger.Debug("=========AppVolDel==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Volume UUID is required."))
	}
	var vol *Vol
	if vol, err = LoadVol(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if vol == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Volume not found."))
	}
	var img *Img
	if vol.ImgUuid != "" {
		if img, err = LoadImg(vol.ImgUuid); err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if img == nil {
			return c.JSON(http.StatusNotFound, NewAppErrorRsp("Image not found."))
		}
	}
	var pool *Pool
	if pool, err = LoadPool(vol.PoolUuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if pool == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Pool not found."))
	}
	for {
		if vol.VmUuid != "" {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Volume is in use."))
		}
		if err = vol.PhaseStart(PhaseTypeDel); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		var done bool
		done, err = Save(nil, []RES{vol})
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	go vol.DeletingVolTask(pool, img)
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(vol.Uuid))
}

func AppVolAttach(c echo.Context) (err error) {
	G.logger.Debug("=========AppVolAttach==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Volume UUID is required."))
	}
	var vol *Vol
	if vol, err = LoadVol(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if vol == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Volume not found."))
	}
	q := VolAttachReq{}
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
	var pool *Pool
	if pool, err = LoadPool(vm.PoolUuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if pool == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Pool not found."))
	}
	for {
		if vol.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Volume is not ready."))
		}
		if vol.IsSuspend() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Volume is suspended."))
		}
		if vol.VmUuid != "" && vol.VmUuid != vm.Uuid {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Volume is in use."))
		}
		if vm.PoolUuid != vol.PoolUuid {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is not in the same pool."))
		}
		if vm.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is not ready."))
		}
		if vm.IsSuspend() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is suspended."))
		}
		if len(vm.Vols) > 3 {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine has 4 attached disks."))
		}
		if len(vm.Cps) > 0 {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine has checkpoints."))
		}
		if err = vm.PhaseStart(PhaseTypeVol); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		if err = vol.PhaseStart(PhaseTypeAttach); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		if !Contains(vm.Vols, vol.Uuid) {
			vm.Vols = append(vm.Vols, vol.Uuid)
		}
		vol.VmUuid = vm.Uuid
		var done bool
		done, err = Save(nil, []RES{vol, vm})
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	go vol.AttachingVolTask(pool, vm)
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(vol.Uuid))
}

func AppVolDetach(c echo.Context) (err error) {
	G.logger.Debug("=========AppVolDetach==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Volume UUID is required."))
	}
	var vol *Vol
	if vol, err = LoadVol(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if vol == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Volume not found."))
	}
	if vol.VmUuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Volume is not in use."))
	}
	vm, err := LoadVM(vol.VmUuid)
	if err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if vm == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Virtual machine not found."))
	}
	var pool *Pool
	if pool, err = LoadPool(vm.PoolUuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if pool == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Pool not found."))
	}
	for {
		if vol.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Volume is not ready."))
		}
		if vol.VmUuid == "" {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Volume is not in use."))
		}
		if vm.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is not ready."))
		}
		if vm.IsSuspend() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is suspended."))
		}
		if len(vm.Cps) > 0 {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine has checkpoints."))
		}
		if err = vm.PhaseStart(PhaseTypeVol); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		if err = vol.PhaseStart(PhaseTypeDetach); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		var done bool
		done, err = Save(nil, []RES{vol})
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	go vol.DetachingVolTask(pool, vm)
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(vol.Uuid))
}

func AppVolExport(c echo.Context) (err error) {
	G.logger.Debug("=========AppVolExport==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Volume UUID is required."))
	}
	q := VolExportReq{}
	if err = c.Bind(&q); err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Cannot parse the request."))
	}
	var vol *Vol
	if vol, err = LoadVol(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if vol == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Volume not found."))
	}
	var vm *VM
	if vol.VmUuid != "" {
		vm, err = LoadVM(vol.VmUuid)
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if vm == nil {
			return c.JSON(http.StatusNotFound, NewAppErrorRsp("Virtual machine not found."))
		}
	}
	var pool *Pool
	if pool, err = LoadPool(vol.PoolUuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if pool == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Pool not found."))
	}
	for {
		reses := []RES{vol}
		if vol.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Volume is not ready."))
		}
		if vol.IsSuspend() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Volume is suspended."))
		}
		if vol.VmUuid != "" {
			if vm.NotReady() {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is not ready."))
			}
			if vm.IsSuspend() {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is suspended."))
			}
			if err = vm.PhaseStart(PhaseTypeVol); err != nil {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
			}
			reses = append(reses, vm)
		}
		if err = vol.PhaseStart(PhaseTypeExport); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
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
	go vol.ExportingVolTask(pool, vm, q.OutputFile)
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(vol.Uuid))
}

func AppVolResize(c echo.Context) (err error) {
	G.logger.Debug("=========AppVolResize==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Volume UUID is required."))
	}
	q := VolResizeReq{}
	if err = c.Bind(&q); err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Cannot parse the request."))
	}
	var vol *Vol
	if vol, err = LoadVol(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if vol == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Volume not found."))
	}
	var vm *VM
	if vol.VmUuid != "" {
		vm, err = LoadVM(vol.VmUuid)
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if vm == nil {
			return c.JSON(http.StatusNotFound, NewAppErrorRsp("Virtual machine not found."))
		}
	}
	var pool *Pool
	if pool, err = LoadPool(vol.PoolUuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if pool == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Pool not found."))
	}
	for {
		reses := []RES{vol}
		if vol.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Volume is not ready."))
		}
		if vol.IsSuspend() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Volume is suspended."))
		}
		if vol.VmUuid != "" {
			if vm.NotReady() {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is not ready."))
			}
			if vm.IsSuspend() {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Virtual machine is suspended."))
			}
			if err = vm.PhaseStart(PhaseTypeVol); err != nil {
				return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
			}
			reses = append(reses, vm)
		}
		if err = vol.PhaseStart(PhaseTypeExport); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
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
	go vol.ResizingVolTask(pool, vm, q.Size)
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(vol.Uuid))
}

func AppVolSuspend(c echo.Context) (err error) {
	G.logger.Debug("=========AppVolSuspend==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Volume UUID is required."))
	}
	var vol *Vol
	if vol, err = LoadVol(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if vol == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Volume not found."))
	}
	if vol.VmUuid != "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Volume is in use."))
	}
	var pool *Pool
	if pool, err = LoadPool(vol.PoolUuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if pool == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Pool not found."))
	}
	for {
		if vol.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Volume is not ready."))
		}
		if vol.VmUuid != "" {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Volume is in use."))
		}
		if vol.IsSuspend() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Volume is suspended."))
		}
		if err = vol.PhaseStart(PhaseTypeSuspend); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		var done bool
		done, err = Save(nil, []RES{vol})
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	go vol.SuspendingVolTask(pool)
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(vol.Uuid))
}

func AppVolResume(c echo.Context) (err error) {
	G.logger.Debug("=========AppVolResume==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Volume UUID is required."))
	}
	var vol *Vol
	if vol, err = LoadVol3(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if vol == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Volume not found."))
	}
	var movRtn *MoveVolRtn
	rests := LoadRests("")
	for {
		if vol.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Volume is not ready."))
		}
		if !vol.IsSuspend() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Volume is not suspended."))
		}
		if movRtn, err = vol.Move(rests); err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}

		reses := append([]RES{}, movRtn.toPut...)
		reses = append(reses, movRtn.toDel...)
		done, err := Save(nil, reses)
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			vol = movRtn.vol
			break
		}
	}
	go vol.ResumingVolTask(movRtn)
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(vol.Uuid))
}
