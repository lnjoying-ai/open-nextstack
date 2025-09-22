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

package com.lnjoying.justice.repo.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.PostConstruct;
import java.io.FileReader;

@Configuration
public class RepoConfigYaml
{
        private static final Logger LOGGER = LogManager.getLogger();

        private RepoAgentConfig repoAgentConfig;

        @Bean("repoAgentConfig")
        public RepoAgentConfig getRepoAgentConfig()
        {
            return repoAgentConfig;
        }

        @PostConstruct
        void createRepoConfigBean()
        {
            loadRepoConfig();
        }

        private void loadRepoConfig()
        {
            String path = System.getProperty("lj_config") + "/repo.yaml";
            LOGGER.info("repo config path {}", path);

            try
            {
                Yaml yaml = new Yaml();
                FileReader fileReader = new FileReader(path);
                repoAgentConfig = yaml.loadAs(fileReader, RepoAgentConfig.class);
                fileReader.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

}
