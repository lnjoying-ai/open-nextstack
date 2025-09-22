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
)

func CreateDockerVolume(volId string, volSize uint32) (err error) {
	cmd := fmt.Sprintf("docker volume inspect %s", volId)
	if err = ExecuteCmdRun(G.config.CmdTimeout*10, cmd); err == nil {
		return nil
	}
	cmd = fmt.Sprintf("docker volume create -d docker-volume-loopback --label agent=%s -o sparse=true -o fs=ext4 -o size=%dGiB %s", G.config.Uuid, volSize, volId)
	return ExecuteCmdRun(G.config.CmdTimeout*10, cmd)
}

func DeleteDockerVolume(volId string) (err error) {
	cmd := fmt.Sprintf("docker volume inspect %s", volId)
	if err = ExecuteCmdRun(G.config.CmdTimeout*10, cmd); err != nil {
		return nil
	}
	cmd = fmt.Sprintf("docker volume rm %s", volId)
	return ExecuteCmdRun(G.config.CmdTimeout*10, cmd)
}

func StartContainer(containerId string, params string, image string, version string) (err error) {
	cmd := fmt.Sprintf("docker inspect -f {{.State.Running}} %s", containerId)
	output, err := ExecuteCmdOutput(G.config.CmdTimeout*10, cmd)
	if err != nil {
		cmd := fmt.Sprintf("docker run -d --name %s --network none --restart=always %s %s", containerId, params, image)
		if version != "" {
			cmd = cmd + ":" + version
		}
		return ExecuteCmdRun(G.config.CmdTimeout*10, cmd)
	}
	if strings.Contains(string(output), "false") {
		cmd := fmt.Sprintf("docker start %s", containerId)
		return ExecuteCmdRun(G.config.CmdTimeout*10, cmd)
	}
	return nil
}

func RestartContainer(containerId string) (err error) {
	cmd := fmt.Sprintf("docker restart %s", containerId)
	return ExecuteCmdRun(G.config.CmdTimeout*10, cmd)
}

func ReloadContainer(containerId string) (err error) {
	cmd := fmt.Sprintf("docker kill -s HUP %s", containerId)
	return ExecuteCmdRun(G.config.CmdTimeout*10, cmd)
}

func DeleteContainer(containerId string) (err error) {
	cmd := fmt.Sprintf("docker inspect -f {{.State.Running}} %s", containerId)
	err = ExecuteCmdRun(G.config.CmdTimeout*10, cmd)
	if err != nil {
		return
	}
	cmd = fmt.Sprintf("docker rm -f %s", containerId)
	return ExecuteCmdRun(G.config.CmdTimeout*10, cmd)
}

func EnableContainerNetns(containerId string) (err error) {
	cmd := fmt.Sprintf("docker inspect -f {{.State.Pid}} %s", containerId)
	output, err := ExecuteCmdOutput(G.config.CmdTimeout*10, cmd)
	if err != nil {
		return err
	}
	pid := strings.TrimSpace(string(output))
	if pid == "" || pid == "0" {
		return fmt.Errorf("pid of container %s is %s", containerId, pid)
	}
	if ok, _ := FileExists(fmt.Sprintf("/var/run/netns/%s", containerId)); ok {
		os.Remove(fmt.Sprintf("/var/run/netns/%s", containerId))
	}
	if err = os.MkdirAll("/var/run/netns", 0755); err != nil {
		return err
	}
	cmd = fmt.Sprintf("ln -sfT /proc/%s/ns/net /var/run/netns/%s", pid, containerId)
	if err = ExecuteCmdRun(G.config.CmdTimeout*10, cmd); err != nil {
		return err
	}
	cmd = fmt.Sprintf("ip netns exec %s sysctl -w net.ipv4.conf.all.rp_filter=0", containerId)
	return ExecuteCmdRun(G.config.CmdTimeout*10, cmd)
}

func DisableContainerNetns(containerId string) (err error) {
	if ok, _ := FileExists(fmt.Sprintf("/var/run/netns/%s", containerId)); ok {
		os.Remove(fmt.Sprintf("/var/run/netns/%s", containerId))
	}
	return nil
}

func ConnectContainerNetns(containerId string, nic string, mac string, ip string, prefix string, vlanid string, ofport string, gw string) (err error) {
	nsNic := nic + ".n"
	if !osNicExists(nic, "") {
		if err = osVethAdd(nic, nsNic); err != nil {
			return err
		}
		if err = osNicMacSet(nsNic, mac); err != nil {
			return err
		}
	}
	if err = osNicUp(nic, ""); err != nil {
		return err
	}
	if err = OvsNicAdd(G.config.LanBridge, nic, false, ofport, vlanid); err != nil {
		return err
	}
	if !osNicExists(nsNic, containerId) {
		if err = osNsNicAdd(containerId, nsNic); err != nil {
			return err
		}
	}
	if err = osNicUp(nsNic, containerId); err != nil {
		return err
	}
	var yes bool
	ip = fmt.Sprintf("%s/%s", ip, prefix)
	if yes, err = osNicHashIp(nsNic, ip, containerId); err != nil {
		return err
	}
	if !yes {
		if err = osNicIpAdd(nsNic, ip, containerId); err != nil {
			return err
		}
		if err = osRouteAdd("default", gw, containerId); err != nil {
			return err
		}
	}
	return nil
}
