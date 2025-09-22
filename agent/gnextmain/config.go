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
	"net"
	"os"
	"path/filepath"
	"strings"
	"time"

	"sigs.k8s.io/yaml"
)

const CMD_TIMEOUT = 30

type Config struct {
	/* common */
	Uuid           string        `json:"uuid"`
	AgentIp        string        `json:"agent_ip"`
	AgentPort      uint32        `json:"agent_port"`
	EtcdEndpoints  []string      `json:"etcd_endpoints"`
	AgentHome      string        `json:"agent_home"`
	LogFile        string        `json:"log_file"`
	LogLevel       string        `json:"log_level"`
	VmMode         bool          `json:"vm_mode"`
	FmIp           string        `json:"fm_ip"`
	LanNic         string        `json:"lan_nic"`
	LanBridge      string        `json:"lan_bridge"`
	L3Mode         bool          `json:"l3_mode"`
	WanNic         string        `json:"wan_nic"`
	WanNicMac      string        `json:"wan_mac"`
	LanNicMac      string        `json:"lan_mac"`
	WanBridge      string        `json:"wan_bridge"`
	VlanRange      string        `json:"vlan_range"`
	VniRange       string        `json:"vni_range"`
	EipGwMac       string        `json:"eip_gw_mac"`
	EipGwIp        string        `json:"eip_gw_ip"`
	WanGwMac       string        `json:"wan_gw_mac"`
	WanGwIp        string        `json:"wan_gw_ip"`
	DefaultEip     string        `json:"default_eip"`
	DnsmasqOptions []string      `json:"dnsmasq_options"`
	DnsServers     []string      `json:"dns_servers"`
	CmdTimeout     time.Duration `json:"cmd_timeout"`
	HaUuid         string        `json:"ha_uuid"`
	DpuIp          string        `json:"dpu_ip"`
	DpuPort        uint32        `json:"dpu_port"`
	DefaultSpeed   uint32        `json:"default_speed"`
	WanTestIp      string        `json:"wan_test_ip"`

	vlan_from      uint32
	vlan_to        uint32
	vni_from       uint32
	vni_to         uint32
	MasterL3       bool
	dnsmasq_config string
	dnsmasq_lease  string
	ROOTKEY        string
}

func loadConfig(host string, port uint32) {
	var yamlFile []byte
	var err error
	if yamlFile, err = os.ReadFile(G.configFile); err != nil {
		panic(err)
	}
	if err = yaml.Unmarshal(yamlFile, &G.config); err != nil {
		panic(err)
	}
	if G.config.LogLevel == "" {
		G.config.LogLevel = "info"
	}
	switch G.config.LogLevel {
	case "debug", "info", "warn", "error":
		break
	default:
		panic("log level is invalid")
	}
	if G.config.LogFile == "" {
		G.config.LogFile = "gnext.log"
	}
	if G.config.LogFile[0] != '/' {
		dir, err := filepath.Abs(filepath.Dir(G.configFile))
		if err != nil {
			panic(err)
		}
		dir = filepath.Join(dir, "/log/")
		if exists, _ := DirExists(dir); !exists {
			if err = os.Mkdir(dir, 0755); err != nil {
				panic(err)
			}
		}
		G.config.LogFile = filepath.Join(dir, G.config.LogFile)
	}
	loggerInit()
	if G.config.Uuid == "" {
		panic("agent id is not set")
	}
	if host != "" {
		G.config.AgentIp = host
	}
	if G.config.AgentIp == "" {
		panic("agent ip is not set")
	}
	ip := net.ParseIP(G.config.AgentIp)
	if ip == nil {
		panic("agent ip address is invalid")
	}
	if port != 0 {
		G.config.AgentPort = port
	}
	if G.config.AgentPort == 0 {
		panic("agent port is not set")
	}
	if G.config.AgentPort < 1024 || G.config.AgentPort > 65535 {
		panic("agent port is invalid")
	}
	if len(G.config.EtcdEndpoints) == 0 {
		panic("etcd endpoints are not set")
	}
	if G.config.AgentHome == "" {
		G.config.AgentHome = "/opt/gnext"
	}
	if exists, _ := DirExists(G.config.AgentHome); !exists {
		if err = os.Mkdir(G.config.AgentHome, 0755); err != nil {
			panic(err)
		}
	}
	if exists, _ := DirExists(G.config.AgentHome + "/var"); !exists {
		if err = os.Mkdir(G.config.AgentHome+"/var", 0755); err != nil {
			panic(err)
		}
	}
	if exists, _ := DirExists(G.config.AgentHome + "/bin"); !exists {
		if err = os.Mkdir(G.config.AgentHome+"/bin", 0755); err != nil {
			panic(err)
		}
	}
	if !G.config.VmMode && !G.config.L3Mode {
		panic("both vm mode and l3 mode are not set")
	}
	if !G.config.VmMode && G.config.L3Mode && !G.config.MasterL3 {
		panic("distributed l3 mode needs vm mode enabled")
	}

	if G.config.LanBridge == "" {
		G.config.LanBridge = "br0"
	}
	if G.config.VmMode {
		if G.config.FmIp != "" {
			ip := net.ParseIP(G.config.FmIp)
			if ip == nil {
				panic("fm ip address is invalid")
			}
			if !strings.HasPrefix(G.config.FmIp, "192.168.122") {
				panic("fm ip address is invalid")
			}
		}
		if G.config.DefaultSpeed == 0 {

			G.config.DefaultSpeed = 1000000
		}
	}
	if G.config.LanNic == "" {
		panic("lan nic are not set")
	}
	if !osNicExists(G.config.LanNic, "") {
		panic("lan nics are invalid")
	}

	if G.config.LanNicMac, err = osNicMac(G.config.LanNic, ""); err != nil {
		panic(err)
	}
	if G.config.CmdTimeout <= 0 {
		G.config.CmdTimeout = CMD_TIMEOUT
	}
	if G.config.L3Mode {
		if G.config.WanNic == "" {
			panic("wan nic is not set")
		}

		nic := G.config.WanNic
		var vlanid string
		if strings.Contains(G.config.WanNic, ".") {
			nic = strings.Split(G.config.WanNic, ".")[0]
			vlanid = strings.Split(G.config.WanNic, ".")[1]
		}
		if !osNicExists(nic, "") {
			panic("wan nic is invalid")
		}
		if vlanid != "" && !osNicExists(G.config.WanNic, "") {
			if err := osVlanNicAdd(G.config.WanNic, nic, vlanid); err != nil {
				panic(err)
			}
		}

		if G.config.WanBridge == "" {
			G.config.WanBridge = "br1"
		}
		if G.config.VlanRange == "" {
			panic("vlan range is not set")
		}
		parts := strings.Split(G.config.VlanRange, "-")
		if len(parts) != 2 {
			panic("vlan range is invalid")
		}
		vlan_from, err := AtoU32(parts[0])
		if err != nil {
			panic("vlan range is invalid")
		}
		vlan_to, err := AtoU32(parts[1])
		if err != nil {
			panic("vlan range is invalid")
		}
		if vlan_from < 1 || vlan_from > 4094 || vlan_to < 1 || vlan_to > 4094 {
			panic("vlan range is invalid")
		}
		if vlan_from > vlan_to {
			vlan_from, vlan_to = vlan_to, vlan_from
		}
		G.config.vlan_from = vlan_from
		G.config.vlan_to = vlan_to
		if G.config.VniRange != "" {
			parts := strings.Split(G.config.VniRange, "-")
			if len(parts) != 2 {
				panic("vni range is invalid")
			}
			vni_from, err := AtoU32(parts[0])
			if err != nil {
				panic("vni range is invalid")
			}
			vni_to, err := AtoU32(parts[1])
			if err != nil {
				panic("vni range is invalid")
			}
			if vni_from < 1 || vni_from > 16777214 || vni_to < 1 || vni_to > 16777214 {
				panic("vni range is invalid")
			}
			if vni_from > vni_to {
				vni_from, vni_to = vni_to, vni_from
			}
			G.config.vni_from = vni_from
			G.config.vni_to = vni_to
		}
		if G.config.DpuIp != "" {
			ip := net.ParseIP(G.config.DpuIp)
			if ip == nil {
				panic("dpu ip address is invalid")
			}
			if G.config.DpuPort == 0 {
				panic("dpu port is not set")
			}
		}
		if G.config.EipGwMac != "" {
			if _, err := net.ParseMAC(G.config.EipGwMac); err != nil {
				fmt.Printf("EIP gateway MAC address is invalid")
			}
			if G.config.EipGwIp == "" {
				panic("eip gateway ip address is not set")
			}
		}
		if G.config.WanGwMac == "" {
			panic("wan gateway mac address is not set")
		}
		if _, err := net.ParseMAC(G.config.WanGwMac); err != nil {
			panic("wan gateway mac address is invalid")
		}
		if G.config.WanGwIp == "" {
			panic("wan gateway ip address is not set")
		}
		ip = net.ParseIP(G.config.WanGwIp)
		if ip == nil {
			panic("wan gateway ip address is invalid")
		}
		if G.config.DefaultEip == "" {
			panic("default eip is not set")
		}
		ip = net.ParseIP(G.config.DefaultEip)
		if ip == nil {
			panic("default eip is invalid")
		}

		if G.config.WanNicMac, err = osNicMac(G.config.WanNic, ""); err != nil {
			panic(err)
		}
		G.config.dnsmasq_config = G.config.AgentHome + "/var/dnsmasq.conf"
		G.config.dnsmasq_lease = G.config.AgentHome + "/var/dnsmasq.lease"
		if len(G.config.DnsmasqOptions) == 0 {
			G.config.DnsmasqOptions = []string{
				"log-queries",
				"log-dhcp",
				"no-resolv",
				"no-hosts",
				"local=/lan/",
				"domain-needed",
				"bogus-priv",
				"dhcp-option=option:domain-search,lan",
			}
		}
		if len(G.config.DnsServers) == 0 {
			G.config.DnsServers = []string{
				"114.114.114.114",
				"114.114.115.115",
				"223.5.5.5",
				"223.6.6.6",
			}
		}
		if G.config.WanTestIp == "" {
			G.config.WanTestIp = "223.5.5.5"
		} else {
			ip = net.ParseIP(G.config.WanTestIp)
			if ip == nil {
				panic("default wan test ip is invalid")
			}
		}
	}
	G.config.ROOTKEY = "/gnext"
	fmt.Println("start the gnext service")
	fmt.Printf("agent_ip: %s\n", G.config.AgentIp)
	fmt.Printf("agent_port: %d\n", G.config.AgentPort)
	fmt.Printf("etcd_endpoints: %v\n", G.config.EtcdEndpoints)
	fmt.Printf("log_file: %s\n", G.config.LogFile)
	fmt.Printf("log_level: %s\n", G.config.LogLevel)
	fmt.Printf("vm_mode: %t\n", G.config.VmMode)
	fmt.Printf("fm_ip: %s\n", G.config.FmIp)
	fmt.Printf("lan_nic: %v\n", G.config.LanNic)
	fmt.Printf("lan_bridge: %s\n", G.config.LanBridge)
	fmt.Printf("l3_mode: %t\n", G.config.L3Mode)
	fmt.Printf("wan_nic: %s\n", G.config.WanNic)
	fmt.Printf("wan_bridge: %s\n", G.config.WanBridge)
	fmt.Printf("vlan_range: %s\n", G.config.VlanRange)
	fmt.Printf("vni_range: %s\n", G.config.VniRange)
	fmt.Printf("eip_gw_mac: %s\n", G.config.EipGwMac)
	fmt.Printf("eip_gw_ip: %s\n", G.config.EipGwIp)
	fmt.Printf("wan_gw_mac: %s\n", G.config.WanGwMac)
	fmt.Printf("wan_gw_ip: %s\n", G.config.WanGwIp)
	fmt.Printf("default_eip: %s\n", G.config.DefaultEip)
	fmt.Printf("vlan_from: %d\n", G.config.vlan_from)
	fmt.Printf("vlan_to: %d\n", G.config.vlan_to)
	fmt.Printf("wan_mac: %s\n", G.config.WanNicMac)
	fmt.Printf("ROOTKEY: %s\n", G.config.ROOTKEY)
}
