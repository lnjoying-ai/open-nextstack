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

package com.lnjoying.justice.schema.common;

public interface RedisCacheField
{
    ////////////////////main key////////////////////
    String AUTH_VER_CODE                   = "auth_verification_code";
    String PATCH_VER_CODE                  = "patch_verification_code";

    String REG_VER_CODE                    =   "reg_verification_code";
    String ACCESS_TOKEN_EXPIRE             =     "access_token_expire";

    String REGION                          =                  "REGION";
    String SITE                            =                    "SITE";

    String SESSION                         =                 "SESSION";
    String LIFE                            =                    "LIFE";

    ////////////////////stat////////////////////
    String STAT                            =                    "STAT";

    ////////////////////scheduler////////////////////
    String SCHED                           =                   "SCHED";
    String EDGE                            =                    "EDGE";
    String TEMP                            =                    "TEMP";
    String RESOURCES                       =               "RESOURCES";
    String ONLINE                          =                  "ONLINE";
    String LABEL                           =                   "LABEL";
    String TAINT                           =                   "TAINT";

    ////////////////////ecrm////////////////////
    String ECRM                            =                    "ECRM";
    String FTP                             =                     "FTP";
    String AGENT                           =                   "AGENT";
    String GW                              =                      "GW";
    String IMAGE                           =                   "IMAGE";
    String CLOUD                           =                   "CLOUD";
    String RESTPORT                        =                "RESTPORT";
    String LOCALPORT                       =               "LOCALPORT";
    String INSTALLFILE                     =             "INSTALLFILE";
    String EHOSTOPERATOR                   =           "EHOSTOPERATOR";

    ////////////////////api////////////////////
    String API                             =                     "API";
    String IF                              =                      "IF";

    //key pattern                              description                   set               use         delete
    //----------------------------------------------------ONLINE--------------------------------------------------------
    //EDGE:ONLINE                              all online edge set.          api\ecrm          shced
    //EDGE:ONLINE:REGION:{region_id}           region's online edge set.     api\ecrm          shced       ecrm
    //EDGE:ONLINE:SITE:{site_id}               site's online edge set.       api\ecrm          shced       ecrm
    //-----------------------------------------------------LABEL--------------------------------------------------------
    //LABEL:REGION:{label_key}:{label_value}   label region set.             ecrm              shced       ecrm
    //LABEL:REGION:{label_key}                 label region set.             ecrm              shced       ecrm
    //LABEL:SITE:{label_key}:{label_value}     label site set.               ecrm              shced       ecrm
    //LABEL:SITE:{label_key}                   label site set.               ecrm              shced       ecrm
    //LABEL:EDGE:{label_key}:{label_value}     label edge set.               ecrm              shced       ecrm
    //LABEL:EDGE:{label_key}                   label edge set.               ecrm              shced       ecrm
    //LABEL:REGION                             region label set.             ecrm              shced
    //LABEL:SITE                               site label set.               ecrm              shced
    //LABEL:EDGE                               edge label set.               ecrm              shced
    //----------------------------------------------------SCHED---------------------------------------------------------
    //SCHED:EDGE:RESOURCES:{node_id}           edge remain resources.        shced             shced       shced
    //SCHED:REGION                             all region set.               shced\ecrm        shced
    //SCHED:SITE                               all site set.                 shced\ecrm        shced
    //SCHED:SITE:REGION:{region_id}            region's site set.            shced\ecrm        shced       ecrm
    //SCHED:TEMP:REGION:{ref_id}               shceduler middle result.      shced             shced       shced
    //SCHED:TEMP:SITE:{ref_id}                 shceduler middle result.      shced             shced       shced
    //SCHED:TEMP:EDGE:{ref_id}                 shceduler middle result.      shced             shced       shced
    //SCHED:TEMP:{ref_id}                      shceduler middle result.      shced             shced       shced
    //-----------------------------------------------------ECRM---------------------------------------------------------
    //ECRM:FTP                                 ftp address.                  manual            ecrm
    //ECRM:INSTALLFILE                         install shell file name.      manual            ecrm
    //ECRM:AGENT:IMAGE                         edge agent image name.        manual            ecrm
    //ECRM:AGENT:RESTPORT                      edge agent rest port.         manual            ecrm
    //ECRM:GW:IMAGE                            edge gw image name.           manual            ecrm
    //ECRM:GW:RESTPORT                         edge gw rest port.            manual            ecrm
    //ECRM:GW:LOCALPORT                        edge gw local port.           manual            ecrm
    //ECRM:GW:CLOUD                            edge gw cloud url.            manual            ecrm
    //ECRM:SITE:{region_id}:{site_id}:{gw_id}                                api               ecrm
    //ECRM:EDGE:IF:{node_id}                   edge if.
    //ECRM:GW:IF:{gw_id}                       gw if.
    //ECRM:EHOSTOPERATOR:SESSION:{session_id}  ehost_operator session.       ecrm              ecrm         auto
    //-----------------------------------------------------CIS----------------------------------------------------------
    //
    //-----------------------------------------------------AOS----------------------------------------------------------
    //
    //-----------------------------------------------------API----------------------------------------------------------
    //
    //----------------------------------------------------OTHER---------------------------------------------------------
    //
}
