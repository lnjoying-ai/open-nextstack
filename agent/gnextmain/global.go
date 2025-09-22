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
	"sync"

	"github.com/labstack/echo"
	"github.com/libvirt/libvirt-go"
	"github.com/spf13/cobra"
	v3 "go.etcd.io/etcd/client/v3"
	"go.uber.org/zap"
)

type GlobalVars struct {
	Host     string
	Port     uint32
	v3client *v3.Client

	configFile   string
	config       *Config
	logger       *zap.Logger
	ctx          context.Context
	cancel       context.CancelFunc
	dhcpCh       chan string
	sgCh         chan string
	rootCmd      *cobra.Command
	sysLock      *sync.Mutex
	echoServer   *echo.Echo
	fmPartitions *FMPartition
	distro       string
	sysRWLock    *sync.RWMutex
	deployedRest map[string]struct{}
	conn         *libvirt.Connect
	fix          bool
	ctxes        map[string]chan struct{}
	ml3Lock      *sync.Mutex
}

func NewGlobalVars() *GlobalVars {
	ctx, cancel := context.WithCancel(context.Background())
	distro := osDistro()
	return &GlobalVars{
		v3client:   nil,
		configFile: "",
		config:     nil,
		logger:     nil,

		ctx:    ctx,
		cancel: cancel,
		ctxes:  make(map[string]chan struct{}),
		distro: distro,
		dhcpCh: make(chan string, 20),
		sgCh:   make(chan string, 20),
		rootCmd: &cobra.Command{
			Use:        "gnext",
			Short:      "Gnext is both a CLI and an agent of the GNext cloud management system.",
			SuggestFor: []string{"gnext"},
			Run: func(cmd *cobra.Command, args []string) {
				fmt.Println("please specify a subcommand for gnext management. use -h for help.")
			},
		},
		sysLock:      &sync.Mutex{},
		echoServer:   echo.New(),
		sysRWLock:    &sync.RWMutex{},
		ml3Lock:      &sync.Mutex{},
		deployedRest: make(map[string]struct{}),
	}
}

var G = NewGlobalVars()
