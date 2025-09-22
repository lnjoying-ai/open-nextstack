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
	"os"
)

func init() {
	envHost := "GNEXT_HOST"
	envPort := "GNEXT_PORT"
	defaultHost := "127.0.0.1"
	var defaultPort uint32 = 8899
	if envVal, ok := os.LookupEnv(envHost); ok {
		defaultHost = envVal
	}
	if envVal, ok := os.LookupEnv(envPort); ok {
		if i, err := AtoU32(envVal); err == nil {
			defaultPort = i
		}
	}
	G.rootCmd.PersistentFlags().StringVar(&G.Host, "host", defaultHost, "set the host of the gnext service")
	G.rootCmd.PersistentFlags().Uint32Var(&G.Port, "port", defaultPort, "set the port of the gnext service")
	ServiceCmdparser(G.rootCmd)
	AgentCmdParser(G.rootCmd)
	VpcCmdParser(G.rootCmd)
	SubnetCmdParser(G.rootCmd)
	PortCmdParser(G.rootCmd)
	HostCmdParser(G.rootCmd)
	SgCmdParser(G.rootCmd)
	PoolCmdParser(G.rootCmd)
	ImgCmdParser(G.rootCmd)
	VolCmdParser(G.rootCmd)
	SnapCmdParser(G.rootCmd)
	GpuCmdParser(G.rootCmd)
	VmCmdParser(G.rootCmd)
	CpCmdParser(G.rootCmd)
	NfsCmdParser(G.rootCmd)
}

func Main() {
	err := G.rootCmd.Execute()
	if err != nil {
		panic(err)
	}
}
