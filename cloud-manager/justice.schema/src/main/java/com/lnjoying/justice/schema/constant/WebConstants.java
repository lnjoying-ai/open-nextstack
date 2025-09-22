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

package com.lnjoying.justice.schema.constant;

public interface WebConstants
{
    /** Header that holds SiteWhere error string on error response */
    String HEADER_LNJOYING_ERROR = "X-Lnjoying-Error";

    String HEADER_LNJOYING_ERROR_LEVEL = "X-Lnjoying-Error-Level";

    /** Header that holds SiteWhere error code on error response */
    String HEADER_LNJOYING_ERROR_CODE = "X-Lnjoying-Error-Code";

    //the period of validity for lnjoying token. 15min
    int  LNJOYING_TOKEN_INDATE = 90000;
    int  LNJOYING_VRFCTOKEN_INDATE = 120;
    String  ACCESS_TOKEN_NAME = "Access-Token";
    String  HEADER_ACCESS_TOKEN_NAME = "X-Access-Token";
    String  LNJOYING_VRFC = "vrfc";
}
