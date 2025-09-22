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

package com.lnjoying.justice.operation.common.constant;

/**
 * @description: TODO   特殊符号枚举类
 * @author: LiSen
 * @date: 2023/5/25
 */

public enum ComparisonOperator {

    LT(0, "<"),
    LE(1, "<="),
    EQ(2, "=="),
    GE(3, ">="),
    GT(4, ">");

    private final int value;
    private final String symbol;

    ComparisonOperator(int value, String symbol) {
        this.value = value;
        this.symbol = symbol;
    }

    public static String getSymbolByValue(int value) {
        for (ComparisonOperator operator : ComparisonOperator.values()) {
            if (operator.getValue() == value) {
                return operator.getSymbol();
            }
        }
        return null;
    }

    public static int getValueBySymbol(String symbol) {
        for (ComparisonOperator operator : ComparisonOperator.values()) {
            if (operator.getSymbol().equals(symbol)) {
                return operator.getValue();
            }
        }
        throw new IllegalArgumentException("Invalid ComparisonOperator symbol: " + symbol);
    }


    public int getValue() {
        return value;
    }

    public String getSymbol() {
        return symbol;
    }


}
