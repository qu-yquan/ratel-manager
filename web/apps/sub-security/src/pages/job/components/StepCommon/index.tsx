import React from 'react';
import { Form, Input, InputNumber, Select } from 'antd';
import {
  MISFIRE_STRATEGY_OPTIONS,
  BLOCK_STRATEGY_OPTIONS,
  ROUTE_STRATEGY_OPTIONS,
} from '../../types';

const StepCommon: React.FC = () => {
  return (
    <>
      <Form.Item name="name" label="任务名称" rules={[{ required: true, message: '请输入任务名称' }]}>
        <Input placeholder="请输入任务名称" />
      </Form.Item>
      <Form.Item name="alarmEmail" label="报警邮件">
        <Input placeholder="报警邮件，多个逗号分隔" />
      </Form.Item>
      <Form.Item name="misfireStrategy" label="过期策略" rules={[{ required: true, message: '请选择过期策略' }]} initialValue="DO_NOTHING">
        <Select options={[...MISFIRE_STRATEGY_OPTIONS]} />
      </Form.Item>
      <Form.Item name="executorRouteStrategy" label="路由策略" rules={[{ required: true, message: '请选择路由策略' }]} initialValue="ROUND">
        <Select options={[...ROUTE_STRATEGY_OPTIONS]} />
      </Form.Item>
      <Form.Item name="executorBlockStrategy" label="阻塞策略" rules={[{ required: true, message: '请选择阻塞策略' }]} initialValue="SERIAL_EXECUTION">
        <Select options={[...BLOCK_STRATEGY_OPTIONS]} />
      </Form.Item>
      <Form.Item name="executorTimeout" label="超时时间(秒)" initialValue={0}>
        <InputNumber min={0} style={{ width: '100%' }} placeholder="0表示不超时" />
      </Form.Item>
      <Form.Item name="executorFailRetryCount" label="重试次数" initialValue={0}>
        <InputNumber min={0} style={{ width: '100%' }} placeholder="0表示不重试" />
      </Form.Item>
    </>
  );
};

export default StepCommon;
