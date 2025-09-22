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
	"encoding/json"
	"fmt"
	"log"
	"net"
	"os"
	"os/exec"
	"path"
	"regexp"
	"strconv"
	"strings"
	"syscall"
	"time"

	"github.com/google/gopacket"
	"github.com/google/gopacket/layers"
	"github.com/google/gopacket/pcap"
	"go.uber.org/zap"
)

type QemuImgInfo struct {
	Size        uint64 `json:"virtual-size"`
	BackingFile string `json:"backing-filename"`
	Format      string `json:"format"`
	ActualSize  uint64 `json:"actual-size"`
}

func osNetnsAdd(netns string) (err error) {
	if osNetnsExists(netns) {
		return nil
	}
	cmd := fmt.Sprintf("ip netns add %s", netns)
	return ExecuteCmdRun(G.config.CmdTimeout, cmd)
}

func osNetnsExists(netns string) bool {
	var cmd string
	if netns == "" {
		cmd = "ip netns ip address"
	} else {
		cmd = fmt.Sprintf("ip netns exec %s ip address", netns)
	}
	return ExecuteCmdRun(G.config.CmdTimeout, cmd) == nil
}

func osNetnsDel(netns string) error {
	cmd := fmt.Sprintf("ip netns delete %s", netns)
	return ExecuteCmdRun(G.config.CmdTimeout, cmd)
}

func osNetnsPids(netns string) (pids []int, err error) {
	pids = []int{}
	cmd := fmt.Sprintf("ip netns pids %s", netns)
	output, err := ExecuteCmdOutput(G.config.CmdTimeout, cmd)
	if err != nil {
		return nil, err
	}
	lines := strings.Split(string(output), "\n")
	for _, line := range lines {
		if line != "" {
			line = strings.TrimSpace(line)
			if line == "" {
				continue
			}
			if pid, err := strconv.Atoi(line); err == nil {
				pids = append(pids, pid)
			}
		}
	}
	return pids, nil
}

func getProcessName(pid int) (string, error) {
	commPath := path.Join("/proc", strconv.Itoa(pid), "comm")
	content, err := os.ReadFile(commPath)
	if err != nil {
		return "", err
	}
	return strings.TrimSpace(string(content)), nil
}

func osKillPids(netns string, process string) error {
	pids, err := osNetnsPids(netns)
	if err != nil {
		return err
	}
	for _, pid := range pids {
		p, _ := os.FindProcess(pid)
		if p == nil {
			continue
		}
		name, _ := getProcessName(pid)
		if name == process {
			p.Kill()
			p.Wait()
		}
	}
	time.Sleep(10 * time.Millisecond)
	return nil
}

func osNetnsKillAll(ns string) error {
	pids, err := osNetnsPids(ns)
	if err != nil {
		return err
	}
	for _, pid := range pids {
		p, _ := os.FindProcess(pid)
		if p == nil {
			continue
		}
		p.Kill()
		p.Wait()
	}
	return nil
}

func osAllNetnsDel() error {
	cmd := "ip netns list"
	output, err := ExecuteCmdOutput(G.config.CmdTimeout, cmd)
	if err != nil {
		return err
	}
	lines := strings.Split(string(output), "\n")
	var netns []string
	for _, line := range lines {
		if strings.HasPrefix(line, "ns-") {
			ns := strings.Split(line, " ")[0]
			netns = append(netns, ns)
		}
	}
	for _, ns := range netns {
		if err := osNetnsKillAll(ns); err != nil {
			return err
		}
	}
	for _, ns := range netns {
		cmd = fmt.Sprintf("ip netns delete %s", ns)
		err := ExecuteCmdRun(G.config.CmdTimeout, cmd)
		if err != nil {
			return err
		}
	}
	return nil
}

func osIptablesExec(ns string, cmd string) error {
	re, _ := regexp.Compile(`^(iptables\s+-t\s+\w+)\s+-([AIDF])\s*(.*)$`)
	cmd = strings.TrimSpace(cmd)
	matches := re.FindStringSubmatch(cmd)
	if len(matches) != 4 {
		G.logger.Fatal("invalid iptables command", zap.String("cmd", cmd))
	}
	nsstr := ""
	if ns != "" {
		nsstr = fmt.Sprintf("ip netns exec %s ", ns)
	}
	var rule string
	if matches[2] == "I" {

		r, _ := regexp.Compile(`(\S+)\s+\d+\s*(.*)`)
		m := r.FindStringSubmatch(matches[3])
		if len(m) == 3 {
			rule = m[1] + " " + m[2]
		} else {
			G.logger.Error("invalid iptables command", zap.String("cmd", cmd))
			return fmt.Errorf("invalid iptables command")
		}
		cmd = fmt.Sprintf("%s%s -C %s", nsstr, matches[1], rule)
	} else {
		cmd = fmt.Sprintf("%s%s -C %s", nsstr, matches[1], matches[3])
	}
	err := ExecuteCmdRun(G.config.CmdTimeout, cmd)

	if err == nil {

		if matches[2] == "A" {
			return nil
		}

		if matches[2] == "I" {
			cmd = fmt.Sprintf("%s%s -D %s", nsstr, matches[1], rule)
			err := ExecuteCmdRun(G.config.CmdTimeout, cmd)
			if err != nil {
				return err
			}
		}
	} else {

		if matches[2] == "D" {
			return nil
		}
	}
	cmd = fmt.Sprintf("%s%s -%s %s", nsstr, matches[1], matches[2], matches[3])
	err = ExecuteCmdRun(G.config.CmdTimeout, cmd)
	if err != nil {
		return err
	}
	return nil
}

func osIptablesRest() error {
	cmd := "iptables-save -t nat |grep L3AGENT"
	output, _ := ExecuteShCmdOutput(G.config.CmdTimeout, cmd)
	lines := strings.Split(string(output), "\n")
	for _, line := range lines {
		line = strings.TrimSpace(line)
		if line == "" {
			continue
		}
		cmd = strings.Replace(line, "-A", "iptables -t nat -D", 1)
		if err := ExecuteCmdRun(G.config.CmdTimeout, cmd); err != nil {
			return err
		}
	}
	return nil
}

func osIpForwardEnable() (err error) {
	cmd := "sysctl -w net.ipv4.ip_forward=1"
	if err = ExecuteCmdRun(G.config.CmdTimeout, cmd); err != nil {
		return err
	}
	cmd = "iptables -t filter -P FORWARD ACCEPT"
	return ExecuteCmdRun(G.config.CmdTimeout, cmd)
}

func osNicMac(nic string, ns string) (mac string, err error) {
	var output []byte
	if ns == "" {
		cmd := fmt.Sprintf("ip link show %s", nic)
		if output, err = ExecuteCmdOutput(G.config.CmdTimeout, cmd); err != nil {
			return "", err
		}
	} else {
		cmd := fmt.Sprintf("ip netns exec %s ip link show dev %s", ns, nic)
		if output, err = ExecuteCmdOutput(G.config.CmdTimeout, cmd); err != nil {
			return "", err
		}
	}
	lines := strings.Split(string(output), "\n")
	for _, line := range lines {
		if strings.Contains(line, "link/ether") {
			mac := strings.Fields(line)[1]
			fmt.Println(mac)
			return mac, nil
		}
	}
	return "", fmt.Errorf("mac address not found")
}

func osNicAddrFlush(nic string) error {
	cmd := fmt.Sprintf("ip addr flush dev %s", nic)
	return ExecuteCmdRun(G.config.CmdTimeout, cmd)
}

func osIpNonlocalBindEnable() error {

	if !G.config.L3Mode {
		return nil
	}
	cmd := "sysctl -w net.ipv4.ip_nonlocal_bind=1"
	return ExecuteCmdRun(G.config.CmdTimeout, cmd)
}

func osNicUp(nic string, ns string) error {
	var cmd string
	if ns == "" {
		cmd = fmt.Sprintf("ip link set dev %s up", nic)
	} else {
		cmd = fmt.Sprintf("ip netns exec %s ip link set dev %s up", ns, nic)
	}
	return ExecuteCmdRun(G.config.CmdTimeout, cmd)
}

func osNicIpAdd(nic string, ip string, ns string) error {
	var cmd string
	if ns == "" {
		cmd = fmt.Sprintf("ip addr add %s dev %s", ip, nic)
	} else {
		cmd = fmt.Sprintf("ip netns exec %s ip addr add %s dev %s", ns, ip, nic)
	}
	return ExecuteCmdRun(G.config.CmdTimeout, cmd)
}

func osNicIpDel(nic string, ip string, ns string) error {
	var cmd string
	if ns == "" {
		cmd = fmt.Sprintf("ip addr del %s dev %s", ip, nic)
	} else {
		cmd = fmt.Sprintf("ip netns exec %s ip addr del %s dev %s", ns, ip, nic)
	}
	return ExecuteCmdRun(G.config.CmdTimeout, cmd)
}

func osNicExists(nic string, ns string) bool {
	var cmd string
	if ns == "" {
		cmd = fmt.Sprintf("ip link show %s", nic)
	} else {
		cmd = fmt.Sprintf("ip netns exec %s ip link show dev %s", ns, nic)
	}
	return ExecuteCmdRun(G.config.CmdTimeout, cmd) == nil
}

func osNicDel(nic string, ns string) error {
	var cmd string
	if ns == "" {
		cmd = fmt.Sprintf("ip link delete %s", nic)
	} else {
		cmd = fmt.Sprintf("ip netns exec %s ip link delete %s", ns, nic)
	}
	return ExecuteCmdRun(G.config.CmdTimeout, cmd)
}

func osNicHashIp(nic string, ip string, ns string) (yes bool, err error) {
	var cmd string
	if ns == "" {
		cmd = fmt.Sprintf("ip address show dev %s", nic)
	} else {
		cmd = fmt.Sprintf("ip netns exec %s ip address show dev %s", ns, nic)
	}
	var output []byte
	if output, err = ExecuteCmdOutput(G.config.CmdTimeout, cmd); err != nil {
		return false, err
	}
	return strings.Contains(string(output), ip), nil
}

func osVlanNicAdd(nic string, vlanNic, vlanid string) error {
	cmd := fmt.Sprintf("ip link add link %s name %s type vlan id %s", vlanNic, nic, vlanid)
	return ExecuteCmdRun(G.config.CmdTimeout, cmd)
}

func osVethAdd(nic1 string, nic2 string) error {
	cmd := fmt.Sprintf("ip link add %s type veth peer name %s", nic1, nic2)
	return ExecuteCmdRun(G.config.CmdTimeout, cmd)
}

func osNicMacSet(nic string, mac string) error {
	cmd := fmt.Sprintf("ip link set dev %s address %s", nic, mac)
	return ExecuteCmdRun(G.config.CmdTimeout, cmd)
}

func osNsNicAdd(ns string, nic string) error {
	cmd := fmt.Sprintf("ip link set %s netns %s", nic, ns)
	return ExecuteCmdRun(G.config.CmdTimeout, cmd)
}

func osDnatAdd(nic string, protocol string, ip string, port uint32, targetPort uint32, insideIp string, ns string) (err error) {
	var cmd string
	switch protocol {
	case "ip":
		cmd = fmt.Sprintf("iptables -t nat -A PREROUTING -i %s -d %s/32 -j DNAT --to-destination %s", nic, ip, insideIp)
	case "tcp", "udp":
		if port == 0 {
			cmd = fmt.Sprintf("iptables -t nat -A PREROUTING -i %s -p %s -d %s/32 -j DNAT --to-destination %s", nic, protocol, ip, insideIp)
		} else {
			cmd = fmt.Sprintf("iptables -t nat -A PREROUTING -i %s -p %s -d %s/32 --dport %d -j DNAT --to-destination %s:%d", nic, protocol, ip, port, insideIp, targetPort)
		}
	}
	return osIptablesExec(ns, cmd)
}

func osDnatDel(nic string, protocol string, ip string, port uint32, targetPort uint32, insideIp string, ns string) (err error) {
	var cmd string
	switch protocol {
	case "ip":
		cmd = fmt.Sprintf("iptables -t nat -D PREROUTING -i %s -d %s/32 -j DNAT --to-destination %s", nic, ip, insideIp)
	case "tcp", "udp":
		if port == 0 {
			cmd = fmt.Sprintf("iptables -t nat -D PREROUTING -i %s -p %s -d %s/32 -j DNAT --to-destination %s", nic, protocol, ip, insideIp)
		} else {
			cmd = fmt.Sprintf("iptables -t nat -D PREROUTING -i %s -p %s -d %s/32 --dport %d -j DNAT --to-destination %s:%d", nic, protocol, ip, port, insideIp, targetPort)
		}
	}
	return osIptablesExec(ns, cmd)
}

func osSnatAdd(nic string, ns string) error {
	cmd := fmt.Sprintf("iptables -t nat -A POSTROUTING -o %s -j MASQUERADE", nic)
	return osIptablesExec(ns, cmd)
}

func osSnatAdd2(insideIp string, wanVpcNsNic string, eip string, lanVpcNsNic string, ns string) (err error) {
	var cmd string

	cmd = fmt.Sprintf("iptables -t nat -I POSTROUTING 1 -s %s -o %s -j SNAT --to-source %s", insideIp, wanVpcNsNic, eip)
	if err = osIptablesExec(ns, cmd); err != nil {
		return err
	}

	cmd = fmt.Sprintf("iptables -t nat -I PREROUTING 1 -s %s -d %s -j DNAT --to-destination %s", insideIp, eip, insideIp)
	if err = osIptablesExec(ns, cmd); err != nil {
		return err
	}
	cmd = fmt.Sprintf("iptables -t nat -I POSTROUTING 1 -s %s -d %s -o %s -j MASQUERADE", insideIp, insideIp, lanVpcNsNic)
	return osIptablesExec(ns, cmd)
}

func osSnatDel2(insideIp string, wanVpcNsNic string, eip string, lanVpcNsNic string, ns string) (err error) {
	var cmd string

	cmd = fmt.Sprintf("iptables -t nat -D POSTROUTING -s %s -o %s -j SNAT --to-source %s", insideIp, wanVpcNsNic, eip)
	if err = osIptablesExec(ns, cmd); err != nil {
		return err
	}

	cmd = fmt.Sprintf("iptables -t nat -D PREROUTING -s %s -d %s -j DNAT --to-destination %s", insideIp, eip, insideIp)
	if err = osIptablesExec(ns, cmd); err != nil {
		return err
	}
	cmd = fmt.Sprintf("iptables -t nat -D POSTROUTING -s %s -d %s -o %s -j MASQUERADE", insideIp, insideIp, lanVpcNsNic)
	return osIptablesExec(ns, cmd)
}

func osSnatAdd3(srcIp string) error {
	cmd := fmt.Sprintf("iptables -t nat -A POSTROUTING -s %s -m comment --comment L3AGENT -j MASQUERADE", srcIp)
	return osIptablesExec("", cmd)
}

func osSnatDel3(srcIp string) (err error) {
	cmd := fmt.Sprintf("iptables -t nat -D POSTROUTING -s %s -m comment --comment L3AGENT -j MASQUERADE", srcIp)
	return osIptablesExec("", cmd)
}

func osArping(ip string, nic string) error {
	cmd := fmt.Sprintf("arping -c 2 -U -I %s %s", nic, ip)
	return ExecuteCmdRun(G.config.CmdTimeout, cmd)
}

func osRouteAdd(dest string, via string, ns string) error {
	var cmd string
	if ns != "" {
		cmd = fmt.Sprintf("ip netns exec %s ip route add %s via %s", ns, dest, via)
	} else {
		cmd = fmt.Sprintf("ip route add %s via %s", dest, via)
	}
	return ExecuteCmdRun(G.config.CmdTimeout, cmd)
}

func osRouteDevAdd(dest string, dev string, ns string) error {
	var cmd string
	if ns != "" {
		cmd = fmt.Sprintf("ip netns exec %s ip route add %s dev %s", ns, dest, dev)
	} else {
		cmd = fmt.Sprintf("ip address add %s dev %s", dest, dev)
	}
	return ExecuteCmdRun(G.config.CmdTimeout, cmd)
}

func osRoute169Exists(ns string) bool {
	cmd := fmt.Sprintf("ip netns exec %s ip route show table 169", ns)
	return ExecuteCmdRun(G.config.CmdTimeout, cmd) == nil
}

func osRoute169Add(nic string, ns string, gwIp string) error {
	var cmd string
	if !osRoute169Exists(ns) {
		cmd = fmt.Sprintf("ip netns exec %s ip route add default via %s dev %s table 169", ns, gwIp, nic)
		_ = ExecuteCmdRun(G.config.CmdTimeout, cmd)
		if !osRoute169Exists(ns) {
			return fmt.Errorf("failed to add route table 169")
		}
	}
	err := osIpRule169Add(ns)
	if err != nil {
		return err
	}

	cmd = fmt.Sprintf("ip netns exec %s sysctl -w net.ipv4.conf.all.rp_filter=0", ns)
	return ExecuteCmdRun(G.config.CmdTimeout, cmd)
}

func osIpRule169Exists(ns string) bool {
	cmd := fmt.Sprintf("ip netns exec %s ip rule show from all fwmark 0xa9 lookup 169", ns)
	output, _ := ExecuteCmdOutput(G.config.CmdTimeout, cmd)
	return strings.Contains(string(output), "lookup 169")
}

func osIpRule169Add(ns string) error {
	if !osIpRule169Exists(ns) {
		cmd := fmt.Sprintf("ip netns exec %s ip rule add from all fwmark 0xa9 lookup 169", ns)
		err := ExecuteCmdRun(G.config.CmdTimeout, cmd)
		if err != nil {
			return err
		}
	}
	return nil
}

func GetQemuImgInfo(img string) (info *QemuImgInfo, err error) {
	cmd := "qemu-img info --output=json " + img
	var output []byte
	output, err = ExecuteCmdOutput(G.config.CmdTimeout, cmd)
	if err != nil {
		return nil, err
	}
	info = &QemuImgInfo{}
	if err = json.Unmarshal(output, info); err != nil {
		G.logger.Fatal("failed to unmarshal qemu-img info", zap.Error(err))
	}
	return info, nil
}

func QemuImgConvert(src string, dst string, sformat string, dformat string) error {
	cmd := fmt.Sprintf("qemu-img convert -t writeback -f %s -O %s %s %s", sformat, dformat, src, dst)
	return ExecuteCmdRun(G.config.CmdTimeout*100, cmd)
}

func QemuImgResize(img string, size uint32) error {
	cmd := fmt.Sprintf("qemu-img resize %s %dG", img, size)
	return ExecuteCmdRun(G.config.CmdTimeout*100, cmd)
}

func QemuImgCreate(img string, size uint32, backing string) error {
	var cmd string
	if backing != "" {
		cmd = fmt.Sprintf("qemu-img create -f qcow2 -F qcow2 -b %s %s", backing, img)
	} else {
		cmd = fmt.Sprintf("qemu-img create -f qcow2 %s", img)
	}
	if size > 0 {
		cmd = cmd + fmt.Sprintf(" %dG", size)
	}
	return ExecuteCmdRun(G.config.CmdTimeout*100, cmd)
}

func DirExists(dirPath string) (bool, error) {
	info, err := os.Stat(dirPath)
	if os.IsNotExist(err) {
		return false, nil
	}
	if err != nil {
		return false, err
	}
	return info.IsDir(), nil
}

func FileExists(filePath string) (bool, error) {
	info, err := os.Stat(filePath)
	if os.IsNotExist(err) {
		return false, nil
	}
	if err != nil {
		return false, err
	}
	return !info.IsDir(), nil
}

func osMkdir(path string) error {
	exist, err := DirExists(path)
	if err != nil {
		return err
	}
	if exist {
		return nil
	}
	if err := os.MkdirAll(path, 0755); err != nil {
		return err
	}
	return nil
}

func getSubdirectories(root string) ([]string, error) {
	var subdirs []string

	entries, err := os.ReadDir(root)
	if err != nil {
		return nil, err
	}

	for _, entry := range entries {
		if entry.IsDir() {
			subdirs = append(subdirs, entry.Name())
		}
	}
	return subdirs, nil
}

func getSubfiles(root string) ([]string, error) {
	var subdirs []string
	entries, err := os.ReadDir(root)
	if err != nil {
		return nil, err
	}
	for _, entry := range entries {
		if !entry.IsDir() {
			subdirs = append(subdirs, entry.Name())
		}
	}
	return subdirs, nil
}

func osDistro() string {
	cmd := "lsb_release -is"
	output, err := exec.Command("sh", "-c", cmd).Output()
	if err != nil {
		return "centos"
	}
	distro := strings.ToLower(strings.TrimSpace(string(output)))
	if distro == "ubuntu" {
		return "ubuntu"
	}
	return "centos"
}

func osChown2Qemu(dir string) error {
	var cmd string
	if G.distro == "ubuntu" {
		cmd = "chown -R libvirt-qemu:libvirt-qemu " + dir
	} else {
		cmd = "chown -R qemu:qemu " + dir
	}
	return ExecuteCmdRun(G.config.CmdTimeout, cmd)
}

func osHugepageGet() int {
	cmd := "cat /proc/meminfo | grep HugePages_Total|awk '{print $2}'"
	output, err := ExecuteShCmdOutput(G.config.CmdTimeout, cmd)
	if err != nil {
		return 0
	}
	hugepage, err := strconv.Atoi(strings.TrimSpace(string(output)))
	if err != nil {
		return 0
	}
	return hugepage
}

func osTapNicAdd(nic string) error {
	cmd := fmt.Sprintf("ip tuntap add %s mode tap", nic)
	return ExecuteCmdRun(G.config.CmdTimeout, cmd)
}

func isFileLocked(filepath string) bool {
	cmd := "qemu-img info " + filepath
	_, err := ExecuteCmdOutput(G.config.CmdTimeout, cmd)
	return err != nil
}

func osClearMangle(ns string) error {
	cmd := "iptables -t mangle -F"
	return osIptablesExec(ns, cmd)
}

func osDhcpAllow(ns string, nic string) error {
	cmd := "iptables -t mangle -A PREROUTING -i " + nic + " -s 0.0.0.0 -j ACCEPT"
	return osIptablesExec(ns, cmd)
}

func osConntrackAllow(ns string, nic string) error {
	cmd := "iptables -t mangle -A PREROUTING -i " + nic + " -m state --state ESTABLISHED,RELATED -j ACCEPT"
	return osIptablesExec(ns, cmd)
}

func osDropInvalid(ns string, nic string, cidr string) error {
	cmd := "iptables -t mangle -A PREROUTING -i " + nic + " ! -s " + cidr + " -j DROP"
	return osIptablesExec(ns, cmd)
}

func osAllowVpc(ns string, nic string, cidr string) error {
	cmd := "iptables -t mangle -A PREROUTING -i " + nic + " -d " + cidr + " -j ACCEPT"
	return osIptablesExec(ns, cmd)
}

func osMark169(ns string, nic string) error {
	cmd := "iptables -t mangle -A PREROUTING -i " + nic + " -p tcp -d 169.254.169.254 --dport 80 -j MARK --set-mark 0xa9"
	return osIptablesExec(ns, cmd)
}

func osAllowMark169(ns string, nic string) error {
	cmd := "iptables -t mangle -A PREROUTING -i " + nic + " -m mark --mark 0xa9 -j ACCEPT"
	return osIptablesExec(ns, cmd)
}

func osPing(ip string) (pintable bool) {
	cmd := fmt.Sprintf("ping -c 3 %s", ip)
	err := ExecuteCmdRun(G.config.CmdTimeout, cmd)
	return err == nil
}

func osIsUnderSystemd() bool {
	pid := os.Getpid()
	cgroup := fmt.Sprintf("/proc/%d/cgroup", pid)
	content, err := os.ReadFile(cgroup)
	if err != nil {
		return false
	}
	return strings.Contains(string(content), "system.slice")
}

func osRestartCurrentProcess() error {
	args := os.Args
	executable, err := os.Executable()
	if err != nil {
		return err
	}
	err = syscall.Exec(executable, args, os.Environ())
	if err != nil {
		return err
	}
	return nil
}

func SendLanBroadcast(nic string, mac string, vlanid uint32, agentid string) error {

	handle, err := pcap.OpenLive(nic, 1600, true, pcap.BlockForever)
	if err != nil {
		G.logger.Error("failed to open veth device", zap.String("nic", nic), zap.Error(err))
		return err
	}
	defer handle.Close()

	macAddr, err := net.ParseMAC(mac)
	if err != nil {
		G.logger.Error("failed to parse mac address", zap.String("mac", mac), zap.Error(err))
		return nil
	}

	ethLayer := &layers.Ethernet{
		SrcMAC:       macAddr,
		DstMAC:       net.HardwareAddr{0xff, 0xff, 0xff, 0xff, 0xff, 0xff},
		EthernetType: layers.EthernetTypeIPv4,
	}

	ipLayer := &layers.IPv4{
		Version:  4,
		IHL:      5,
		TTL:      64,
		SrcIP:    net.IPv4(0, 0, 0, 0),
		DstIP:    net.IPv4(255, 255, 255, 255),
		Protocol: layers.IPProtocolUDP,
	}

	v := uint16(vlanid)
	udpLayer := &layers.UDP{
		SrcPort: layers.UDPPort(0xfffe),
		DstPort: layers.UDPPort(v),
	}
	udpLayer.SetNetworkLayerForChecksum(ipLayer)

	payload := make([]byte, len(agentid))
	copy(payload[:], agentid)

	buffer := gopacket.NewSerializeBuffer()
	options := gopacket.SerializeOptions{
		FixLengths:       true,
		ComputeChecksums: true,
	}
	err = gopacket.SerializeLayers(buffer, options,
		ethLayer,
		ipLayer,
		udpLayer,
		gopacket.Payload(payload),
	)
	if err != nil {
		log.Fatalf("Error serializing layers: %v", err)
	}

	outgoingPacket := buffer.Bytes()

	err = handle.WritePacketData(outgoingPacket)
	if err != nil {
		log.Fatalf("Error writing packet to device: %v", err)
	}

	fmt.Println("Packet sent successfully")
	return nil
}

func SendICMP(nic string, mac string, ip string, id uint16) (err error) {

	handle, err := pcap.OpenLive(nic, 1600, true, pcap.BlockForever)
	if err != nil {
		G.logger.Error("failed to open veth device", zap.String("nic", nic), zap.Error(err))
		return err
	}
	defer handle.Close()

	srcMac, err := net.ParseMAC(G.config.WanNicMac)
	if err != nil {
		G.logger.Error("failed to parse mac address", zap.String("mac", G.config.WanGwMac), zap.Error(err))
		return nil
	}
	dstMac, err := net.ParseMAC(mac)
	if err != nil {
		G.logger.Error("failed to parse mac address", zap.String("mac", mac), zap.Error(err))
		return nil
	}

	ethLayer := &layers.Ethernet{
		SrcMAC:       srcMac,
		DstMAC:       dstMac,
		EthernetType: layers.EthernetTypeIPv4,
	}

	ipLayer := &layers.IPv4{
		Version:  4,
		IHL:      5,
		TTL:      64,
		SrcIP:    net.ParseIP(G.config.DefaultEip),
		DstIP:    net.ParseIP(ip),
		Protocol: layers.IPProtocolICMPv4,
	}

	icmpLayer := &layers.ICMPv4{
		TypeCode: layers.CreateICMPv4TypeCode(layers.ICMPv4TypeEchoRequest, 0),
		Id:       id,
	}

	buffer := gopacket.NewSerializeBuffer()
	options := gopacket.SerializeOptions{
		FixLengths:       true,
		ComputeChecksums: true,
	}
	err = gopacket.SerializeLayers(buffer, options,
		ethLayer,
		ipLayer,
		icmpLayer,
	)
	if err != nil {
		log.Fatalf("Error serializing layers: %v", err)
	}

	outgoingPacket := buffer.Bytes()

	err = handle.WritePacketData(outgoingPacket)
	if err != nil {
		log.Fatalf("Error writing packet to device: %v", err)
	}

	fmt.Println("Packet sent successfully")
	return nil
}
