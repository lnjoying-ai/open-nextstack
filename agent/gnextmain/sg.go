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
)

type SG struct {
	Rules    []*Rule             `json:"rules0"`
	Ports    []string            `json:"ports"`
	Addrs    map[string][]string `json:"addrs"`
	RuleStrs []string            `json:"rules"`
	SHAREREST
	PHASEINFO
}

type Rule struct {
	Direction string `json:"direction"`
	Priority  uint32 `json:"priority"`
	Protocol  string `json:"protocol"`
	Ports     string `json:"ports"`
	Addr      string `json:"addr"`
	Action    string `json:"action"`
	PhaseTime uint32 `json:"phase_time"`
}

type SgFlags struct {
	uuid     string
	abnormal bool
	rules    []string
}

type SgUpdateReq struct {
	Rules []string `json:"rules"`
}

type SgGetRsp struct {
	Status   string   `json:"status"`
	Ports    []string `json:"ports"`
	RuleStrs []string `json:"rules"`
	SHAREREST
	PHASEINFO
}

var sgFlags = &SgFlags{}

func LoadSG(sg_uuid string) (sg *SG, err error) {
	sg = &SG{SHAREREST: SHAREREST{UUIDINFO: UUIDINFO{Uuid: sg_uuid}, ETCDINFO: ETCDINFO{Type: "sg"}}}
	path := sg.GetKey()
	var ret RES
	if ret, err = LoadRes(path, sg); err != nil {
		return nil, err
	}
	if ret != nil {
		sg, ok := ret.(*SG)
		if ok {
			return sg, nil
		}
	}
	return nil, nil
}

func (sg *SG) VerifyUuid() error {
	return AssignUuid("sg", sg)
}

func (sg *SG) VerifySgRules() (err error) {
	sg.Rules = make([]*Rule, 0, len(sg.RuleStrs))
	for _, ruleStr := range sg.RuleStrs {
		kvs := ParseKVs(ruleStr)
		if kvs["DIR"] != "in" && kvs["DIR"] != "out" {
			return fmt.Errorf("invalid direction")
		}
		if kvs["PRIORITY"] == "" {
			return fmt.Errorf("invalid priority")
		}
		var p uint32
		if p, err = AtoU32(kvs["PRIORITY"]); err != nil {
			return fmt.Errorf("invalid priority")
		}
		if p > 65535 {
			return fmt.Errorf("invalid priority")
		}
		if kvs["PROTOCOL"] == "" {
			kvs["PROTOCOL"] = "all"
		}
		if kvs["PROTOCOL"] != "all" && kvs["PROTOCOL"] != "tcp" && kvs["PROTOCOL"] != "udp" && kvs["PROTOCOL"] != "icmp" {
			return fmt.Errorf("invalid protocol")
		}
		if kvs["PORT"] == "" {
			kvs["PORT"] = "all"
		}
		if kvs["PROTOCOL"] == "all" && kvs["PORT"] != "all" {
			return fmt.Errorf("invalid port")
		}
		var port string
		port, err = ParseSgPort(kvs["PROTOCOL"], kvs["PORT"])
		if err != nil {
			return err
		}
		kvs["PORT"] = port
		if kvs["ADDR"] == "" {
			kvs["ADDR"] = "all"
		} else {
			err = VerifySgAddr(kvs["ADDR"])
			if err != nil {
				return err
			}
		}
		if kvs["ACTION"] == "" {
			kvs["ACTION"] = "accept"
		} else if kvs["ACTION"] != "accept" && kvs["ACTION"] != "drop" {
			return fmt.Errorf("invalid action")
		}
		rule := Rule{
			Direction: kvs["DIR"],
			Priority:  p,
			Protocol:  kvs["PROTOCOL"],
			Ports:     kvs["PORT"],
			Addr:      kvs["ADDR"],
			Action:    kvs["ACTION"],
		}
		sg.Rules = append(sg.Rules, &rule)
	}
	return nil
}

func SgCmdParser(command *cobra.Command) {
	var err error
	var sgCmd = &cobra.Command{
		Use:   "sg",
		Short: "SG management",
		Run: func(cmd *cobra.Command, args []string) {
			fmt.Println("please specify a subcommand for sg management. use -h for help.")
		},
	}
	var SgAddCmd = &cobra.Command{
		Use:   "add",
		Short: "Add a SG",
		Run:   SgAddHandle,
	}
	var SgGetCmd = &cobra.Command{
		Use:   "get",
		Short: "Get a SG",
		Run:   SgGetHandle,
	}
	SgGetCmd.Flags().StringVarP(&sgFlags.uuid, "uuid", "U", "", "set the UUID of the sg")
	if err = SgGetCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var SgDelCmd = &cobra.Command{
		Use:   "del",
		Short: "Delete a SG",
		Run:   SgDelHandle,
	}
	SgDelCmd.Flags().StringVarP(&sgFlags.uuid, "uuid", "U", "", "set the UUID of the sg")
	if err = SgDelCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var SgListCmd = &cobra.Command{
		Use:   "list",
		Short: "List all SGs",
		Run:   SgListHandle,
	}
	SgListCmd.Flags().BoolVar(&sgFlags.abnormal, "abnormal", false, "list abnormal sgs")
	var SgUpdateCmd = &cobra.Command{
		Use:   "update",
		Short: "Update rules of a SG",
		Run:   SgUpdateHandle,
	}
	SgUpdateCmd.Flags().StringVarP(&sgFlags.uuid, "uuid", "U", "", "set the UUID of the SG")
	if err = SgUpdateCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	SgUpdateCmd.Flags().StringArrayVar(&sgFlags.rules, "rule", nil, "set the rules of the SG")
	sgCmd.AddCommand(SgAddCmd, SgDelCmd, SgUpdateCmd, SgGetCmd, SgListCmd)
	command.AddCommand(sgCmd)
}

func SgAddHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/sgs", G.Host, G.Port)
	req, _ := http.NewRequest("POST", url, nil)
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

func SgGetHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/sgs/%s", G.Host, G.Port, sgFlags.uuid)
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

func SgDelHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/sgs/%s", G.Host, G.Port, sgFlags.uuid)
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

func SgListHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/sgs", G.Host, G.Port)
	data := map[string]interface{}{}
	if sgFlags.abnormal {
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

func SgUpdateHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/sgs/%s/update", G.Host, G.Port, sgFlags.uuid)
	data := SgUpdateReq{Rules: sgFlags.rules}
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

func SgAppSetup() {
	if !G.config.L3Mode {
		return
	}
	G.echoServer.POST("/v1/sgs", AppSgAdd)
	G.echoServer.DELETE("/v1/sgs/:uuid", AppSgDel)
	G.echoServer.PUT("/v1/sgs/:uuid/update", AppSgUpdate)
	G.echoServer.GET("/v1/sgs", AppSgList)
	G.echoServer.GET("/v1/sgs/:uuid", AppSgGet)
}

func AppSgAdd(c echo.Context) (err error) {
	G.logger.Debug("=========AppSgAdd==========")
	if !G.config.MasterL3 {
		return c.JSON(http.StatusForbidden, NewAppErrorRsp("This node is not a master L3 node."))
	}
	var sg *SG
	for {
		sg = &SG{Rules: []*Rule{}, Ports: []string{}, RuleStrs: []string{}, Addrs: map[string][]string{}, SHAREREST: SHAREREST{ETCDINFO: ETCDINFO{Type: "sg"}}}
		_ = sg.PhaseStart(PhaseTypeAdd)
		sg.PhaseStop(true)
		if err = sg.VerifyUuid(); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		var done bool
		done, err = Save(nil, []RES{sg})
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	return c.JSON(http.StatusOK, AppOKRsp(sg.Uuid))
}

func AppSgList(c echo.Context) (err error) {
	G.logger.Debug("=========AppSgList==========")
	q := AppListReq{}
	if err = c.Bind(&q); err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
	}
	rests := LoadRests("sg")
	sgUuids := []string{}
	for _, rest := range rests["sg"] {
		sg, ok := rest.(*SG)
		if !ok {
			continue
		}
		if q.Abnormal {
			if sg.NotReady() {
				sgUuids = append(sgUuids, sg.Uuid)
			}
		} else {
			sgUuids = append(sgUuids, sg.Uuid)
		}
	}
	jsonMap := make(map[string]interface{})
	jsonMap["status"] = "ok"
	jsonMap["sg_count"] = len(sgUuids)
	jsonMap["sgs"] = sgUuids
	return c.JSON(http.StatusOK, jsonMap)
}

func AppSgGet(c echo.Context) (err error) {
	G.logger.Debug("=========AppSgGet==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Security group UUID is required."))
	}
	var sg *SG
	if sg, err = LoadSG(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if sg == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Security group not found."))
	}
	rsp := SgGetRsp{
		Status:    "ok",
		RuleStrs:  sg.RuleStrs,
		Ports:     sg.Ports,
		SHAREREST: sg.SHAREREST,
		PHASEINFO: sg.PHASEINFO}
	return c.JSON(http.StatusOK, &rsp)
}

func AppSgDel(c echo.Context) (err error) {
	G.logger.Debug("=========AppSgDel==========")
	if !G.config.MasterL3 {
		return c.JSON(http.StatusForbidden, NewAppErrorRsp("This node is not a master L3 node."))
	}
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Security group UUID is required."))
	}
	var sg *SG
	if sg, err = LoadSG(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if sg == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Security group not found."))
	}
	for {
		if len(sg.Ports) > 0 {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Security group is in use."))
		}
		var done bool
		done, err = Save([]RES{sg}, nil)
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	jsonMap := make(map[string]interface{})
	jsonMap["status"] = "ok"
	return c.JSON(http.StatusOK, jsonMap)
}

func AppSgUpdate(c echo.Context) (err error) {
	G.logger.Debug("=========AppSgUpdate==========")
	if !G.config.MasterL3 {
		return c.JSON(http.StatusForbidden, NewAppErrorRsp("This node is not a master L3 node."))
	}
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Security group UUID is required."))
	}
	q := &SgUpdateReq{}
	if err = c.Bind(q); err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Cannot parse the request."))
	}
	var sg *SG
	if sg, err = LoadSG(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if sg == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Security group not found."))
	}
	for {
		if sg.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Security group is not ready."))
		}
		sg.RuleStrs = q.Rules
		if err = sg.VerifySgRules(); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		if err = sg.PhaseStart(PhaseTypeUpdate); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		sg.PhaseStop(true)

		reses := []RES{sg}
		hostSGs := map[string]*HOSTSGT{}
		for _, portUuid := range sg.Ports {
			if _, ok := hostSGs[portUuid]; !ok {
				var port *Port
				if port, err = LoadPort(portUuid); err != nil {
					continue
				}
				if port == nil {
					continue
				}
				hostsg, _ := LoadHostSG(port.AgentUuid)
				hostSGs[port.AgentUuid] = hostsg
			}
		}
		for _, hostsg := range hostSGs {
			reses = append(reses, hostsg)
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
	return c.JSON(http.StatusOK, AppOKRsp(sg.Uuid))
}
