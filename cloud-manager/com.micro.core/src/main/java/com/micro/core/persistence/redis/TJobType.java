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

package com.micro.core.persistence.redis;

import java.io.Serializable;

public enum TJobType implements Serializable
{
    OFFSET(100, "offset"),
    RENDER(0, "render"),
    SIMULATION(1, "simulation"),
    VARIOUS(20, "various"),
    MRENDER(100, "render"),
    MSIMULATION(101,"simulation");

    private final int value;
    /** Error message */
    private String name;

    private TJobType(int value, String message)
    {
        this.value = value;
        this.name = message;
    }

    public static TJobType valueOf(int value)
    {
        for (TJobType current : TJobType.values()) {
            if (current.getValue() == value) {
                return current;
            }
        }
        throw new RuntimeException("Invalid error status: " + value);
    }

    public int getValue()
    {
        return value;
    }

    public String getName()
    {
        return name;
    }
}

