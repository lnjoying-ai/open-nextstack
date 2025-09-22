// Copyright 2024 The NEXTSTACK Authors.
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

package com.lnjoying.justice.schema.msg;

import lombok.Data;

import java.io.Serializable;

@Data
public class EdgeMessage implements Serializable
{
    String nodeId;  //(1) cloud->gw->edge，fill edge nodeId;(2) cloud->gw, fill gw nodeId (3) gw->cloud, fill gw nodeId
    byte[] netMessage;
}
