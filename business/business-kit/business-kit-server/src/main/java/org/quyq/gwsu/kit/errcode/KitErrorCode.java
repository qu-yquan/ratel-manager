package org.quyq.gwsu.kit.errcode;


import org.quyq.gwsu.common.core.constants.ErrorCodeConstants;
import org.quyq.gwsu.common.core.domain.ReturnCode;
import org.quyq.gwsu.common.core.exception.errcode.ErrorCodeMeta;

/**
 * @author Quyq
 * @date 2026/6/5
 * @description
 */
@ErrorCodeMeta(moduleCode = ErrorCodeConstants.KIT_ERROR_CODE_MODULE, notes = "工具套件模块错误码")
public enum KitErrorCode implements ReturnCode {

    // 文件上传模块错误码
    E01001("该功能加班加点实现中..."),
    E01002("文件上传失败"),
    E01003("文件下载失败"),
    E01004("未找到分片数据，合并失败"),
    E01005("文件合并失败"),
    E01006("文件分片上传失败"),
    E01007("文件删除失败"),
    E01008("附件上传缺少配置，请联系管理员"),
    E01009("解析文件媒体类型失败"),
    E01010("禁止上传空文件"),
    E01011("文件后缀被串改"),
    E01012("该类型文件被禁止上传"),
    E01013("文件类型校验失败"),
    E01014("未识别出文件类型"),

    // 定时任务模块错误码
    E02001("任务不存在"),
    E02002("执行器不存在"),
    E02003("任务处理器未找到"),
    E02004("Cron表达式无效"),
    E02005("任务已运行中"),
    E02006("执行器地址为空"),
    E02007("任务参数无效"),
    E02008("调度类型无效"),
    E02009("调度过期策略无效"),
    E02010("阻塞处理策略无效"),
    E02011("路由策略无效"),
    E02012("Glue类型无效"),
    E02013("请输入任务名称"),
    E02014("请输入负责人"),
    E02015("请选择执行器"),
    E02016("该执行器下存在任务，无法删除"),
    E02017("AppName已存在"),
    E02018("手动录入地址不能为空"),
    E02019("子任务ID({0})不存在"),
    E02020("子任务ID({0})无效"),
    E02021("调度类型为空，不允许启动"),
    E02022("请输入AppName"),
    E02023("请输入执行器名称"),
    E02024("日志ID无效"),
    E02025("日志清理类型无效"),
    E02026("至少保留一个执行器"),

    // 知识库模块错误码
    E03001("知识源文档不存在"),
    E03002("知识导入任务不存在"),
    E03003("仅失败任务允许重试"),
    E03004("该知识源文档已有导入任务运行中"),
    E03005("配置无效"),
    E03006("知识源文档内容为空"),
    E03007("知识源文档解析失败"),
    E03008("知识Page生成失败"),
    E03009("知识Page构建参数不能为空"),
    E03010("知识文档版本不存在或不属于该文档"),
    E03011("知识Chunk索引操作失败"),
    E03012("pageBlockID不能为空"),

    ;

    private final String msg;

    KitErrorCode(String msg) {
        this.msg = msg;
    }

    @Override
    public String msg() {
        return msg;
    }
}
