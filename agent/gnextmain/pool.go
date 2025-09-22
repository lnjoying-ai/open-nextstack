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
	"strings"

	"github.com/labstack/echo"
	"github.com/spf13/cobra"
	"go.uber.org/zap"
)

type Pool struct {
	Sid   string `json:"sid"`
	Stype string `json:"stype"`
	Paras string `json:"cidr"`

	Vols    []string          `json:"vols"`
	Imgs    map[string]string `json:"imgs"`
	ParaMap map[string]string `json:"para_map"`
	AGENTREST
	PHASEINFO
}

type PoolFlags struct {
	uuid     string
	abnormal bool
	sid      string
	stype    string
	paras    string
}

type PoolAddReq struct {
	Sid   string `json:"sid"`
	Stype string `json:"stype"`
	Paras string `json:"paras"`
}

type PoolGetRsp struct {
	Status    string   `json:"status"`
	Sid       string   `json:"sid"`
	Stype     string   `json:"stype"`
	Paras     string   `json:"paras"`
	AgentUuid string   `json:"agent"`
	Vols      []string `json:"vols"`
	Imgs      []string `json:"imgs"`
	AGENTREST
	PHASEINFO
}

var poolFlags PoolFlags

func LoadPool(pool_uuid string) (pool *Pool, err error) {
	pool = &Pool{AGENTREST: AGENTREST{UUIDINFO: UUIDINFO{Uuid: pool_uuid}, AgentUuid: G.config.Uuid, ETCDINFO: ETCDINFO{Type: "pool"}}}
	path := pool.GetKey()
	var ret RES
	if ret, err = LoadRes(path, pool); err != nil {
		return nil, err
	}
	if ret != nil {
		pool, ok := ret.(*Pool)
		if ok {
			return pool, nil
		}
	}
	return nil, nil
}

func LoadPool2(pool_uuid string, agentUuid string) (pool *Pool, err error) {
	pool = &Pool{AGENTREST: AGENTREST{UUIDINFO: UUIDINFO{Uuid: pool_uuid}, AgentUuid: agentUuid, ETCDINFO: ETCDINFO{Type: "pool"}}}
	path := pool.GetKey()
	var ret RES
	if ret, err = LoadRes(path, pool); err != nil {
		return nil, err
	}
	if ret != nil {
		pool, ok := ret.(*Pool)
		if ok {
			return pool, nil
		}
	}
	return nil, nil
}

func LoadPool3(rests RESTS, sid string) (pool *Pool, err error) {
	for _, rest := range rests["pool"] {
		pool, ok := rest.(*Pool)
		if !ok {
			continue
		}
		if pool.AgentUuid != G.config.Uuid {
			continue
		}
		if pool.Sid == sid {
			return pool, nil
		}
	}
	return nil, nil
}

func (pool *Pool) Add() (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()
	dir := pool.ParaMap["DIR"]
	if dir == "" {
		return fmt.Errorf("dir is required")
	}
	if exists, _ := DirExists(dir); !exists {
		return fmt.Errorf("dir %s does not exist", dir)
	}
	subdirs := []string{"backing", "volumes", "tokens"}
	for _, subdir := range subdirs {
		subdirPath := dir + "/" + subdir
		if exists, _ := DirExists(subdirPath); !exists {
			if err = os.Mkdir(subdirPath, 0755); err != nil {
				return err
			}
		}
		if err = osChown2Qemu(subdirPath); err != nil {
			return err
		}
	}
	return nil
}

func (pool *Pool) Del() (err error) {
	G.sysLock.Lock()
	defer G.sysLock.Unlock()
	return nil
}

func (pool *Pool) Restore() (err error) {
	if err = pool.Add(); err != nil {
		return err
	}
	return nil
}

func (pool *Pool) AddingPoolTask() {
	success := false
	err := pool.Add()
	if err != nil {
		G.logger.Error("cannot add pool", zap.Error(err))
		goto UPDATE
	}
	success = true
UPDATE:
	if _, err = UpdatePhaseStop(pool, success); err != nil {
		G.logger.Error("cannot update pool phase", zap.Error(err))
	}
}

func (pool *Pool) DeletingPoolTask() {

	err := pool.Del()
	if err != nil {

		G.logger.Error("cannot delete pool", zap.Error(err))
		if _, err = UpdatePhaseStop(pool, false); err != nil {
			G.logger.Error("cannot update pool phase", zap.Error(err))
			return
		}
	} else {
		for {

			var done bool
			done, err = Save([]RES{pool}, nil)
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

func (pool *Pool) VerifyUuid() error {
	return AssignUuid("pool", pool)
}

func (pool *Pool) VerifyPoolParas(pools *POOLS) error {
	if pool.Sid == "" {
		return fmt.Errorf("sid is required")
	}
	if pool.Paras == "" {
		return fmt.Errorf("paras is required")
	}
	if pool.Stype == "" {
		return fmt.Errorf("type is required")
	}
	if pool.Stype != "fs" {
		return fmt.Errorf("only fs is supported now")
	}
	kvs := ParseKVs(pool.Paras)
	if kvs["DIR"] == "" {
		return fmt.Errorf("dir is required")
	}
	if !strings.HasPrefix(kvs["DIR"], "/") {
		return fmt.Errorf("dir must be an absolute path")
	}
	if exists, _ := DirExists(kvs["DIR"]); !exists {
		return fmt.Errorf("dir %s does not exist", kvs["DIR"])
	}
	pool.ParaMap = kvs
	if pools.DirInfo[pool.AgentUuid] == nil {
		pools.DirInfo[pool.AgentUuid] = make(map[string]string)
	}
	if pools.SidInfo[pool.AgentUuid] == nil {
		pools.SidInfo[pool.AgentUuid] = make(map[string]string)
	}
	if _, ok := pools.DirInfo[pool.AgentUuid][kvs["DIR"]]; ok {
		if pools.DirInfo[pool.AgentUuid][kvs["DIR"]] != pool.Uuid {
			return fmt.Errorf("pool dir already exists in the agent")
		}
	}
	if _, ok := pools.SidInfo[pool.AgentUuid][pool.Sid]; ok {
		if pools.SidInfo[pool.AgentUuid][pool.Sid] != pool.Uuid {
			return fmt.Errorf("pool sid already exists in the agent")
		}
	}
	pools.DirInfo[pool.AgentUuid][kvs["DIR"]] = pool.Uuid
	pools.SidInfo[pool.AgentUuid][pool.Sid] = pool.Uuid
	return nil
}

func PoolCmdParser(command *cobra.Command) {
	var err error
	var poolCmd = &cobra.Command{
		Use:   "pool",
		Short: "Pool management",
		Run: func(cmd *cobra.Command, args []string) {
			fmt.Println("please specify a subcommand for pool management. use -h for help.")
		},
	}
	var poolAddCmd = &cobra.Command{
		Use:   "add",
		Short: "Add a Pool",
		Run:   PoolAddHandle,
	}
	poolAddCmd.Flags().StringVar(&poolFlags.sid, "sid", "", "set the uniq share ID of the Pool")
	if err = poolAddCmd.MarkFlagRequired("sid"); err != nil {
		panic(err)
	}
	poolAddCmd.Flags().StringVar(&poolFlags.stype, "type", "", "set the type of the Pool")
	if err = poolAddCmd.MarkFlagRequired("type"); err != nil {
		panic(err)
	}
	poolAddCmd.Flags().StringVar(&poolFlags.paras, "paras", "", "set the parameters of the Pool")
	if err = poolAddCmd.MarkFlagRequired("paras"); err != nil {
		panic(err)
	}
	var poolGetCmd = &cobra.Command{
		Use:   "get",
		Short: "Get a Pool",
		Run:   PoolGetHandle,
	}
	poolGetCmd.Flags().StringVarP(&poolFlags.uuid, "uuid", "U", "", "set the UUID of the Pool")
	if err = poolGetCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var poolDelCmd = &cobra.Command{
		Use:   "del",
		Short: "Delete a Pool",
		Run:   PoolDelHandle,
	}
	poolDelCmd.Flags().StringVarP(&poolFlags.uuid, "uuid", "U", "", "set the UUID of the Pool")
	if err = poolDelCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var poolListCmd = &cobra.Command{
		Use:   "list",
		Short: "List all Pools",
		Run:   PoolListHandle,
	}
	poolListCmd.Flags().BoolVar(&poolFlags.abnormal, "abnormal", false, "list abnormal Pools")
	poolCmd.AddCommand(poolAddCmd, poolGetCmd, poolDelCmd, poolListCmd)
	command.AddCommand(poolCmd)
}

func PoolAddHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/pools", G.Host, G.Port)
	data := map[string]interface{}{"sid": poolFlags.sid, "stype": poolFlags.stype, "paras": poolFlags.paras}
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

func PoolGetHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/pools/%s", G.Host, G.Port, poolFlags.uuid)
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

func PoolDelHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/pools/%s", G.Host, G.Port, poolFlags.uuid)
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

func PoolListHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/pools", G.Host, G.Port)
	data := map[string]interface{}{}
	if poolFlags.abnormal {
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

func PoolAppSetup() {
	G.echoServer.POST("/v1/pools", AppPoolAdd)
	G.echoServer.GET("/v1/pools", AppPoolList)
	G.echoServer.GET("/v1/pools/:uuid", AppPoolGet)
	G.echoServer.DELETE("/v1/pools/:uuid", AppPoolDel)
}

func AppPoolAdd(c echo.Context) (err error) {
	G.logger.Debug("=========AppPoolAdd==========")
	q := PoolAddReq{}
	if err = c.Bind(&q); err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Cannot parse the request."))
	}
	var pool *Pool
	pools, _ := LoadPools()
	for {
		pool = &Pool{Vols: []string{}, Imgs: make(map[string]string), AGENTREST: AGENTREST{AgentUuid: G.config.Uuid, ETCDINFO: ETCDINFO{Type: "pool"}}}
		pool.Sid = q.Sid
		pool.Stype = q.Stype
		pool.Paras = q.Paras
		_ = pool.PhaseStart(PhaseTypeAdd)
		if err = pool.VerifyUuid(); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		if err = pool.VerifyPoolParas(pools); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		var done bool
		done, err = Save(nil, []RES{pool, pools})
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	go pool.AddingPoolTask()
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(pool.Uuid))
}

func AppPoolList(c echo.Context) (err error) {
	G.logger.Debug("=========AppPoolList==========")
	q := AppListReq{}
	if err = c.Bind(&q); err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
	}
	poolUuids := []string{}
	rests := LoadRests("pool")
	for _, rest := range rests["pool"] {
		pool, ok := rest.(*Pool)
		if !ok {
			continue
		}
		if pool.AgentUuid != G.config.Uuid {
			continue
		}
		if q.Abnormal {
			if pool.NotReady() {
				poolUuids = append(poolUuids, pool.Uuid)
			}
		} else {
			poolUuids = append(poolUuids, pool.Uuid)
		}
	}
	jsonMap := make(map[string]interface{})
	jsonMap["status"] = "ok"
	jsonMap["pool_count"] = len(poolUuids)
	jsonMap["pools"] = poolUuids
	return c.JSON(http.StatusOK, jsonMap)
}

func AppPoolGet(c echo.Context) (err error) {
	G.logger.Debug("=========AppPoolGet==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Pool UUID is required."))
	}
	var pool *Pool
	if pool, err = LoadPool(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if pool == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Pool not found."))
	}
	rsp := PoolGetRsp{
		Status:    "ok",
		Sid:       pool.Sid,
		Stype:     pool.Stype,
		Paras:     pool.Paras,
		AgentUuid: pool.AgentUuid,
		Vols:      pool.Vols,
		Imgs:      GetKeys(pool.Imgs),
		AGENTREST: pool.AGENTREST,
		PHASEINFO: pool.PHASEINFO}
	return c.JSON(http.StatusOK, &rsp)
}

func AppPoolDel(c echo.Context) (err error) {
	G.logger.Debug("=========AppPoolDel==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Pool UUID is required."))
	}
	var pool *Pool
	if pool, err = LoadPool(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if pool == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Pool not found."))
	}
	pools, _ := LoadPools()
	for {
		if len(pool.Vols) > 0 {
			return c.JSON(http.StatusForbidden, NewAppErrorRsp("Pool contains volumes."))
		}
		if len(pool.Imgs) > 0 {
			return c.JSON(http.StatusForbidden, NewAppErrorRsp("Pool contains images."))
		}
		if err = pool.PhaseStart(PhaseTypeDel); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		if pools.DirInfo[pool.AgentUuid] != nil {
			delete(pools.DirInfo[pool.AgentUuid], pool.ParaMap["DIR"])
		}
		if pools.SidInfo[pool.AgentUuid] != nil {
			delete(pools.SidInfo[pool.AgentUuid], pool.Sid)
		}
		var done bool
		done, err = Save(nil, []RES{pool, pools})
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	go pool.DeletingPoolTask()
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(pool.Uuid))
}
