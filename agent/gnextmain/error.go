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

type CmdError struct {
	Err error
	Cmd string
}

func (e *CmdError) Error() string {
	return "CMD: " + e.Cmd + " -- " + e.Err.Error()
}

type FileError struct {
	Err  error
	File string
}

func (e *FileError) Error() string {
	return "FILE: " + e.File + " -- " + e.Err.Error()
}

type EtcdError struct {
	Err error
	Key string
}

func (e *EtcdError) Error() string {
	return "ETCD: " + e.Key + " -- " + e.Err.Error()
}

type InputError struct {
	Err   error
	Input string
}

func (e *InputError) Error() string {
	return "INPUT: " + e.Input + " -- " + e.Err.Error()
}
