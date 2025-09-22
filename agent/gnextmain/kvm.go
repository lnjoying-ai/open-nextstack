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
	"fmt"
	"os"
	"regexp"
	"strings"
	"text/template"
	"time"

	"github.com/libvirt/libvirt-go"
	"go.uber.org/zap"
)

func CreateRootDiskXml(vol *Vol, pool *Pool) string {
	if pool.Stype != "fs" {
		G.logger.Fatal("createrootdiskxml", zap.Error(fmt.Errorf("pool type is not fs")))
	}
	if !vol.Root {
		G.logger.Fatal("createrootdiskxml", zap.Error(fmt.Errorf("volume is not root")))
	}
	var diskPath string
	diskName := vol.Uuid + ".qcow2"
	diskPath = pool.ParaMap["DIR"] + "/volumes/" + diskName
	diskTempl := "<disk type='file' device='disk'>\n" +
		"  <driver name='qemu' type='qcow2'/>\n" +
		"  <source file='{{.DISK}}'/>\n" +
		"  <target dev='{{.DISK_DEV}}' bus='scsi'/>\n" +
		"</disk>\n"

	tmpl, err := template.New("DISKXML").Parse(diskTempl)
	if err != nil {
		G.logger.Fatal("createrootdiskxml", zap.Error(err))
	}

	data := map[string]string{
		"DISK":     diskPath,
		"DISK_DEV": "sda",
	}
	var resultBuffer bytes.Buffer

	if err = tmpl.Execute(&resultBuffer, data); err != nil {
		G.logger.Fatal("createrootdiskxml", zap.Error(err))
	}
	return resultBuffer.String()
}

func CreateDataDisksXml(vols []*Vol, pool *Pool) string {
	if pool.Stype != "fs" {
		G.logger.Fatal("CreateDataDisksXml", zap.Error(fmt.Errorf("pool type is not fs")))
	}
	devs := []string{"sdb", "sdc", "sdd", "sde"}
	diskTempl := "" +
		"<disk type='file' device='disk'>\n" +
		"  <driver name='qemu' type='qcow2'/>\n" +
		"  <source file='{{.DISK}}'/>\n" +
		"  <target dev='{{.DISK_DEV}}' bus='scsi'/>\n" +
		"</disk>\n"

	tmpl, err := template.New("DISKXML").Parse(diskTempl)
	if err != nil {
		G.logger.Fatal("CreateDataDisksXml", zap.Error(err))
	}
	disks := []string{}
	for i, vol := range vols {
		if i > 3 {
			G.logger.Fatal("CreateDataDisksXml", zap.Error(fmt.Errorf("too many data volumes")))
		}
		var diskPath string
		diskName := vol.Uuid + ".qcow2"
		diskPath = pool.ParaMap["DIR"] + "/volumes/" + diskName

		data := map[string]string{
			"DISK":     diskPath,
			"DISK_DEV": devs[i],
		}
		var resultBuffer bytes.Buffer

		if err = tmpl.Execute(&resultBuffer, data); err != nil {
			G.logger.Fatal("CreateDataDisksXml", zap.Error(err))
		}
		disks = append(disks, resultBuffer.String())
	}
	return strings.Join(disks, "\n")
}

func CreateDataDisXmlFile(source string, dev string) (string, error) {
	diskTempl := "" +
		"<disk type='file' device='disk'>\n" +
		"  <driver name='qemu' type='qcow2'/>\n" +
		"  <source file='{{.DISK}}'/>\n" +
		"  <target dev='{{.DISK_DEV}}' bus='scsi'/>\n" +
		"</disk>\n"

	tmpl, err := template.New("DISKXML").Parse(diskTempl)
	if err != nil {
		G.logger.Fatal("CreateDataDisXmlFile", zap.Error(err))
	}

	data := map[string]string{
		"DISK":     source,
		"DISK_DEV": dev,
	}
	var resultBuffer bytes.Buffer

	if err = tmpl.Execute(&resultBuffer, data); err != nil {
		G.logger.Fatal("CreateDataDisXmlFile", zap.Error(err))
	}
	return SaveTmpXml(resultBuffer.String())
}

func CreateNicsXml(ports []*Port) string {
	nicTempl := "" +
		"<interface type='ethernet'>\n" +
		"  <mac address='{{.MAC}}'/>\n" +
		"  <target dev='{{.NIC_NAME}}' managed='no'/>\n" +
		"  <model type='{{.NIC_TYPE}}'/>\n" +
		"</interface>\n"

	tmpl, err := template.New("NICXML").Parse(nicTempl)
	if err != nil {
		G.logger.Fatal("CreateNicsXml", zap.Error(err))
	}
	nicsXml := []string{}
	for _, port := range ports {

		data := map[string]string{
			"MAC":      port.Mac,
			"NIC_NAME": port.Nic,
			"NIC_TYPE": "virtio",
		}
		var resultBuffer bytes.Buffer

		if err = tmpl.Execute(&resultBuffer, data); err != nil {
			G.logger.Fatal("CreateNicsXml", zap.Error(err))
		}
		nicsXml = append(nicsXml, resultBuffer.String())
	}
	return strings.Join(nicsXml, "\n")
}

func CreateGpusXml(gpus []*GPU) string {
	hostdevTempl := "" +
		"<hostdev mode='subsystem' type='pci' managed='yes'>\n" +
		"  <source>\n" +
		"    <address domain='{{.PCI_DOMAIN}}' bus='{{.PCI_BUS}}' slot='{{.PCI_SLOT}}' function='{{.PCI_FUNC}}'/>\n" +
		"  </source>\n" +
		"</hostdev>"

	tmpl, err := template.New("HOSTDEV").Parse(hostdevTempl)
	if err != nil {
		G.logger.Fatal("CreateHostdevTempl", zap.Error(err))
	}
	gpusXml := []string{}
	pcis := []string{}
	for _, gpu := range gpus {
		pcis = append(pcis, gpu.DevicePci)
		if gpu.IBPci != "" && !Contains(pcis, gpu.IBPci) {
			pcis = append(pcis, gpu.IBPci)
		}
		if gpu.OtherPcis != nil {
			for _, pci := range gpu.OtherPcis {
				if !Contains(pcis, pci) {
					pcis = append(pcis, pci)
				}
			}
		}
	}
	for _, pci := range pcis {
		parts := strings.Split(pci, ":")
		if len(parts) != 3 {
			G.logger.Fatal("CreateGpusXml", zap.Error(fmt.Errorf("pci address is invalid")))
		}
		domain := "0x" + parts[0]
		bus := "0x" + parts[1]
		_parts := strings.Split(parts[2], ".")
		if len(_parts) != 2 {
			G.logger.Fatal("CreateGpusXml", zap.Error(fmt.Errorf("pci address is invalid")))
		}
		slot := "0x" + _parts[0]
		_func := "0x" + _parts[1]

		data := map[string]string{
			"PCI_DOMAIN": domain,
			"PCI_BUS":    bus,
			"PCI_SLOT":   slot,
			"PCI_FUNC":   _func,
		}
		var resultBuffer bytes.Buffer

		if err = tmpl.Execute(&resultBuffer, data); err != nil {
			G.logger.Fatal("CreateGpusXml", zap.Error(err))
		}
		gpusXml = append(gpusXml, resultBuffer.String())
	}
	return strings.Join(gpusXml, "\n")
}

func ExistVM(vmUuid string) bool {
	domain, _ := VirtDomainGet(vmUuid)
	return domain != nil
}

func CreateVM(vm *VM, ports []*Port, vols []*Vol, pool *Pool, gpus []*GPU, vncPort string) (err error) {
	var rootDiskXml, dataDisksXml, nicsXml, gpusXml string
	rootDiskXml = CreateRootDiskXml(vols[0], pool)
	dataDisksXml = CreateDataDisksXml(vols[1:], pool)
	nicsXml = CreateNicsXml(ports)
	gpusXml = CreateGpusXml(gpus)
	vmTempl := "" +
		"<domain type='kvm'>\n" +
		"  <name>{{.ID}}</name>\n" +
		"  <memory unit='KiB'>{{.RAM}}</memory>\n" +
		"  <vcpu placement='static'>{{.VCPU}}</vcpu>\n" +
		"  <cpu mode='host-passthrough' check='none'>\n" +
		"    <feature policy='disable' name='svm'/>\n" +
		"    <feature policy='disable' name='vmx'/>\n" +
		"    <feature policy='disable' name='hypervisor'/>\n" +
		"  </cpu>\n" +
		"  <clock offset='utc'>\n" +
		"    <timer name='rtc' tickpolicy='catchup'/>\n" +
		"    <timer name='pit' tickpolicy='delay'/>\n" +
		"    <timer name='hpet' present='no'/>\n" +
		"  </clock>\n" +
		"  <on_poweroff>destroy</on_poweroff>\n" +
		"  <on_reboot>restart</on_reboot>\n" +
		"  <on_crash>destroy</on_crash>\n" +
		"  <pm>\n" +
		"    <suspend-to-mem enabled='no'/>\n" +
		"    <suspend-to-disk enabled='no'/>\n" +
		"  </pm>\n" +
		"  <os>\n" +
		"    <type arch='x86_64' machine='pc'>hvm</type>\n" +
		"    <boot dev='hd'/>\n" +
		"  </os>\n" +
		"  <features>\n" +
		"    <acpi/>\n" +
		"    <apic/>\n" +
		"  </features>\n" +
		"  <devices>\n" +
		"    <video>\n" +
		"  	   <model type='qxl' ram='65536' vram='65536' vgamem='16384' heads='1' primary='yes'/>\n" +
		"  	   <address type='pci' domain='0x0000' bus='0x00' slot='0x02' function='0x0'/>\n" +
		"    </video>\n" +
		"    <input type='tablet' bus='usb'/>\n" +
		"    {{.ROOT_DISK}}\n" +
		"    {{.DATA_DISKS}}\n" +
		"    <disk type='file' device='cdrom'>\n" +
		"  	   <driver name='qemu' type='raw'/>\n" +
		"  	   <backingStore/>\n" +
		"  	   <target dev='hda' bus='ide' tray='open'/>\n" +
		"  	   <readonly/>\n" +
		"    </disk>\n" +
		"    <controller type='scsi' index='0' model='virtio-scsi'/>\n" +
		"    {{.INTERFACES}}\n" +
		"    <graphics type='vnc' port='{{.VNC_PORT}}' autoport='no' listen='127.0.0.1'>\n" +
		"  	   <listen type='address' address='127.0.0.1'/>\n" +
		"    </graphics>\n" +
		"    {{.HOSTDEVS}}\n" +
		"    <memballoon model='virtio'>\n" +
		"  	   <stats period='10'/>\n" +
		"    </memballoon>\n" +
		"    <channel type='unix'>\n" +
		"  	   <source mode='bind' path='/var/lib/libvirt/qemu/{{.ID}}'/>\n" +
		"  	   <target type='virtio' name='org.qemu.guest_agent.0'/>\n" +
		"    </channel>\n" +
		"  </devices>\n" +
		"</domain>"

	tmpl, err := template.New("DISKXML").Parse(vmTempl)
	if err != nil {
		G.logger.Fatal("CreateVM", zap.Error(err))
	}

	data := map[string]string{
		"ID":         vm.Uuid,
		"VCPU":       fmt.Sprintf("%d", vm.Vcpu),
		"RAM":        fmt.Sprintf("%d", vm.Mem*1024*1024),
		"VNC_PORT":   vm.Vnc,
		"INTERFACES": nicsXml,
		"ROOT_DISK":  rootDiskXml,
		"DATA_DISKS": dataDisksXml,
		"HOSTDEVS":   gpusXml,
		"CDROM_DEV":  "hda",
	}
	var resultBuffer bytes.Buffer

	if err = tmpl.Execute(&resultBuffer, data); err != nil {
		G.logger.Fatal("CreateVM", zap.Error(err))
	}
	vmXml := resultBuffer.String()
	vmXml = FixHugepageFromString(vmXml)
	_, err = VirtDomainDefine(vmXml)
	return err
}

func ResumeVM(vmUuid string, mempath string, defpath string, gpus []*GPU) (err error) {
	if err = VirtDomainRestore(mempath, defpath); err != nil {
		return err
	}
	domain, err := VirtDomainGet(vmUuid)
	if err != nil {
		return err
	}
	if domain == nil {
		return fmt.Errorf("domain not found")
	}
	if err = VirtDomainPersistent(domain); err != nil {
		return err
	}
	i := 120
	for i > 0 {
		state, err := VirtDomainState(domain)
		if err != nil {
			return err
		}
		if state == libvirt.DOMAIN_RUNNING {
			os.Remove(mempath)
			if defpath != "" {
				os.Remove(defpath)
			}
			for _, gpu := range gpus {
				if err = _attachGPU(domain, gpu); err != nil {
					return err
				}
			}
			return nil
		}
		time.Sleep(1 * time.Second)
		i--
	}
	return fmt.Errorf("vm state is invalid")
}

func SaveTmpXml(content string) (fileName string, err error) {
	var f *os.File
	if f, err = os.CreateTemp("", "*.xml"); err != nil {
		return "", err
	}
	defer f.Close()
	if _, err = f.WriteString(content); err != nil {
		return "", err
	}
	return f.Name(), nil
}

func FixHugepageFromString(content string) string {

	hugepages := osHugepageGet()
	if hugepages == 0 {

		re := regexp.MustCompile(`(?s)<memoryBacking>[\s\S]*?</memoryBacking>`)
		return re.ReplaceAllString(content, "")
	}

	insertContent := "<memoryBacking><hugepages/></memoryBacking>"
	re := regexp.MustCompile(`(</memory>)`)
	return re.ReplaceAllString(string(content), "${1}"+insertContent)
}

func FixHugepage(defpath string) (err error) {
	var content []byte
	if content, err = os.ReadFile(defpath); err != nil {
		return err
	}
	return os.WriteFile(defpath, []byte(FixHugepageFromString(string(content))), 0644)
}

func FixVnc(defpath string, vncPort string) (err error) {
	var content []byte
	if content, err = os.ReadFile(defpath); err != nil {
		return err
	}
	re := regexp.MustCompile(`type='vnc' port='\d+'`)
	newContent := re.ReplaceAllString(string(content), "type='vnc' port='"+vncPort+"'")
	return os.WriteFile(defpath, []byte(newContent), 0644)
}
func FixCdrom(defpath string) (err error) {
	var content []byte
	if content, err = os.ReadFile(defpath); err != nil {
		return err
	}

	disk := `
	<disk type='file' device='cdrom'>
	  <driver name='qemu' type='raw'/>
	  <target dev='hda' bus='ide'/>
	  <readonly/>
	</disk>
	`
	re := regexp.MustCompile(`(?s)<disk type='file' device='cdrom'>.*?</disk>`)
	newContent := re.ReplaceAllString(string(content), disk)
	return os.WriteFile(defpath, []byte(newContent), 0644)
}

func FixVnet(defpath string, nic string, mac string) (err error) {
	var content []byte
	if content, err = os.ReadFile(defpath); err != nil {
		return err
	}
	re := regexp.MustCompile(`(?s)<mac address='` + mac + `'/>\s*<target dev='\S+?'`)
	newContent := re.ReplaceAllString(string(content), "<mac address='"+mac+"'/>\n  <target dev='"+nic+"'")
	return os.WriteFile(defpath, []byte(newContent), 0644)
}

func VMBlks(vmUuid string) (map[string]map[string]string, error) {
	domain, err := VirtDomainGet(vmUuid)
	if err != nil {
		return nil, err
	}
	if domain == nil {
		return nil, fmt.Errorf("domain not found")
	}
	return VirtDomainDisks(domain)
}

func VmState(vmUuid string) (string, error) {
	domain, err := VirtDomainGet(vmUuid)
	if err != nil {
		return "", err
	}
	if domain == nil {
		return "", fmt.Errorf("domain not found")
	}
	state, err := VirtDomainState(domain)
	if err != nil {
		return "", err
	}
	switch state {
	case libvirt.DOMAIN_NOSTATE:
		return "unknown", nil
	case libvirt.DOMAIN_RUNNING:
		return "running", nil
	case libvirt.DOMAIN_BLOCKED:
		return "blocked", nil
	case libvirt.DOMAIN_PAUSED:
		return "paused", nil
	case libvirt.DOMAIN_SHUTDOWN:
		return "shut off", nil
	case libvirt.DOMAIN_SHUTOFF:
		return "shut off", nil
	case libvirt.DOMAIN_CRASHED:
		return "crashed", nil
	case libvirt.DOMAIN_PMSUSPENDED:
		return "suspended", nil
	default:
		return "unknown", nil
	}
}

func DelVM(vmUuid string) error {
	domain, err := VirtDomainGet(vmUuid)
	if err != nil {
		return err
	}
	if domain == nil {
		return nil
	}
	state, err := VirtDomainState(domain)
	if err != nil {
		return err
	}
	if state == libvirt.DOMAIN_RUNNING {
		if err := VirtDomainDestroy(domain); err != nil {
			return err
		}
	}
	return VirtDomainUndefine(domain)
}

func ResizeVol(vm *VM, pool *Pool, vol *Vol, size uint32) (err error) {
	if pool.Stype != "fs" {
		return fmt.Errorf("pool type is not fs")
	}
	if vol.PoolUuid != pool.Uuid {
		return fmt.Errorf("pool not match")
	}
	diskPath := pool.ParaMap["DIR"] + "/volumes/" + vol.Uuid + ".qcow2"
	if ok, _ := FileExists(diskPath); !ok {
		return fmt.Errorf("disk file does not exist")
	}
	if vm != nil {
		if err = VmPreSnap(vm.Uuid, pool.ParaMap["DIR"], ""); err != nil {
			return err
		}
	}
	cmd := fmt.Sprintf("qemu-img resize %s +%dG", diskPath, size)
	if err = ExecuteCmdRun(G.config.CmdTimeout*100, cmd); err != nil {
		G.logger.Error("ExportVol", zap.Error(err))
		return err
	}
	if vm != nil {
		if err = VmPostSnap(vm.Uuid, pool.ParaMap["DIR"], ""); err != nil {
			return err
		}
	}
	return nil
}

func ExportVol(vm *VM, pool *Pool, vol *Vol, outputFile string) (err error) {
	if pool.Stype != "fs" {
		return fmt.Errorf("pool type is not fs")
	}
	if vol.PoolUuid != pool.Uuid {
		return fmt.Errorf("pool not match")
	}
	diskPath := pool.ParaMap["DIR"] + "/volumes/" + vol.Uuid + ".qcow2"
	if ok, _ := FileExists(diskPath); !ok {
		return fmt.Errorf("disk file does not exist")
	}
	backingDir := pool.ParaMap["DIR"] + "/backing"
	if err := osMkdir(backingDir); err != nil {
		return err
	}
	outputFile = strings.ToLower(outputFile)
	if !strings.HasSuffix(outputFile, ".qcow2") {
		outputFile += ".qcow2"
	}
	fileName := pool.ParaMap["DIR"] + "/backing/" + outputFile
	if ok, _ := FileExists(fileName); ok {
		return fmt.Errorf("output file %s already exists", fileName)
	}
	if vm != nil {
		if err = VmPreSnap(vm.Uuid, pool.ParaMap["DIR"], ""); err != nil {
			return err
		}
	}
	cmd := fmt.Sprintf("qemu-img convert -O qcow2 -c %s %s", diskPath, fileName)
	if err = ExecuteCmdRun(G.config.CmdTimeout*100, cmd); err != nil {
		G.logger.Error("ExportVol", zap.Error(err))
		return err
	}
	if vm != nil {
		if err = VmPostSnap(vm.Uuid, pool.ParaMap["DIR"], ""); err != nil {
			return err
		}
	}
	return nil
}

func DetachVol(vm *VM, pool *Pool, vol *Vol) error {
	if pool.Stype != "fs" {
		return fmt.Errorf("pool type is not fs")
	}
	if vm.PoolUuid != pool.Uuid {
		return fmt.Errorf("pool not match")
	}
	if vol.PoolUuid != pool.Uuid {
		return fmt.Errorf("pool not match")
	}
	diskPath := pool.ParaMap["DIR"] + "/volumes/" + vol.Uuid + ".qcow2"
	if ok, _ := FileExists(diskPath); !ok {
		return fmt.Errorf("disk file does not exist")
	}
	domain, err := VirtDomainGet(vm.Uuid)
	if err != nil {
		return err
	}
	if domain == nil {
		return fmt.Errorf("domain not found")
	}
	disks, err := VirtDomainDisks(domain)
	if err != nil {
		return err
	}
	for target, blk := range disks {
		if blk["source"] == diskPath {
			return VirtDomainDetachDisk(domain, target)
		}
	}
	return fmt.Errorf("disk not found")
}

func AttachVol(vm *VM, pool *Pool, vol *Vol) error {
	if pool.Stype != "fs" {
		return fmt.Errorf("pool type is not fs")
	}
	if vm.PoolUuid != pool.Uuid {
		return fmt.Errorf("pool not match")
	}
	if vol.PoolUuid != pool.Uuid {
		return fmt.Errorf("pool not match")
	}
	diskPath := pool.ParaMap["DIR"] + "/volumes/" + vol.Uuid + ".qcow2"
	if ok, _ := FileExists(diskPath); !ok {
		return fmt.Errorf("disk file does not exist")
	}
	domain, err := VirtDomainGet(vm.Uuid)
	if err != nil {
		return err
	}
	if domain == nil {
		return fmt.Errorf("domain not found")
	}
	return VirtDomainAttachDisk(domain, diskPath)
}

func PoweronVM(vmUuid string) error {
	domain, err := VirtDomainGet(vmUuid)
	if err != nil {
		return err
	}
	if domain == nil {
		return fmt.Errorf("domain not found")
	}
	return VirtDomainPoweron(domain)
}

func PoweroffVM(vmUuid string, force bool) error {
	domain, err := VirtDomainGet(vmUuid)
	if err != nil {
		return err
	}
	if domain == nil {
		return fmt.Errorf("domain not found")
	}
	return VirtDomainPoweroff(domain, force, 120)
}

func InjectVM(vmUuid string, isoFile string) (err error) {
	domain, err := VirtDomainGet(vmUuid)
	if err != nil {
		return err
	}
	if domain == nil {
		return fmt.Errorf("domain not found")
	}
	disks, err := VirtDomainDisks(domain)
	if err != nil {
		return err
	}
	if err = VirtDomainPoweroff(domain, true, 0); err != nil {
		return err
	}
	for _, blk := range disks {
		if blk["device"] == "cdrom" && strings.HasSuffix(blk["source"], ".iso") {
			if ok, _ := FileExists(blk["source"]); ok {
				os.Remove(blk["source"])
			}
		}
	}
	if err := VirtDomainAttachCdrom(domain, isoFile); err != nil {
		return err
	}
	return VirtDomainPoweron(domain)
}

func Inject2VM(vmUuid string, nbddev string) (err error) {
	domain, err := VirtDomainGet(vmUuid)
	if err != nil {
		return err
	}
	if domain == nil {
		return fmt.Errorf("domain not found")
	}
	return VirtDomainAttachNbd(domain, nbddev)
}

func EjectVM(vmUuid string) (err error) {
	domain, err := VirtDomainGet(vmUuid)
	if err != nil {
		return err
	}
	if domain == nil {
		return fmt.Errorf("domain not found")
	}
	disks, err := VirtDomainDisks(domain)
	if err != nil {
		return err
	}
	for _, blk := range disks {
		if blk["device"] == "cdrom" && strings.HasSuffix(blk["source"], ".iso") {
			err = VirtDomainDetachCdrom(domain)
			if err != nil {
				return err
			}
			os.Remove(blk["source"])
			return nil
		}
	}
	return fmt.Errorf("cdrom not found")
}

func Eject2VM(vmUuid string, nbddev string) (err error) {
	domain, err := VirtDomainGet(vmUuid)
	if err != nil {
		return err
	}
	if domain == nil {
		return fmt.Errorf("domain not found")
	}
	disks, err := VirtDomainDisks(domain)
	if err != nil {
		return err
	}
	if !strings.HasPrefix(nbddev, "/dev/") {
		nbddev = "/dev/" + nbddev
	}
	for _, blk := range disks {
		if blk["device"] == "cdrom" && blk["source"] == nbddev {
			return VirtDomainDetachNbd(domain)
		}
	}
	return fmt.Errorf("cdrom not found")

}

func SuspendVM(vmUuid string, poolDir string) (err error) {
	mempath := poolDir + "/volumes/" + vmUuid + ".suspend"
	defpath := poolDir + "/volumes/" + vmUuid + ".xml"
	domain, err := VirtDomainGet(vmUuid)
	if err != nil {
		return err
	}
	if domain == nil {
		return fmt.Errorf("domain not found")
	}
	state, err := VirtDomainState(domain)
	if err != nil {
		return err
	}
	if state == libvirt.DOMAIN_RUNNING {
		if err = VirtDomainSave(domain, mempath, defpath, ""); err != nil {
			return err
		}
	} else {
		if err = VirtDomainPoweroff(domain, true, 0); err != nil {
			return err
		}
	}
	return VirtDomainUndefine(domain)
}

func ModifyVM(vmUuid string, vcpu uint32, mem uint32, bootdev string) (err error) {
	domain, err := VirtDomainGet(vmUuid)
	if err != nil {
		return err
	}
	if domain == nil {
		return fmt.Errorf("domain not found")
	}
	if err = VirtDomainModify(domain, vcpu, mem, bootdev); err != nil {
		return err
	}
	return nil
}

func CreateVolSnap(volUuid string, snapUuid string, vmUuid string, poolDir string) (err error) {
	volFile := poolDir + "/volumes/" + volUuid + ".qcow2"
	if ok, _ := FileExists(volFile); !ok {
		return fmt.Errorf("volume file %s does not exist", volFile)
	}
	snapFile := poolDir + "/volumes/" + snapUuid + ".qcow2"
	if ok, _ := FileExists(snapFile); ok {
		return fmt.Errorf("snapshot file %s exists", snapFile)
	}
	if vmUuid != "" {
		if err = VmPreSnap(vmUuid, poolDir, ""); err != nil {
			return err
		}
	}

	if err = os.Rename(volFile, snapFile); err != nil {
		return err
	}

	cmd := fmt.Sprintf("qemu-img create -f qcow2 -F qcow2 -b %s %s", snapFile, volFile)
	if err = ExecuteCmdRun(G.config.CmdTimeout*100, cmd); err != nil {
		G.logger.Error("createvolsnap", zap.Error(err))
		return err
	}
	if vmUuid != "" {
		if err = VmPostSnap(vmUuid, poolDir, ""); err != nil {
			return err
		}
	}
	return nil
}

func CreateVolsSnap(volUuids []string, snapUuids []string, vmUuid string, poolDir string, cpUuid string) (err error) {
	if len(volUuids) != len(snapUuids) {
		return fmt.Errorf("number of vol uuids does not match to the number of snap uuids")
	}
	var volFiles []string
	for _, volUuid := range volUuids {
		volFile := poolDir + "/volumes/" + volUuid + ".qcow2"
		if ok, _ := FileExists(volFile); !ok {
			return fmt.Errorf("volume file %s does not exist", volFile)
		}
		volFiles = append(volFiles, volFile)
	}
	var snapFiles []string
	for _, snapUuid := range snapUuids {
		snapFile := poolDir + "/volumes/" + snapUuid + ".qcow2"
		if ok, _ := FileExists(snapFile); ok {
			return fmt.Errorf("snapshot file %s exists", snapFile)
		}
		snapFiles = append(snapFiles, snapFile)
	}
	if err = VmPreSnap(vmUuid, poolDir, cpUuid); err != nil {
		return err
	}
	for i := range volUuids {

		volFile := volFiles[i]
		snapFile := snapFiles[i]
		if err = os.Rename(volFile, snapFile); err != nil {
			return err
		}

		cmd := fmt.Sprintf("qemu-img create -f qcow2 -F qcow2 -b %s %s", snapFile, volFile)
		if err = ExecuteCmdRun(G.config.CmdTimeout*100, cmd); err != nil {
			G.logger.Error("createvolssnap", zap.Error(err))
			return err
		}
	}
	if err = VmPostSnap(vmUuid, poolDir, cpUuid); err != nil {
		return err
	}
	return nil
}

func DeleteVolSnap(snapUuid string, mergeTo []string, backing string, vmUuid string, poolDir string) (err error) {
	snapFile := poolDir + "/volumes/" + snapUuid + ".qcow2"
	if ok, _ := FileExists(snapFile); !ok {
		return fmt.Errorf("snapshot file %s does not exist", snapFile)
	}
	for i, m := range mergeTo {
		f := poolDir + "/volumes/" + m + ".qcow2"
		if ok, _ := FileExists(f); !ok {
			return fmt.Errorf("file %s does not exist", f)
		}
		mergeTo[i] = f
	}
	if backing != "" {
		f := poolDir + "/volumes/" + backing + ".qcow2"
		if ok, _ := FileExists(f); !ok {
			f = poolDir + "/backing/" + backing + ".qcow2"
			if ok, _ := FileExists(f); !ok {
				return fmt.Errorf("backing file %s does not exist", f)
			}
		}
		backing = f
	}
	if vmUuid != "" {
		if err = VmPreSnap(vmUuid, poolDir, ""); err != nil {
			return err
		}
	}
	if backing == "" {
		for _, m := range mergeTo {
			cmd := fmt.Sprintf("qemu-img convert -O qcow2 -c %s %s", m, m+".tmp")
			if err = ExecuteCmdRun(G.config.CmdTimeout*100, cmd); err != nil {
				G.logger.Error("deletevolsnap", zap.Error(err))
				return err
			}
			os.Remove(m)
			if err = os.Rename(m+".tmp", m); err != nil {
				G.logger.Error("deletevolsnap", zap.Error(err))
				return err
			}
		}
	} else {
		for _, m := range mergeTo {
			cmd := fmt.Sprintf("qemu-img rebase -F qcow2 -b %s %s", backing, m)
			if err = ExecuteCmdRun(G.config.CmdTimeout*100, cmd); err != nil {
				G.logger.Error("deletevolsnap", zap.Error(err))
				return err
			}
		}
	}
	os.Remove(snapFile)
	if vmUuid != "" {
		if err = VmPostSnap(vmUuid, poolDir, ""); err != nil {
			return err
		}
	}
	return nil
}

func DeleteVolsSnap(snapUuids []string, mergeTos map[string][]string, backings []string, vmUuid string, poolDir string, cpUuid string) (err error) {
	snapFiles := []string{}
	backingFiles := []string{}
	mergeToFiles := map[string][]string{}
	if len(backings) != len(snapUuids) {
		return fmt.Errorf("number of backings does not match to the number of snap uuids")
	}
	for _, snapUuid := range snapUuids {
		snapFile := poolDir + "/volumes/" + snapUuid + ".qcow2"
		if ok, _ := FileExists(snapFile); !ok {
			return fmt.Errorf("snapshot file %s does not exist", snapFile)
		}
		snapFiles = append(snapFiles, snapFile)
		mergeTo := mergeTos[snapUuid]
		for i, m := range mergeTo {
			f := poolDir + "/volumes/" + m + ".qcow2"
			if ok, _ := FileExists(f); !ok {
				return fmt.Errorf("file %s does not exist", f)
			}
			mergeTo[i] = f
		}
		mergeToFiles[snapUuid] = mergeTo
	}
	for _, backing := range backings {
		if backing != "" {
			f := poolDir + "/volumes/" + backing + ".qcow2"
			if ok, _ := FileExists(f); !ok {
				f = poolDir + "/backing/" + backing + ".qcow2"
				if ok, _ := FileExists(f); !ok {
					return fmt.Errorf("backing file %s does not exist", f)
				}
			}
			backing = f
		}
		backingFiles = append(backingFiles, backing)
	}
	if err = VmPreSnap(vmUuid, poolDir, ""); err != nil {
		return err
	}
	for i := range snapUuids {
		backing := backingFiles[i]
		mergeTo := mergeToFiles[snapUuids[i]]
		if backing == "" {
			for _, m := range mergeTo {
				cmd := fmt.Sprintf("qemu-img convert -O qcow2 -c %s %s", m, m+".tmp")
				if err = ExecuteCmdRun(G.config.CmdTimeout*100, cmd); err != nil {
					G.logger.Error("deletevolssnap", zap.Error(err))
					return err
				}
				os.Remove(m)
				if err = os.Rename(m+".tmp", m); err != nil {
					G.logger.Error("deletevolssnap", zap.Error(err))
					return err
				}
			}
		} else {
			for _, m := range mergeTo {
				cmd := fmt.Sprintf("qemu-img rebase -F qcow2 -b %s %s", backing, m)
				if err = ExecuteCmdRun(G.config.CmdTimeout*100, cmd); err != nil {
					G.logger.Error("deletevolssnap", zap.Error(err))
					return err
				}
			}
		}
		os.Remove(snapFiles[i])
	}
	mempath := poolDir + "/volumes/" + cpUuid + ".mem"
	if ok, _ := FileExists(mempath); ok {
		os.Remove(mempath)
	}
	defpath := poolDir + "/volumes/" + cpUuid + ".xml"
	if ok, _ := FileExists(defpath); ok {
		os.Remove(defpath)
	}
	if err = VmPostSnap(vmUuid, poolDir, ""); err != nil {
		return err
	}
	return nil
}

func SwitchVolSnap(snapUuid string, volUuid string, vmUuid string, poolDir string) (err error) {
	snapFile := poolDir + "/volumes/" + snapUuid + ".qcow2"
	if ok, _ := FileExists(snapFile); !ok {
		return fmt.Errorf("snapshot file %s does not exist", snapFile)
	}
	volFile := poolDir + "/volumes/" + volUuid + ".qcow2"
	if ok, _ := FileExists(volFile); !ok {
		return fmt.Errorf("volume file %s does not exist", volFile)
	}
	var state string
	if vmUuid != "" {

		if err = PoweroffVM(vmUuid, true); err != nil {
			return err
		}
	}
	os.Remove(volFile)
	cmd := fmt.Sprintf("qemu-img create -f qcow2 -F qcow2 -b %s %s", snapFile, volFile)
	if err = ExecuteCmdRun(G.config.CmdTimeout*100, cmd); err != nil {
		G.logger.Error("switchvolsnap", zap.Error(err))
		return err
	}
	if vmUuid != "" {
		if state != "shut off" {
			if err = PoweronVM(vmUuid); err != nil {
				return err
			}
		}
	}
	return nil
}

func SwitchVolsSnap(snapUuids []string, volUuids []string, vmUuid string, poolDir string, cpUuid string, gpus []*GPU) (err error) {
	if len(volUuids) != len(snapUuids) {
		return fmt.Errorf("number of vol uuids does not match to the number of snap uuids")
	}
	var volFiles []string
	for _, volUuid := range volUuids {
		volFile := poolDir + "/volumes/" + volUuid + ".qcow2"
		if ok, _ := FileExists(volFile); !ok {
			return fmt.Errorf("volume file %s does not exist", volFile)
		}
		volFiles = append(volFiles, volFile)
	}
	var snapFiles []string
	for _, snapUuid := range snapUuids {
		snapFile := poolDir + "/volumes/" + snapUuid + ".qcow2"
		if ok, _ := FileExists(snapFile); !ok {
			return fmt.Errorf("snapshot file %s does not exist", snapFile)
		}
		snapFiles = append(snapFiles, snapFile)
	}
	var state string
	if vmUuid != "" {

		if state, err = VmState(vmUuid); err != nil {
			return err
		}
		if err = PoweroffVM(vmUuid, true); err != nil {
			return err
		}
	}
	for i := range snapUuids {
		snapFile := snapFiles[i]
		volFile := volFiles[i]
		os.Remove(volFile)
		cmd := fmt.Sprintf("qemu-img create -f qcow2 -F qcow2 -b %s %s", snapFile, volFile)
		if err = ExecuteCmdRun(G.config.CmdTimeout*100, cmd); err != nil {
			G.logger.Error("switchvolsnap", zap.Error(err))
			return err
		}
	}
	if vmUuid != "" {
		mempath := poolDir + "/volumes/" + cpUuid + ".mem"
		defpath := poolDir + "/volumes/" + cpUuid + ".xml"
		if ok, _ := FileExists(mempath); !ok {
			if state != "shut off" {
				if err = PoweronVM(vmUuid); err != nil {
					return err
				}
			}
		} else {

			if err = VirtDomainRedefine(vmUuid, defpath); err != nil {
				return err
			}
			if err = VirtDomainRestore(mempath, defpath); err != nil {
				return err
			}
			domain, err := VirtDomainGet(vmUuid)
			if err != nil {
				return err
			}
			if domain == nil {
				return fmt.Errorf("domain not found")
			}
			for _, gpu := range gpus {
				if err = _attachGPU(domain, gpu); err != nil {
					return err
				}
			}
		}
	}
	return nil
}

func VmPreSnap(vmUuid string, poolDir string, cpUuid string) (err error) {
	domain, err := VirtDomainGet(vmUuid)
	if err != nil {
		return err
	}
	if domain == nil {
		return fmt.Errorf("domain not found")
	}
	state, err := VirtDomainState(domain)
	if err != nil {
		return err
	}
	if state == libvirt.DOMAIN_SHUTOFF {
		return nil
	}
	var mempath string
	var defpath string
	if cpUuid != "" {
		mempath = poolDir + "/volumes/" + cpUuid + ".mem"
		defpath = poolDir + "/volumes/" + cpUuid + ".xml"
	} else {
		mempath = poolDir + "/volumes/" + vmUuid + ".tmpmem"
	}
	pcipath := poolDir + "/volumes/" + vmUuid + ".tmppci"
	return VirtDomainSave(domain, mempath, defpath, pcipath)
}

func VmPostSnap(vmUuid string, poolDir string, cpUuid string) (err error) {
	var mempath string
	var defpath string
	if cpUuid != "" {
		mempath = poolDir + "/volumes/" + cpUuid + ".mem"
		defpath = poolDir + "/volumes/" + cpUuid + ".xml"
	} else {
		mempath = poolDir + "/volumes/" + vmUuid + ".tmpmem"
	}
	pcipath := poolDir + "/volumes/" + vmUuid + ".tmppci"
	if ok, _ := FileExists(mempath); !ok {
		return nil
	}
	if cpUuid == "" {
		defer os.Remove(mempath)
	}
	if err = VirtDomainRestore(mempath, defpath); err != nil {
		return err
	}
	domain, err := VirtDomainGet(vmUuid)
	if err != nil {
		return err
	}
	if domain == nil {
		return fmt.Errorf("domain not found")
	}
	if ok, _ := FileExists(pcipath); ok {
		defer os.Remove(pcipath)
		var hostdevs []byte
		hostdevs, err = os.ReadFile(pcipath)
		if err != nil {
			G.logger.Error("cannot read hostdevs", zap.Error(err))
			return err
		}
		for _, hostdev := range strings.Split(string(hostdevs), "\n") {
			if hostdev == "" {
				continue
			}
			if err = VirtDomainAttachHostdev(domain, hostdev); err != nil {
				G.logger.Error("cannot attach hostdev", zap.Error(err))
				return err
			}
		}
	}
	return nil
}

func DetachGPU(vm *VM, gpu *GPU) error {
	domain, err := VirtDomainGet(vm.Uuid)
	if err != nil {
		return err
	}
	if domain == nil {
		return fmt.Errorf("domain not found")
	}
	return _detachGPU(domain, gpu)
}

func _detachGPU(domain *libvirt.Domain, gpu *GPU) error {
	if err := VirtDomainDetachHostdev(domain, gpu.DevicePci); err != nil {
		return err
	}
	if gpu.IBPci != "" {
		if err := VirtDomainDetachHostdev(domain, gpu.IBPci); err != nil {
			return err
		}
	}
	for _, pci := range gpu.OtherPcis {
		if pci == "" {
			continue
		}
		if err := VirtDomainDetachHostdev(domain, pci); err != nil {
			return err
		}
	}
	return nil
}

func AttachGPU(vm *VM, gpu *GPU) error {
	domain, err := VirtDomainGet(vm.Uuid)
	if err != nil {
		return err
	}
	if domain == nil {
		return fmt.Errorf("domain not found")
	}
	return _attachGPU(domain, gpu)
}

func _attachGPU(domain *libvirt.Domain, gpu *GPU) error {
	if err := VirtDomainAttachHostdev(domain, gpu.DevicePci); err != nil {
		return err
	}
	if gpu.IBPci != "" {
		if err := VirtDomainAttachHostdev(domain, gpu.IBPci); err != nil {
			return err
		}
	}
	for _, pci := range gpu.OtherPcis {
		if pci == "" {
			continue
		}
		if err := VirtDomainAttachHostdev(domain, pci); err != nil {
			return err
		}
	}
	return nil
}

func DetachAllHostdev(domain *libvirt.Domain) (pcis []string, err error) {
	hostdevs, err := VirtDomainHostdevs(domain)
	if err != nil {
		return nil, err
	}
	for _, hostdev := range hostdevs {
		if err = VirtDomainDetachHostdev(domain, hostdev); err != nil {
			return nil, err
		}
		pcis = append(pcis, hostdev)
	}
	return pcis, nil
}
