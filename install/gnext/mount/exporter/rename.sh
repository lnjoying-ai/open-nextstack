#!/bin/bash
find manager_dir -name "*.temp.bak" -exec bash -c 'mv $0 ${0%.temp.bak}' {} \;
