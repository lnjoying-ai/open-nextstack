// Copyright 2024 The GNEXT Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//	http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package gnextmain

import (
	"bufio"
	"context"
	"fmt"
	"math"
	"math/rand/v2"
	"net"
	"os"
	"os/exec"
	"regexp"
	"strconv"
	"strings"
	"time"

	"go.uber.org/zap"
)

const LIMIT uint32 = 65535

type AppListReq struct {
	Abnormal bool `json:"abnormal"`
	Local    bool `json:"local"`
}

type AppRsp struct {
	Status string `json:"status"`
	Uuid   string `json:"uuid"`
}

type AppErrorRsp struct {
	Status string `json:"status"`
	Reason string `json:"reason"`
}

func Contains[T comparable](arr []T, elem T) bool {
	for _, a := range arr {
		if a == elem {
			return true
		}
	}
	return false
}

func Remove[T comparable](arr []T, elem T) []T {
	for i, a := range arr {
		if a == elem {
			return append(arr[:i], arr[i+1:]...)
		}
	}
	return arr
}

func GetKeys[T comparable, V any](m map[T]V) []T {
	keys := []T{}
	for k := range m {
		keys = append(keys, k)
	}
	return keys
}

func ConflictPorts(a PROTO2PORTRANGEST, b PROTO2PORTRANGEST) bool {

	if a["ip"] != nil || b["ip"] != nil {
		return true
	}
	for proto, ports := range a {

		if b[proto] == nil {
			continue
		}

		for _, port := range ports {
			if Contains(b[proto], port) {
				return true
			}
		}
	}
	return false
}

func IsSubnet(n *net.IPNet, s *net.IPNet) bool {
	sp, _ := s.Mask.Size()
	np, _ := n.Mask.Size()
	return n.Contains(s.IP) && sp >= np
}

func IsOverlap(n1 *net.IPNet, n2 *net.IPNet) bool {
	return n1.Contains(n2.IP) || n2.Contains(n1.IP)
}

func IpFromCidr(cidr string, idx []int) ([]net.IP, uint32, string) {
	ip, ipnet, _ := net.ParseCIDR(cidr)
	ip = ip.To4()
	prefix, _ := ipnet.Mask.Size()
	n := uint32(math.Pow(2, float64(32-prefix)))
	ips := []net.IP{}
	ipInt := uint32(ip[0])<<24 | uint32(ip[1])<<16 | uint32(ip[2])<<8 | uint32(ip[3])
	for _, i := range idx {
		var ipInt1 uint32
		if i < 0 {
			ipInt1 = ipInt + n - uint32(-i)
		} else {
			ipInt1 = ipInt + uint32(i)
		}

		newIP := net.IPv4(
			byte(ipInt1>>24),
			byte(ipInt1>>16),
			byte(ipInt1>>8),
			byte(ipInt1),
		)
		ips = append(ips, newIP)
	}
	mask := net.CIDRMask(prefix, 32)
	i := net.IP(mask)
	return ips, uint32(prefix), i.String()
}

func GenIp(cidr string) net.IP {
	ip, ipnet, _ := net.ParseCIDR(cidr)
	ip = ip.To4()
	prefix, _ := ipnet.Mask.Size()
	n := uint32(math.Pow(2, float64(32-prefix)))
	ipInt := uint32(ip[0])<<24 | uint32(ip[1])<<16 | uint32(ip[2])<<8 | uint32(ip[3])
	ipInt = ipInt + 2 + uint32(rand.Uint32N(uint32(n-6)))
	return net.IPv4(
		byte(ipInt>>24),
		byte(ipInt>>16),
		byte(ipInt>>8),
		byte(ipInt),
	)
}

func MaskFromCidr(cidr string) string {
	_, ipnet, _ := net.ParseCIDR(cidr)
	return ipnet.Mask.String()
}

func SubnetIps(subnetCidr string) []string {
	ips, prefix, mask := IpFromCidr(subnetCidr, []int{1, -2, 2, -5})
	ipStrs := []string{}
	ipStrs = append(ipStrs, fmt.Sprintf("%s/%d", ips[0].String(), prefix))
	ipStrs = append(ipStrs, fmt.Sprintf("%s/%d", ips[1].String(), prefix))
	ipStrs = append(ipStrs, ips[0].String())
	ipStrs = append(ipStrs, ips[1].String())
	ipStrs = append(ipStrs, ips[2].String())
	ipStrs = append(ipStrs, ips[3].String())
	ipStrs = append(ipStrs, mask)
	return ipStrs
}

func VpcIps(vlanid string) []string {

	vlanidInt, _ := AtoU32(vlanid)
	ip_part_a := vlanidInt >> 4 & 0xFF
	ip_part_b := vlanidInt << 4 & 0xF0
	return []string{
		fmt.Sprintf("100.%d.%d.1", ip_part_a, ip_part_b),
		fmt.Sprintf("100.%d.%d.2", ip_part_a, ip_part_b),
		fmt.Sprintf("100.%d.%d.3", ip_part_a, ip_part_b),
		fmt.Sprintf("100.%d.%d.4", ip_part_a, ip_part_b),
	}
}

func WanVethPair(vlanid string) []string {
	return []string{"wan." + vlanid, "wan." + vlanid + ".n"}
}

func LanVethPair(vlanid string) []string {
	return []string{"lan." + vlanid, "lan." + vlanid + ".n"}
}

func ManVethPair(vlanid string) []string {
	return []string{"man." + vlanid, "man." + vlanid + ".n"}
}

func MonVethPair(lan bool) []string {
	if lan {
		return []string{"monlport", "monlportb"}
	}
	return []string{"monwport", "monwportb"}
}

func GetLanVpcNsNic(vlanid string) string {
	return LanVethPair(vlanid)[1]
}

func GetWanVpNscNic(vlanid string) string {
	return WanVethPair(vlanid)[1]
}

func GetNsName(vlanid string) string {
	return "ns-" + vlanid
}

func NewAppErrorRsp(reason string) *AppErrorRsp {
	return &AppErrorRsp{
		Status: "failed",
		Reason: reason,
	}
}

func NewAppPendingRsp(uuid string) *AppRsp {
	return &AppRsp{
		Status: "pending",
		Uuid:   uuid,
	}
}

func AppOKRsp(uuid string) *AppRsp {
	return &AppRsp{
		Status: "ok",
		Uuid:   uuid,
	}
}

func RandString(n int) string {
	const chars = "abcdefghijklmnopqrstuvwxyz0123456789"
	b := make([]byte, n)
	for i := range b {
		b[i] = chars[rand.Int32N(int32(len(chars)))]
	}
	return string(b)
}

func GenMac(purpose string) string {

	switch purpose {
	case "haproxy":
		return fmt.Sprintf("52:54:02:%02x:%02x:%02x", rand.Int32N(256), rand.Int32N(256), rand.Int32N(256))
	case "nfs":
		return fmt.Sprintf("52:54:03:%02x:%02x:%02x", rand.Int32N(256), rand.Int32N(256), rand.Int32N(256))
	case "vpc":
		return fmt.Sprintf("52:54:04:%02x:%02x:%02x", rand.Int32N(256), rand.Int32N(256), rand.Int32N(256))
	}

	return fmt.Sprintf("52:54:00:%02x:%02x:%02x", rand.Int32N(256), rand.Int32N(256), rand.Int32N(256))
}

func GenNic() string {
	return "vnet-" + RandString(8)
}

func GenVncPort() string {
	for {
		port := rand.Uint32N(65535-6100) + 6100
		address := fmt.Sprintf("%d", port)
		listener, err := net.Listen("tcp", address)
		if err != nil {
			return fmt.Sprintf("%d", port)
		}
		defer listener.Close()
	}
}

func ParseKVs(kvstr string) map[string]string {
	kvs := map[string]string{}
	for _, kv := range strings.Split(kvstr, ",") {
		kvpair := strings.Split(kv, ":")
		if len(kvpair) == 2 {
			kvs[strings.ToUpper(kvpair[0])] = strings.ToLower(kvpair[1])
		}
	}
	return kvs
}

func ParseSgPort(protocol string, portStr string) (port string, err error) {
	icmp_ports := map[string]string{
		"echo":                 "8-0",
		"echo_reply":           "0-0",
		"fragment_need_sf_set": "3-4",
		"host_redirect":        "5-1",
		"host_tos_redirect":    "5-3",
		"host_unreachable":     "3-1",
		"information_reply":    "16",
		"information_request":  "15",
		"net_redirect":         "5-0",
		"net_tos_redirect":     "5-2",
		"net_unreachable":      "3-0",
		"parameter_problem":    "12",
		"port_unreachable":     "3-3",
		"protocol_unreachable": "3-2",
		"reassembly_timeout":   "11-1",
		"source_quench":        "4",
		"source_route_failed":  "3-5",
		"timestamp_reply":      "14",
		"timestamp_request":    "15",
		"ttl_exceeded":         "11-0",
	}
	if portStr == "all" {
		return "all", nil
	}
	if protocol == "icmp" {
		ports := map[string]string{}
		parts := strings.Split(portStr, "+")
		for _, part := range parts {
			if _, ok := icmp_ports[part]; !ok {
				return "", fmt.Errorf("invalid icmp port: %s", part)
			}
			ports[part] = icmp_ports[part]
		}
		for _, value := range ports {
			if port == "" {
				port = value
			} else {
				port += "+" + value
			}
		}
		return port, nil
	}
	if protocol == "tcp" || protocol == "udp" {

		ports := [][2]uint32{}
		parts := strings.Split(portStr, "+")
		for _, part := range parts {
			part = strings.TrimSpace(part)
			var start, end uint32
			if strings.Contains(part, "-") {
				rangeParts := strings.Split(part, "-")
				if len(rangeParts) != 2 {
					return "", fmt.Errorf("invalid port range: %s", part)
				}
				start, err = AtoU32(rangeParts[0])
				if err != nil {
					return "", fmt.Errorf("invalid port range: %s", part)
				}
				end, err = AtoU32(rangeParts[1])
				if err != nil {
					return "", fmt.Errorf("invalid port range: %s", part)
				}
				if start > end {
					return "", fmt.Errorf("invalid port range: %s", part)
				}
			} else {
				start, err = AtoU32(part)
				if err != nil {
					return "", fmt.Errorf("invalid port: %s", part)
				}
				end = start
			}
			for _, r := range ports {
				if (start >= r[0] && start <= r[1]) || (end >= r[0] && end <= r[1]) {
					return "", fmt.Errorf("port range conflict: %s", part)
				}
			}
			ports = append(ports, [2]uint32{start, end})
		}
		for _, r := range ports {
			if port == "" {
				port = fmt.Sprintf("%d-%d", r[0], r[1])
			} else {
				port += fmt.Sprintf("+%d-%d", r[0], r[1])
			}
		}
		return port, nil
	}
	return "", fmt.Errorf("invalid protocol: %s", protocol)
}

func VerifySgAddr(addr string) error {
	var err error
	var sg *SG
	if addr == "all" {
		return nil
	}
	if addr == "default" {
		return nil
	}

	if strings.Contains(addr, ".") {
		if _, _, err := net.ParseCIDR(addr); err != nil {
			return err
		}
		return nil
	}

	sgUuids := strings.Split(addr, "+")
	for _, sgUuid := range sgUuids {
		if sg, err = LoadSG(sgUuid); err != nil {
			return err
		}
		if sg.NotReady() {
			return fmt.Errorf("sg %s is not ready", addr)
		}
	}
	return nil
}

func MaxPort(port uint32, mask uint32) uint32 {
	xid := LIMIT - mask
	nid := port & mask
	return nid + xid
}

func PortMask(port uint32, end uint32) uint32 {
	var bit uint32 = 1
	mask := LIMIT
	test_mask := LIMIT
	net := port & LIMIT
	max_p := MaxPort(net, LIMIT)
	for net != 0 && max_p < end {
		net = port & test_mask
		if net < port {
			break
		}
		max_p = MaxPort(net, test_mask)
		if max_p <= end {
			mask = test_mask
		}
		test_mask -= bit
		bit <<= 1
	}
	return mask
}

func MaskRange(startStr string, endStr string) []string {
	port_masks := []string{}
	start, _ := AtoU32(startStr)
	end, _ := AtoU32(endStr)
	port := start
	for port <= end {
		mask := PortMask(port, end)
		port_mask := fmt.Sprintf("0x%x/0x%x", port, mask)
		port_masks = append(port_masks, port_mask)
		port = MaxPort(port, mask) + 1
	}
	return port_masks
}

func GetRuleAddrs(sg *SG, rule *Rule, nid string) []string {
	addrs := []string{}
	if rule.Addr == "all" {
		return addrs
	}
	if rule.Addr == "default" {
		return sg.Addrs[nid]
	}
	if strings.Contains(rule.Addr, ".") {
		return []string{rule.Addr}
	}
	sgUuids := strings.Split(rule.Addr, "+")
	for _, sgUuid := range sgUuids {
		sg, _ := LoadSG(sgUuid)
		if sg == nil {
			continue
		}
		if sg.Addrs[nid] != nil {
			for _, addr := range sg.Addrs[nid] {
				if !Contains(addrs, addr) {
					addrs = append(addrs, addr)
				}
			}
		}
	}
	return addrs
}

func ConfigDnsmasq() error {
	f, err := os.Create(G.config.dnsmasq_config)
	if err != nil {
		return err
	}
	defer func() {
		f.Close()
	}()
	var buff string
	for _, dnsmasq_option := range G.config.DnsmasqOptions {
		buff += fmt.Sprintf("%s\n", dnsmasq_option)
	}
	buff += "\n"
	for _, dns_server := range G.config.DnsServers {
		buff += fmt.Sprintf("server=%s\n", dns_server)
	}
	buff += "\n"

	rests := LoadRests("")
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
		lan_nic := fmt.Sprintf("lan.%s.n", vlanid)
		idx := 1
		macs := []string{}
		for subnetUuid := range vpc.Subnets {
			s := &Subnet{SHAREREST: SHAREREST{UUIDINFO: UUIDINFO{Uuid: subnetUuid}, ETCDINFO: ETCDINFO{Type: "subnet"}}}
			rest := rests["subnet"][s.GetKey()]
			if rest == nil {
				continue
			}
			subnet, ok := rest.(*Subnet)
			if !ok {
				continue
			}
			subnet_tag := fmt.Sprintf("subnet.%d", idx)
			idx += 1
			ips := SubnetIps(subnet.Cidr)
			str := fmt.Sprintf("dhcp-range=tag:%s,tag:%s,%s,%s,%s,infinite\n", subnet_tag, lan_nic, ips[4], ips[5], ips[6])
			buff += str
			buff += fmt.Sprintf("dhcp-option=tag:%s,tag:%s,option:router,%s\n", subnet_tag, lan_nic, ips[2])
			buff += fmt.Sprintf("dhcp-option=tag:%s,tag:%s,option:dns-server,%s\n", subnet_tag, lan_nic, ips[3])
			buff += "\n"
			for portUuid := range subnet.Ports {
				p := &Port{SHAREREST: SHAREREST{UUIDINFO: UUIDINFO{Uuid: portUuid}, ETCDINFO: ETCDINFO{Type: "port"}}}
				rest := rests["port"][p.GetKey()]
				if rest == nil {
					continue
				}
				port, ok := rest.(*Port)
				if !ok {
					continue
				}
				if port.LocalRes[G.config.Uuid] != nil {
					buff += fmt.Sprintf("dhcp-mac=set:%s,%s\n", subnet_tag, port.Mac)
					buff += fmt.Sprintf("dhcp-host=tag:%s,%s,%s,id:*\n", lan_nic, port.Mac, port.Ip)
					macs = append(macs, port.Mac)
				}
			}
		}
		for hostUuid := range vpc.Hosts {
			h := &Host{SHAREREST: SHAREREST{UUIDINFO: UUIDINFO{Uuid: hostUuid}, ETCDINFO: ETCDINFO{Type: "host"}}}
			rest := rests["host"][h.GetKey()]
			if rest == nil {
				continue
			}
			host, ok := rest.(*Host)
			if !ok {
				continue
			}
			buff += fmt.Sprintf("address=/%s/%s@%s\n", host.Hostname, host.Ip, lan_nic)

			if !strings.Contains(host.Hostname, ".") {
				buff += fmt.Sprintf("address=/%s.lan/%s@%s\n", host.Hostname, host.Ip, lan_nic)
			}
		}
		lease_file := fmt.Sprintf("%s.%s", G.config.dnsmasq_lease, vlanid)
		{
			file_data := ""
			var lease *os.File
			if lease, err = os.OpenFile(lease_file, os.O_RDWR|os.O_CREATE, 0644); err != nil {
				return err
			}
			defer func() {
				lease.Close()
			}()
			scanner := bufio.NewScanner(lease)
			for scanner.Scan() {
				line := scanner.Text()
				for _, mac := range macs {
					if strings.Contains(line, mac) {
						file_data += line + "\n"
						break
					}
				}
			}
			if err := scanner.Err(); err != nil {
				G.logger.Error("Scanner failed", zap.Error(err))
				return err
			}
			if _, err := lease.Seek(0, 0); err != nil {
				G.logger.Error("Seek failed", zap.Error(err))
				return err
			}
			// 先截断文件
			if err = lease.Truncate(0); err != nil {
				G.logger.Error("Truncate failed", zap.Error(err))
				return err
			}
			// 重新定位到文件开头
			if _, err = lease.Seek(0, 0); err != nil {
				G.logger.Error("Seek failed", zap.Error(err))
				return err
			}
			// 写入新内容
			if _, err = lease.WriteString(file_data); err != nil {
				G.logger.Error("WriteString failed", zap.Error(err))
				return err
			}
			// 同步到磁盘
			if err = lease.Sync(); err != nil {
				G.logger.Error("Sync failed", zap.Error(err))
				return err
			}
		}
	}
	if _, err := f.WriteString(buff); err != nil {
		return err
	}
	return nil
}

func RestartDnsmasq(vlanid string) error {
	ns := fmt.Sprintf("ns-%s", vlanid)

	if !osNetnsExists(ns) {
		return nil
	}
	err := osKillPids(ns, "dnsmasq")
	if err != nil {
		return err
	}
	leaseFile := fmt.Sprintf("%s.%s", G.config.dnsmasq_lease, vlanid)
	cmd := fmt.Sprintf("ip netns exec %s %s/bin/dnsmasq -C %s -l %s", ns, G.config.AgentHome, G.config.dnsmasq_config, leaseFile)
	if err := ExecuteCmdRun(G.config.CmdTimeout, cmd); err != nil {
		G.logger.Error("restartdnsmasq failed", zap.Error(err))
		return err
	}
	return nil
}

func isValidHostnameSegment(segment string) bool {

	if len(segment) < 1 || len(segment) > 63 || strings.HasPrefix(segment, "-") || strings.HasSuffix(segment, "-") {
		return false
	}

	re := regexp.MustCompile(`^[A-Za-z\d-]+$`)
	return re.MatchString(segment)
}

func AtoU32(a string) (uint32, error) {
	u, err := strconv.Atoi(a)
	if err != nil {
		return 0, err
	}
	if u < 0 || u > math.MaxUint32 {
		return 0, fmt.Errorf("invalid uint32: %d", u)
	}
	return uint32(u), nil
}

func AtoU16(a string) (uint16, error) {
	u, err := strconv.Atoi(a)
	if err != nil {
		return 0, err
	}
	if u < 0 || u > math.MaxUint16 {
		return 0, fmt.Errorf("invalid uint16: %d", u)
	}
	return uint16(u), nil
}

func BytesToGB(bytes uint64) uint32 {
	gb := float64(bytes) / (1024 * 1024 * 1024)
	return uint32(math.Ceil(gb))
}

func ExecuteCmd(timeOut time.Duration, cmd string) *exec.Cmd {
	G.logger.WithOptions(zap.AddCallerSkip(2)).Debug("ExecuteCmd", zap.String("cmd", cmd))
	cmds := strings.Fields(cmd)
	if timeOut > 0 {
		ctx, cancel := context.WithTimeout(G.ctx, timeOut*time.Second)
		go func() {
			<-ctx.Done()
			cancel()
		}()
		if len(cmds) == 1 {
			return exec.CommandContext(ctx, cmds[0])
		} else {
			return exec.CommandContext(ctx, cmds[0], cmds[1:]...)
		}
	}
	if len(cmds) == 1 {
		return exec.Command(cmds[0])
	} else {
		return exec.Command(cmds[0], cmds[1:]...)
	}
}

func ExecuteCmdRun(timeOut time.Duration, cmd string) error {
	if err := ExecuteCmd(timeOut, cmd).Run(); err != nil {
		return err
	}
	return nil
}

func ExecuteCmdOutput(timeOut time.Duration, cmd string) ([]byte, error) {
	output, err := ExecuteCmd(timeOut, cmd).Output()
	if err != nil {
		return nil, err
	}
	return output, nil
}

func ExecuteShCmd(timeOut time.Duration, cmd string) *exec.Cmd {
	G.logger.WithOptions(zap.AddCallerSkip(2)).Debug("ExecuteShCmd", zap.String("cmd", cmd))
	if timeOut > 0 {
		ctx, cancel := context.WithTimeout(G.ctx, timeOut*time.Second)
		go func() {
			<-ctx.Done()
			cancel()
		}()
		return exec.CommandContext(ctx, "sh", "-c", cmd)
	}
	return exec.Command("sh", "-c", cmd)
}

func ExecuteShCmdRun(timeOut time.Duration, cmd string) error {
	if err := ExecuteShCmd(timeOut, cmd).Run(); err != nil {
		return err
	}
	return nil
}

func ExecuteShCmdOutput(timeOut time.Duration, cmd string) ([]byte, error) {
	output, err := ExecuteShCmd(timeOut, cmd).Output()
	if err != nil {
		return nil, err
	}
	return output, nil
}

func HostSgs2Update(sgUuids []string) (hostSgs map[string]*HOSTSGT) {
	var err error
	var sgs []*SG

	rests := LoadRests("sg")
	for _, rest := range rests["sg"] {
		sg, ok := rest.(*SG)
		if !ok {
			continue
		}

		if Contains(sgUuids, sg.GetUuid()) {
			sgs = append(sgs, sg)
			continue
		}

		for _, rule := range sg.Rules {
			if rule.Addr == "all" {
				continue
			}
			if strings.Contains(rule.Addr, ".") {
				continue
			}

			uuids := strings.Split(rule.Addr, "+")
			for _, u := range uuids {
				if Contains(sgUuids, u) {
					sgs = append(sgs, sg)
					goto NEXT
				}
			}
		}
	NEXT:
	}
	hostSgs = map[string]*HOSTSGT{}

	for _, sg := range sgs {
		for _, portUuid := range sg.Ports {
			var port *Port
			var hostsg *HOSTSGT
			if port, err = LoadPort(portUuid); err != nil {
				return nil
			}
			if port == nil {
				continue
			}
			if _, ok := hostSgs[port.AgentUuid]; !ok {
				hostsg, _ = LoadHostSG(port.AgentUuid)
				hostSgs[port.AgentUuid] = hostsg
			}
		}
	}
	return hostSgs
}
