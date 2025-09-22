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
	"net"
	"net/http"
	"strings"

	"github.com/labstack/echo"
	"github.com/spf13/cobra"
	"go.uber.org/zap"
)

type Host struct {
	Hostname string `json:"hostname"`
	Ip       string `json:"ip"`
	VpcUuid  string `json:"vpc"`
	SHAREREST
	PHASEINFO
}

type HostFlags struct {
	uuid     string
	hostname string
	ip       string
	abnormal bool
}

type HostAddReq struct {
	Hostname string `json:"hostname"`
	Ip       string `json:"ip"`
}

type HostGetRsp struct {
	Status   string `json:"status"`
	Hostname string `json:"hostname"`
	Ip       string `json:"ip"`
	VpcUuid  string `json:"vpc"`
	SHAREREST
	PHASEINFO
}

var hostFlags HostFlags

func LoadHost(host_uuid string) (host *Host, err error) {
	host = &Host{SHAREREST: SHAREREST{UUIDINFO: UUIDINFO{Uuid: host_uuid}, ETCDINFO: ETCDINFO{Type: "host"}}}
	path := host.GetKey()
	var ret RES
	if ret, err = LoadRes(path, host); err != nil {
		return nil, err
	}
	if ret != nil {
		host, ok := ret.(*Host)
		if ok {
			return host, nil
		}
	}
	return nil, nil
}

func (host *Host) AddingHostTask(vpc *VPC) {
	if _, err := UpdatePhaseStop(host, true); err != nil {
		G.logger.Error("cannot update host phase", zap.Error(err))
		return
	}
	localRes, err := vpc.AssignLocalRes(true)
	if err != nil {
		G.logger.Error("cannot assign local resource", zap.Error(err))
		return
	}
	vlanid := localRes[0]
	NoticeDnsmasq(vlanid)
}

func (host *Host) DeletingHostTask(vpc *VPC) {
	var done bool
	var err error
	for {
		delete(vpc.Hosts, host.Uuid)
		done, err = Save([]RES{host}, []RES{vpc})
		if err != nil {
			G.logger.Error("cannot save host", zap.Error(err))
			return
		}
		if done {
			localRes, err := vpc.AssignLocalRes(true)
			if err != nil {
				G.logger.Error("cannot load local resource", zap.Error(err))
			}
			vlanid := localRes[0]
			NoticeDnsmasq(vlanid)
			break
		}
	}
}

func (host *Host) VerifyUuid() error {
	return AssignUuid("host", host)
}

func (host *Host) VerifyHostname(vpc *VPC) (err error) {
	if strings.HasPrefix(host.Hostname, ".") || strings.HasSuffix(host.Hostname, ".") {
		return fmt.Errorf("wong format of hostname %s", host.Hostname)
	}
	if len(host.Hostname) > 255 {
		return fmt.Errorf("lenght of hostname %s should less than 256", host.Hostname)
	}
	parts := strings.Split(host.Hostname, ".")
	for _, part := range parts {
		if !isValidHostnameSegment(part) {
			return fmt.Errorf("wrong format of hostname %s", host.Hostname)
		}
	}
	for hostUuid, hostname := range vpc.Hosts {
		if hostname == host.Hostname && hostUuid != host.Uuid {
			return fmt.Errorf("hostname %s is already used", host.Hostname)
		}
	}
	if vpc.Hosts == nil {
		vpc.Hosts = make(map[string]string)
	}
	vpc.Hosts[host.Uuid] = host.Hostname
	return nil
}

func (host *Host) VerifyHostIp() (err error) {
	if host.Ip == "" {
		return fmt.Errorf("ip address is required")
	}
	if net.ParseIP(host.Ip) == nil {
		return fmt.Errorf("invalid host ip")
	}
	return nil
}

func HostCmdParser(command *cobra.Command) {
	var err error
	var hostCmd = &cobra.Command{
		Use:   "host",
		Short: "Host management",
		Run: func(cmd *cobra.Command, args []string) {
			fmt.Println("please specify a subcommand for host management. use -h for help.")
		},
	}
	var hostAddCmd = &cobra.Command{
		Use:   "add",
		Short: "Add a Host",
		Run:   HostAddHandle,
	}
	hostAddCmd.Flags().StringVar(&hostFlags.hostname, "hostname", "", "set the hostname of the host")
	if err = hostAddCmd.MarkFlagRequired("hostname"); err != nil {
		panic(err)
	}
	hostAddCmd.Flags().StringVar(&hostFlags.ip, "ip", "", "set the IP of the host")
	if err = hostAddCmd.MarkFlagRequired("ip"); err != nil {
		panic(err)
	}
	hostAddCmd.Flags().StringVarP(&hostFlags.uuid, "uuid", "U", "", "set the UUID of the VPC")
	if err = hostAddCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var hostGetCmd = &cobra.Command{
		Use:   "get",
		Short: "Get a Host",
		Run:   HostGetHandle,
	}
	hostGetCmd.Flags().StringVarP(&hostFlags.uuid, "uuid", "U", "", "set the UUID of the host")
	if err = hostGetCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var hostDelCmd = &cobra.Command{
		Use:   "del",
		Short: "Delete a Host",
		Run:   HostDelHandle,
	}
	hostDelCmd.Flags().StringVarP(&hostFlags.uuid, "uuid", "U", "", "set the UUID of the host")
	if err = hostDelCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	var hostListCmd = &cobra.Command{
		Use:   "list",
		Short: "List all Hosts",
		Run:   HostListHandle,
	}
	hostListCmd.Flags().BoolVar(&hostFlags.abnormal, "abnormal", false, "list abnormal hosts")
	hostListCmd.Flags().StringVarP(&hostFlags.uuid, "uuid", "U", "", "set the UUID of the VPC")
	if err = hostGetCmd.MarkFlagRequired("uuid"); err != nil {
		panic(err)
	}
	hostCmd.AddCommand(hostAddCmd, hostDelCmd, hostGetCmd, hostListCmd)
	command.AddCommand(hostCmd)
}

func HostAddHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/vpcs/%s/hosts", G.Host, G.Port, hostFlags.uuid)
	data := map[string]interface{}{"hostname": hostFlags.hostname, "ip": hostFlags.ip}
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

func HostGetHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/hosts/%s", G.Host, G.Port, hostFlags.uuid)
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

func HostDelHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/hosts/%s", G.Host, G.Port, hostFlags.uuid)
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

func HostListHandle(cmd *cobra.Command, args []string) {
	url := fmt.Sprintf("http://%s:%d/v1/vpcs/%s/hosts", G.Host, G.Port, hostFlags.uuid)
	data := map[string]interface{}{}
	if hostFlags.abnormal {
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

func HostAppSetup() {
	if !G.config.L3Mode {
		return
	}
	G.echoServer.POST("/v1/vpcs/:uuid/hosts", AppHostAdd)
	G.echoServer.DELETE("/v1/hosts/:uuid", AppHostDel)
	G.echoServer.GET("/v1/vpcs/:uuid/hosts", AppHostList)
	G.echoServer.GET("/v1/hosts/:uuid", AppHostGet)
}

func AppHostAdd(c echo.Context) (err error) {
	G.logger.Debug("=========AppHostAdd==========")
	if !G.config.MasterL3 {
		return c.JSON(http.StatusForbidden, NewAppErrorRsp("This node is not a master L3 node."))
	}
	var host *Host
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("VPC UUID is required."))
	}
	vpc, err := LoadVPC(uuid)
	if err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load VPC."))
	}
	if vpc == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("VPC not ready."))
	}
	q := &HostAddReq{}
	if err = c.Bind(q); err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Cannot parse the request."))
	}
	for {
		if vpc.NotReady() {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp("VPC is not ready."))
		}
		host = &Host{VpcUuid: vpc.Uuid, SHAREREST: SHAREREST{ETCDINFO: ETCDINFO{Type: "host"}}}
		host.Hostname = q.Hostname
		host.Ip = q.Ip
		_ = host.PhaseStart(PhaseTypeAdd)
		if err = host.VerifyUuid(); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		if err = host.VerifyHostname(vpc); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		if err = host.VerifyHostIp(); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		var done bool
		done, err = Save(nil, []RES{host, vpc})
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	go host.AddingHostTask(vpc)
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(host.Uuid))
}

func AppHostList(c echo.Context) (err error) {
	G.logger.Debug("=========AppHostList==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("VPC UUID is required."))
	}
	q := AppListReq{}
	if err = c.Bind(&q); err != nil {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
	}
	var vpc *VPC
	if vpc, err = LoadVPC(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load VPC."))
	}
	if vpc == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("VPC not ready."))
	}
	hostUuids := []string{}
	rests := LoadRests("host")
	for hostUuid := range vpc.Hosts {
		h := &Host{SHAREREST: SHAREREST{UUIDINFO: UUIDINFO{Uuid: hostUuid}, ETCDINFO: ETCDINFO{Type: "host"}}}
		if rest := GetRest(rests, "host", h.GetKey()); rest != nil {
			host, ok := rest.(*Host)
			if !ok {
				continue
			}
			if q.Abnormal {
				if host.NotReady() {
					hostUuids = append(hostUuids, host.Uuid)
				}
			} else {
				hostUuids = append(hostUuids, host.Uuid)
			}
		}
	}
	jsonMap := make(map[string]interface {
	})
	jsonMap["status"] = "ok"
	jsonMap["host_count"] = len(hostUuids)
	jsonMap["hosts"] = hostUuids
	return c.JSON(http.StatusOK, jsonMap)
}

func AppHostGet(c echo.Context) (err error) {
	G.logger.Debug("=========AppHostGet==========")
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Host UUID is required."))
	}
	var host *Host
	if host, err = LoadHost(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if host == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Host not found."))
	}
	rsp := HostGetRsp{
		Status:    "ok",
		Hostname:  host.Hostname,
		Ip:        host.Ip,
		VpcUuid:   host.VpcUuid,
		SHAREREST: host.SHAREREST,
		PHASEINFO: host.PHASEINFO}
	return c.JSON(http.StatusOK, &rsp)
}

func AppHostDel(c echo.Context) (err error) {
	G.logger.Debug("=========AppHostDel==========")
	if !G.config.MasterL3 {
		return c.JSON(http.StatusForbidden, NewAppErrorRsp("This node is not a master L3 node."))
	}
	uuid := c.Param("uuid")
	if uuid == "" {
		return c.JSON(http.StatusBadRequest, NewAppErrorRsp("Host UUID is required."))
	}
	var host *Host
	if host, err = LoadHost(uuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
	}
	if host == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("Host not found."))
	}
	var vpc *VPC
	if vpc, err = LoadVPC(host.VpcUuid); err != nil {
		return c.JSON(http.StatusInternalServerError, NewAppErrorRsp("Cannot load VPC."))
	}
	if vpc == nil {
		return c.JSON(http.StatusNotFound, NewAppErrorRsp("VPC not ready."))
	}
	for {
		if err = host.PhaseStart(PhaseTypeDel); err != nil {
			return c.JSON(http.StatusBadRequest, NewAppErrorRsp(err.Error()))
		}
		var done bool
		done, err = Save(nil, []RES{host})
		if err != nil {
			return c.JSON(http.StatusInternalServerError, NewAppErrorRsp(err.Error()))
		}
		if done {
			break
		}
	}
	go host.DeletingHostTask(vpc)
	return c.JSON(http.StatusAccepted, NewAppPendingRsp(host.Uuid))
}
