#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
测试 etcd 集群配置功能的脚本
"""

import sys
import os

# 添加当前目录到 Python 路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from gnext_depoly_geo import *

def test_config_parsing():
    """测试配置文件解析"""
    config_file = "config.etcd.example.yml"
    if not os.path.exists(config_file):
        print(f"Configuration file {config_file} not found")
        return False
    
    # 加载配置
    load_config(config_file)
    
    # 检查节点配置
    nodes = g_settings.config.get("nodes", [])
    etcd_nodes = [node for node in nodes if node.get("etcd")]
    
    print(f"Found {len(etcd_nodes)} etcd nodes:")
    for node in etcd_nodes:
        etcd_name = node["etcd"]["etcd_name"]
        host = node["host"]
        print(f"  - {etcd_name}: {host}")
    
    # 判断是单节点还是集群模式
    is_single_node = len(etcd_nodes) == 1
    if is_single_node:
        print("Configuration: Single node mode")
    else:
        print("Configuration: Cluster mode")
    
    return len(etcd_nodes) > 0

def test_cluster_member_generation():
    """测试集群成员列表生成"""
    config_file = "config.etcd.example.yml"
    load_config(config_file)
    
    nodes = g_settings.config.get("nodes", [])
    etcd_nodes = [node for node in nodes if node.get("etcd")]
    
    # 构建集群成员列表
    cluster_members = []
    for node in etcd_nodes:
        etcd_name = node["etcd"]["etcd_name"]
        host_ip = node["host"]
        cluster_members.append(f"{etcd_name}=http://{host_ip}:2380")
    
    initial_cluster = ",".join(cluster_members)
    print(f"Initial cluster string: {initial_cluster}")
    
    # 测试单节点和集群模式的不同配置
    is_single_node = len(etcd_nodes) == 1
    if is_single_node:
        print("Single node configuration will use --force-new-cluster flag")
    else:
        print("Cluster configuration will use normal cluster settings")
    
    return initial_cluster

if __name__ == "__main__":
    print("Testing etcd configuration...")
    
    if test_config_parsing():
        print("✓ Configuration parsing test passed")
    else:
        print("✗ Configuration parsing test failed")
        sys.exit(1)
    
    cluster_string = test_cluster_member_generation()
    if cluster_string:
        print("✓ Cluster member generation test passed")
    else:
        print("✗ Cluster member generation test failed")
        sys.exit(1)
    
    print("All tests passed!")
