# 登录验证码弹窗 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将后台管理登录、OAuth 登录和钉钉“绑定已有账号”的验证码改为点击提交后弹出，并在验证成功后自动继续登录或绑定。

**Architecture:** 将 `CaptchaVerify` 从表单内嵌入口重构为受控 `Modal`，调用方用 `open` 控制显示，并通过 `onSuccess(CaptchaPass)` 接收一次性验证码结果。三个业务入口先做前端字段校验，再打开弹窗；验证码成功回调直接调用现有登录或绑定接口，确保后端登录请求始终同时携带验证码 ID 和 Code。

**Tech Stack:** React 19、TypeScript、Ant Design 6、UmiJS 4、CSS Modules/Less、pnpm

---

## 文件结构与验证策略

- 修改 `web/apps/gwsu-sub-system/src/pages/components/CaptchaVerify.tsx`：受控验证码弹窗及验证码生命周期。
- 修改 `web/apps/gwsu-sub-system/src/pages/components/CaptchaVerify.module.less`：弹窗内容布局。
- 修改 `web/apps/gwsu-sub-system/src/pages/login.tsx`：后台登录接入。
- 修改 `web/apps/gwsu-sub-system/src/pages/oauth2/login.tsx`：OAuth 登录接入。
- 修改 `web/apps/gwsu-sub-system/src/pages/components/DingTalkFirstLoginModal.tsx`：仅已有账号绑定接入。

项目声明的 `umi test` 在当前 Umi 4.6.53 下会报 `Invalid sub command test`，仓库也没有测试运行器或现有前端测试。因此本次不额外引入测试框架，使用先改接口契约后运行 TypeScript 检查的类型级红绿验证、生产构建、定向代码检查和手工交互验收。

### Task 1: 将验证码组件改为受控弹窗

**Files:**
- Modify: `web/apps/gwsu-sub-system/src/pages/components/CaptchaVerify.tsx`
- Modify: `web/apps/gwsu-sub-system/src/pages/components/CaptchaVerify.module.less`

- [ ] **Step 1: 修改 Props 契约并确认旧调用方失败**

```tsx
interface CaptchaVerifyProps {
    open: boolean;
    onCancel: () => void;
    onSuccess: (value: CaptchaPass) => void;
}

const CaptchaVerify: React.FC<CaptchaVerifyProps> = ({open, onCancel, onSuccess}) => {
```

运行 `cd web && pnpm --filter gwsu-sub-system exec tsc --noEmit`。

预期：FAIL，三处旧调用方的 `value` / `onChange` 不符合新 Props，证明所有接入点均受类型系统约束。

- [ ] **Step 2: 改为弹窗打开时才获取验证码**

删除 `panelOpen`、`previousValueRef`、挂载时刷新验证码的 Effect，以及 `refreshCaptcha` 内的 `onChange(null)`。实现：

```tsx
const resetCaptchaState = useCallback(() => {
    setCaptcha(null);
    setSliderX(0);
    sliderXRef.current = 0;
    setClickPoints([]);
    setDragging(false);
}, []);

const refreshCaptcha = useCallback(async () => {
    setLoading(true);
    setSliderX(0);
    sliderXRef.current = 0;
    setClickPoints([]);
    try {
        setCaptcha(await getCaptcha());
    } catch {
        setCaptcha(null);
        message.error('验证码加载失败，请稍后重试');
    } finally {
        setLoading(false);
    }
}, [message]);

useEffect(() => {
    if (open) {
        void refreshCaptcha();
        return;
    }
    resetCaptchaState();
}, [open, refreshCaptcha, resetCaptchaState]);
```

- [ ] **Step 3: 校验成功后将一次性结果交给业务层**

`submitCaptchaCheck` 成功分支改为：

```tsx
const result = await checkCaptcha({
    captchaId: captchaData.captchaId,
    captchaCode,
    pointJson,
});
message.success('验证码校验通过');
onSuccess({
    captchaId: result.captchaId,
    captchaCode: result.captchaCode,
});
```

失败分支继续 `await refreshCaptcha()`，不得调用 `onSuccess`。滑块和点选的前置判断移除旧 `value` 条件。

- [ ] **Step 4: 将内嵌入口改为 Ant Design Modal**

导入 `Modal`，删除触发按钮和 `openCaptchaPanel`，顶层结构改为：

```tsx
<Modal
    open={open}
    title={isBlockPuzzle ? '拖动滑块完成拼图' : '请依次点击文字'}
    centered
    width={420}
    footer={null}
    maskClosable={!checking}
    keyboard={!checking}
    closable={!checking}
    destroyOnHidden
    onCancel={onCancel}
    className={styles.captchaModal}
>
    <div className={styles.captchaContent} aria-busy={loading || checking}>
        <div className={styles.captchaToolbar}>
            <span>{checking ? '正在验证...' : '请完成安全验证'}</span>
            <button type="button" className={styles.captchaRefresh}
                    onClick={refreshCaptcha} disabled={loading || checking}>
                换一张
            </button>
        </div>
        {loading || !captchaData ? (
            <div className={styles.captchaLoading}>验证码加载中...</div>
        ) : isBlockPuzzle ? (
            <>
                <div className={styles.captchaImageBox}>
                    <img ref={captchaImageRef}
                         src={imageSource(captchaData.originalImageBase64)}
                         className={styles.captchaImage}
                         alt="滑块验证码背景" draggable={false}/>
                    <img ref={jigsawImageRef}
                         src={imageSource(captchaData.jigsawImageBase64)}
                         className={styles.jigsawImage}
                         alt="滑块拼图" draggable={false}
                         style={{transform: `translateX(${sliderX}px)`}}/>
                </div>
                <div ref={trackRef} className={styles.sliderTrack}>
                    <div className={styles.sliderProgress}
                         style={{width: `${sliderX + 42}px`}}/>
                    <button type="button" className={styles.sliderHandle}
                            style={{transform: `translateX(${sliderX}px)`}}
                            onPointerDown={handleSliderPointerDown}
                            onPointerMove={handleSliderPointerMove}
                            onPointerUp={(event) => void handleSliderPointerUp(event)}
                            disabled={checking} aria-label="拖动滑块完成拼图">
                        →
                    </button>
                    <span className={styles.sliderText}>向右拖动滑块</span>
                </div>
            </>
        ) : (
            <div className={styles.clickWordBox}>
                <div className={styles.wordTip}>
                    请依次点击：{captchaData.wordList?.join('、') || '图中文字'}
                </div>
                <div className={styles.clickImageWrap}>
                    <img ref={captchaImageRef}
                         src={imageSource(captchaData.originalImageBase64)}
                         className={styles.captchaImage}
                         alt="文字点选验证码" draggable={false}
                         onClick={(event) => void handleClickWord(event)}/>
                    {clickPoints.map((point, index) => (
                        <span key={`${point.x}-${point.y}-${index}`}
                              className={styles.clickMarker}
                              style={{
                                  left: `${point.x / (captchaImageRef.current?.naturalWidth || 1) * 100}%`,
                                  top: `${point.y / (captchaImageRef.current?.naturalHeight || 1) * 100}%`,
                              }}>
                            {index + 1}
                        </span>
                    ))}
                </div>
            </div>
        )}
    </div>
</Modal>
```

Less 中删除 `.captchaGroup`、`.captchaTrigger*`、`.captchaPanel*` 等内嵌入口规则，新增 `.captchaModal`、`.captchaContent`、`.captchaToolbar`；保留图片、滑块和点选标记样式，颜色使用主题变量。

- [ ] **Step 5: 检查组件自身并提交**

运行 `cd web && pnpm --filter gwsu-sub-system exec tsc --noEmit`。

预期：仍仅因三个业务调用方未迁移而 FAIL，验证码组件自身无错误。

```bash
git add web/apps/gwsu-sub-system/src/pages/components/CaptchaVerify.tsx web/apps/gwsu-sub-system/src/pages/components/CaptchaVerify.module.less
git commit -m "refactor: 将登录验证码改为受控弹窗"
```

### Task 2: 接入后台管理登录与 OAuth 登录

**Files:**
- Modify: `web/apps/gwsu-sub-system/src/pages/login.tsx`
- Modify: `web/apps/gwsu-sub-system/src/pages/oauth2/login.tsx`

- [ ] **Step 1: 后台登录先校验字段再打开验证码**

用 `captchaOpen` 替换 `captchaPass`。提交事件不再调用后端：

```tsx
const [captchaOpen, setCaptchaOpen] = useState(false);

const handleLogin = (event: React.FormEvent) => {
    event.preventDefault();
    if (!username.trim() || !password.trim()) {
        message.warning('请输入用户名和密码');
        return;
    }
    setCaptchaOpen(true);
};
```

验证码成功后自动登录：

```tsx
const handleCaptchaSuccess = async (captchaPass: CaptchaPass) => {
    setCaptchaOpen(false);
    setLoading(true);
    try {
        const loginToken = await login({
            type: 'password', terminal: TerminalType.WEB,
            username: username.trim(), password: encryptPassword(password),
            captchaId: captchaPass.captchaId,
            captchaCode: captchaPass.captchaCode,
        });
        await handleLoginSuccess(loginToken);
    } catch {
        // 请求层统一展示错误；验证码不复用。
    } finally {
        setLoading(false);
    }
};
```

从表单删除内嵌验证码，在页面根节点内加入：

```tsx
<CaptchaVerify open={captchaOpen}
               onCancel={() => setCaptchaOpen(false)}
               onSuccess={(pass) => void handleCaptchaSuccess(pass)}/>
```

- [ ] **Step 2: OAuth 登录采用相同流程**

同样用 `captchaOpen` 替换 `captchaPass`。`handleLogin` 保留账号字段检查，并在 `redirect` 无效时直接提示和返回；有效时仅打开验证码。新增成功回调：

```tsx
const handleCaptchaSuccess = async (captchaPass: CaptchaPass) => {
    setCaptchaOpen(false);
    setLoading(true);
    try {
        await login({
            type: 'password', terminal: TerminalType.WEB,
            username: username.trim(), password: encryptPassword(password),
            captchaId: captchaPass.captchaId,
            captchaCode: captchaPass.captchaCode,
        });
        handleLoginSuccess();
    } catch {
        // 请求层统一展示错误。
    } finally {
        setLoading(false);
    }
};
```

删除表单内验证码，并渲染与后台登录相同的新 Props。

- [ ] **Step 3: 类型检查并提交**

运行 `cd web && pnpm --filter gwsu-sub-system exec tsc --noEmit`。

预期：后台和 OAuth 不再报验证码 Props 错误，仅钉钉旧调用方仍 FAIL。

```bash
git add web/apps/gwsu-sub-system/src/pages/login.tsx web/apps/gwsu-sub-system/src/pages/oauth2/login.tsx
git commit -m "feat: 登录提交后弹出验证码"
```

### Task 3: 接入钉钉绑定已有账号

**Files:**
- Modify: `web/apps/gwsu-sub-system/src/pages/components/DingTalkFirstLoginModal.tsx`

- [ ] **Step 1: 分离创建提交与绑定验证入口**

用 `captchaOpen` 替换 `captchaPass`；父弹框打开、关闭或切换页签时均关闭验证码。`handleSubmit` 校验临时凭证和表单后，绑定模式只执行 `setCaptchaOpen(true)`；创建模式保留原 `completeDingTalkLogin(buildDingTalkCompleteParams('create', ...))` 调用，且不得获取验证码。

核心分支：

```tsx
const values = await form.validateFields();
if (method === 'binding') {
    setCaptchaOpen(true);
    return;
}
setSubmitting(true);
const finalToken = await completeDingTalkLogin(
    buildDingTalkCompleteParams('create', temporaryVoucher, {
        username: values.username.trim(),
        password: encryptPassword(values.password),
    }),
);
await onSuccess(finalToken);
```

- [ ] **Step 2: 验证成功后自动完成已有账号绑定**

```tsx
const handleCaptchaSuccess = async (captchaPass: CaptchaPass) => {
    if (!temporaryVoucher) {
        setCaptchaOpen(false);
        message.error('临时凭证已失效，请重新进行钉钉登录');
        return;
    }
    setCaptchaOpen(false);
    setSubmitting(true);
    try {
        const values = await form.validateFields();
        const passwordToken = await login({
            type: 'password', terminal: TerminalType.WEB,
            username: values.username.trim(), password: encryptPassword(values.password),
            captchaId: captchaPass.captchaId,
            captchaCode: captchaPass.captchaCode,
        });
        const finalToken = await completeDingTalkLogin(
            buildDingTalkCompleteParams('binding', temporaryVoucher, {
                bindingToken: passwordToken.token,
            }),
        );
        await onSuccess(finalToken);
    } catch {
        // 请求层统一展示错误；下次绑定重新验证。
    } finally {
        setSubmitting(false);
    }
};
```

删除绑定表单里的“安全验证”表单项，在父 `Modal` 内容末尾渲染受控 `CaptchaVerify`。切换到创建页签时关闭验证码，确保创建路径不受影响。

- [ ] **Step 3: 完整类型检查并提交**

运行 `cd web && pnpm --filter gwsu-sub-system exec tsc --noEmit`。

预期：PASS，无 TypeScript 错误。

```bash
git add web/apps/gwsu-sub-system/src/pages/components/DingTalkFirstLoginModal.tsx
git commit -m "feat: 钉钉绑定验证通过后自动提交"
```

### Task 4: 完整验证与收尾

**Files:**
- Verify: `web/apps/gwsu-sub-system/src/pages/components/CaptchaVerify.tsx`
- Verify: `web/apps/gwsu-sub-system/src/pages/login.tsx`
- Verify: `web/apps/gwsu-sub-system/src/pages/oauth2/login.tsx`
- Verify: `web/apps/gwsu-sub-system/src/pages/components/DingTalkFirstLoginModal.tsx`

- [ ] **Step 1: 定向检查旧状态和新接口**

```bash
rg -n "<CaptchaVerify|captchaPass|setCaptchaPass|panelOpen|captchaTrigger" web/apps/gwsu-sub-system/src/pages
rg -n -C 5 "captchaId: captchaPass.captchaId" web/apps/gwsu-sub-system/src/pages
```

预期：不存在页面级 `captchaPass` 状态、内嵌触发器或 `panelOpen`；恰有三处登录请求同时传递 `captchaId` 和 `captchaCode`。

- [ ] **Step 2: 运行生产构建**

运行 `cd web && pnpm --filter gwsu-sub-system build`。

预期：PASS，Umi 输出 `Build successfully`；允许既有包体积提示，不得有编译错误。

- [ ] **Step 3: 手工验收**

```text
1. 后台登录：空字段不弹验证码；字段完整时弹出；通过后自动登录。
2. OAuth 登录：有效授权请求下弹出；通过后自动登录并继续授权。
3. 钉钉绑定：仅“绑定已有账号”弹出；通过后自动登录并绑定。
4. 钉钉创建：直接提交，全程不获取或展示验证码。
5. 验证失败：刷新挑战并保留弹窗，登录接口不发送。
6. 取消验证：保留账号输入，登录接口不发送。
7. 登录失败：再次提交获取新验证码，不复用旧结果。
```

- [ ] **Step 4: 检查工作区**

```bash
git diff --check
git status --short
git log -4 --oneline
```

预期：无空白错误和非预期文件；提交记录清晰覆盖组件、两个登录入口和钉钉绑定。
