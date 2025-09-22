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
	"sync"

	"go.etcd.io/etcd/client/v3/concurrency"
	recipe "go.etcd.io/etcd/client/v3/experimental/recipes"
	"go.uber.org/zap"
)

type RWLockMgr struct {
	Session   *concurrency.Session
	LocalLock *sync.RWMutex
	Locks     map[string]*recipe.RWMutex
}

func NewRWLockMgr() *RWLockMgr {
	session, err := concurrency.NewSession(G.v3client)
	localLock := sync.RWMutex{}
	if err != nil {
		return nil
	}
	return &RWLockMgr{
		Session:   session,
		LocalLock: &localLock,
		Locks:     make(map[string]*recipe.RWMutex),
	}
}

func (rwLockMgr *RWLockMgr) RLock(key string) {
	rwLockMgr.LocalLock.RLock()
	lock, ok := rwLockMgr.Locks[key]
	if !ok {
		rwLockMgr.LocalLock.RUnlock()
		rwLockMgr.LocalLock.Lock()
		lock, ok = rwLockMgr.Locks[key]
		if !ok {
			lock = recipe.NewRWMutex(rwLockMgr.Session, key)
			rwLockMgr.Locks[key] = lock
		}
		rwLockMgr.LocalLock.Unlock()
	} else {
		rwLockMgr.LocalLock.RUnlock()
	}
	err := lock.RLock()
	if err != nil {
		G.logger.Error("cannot lock rlock", zap.Error(err))
	}
}

func (rwLockMgr *RWLockMgr) RUnlock(key string) {
	rwLockMgr.LocalLock.RLock()
	defer rwLockMgr.LocalLock.RUnlock()
	lock, ok := rwLockMgr.Locks[key]
	if ok {
		err := lock.RUnlock()
		if err != nil {
			G.logger.Error("cannot unlock rlock", zap.Error(err))
		}
	}
}

func (rwLockMgr *RWLockMgr) Lock(key string) {
	rwLockMgr.LocalLock.RLock()
	lock, ok := rwLockMgr.Locks[key]
	if !ok {
		rwLockMgr.LocalLock.RUnlock()
		rwLockMgr.LocalLock.Lock()
		lock, ok = rwLockMgr.Locks[key]
		if !ok {
			lock = recipe.NewRWMutex(rwLockMgr.Session, key)
			rwLockMgr.Locks[key] = lock
		}
		rwLockMgr.LocalLock.Unlock()
	} else {
		rwLockMgr.LocalLock.RUnlock()
	}
	err := lock.Lock()
	if err != nil {
		G.logger.Error("cannot lock lock", zap.Error(err))
	}
}

func (rwLockMgr *RWLockMgr) Unlock(key string) {
	rwLockMgr.LocalLock.RLock()
	defer rwLockMgr.LocalLock.RUnlock()
	lock, ok := rwLockMgr.Locks[key]
	if ok {
		err := lock.Unlock()
		if err != nil {
			G.logger.Error("cannot unlock lock", zap.Error(err))
		}
	}
}
