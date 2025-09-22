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
	"os"
	"strings"
	"sync"
	"time"

	"github.com/google/gopacket"
	"github.com/google/gopacket/layers"
	"github.com/google/gopacket/pcap"
	"github.com/libvirt/libvirt-go"
	"github.com/spf13/cobra"
	v3 "go.etcd.io/etcd/client/v3"
	"go.uber.org/zap"
	"sigs.k8s.io/yaml"
)

func ServiceCmdparser(command *cobra.Command) {
	var serviceCmd = &cobra.Command{
		Use:   "service",
		Short: "Service management",
		Run: func(cmd *cobra.Command, args []string) {
			fmt.Println("please specify a subcommand for vpc management. use -h for help.")
		},
	}
	var serviceStartCmd = &cobra.Command{
		Use:   "start",
		Short: "Start the gnext service",
		Run:   serviceStartHandle,
	}
	serviceStartCmd.Flags().StringVarP(&G.configFile, "config", "C", "/opt/gnext/config.yml", "set the configuration file of the gnext service")
	serviceStartCmd.Flags().BoolVar(&G.fix, "fix", false, "fix pending resources")
	var serviceStopCmd = &cobra.Command{
		Use:   "stop",
		Short: "Stop the gnext service",
		Run:   serviceStopHandle,
	}
	serviceStopCmd.Flags().StringVarP(&G.configFile, "config", "C", "/opt/gnext/config.yml", "set the configuration file of the gnext service")
	serviceCmd.AddCommand(serviceStartCmd, serviceStopCmd)
	command.AddCommand(serviceCmd)
	var completionCmd = &cobra.Command{
		Use:   "completion",
		Short: "Generate shell completion scripts",
	}
	completionCmd.Hidden = true
	command.AddCommand(completionCmd)
}

func serviceStartHandle(cmd *cobra.Command, args []string) {
	host := ""
	var port uint32
	hostFlag := cmd.Flag("host")
	if hostFlag.Changed {
		host = G.Host
	}
	portFlag := cmd.Flag("port")
	if portFlag.Changed {
		port = G.Port
	}
	loadConfig(host, port)
	osInit()
	serviceStart()
}

func serviceStopHandle(cmd *cobra.Command, args []string) {
	fmt.Println("stop the gnext service")
}

func osInit() {
	var err error
	if err = osAllNetnsDel(); err != nil {
		G.logger.Error("failed to remove net ns", zap.Error(err))
		panic(err)
	}
	if err = OvsBridgeDel(G.config.LanBridge); err != nil {
		G.logger.Error("failed to remove lan bridge", zap.Error(err))
		panic(err)
	}
	if err = OvsBridgeDel(G.config.WanBridge); err != nil {
		G.logger.Error("failed to remove wan bridge", zap.Error(err))
		panic(err)
	}
	if err = osIptablesRest(); err != nil {
		G.logger.Error("failed to reset iptables", zap.Error(err))
		panic(err)
	}
}

func serviceStart() {
	var err error
	if G.v3client, err = v3.New(v3.Config{Endpoints: G.config.EtcdEndpoints}); err != nil {
		G.logger.Error("failed to create etcd client", zap.Error(err))
		panic(err)
	}

	InitShareRes()
	FixRests()
	RegistAgentsMonitor()
	VirtConnect()

	Restore()

	RegisterSgMonitor()

	RegisterDhcpUpdater()

	RegisterEipMonitor()
	RegisterVmMonitor()
	RegisterHaMonitor()
	RegisterMeterMonitor()
	AgentAppSetup()
	VpcAppSetup()
	SubnetAppSetup()
	PortAppSetup()
	HostAppSetup()
	SgAppSetup()
	if G.config.VmMode {
		GpuAppSetup()
		PoolAppSetup()
		ImgAppSetup()
		VolAppSetup()
		SnapAppSetup()
		VmAppSetup()
		CpAppSetup()
		NfsAppSetup()
	}
	if err = G.echoServer.Start(fmt.Sprintf("%s:%d", G.config.AgentIp, G.config.AgentPort)); err != nil {
		G.logger.Error("failed to start gnext api service", zap.Error(err))
		panic(err)
	}
}

func Restore() {
	var err error
	if err = osIpForwardEnable(); err != nil {
		G.logger.Error("cannot enable ip forwarding", zap.Error(err))
	}
	if err = OvsWanInit(); err != nil {
		G.logger.Error("cannot initialize wan bridge", zap.Error(err))
	}
	if err = OvsLanInit(); err != nil {
		G.logger.Error("cannot initialize lan bridge", zap.Error(err))
	}
	if err = osIpNonlocalBindEnable(); err != nil {
		G.logger.Error("cannot enable ip nonlocal bind", zap.Error(err))
	}
	if G.config.VmMode {
		if err = LoadGpus(); err != nil {
			G.logger.Error("cannot load gpus", zap.Error(err))
		}
		if G.config.DpuIp != "" {
			DpuClear()
		}
	}
	rests := LoadRests("")
	var wg sync.WaitGroup
	for _, rest := range rests["vpc"] {
		if vpc, ok := rest.(*VPC); ok {
			if !vpc.NotReady() {
				wg.Add(1)
				go func(wg *sync.WaitGroup) {
					defer wg.Done()
					if err = vpc.Restore(); err != nil {
						G.logger.Error("cannot restore vpc", zap.Error(err))
					}
				}(&wg)
			}
		}
	}
	wg.Wait()
	for _, rest := range rests["vm"] {
		if vm, ok := rest.(*VM); ok {
			if vm.AgentUuid != G.config.Uuid {
				continue
			}
			if vm.NotReady() {
				continue
			}
			if vm.PhaseType == PhaseTypeMigrate && vm.PhaseStatus != PhaseStatusSuccess {
				continue
			}
			if vm.PhaseType == PhaseTypeSuspend && vm.PhaseStatus == PhaseStatusSuccess {
				continue
			}
			wg.Add(1)
			go func(wg *sync.WaitGroup) {
				defer wg.Done()
				if err = vm.Restore(); err != nil {
					G.logger.Error("cannot restore vm", zap.Error(err))
				}
			}(&wg)
		}
	}
	for _, rest := range rests["nfs"] {
		if nfs, ok := rest.(*Nfs); ok {
			if !nfs.NotReady() {
				wg.Add(1)
				go func(wg *sync.WaitGroup) {
					defer wg.Done()
					if err = nfs.Restore(); err != nil {
						G.logger.Error("cannot restore nfs", zap.Error(err))
					}
				}(&wg)
			}
		}
	}
	wg.Wait()
	for _, rest := range rests["vm"] {
		if vm, ok := rest.(*VM); ok {
			if vm.AgentUuid == G.config.Uuid && vm.PhaseType == PhaseTypeMigrate && vm.PhaseStatus == PhaseStatusSuccess {
				if err = vm.PostMigrate(); err != nil {
					G.logger.Error("cannot post migrate vm", zap.Error(err))
				}
			}
		}
	}
}

func UpdateSgFlow(hostSG *HOSTSGT) {
	flows := GenerateSgFlows(hostSG)
	if err := OvsFlowsLoad(G.config.LanBridge, flows); err != nil {
		G.logger.Error("cannot load flows", zap.Error(err))
	}
}

func UpdateEipFlow(eips *EIPS) {
	flows := GenerateEipFlows(eips)
	if err := OvsFlowsLoad(G.config.WanBridge, flows); err != nil {
		G.logger.Error("cannot load flows", zap.Error(err))
	}
}

func InitShareRes() {
	for {
		var reses []RES
		nids, err := LoadNids()
		if err != nil {
			G.logger.Fatal("cannot load nids", zap.Error(err))
		}
		if nids.GetModrev() == 0 {
			reses = append(reses, nids)
		}
		vlanids, err := LoadVlanids()
		if err != nil {
			G.logger.Fatal("cannot load vlanids", zap.Error(err))
		}
		if vlanids.GetModrev() == 0 {
			reses = append(reses, vlanids)
		}
		macs, err := LoadMacs()
		if err != nil {
			G.logger.Fatal("cannot load macs", zap.Error(err))
		}
		if macs.GetModrev() == 0 {
			reses = append(reses, macs)
		}
		nics, err := LoadNics()
		if err != nil {
			G.logger.Fatal("cannot load nics", zap.Error(err))
		}
		if nics.GetModrev() == 0 {
			reses = append(reses, nics)
		}
		ofports, err := LoadOfports()
		if err != nil {
			G.logger.Fatal("cannot load ofports", zap.Error(err))
		}
		if ofports.GetModrev() == 0 {
			reses = append(reses, ofports)
		}
		vncs, err := LoadVncs()
		if err != nil {
			G.logger.Fatal("cannot load vncs", zap.Error(err))
		}
		if vncs.GetModrev() == 0 {
			reses = append(reses, vncs)
		}
		subnets, err := LoadSubnets()
		if err != nil {
			G.logger.Fatal("cannot load subnets", zap.Error(err))
		}
		if subnets.GetModrev() == 0 {
			reses = append(reses, subnets)
		}
		ports, err := LoadPorts()
		if err != nil {
			G.logger.Fatal("cannot load ports", zap.Error(err))
		}
		if ports.GetModrev() == 0 {
			reses = append(reses, ports)
		}
		eips, err := LoadEips()
		if err != nil {
			G.logger.Fatal("cannot load eips", zap.Error(err))
		}
		if eips.GetModrev() == 0 {
			reses = append(reses, eips)
		}
		hostsg, err := LoadHostSG(G.config.Uuid)
		if err != nil {
			G.logger.Fatal("cannot load hostsg", zap.Error(err))
		}
		if hostsg.GetModrev() == 0 {
			reses = append(reses, hostsg)
		}
		lantest, err := LoadLanTest()
		if err != nil {
			G.logger.Fatal("cannot load lantest", zap.Error(err))
		}
		if lantest.GetModrev() == 0 {
			reses = append(reses, lantest)
		}
		wantest, err := LoadWanTest()
		if err != nil {
			G.logger.Fatal("cannot load wantest", zap.Error(err))
		}
		wantest.WanTestInfo[G.config.Uuid] = make(map[string]int64)
		if _, ok := wantest.WanTestInfo[G.config.Uuid][G.config.WanGwMac]; !ok {
			wantest.WanTestInfo[G.config.Uuid][G.config.WanGwMac] = 0
		}
		if G.config.EipGwMac != "" {
			if _, ok := wantest.WanTestInfo[G.config.Uuid][G.config.EipGwMac]; !ok {
				wantest.WanTestInfo[G.config.Uuid][G.config.EipGwMac] = 0
			}
		}
		reses = append(reses, wantest)
		pools, err := LoadPools()
		if err != nil {
			G.logger.Fatal("cannot load pools", zap.Error(err))
		}
		if pools.GetModrev() == 0 {
			reses = append(reses, pools)
		}
		ml3, err := LoadMl3()
		if err != nil {
			G.logger.Fatal("cannot load ml3", zap.Error(err))
		}
		if ml3.GetModrev() == 0 {
			reses = append(reses, ml3)
		}
		var done bool
		done, err = Save(nil, reses)
		if err != nil {
			G.logger.Fatal("cannot save reses", zap.Error(err))
		}
		if done {
			return
		}
	}
}

func NoticeDnsmasq(vlanid string) {
	G.dhcpCh <- vlanid
}

func NoticeSG() {
	G.sgCh <- ""
}

func SwitchMl3() (toUpdate []RES, err error) {
	ports, err := LoadPorts()
	if err != nil {
		return nil, err
	}
	ports.PortsInfo = make(map[string]map[string]string)
	toUpdate = append(toUpdate, ports)

	subnets, err := LoadSubnets()
	if err != nil {
		return nil, err
	}
	subnets.SubnetsInfo = make(map[string]map[string]string)
	toUpdate = append(toUpdate, subnets)

	restPorts := LoadRests("port")
	for _, rest := range restPorts["port"] {
		port, ok := rest.(*Port)
		if !ok {
			continue
		}
		port.LocalRes = make(map[string][]string)
		port.LocalRes[G.config.Uuid] = []string{"1"}
		if port.AgentUuid != G.config.Uuid {
			port.LocalRes[port.AgentUuid] = []string{"0"}
			G.logger.Debug("switch ml3 ,current agent: " + G.config.Uuid + " port agent: " + port.AgentUuid + " port uuid: " + port.Uuid)
		}
		toUpdate = append(toUpdate, port)

		for k := range port.LocalRes {
			if ports.PortsInfo[k] == nil {
				ports.PortsInfo[k] = make(map[string]string)
			}
			ports.PortsInfo[k][port.Uuid] = port.SubnetUuid
		}
	}

	restSubnets := LoadRests("subnet")
	for _, rest := range restSubnets["subnet"] {
		subnet, ok := rest.(*Subnet)
		if !ok {
			continue
		}
		subnet.LocalRes = make(map[string][]string)
		subnet.LocalRes[G.config.Uuid] = []string{"1"}
		for _, rest := range restPorts["port"] {
			port, ok := rest.(*Port)
			if !ok {
				continue
			}
			if _, ok := subnet.Ports[port.Uuid]; !ok {
				continue
			}
			for k, v := range port.LocalRes {
				if subnet.LocalRes[k] == nil {
					subnet.LocalRes[k] = v
				}
			}
		}
		toUpdate = append(toUpdate, subnet)

		for k := range subnet.LocalRes {
			if subnets.SubnetsInfo[k] == nil {
				subnets.SubnetsInfo[k] = make(map[string]string)
			}
			subnets.SubnetsInfo[k][subnet.Uuid] = subnet.VpcUuid
		}
	}

	restVpcs := LoadRests("vpc")
	for _, rest := range restVpcs["vpc"] {
		vpc, ok := rest.(*VPC)
		if !ok {
			continue
		}
		loclRes0 := vpc.LocalRes
		vpc.LocalRes = make(map[string][]string)
		vpc.AssignLocalResMl3()
		for _, rest := range restSubnets["subnet"] {
			subnet, ok := rest.(*Subnet)
			if !ok {
				continue
			}
			if _, ok := vpc.Subnets[subnet.Uuid]; !ok {
				continue
			}
			for k, v := range subnet.LocalRes {
				if vpc.LocalRes[k] == nil {
					if _, ok := loclRes0[k]; ok {
						vpc.LocalRes[k] = loclRes0[k]
						vpc.LocalRes[k][len(vpc.LocalRes[k])-1] = v[len(v)-1]
					}
				}
			}
		}
		toUpdate = append(toUpdate, vpc)
	}
	return toUpdate, nil
}

func ChooseMl3() (ml3config *Config, err error) {
	G.ml3Lock.Lock()
	defer G.ml3Lock.Unlock()
	for {
		var done bool
		var toUpdates []RES

		ml3, err := LoadMl3()
		if err != nil {
			G.logger.Fatal("cannot load ml3", zap.Error(err))
		}
		if ml3.AgentUuid == "" {
			ml3.AgentUuid = G.config.Uuid
			toUpdates, err = SwitchMl3()
			if err != nil {
				G.logger.Fatal("cannot change ml3", zap.Error(err))
			}
		} else {
			if ml3.AgentUuid != G.config.Uuid {
				agent := LoadAgent(ml3.AgentUuid)
				if agent == nil {
					time.Sleep(5 * time.Second)
					agent := LoadAgent(ml3.AgentUuid)
					if agent == nil {
						ml3.AgentUuid = G.config.Uuid
						toUpdates, err = SwitchMl3()
						if err != nil {
							G.logger.Fatal("cannot change ml3", zap.Error(err))
						}
					} else {
						G.config.MasterL3 = false
						return agent.ConfigData, nil
					}
				} else {
					G.config.MasterL3 = false
					return agent.ConfigData, nil
				}
			} else {
				G.config.MasterL3 = true
				return G.config, nil
			}
		}
		toUpdates = append(toUpdates, ml3)
		if done, err = Save(nil, toUpdates); err != nil {
			G.logger.Fatal("cannot save ml3", zap.Error(err))
		}
		if done {
			G.config.MasterL3 = true
			return G.config, nil
		}
	}
}

func RegistAgentsMonitor() {
	var err error
	if !G.config.L3Mode {
		return
	}
	ml3config, err := ChooseMl3()
	if err != nil {
		G.logger.Fatal("cannot update masterl3", zap.Error(err))
	}
	G.logger.Debug("switch ml3 to: " + ml3config.Uuid)
	session, agent, err := AddAgent(nil, nil)
	if err != nil {
		G.logger.Fatal("cannot add agent", zap.Error(err))
	}
	go func() {
		path := G.config.ROOTKEY + "/agent/"
		wc := G.v3client.Watch(G.ctx, path, v3.WithPrefix())
		defer session.Close()
		for {
			select {
			case <-G.ctx.Done():
				return
			case wresp, ok := <-wc:
				if !ok {

					wc = G.v3client.Watch(G.ctx, path, v3.WithPrefix())
					continue
				}
				for _, ev := range wresp.Events {
					switch ev.Type {
					case v3.EventTypeDelete:
						key := string(ev.Kv.Key)
						isMasterDeleted := strings.HasSuffix(key, "/"+ml3config.Uuid)
						preMasterL3 := G.config.MasterL3

						if isMasterDeleted {
							ml3config, err = ChooseMl3()
							if err != nil {
								G.logger.Fatal("cannot update masterl3", zap.Error(err))
							}
						}
						if preMasterL3 != G.config.MasterL3 || key == agent.GetKey() {
							_, _, err = AddAgent(session, agent)
							if err != nil {
								G.logger.Fatal("cannot add agent", zap.Error(err))
							}
						}
						if G.config.MasterL3 && !preMasterL3 {
							if osIsUnderSystemd() {
								G.logger.Debug("quit current process, let systemd restart it")
								os.Exit(0)
							} else {
								G.logger.Debug("restart current process")
								osRestartCurrentProcess()
							}
						}
					}
				}
			}
		}
	}()
}

func RegisterHaMonitor() {
	if !G.config.VmMode {
		return
	}
	if G.config.HaUuid == "" {
		return
	}
	var haIP string
	go func() {
		ticker := time.NewTicker(5 * time.Second)
		for {
			select {
			case <-G.ctx.Done():
				return
			case <-ticker.C:
				func() {

					SendLanBroadcast("monlport", G.config.LanNicMac, G.config.vlan_from, G.config.Uuid)

					SendICMP("monwport", G.config.WanGwMac, G.config.WanTestIp, 0x3322)
					if G.config.EipGwMac != "" {
						SendICMP("monwport", G.config.EipGwMac, G.config.WanTestIp, 0x3333)
					}
					haAgent := LoadAgent(G.config.HaUuid)

					if haAgent != nil {
						haIP = haAgent.ConfigData.AgentIp
						return
					}
					G.ml3Lock.Lock()
					ml3, err := LoadMl3()
					if err != nil {
						G.logger.Error("cannot load ml3", zap.Error(err))
						G.ml3Lock.Unlock()
						return
					}
					G.ml3Lock.Unlock()

					if ml3.AgentUuid == "" {
						return
					}
					ml3Agent := LoadAgent(ml3.AgentUuid)
					if ml3Agent == nil {
						return
					}

					if haIP == "" {
						return
					}

					if osPing(haIP) {
						return
					}
					vms, err := LoadVms(G.config.HaUuid)
					if err != nil {
						return
					}
					for _, vm := range vms {
						vm.Migrate()
					}
				}()
			}
		}
	}()

	go func() {
		handle, err := pcap.OpenLive("monlport", 1600, true, pcap.BlockForever)
		if err != nil {
			G.logger.Error("cannot open monlport", zap.Error(err))
			return
		}
		defer handle.Close()
		err = handle.SetBPFFilter("inbound and udp and src port 0xfffe and src host 0.0.0.0 and dst host 255.255.255.255")
		if err != nil {
			G.logger.Error("cannot set bpf filter", zap.Error(err))
			return
		}
		packetSource := gopacket.NewPacketSource(handle, handle.LinkType())
		for packet := range packetSource.Packets() {
			udpLayer := packet.Layer(layers.LayerTypeUDP)
			if udpLayer == nil {
				continue
			}
			payload := udpLayer.LayerPayload()
			peerUuid := string(payload)
			currentTime := int64(time.Now().Unix())
			lantest, _ := LoadLanTest()
			if lantest == nil {
				continue
			}
			for {
				if _, ok := lantest.LanTestInfo[G.config.Uuid]; !ok {
					lantest.LanTestInfo[G.config.Uuid] = make(map[string]int64)
				}
				lantest.LanTestInfo[G.config.Uuid][peerUuid] = currentTime
				var done bool
				done, err = Save(nil, []RES{lantest})
				if err != nil {
					G.logger.Error("cannot save lantest", zap.Error(err))
					break
				}
				if done {
					break
				}
			}
		}
	}()

	go func() {
		handle, err := pcap.OpenLive("monwport", 1600, true, pcap.BlockForever)
		if err != nil {
			G.logger.Error("cannot open monwport", zap.Error(err))
			return
		}
		defer handle.Close()
		err = handle.SetBPFFilter("inbound and icmp and icmp[icmptype]==icmp-echoreply and src host " + G.config.WanTestIp)
		if err != nil {
			G.logger.Error("cannot set bpf filter", zap.Error(err))
			return
		}
		packetSource := gopacket.NewPacketSource(handle, handle.LinkType())
		for packet := range packetSource.Packets() {
			icmpLayer := packet.Layer(layers.LayerTypeICMPv4)
			if icmpLayer == nil {
				continue
			}
			id := icmpLayer.(*layers.ICMPv4).Id
			var src_mac string
			switch id {
			case 0x3322:
				src_mac = G.config.WanGwMac
			case 0x3333:
				src_mac = G.config.EipGwMac
			default:
				continue
			}
			currentTime := int64(time.Now().Unix())
			wantest, _ := LoadWanTest()
			if wantest == nil {
				continue
			}
			for {
				if _, ok := wantest.WanTestInfo[G.config.Uuid]; !ok {
					wantest.WanTestInfo[G.config.Uuid] = make(map[string]int64)
				}
				wantest.WanTestInfo[G.config.Uuid][src_mac] = currentTime
				var done bool
				done, err = Save(nil, []RES{wantest})
				if err != nil {
					G.logger.Error("cannot save lantest", zap.Error(err))
					break
				}
				if done {
					break
				}
			}
		}
	}()
}

func RegisterSgMonitor() {

	if !G.config.VmMode {
		return
	}
	go func() {
		hostsg, _ := LoadHostSG(G.config.Uuid)
		UpdateSgFlow(hostsg)
		path := hostsg.GetKey()
		wc := G.v3client.Watch(G.ctx, path)
		for {
			select {
			case <-G.ctx.Done():
				return
			case <-G.sgCh:
				UpdateSgFlow(hostsg)
			case wresp, ok := <-wc:
				if !ok {

					wc = G.v3client.Watch(G.ctx, path)
					continue
				}
				for _, ev := range wresp.Events {
					switch ev.Type {
					case v3.EventTypePut:
						if err := yaml.Unmarshal(ev.Kv.Value, hostsg); err != nil {
							G.logger.Error("cannot unmarshal hostsg", zap.Error(err))
							continue
						}
					}
				}
				UpdateSgFlow(hostsg)
			}
		}
	}()
}

func RegisterDhcpUpdater() {
	var err error
	if !G.config.L3Mode {
		return
	}
	if err = ConfigDnsmasq(); err != nil {
		G.logger.Error("config dnsmasq failed", zap.Error(err))
	}
	rests := LoadRests("vpc")
	for _, rest := range rests["vpc"] {
		vpc, ok := rest.(*VPC)
		if !ok {
			continue
		}
		var localRes []string
		if localRes, err = vpc.AssignLocalRes(true); err != nil {
			continue
		}
		vlanid := localRes[0]
		if err := RestartDnsmasq(vlanid); err != nil {
			G.logger.Error("restartdnsmasq failed", zap.Error(err))
			continue
		}
	}
	go func() {
		vlanids := make(map[string]struct{})
		for {

			select {
			case <-G.ctx.Done():
				return
			case vlanid := <-G.dhcpCh:
				if vlanid != "" {
					vlanids[vlanid] = struct{}{}
				}
				for {
					select {
					case <-G.ctx.Done():
						return
					case vlanid := <-G.dhcpCh:
						if vlanid != "" {
							vlanids[vlanid] = struct{}{}
						}
					default:
						goto NOMORE
					}
				}
			}
		NOMORE:
			if err := ConfigDnsmasq(); err != nil {
				G.logger.Error("configdnsmasq failed", zap.Error(err))
			}
			for vlanid := range vlanids {
				if err := RestartDnsmasq(vlanid); err != nil {
					G.logger.Error("restartdnsmasq failed", zap.Error(err))
					continue
				}
			}
			for k := range vlanids {
				delete(vlanids, k)
			}
		}
	}()
}

func RegisterHostMonitor(vpcUuid string, vlanid string) {
	if G.config.MasterL3 {
		return
	}
	G.ctxes[vpcUuid] = make(chan struct{})
	ctx := G.ctxes[vpcUuid]
	go func() {
		path := G.config.ROOTKEY + "/host/"
		wc := G.v3client.Watch(G.ctx, path, v3.WithPrefix())
		for {
			select {
			case <-ctx:
				G.sysLock.Lock()
				defer G.sysLock.Unlock()
				delete(G.ctxes, vpcUuid)
				return
			case wresp, ok := <-wc:
				if !ok {
					wc = G.v3client.Watch(G.ctx, path)
					continue
				}
				toNotice := false
				for _, ev := range wresp.Events {
					switch ev.Type {
					case v3.EventTypePut:
						_host := Host{}
						if err := yaml.Unmarshal(ev.Kv.Value, &_host); err != nil {
							G.logger.Error("cannot unmarshal host", zap.Error(err))
							continue
						}
						if _host.VpcUuid != vpcUuid {
							continue
						}
						toNotice = true
					}
				}
				if toNotice {
					NoticeDnsmasq(vlanid)
				}
			}
		}
	}()
}

func RegisterPortMonitor(port *Port, subnet *Subnet, vpc *VPC, agent *Agent, vpcLocalRes []string) {
	G.ctxes[port.Uuid] = make(chan struct{})
	ctx := G.ctxes[port.Uuid]
	path := port.GetKey()
	go func() {
		var floating *Floating
		var agentIp string
		var agentPort uint32
		wc := G.v3client.Watch(G.ctx, path)
		for {
			select {
			case <-ctx:
				G.sysLock.Lock()
				defer G.sysLock.Unlock()
				delete(G.ctxes, port.Uuid)
				return
			case wresp, ok := <-wc:
				if !ok {
					wc = G.v3client.Watch(G.ctx, path)
					continue
				}
				for _, ev := range wresp.Events {
					switch ev.Type {
					case v3.EventTypePut:
						_port := Port{}
						if err := yaml.Unmarshal(ev.Kv.Value, &_port); err != nil {
							G.logger.Error("cannot unmarshal port", zap.Error(err))
							continue
						}
						if !G.config.MasterL3 {
							if _port.PhaseType != PhaseTypeBind && _port.PhaseType != PhaseTypeUnbind {
								continue
							}
							if _port.PhaseStatus == PhaseStatusPending {
								floating = _port.Floating
								continue
							}
							if _port.PhaseStatus != PhaseStatusSuccess {
								continue
							}
							switch _port.PhaseType {
							case PhaseTypeBind:
								_ = _port.Bind(subnet, vpc, agent, vpcLocalRes)
							case PhaseTypeUnbind:
								_port.Floating = floating
								_ = _port.UnBind(subnet, vpc, agent, vpcLocalRes)
							}
						} else if G.config.MasterL3 {
							if _port.PhaseType != PhaseTypeResume {
								continue
							}
							if _port.PhaseStatus == PhaseStatusPending {

								agentIp = _port.AgentIp
								agentPort = _port.AgentPort
								continue
							}
							if _port.PhaseStatus != PhaseStatusSuccess {
								continue
							}
							if agent.ConfigData.Uuid != _port.AgentUuid {
								agent = LoadAgent(_port.AgentUuid)

								_ = _port.PhoneHomeSet(agent, vpcLocalRes[0], agentIp, agentPort)
							}
						}
					}
				}
			}
		}
	}()
}

func RegisterEipMonitor() {
	if !G.config.L3Mode {
		return
	}
	go func() {
		eips, _ := LoadEips()
		UpdateEipFlow(eips)
		path := eips.GetKey()
		wc := G.v3client.Watch(G.ctx, path)
		for {
			select {
			case <-G.ctx.Done():
				return
			case wresp, ok := <-wc:
				if !ok {

					wc = G.v3client.Watch(G.ctx, path)
					continue
				}
				for _, ev := range wresp.Events {
					switch ev.Type {
					case v3.EventTypePut:
						if err := yaml.Unmarshal(ev.Kv.Value, eips); err != nil {
							G.logger.Error("cannot unmarshal eipst", zap.Error(err))
							continue
						}
					}
				}
				UpdateEipFlow(eips)
			}
		}
	}()
}

func RegisterVmMonitor() {
	if !G.config.VmMode {
		return
	}
	type VMEvent struct {
		vmUuid string
		event  libvirt.DomainEventType
	}

	eventCh := make(chan VMEvent)
	callback := func(c *libvirt.Connect, d *libvirt.Domain, event *libvirt.DomainEventLifecycle) {
		vmUuid, err := d.GetName()
		if err != nil {
			return
		}
		eventCh <- VMEvent{vmUuid: vmUuid, event: event.Event}
	}

	callbackId, err := G.conn.DomainEventLifecycleRegister(nil, callback)
	if err != nil {
		G.logger.Fatal("cannot register domain event lifecycle", zap.Error(err))
	}
	go func() {
		for {
			select {
			case <-G.ctx.Done():
				goto EXIT
			case vmEvent := <-eventCh:
				event := vmEvent.event
				vmUuid := vmEvent.vmUuid
				G.logger.Debug("vm event", zap.String("vmUuid", vmUuid), zap.Int("event", int(event)))
				for {
					vm, err := LoadVM(vmUuid)
					if err != nil {
						G.logger.Error("cannot load vm", zap.Error(err))
						break
					}
					if vm == nil {
						G.logger.Error("vm not found", zap.String("vmUuid", vmUuid))
						break
					}
					switch event {
					case libvirt.DOMAIN_EVENT_DEFINED:

						if vm.Power == "" || vm.Power == "undefined" {
							vm.Power = "shut off"
						}
					case libvirt.DOMAIN_EVENT_STARTED:
						vm.Power = "running"
					case libvirt.DOMAIN_EVENT_STOPPED:
						vm.Power = "shut off"
					case libvirt.DOMAIN_EVENT_RESUMED:
						vm.Power = "running"
					case libvirt.DOMAIN_EVENT_SUSPENDED:
						vm.Power = "suspended"
					case libvirt.DOMAIN_EVENT_SHUTDOWN:
						vm.Power = "shutdown"
					case libvirt.DOMAIN_EVENT_UNDEFINED:
						vm.Power = "undefined"
					default:
					}
					var done bool
					done, err = Save(nil, []RES{vm})
					if err != nil {
						G.logger.Error("cannot save vm", zap.Error(err))
						break
					}
					if done {
						break
					}
				}
			}
		}
	EXIT:
		G.conn.DomainEventDeregister(callbackId)
	}()
}

func RegisterMeterMonitor() {
	if !G.config.VmMode {
		return
	}
	go func() {
		ticker := time.NewTicker(60 * time.Second)
		isFirst := true
		for {
			select {
			case <-G.ctx.Done():
				return
			case <-ticker.C:
				func() {
					ofports, err := LoadOfports()
					if err != nil {
						return
					}
					if len(ofports.OfportsInfo[G.config.Uuid]) == 0 {
						return
					}
					stats, err := GetMetersStat(G.config.LanBridge)
					if err != nil {
						return
					}
					for ofport, portUuid := range ofports.OfportsInfo[G.config.Uuid] {
						if _, ok := stats[ofport]; !ok {
							continue
						}
						portstat, _ := LoadPortStat(portUuid)
						if portstat == nil {
							continue
						}
						t := time.Unix(portstat.Stat.LastTime, 0)
						for {
							if t.Year() != time.Now().Year() || t.Month() != time.Now().Month() {
								portstat.Stat.BytesOfMonth = stats[ofport] - portstat.Stat.BytesOfMeter
							} else {
								if isFirst {
									portstat.Stat.BytesOfMonth += stats[ofport]
								} else {
									portstat.Stat.BytesOfMonth += stats[ofport] - portstat.Stat.BytesOfMeter
								}
							}
							portstat.Stat.BytesOfMeter = stats[ofport]
							portstat.Stat.LastTime = time.Now().Unix()
							var done bool
							done, err = Save(nil, []RES{portstat})
							if err != nil {
								return
							}
							if done {
								break
							}

						}
					}
					isFirst = false
				}()
			}
		}
	}()
}
