# Ratel 项目生成器

根据当前 `ratel-manager-basic` 基线生成独立的新项目。

```bash
./tools/project-generator/create-project.sh demo
```

默认在当前仓库上一级创建 `ratel-demo`，默认 groupId 为 `org.quyq.demo`。

```bash
./tools/project-generator/create-project.sh demo --group-prefix com.test
./tools/project-generator/create-project.sh demo --output /tmp/test-demo
./tools/project-generator/create-project.sh demo --dry-run
```

生成器不会覆盖已经存在的目标目录。新项目的前端仅包含独立 system 子应用，公共 `gwsu-main` 不会被复制；`@gwsu/core` 通过 `pnpm-workspace.yaml` 中的一条外部 workspace 路径引用。
