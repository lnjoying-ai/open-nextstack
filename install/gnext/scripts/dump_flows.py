#!/usr/bin/env python
import sys
import subprocess

# usage: dump_flows.py <bridge_name>
cmd = "ovs-ofctl dump-flows %s" % sys.argv[1]
p = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE)
for line in p.stdout:
    line = line.decode("utf-8").strip()
    # split line by comma and trim spaces
    parts = [part.strip() for part in line.split(",")]

    # remove cookie, duration, n_bytes, idle_age and value of them
    parts = [
        part
        for part in parts
        if not part.startswith("cookie=")
        and not part.startswith("duration=")
        and not part.startswith("n_bytes=")
        and not part.startswith("idle_age=")
        and not part.startswith("hard_age=")
        and not part.startswith("n_packets=")
    ]
    line = ", ".join(parts)
    print(line)
