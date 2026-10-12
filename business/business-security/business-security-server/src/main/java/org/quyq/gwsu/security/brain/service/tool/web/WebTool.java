package org.quyq.gwsu.security.brain.service.tool.web;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.ai.agui.model.AIRunnerInstanceWrapper;
import org.quyq.gwsu.common.ai.agui.utils.WebToolUtils;
import org.quyq.gwsu.common.ai.constants.AIConstants;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.*;

/**
 * Web端工具集合
 * 提供AI助手查看和操作Web界面的能力
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class WebTool {

    private final WebToolUtils webToolUtils;
    private final ObjectMapper objectMapper;
    private final WebPageApprovalStateService webPageApprovalStateService;
    private final ViewOperationCallerGuard viewOperationCallerGuard;


    // ==================== 操作模式 ====================

    @Tool(name = "ExitAiMode", description = """
            退出`AI操作模式`，将界面控制权交还给用户。
            当你完成所有界面操作后，必须调用此工具退出AI操作模式。""")
    @SuppressWarnings("unchecked")
    public Mono<ToolResultBlock> exitAiMode(Agent caller, RuntimeContext runtimeContext) {
        viewOperationCallerGuard.check(caller);
        AIRunnerInstanceWrapper wrapper = runtimeContext.get(AIRunnerInstanceWrapper.class);
        return Mono.just(webToolUtils.webExecuteTool(wrapper, "ExitAiMode", Map.of()));

    }

    // ==================== 路由导航 ====================

    @Tool(name = "RouteNavigation", description = """
            控制web界面跳转到指定路由
            工具使用前提：界面操作模式必须是`AI操作模式`。
            """)
    @SuppressWarnings("unchecked")
    public Mono<ToolResultBlock> routeNavigation(@ToolParam(name = "path",
            description = """
                    跳转的前端路由地址
                    示例：/sub-system/user
                    """) String path, Agent caller, RuntimeContext runtimeContext) {

        viewOperationCallerGuard.check(caller);

        AIRunnerInstanceWrapper wrapper = runtimeContext.get(AIRunnerInstanceWrapper.class);
        return Mono.just(webToolUtils
                        .webExecuteTool(wrapper, "RouteNavigation", Map.of("path", path)));

    }

    // ==================== 查看界面 ====================

    @Tool(name = "GetPageState", description = """
            获取当前Web界面的状态信息，返回页面中可见的交互元素列表和页面基本信息。
            返回内容为JSON字符串，其中包含简化的HTML文本和审批索引。
            
            元素格式说明：
            - [index]{tags}元素： []标识的内容为元素索引编号，用该编号定位操作元素,必有 ，{}包裹的为元素标签，用户对元素的额外功能标注，多个,分割，由前端生成，可能不包含
              示例：  普通元素：[0]<button>提交</button>  带标签的元素：[0]{approval}<button>提交</button>
            - approval：需要人工审批的元素索引数组，例如 [142]，没有则 []
            """)
    public Mono<ToolResultBlock> getPageState(Agent caller, RuntimeContext runtimeContext) {
        viewOperationCallerGuard.check(caller);
        return Mono.deferContextual(contextView -> {
            AIRunnerInstanceWrapper wrapper = runtimeContext.get(AIRunnerInstanceWrapper.class);
            ToolResultBlock result = webToolUtils.webExecuteTool(wrapper, "GetPageState", Map.of());
            syncPageState(runtimeContext, result);
            return Mono.just(result);
        });
    }

    // ==================== 操作界面 ====================

    @Tool(name = "InputText", description = """
            在指定索引的输入框中输入文本。会先清空现有内容再输入新文本，兼容React受控组件。
            需要先调用GetPageState获取输入框的元素索引。
            工具使用前提：界面操作模式必须是`AI操作模式`。
            参数：index - 输入框元素索引编号，text - 要输入的文本内容""")
    public Mono<ToolResultBlock> inputText(
            @ToolParam(name = "index", description = "输入框元素索引编号") Integer index,
            @ToolParam(name = "text", description = "要输入的文本内容") String text,
            Agent caller,
            RuntimeContext runtimeContext) {
        viewOperationCallerGuard.check(caller);
        AIRunnerInstanceWrapper wrapper = runtimeContext.get(AIRunnerInstanceWrapper.class);
        return Mono.just(webToolUtils.webExecuteTool(wrapper, "InputText", Map.of("index", index, "text", text)));
    }

    @Tool(name = "SelectOption", description = """
            在指定索引的下拉选择框中选择选项。支持原生select元素和Ant Design Select组件。
            需要先调用GetPageState获取select元素的索引。
            工具使用前提：界面操作模式必须是`AI操作模式`。
            参数：index - select元素索引编号，text - 要选择的选项文本""")
    public Mono<ToolResultBlock> selectOption(
            @ToolParam(name = "index", description = "select元素索引编号") Integer index,
            @ToolParam(name = "text", description = "要选择的选项文本") String text,
            Agent caller,
            RuntimeContext runtimeContext) {
        viewOperationCallerGuard.check(caller);
        AIRunnerInstanceWrapper wrapper = runtimeContext.get(AIRunnerInstanceWrapper.class);
        return Mono.just(webToolUtils.webExecuteTool(wrapper, "SelectOption", Map.of("index", index, "text", text)));
    }

    @Tool(name = "HoverElement", description = """
            将鼠标悬停在指定索引的元素上，触发hover效果使隐藏的交互元素显示出来。
            使用场景：某些按钮或操作入口仅在鼠标悬停时才显示，例如：
            - 表格行悬停后出现的编辑/删除按钮
            - 下拉选择框悬停后出现的清除/关闭按钮
            - 卡片悬停后出现的操作按钮
            需要先调用GetPageState获取元素索引。
            工具使用前提：界面操作模式必须是`AI操作模式`。
            悬停后请调用GetPageState查看新出现的交互元素。
            参数：index - 要悬停的元素索引编号""")
    public Mono<ToolResultBlock> hoverElement(
            @ToolParam(name = "index", description = "要悬停的元素索引编号，从GetPageState结果中获取") Integer index,
            Agent caller,
            RuntimeContext runtimeContext) {
        viewOperationCallerGuard.check(caller);
        AIRunnerInstanceWrapper wrapper = runtimeContext.get(AIRunnerInstanceWrapper.class);
        return Mono.just(webToolUtils.webExecuteTool(wrapper, "HoverElement", Map.of("index", index)));
    }

    @Tool(name = "ScrollPage", description = """
            滚动页面或页面内的可滚动元素。
            工具使用前提：界面操作模式必须是`AI操作模式`。
            参数：direction - 滚动方向(up/down/left/right)，amount - 滚动量(1=一页，像素值=精确滚动)""")
    public Mono<ToolResultBlock> scrollPage(
            @ToolParam(name = "direction", description = "滚动方向：up/down/left/right") String direction,
            @ToolParam(name = "amount", description = "滚动量：1表示一页，数字像素值表示精确滚动") Integer amount,
            Agent caller,
            RuntimeContext runtimeContext) {
        viewOperationCallerGuard.check(caller);
        AIRunnerInstanceWrapper wrapper = runtimeContext.get(AIRunnerInstanceWrapper.class);
        return Mono.just(webToolUtils.webExecuteTool(wrapper, "ScrollPage",
                Map.of("direction", direction, "amount", amount)));
    }

    @Tool(name = "AttachUploadedFile", description = """
            将当前会话中用户已上传的文件挂载到页面上的文件上传控件。
            使用步骤：
            1. 先调用GetPageState，找到带有 data-ai-upload-id 属性的上传控件元素；
            2. 从系统提示词中的<fileInfos>里选择要上传的 fileId；
            3. 调用本工具，将 fileId 挂载到对应 uploadId 的上传控件。
            
            注意：
            - uploadId 必须来自当前页面 GetPageState 返回结果里的 data-ai-upload-id 属性；
            - fileId 必须来自当前系统提示词提供的 <fileInfos> 列表；
            - 工具会根据 uploadId 找到页面上传控件，并按该控件配置复制文件后回填；
            - 工具使用前提：界面操作模式必须是`AI操作模式`。
            """)
    public Mono<ToolResultBlock> attachUploadedFile(
            @ToolParam(name = "uploadId", description = "上传控件标识，必须来自GetPageState结果中的data-ai-upload-id属性") String uploadId,
            @ToolParam(name = "fileId", description = "要挂载的文件ID，必须来自系统提示词<fileInfos>中的fileId") String fileId,
            Agent caller,
            RuntimeContext runtimeContext) {
        viewOperationCallerGuard.check(caller);
        AIRunnerInstanceWrapper wrapper = runtimeContext.get(AIRunnerInstanceWrapper.class);
        return Mono.just(webToolUtils.webExecuteTool(wrapper, "AttachUploadedFile",
                Map.of("uploadId", uploadId, "fileId", fileId)));
    }

    private void syncPageState(RuntimeContext runtimeContext, ToolResultBlock result) {
        Set<Integer> approvalIndexes = extractApprovalIndexes(result);
        webPageApprovalStateService.save(runtimeContext, approvalIndexes);
    }

    private Set<Integer> extractApprovalIndexes(ToolResultBlock result) {
        String payload = result.getOutput().stream()
                .filter(TextBlock.class::isInstance)
                .map(TextBlock.class::cast)
                .map(TextBlock::getText)
                .findFirst()
                .orElse("");
        if (!StringUtils.hasText(payload)) {
            return Collections.emptySet();
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode approvalNode = root.path("approval");
            if (!approvalNode.isArray()) {
                return Collections.emptySet();
            }

            Set<Integer> approvalIndexes = new LinkedHashSet<>();
            approvalNode.forEach(item -> {
                if (item.canConvertToInt()) {
                    approvalIndexes.add(item.intValue());
                }
            });
            return Collections.unmodifiableSet(approvalIndexes);
        } catch (Exception ex) {
            log.warn("解析 GetPageState 审批索引失败", ex);
            return Collections.emptySet();
        }
    }
}
