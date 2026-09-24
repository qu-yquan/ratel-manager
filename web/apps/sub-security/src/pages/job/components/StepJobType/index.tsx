import React, { useState, useEffect } from 'react';
import { Form, Select, Input } from 'antd';
import type { JobMode } from '../../types';
import { JOB_MODE_OPTIONS, GLUE_TYPE_OPTIONS } from '../../types';
import { getHandlerList, getModuleList } from '../../services/job';
import type { ModuleInfo } from '../../types';
import { getApiResourcePage } from '../../../menu/services/apiResource';
import type { ApiResourceItem } from '../../../menu/types';
import GlueCodeEditor from '../GlueCodeEditor';
import { DEFAULT_GLUE_INIT_REMARK, getDefaultGlueSource } from '../../glue';

interface StepJobTypeProps {
  jobMode: JobMode;
  onJobModeChange: (mode: JobMode) => void;
}

const StepJobType: React.FC<StepJobTypeProps> = ({ jobMode, onJobModeChange }) => {
  const form = Form.useFormInstance();
  const [modules, setModules] = useState<ModuleInfo[]>([]);
  const [handlers, setHandlers] = useState<string[]>([]);
  const [postApis, setPostApis] = useState<ApiResourceItem[]>([]);
  const selectedPrefix = Form.useWatch('prefix', form);
  const selectedGlueType = Form.useWatch('glueType', form);

  useEffect(() => {
    getModuleList().then(setModules);
    getHandlerList().then(setHandlers);
  }, []);

  useEffect(() => {
    if (jobMode !== 'URL' || !selectedPrefix) {
      setPostApis([]);
      return;
    }
    getApiResourcePage({ pageNum: 1, pageSize: 200, modulePrefix: selectedPrefix }).then((res) => {
      const postItems = (res?.records ?? []).filter((item) => item.reqMethod === 'POST');
      setPostApis(postItems);
    });
  }, [jobMode, selectedPrefix]);

  const handleModuleChange = (prefix: string) => {
    form.setFieldValue('url', undefined);
    if (!prefix) {
      setPostApis([]);
    }
  };

  const handleJobModeChange = (value: JobMode) => {
    onJobModeChange(value);
    form.setFieldValue('prefix', undefined);
    form.setFieldValue('url', undefined);
    form.setFieldValue('bodyJson', undefined);
    form.setFieldValue('executorHandler', undefined);
    form.setFieldValue('executorParam', undefined);
    form.setFieldValue('glueType', undefined);
    form.setFieldValue('glueSource', undefined);
    form.setFieldValue('glueRemark', undefined);
  };

  const handleGlueTypeChange = (value: string) => {
    form.setFieldValue('glueType', value);
    form.setFieldValue('glueSource', getDefaultGlueSource(value));
    if (!form.getFieldValue('glueRemark')?.trim()) {
      form.setFieldValue('glueRemark', DEFAULT_GLUE_INIT_REMARK);
    }
  };

  return (
    <>
      <Form.Item name="jobMode" label="任务模式" rules={[{ required: true, message: '请选择任务模式' }]}>
        <Select options={[...JOB_MODE_OPTIONS]} onChange={handleJobModeChange} placeholder="请选择任务模式" />
      </Form.Item>

      {jobMode === 'URL' && (
        <>
          <Form.Item name="prefix" label="所属服务" rules={[{ required: true, message: '请选择所属服务' }]}>
            <Select
              placeholder="请选择所属服务"
              options={modules.map((m) => ({ label: m.name, value: m.prefix }))}
              onChange={handleModuleChange}
              showSearch
              optionFilterProp="label"
            />
          </Form.Item>
          <Form.Item name="url" label="接口URL" rules={[{ required: true, message: '请选择接口URL' }]}>
            <Select
              placeholder="请输入关键词搜索POST接口"
              options={postApis.map((a) => ({
                label: a.summary ? `${a.reqPath}（${a.summary}）` : a.reqPath,
                value: a.reqPath,
              }))}
              showSearch
              optionFilterProp="label"
              notFoundContent="请先选择所属服务"
            />
          </Form.Item>
          <Form.Item name="bodyJson" label="请求体数据">
            <Input.TextArea rows={6} placeholder="请输入JSON格式的请求体数据" />
          </Form.Item>
        </>
      )}

      {jobMode === 'BEAN' && (
        <>
          <Form.Item name="executorHandler" label="Handler" rules={[{ required: true, message: '请选择Handler' }]}>
            <Select
              placeholder="请选择Handler"
              options={handlers.map((h) => ({ label: h, value: h }))}
              showSearch
              optionFilterProp="label"
            />
          </Form.Item>
          <Form.Item name="executorParam" label="任务参数">
            <Input.TextArea rows={4} placeholder="请输入任务参数" />
          </Form.Item>
        </>
      )}

      {jobMode === 'GLUE' && (
        <>
          <Form.Item name="glueType" label="GLUE类型" rules={[{ required: true, message: '请选择GLUE类型' }]}>
            <Select
              options={[...GLUE_TYPE_OPTIONS]}
              placeholder="请选择GLUE类型"
              onChange={handleGlueTypeChange}
            />
          </Form.Item>
          <Form.Item name="executorParam" label="任务参数">
            <Input.TextArea rows={4} placeholder="请输入任务参数" />
          </Form.Item>
          <Form.Item name="glueSource" label="脚本代码" rules={[{ required: true, message: '请输入脚本代码' }]}>
            <GlueCodeEditor glueType={selectedGlueType} />
          </Form.Item>
          <Form.Item name="glueRemark" label="版本备注">
            <Input placeholder="请输入版本备注" />
          </Form.Item>
        </>
      )}
    </>
  );
};

export default StepJobType;
