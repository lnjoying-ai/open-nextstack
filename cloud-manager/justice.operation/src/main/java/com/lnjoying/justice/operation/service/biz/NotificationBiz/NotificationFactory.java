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

package com.lnjoying.justice.operation.service.biz.NotificationBiz;

import com.lnjoying.justice.operation.service.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NotificationFactory {

    @Autowired
    private EmailNotification emailNotification;

    @Autowired
    private SmsNotification smsNotification;

    @Autowired
    private PhoneNotification phoneNotification;


    /**
     * @param: notificationType
     * @description: TODO   工厂类分发
     * @return: com.lnjoying.justice.operation.service.Notification
     * @author: LiSen
     * @date: 2023/6/2
     */
    public Notification createNotificationService(int notificationType) {
        switch (notificationType) {
            case 0:
                return emailNotification;
            case 1:
                return smsNotification;
            case 2:
                return phoneNotification;
            default:
                throw new IllegalArgumentException("Invalid notification type: " + notificationType);
        }
    }


}
