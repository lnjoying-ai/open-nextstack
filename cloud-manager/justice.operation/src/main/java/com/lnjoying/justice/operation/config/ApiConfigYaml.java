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

package com.lnjoying.justice.operation.config;

//import com.esotericsoftware.yamlbeans.YamlException;
//import com.esotericsoftware.yamlbeans.YamlReader;
//import com.esotericsoftware.yamlbeans.YamlWriter;

import com.lnjoying.justice.operation.utils.CipherUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.PostConstruct;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

@Configuration
public class ApiConfigYaml
{
    ApiConfig apiConfig = null;
    ContactAdmin contactAdmin = null;
    NodeConfig nodeConfig = null;

    @PostConstruct
    void createRenderServiceParamBean()
    {
        loadApiYaml();
        loadContactYaml();
        loadNodeConfigYaml();
    }

    void loadApiYaml()
    {
        String path = System.getProperty("lj_config") + "/" + "api.yaml";
        Yaml yaml = new Yaml();

        try
        {
            FileReader reader = new FileReader(path);
            apiConfig = yaml.loadAs(reader, ApiConfig.class);
			reader.close();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e) {
			e.printStackTrace();
		}
	}

    void loadContactYaml()
    {
        String path = System.getProperty("lj_config") + "/" + "contact.yaml";
        Yaml yaml = new Yaml();

        try
        {
            FileReader reader = new FileReader(path);
            contactAdmin = yaml.loadAs(reader, ContactAdmin.class);
            reader.close();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e) {
			e.printStackTrace();
		}
	}

    void loadNodeConfigYaml()
    {
        String path = System.getProperty("lj_config") + "/" + "node_config.yaml";
        Yaml yaml = new Yaml();

        try
        {
            FileReader reader = new FileReader(path);
            nodeConfig = yaml.loadAs(reader, NodeConfig.class);
            if (nodeConfig != null && nodeConfig.getPrivate_key() != null && nodeConfig.getNode_id() != null)
            {
                CipherUtil.import_private_key(nodeConfig.getPrivate_key());
            }
            reader.close();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
			if (nodeConfig != null) nodeConfig.reset();
        }
        catch (IllegalArgumentException e)
		{
			e.printStackTrace();
            if (nodeConfig != null) nodeConfig.reset();
        }
        catch (IOException e)
        {
            if (nodeConfig != null) nodeConfig.reset();
        }

		if (nodeConfig == null || nodeConfig.getPrivate_key() == null && nodeConfig.getNode_id() == null
                || nodeConfig.getPrivate_key().isEmpty() || nodeConfig.getNode_id().isEmpty())
        {
            nodeConfig  = CipherUtil.gen_node();
            try
            {
                FileWriter writer = new FileWriter(path);
                yaml.dump(nodeConfig, writer);
                writer.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    @Bean("apiConfig")
    public ApiConfig createApiConfig()
    {
        return apiConfig;
    }

    @Bean("contactAdmin")
    public ContactAdmin createContactAdminConfig()
    {
        return contactAdmin;
    }

    @Bean("nodeConfig")
    public NodeConfig createNodeConfig()
    {
        return nodeConfig;
    }
}
