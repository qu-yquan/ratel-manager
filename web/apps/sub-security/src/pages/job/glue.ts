export type GlueEditorLanguage =
  | 'java'
  | 'shell'
  | 'python'
  | 'javascript'
  | 'php'
  | 'powershell';

export const DEFAULT_GLUE_INIT_REMARK = 'GLUE代码初始化';
export const DEFAULT_GLUE_UPDATE_REMARK = 'GLUE代码更新';

const GLUE_SOURCE_TEMPLATES: Record<string, string> = {
  GLUE_GROOVY: `package org.quyq.gwsu.job.service.handler;

import org.quyq.gwsu.common.job.context.XxlJobHelper;
import org.quyq.gwsu.common.job.handler.IJobHandler;

public class DemoGlueJobHandler extends IJobHandler {

    @Override
    public void execute() throws Exception {
        XxlJobHelper.log("XXL-JOB, Hello World.");
    }

}
`,
  GLUE_SHELL: `#!/bin/bash
echo "xxl-job: hello shell"

echo "脚本位置：$0"
echo "任务参数：$1"
echo "分片序号 = $2"
echo "分片总数 = $3"
# for param in $*
# do
#     echo "参数 : $param"
#     sleep 1s
# done

echo "Good bye!"
exit 0
`,
  GLUE_PYTHON: `#!/usr/bin/python
# -*- coding: UTF-8 -*-
import sys

print("xxl-job: hello python")

print("脚本位置：", sys.argv[0])
print("任务参数：", sys.argv[1])
print("分片序号：", sys.argv[2])
print("分片总数：", sys.argv[3])

print("Good bye!")
exit(0)
`,
  GLUE_NODEJS: `#!/usr/bin/env node
console.log("xxl-job: hello nodejs")

const args = process.argv

console.log("脚本位置: " + args[1])
console.log("任务参数: " + args[2])
console.log("分片序号: " + args[3])
console.log("分片总数: " + args[4])

console.log("Good bye!")
process.exit(0)
`,
  GLUE_PHP: `<?php

echo "xxl-job: hello php\\n";

echo "脚本位置：$argv[0]\\n";
echo "任务参数：$argv[1]\\n";
echo "分片序号 = $argv[2]\\n";
echo "分片总数 = $argv[3]\\n";

echo "Good bye!\\n";
exit(0);
`,
  GLUE_POWERSHELL: `Write-Host "xxl-job: hello powershell"

Write-Host "脚本位置: " $MyInvocation.MyCommand.Definition
Write-Host "任务参数: "
if ($args.Count -gt 2) { $args[0..($args.Count-3)] }
Write-Host "分片序号: " $args[$args.Count-2]
Write-Host "分片总数: " $args[$args.Count-1]

Write-Host "Good bye!"
exit 0
`,
};

const GLUE_EDITOR_LANGUAGE_MAP: Record<string, GlueEditorLanguage> = {
  GLUE_GROOVY: 'java',
  GLUE_SHELL: 'shell',
  GLUE_PYTHON: 'python',
  GLUE_NODEJS: 'javascript',
  GLUE_PHP: 'php',
  GLUE_POWERSHELL: 'powershell',
};

export function getDefaultGlueSource(glueType?: string): string {
  if (!glueType) {
    return '';
  }

  return GLUE_SOURCE_TEMPLATES[glueType] ?? '';
}

export function getGlueEditorLanguage(glueType?: string): GlueEditorLanguage {
  if (!glueType) {
    return 'java';
  }

  return GLUE_EDITOR_LANGUAGE_MAP[glueType] ?? 'java';
}

export function normalizeGlueRemark(glueRemark: string | undefined, defaultRemark: string): string {
  const normalizedRemark = glueRemark?.trim();
  return normalizedRemark ? normalizedRemark : defaultRemark;
}
