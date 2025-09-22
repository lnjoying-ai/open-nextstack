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

package com.lnjoying.justice.schema.entity.dev;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SchedulingStrategy implements Serializable
{
    private List<LabelSelector> label_selectors;

    private boolean replica_complete_strategy;

    private Integer on_one_node;

    private List<TargetNode> target_nodes;

    //Later version extend, default is null
    private String strategy;
}
