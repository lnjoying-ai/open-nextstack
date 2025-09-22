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
)

type DpuPortReq struct {
	Vni     uint32 `json:"vni"`
	VlanId  uint16 `json:"vlanId"`
	MacAddr string `json:"macAddr"`
	IpAddr  string `json:"ipAddr"`
	PortId  string `json:"portId"`
	Status  string `json:"status"`
	Message string `json:"message"`
}

type DpuResp struct {
	Message string `json:"message"`
	Status  string `json:"status"`
}

func DpuAddPort(Vni string, VlanId string, MacAddr, IpAddr, PortId string) error {
	v, _ := AtoU32(Vni)
	l, _ := AtoU16(VlanId)
	url := fmt.Sprintf("http://%s:%d/api/v1/ports", G.config.DpuIp, G.config.DpuPort)
	data := DpuPortReq{Vni: v, VlanId: l, MacAddr: MacAddr, IpAddr: IpAddr, PortId: PortId}
	jsonBytes, _ := json.Marshal(data)
	req, _ := http.NewRequest("POST", url, bytes.NewBuffer(jsonBytes))
	req.Header.Set("Content-Type", "application/json")
	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return err
	}
	var dpuResp DpuResp
	err = json.Unmarshal(body, &dpuResp)
	if err != nil {
		return err
	}
	if dpuResp.Status != "ok" {
		return fmt.Errorf("DPU response status is not ok")
	}
	return nil
}

func DpuDelPort(PortId string) error {
	url := fmt.Sprintf("http://%s:%d/api/v1/ports/%s", G.config.DpuIp, G.config.DpuPort, PortId)
	req, _ := http.NewRequest("DELETE", url, nil)
	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return err
	}
	var dpuResp DpuResp
	err = json.Unmarshal(body, &dpuResp)
	if err != nil {
		return err
	}
	if dpuResp.Status != "ok" {
		return fmt.Errorf("DPU response status is not ok")
	}
	return nil
}

func DpuClear() error {
	url := fmt.Sprintf("http://%s:%d/api/v1/network/clear", G.config.DpuIp, G.config.DpuPort)
	req, _ := http.NewRequest("DELETE", url, nil)
	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return err
	}
	var dpuResp DpuResp
	err = json.Unmarshal(body, &dpuResp)
	if err != nil {
		return err
	}
	if dpuResp.Status != "ok" {
		return fmt.Errorf("DPU response status is not ok")
	}
	return nil
}
