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

package com.lnjoying.justice.usermanager.config;

public class DescriptionConfig
{
    private DescriptionConfig() {}
    //auth
    public static final String DELETE_TOKEN_MSG         = "The API can receive the delete token request, it will return nothing.";

    //bp
    public static final String ADD_BP_MSG               = "The API can receive the add bp request.";
    public static final String DELETE_BP_MSG            = "The API can receive the delete bp request.";
    public static final String UPDATE_BP_MSG            = "The API can receive the update bp request.";
    public static final String BP_INFO_MSG              = "The API can receive the get bp info request.";
    public static final String BP_LIST_MSG              = "The API can receive the get bp list request.";

    //role
    public static final String ADD_USER_ROLE_MSG        = "The API can receive the add roles by userId request.";
    public static final String GET_USER_ROLE_MSG        = "The API can receive the get roles by userId request.";
    public static final String ROLE_LIST_MSG            = "The API can receive the get role list request.";

    //user
    public static final String REGISTER_MSG             = "The API can receive register user request. If register success, it will "
        + "return status 201. If username or telephone has existed, or verification code is error, it will reutrn "
        + "status 403. If database connection has exception, it will return status 500. If register failed, it "
        + "will return status 400.";
    public static final String ADMIN_ADD_USER_MSG       = "The API can receive the admin add user request.";
    public static final String RETRIEVE_PASSWORD_MSG    = "The API can receive the retrieve password request. If retrieve "
        + "success, it will return status 200. If telephone do not exist or the verification code is error, it "
        + "will return status 403. If database connection has exception, it will return status 500. If retrieve "
        + "failed, it will return status 400.";
    public static final String UPDATE_PASSWORD_MSG      = "The API can receive the update user password request.";
    public static final String UNIQUENESS_MSG = "The API can receive the unique verify request for mailAddress or "
        + "telephone or username. If the request param is unique, it will return status 200, "
        + "otherwise it will return status 400.";
    public static final String DELETE_USER_MSG = "The API can receive the delete user by user id request.";
    public static final String UPDATE_USER_BY_ID_MSG   = "The API can receive the update user by user id request.";
    public static final String UPDATE_CURRENT_USER_MSG = "The API can receive the update current user request.";
    public static final String CURRENT_USER_MSG        = "The API can receive the get current user info request.";
    public static final String USER_LIST_MSG           = "The API can receive the get user list request.";
    public static final String UPDATE_USER_PHONE_MSG   = "The API can receive the update user phone request.";
    public static final String UPDATE_USER_EMAIL_MSG   = "The API can receive the update user email request.";
    public static final String SET_USER_API_KEY = "The API can receive the user api key request.";
    //verification
    public static final String VERIFICATION_SMS_MSG    = "The API can receive the send verification code to phone request." +
            " If send verification code success, it will return status 200. If send verification code error, it will" +
            " return status 417.";
    public static final String VERIFICATION_EMAIL_MSG  = "The API can receive the send verification code to email request." +
            " If send verification code success, it will return status 200. If send verification code error, it will" +
            " return status 417.";
    public static final String SMS_REGISTRATION_MSG    = "The API can receive the sms registration code request. If send "
            + "verification code success, it will return status 200. If send verification code error, it will return "
            + "status 417.";
    public static final String EMAIL_REGISTRATION_MSG  = "The API can receive the email registration request. If send "
            + "verification code success, it will return status 200. If send verification code error, it will return "
            + "status 417.";

}
