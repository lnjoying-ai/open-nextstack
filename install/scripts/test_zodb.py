#!/usr/bin/env python
import os
import threading
import ZODB.DB
import ZODB.FileStorage
from persistent.list import PersistentList
import transaction

if os.path.exists('mydata.fs'):
    os.unlink('mydata.fs')
storage = ZODB.FileStorage.FileStorage('mydata.fs')
db = ZODB.DB(storage,pool_size=100)


def add(thread_num):
    local = threading.local()
    for attempt in transaction.manager.attempts(100):
        with attempt:
            if not hasattr(local, "conn"):
                local.conn = db.open()
            root = local.conn.root()
            item = root.get('list')
            if item is None:
                item = PersistentList()
                root['list'] = item
            for i in range(10):
                val = "%s-%s" % (thread_num, i)
                item.append(val)
                print(root["list"])

# def add2(thread_num):
#     local = threading.local()
#     for i in range(10):
#         for attempt in transaction.manager.attempts(100):
#             with attempt:
#                 if not hasattr(local, "conn"):
#                     local.conn = db.open()
#                 root = local.conn.root()
#                 item = root.get('list')
#                 if item is None:
#                     item = PersistentList()
#                     root['list'] = item
#                 val = "%s-%s" % (thread_num, i)
#                 item.append(val)

# threads = []
# for i in range(30):
#     thread = threading.Thread(target=add2, args=(i,))
#     thread.start()
#     threads.append(thread)

# for thread in threads:
#     thread.join()

# with db.transaction() as conn:
#     root = conn.root()
#     print(root['list'])
#     print(len(root['list']))

conn = db.open()
root = conn.root()
root["two"] = 2
#transaction.commit()

thread = threading.Thread(target=add, args=(10,))
thread.start()
thread.join()
#transaction.begin()
#root["two"] = 2
transaction.commit()
print("="+root['list'])
print("="+str(root['two']))
#for attempt in transaction.manager.attempts(100):
#    with attempt:
#        print("="+root['list'])
