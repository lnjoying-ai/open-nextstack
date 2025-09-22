import subprocess
import os


grafana_server = 'grafana_server'
nodes = 'nodes'
prometheus_server = 'prometheus_server'
network_agent_server = 'network_agent'
pxe_agent_server = 'pxe_agent'

def open_config():
    with open('./init.config') as f:
        configs = f.readlines()
        for config in configs:
            if config.startswith(grafana_server): 
                monitor_server = config.strip().split('=')[1].strip()
                grafana_config(monitor_server)
                nginx_config(monitor_server)
                openresty_config(monitor_server)
                compute_grafana_config(monitor_server)
            if config.startswith(nodes): prometheus_config(config.strip().split('=')[1].strip())
            if config.startswith(prometheus_server): 
                prometheus_ip = config.strip().split('=')[1].strip()
                datasource_config(prometheus_ip)
                compute_prometheus_config(prometheus_ip)
            if  config.startswith(network_agent_server): network_config(config.strip().split('=')[1].strip())
            if  config.startswith(pxe_agent_server): compute_pxe_config(config.strip().split('=')[1].strip())

#pxeAgentUrl: http://localhost:8898
#waitingRemoveTime: 0.01
#monitorServer: http://localhost:3000
#prometheusServer: http://localhost:9090

def compute_grafana_config(config):
    cmd = "sed -i '/^monitorServer/s/localhost/%s/' justice/config/etc/compute.yaml" %(config)
    result = subprocess.run(cmd, shell=True, stdout=subprocess.PIPE)
    print(result)

def compute_prometheus_config(config):
    cmd = "sed -i '/^prometheusServer/s/localhost/%s/' justice/config/etc/compute.yaml" %(config)
    result = subprocess.run(cmd, shell=True, stdout=subprocess.PIPE)
    print(result)

def network_config(config):
    cmd = "sed -i 's/localhost/%s/' justice/config/etc/network.yaml" %(config)
    result = subprocess.run(cmd, shell=True, stdout=subprocess.PIPE)
    print(result)


def compute_pxe_config(config):
    cmd = "sed -i 's/localhost/%s/' justice/config/etc/compute.yaml" %(config)
    result = subprocess.run(cmd, shell=True, stdout=subprocess.PIPE)
    print(result)

def nginx_config(config):
    cmd = "sed -i 's/localhost/%s/' nginx/nginx.conf" %(config)
    result = subprocess.run(cmd, shell=True, stdout=subprocess.PIPE)
    print(result)

def datasource_config(config):
    cmd = "sed -i 's/localhost/%s/' grafana/provisioning/datasources/datasources.yaml" %(config)
    result = subprocess.run(cmd, shell=True, stdout=subprocess.PIPE)
    print(result)

def openresty_config(config):
    cmd = "sed -i 's/192.168.11.220:3000/%s:3000/g' openresty/nginx.conf" %(config)
    result = subprocess.run(cmd, shell=True, stdout=subprocess.PIPE)
    print(result)



def grafana_config(config):
    cmd = "sed -i '/^root_url/s/localhost/%s/' grafana/grafana.ini" %(config)
    result = subprocess.run(cmd, shell=True, stdout=subprocess.PIPE)

#    subprocess.call(cmd)

def prometheus_config(config):
    libvirt_port=9000
    node_port=9100
    libvirt_servers = []
    node_servers = []
    for node in config.split(","):
        libvirt_server = "%s:%d" %(node,libvirt_port)
        node_server = "%s:%d" %(node, node_port)
        libvirt_servers.append(libvirt_server)
        node_servers.append(node_server)
        
    libvirt_server_pro(libvirt_servers)
    node_server_pro(node_servers)

def node_server_pro(nodes):
    nodes_str=str(nodes)
    cmd = "sed -i 's/list/%s/' prometheus/node.json" %(nodes_str.replace("'",'"'))
    result = subprocess.run(cmd, shell=True, stdout=subprocess.PIPE)
    print(result)
def libvirt_server_pro(nodes):
    nodes_str=str(nodes)
    cmd = "sed -i 's/list/%s/' prometheus/libvirt.json" %(nodes_str.replace("'",'"'))
    result = subprocess.run(cmd, shell=True, stdout=subprocess.PIPE)
    print(result)

def setup_system():
    firewall_cmd = "systemctl stop firewalld && systemctl disable firewalld"
    selinux_cmd = "sed -i '/SELINUX=enforcing/s/enforcing/disabled/' /etc/selinux/config"
    subprocess.run(firewall_cmd, shell=True, stdout=subprocess.PIPE)
    subprocess.run(selinux_cmd, shell=True, stdout=subprocess.PIPE)

def install_docker():
    yum_cmd = "yum install -y yum-utils"
    add_repo_cmd = "yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo"
    install_cmd = "yum install -y docker-ce docker-ce-cli containerd.io"
    subprocess.run(yum_cmd, shell=True, stdout=subprocess.PIPE)
    subprocess.run(add_repo_cmd, shell=True, stdout=subprocess.PIPE)
    subprocess.run(install_cmd, shell=True, stdout=subprocess.PIPE)


#setup_system()
open_config()

