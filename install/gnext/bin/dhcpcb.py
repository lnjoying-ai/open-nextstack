#!/usr/bin/env python3
import requests
import os
from sys import argv


def main(argv):
    if len(argv) >= 3:
        l3 = os.environ.get("L3AGENT_SERVICE")
        if l3:
            json = {"action": argv[0], "ip": argv[2]}
            mac = argv[1]
            requests.post("http://%s/v1/ports/%s/dhcp" % (l3, mac), json=json)


if __name__ == "__main__":
    main(argv[1:])
