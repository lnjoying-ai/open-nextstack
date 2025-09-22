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
	"context"
	"fmt"
	"reflect"

	"github.com/google/uuid"
	v3 "go.etcd.io/etcd/client/v3"
	"go.uber.org/zap"
	"sigs.k8s.io/yaml"
)

type PROTO2PORTRANGEST map[string][][2]uint32
type PORT2PROTOST map[string]PROTO2PORTRANGEST
type EIPINFOT struct {
	VpcUuid string       `json:"vpc"`
	Mac     string       `json:"mac"`
	Mapping PORT2PROTOST `json:"mapping"`
}

type NIDS struct {
	NidsInfo map[string]string `json:"nids"`
	UNIQRES
}

type VLANIDS struct {
	VlanidsInfo map[string]map[string]string `json:"vlanids"`
	UNIQRES
}

type MACS struct {
	MacsInfo map[string]string `json:"macs"`
	UNIQRES
}

type NICS struct {
	NicsInfo map[string]map[string]string `json:"nics"`
	UNIQRES
}

type OFPORTS struct {
	OfportsInfo map[string]map[string]string `json:"ofports"`
	UNIQRES
}

type VNCS struct {
	VncsInfo map[string]map[string]string `json:"vncs"`
	UNIQRES
}

type SUBNETS struct {
	SubnetsInfo map[string]map[string]string `json:"subnets"`
	UNIQRES
}

type PORTS struct {
	PortsInfo map[string]map[string]string `json:"ports"`
	UNIQRES
}

type EIPS struct {
	EipsInfo map[string]*EIPINFOT `json:"eips"`
	UNIQRES
}

type POOLS struct {
	DirInfo map[string]map[string]string `json:"dirs"`
	SidInfo map[string]map[string]string `json:"sids"`
	UNIQRES
}

type ML3 struct {
	AgentUuid string `json:"agent"`
	UNIQRES
}

type RESTS map[string]map[string]REST

type HOSTSGT struct {
	Ports map[string]string `json:"ports"`
	AGENTRES
}

type LANTEST struct {
	LanTestInfo map[string]map[string]int64 `json:"lantest"`
	UNIQRES
}

type WANTEST struct {
	WanTestInfo map[string]map[string]int64 `json:"wantest"`
	UNIQRES
}

func LoadRes(path string, res RES) (ret RES, err error) {
	var resp *v3.GetResponse
	if resp, err = G.v3client.KV.Get(context.Background(), path); err != nil {
		G.logger.Error("cannot get object from etcd", zap.Error(err))
		return nil, err
	}
	if resp.Count == 0 {
		return nil, nil
	}
	kv := resp.Kvs[0]
	if err = yaml.Unmarshal(kv.Value, res); err != nil {
		G.logger.Error("cannot unmarshal object", zap.Error(err))
		return nil, err
	}
	res.SetModrev(kv.ModRevision)
	return res, nil
}

func LoadRests(resType string) RESTS {
	var err error
	var resp *v3.GetResponse
	rests := RESTS{}

	var allRestTypes = map[string][]interface{}{
		"agent":  {reflect.TypeOf(Agent{}), false},
		"sg":     {reflect.TypeOf(SG{}), false},
		"vpc":    {reflect.TypeOf(VPC{}), false},
		"subnet": {reflect.TypeOf(Subnet{}), false},
		"port":   {reflect.TypeOf(Port{}), false},
		"host":   {reflect.TypeOf(Host{}), false},
		"pool":   {reflect.TypeOf(Pool{}), true},
		"vol":    {reflect.TypeOf(Vol{}), true},
		"snap":   {reflect.TypeOf(Snap{}), true},
		"img":    {reflect.TypeOf(Img{}), true},
		"gpu":    {reflect.TypeOf(GPU{}), true},
		"vm":     {reflect.TypeOf(VM{}), true},
		"cp":     {reflect.TypeOf(CP{}), true},
		"nfs":    {reflect.TypeOf(Nfs{}), true},
	}
	for _resType := range allRestTypes {

		if resType != "" && resType != _resType {
			continue
		}

		var path string
		if allRestTypes[_resType][1].(bool) {
			path = G.config.ROOTKEY + "/" + _resType + "/" + G.config.Uuid
		} else {
			path = G.config.ROOTKEY + "/" + _resType + "/"
		}
		if resp, err = G.v3client.KV.Get(context.Background(), path, v3.WithPrefix()); err != nil {
			G.logger.Error("cannot get object from etcd", zap.Error(err))
			continue
		}
		if resp.Count == 0 {
			continue
		}
		for _, kv := range resp.Kvs {
			val := reflect.New(allRestTypes[_resType][0].(reflect.Type)).Elem()
			obj := val.Addr().Interface().(REST)
			if err = yaml.Unmarshal(kv.Value, obj); err != nil {
				G.logger.Error("cannot unmarshal object", zap.Error(err))
				continue
			}
			obj.SetModrev(kv.ModRevision)
			if _, ok := rests[_resType]; !ok {
				rests[_resType] = make(map[string]REST)
			}
			rests[_resType][obj.GetKey()] = obj
		}
	}
	return rests
}

func FixRests() {
	if !G.fix {
		return
	}
	rests := LoadRests("")
	for _, reses := range rests {
		for _, res := range reses {
			for {
				if res.FixPending() {
					var done bool
					var err error
					if done, err = Save(nil, []RES{res}); err != nil {
						G.logger.Fatal("cannot save res", zap.Error(err))
					}
					if done {
						break
					}
				} else {
					break
				}
			}
		}
	}
}

func LoadLanTest() (*LANTEST, error) {
	lantest := &LANTEST{LanTestInfo: make(map[string]map[string]int64), UNIQRES: UNIQRES{ETCDINFO: ETCDINFO{Type: "lantest"}}}
	res, err := LoadRes(lantest.GetKey(), lantest)
	if err != nil {
		return nil, err
	}
	if res != nil {
		return res.(*LANTEST), nil
	}
	return lantest, nil
}

func LoadWanTest() (*WANTEST, error) {
	wantest := &WANTEST{WanTestInfo: make(map[string]map[string]int64), UNIQRES: UNIQRES{ETCDINFO: ETCDINFO{Type: "wantest"}}}
	res, err := LoadRes(wantest.GetKey(), wantest)
	if err != nil {
		return nil, err
	}
	if res != nil {
		return res.(*WANTEST), nil
	}
	return wantest, nil
}

func LoadHostSG(agentUuid string) (*HOSTSGT, error) {
	hostsg := &HOSTSGT{Ports: map[string]string{}, AGENTRES: AGENTRES{AgentUuid: agentUuid, ETCDINFO: ETCDINFO{Type: "hostsg"}}}
	res, err := LoadRes(hostsg.GetKey(), hostsg)
	if err != nil {
		return nil, err
	}
	if res != nil {
		return res.(*HOSTSGT), nil
	}
	return hostsg, nil
}

func LoadEips() (*EIPS, error) {
	eips := &EIPS{EipsInfo: make(map[string]*EIPINFOT), UNIQRES: UNIQRES{ETCDINFO: ETCDINFO{Type: "eips"}}}
	res, err := LoadRes(eips.GetKey(), eips)
	if err != nil {
		return nil, err
	}
	if res != nil {
		return res.(*EIPS), nil
	}
	return eips, nil
}

func LoadNids() (*NIDS, error) {
	nids := &NIDS{NidsInfo: make(map[string]string), UNIQRES: UNIQRES{ETCDINFO: ETCDINFO{Type: "nids"}}}
	res, err := LoadRes(nids.GetKey(), nids)
	if err != nil {
		return nil, err
	}
	if res != nil {
		return res.(*NIDS), nil
	}
	return nids, nil
}

func LoadVlanids() (*VLANIDS, error) {
	vlanids := &VLANIDS{VlanidsInfo: make(map[string]map[string]string), UNIQRES: UNIQRES{ETCDINFO: ETCDINFO{Type: "vlanids"}}}
	res, err := LoadRes(vlanids.GetKey(), vlanids)
	if err != nil {
		return nil, err
	}
	if res != nil {
		return res.(*VLANIDS), nil
	}
	return vlanids, nil
}

func LoadMacs() (*MACS, error) {
	macs := &MACS{MacsInfo: make(map[string]string), UNIQRES: UNIQRES{ETCDINFO: ETCDINFO{Type: "macs"}}}
	res, err := LoadRes(macs.GetKey(), macs)
	if err != nil {
		return nil, err
	}
	if res != nil {
		return res.(*MACS), nil
	}
	return macs, nil
}

func LoadNics() (*NICS, error) {
	nics := &NICS{NicsInfo: make(map[string]map[string]string), UNIQRES: UNIQRES{ETCDINFO: ETCDINFO{Type: "nics"}}}
	res, err := LoadRes(nics.GetKey(), nics)
	if err != nil {
		return nil, err
	}
	if res != nil {
		return res.(*NICS), nil
	}
	return nics, nil
}

func LoadOfports() (*OFPORTS, error) {
	ofports := &OFPORTS{OfportsInfo: make(map[string]map[string]string), UNIQRES: UNIQRES{ETCDINFO: ETCDINFO{Type: "ofports"}}}
	res, err := LoadRes(ofports.GetKey(), ofports)
	if err != nil {
		return nil, err
	}
	if res != nil {
		return res.(*OFPORTS), nil
	}
	return ofports, nil
}

func LoadVncs() (*VNCS, error) {
	vncs := &VNCS{VncsInfo: make(map[string]map[string]string), UNIQRES: UNIQRES{ETCDINFO: ETCDINFO{Type: "vncs"}}}
	res, err := LoadRes(vncs.GetKey(), vncs)
	if err != nil {
		return nil, err
	}
	if res != nil {
		return res.(*VNCS), nil
	}
	return vncs, nil
}

func LoadSubnets() (*SUBNETS, error) {
	subnets := &SUBNETS{SubnetsInfo: make(map[string]map[string]string), UNIQRES: UNIQRES{ETCDINFO: ETCDINFO{Type: "subnets"}}}
	res, err := LoadRes(subnets.GetKey(), subnets)
	if err != nil {
		return nil, err
	}
	if res != nil {
		return res.(*SUBNETS), nil
	}
	return subnets, nil
}

func LoadPorts() (*PORTS, error) {
	ports := &PORTS{PortsInfo: make(map[string]map[string]string), UNIQRES: UNIQRES{ETCDINFO: ETCDINFO{Type: "ports"}}}
	res, err := LoadRes(ports.GetKey(), ports)
	if err != nil {
		return nil, err
	}
	if res != nil {
		return res.(*PORTS), nil
	}
	return ports, nil
}

func LoadPools() (*POOLS, error) {
	pools := &POOLS{DirInfo: make(map[string]map[string]string), SidInfo: make(map[string]map[string]string), UNIQRES: UNIQRES{ETCDINFO: ETCDINFO{Type: "pools"}}}
	res, err := LoadRes(pools.GetKey(), pools)
	if err != nil {
		return nil, err
	}
	if res != nil {
		return res.(*POOLS), nil
	}
	return pools, nil
}

func LoadMl3() (*ML3, error) {
	ml3 := &ML3{UNIQRES: UNIQRES{ETCDINFO: ETCDINFO{Type: "ml3"}}}
	res, err := LoadRes(ml3.GetKey(), ml3)
	if err != nil {
		return nil, err
	}
	if res != nil {
		return res.(*ML3), nil
	}
	return ml3, nil
}

func AssignUuid(resType string, res REST) error {
	if res.GetUuid() != "" {
		return fmt.Errorf("uuid already assigned")
	}
	uuidStr := uuid.NewString()
	res.SetUuid(uuidStr)
	return nil
}

func AssignMac(macs *MACS, port *Port) error {
	if port.Mac != "" {
		return fmt.Errorf("mac already assigned")
	}
	for {
		mac := GenMac(port.Purpose)
		if _, ok := macs.MacsInfo[mac]; !ok {
			macs.MacsInfo[mac] = port.Uuid
			port.Mac = mac
			return nil
		}
	}
}

func UpdatePhaseStop(rest REST, success bool) (REST, error) {
	var err error
	for {
		rest.PhaseStop(success)
		var done bool
		done, err = Save(nil, []RES{rest})
		if err != nil {
			if err.Error() == rest.GetKey() {
				return nil, nil
			}
			return nil, err
		}
		if done {
			break
		}
	}
	return rest, nil
}

func reinitialize(p RES) {

	value := reflect.ValueOf(p)

	if value.Kind() == reflect.Ptr {

		elem := value.Elem()

		newValue := reflect.New(elem.Type()).Elem()

		elem.Set(newValue)
	}
}

func Save(toDel []RES, toPut []RES) (done bool, err error) {
	cmps := make([]v3.Cmp, 0)
	thens := make([]v3.Op, 0)
	elses := make([]v3.Op, 0)
	var tmps []RES
	for _, res := range toDel {
		if res != nil && !reflect.ValueOf(res).IsNil() {
			tmps = append(tmps, res)
		}
	}
	toDel = tmps
	tmps = nil
	for _, res := range toPut {
		if res != nil && !reflect.ValueOf(res).IsNil() {
			tmps = append(tmps, res)
		}
	}
	toPut = tmps
	var toElse []RES
	for _, res := range toPut {

		if res.GetModrev() != 0 {
			cmps = append(cmps, v3.Compare(v3.ModRevision(res.GetKey()), "=", res.GetModrev()))
		}
		data, err := yaml.Marshal(res)
		if err != nil {
			return false, err
		}
		thens = append(thens, v3.OpPut(res.GetKey(), string(data)))
		elses = append(elses, v3.OpGet(res.GetKey()))
		toElse = append(toElse, res)
	}
	for _, res := range toDel {
		cmps = append(cmps, v3.Compare(v3.ModRevision(res.GetKey()), "=", res.GetModrev()))
		elses = append(elses, v3.OpGet(res.GetKey()))
		thens = append(thens, v3.OpDelete(res.GetKey()))
		toElse = append(toElse, res)
	}
	txnResp, err := G.v3client.KV.Txn(context.Background()).If(cmps...).Then(thens...).Else(elses...).Commit()
	if err != nil {
		return false, err
	}
	if !txnResp.Succeeded {
		for i, txnResp := range txnResp.Responses {
			resp := txnResp.GetResponseRange()
			if resp != nil {
				if resp.Count == 0 {
					if toElse[i].GetModrev() != 0 {
						return false, fmt.Errorf(toElse[i].GetKey())
					} else {
						continue
					}
				}
				reinitialize(toElse[i])
				if err = yaml.Unmarshal(resp.Kvs[0].Value, toElse[i]); err != nil {
					return false, err
				}
				toElse[i].SetModrev(resp.Kvs[0].ModRevision)
			}
		}
		return false, nil
	}
	for i, txnResp := range txnResp.Responses {
		if i < len(toPut) {
			putresp := txnResp.GetResponsePut()
			if putresp != nil {
				toPut[i].SetModrev(putresp.Header.Revision)
			}
		}
	}
	return true, nil
}

func LoadVms(agentUuid string) (vms []*VM, err error) {
	var resp *v3.GetResponse
	path := G.config.ROOTKEY + "/vm/" + agentUuid
	if resp, err = G.v3client.KV.Get(context.Background(), path, v3.WithPrefix()); err != nil {
		return nil, err
	}
	if resp.Count == 0 {
		return nil, nil
	}
	vms = make([]*VM, resp.Count)
	for i, kv := range resp.Kvs {
		vms[i] = &VM{}
		if err = yaml.Unmarshal(kv.Value, vms[i]); err != nil {
			return nil, err
		}
		vms[i].SetModrev(kv.ModRevision)
	}
	return vms, nil
}

func GetRest(rests RESTS, resType string, key string) REST {
	if _, ok := rests[resType]; !ok {
		return nil
	}
	return rests[resType][key]
}
