/*
 * Copyright 2017 ~ 2025 the original author or authors. <wanglsir@gmail.com, 983708408@qq.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wl4g.devops.ci.web;

import com.wl4g.devops.ci.service.VcsService;
import com.wl4g.devops.common.bean.ci.Vcs;
import com.wl4g.devops.common.web.BaseController;
import com.wl4g.devops.common.web.RespBase;
import com.wl4g.devops.page.PageModel;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.apache.shiro.authz.annotation.Logical.AND;

/**
 * @author vjay
 * @date 2019-11-12 11:03:00
 */
@RestController
@RequestMapping("/vcs")
public class VcsController extends BaseController {

	@Autowired
	private VcsService vcsService;

	@RequestMapping("/list")
	@RequiresPermissions(value = {"ci","ci:vcs"},logical = AND)
	public RespBase<?> list(PageModel pm, String name, String providerKind, Integer authType) {
		RespBase<Object> resp = RespBase.create();
		resp.setData(vcsService.list(pm, name, providerKind, authType));
		return resp;
	}

	@RequestMapping("/save")
	@RequiresPermissions(value = {"ci","ci:vcs"},logical = AND)
	public RespBase<?> save(Vcs vcs) {
		RespBase<Object> resp = RespBase.create();
		vcsService.save(vcs);
		return resp;
	}

	@RequestMapping("/del")
	@RequiresPermissions(value = {"ci","ci:vcs"},logical = AND)
	public RespBase<?> del(Integer id) {
		RespBase<Object> resp = RespBase.create();
		vcsService.del(id);
		return resp;
	}

	@RequestMapping("/detail")
	@RequiresPermissions(value = {"ci","ci:vcs"},logical = AND)
	public RespBase<?> detail(Integer id) {
		RespBase<Object> resp = RespBase.create();
		Vcs vcs = vcsService.detail(id);
		resp.setData(vcs);
		return resp;
	}

	@RequestMapping("/all")
	public RespBase<?> all() {
		RespBase<Object> resp = RespBase.create();
		resp.setData(vcsService.all());
		return resp;
	}

}