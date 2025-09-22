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
	"fmt"
	"time"
)

type RES interface {
	GetType() string
	GetKey() string
	GetModrev() int64
	SetModrev(int64)
}

type REST interface {
	RES
	GetUuid() string
	SetUuid(string)
	PhaseStart(PhaseType) error
	PhaseStop(bool)
	NotReady() bool
	FixPending() bool
}

type ETCDINFO struct {
	Modrev int64  `json:"-"`
	Type   string `json:"type"`
}

func (o *ETCDINFO) GetModrev() int64 {
	return o.Modrev
}

func (o *ETCDINFO) SetModrev(modrev int64) {
	o.Modrev = modrev
}

func (o *ETCDINFO) GetType() string {
	return o.Type
}

type UUIDINFO struct {
	Uuid string `json:"uuid"`
}

func (o *UUIDINFO) GetUuid() string {
	return o.Uuid
}

func (o *UUIDINFO) SetUuid(uuid string) {
	o.Uuid = uuid
}

type AGENTREST struct {
	ETCDINFO
	UUIDINFO
	AgentUuid string `json:"agent"`
}

func (o *AGENTREST) GetKey() string {
	return G.config.ROOTKEY + "/" + o.GetType() + "/" + o.AgentUuid + "/" + o.GetUuid()
}

type SHAREREST struct {
	ETCDINFO
	UUIDINFO
}

func (o *SHAREREST) GetKey() string {
	return G.config.ROOTKEY + "/" + o.GetType() + "/" + o.GetUuid()
}

type UNIQRES struct {
	ETCDINFO
}

func (o *UNIQRES) GetKey() string {
	return G.config.ROOTKEY + "/" + o.GetType()
}

type AGENTRES struct {
	ETCDINFO
	AgentUuid string `json:"agent"`
}

func (o *AGENTRES) GetKey() string {
	return G.config.ROOTKEY + "/" + o.GetType() + "/" + o.AgentUuid
}

type PHASEINFO struct {
	IsAdded     bool        `json:"added"`
	PhaseType   PhaseType   `json:"phase_type"`
	Start       uint32      `json:"phase_start"`
	Update      uint32      `json:"phase_update"`
	PhaseStatus PhaseStatus `json:"phase_status"`
}

func (o *PHASEINFO) PhaseStart(phaseType PhaseType) error {
	if o.PhaseStatus == PhaseStatusPending {
		return fmt.Errorf("current phase %s is on processing", o.PhaseType)
	}
	o.Start = uint32(time.Now().Unix())
	o.Update = o.Start
	o.PhaseType = phaseType
	o.PhaseStatus = PhaseStatusPending
	return nil
}

func (o *PHASEINFO) PhaseStop(sucess bool) {
	o.Update = uint32(time.Now().Unix())
	o.PhaseStatus = PhaseStatusSuccess
	if !sucess {
		o.PhaseStatus = PhaseStatusFail
	} else {
		if o.PhaseType == PhaseTypeAdd {
			o.IsAdded = true
		}
	}
}

func (o *PHASEINFO) NotReady() bool {
	return o.PhaseStatus == PhaseStatusPending || !o.IsAdded
}

func (o *PHASEINFO) FixPending() bool {
	if o.PhaseStatus == PhaseStatusPending {
		o.PhaseStatus = PhaseStatusFail
		return true
	}
	return false
}

type PhaseType string

const (
	PhaseTypeAdd      PhaseType = "add"
	PhaseTypeDel      PhaseType = "del"
	PhaseTypeBind     PhaseType = "bind"
	PhaseTypeUnbind   PhaseType = "unbind"
	PhaseTypeApply    PhaseType = "apply"
	PhaseTypeUpdate   PhaseType = "update"
	PhaseTypeAttach   PhaseType = "attach"
	PhaseTypeDetach   PhaseType = "detach"
	PhaseTypeExport   PhaseType = "export"
	PhaseTypeResize   PhaseType = "resize"
	PhaseTypeSnap     PhaseType = "snap"
	PhaseTypePoweron  PhaseType = "poweron"
	PhaseTypePoweroff PhaseType = "poweroff"
	PhaseTypeSuspend  PhaseType = "suspend"
	PhaseTypeResume   PhaseType = "resume"
	PhaseTypeModify   PhaseType = "modify"
	PhaseTypeInject   PhaseType = "inject"
	PhaseTypeInject2  PhaseType = "inject2"
	PhaseTypeEject    PhaseType = "eject"
	PhaseTypeEject2   PhaseType = "eject2"
	PhaseTypeMigrate  PhaseType = "migrate"
	PhaseTypeVol      PhaseType = "vol"
	PhaseTypeCP       PhaseType = "cp"
	PhaseTypeGPU      PhaseType = "gpu"
	PhaseTypeSwitch   PhaseType = "switch"
)

type PhaseStatus string

const (
	PhaseStatusPending PhaseStatus = "pending"
	PhaseStatusSuccess PhaseStatus = "success"
	PhaseStatusFail    PhaseStatus = "fail"
)
