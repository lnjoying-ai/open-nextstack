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

package com.lnjoying.justice.network.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lnjoying.justice.commonweb.aspect.LogAnnotation;
import com.lnjoying.justice.network.entity.Eip;
import com.lnjoying.justice.network.service.EipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author george
 * @since 2023-01-04
 */
@RestController
@RequestMapping("/eip")
public class EipController {


    @Autowired
    private EipService eipService;

    @GetMapping(value = "/")
    public ResponseEntity<Page<Eip>> list(@RequestParam(required = false) Integer current, @RequestParam(required = false) Integer pageSize) {
        if (current == null) {
            current = 1;
        }
        if (pageSize == null) {
            pageSize = 10;
        }
        Page<Eip> aPage = eipService.page(new Page<>(current, pageSize));
        return new ResponseEntity<>(aPage, HttpStatus.OK);
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<Eip> getById(@PathVariable("id") String id) {
        return new ResponseEntity<>(eipService.getById(id), HttpStatus.OK);
    }

    @LogAnnotation(resource = "网络-EIP",description = "创建EIP【ip地址：{}】", obtainParameter = "ipaddr")
    @PostMapping(value = "/create")
    public ResponseEntity<Object> create(@RequestBody Eip params) {
        eipService.save(params);
        return new ResponseEntity<>("created successfully", HttpStatus.OK);
    }

    @LogAnnotation(resource = "网络-EIP",description = "删除EIP【id：{}】", obtainParameter = "id")
    @PostMapping(value = "/delete/{id}")
    public ResponseEntity<Object> delete(@PathVariable("id") String id) {
        eipService.removeById(id);
        return new ResponseEntity<>("deleted successfully", HttpStatus.OK);
    }

    @LogAnnotation(resource = "网络-EIP",description = "修改EIP【ip地址：{}】", obtainParameter = "ipaddr")
    @PostMapping(value = "/update")
    public ResponseEntity<Object> update(@RequestBody Eip params) {
        eipService.updateById(params);
        return new ResponseEntity<>("updated successfully", HttpStatus.OK);
    }
}
