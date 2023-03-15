/*
 * Copyright 2016-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.center.auth.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.auth.entity.query.TenantBindPageQuery;
import io.github.pnoker.center.auth.mapper.TenantBindMapper;
import io.github.pnoker.center.auth.service.TenantBindService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.model.TenantBind;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * TenantBindService Impl
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class TenantBindServiceImpl implements TenantBindService {

    @Resource
    private TenantBindMapper tenantBindMapper;

    @Override
    public TenantBind add(TenantBind tenantBind) {
        if (tenantBindMapper.insert(tenantBind) > 0) {
            return tenantBindMapper.selectById(tenantBind.getId());
        }
        throw new ServiceException("The tenant bind add failed");
    }

    @Override
    public Boolean delete(String id) {
        selectById(id);
        return tenantBindMapper.deleteById(id) > 0;
    }

    @Override
    public TenantBind update(TenantBind tenantBind) {
        selectById(tenantBind.getId());
        tenantBind.setUpdateTime(null);
        if (tenantBindMapper.updateById(tenantBind) > 0) {
            return tenantBindMapper.selectById(tenantBind.getId());
        }
        throw new ServiceException("The tenant bind update failed");
    }

    @Override
    public TenantBind selectById(String id) {
        TenantBind tenantBind = tenantBindMapper.selectById(id);
        if (ObjectUtil.isNull(tenantBind)) {
            throw new NotFoundException();
        }
        return tenantBind;
    }

    @Override
    public TenantBind selectByTenantIdAndUserId(String tenantId, String userId) {
        LambdaQueryWrapper<TenantBind> queryWrapper = Wrappers.<TenantBind>query().lambda();
        queryWrapper.eq(TenantBind::getTenantId, tenantId);
        queryWrapper.eq(TenantBind::getUserId, userId);
        queryWrapper.last("limit 1");
        return tenantBindMapper.selectOne(queryWrapper);
    }

    @Override
    public Page<TenantBind> list(TenantBindPageQuery tenantBindPageQuery) {
        if (ObjectUtil.isNull(tenantBindPageQuery.getPage())) {
            tenantBindPageQuery.setPage(new Pages());
        }
        return tenantBindMapper.selectPage(tenantBindPageQuery.getPage().convert(), fuzzyQuery(tenantBindPageQuery));
    }

    @Override
    public LambdaQueryWrapper<TenantBind> fuzzyQuery(TenantBindPageQuery tenantBindPageQuery) {
        LambdaQueryWrapper<TenantBind> queryWrapper = Wrappers.<TenantBind>query().lambda();
        if (ObjectUtil.isNotNull(tenantBindPageQuery)) {
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(tenantBindPageQuery.getTenantId()), TenantBind::getTenantId, tenantBindPageQuery.getTenantId());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(tenantBindPageQuery.getUserId()), TenantBind::getUserId, tenantBindPageQuery.getUserId());
        }
        return queryWrapper;
    }

}
