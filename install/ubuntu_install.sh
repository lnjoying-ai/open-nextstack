#!/bin/bash

#region 常量配置
declare -rx DOCKER_VERSION="20.10.17"
declare -rx GW="edge-gw"
declare -rx AGENT="edge-agent"
declare -rx NVIDIA_DRIVER='NVIDIA-Linux-x86_64-470.86.run'
declare -rx GPU_DRIVER_PARAM='--no-opengl-files'
declare -rx FILE_HOST='http://edge.lnjoying.com:9080/lnjoying/v12/edge_cloud/amd64/agent'
declare -rx FILE_HOST_IP='http://edge.lnjoying.com:9080/lnjoying/v12/edge_cloud/amd64/agent'
declare -rx INSTALL_SOFTWARE="gnupg2 pass xfsprogs gcc+ wget curl vim openssh-server gcc make util-linux python3 net-tools"
declare -rx KERNEL_VERSION=$(uname -r | cut -d '-' -f 1)
declare -rx REQUIRED_VERSION="4.19.0"
declare -rx USER_HOME_DIR=~/
PS3="please input the option number:"
#endregion

function echo_red() {
  echo -e "\033[1;31m$1\033[0m"
}

function echo_green() {
  echo -e "\033[1;32m$1\033[0m"
}

function echo_yellow() {
  echo -e "\033[1;33m$1\033[0m"
}

function echo_done() {
  sleep 0.5
  echo "$(gettext 'complete')"
}

function echo_check() {
  echo -e "$1 \t [\033[32m √ \033[0m]"
}


function log_info() {
  echo_green "[INFO] $1"
}

function log_warn() {
  echo_yellow "[WARN] $1"
}

function log_error() {
  echo_red "[ERROR] $1"
}

function try_download() {
  local arg_arr=("$@")
  local max_try_times="${arg_arr[0]}"
  local count="${#arg_arr[@]}"
  local flag=0
  for ((i=1; i<"$count"; i++)); do
    url="${arg_arr[i]}"
    for((j=0;j<"$max_try_times";j++));do
          local rest_times=$((j+1))
          echo "尝试($rest_times/3)-下载$url..."
          wget -nc "$url"
          if [ $? == 0 ]; then
            echo "成功下载 $url"
            flag=1
            break
          fi
          if [ "$flag" -eq 1 ]; then
            break
          fi
    done
    if [ "$flag" -eq 1 ]; then
      break
    fi
  done
  if [ ! "$flag" -eq 1 ]; then
    echo "下载失败"
    kill -SIGINT $$
  fi
  unset flag
}

function exit_self() {
  echo -e "\n\n\033[1;32mThe deployment has been terminated!\033[0m"
  exit 1
}

function quick_fail() {
    kill -SIGINT $$
}

function command_exists() {
    command -v "$@" > /dev/null 2>&1
}

trap exit_self SIGINT
set -e

function __empty_line__(){
  echo -e "\n"
}

function script_debug()
{
    read -rp "Do you want to turn on the script debugging function? yes/no The default is No >" input_option
    if [[ $input_option =  yes ]];then
        echo "Successfully opened script debugging function"
        set -x
    else
        echo "Will not open script debugging function"
    fi
}

function docker_run()
{
    docker run --rm --privileged -v /var/run/docker.sock:/var/run/docker.sock $*
}

function version_gt() { test "$(echo "$@" | tr " " "\n" | sort -V | head -n 1)" != "$1"; }

function kernel_check()
{
    if  version_gt $REQUIRED_VERSION $KERNEL_VERSION; then
      log_error "kernel version is less than 4.19"
      log_error "Please manually upgrade thr kernel to version 4.19 or above"
      uname -a
      kill -SIGINT $$
    else
      log_info "kernel done"
    fi
}

function root_check()
{
    if [ $EUID -ne 0 ]; then
	    log_error This script must be run as root
      log_error Please use the su command to enter the root user to run this script
	    kill -SIGINT $$
    fi
}

function replace_apt_mirror_with_aliyun(){
    if [ "$(id -u)" -ne 0 ]; then
        echo "Please run as root!"
        kill -SIGINT $$
    fi
    cp /etc/apt/sources.list "/etc/apt/sources.list.bak.$(date "+%Y-%m-%d %H:%M:%S")"
    sed -i 's/http:\/\/archive.ubuntu.com/http:\/\/mirrors.aliyun.com/g' /etc/apt/sources.list
    sed -i 's/http:\/\/security.ubuntu.com/http:\/\/mirrors.aliyun.com/g' /etc/apt/sources.list
    apt-get update
}

function install_required_software()
{
    log_info "Installing prerequisite software"
    touch "$USER_HOME_DIR"/old_file/lnjoying_install.log
    read -rp "Do you need to change the software source? yes/no The default is No >" input_option
    if  [[ "$input_option" == "yes"  ]];then
        replace_apt_mirror_with_aliyun
        echo "The software source has been replaced."
    else
        echo "The software source is using the default."
    fi
	  apt install -y $INSTALL_SOFTWARE > /root/old_file/lnjoying_install.log 2>&1 || quick_fail
   	log_info "Installing prerequisite software completed"
}

function disk_hdd()
{
    echo "mkfs hdd"
    lsblk -i
    echo -e "The default is No"
    read -rp "mkfs.xfs yes/no?>" input_option
    if [[ $input_option = 'yes' ]];then
        read -rp "disk >"  hdd
        log_info "The HDD will be formatted for you"
        mkfs.xfs -f $hdd
        outfile=/etc/fstab
        echo "$hdd  /data  xfs  defaults  0  0" >> $outfile
        log_info mount_hdd done...
    else
        log_warn "umount_hdd"
    fi
}


function agent_cleaning() {
    log_info "Removing agent..."
    if [ -n "$AGENT_ID" ]; then
        umount "/data/docker/containers/$AGENT_ID/mounts/shm"
        rm -rf "data/docker/containers/$AGENT_ID/$AGENT_ID-json.log"
    fi
    if command_exists docker; then
        docker stop justice-agent || true
        docker rm justice-agent || true
    fi
    rm -rf /var/lnjoying/justice-agent/data
    log_info "agent cleaning completed"
}

function gw_cleaning() {
    log_info "Removing gw..."
    if [ -n "$GW_ID" ]; then
        umount "/data/docker/containers/$GW_ID/mounts/shm"
        rm -rf "data/docker/containers/$GW_ID/$GW_ID-json.log"
    fi
    if command_exists docker; then
        docker stop justice-gw || true
        docker rm justice-gw || true
    fi
    rm -rf "/var/dat/lnjoying/justice-gw" \
         "/var/db/lnjoying/justice-gw" \
         "/var/log/lnjoying/justice-gw" \
         "/var/run/lnjoying/storage"
    log_info "gw cleaning completed"
}

function lnjoying_rm()
{
    log_info "lnjoying_rm task is running..."
    docker_installed=0
    if command_exists docker; then
        docker_installed=1
    fi
    if [ "$docker_installed" -gt 0 ]; then
        agent_container=$(docker ps -aq --filter name=justice-agent --format "{{.Names}}" | head -n 1)
        gw_container=$(docker ps -aq --filter name=justice-gw --format "{{.Names}}" | head -n 1)
    fi
    if [[ "$agent_container" == "justice-agent" && "$gw_container" == "justice-gw"  ]] ;then
        log_warn "The containers justice-agent and justice-gw already exist!"
    elif [[ $gw_container == "justice-gw"  ]];then
        log_warn "The justice-gw container already exists!"
    elif [[ $agent_container == "justice-agent"  ]];then
        log_warn "The agent_container already exists!"
    else
        log_warn "There are no lnjoying containers running!"
    fi
    if [ "$docker_installed" -gt 0 ]; then
        AGENT_ID=$(docker ps -aq --no-trunc  --filter "name=justice-agent" | head -n 1)
        GW_ID=$(docker ps -aq --no-trunc  --filter "name=justice-gw" | head -n 1)
    fi
    echo "Please select an option to delete the corresponding cache, enter the option number:"
    select option in "agent" "gw" "all(agent and gw)" "none"
    do
      case $option in
        "agent")
          agent_cleaning
        break
        ;;
        "gw")
          gw_cleaning
        break
        ;;
        "all(agent and gw)")
          log_info "Removing agent and gw..."
          if [ "$docker_installed" -gt 0 ]; then
              local gw_agent_regex="^justice-(gw|agent)(-.*)?$"
              if [ $(docker ps -a -f name="$gw_agent_regex" --format '{{.Names}}' | wc -l) -gt 0 ]; then
                 docker ps -a -f name="$gw_agent_regex" --format '{{.Names}}' | xargs docker stop
                 docker ps -a -f name="$gw_agent_regex" --format '{{.Names}}' | xargs docker rm
              fi
          fi
          agent_cleaning || quick_fail
          gw_cleaning || quick_fail
        break
        ;;
        "none")
          log_info "None has been selected and nothing will be deleted."
        break
        ;;
        *)
        log_error "Input error, please try again."
        ;;
      esac
    done
}

function docker_install()
{
    log_info "Installing docker..."
    if command_exists docker; then
        log_info "Docker has been detected on the current system"
        docker_version=$(docker info --format='{{.ServerVersion}}' | cut -d "." -f 1)
        docker_dir=$(docker info --format='{{.DockerRootDir}}')
    else
        docker_version=0
    fi

    if [[ $docker_version -lt 19 ]];then
        read -rp "docker version is less than 19 or not installed, you'll need to reinstall, please confirm and continue. yes/no? >" docker_version_install
        if [[ $docker_version_install == "yes"  ]];then
            echo "docker will be installed or upgraded immediately"
        else
            exit_self || (log_error "docker installation is terminated." && kill -SIGINT $$)
        fi
        if [ "$docker_version" -gt 0 ]; then
            systemctl stop docker
        fi
        apt autoremove -y docker docker-ce docker-engine  docker.io  containerd runc
        if command_exists dpkg; then
          dpkg -l |grep ^rc | awk '{print $2}' | xargs dpkg -P docker
        fi
        apt -y install autoremove docker-ce-* || true
        if [ -d "$docker_dir" ]; then
          mv "$docker_dir" "$USER_HOME_DIR/old_file/docker.bak"
          mv /etc/systemd/system/docker.service.d "$USER_HOME_DIR"/old_file/docker.service.d.bak || true
        fi
        curl -fsSL https://get.docker.com -o get-docker.sh && sh get-docker.sh --mirror Aliyun --version "$DOCKER_VERSION"
	      systemctl enable docker
	      systemctl start docker
        echo "docker reinstall done."
    else
        docker -v
    fi

#    read -rp "Do you want to replace the docker configuration file, yes/no? The default is No >" input_option
#    if [[ $input_option == "yes" ]]; then
#        cd /etc/docker || (log_error "cd /etc/docker failed" && kill -SIGINT $$)
#        mv /etc/docker/daemon.json /root/old_file/daemon.json.bak
#        daemon_json__download_url=("$FILE_HOST/daemon.json" "$FILE_HOST_IP/daemon.json")
#        try_download 3 "${daemon_json__download_url[@]}"
#        systemctl daemon-reload
#        systemctl restart docker
#    else
#        log_warn "keep the current configuration file"
#    fi
}


function nvidia_driver_install()
{
    __empty_line__
    log_info "nvidia_driver install"
    lspci | grep NVIDIA || true
    if [[ $? -eq 0 ]];then
        if command_exists nvidia-smi;then
            log_info "NVIDIA driver installed"
        else
            echo "Not found NVIDIA driver"
            read -rp "install NVIDIA driver yes/no? >" nvidia_options
            if [[ $nvidia_options = 'yes' ]];then
                no_nouveau_sh__download_url=("$FILE_HOST/no_nouveau.sh" "$FILE_HOST_IP/no_nouveau.sh")
                try_download 3 "${no_nouveau_sh__download_url[@]}"
                bash no_nouveau.sh

                nvvidia_driver__download_url=("$FILE_HOST/$NVIDIA_DRIVER" "$FILE_HOST_IP/$NVIDIA_DRIVER")
                try_download 3 "${nvvidia_driver__download_url[@]}"
                bash $NVIDIA_DRIVER $GPU_DRIVER_PARAM

                nvidia_docker_sh__download_url=("$FILE_HOST/nvidia_docker.sh" "$FILE_HOST_IP/nvidia_docker.sh")
                try_download 3 "${nvidia_docker_sh__download_url[@]}"
                bash nvidia_docker.sh
                nvidia-persistenced
                log_info "NVIDIA driver is installed successfully."
            else
                log_warn "Since no was selected, the NVIDIA driver installation will be skipped."
            fi
        fi
    else
        log_info "No graphics card detected. Installation will continue"
    fi
}

function help()
{
    echo "agent examples are as follows"
    echo "============================="
    echo "bash <(curl -s -S -L lnjoying.io:9080/lnjoying/agent/install.sh)  lnjoying/edge-agent:v0.3.0.pre --node_name 001 --rest 0.0.0.0:41107 --region gw1 --site st1 --reg_token 503889"
    echo "============================="
    echo "gw examples are as follows"
    echo "============================="
    echo "bash <(curl -s -S -L lnjoying.io:9080/lnjoying/agent/install.sh) -e port_bind=0.0.0.0:6666:6666 lnjoying/edge-gw:v0.3.0.1 --advertise 0.0.0.0:6666 --node_name test02 --rest 0.0.0.0:41107 --local_endpoint 0.0.0.0:6666 --region 000000001 --cloud 27.221.79.185:9188 --reg_token 845554"
    echo "============================="
}

function agent_run()
{
    log_info "Installing agent container..."
    echo "docker_run $*"
    docker_run "$*"
    if [[ $? -eq 0  ]];then
        log_info "Installing agent container completed"
         exit 0
    else
        log_error "Installing agent container failed"
        kill -SIGINT $$
    fi
}


function gw_run()
{
    log_info "Installing gw container..."
    echo "docker_run $*"
    docker_run "$*"
    if [[ $? -eq 0  ]]; then
        log_info "Installing gw container completed"
        exit 0
    else
      log_error "Installing gw container failed"
      kill -SIGINT $$
    fi
}

function agent_dns()
{
    nslookup lnjoying-test.jdl.com | egrep 'Address.*'|awk '{if(NR!=1)print $NF}'
}

function create_required_dirs() {
  log_info "Creating required directories..."
  mkdir -p "$USER_HOME_DIR"/old_file
  mkdir -p /data
  log_info "Creating required directories completed"
}

function install_agent() {
  log_info "############ Installing agent ############"
  docker_install || quick_fail
  nvidia_driver_install || quick_fail
  #agent_dns
  agent_run "$*"
}

function install_gw() {
  log_info "############ Installing gw ############"
  docker_install || quick_fail
  #gw_dns
  gw_run "$*"
}

function do_install() {
  if [ $(echo "$*" | grep -c "$AGENT") -gt 0 ]; then
    install_agent "$*"
  elif [ $(echo "$*" | grep -c "$GW") -gt 0 ]; then
    install_gw "$*"
  else
    echo "Input Is Error"
    help
    quick_fail
  fi
}

############ main code goes below ############
install_procedures=(
[0]=script_debug
[1]=create_required_dirs
[2]=install_required_software
[3]=lnjoying_rm
[4]=do_install
)
function install_executor() {
  func_len=${#install_procedures[@]}
  progress_index=0
  for precedure_index in "${!install_procedures[@]}"; do
   if [ -z "${install_procedures[$precedure_index]}" ]; then
     continue
   fi
   progress_index=$((progress_index+1))
   "${install_procedures[$precedure_index]}" "$*"
   if [ $? -ne 0 ]; then
      log_error "${install_procedures[$precedure_index]} failed!" && quick_fail
   fi
   install_progress=$(( progress_index * 100 / func_len ))
   log_info "##################### progress：${install_progress}％"
   __empty_line__
  done
}


__empty_line__
log_info "Installing for Ubuntu amd64 architecture"
if [ "$USER" != "root" ]; then
    while [ -z "$SUDO_USER" ]; do
      echo
			cat >&2 <<-'EOF'
			Error: this installer needs the ability to run commands as root.
			Please run this script either as root, or using sudo to perform the installation
			EOF
			exit 1
    done
fi

install_executor "$*"
