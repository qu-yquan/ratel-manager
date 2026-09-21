# 六、菜单配置与功能描述

## 6.1 适用范围

开发新菜单页面、新增功能按钮或调整按钮权限时，完成代码后必须同步处理菜单配置：

1. 检查页面及按钮对应的接口权限、`button_key` 和父子关系。
2. 在项目初始化数据中新增或更新 `security_menu` 配置。
3. 在交付结果中提供可执行的 `security_menu` `INSERT` 语句，方便维护菜单数据。

新增菜单时，若无法从现有菜单数据中确认父菜单 ID，不得猜测，`parent_id` 使用 `'<PARENT_MENU_ID>'` 占位，并明确提示替换。其他可以从代码或现有数据确认的字段应填写真实值。

## 6.2 description 编写原则

`description` 是智能助手理解和操作界面的主要依据，应以最少文字准确说明功能与稳定的界面结构。

### 功能介绍

- 说明该菜单或按钮解决什么问题、作用对象和主要结果。
- 有权限范围、数据范围或重要联动结果时，用一句话说明。
- 避免实现细节、接口名称、代码术语和重复表述。

### 界面布局

- 描述页面、弹框或抽屉中稳定的区域、排列顺序和内容位置，使助手能据此定位界面。
- 页面菜单应描述搜索区、树、列表、详情区等总体结构，不罗列具体按钮；按钮会随用户权限变化。
- 按钮只有在打开具有实际操作内容的弹框、抽屉或独立界面时才写界面布局。
- 普通确认框、提示框以及点击后直接执行的操作不写界面布局。
- 不同内容必须换行表达，禁止把多类权限或多个区域挤在一行。

### 推荐格式

有独立界面布局时：

```markdown
# 功能介绍
用一至两句说明功能用途和结果。
# 界面布局
按从上到下、从左到右的顺序说明主要区域和内容。
```

没有特殊界面时：

```markdown
# 功能介绍
用一句话说明功能用途和结果。
```

## 6.3 SQL 编写要求

- 页面菜单使用 `menu_type = 2`，权限按钮使用 `menu_type = 3`。
- 按钮的 `parent_id` 指向所属页面菜单 ID，`button_key` 必须与前端 `permissionConstants.ts` 完全一致。
- `permission` 填写该功能实际调用的接口；同一按钮覆盖多个相关操作时，将接口归入同一按钮权限。
- 页面菜单仅配置进入页面和基础展示所需接口，受按钮控制的写操作接口放在对应按钮记录中。
- `description` 使用真实换行保存功能介绍和界面布局，不压缩成一行。
- 沿用项目现有字段顺序、时间格式、服务前缀和接口权限语法。

页面菜单模板：

```sql
INSERT INTO security_menu
(id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission,
 tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op,
 delete_time, position, owner, button_key, description)
VALUES
('<MENU_ID>', '<PARENT_MENU_ID>', '<菜单名称>', 2, 0, NULL, '<页面路径>', 1, 1,
 '<页面基础接口权限>', NULL, 'admin', '<CREATE_TIME>', 'admin', '<MODIFY_TIME>',
 0, NULL, NULL, 1, 1, NULL, '# 功能介绍
<功能用途和范围>
# 界面布局
<稳定的页面区域和内容布局>');
```

带特殊界面的按钮模板：

```sql
INSERT INTO security_menu
(id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission,
 tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op,
 delete_time, position, owner, button_key, description)
VALUES
('<BUTTON_ID>', '<PAGE_MENU_ID>', '<按钮名称>', 3, 0, NULL, NULL, 1, 1,
 '<按钮接口权限>', NULL, 'admin', '<CREATE_TIME>', 'admin', '<MODIFY_TIME>',
 0, NULL, NULL, 1, 1, '<BUTTON_KEY>', '# 功能介绍
<功能用途和结果>
# 界面布局
<弹框、抽屉或独立界面的主要区域>');
```

没有特殊界面的按钮只保留功能介绍：

```sql
INSERT INTO security_menu
(id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission,
 tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op,
 delete_time, position, owner, button_key, description)
VALUES
('<BUTTON_ID>', '<PAGE_MENU_ID>', '<按钮名称>', 3, 0, NULL, NULL, 1, 1,
 '<按钮接口权限>', NULL, 'admin', '<CREATE_TIME>', 'admin', '<MODIFY_TIME>',
 0, NULL, NULL, 1, 1, '<BUTTON_KEY>', '# 功能介绍
<功能用途和结果>');
```

## 6.4 交付要求

最终说明应包含：

- 新增或变更的菜单、按钮及权限关系。
- 对应的 `security_menu` `INSERT` 语句；多条语句按页面菜单、按钮的顺序排列。
- 使用了占位符时，列出需要替换的字段。

描述应直接写入 `INSERT` 语句的 `description` 字段，不再额外输出一份含义相同的长篇说明。
