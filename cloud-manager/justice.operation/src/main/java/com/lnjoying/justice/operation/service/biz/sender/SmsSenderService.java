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


package com.lnjoying.justice.operation.service.biz.sender;


import com.alibaba.fastjson.JSON;
import com.lnjoying.justice.operation.config.ApiConfig;
import com.lnjoying.justice.operation.config.NotifyTemplate;
import com.lnjoying.justice.operation.config.SmsConfig;
import com.lnjoying.justice.operation.entity.sender.BatchSmsRsp;
import com.lnjoying.justice.operation.service.SmsSender;
import com.lnjoying.justice.operation.utils.MD5Utils;
import com.micro.core.common.Utils;
import io.vertx.core.json.Json;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service("smsSender")
public class SmsSenderService implements SmsSender
{
	@Autowired
	SmsConfig smsConfig;
	@Autowired
	ApiConfig apiConfig;

	public BatchSmsRsp sendBatchSms(String content, String mobile, String template, String url)
	{
		Map<String, Object> params = setSmsParams(template, content, mobile, Utils.assignUUId());
		if (null == params)
		{
			return null;
		}
		return submitBatchMessage(params, url);
	}


	public BatchSmsRsp submitBatchMessage(Map<String, Object> sendParams, String url) {
		try {

			HttpClient httpClient = HttpClients.createDefault();
			HttpPost httpPost = new HttpPost(smsConfig.getUrl() + url);
			httpPost.addHeader("Content-Type", "application/json");
			httpPost.setEntity(new StringEntity(JSON.toJSONString(sendParams),"UTF-8"));
			HttpEntity entity = httpClient.execute(httpPost).getEntity();
			String result = EntityUtils.toString(entity);
			BatchSmsRsp smsRsp = Json.decodeValue(result, BatchSmsRsp.class);
			log.info("SMS response result:" + smsRsp.toString());
			return smsRsp;
		} catch (Exception e) {
//			e.printStackTrace();
			return null;
		}
	}

	private Map<String, Object>  setSmsParams(String template, String content, String mobile, String uid)
	{
		Map<String, Object> params = new HashMap<>();
		NotifyTemplate notifyTemplate = apiConfig.getNotifyTemplate().get(template);

		if (null == notifyTemplate)
		{
			log.error("template not exist, pls check {}", template);
			return null;
		}
		String templateId = notifyTemplate.getSms();

		setCommonParams(params);
		params.put("templateid", templateId);
		params.put("param", content);
		params.put("mobile", mobile);
		params.put("uid", uid);
		return params;
	}

	void setCommonParams(Map<String, Object> params)
	{
		params.put("clientid", smsConfig.getClientid());
		params.put("password", MD5Utils.getPWD(smsConfig.getPassword()));
	}


}
