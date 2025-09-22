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
	"log"
	"os"
	"strings"
	"time"

	"github.com/beevik/etree"
	"github.com/libvirt/libvirt-go"
	"go.uber.org/zap"
)

func VirtConnect() (err error) {
	if !G.config.VmMode {
		return
	}
	err = libvirt.EventRegisterDefaultImpl()
	if err != nil {
		log.Fatalf("无法注册默认事件循环实现: %v", err)
	}
	go func() {
		for {
			err := libvirt.EventRunDefaultImpl()
			if err != nil {
				log.Fatalf("事件循环运行错误: %v", err)
			}
		}
	}()
	conn, err := libvirt.NewConnect("qemu:///system")
	if err != nil {
		G.logger.Error("Failed to connect to qemu:///system")
		return err
	}
	G.conn = conn

	go func() {
		for {
			ticker := time.NewTicker(5 * time.Second)
			for {
				select {
				case <-G.ctx.Done():
					return
				case <-ticker.C:
					_, err := G.conn.GetLibVersion()
					if err != nil {
						G.logger.Error("Connection to qemu:///system is broken, reconnecting...")
						conn, err := libvirt.NewConnect("qemu:///system")
						if err != nil {
							G.logger.Error("Failed to reconnect to qemu:///system")
							continue
						}
						G.conn = conn
					}
				}
			}
		}
	}()
	return nil
}

func VirtDomains() (domains []*libvirt.Domain, err error) {
	G.logger.Debug("Get domains")
	d, err := G.conn.ListAllDomains(0)
	if err != nil {
		G.logger.Error("Failed to get domains")
		return nil, err
	}
	for _, domain := range d {
		domains = append(domains, &domain)
	}
	return domains, nil
}

func VirtDomainGet(vmUuid string) (domain *libvirt.Domain, err error) {
	G.logger.Debug("Get domain: " + vmUuid)
	domain, err = G.conn.LookupDomainByName(vmUuid)
	if err != nil {
		if libvirtErr, ok := err.(libvirt.Error); ok {
			if libvirtErr.Code == libvirt.ERR_NO_DOMAIN {
				return nil, nil
			}
		}
		G.logger.Error("Failed to get domain: " + vmUuid)
		return nil, err
	}
	return domain, nil
}

func VirtDomainSave(domain *libvirt.Domain, mempath string, defpath string, pcipath string) (err error) {
	G.logger.Debug("Save domain to " + mempath)
	hostdevs, err := DetachAllHostdev(domain)
	if err != nil {
		G.logger.Error("cannot detach hostdevs", zap.Error(err))
		return err
	}
	if hostdevs != nil && pcipath != "" {
		if err = os.WriteFile(pcipath, []byte(strings.Join(hostdevs, "\n")), 0644); err != nil {
			G.logger.Error("cannot write hostdevs", zap.Error(err))
			return err
		}
	}

	if err = domain.Save(mempath); err != nil {
		time.Sleep(5 * time.Second)
		if err = domain.Save(mempath); err != nil {
			G.logger.Error("Failed to save domain to " + mempath)
			return err
		}
	}
	if defpath != "" {
		xmlDesc, err := domain.GetXMLDesc(0)
		if err != nil {
			G.logger.Error("Failed to get domain xml description")
			return err
		}

		err = os.WriteFile(defpath, []byte(xmlDesc), 0644)
		if err != nil {
			G.logger.Error("Failed to save domain xml to " + defpath)
			return err
		}
	}
	return nil
}

func VirtDomainDisks(domain *libvirt.Domain) (disks map[string]map[string]string, err error) {
	G.logger.Debug("Get domain disks")
	xmlDesc, err := domain.GetXMLDesc(0)
	if err != nil {
		G.logger.Error("Failed to get domain XML description")
		return nil, err
	}

	doc := etree.NewDocument()
	if err := doc.ReadFromBytes([]byte(xmlDesc)); err != nil {
		G.logger.Error("Failed to parse domain XML description")
		return nil, err
	}
	disks = make(map[string]map[string]string)

	for _, disk := range doc.FindElements("//disk") {
		device := disk.SelectAttrValue("device", "")
		target := disk.SelectElement("target").SelectAttrValue("dev", "")
		source := ""
		sourceElem := disk.SelectElement("source")
		if sourceElem != nil {
			source = sourceElem.SelectAttrValue("file", "")
			if source == "" {
				source = sourceElem.SelectAttrValue("dev", "")
			}
		}
		if device == "" || target == "" || source == "" {
			continue
		}
		disks[target] = map[string]string{"device": device, "source": source}
	}
	return disks, nil
}

func VirtDomainDefine(domainDefine string) (domain *libvirt.Domain, err error) {
	G.logger.Debug("Define domain")
	domain, err = G.conn.DomainDefineXML(domainDefine)
	if err != nil {
		G.logger.Error("Failed to define domain")
		return nil, err
	}
	return domain, nil
}

func VirtDomainDestroy(domain *libvirt.Domain) (err error) {
	G.logger.Debug("Destroy domain")
	err = domain.Destroy()
	if err != nil {
		G.logger.Error("Failed to destroy domain")
		return err
	}
	return nil
}

func VirtDomainState(domain *libvirt.Domain) (state libvirt.DomainState, err error) {
	G.logger.Debug("Get domain state")
	state, _, err = domain.GetState()
	if err != nil {
		G.logger.Error("Failed to get domain state")
		return 0, err
	}
	return state, nil
}

func VirtDomainRestore(mempath string, defpath string) (err error) {
	G.logger.Debug("Restore domain from " + mempath)
	if defpath == "" {
		err = G.conn.DomainRestore(mempath)
	} else {
		content, err := os.ReadFile(defpath)
		if err != nil {
			G.logger.Error("Failed to read domain xml file")
			return err
		}
		err = G.conn.DomainRestoreFlags(mempath, string(content), 0)
		if err != nil {
			G.logger.Error("Failed to restore domain from " + mempath)
			return err
		}
	}

	if err != nil {
		G.logger.Error("Failed to restore domain from " + mempath)
		return err
	}
	return nil
}

func VirtDomainPersistent(domain *libvirt.Domain) (err error) {
	G.logger.Debug("Make domain persistent")
	persist, err := domain.IsPersistent()
	if err != nil {
		G.logger.Error("Failed to check domain persistent")
		return err
	}
	if persist {
		return nil
	}
	xmlDesc, err := domain.GetXMLDesc(0)
	if err != nil {
		G.logger.Error("Failed to get domain xml description")
	}
	_, err = VirtDomainDefine(xmlDesc)
	if err != nil {
		return err
	}
	return nil
}

func VirtDomainRedefine(vmUuid string, defpath string) (err error) {
	G.logger.Debug("Redefine domain")
	content, err := os.ReadFile(defpath)
	if err != nil {
		G.logger.Error("Failed to read domain xml file")
		return err
	}
	domain, err := VirtDomainGet(vmUuid)
	if err != nil {
		return err
	}
	err = domain.Undefine()
	if err != nil {
		G.logger.Error("Failed to undefine domain")
		return err
	}
	_, err = VirtDomainDefine(string(content))
	if err != nil {
		return err
	}
	return nil
}

func VirtDomainUndefine(domain *libvirt.Domain) (err error) {
	G.logger.Debug("Undefine domain")
	err = domain.Undefine()
	if err != nil {
		G.logger.Error("Failed to undefine domain")
		return err
	}
	return nil
}

func VirtDomainPoweron(domain *libvirt.Domain) (err error) {
	G.logger.Debug("Power on domain")
	state, err := VirtDomainState(domain)
	if err != nil {
		return err
	}
	if state == libvirt.DOMAIN_RUNNING {
		return nil
	}
	if state == libvirt.DOMAIN_SHUTOFF {
		err = domain.Create()
		if err != nil {
			G.logger.Error("Failed to power on domain")
			return err
		}
	}
	if state == libvirt.DOMAIN_PAUSED {
		err = domain.Resume()
		if err != nil {
			G.logger.Error("Failed to resume domain")
			return err
		}
	}
	return nil
}

func VirtDomainPoweroff(domain *libvirt.Domain, byforce bool, timeout int) (err error) {
	G.logger.Debug("Power off domain")
	state, err := VirtDomainState(domain)
	if err != nil {
		return err
	}
	if state == libvirt.DOMAIN_SHUTOFF {
		return nil
	}
	if byforce {
		err = domain.Destroy()
		if err != nil {
			G.logger.Error("Failed to power off domain")
			return err
		}
		return nil
	}

	err = domain.Shutdown()
	if err != nil {
		return err
	}
	for i := 0; i < timeout; i++ {
		state, err = VirtDomainState(domain)
		if err != nil {
			return err
		}
		if state == libvirt.DOMAIN_SHUTOFF {
			return nil
		}
		time.Sleep(time.Second)
	}
	err = domain.Destroy()
	if err != nil {
		G.logger.Error("Failed to power off domain")
		return err
	}
	return nil
}

func VirtDomainAttachCdrom(domain *libvirt.Domain, isoFile string) (err error) {
	G.logger.Debug("Attach cdrom")
	state, err := VirtDomainState(domain)
	if err != nil {
		return err
	}
	flag := libvirt.DOMAIN_DEVICE_MODIFY_CONFIG
	if state == libvirt.DOMAIN_RUNNING || state == libvirt.DOMAIN_PAUSED {
		flag |= libvirt.DOMAIN_DEVICE_MODIFY_LIVE
	}

	diskXML := "" +
		"<disk type='file' device='cdrom'>\n" +
		"  <driver name='qemu' type='raw'/>\n" +
		"  <source file='" + isoFile + "'/>\n" +
		"  <target dev='hda' bus='ide'/>\n" +
		"  <readonly/>\n" +
		"</disk>"
	err = domain.UpdateDeviceFlags(diskXML, flag)
	if err != nil {
		G.logger.Error("Failed to attach cdrom")
		return err
	}
	return nil
}

func VirtDomainAttachNbd(domain *libvirt.Domain, nbddev string) (err error) {
	G.logger.Debug("Attach nbd")
	state, err := VirtDomainState(domain)
	if err != nil {
		return err
	}
	flag := libvirt.DOMAIN_DEVICE_MODIFY_CONFIG
	if state == libvirt.DOMAIN_RUNNING || state == libvirt.DOMAIN_PAUSED {
		flag |= libvirt.DOMAIN_DEVICE_MODIFY_LIVE
	}
	if !strings.HasPrefix(nbddev, "/dev/") {
		nbddev = "/dev/" + nbddev
	}

	diskXML := "" +
		"<disk type='block' device='cdrom'>\n" +
		"  <driver name='qemu' type='raw'/>\n" +
		"  <source dev='" + nbddev + "'/>\n" +
		"  <target dev='hda' bus='ide'/>\n" +
		"  <readonly/>\n" +
		"</disk>"
	err = domain.UpdateDeviceFlags(diskXML, flag)
	if err != nil {
		G.logger.Error("Failed to attach nbd:" + diskXML)
		return err
	}
	return nil
}

func VirtDomainAttachDisk(domain *libvirt.Domain, diskFile string) (err error) {
	G.logger.Debug("Attach disk")
	disks, err := VirtDomainDisks(domain)
	if err != nil {
		return err
	}
	for _, disk := range disks {
		if disk["source"] == diskFile {
			return nil
		}
	}
	i := len(disks)
	if i >= 5 {
		return fmt.Errorf("the number of disks has reached the maximum limit of 4")
	}
	state, err := VirtDomainState(domain)
	if err != nil {
		return err
	}
	flag := libvirt.DOMAIN_DEVICE_MODIFY_CONFIG
	if state == libvirt.DOMAIN_RUNNING || state == libvirt.DOMAIN_PAUSED {
		flag |= libvirt.DOMAIN_DEVICE_MODIFY_LIVE
	}
	devs := []string{"sda", "sdb", "sdc", "sdd", "sde"}
	devname := devs[i]

	diskXML := "" +
		"<disk type='file' device='disk'>\n" +
		"  <driver name='qemu' type='qcow2'/>\n" +
		"  <source file='" + diskFile + "'/>\n" +
		"  <target dev='" + devname + "' bus='virtio'/>\n" +
		"</disk>"

	err = domain.AttachDeviceFlags(diskXML, flag)
	if err != nil {
		G.logger.Error("Failed to attach disk")
		return err
	}
	return nil
}

func VirtDomainDetachDisk(domain *libvirt.Domain, devname string) (err error) {
	G.logger.Debug("Detach disk")
	state, err := VirtDomainState(domain)
	if err != nil {
		return err
	}
	flag := libvirt.DOMAIN_DEVICE_MODIFY_CONFIG
	if state == libvirt.DOMAIN_RUNNING || state == libvirt.DOMAIN_PAUSED {
		flag |= libvirt.DOMAIN_DEVICE_MODIFY_LIVE
	}

	diskXML := "" +
		"<disk type='file' device='disk'>\n" +
		"  <driver name='qemu' type='qcow2'/>\n" +
		"  <source file=''/>\n" +
		"  <target dev='" + devname + "' bus='virtio'/>\n" +
		"</disk>"

	err = domain.DetachDeviceFlags(diskXML, flag)
	if err != nil {
		err := err.(libvirt.Error)
		if err.Code != libvirt.ERR_DEVICE_MISSING {
			G.logger.Error("Failed to detach disk")
			return err
		}
	}
	return nil
}

func VirtDomainAttachHostdev(domain *libvirt.Domain, pciAddr string) (err error) {
	G.logger.Debug("Attach hostdev")
	state, err := VirtDomainState(domain)
	if err != nil {
		return err
	}
	flag := libvirt.DOMAIN_DEVICE_MODIFY_CONFIG
	if state == libvirt.DOMAIN_RUNNING || state == libvirt.DOMAIN_PAUSED {
		flag |= libvirt.DOMAIN_DEVICE_MODIFY_LIVE
	}

	pciAddr = strings.Replace(pciAddr, ".", ":", -1)
	pciParts := strings.Split(pciAddr, ":")
	hostdevXML := "" +
		"<hostdev mode='subsystem' type='pci' managed='yes'>\n" +
		"<source>\n" +
		"    <address domain='0x" + pciParts[0] + "' bus='0x" + pciParts[1] + "' slot='0x" + pciParts[2] + "' function='0x" + pciParts[3] + "'/>\n" +
		"</source>\n" +
		"</hostdev> \n"

	err = domain.AttachDeviceFlags(hostdevXML, flag)
	if err != nil {
		G.logger.Error("Failed to attach hostdev")
		return err
	}
	return nil
}

func VirtDomainDetachHostdev(domain *libvirt.Domain, pciAddr string) (err error) {
	G.logger.Debug("Detach hostdev")
	state, err := VirtDomainState(domain)
	if err != nil {
		return err
	}
	flag := libvirt.DOMAIN_DEVICE_MODIFY_CONFIG
	if state == libvirt.DOMAIN_RUNNING || state == libvirt.DOMAIN_PAUSED {
		flag |= libvirt.DOMAIN_DEVICE_MODIFY_LIVE
	}

	pciAddr = strings.Replace(pciAddr, ".", ":", -1)
	pciParts := strings.Split(pciAddr, ":")
	hostdevXML := "" +
		"<hostdev mode='subsystem' type='pci' managed='yes'>\n" +
		"<source>\n" +
		"    <address domain='0x" + pciParts[0] + "' bus='0x" + pciParts[1] + "' slot='0x" + pciParts[2] + "' function='0x" + pciParts[3] + "'/>\n" +
		"</source>\n" +
		"</hostdev> \n"

	err = domain.DetachDeviceFlags(hostdevXML, flag)
	if err != nil {
		err := err.(libvirt.Error)
		if err.Code != libvirt.ERR_DEVICE_MISSING {
			G.logger.Error("Failed to detach hostdev")
			return err
		}
	}
	return nil
}

func VirtDomainDetachCdrom(domain *libvirt.Domain) (err error) {
	G.logger.Debug("Detach cdrom")
	state, err := VirtDomainState(domain)
	if err != nil {
		return err
	}
	flag := libvirt.DOMAIN_DEVICE_MODIFY_CONFIG
	if state == libvirt.DOMAIN_RUNNING || state == libvirt.DOMAIN_PAUSED {
		flag |= libvirt.DOMAIN_DEVICE_MODIFY_LIVE
	}

	diskXML := "" +
		"<disk type='file' device='cdrom'>\n" +
		"  <driver name='qemu' type='raw'/>\n" +
		"  <source file=''/>\n" +
		"  <target dev='hda' bus='ide'/>\n" +
		"  <readonly/>\n" +
		"</disk>"

	err = domain.UpdateDeviceFlags(diskXML, flag)
	if err != nil {
		G.logger.Error("Failed to detach cdrom")
		return err
	}
	return nil
}

func VirtDomainDetachNbd(domain *libvirt.Domain) (err error) {
	G.logger.Debug("Detach nbd")
	state, err := VirtDomainState(domain)
	if err != nil {
		return err
	}
	flag := libvirt.DOMAIN_DEVICE_MODIFY_CONFIG
	if state == libvirt.DOMAIN_RUNNING || state == libvirt.DOMAIN_PAUSED {
		flag |= libvirt.DOMAIN_DEVICE_MODIFY_LIVE
	}

	diskXML := "" +
		"<disk type='file' device='cdrom'>\n" +
		"  <driver name='qemu' type='raw'/>\n" +
		"  <source file=''/>\n" +
		"  <target dev='hda' bus='ide'/>\n" +
		"  <readonly/>\n" +
		"</disk>"

	err = domain.UpdateDeviceFlags(diskXML, flag)
	if err != nil {
		G.logger.Error("Failed to detach nbd:"+diskXML, zap.Error(err))
		return err
	}
	return nil
}

func VirtDomainModify(domain *libvirt.Domain, vcpu uint32, mem uint32, bootdev string) (err error) {
	vcpuUint := uint(vcpu)
	memUint := uint64(mem) * 1024 * 1024
	xmlDesc, err := domain.GetXMLDesc(0)
	if err != nil {
		G.logger.Error("Failed to get domain xml description")
		return err
	}
	doc := etree.NewDocument()
	if err := doc.ReadFromBytes([]byte(xmlDesc)); err != nil {
		G.logger.Error("Failed to parse domain XML description")
		return err
	}
	if vcpuUint > 0 {
		vcpuElem := doc.FindElement("/domain/vcpu")
		if vcpuElem == nil {
			return fmt.Errorf("failed to find vcpu element")
		}
		vcpuElem.SetText(fmt.Sprintf("%d", vcpuUint))
	}
	if memUint > 0 {
		memElem := doc.FindElement("/domain/memory")
		if memElem == nil {
			return fmt.Errorf("failed to find memory element")
		}
		memElem.SetText(fmt.Sprintf("%d", memUint))
		currentMemElem := doc.FindElement("/domain/currentMemory")
		if currentMemElem == nil {
			return fmt.Errorf("failed to find currentMemory element")
		}
		currentMemElem.SetText(fmt.Sprintf("%d", memUint))
	}
	if bootdev != "" {
		bootElem := doc.FindElement("/domain/os/boot")
		if bootElem == nil {
			return fmt.Errorf("failed to find boot element")
		}
		bootElem.SelectAttr("dev").Value = bootdev
	}
	newXmlDesc, err := doc.WriteToString()
	if err != nil {
		G.logger.Error("Failed to write domain XML description")
		return err
	}
	_, err = VirtDomainDefine(newXmlDesc)
	if err != nil {
		return err
	}
	return nil
}

func VirtDomainHostdevs(domain *libvirt.Domain) (hostdevs []string, err error) {
	G.logger.Debug("Get domain hostdevs")
	xmlDesc, err := domain.GetXMLDesc(0)
	if err != nil {
		G.logger.Error("Failed to get domain XML description")
		return nil, err
	}

	doc := etree.NewDocument()
	if err := doc.ReadFromBytes([]byte(xmlDesc)); err != nil {
		G.logger.Error("Failed to parse domain XML description")
		return nil, err
	}

	for _, hostdev := range doc.FindElements("//hostdev") {
		source := hostdev.SelectElement("source").SelectElement("address")
		domain := source.SelectAttrValue("domain", "")[2:]
		bus := source.SelectAttrValue("bus", "")[2:]
		slot := source.SelectAttrValue("slot", "")[2:]
		function := source.SelectAttrValue("function", "")[2:]
		hostdevs = append(hostdevs, domain+":"+bus+":"+slot+"."+function)
	}
	return hostdevs, nil
}
