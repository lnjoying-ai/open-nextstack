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
	"path"

	"github.com/labstack/echo"
	"github.com/spf13/cobra"
	"go.uber.org/zap"
)

type Img struct {
	Name string `json:"name"`
	Desc string `json:"desc"`
	Size uint32 `json:"size"`

	PoolUuid string   `json:"pool"`
	Vols     []string `json:"vols"`
	AGENTREST
	PHASEINFO
}

type ImgFlags struct {
	uuid     string
	abnormal bool
	force    bool
	name     string
	desc     string
}

type ImgAddReq struct {
	Name string `json:"name"`
	Desc string `json:"desc"`
}

type ImgGetRsp struct {
	Status    string   `json:"status"`
	Name      string   `json:"name"`
	Desc      string   `json:"desc"`
	Size      uint32   `json:"size"`
	AgentUuid string   `json:"agent"`
	PoolUuid  string   `json:"pool"`
	Vols      []string `json:"vols"`
	AGENTREST
	PHASEINFO
}

type ImgDelReq struct {
	Force bool `json:"force"`
}

var imgFlags ImgFlags

func LoadImg(img_uuid string) (img *Img, err error) {
	img = &Img{AGENTREST: AGENTREST{UUIDINFO: UUIDINFO{Uuid: img_uuid}, AgentUuid: G.config.Uuid, ETCDINFO: ETCDINFO{Type: "img"}}}
	path := img.GetKey()
	var ret RES
	if ret, err = LoadRes(path, img); err != nil {
		return nil, err
	}
	if ret != nil {
		img, ok := ret.(*Img)
		if ok {
			return img, nil
		}
	}
	return nil, nil
}

func LoadImg2(img_uuid string, agentUuid string) (img *Img, err error) {
	img = &Img{AGENTREST: AGENTREST{UUIDINFO: UUIDINFO{Uuid: img_uuid}, AgentUuid: agentUuid, ETCDINFO: ETCDINFO{Type: "img"}}}
	path := img.GetKey()
	var ret RES
	if ret, err = LoadRes(path, img); err != nil {
		return nil, err
	}
	if ret != nil {
		img, ok := ret.(*Img)
		if ok {
			return img, nil
		}
	}
	return nil, nil
}

func LoadImg3(rests RESTS, name string) (img *Img, err error) {
	for _, rest := range rests["img"] {
		img, ok := rest.(*Img)
		if !ok {
			continue
		}
		if img.AgentUuid != G.config.Uuid {
			continue
		}
		if img.Name == name {
			return img, nil
		}
	}
	return nil, nil
}

func (img *Img) Add(pool *Pool) (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()
	if pool.Stype == "fs" {
		backingDir := pool.ParaMap["DIR"] + "/backing"
		if err := osMkdir(backingDir); err != nil {
			return err
		}
		fileName := pool.ParaMap["DIR"] + "/backing/" + img.Name + ".qcow2"
		if ok, _ := FileExists(fileName); !ok {
			return fmt.Errorf("image file does not exist")
		}
		info, err := GetQemuImgInfo(fileName)
		if err != nil {
			return err
		}
		if info.BackingFile != "" {
			return fmt.Errorf("image file should not have backing file")
		}
		if info.Format != "qcow2" {
			return fmt.Errorf("image file is not qcow2")
		}
		img.Size = BytesToGB(info.Size)
	}
	return nil
}

func (img *Img) Del(pool *Pool, force bool) (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()
	if pool.Stype != "fs" {
		return fmt.Errorf("pool type is not fs")
	}
	fileName := pool.ParaMap["DIR"] + "/backing/" + img.Name + ".qcow2"
	if force {
		if ok, _ := FileExists(fileName); !ok {
			return nil
		}
		if err := os.Remove(fileName); err != nil {
			return err
		}
	}
	return nil
}

func (img *Img) AddingImgTask(pool *Pool) {
	var err error
	success := false
	if err = img.Add(pool); err != nil {
		G.logger.Error("cannot add img", zap.Error(err))
		goto UPDATE
	}
	success = true
UPDATE:
	if _, err = UpdatePhaseStop(img, success); err != nil {
		G.logger.Error("cannot update img phase", zap.Error(err))
	}
}

func (img *Img) DeletingImgTask(pool *Pool, force bool) {

	err := img.Del(pool, force)
	if err != nil {

		G.logger.Error("cannot delete img", zap.Error(err))
		if _, err = UpdatePhaseStop(img, false); err != nil {
			G.logger.Error("cannot update img phase", zap.Error(err))
			return
		}
	} else {
		for {
			delete(pool.Imgs, img.Uuid)

			var done bool
			done, err = Save([]RES{img}, []RES{pool})
			if err != nil {
				G.logger.Error("cannot save res", zap.Error(err))
				return
			}
			if done {
				return
			}
		}
	}
}

func (img *Img) VerifyUuid() error {
	return AssignUuid("img", img)
}

func (img *Img) VerifyImg(pool *Pool) error {
	if img.Name == "" {
		return fmt.Errorf("image name is required")
	}
	if img.Name != path.Base(img.Name) {
		return fmt.Errorf("image name is invalid")
	}
	for k, v := range pool.Imgs {
		if k != img.Uuid && v == img.Name {
			return fmt.Errorf("image name already exists")
		}
	}
	fileName := pool.ParaMap["DIR"] + "/backing/" + img.Name + ".qcow2"
	if ok, _ := FileExists(fileName); !ok {
		return fmt.Errorf("image file does not exist")
	}
	pool.Imgs[img.Uuid] = img.Name
	return nil
}

func ImgCmdParser(command *cobra.Command) {
	var err error
	var imgCmd = &cobra.Command{
		Use:   "img",
		Short: "Img management",
		Run: func(cmd *cobra.Command, args []string) {
			fmt.Println("please specify a subcommand for img management. use -h for help.")
		},
	}
	var imgAddCmd = &cobra.Command{
		Use:   "add",
		Short: "Add a Img",
		Run:   ImgAddHandle,
	}
	imgAddCmd.Flags().StringVarP(&imgFlags.uuid, "uuid", "U", "", "set the UUID of the Pool")
	if err = imgAddCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	imgAddCmd.Flags().StringVar(&imgFlags.name, "name", "", "set the name of the Img")
	if err = imgAddCmd.MarkFlagRequired("name"); err != nil {
		panic(err)
	}
	imgAddCmd.Flags().StringVar(&imgFlags.desc, "desc", "", "set the description of the Img")
	var imgGetCmd = &cobra.Command{
		Use:   "get",
		Short: "Get a Img",
		Run:   ImgGetHandle,
	}
	imgGetCmd.Flags().StringVarP(&imgFlags.uuid, "uuid", "U", "", "set the UUID of the Img")
	if err = imgGetCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var imgDelCmd = &cobra.Command{
		Use:   "del",
		Short: "Delete a Img",
		Run:   ImgDelHandle,
	}
	imgDelCmd.Flags().BoolVar(&imgFlags.force, "force", false, "delete the Img forcefully")
	imgDelCmd.Flags().StringVarP(&imgFlags.uuid, "uuid", "U", "", "set the UUID of the Img")
	if err = imgDelCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var imgListCmd = &cobra.Command{
		Use:   "list",
		Short: "List all Imgs",
		Run:   ImgListHandle,
	}
	imgListCmd.Flags().BoolVar(&imgFlags.abnormal, "abnormal", false, "list abnormal Imgs")
	imgListCmd.Flags().StringVarP(&imgFlags.uuid, "uuid", "U", "", "set the UUID of the Img")
	if err = imgGetCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	imgCmd.AddCommand(imgAddCmd, imgGetCmd, imgDelCmd, imgListCmd)
	command.AddCommand(imgCmd)
}

func ImgAddHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/pools/%s/imgs", G.Host, G.Port, imgFlags.uuid)
	data := map[string]interface{}{"name": imgFlags.name, "desc": imgFlags.desc}
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

func ImgGetHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/imgs/%s", G.Host, G.Port, imgFlags.uuid)
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

func ImgDelHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/imgs/%s", G.Host, G.Port, imgFlags.uuid)
	data := map[string]interface{}{}
	if imgFlags.force {
		data["force"] = true
	}
	jsonBytes, _ := json.Marshal(data)
	req, _ := http.NewRequest("DELETE", url, bytes.NewBuffer(jsonBytes))
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

func ImgListHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/pools/%s/imgs", G.Host, G.Port, imgFlags.uuid)
	data := map[string]interface{}{}
	if imgFlags.abnormal {
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

func ImgAppSetup() {
	G.echoServer.POST("/v1/pools/:uuid/imgs", AppImgAdd)
	G.echoServer.GET("/v1/pools/:uuid/imgs", AppImgList)
	G.echoServer.GET("/v1/imgs/:uuid", AppImgGet)
	G.echoServer.DELETE("/v1/imgs/:uuid", AppImgDel)
}

func AppImgAdd(c echo.Context) (err error) {
	G.logger.Debug("=========AppImgAdd==========")
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
	q := ImgAddReq{}
	if err = c.Bind(&q); err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Cannot parse the request."))
	}
	var img *Img
	for {
		if pool.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Pool is not ready."))
		}
		img = &Img{Vols: []string{}, AGENTREST: AGENTREST{AgentUuid: G.config.Uuid, ETCDINFO: ETCDINFO{Type: "img"}}}
		img.Name = q.Name
		img.Desc = q.Desc
		img.PoolUuid = uuid
		_ = img.PhaseStart(PhaseTypeAdd)
		if err = img.VerifyUuid(); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		if err = img.VerifyImg(pool); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		var done bool
		done, err = Save(nil, []RES{img, pool})
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	go img.AddingImgTask(pool)
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(img.Uuid))
}

func AppImgList(c echo.Context) (err error) {
	G.logger.Debug("=========AppImgList==========")
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
	imgUuids := []string{}
	rests := LoadRests("img")
	for imgUuid := range pool.Imgs {
		i := &Img{AGENTREST: AGENTREST{UUIDINFO: UUIDINFO{Uuid: imgUuid}, AgentUuid: G.config.Uuid, ETCDINFO: ETCDINFO{Type: "img"}}}
		if rest := GetRest(rests, "img", i.GetKey()); rest != nil {
			img, ok := rest.(*Img)
			if !ok {
				continue
			}
			if q.Abnormal {
				if img.NotReady() {
					imgUuids = append(imgUuids, img.Uuid)
				}
			} else {
				imgUuids = append(imgUuids, img.Uuid)
			}
		}
	}
	jsonMap := make(map[string]interface{})
	jsonMap["status"] = "ok"
	jsonMap["img_count"] = len(imgUuids)
	jsonMap["imgs"] = imgUuids
	return c.JSON(http.StatusOK, jsonMap)
}

func AppImgGet(c echo.Context) (err error) {
	G.logger.Debug("=========AppImgGet==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Image UUID is required."))
	}
	var img *Img
	if img, err = LoadImg(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if img == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Image not found."))
	}
	rsp := ImgGetRsp{
		Status:    "ok",
		Name:      img.Name,
		Desc:      img.Desc,
		Size:      img.Size,
		Vols:      img.Vols,
		AgentUuid: img.AgentUuid,
		PoolUuid:  img.PoolUuid,
		AGENTREST: img.AGENTREST,
		PHASEINFO: img.PHASEINFO}
	return c.JSON(http.StatusOK, &rsp)
}

func AppImgDel(c echo.Context) (err error) {
	G.logger.Debug("=========AppImgDel==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Image UUID is required."))
	}
	var img *Img
	if img, err = LoadImg(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if img == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Image not found."))
	}
	var pool *Pool
	if pool, err = LoadPool(img.PoolUuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if pool == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Pool not found."))
	}
	q := ImgDelReq{}
	if err = c.Bind(&q); err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
	}
	for {
		if len(img.Vols) > 0 {
			return c.JSON(http.StatusForbidden, NewAppErrorRsp("Image has volumes attached."))
		}
		if err = img.PhaseStart(PhaseTypeDel); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		var done bool
		done, err = Save(nil, []RES{img, pool})
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	go img.DeletingImgTask(pool, q.Force)
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(img.Uuid))
}
