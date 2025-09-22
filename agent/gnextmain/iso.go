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
	"os"
	"text/template"

	"github.com/kdomanski/iso9660"
	"go.uber.org/zap"
)

func GenMetaData(os string, paras map[string]string) (buf *bytes.Buffer) {
	windows_metadata := "instance-id: {{.INSTANCE_ID}}\n"
	linux_metadata := "" +
		"instance-id: {{.INSTANCE_ID}}\n" +
		"local-hostname: {{.HOSTNAME}}"
	var meta_template *template.Template
	if os == "windows" {
		meta_template, _ = template.New("METADATA").Parse(windows_metadata)
	} else {
		meta_template, _ = template.New("METADATA").Parse(linux_metadata)
	}
	var resultBuffer bytes.Buffer
	if err := meta_template.Execute(&resultBuffer, paras); err != nil {
		G.logger.Fatal("GenMetaData", zap.Error(err))
	}
	return &resultBuffer
}

func GenNetData(os string, nics []map[string]string) (buf *bytes.Buffer) {
	windows_netdata := "" +
		"version: 1\n" +
		"config:\n" +
		"{{.NICS}}\n"
	windows_nic_static := "" +
		"  - type: physical\n" +
		"    name: interface{{.IDX}}\n" +
		"    mac_address: \"{{.MAC}}\"\n" +
		"    subnets:\n" +
		"      - type: static\n" +
		"        address: {{.IP}}\n" +
		"        netmask: {{.MASK}}\n" +
		"        gateway: {{.GATEWAY}}\n" +
		"        dns_nameservers:\n" +
		"          - {{.DNS}}\n" +
		"        dns_search:\n" +
		"          - lan\n"
	windows_nic_dhcp := "" +
		"  - type: physical\n" +
		"    name: interface{{.IDX}}\n" +
		"    mac_address: \"{{.MAC}}\"\n" +
		"    subnets:\n" +
		"      - type: dhcp\n"
	linux_netdata := "" +
		"version: 2\n" +
		"ethernets:\n" +
		"{{.NICS}}"
	linux_nic_static := "" +
		"    id{{.IDX}}:\n" +
		"        set-name: eth{{.IDX}}\n" +
		"        match:\n" +
		"            macaddress: '{{.MAC}}'\n" +
		"        addresses:\n" +
		"            - {{.IP}}/{{.PREFIX}}\n" +
		"        gateway4: {{.GATEWAY}}\n" +
		"\n" +
		"        nameservers:\n" +
		"            search: [lan]\n" +
		"            addresses: [{{.DNS}}]\n" +
		"        routes:\n" +
		"          - to: 169.254.169.254/32\n" +
		"            via: {{.DNS}}\n"
	linux_nic_dhcp := "" +
		"    id{{.IDX}}:\n" +
		"        set-name: eth{{.IDX}}\n" +
		"        match:\n" +
		"            macaddress: '{{.MAC}}'\n" +
		"        dhcp4: true\n" +
		"        routes:\n" +
		"          - to: 169.254.169.254/32\n" +
		"            via: {{.DNS}}\n"
	var static_template *template.Template
	var dhcp_template *template.Template
	var netdata_template *template.Template
	if os == "windows" {
		netdata_template, _ = template.New("WINDOWSNETDATA").Parse(windows_netdata)
		static_template, _ = template.New("WINDOWSNICSTATIC").Parse(windows_nic_static)
		dhcp_template, _ = template.New("WINDOWSNICDHCP").Parse(windows_nic_dhcp)
	} else {
		netdata_template, _ = template.New("LINUXNETDATA").Parse(linux_netdata)
		static_template, _ = template.New("LINUXNICSTATIC").Parse(linux_nic_static)
		dhcp_template, _ = template.New("LINUXNICDHCP").Parse(linux_nic_dhcp)
	}
	allNics := ""
	for _, nic := range nics {
		var resultBuffer bytes.Buffer
		if nic["STATIC"] == "true" {
			if err := static_template.Execute(&resultBuffer, nic); err != nil {
				G.logger.Fatal("GenNetData", zap.Error(err))
			}
		} else {
			if err := dhcp_template.Execute(&resultBuffer, nic); err != nil {
				G.logger.Fatal("GenNetData", zap.Error(err))
			}
		}
		allNics = allNics + resultBuffer.String() + "\n"
	}
	data := map[string]string{
		"NICS": allNics,
	}
	var resultBuffer bytes.Buffer
	if err := netdata_template.Execute(&resultBuffer, data); err != nil {
		G.logger.Fatal("GenNetData", zap.Error(err))
	}
	return &resultBuffer
}

func GenUserData(os string, paras map[string]string) (buf *bytes.Buffer) {
	windows_userdata := "" +
		"#cloud-config\n" +
		"set_timezone: Asia/Shanghai\n" +
		"set_hostname: {{.HOSTNAME}}\n" +
		"write_files:\n" +
		"  path: C:\\Windows\\Temp\\phone_home.py\n" +
		"  permissions: '0755'\n" +
		"  content: |\n" +
		"    #! /usr/bin/env python\n" +
		"    from cloudbaseinit.osutils import factory as osutils_factory\n" +
		"    from cloudbaseinit.utils import serialization\n" +
		"    import os\n" +
		"    import json\n" +
		"    import requests\n" +
		"    import time\n" +
		"    import socket\n" +
		"    import subprocess\n" +
		"    def get_cdrom_driver():\n" +
		"        no_cloud_label = \"cidata\"\n" +
		"        osutils = osutils_factory.get_os_utils()\n" +
		"        for drive in osutils.get_cdrom_drives():\n" +
		"            label = osutils.get_volume_label(drive)\n" +
		"            if no_cloud_label == label:\n" +
		"                return drive\n" +
		"    def get_instance_id():\n" +
		"        drive = get_cdrom_driver()\n" +
		"        norm_path = os.path.normpath(os.path.join(drive ,\"meta-data\"))\n" +
		"        raw_data = ''\n" +
		"        with open(norm_path, 'rb') as stream:\n" +
		"            raw_data = stream.read()\n" +
		"        meta_data = serialization.parse_json_yaml(raw_data)\n" +
		"        return meta_data[\"instance-id\"]\n" +
		"    \n" +
		"    def get_ip_addr():\n" +
		"        ip_addresses = []\n" +
		"        result = subprocess.run([\"ipconfig\"], capture_output=True, text=True)\n" +
		"        for line in result.stdout.splitlines():\n" +
		"            if \"IPv4 \" in line:\n" +
		"                ip_address = line.split(\":\")[-1].strip()\n" +
		"                ip_addresses.append(ip_address)\n" +
		"        return ip_addresses\n" +
		"    def wait_for_ip():\n" +
		"        while True:\n" +
		"            ip_addresses = get_ip_addr()\n" +
		"            if \"{{.IP}}\" in ip_addresses:\n" +
		"                return\n" +
		"            time.sleep(5)\n" +
		"    instance_id = get_instance_id()\n" +
		"    wait_for_ip()\n" +
		"    url = \"http://169.254.169.254/v1/phonehome/%s\" %(instance_id)\n" +
		"    requests.post(url)\n" +
		"users:\n" +
		"  - name: Administrator\n" +
		"    primary_group: administrators\n" +
		"    passwd: '{{.PASSWORD}}'\n" +
		"    inactive: False\n" +
		"groups:\n" +
		"  - administrators\n" +
		"runcmd:\n" +
		"  - \"route ADD 169.254.169.254 MASK 255.255.255.255 {{.DNS}} -p\"\n" +
		"  - ['python', 'C:\\Windows\\Temp\\phone_home.py']"
	linux_userdata := "" +
		"#cloud-config\n" +
		"runcmd:\n" +
		"  - passwd -l root\n" +
		"\n" +
		"system_info:\n" +
		"  default_user:\n" +
		"    name: {{.USERNAME}}\n" +
		"    sudo: 'ALL=(ALL) NOPASSWD:ALL'\n" +
		"    plain_text_passwd: {{.PASSWORD}}\n" +
		"    gecos: Default Cloud User\n" +
		"    lock_passwd: false\n" +
		"\n" +
		"ssh_authorized_keys:\n" +
		"  - {{.SSH_PUB_KEY}}\n" +
		"\n" +
		"disable_root: true\n" +
		"ssh_pwauth: true\n" +
		"allow_public_ssh_keys: true\n" +
		"\n" +
		"growpart:\n" +
		"  mode: auto\n" +
		"  devices: [\"/dev/sda1\", \"/dev/sda2\", \"/dev/sda3\", \"/dev/sda4\", \"/dev/sda5\"]\n" +
		"  ignore_growroot_disabled: false\n" +
		"\n" +
		"phone_home:\n" +
		"    url: http://169.254.169.254/v1/phonehome/$INSTANCE_ID\n" +
		"    post:\n" +
		"      - instance_id\n" +
		"      - hostname\n"
	linux_userdata_with_password := "" +
		"#cloud-config\n" +
		"runcmd:\n" +
		"  - passwd -l root\n" +
		"\n" +
		"system_info:\n" +
		"  default_user:\n" +
		"    name: {{.USERNAME}}\n" +
		"    sudo: 'ALL=(ALL) NOPASSWD:ALL'\n" +
		"    plain_text_passwd: {{.PASSWORD}}\n" +
		"    gecos: Default Cloud User\n" +
		"    lock_passwd: false\n" +
		"\n" +
		"disable_root: true\n" +
		"ssh_pwauth: true\n" +
		"allow_public_ssh_keys: true\n" +
		"\n" +
		"growpart:\n" +
		"  mode: auto\n" +
		"  devices: [\"/dev/sda1\", \"/dev/sda2\", \"/dev/sda3\", \"/dev/sda4\", \"/dev/sda5\"]\n" +
		"  ignore_growroot_disabled: false\n" +
		"\n" +
		"phone_home:\n" +
		"    url: http://169.254.169.254/v1/phonehome/$INSTANCE_ID\n" +
		"    post:\n" +
		"      - instance_id\n" +
		"      - hostname\n"
	linux_userdata_with_pubkey := "" +
		"#cloud-config\n" +
		"runcmd:\n" +
		"  - passwd -l root\n" +
		"\n" +
		"system_info:\n" +
		"  default_user:\n" +
		"    name: {{.USERNAME}}\n" +
		"    sudo: 'ALL=(ALL) NOPASSWD:ALL'\n" +
		"    gecos: Default Cloud User\n" +
		"    lock_passwd: true\n" +
		"\n" +
		"ssh_authorized_keys:\n" +
		"  - {{.SSH_PUB_KEY}}\n" +
		"\n" +
		"disable_root: true\n" +
		"ssh_pwauth: false\n" +
		"allow_public_ssh_keys: true\n" +
		"\n" +
		"growpart:\n" +
		"  mode: auto\n" +
		"  devices: [\"/dev/sda1\", \"/dev/sda2\", \"/dev/sda3\", \"/dev/sda4\", \"/dev/sda5\"]\n" +
		"  ignore_growroot_disabled: false\n" +
		"\n" +
		"phone_home:\n" +
		"    url: http://169.254.169.254/v1/phonehome/$INSTANCE_ID\n" +
		"    post:\n" +
		"      - instance_id\n" +
		"      - hostname\n"
	var userdata_template *template.Template
	if os == "windows" {
		userdata_template, _ = template.New("WINDOWSUSERDATA").Parse(windows_userdata)
	} else {
		password := paras["PASSWORD"]
		pubkey := paras["SSH_PUB_KEY"]
		if password != "" && pubkey != "" {
			userdata_template, _ = template.New("LINUXUSERDATA").Parse(linux_userdata)
		} else if password != "" {
			userdata_template, _ = template.New("LINUXUSERDATAWITHPASSWORD").Parse(linux_userdata_with_password)
		} else if pubkey != "" {
			userdata_template, _ = template.New("LINUXUSERDATAWITHPUBKEY").Parse(linux_userdata_with_pubkey)
		}
	}
	var resultBuffer bytes.Buffer
	if err := userdata_template.Execute(&resultBuffer, paras); err != nil {
		G.logger.Fatal("GenUserData", zap.Error(err))
	}
	return &resultBuffer
}

func GenISO(vmUuid string, paras map[string]string, nics []map[string]string, instanceid string) (isoFileName string, err error) {
	metaData := GenMetaData(paras["OS"], paras)
	userData := GenUserData(paras["OS"], paras)
	netData := GenNetData(paras["OS"], nics)
	writer, err := iso9660.NewWriter()
	if err != nil {
		G.logger.Fatal("GenISO", zap.Error(err))
	}
	err = writer.AddFile(metaData, "meta-data")
	if err != nil {
		G.logger.Fatal("GenISO", zap.Error(err))
	}
	err = writer.AddFile(userData, "user-data")
	if err != nil {
		G.logger.Fatal("GenISO", zap.Error(err))
	}
	err = writer.AddFile(netData, "network-config")
	if err != nil {
		G.logger.Fatal("GenISO", zap.Error(err))
	}

	isoFile := "/tmp/" + instanceid + ".iso"
	outputFile, err := os.OpenFile(isoFile, os.O_RDWR|os.O_CREATE, 0666)
	if err != nil {
		return "", err
	}
	defer outputFile.Close()

	err = writer.WriteTo(outputFile, "cidata")
	if err != nil {
		return "", err
	}

	writer.Cleanup()
	return outputFile.Name(), nil
}
