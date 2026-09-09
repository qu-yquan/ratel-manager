package org.quyq.gwsu.security.apiresource.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.common.security.annotation.TableModelPermission;
import org.quyq.gwsu.security.api.apiresource.dto.ApiResourceQueryByTableModelDTO;
import org.quyq.gwsu.security.api.apiresource.dto.ApiResourceQueryDTO;
import org.quyq.gwsu.security.api.apiresource.vo.ApiResourceVO;
import org.quyq.gwsu.security.apiresource.domain.SecurityApiResource;
import org.quyq.gwsu.security.apiresource.service.ISecurityApiResourceService;
import org.quyq.gwsu.security.errcode.SecurityErrorCode;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 接口资源控制器
 *
 * @author Quyq
 */
@RestController
@RequestMapping("apiResource")
@Tag(name = "接口资源管理", description = "接口资源管理接口")
@TableModelPermission({SecurityApiResource.class})
@RequiredArgsConstructor
public class SecurityApiResourceController {

    private final ISecurityApiResourceService apiResourceService;

    @Operation(summary = "分页查询")
    @PostMapping("page")
    public R<IPage<ApiResourceVO>> page(@RequestBody ApiResourceQueryDTO query) {
        return R.ok(apiResourceService.pageByCondition(query));
    }


    @Operation(summary = "通过服务名获取数据源列表")
    @GetMapping("getDatasourceList")
    public R<List<String>> getDatasourceList(@RequestParam String serverName) {
        return R.ok(apiResourceService.getAllDatasource(serverName));
    }

    @Operation(summary = "获取指定表模型关联的api接口资源")
    @PostMapping("listByTableModel")
    public R<List<ApiResourceVO>> selectByTableModel(@RequestBody ApiResourceQueryByTableModelDTO queryDTO) {
        AssertUtils.hasText(queryDTO.modulePrefix(), SecurityErrorCode.E03004);
        AssertUtils.hasText(queryDTO.datasource(), SecurityErrorCode.E03002);
        AssertUtils.hasText(queryDTO.tableName(), SecurityErrorCode.E03001);
        return R.ok(apiResourceService.selectByTableModel(queryDTO));

    }

    @Operation(summary = "根据ID查询")
    @GetMapping("{id}")
    public R<ApiResourceVO> getById(@PathVariable Long id) {
        return R.ok(apiResourceService.getById(id));
    }

    @Operation(summary = "根据模块前缀查询列表")
    @GetMapping("list/by-module/{modulePrefix}")
    public R<List<ApiResourceVO>> listByModulePrefix(@PathVariable String modulePrefix) {
        return R.ok(apiResourceService.listByModulePrefix(modulePrefix));
    }

    @Operation(summary = "根据Tag名称查询列表")
    @GetMapping("list/by-tag/{tagName}")
    public R<List<ApiResourceVO>> listByTagName(@PathVariable String tagName) {
        return R.ok(apiResourceService.listByTagName(tagName));
    }

    @Operation(summary = "根据ID批量查询接口资源")
    @PostMapping("list/by-ids")
    public R<List<ApiResourceVO>> listByIds(@RequestBody List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return R.ok(List.of());
        }
        return R.ok(apiResourceService.listByIds(ids).stream()
                .map(SecurityApiResource::toVo)
                .toList());
    }

    @Operation(summary = "新增或更新")
    @PostMapping
    public R<Boolean> saveOrUpdate(@RequestBody SecurityApiResource entity) {
        return R.ok(apiResourceService.saveOrUpdate(entity));
    }

    @Operation(summary = "批量删除")
    @DeleteMapping
    public R<Boolean> remove(@RequestBody List<Long> ids) {
        return R.ok(apiResourceService.removeByIds(ids));
    }
}
