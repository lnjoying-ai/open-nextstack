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

package com.lnjoying.justice.network.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.PostConstruct;
import java.io.FileReader;

@Configuration
@Slf4j
public class NetworkConfigYaml
{
//        private static final Logger LOGGER = LogManager.getLogger();

        private NetworkAgentConfig networkAgentConfig;

        @Bean("networkAgentConfig")
        public NetworkAgentConfig getNetworkAgentConfig()
        {
            return networkAgentConfig;
        }

        @PostConstruct
        void createNetworkConfigBean()
        {
            loadNetworkConfig();
        }

        private void loadNetworkConfig()
        {
            String path = System.getProperty("lj_config") + "/network.yaml";
            log.info("network config path {}", path);

            try
            {
                Yaml yaml = new Yaml();
                FileReader fileReader = new FileReader(path);
                networkAgentConfig = yaml.loadAs(fileReader, NetworkAgentConfig.class);
                fileReader.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

}
