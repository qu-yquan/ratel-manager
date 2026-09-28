import React, {
  useCallback,
  useEffect,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import {
  Alert,
  Button,
  DatePicker,
  Empty,
  Form,
  Input,
  Radio,
  Select,
  Spin,
} from "antd";
import {
  DownloadOutlined,
  SearchOutlined,
  StepBackwardOutlined,
  StepForwardOutlined,
} from "@ant-design/icons";
import dayjs, { type Dayjs } from "dayjs";

import {
  getServerLogContext,
  getServerLogOptions,
  searchServerLogs,
} from "../../../log/services/log";
import type {
  ServerLogContext,
  ServerLogGroup,
  ServerLogMatch,
  ServerLogOptions,
  ServerLogSearchQuery,
} from "../../../log/types";

import styles from "./index.module.less";

interface SearchValues {
  mode: "content" | "tid";
  query: string;
  serviceNames?: string[];
  range: [Dayjs, Dayjs];
}

interface LogHit {
  key: string;
  group: ServerLogGroup;
  match: ServerLogMatch;
}

const INITIAL_LINES_BEFORE = 10;
const INITIAL_LINES_AFTER = 30;
const CONTEXT_PAGE_LINES = 40;

function renderHighlightedText(
  text: string,
  searchText: string
): React.ReactNode {
  if (!searchText) {
    return text;
  }

  const fragments: React.ReactNode[] = [];
  let cursor = 0;
  let matchIndex = text.indexOf(searchText, cursor);
  while (matchIndex >= 0) {
    if (matchIndex > cursor) {
      fragments.push(text.slice(cursor, matchIndex));
    }
    fragments.push(
      <mark className={styles.matchedText} key={matchIndex}>
        {text.slice(matchIndex, matchIndex + searchText.length)}
      </mark>
    );
    cursor = matchIndex + searchText.length;
    matchIndex = text.indexOf(searchText, cursor);
  }
  if (cursor < text.length) {
    fragments.push(text.slice(cursor));
  }
  return fragments.length ? fragments : text;
}

function flattenHits(
  groups: ServerLogGroup[],
  mode: SearchValues["mode"]
): LogHit[] {
  return groups
    .flatMap((group) =>
      group.matches.map((match) => ({
        key: `${group.targetId}-${match.fileId}-${match.lineNumber}`,
        group,
        match,
      }))
    )
    .sort((left, right) => {
      const comparison = (left.match.logTime ?? "").localeCompare(
        right.match.logTime ?? ""
      );
      return mode === "tid" ? comparison : -comparison;
    });
}

const ServerLogPanel: React.FC = () => {
  const [form] = Form.useForm<SearchValues>();
  const mode = Form.useWatch("mode", form) ?? "content";
  const [options, setOptions] = useState<ServerLogOptions>({
    distributed: false,
    services: [],
  });
  const [loading, setLoading] = useState(false);
  const [contextLoading, setContextLoading] = useState(false);
  const [groups, setGroups] = useState<ServerLogGroup[]>([]);
  const [selectedGroup, setSelectedGroup] = useState<ServerLogGroup | null>(
    null
  );
  const [selectedMatch, setSelectedMatch] = useState<ServerLogMatch | null>(
    null
  );
  const [context, setContext] = useState<ServerLogContext | null>(null);
  const [warningText, setWarningText] = useState("");
  const [highlightText, setHighlightText] = useState("");
  const [resultMode, setResultMode] = useState<SearchValues["mode"]>("content");
  const logContentRef = useRef<HTMLDivElement>(null);
  const contextRequestIdRef = useRef(0);
  const scrollRestoreRef = useRef<
    | { type: "focus" }
    | { type: "prepend"; scrollHeight: number; scrollTop: number }
    | null
  >(null);

  useEffect(() => {
    void getServerLogOptions()
      .then(setOptions)
      .catch(() => {});
  }, []);

  useLayoutEffect(() => {
    const container = logContentRef.current;
    const restore = scrollRestoreRef.current;
    if (!container || !restore) {
      return;
    }
    if (restore.type === "prepend") {
      container.scrollTop =
        container.scrollHeight - restore.scrollHeight + restore.scrollTop;
    } else if (selectedMatch) {
      const matchedLine = container.querySelector<HTMLElement>(
        `[data-line-number="${selectedMatch.lineNumber}"]`
      );
      if (matchedLine) {
        container.scrollTop = Math.max(
          0,
          matchedLine.offsetTop - container.clientHeight / 2
        );
      }
    }
    scrollRestoreRef.current = null;
  }, [context, selectedMatch]);

  const loadContext = useCallback(
    async (group: ServerLogGroup, match: ServerLogMatch) => {
      const requestId = ++contextRequestIdRef.current;
      const startLine = Math.max(1, match.lineNumber - INITIAL_LINES_BEFORE);
      const lineCount = match.lineNumber - startLine + 1 + INITIAL_LINES_AFTER;
      setSelectedGroup(group);
      setSelectedMatch(match);
      setContextLoading(true);
      scrollRestoreRef.current = { type: "focus" };
      try {
        const result = await getServerLogContext({
          targetId: group.targetId,
          fileId: match.fileId,
          startLine,
          lineCount,
        });
        if (requestId === contextRequestIdRef.current) {
          setContext(result);
        }
      } catch {
        if (requestId === contextRequestIdRef.current) {
          setContext(null);
        }
      } finally {
        if (requestId === contextRequestIdRef.current) {
          setContextLoading(false);
        }
      }
    },
    []
  );

  const loadMoreContext = useCallback(
    async (direction: "previous" | "next") => {
      if (!selectedGroup || !selectedMatch || !context) {
        return;
      }
      const previous = direction === "previous";
      const endLine = previous ? context.startLine - 1 : undefined;
      const startLine = previous
        ? Math.max(1, (endLine ?? 1) - CONTEXT_PAGE_LINES + 1)
        : context.endLine + 1;
      const lineCount = previous
        ? Math.max(0, (endLine ?? 0) - startLine + 1)
        : CONTEXT_PAGE_LINES;
      if (lineCount === 0) {
        return;
      }

      const requestId = ++contextRequestIdRef.current;
      const container = logContentRef.current;
      if (previous && container) {
        scrollRestoreRef.current = {
          type: "prepend",
          scrollHeight: container.scrollHeight,
          scrollTop: container.scrollTop,
        };
      }
      setContextLoading(true);
      try {
        const result = await getServerLogContext({
          targetId: selectedGroup.targetId,
          fileId: selectedMatch.fileId,
          startLine,
          lineCount,
        });
        if (requestId !== contextRequestIdRef.current) {
          return;
        }
        setContext((current) => {
          if (!current) {
            return result;
          }
          return previous
            ? {
                ...current,
                startLine: result.startLine,
                beginningOfFile: result.beginningOfFile,
                lines: [...result.lines, ...current.lines],
              }
            : {
                ...current,
                endLine: result.endLine,
                endOfFile: result.endOfFile,
                lines: [...current.lines, ...result.lines],
              };
        });
      } catch {
        scrollRestoreRef.current = null;
      } finally {
        if (requestId === contextRequestIdRef.current) {
          setContextLoading(false);
        }
      }
    },
    [context, selectedGroup, selectedMatch]
  );

  const handleSearch = useCallback(async () => {
    try {
      const values = await form.validateFields();
      const normalizedQuery = values.query.trim();
      const query: ServerLogSearchQuery = {
        serviceNames: options.distributed ? values.serviceNames : undefined,
        startTime: values.range[0].format("YYYY-MM-DD HH:mm:ss"),
        endTime: values.range[1].format("YYYY-MM-DD HH:mm:ss"),
        ...(values.mode === "tid"
          ? { tid: normalizedQuery }
          : { content: normalizedQuery }),
      };
      setHighlightText(
        values.mode === "tid" ? `[${normalizedQuery}]` : normalizedQuery
      );
      setResultMode(values.mode);
      setLoading(true);
      contextRequestIdRef.current += 1;
      scrollRestoreRef.current = null;
      setContextLoading(false);
      setWarningText("");
      setGroups([]);
      setSelectedGroup(null);
      setSelectedMatch(null);
      setContext(null);
      const result = await searchServerLogs(query);
      setGroups(result.groups);
      const firstHit = flattenHits(result.groups, values.mode)[0];
      if (firstHit) {
        void loadContext(firstHit.group, firstHit.match);
      }
      const warnings = result.warnings
        .map(
          (item) => `${item.serviceName} ${item.instanceName}：${item.message}`
        )
        .join("；");
      setWarningText(
        result.truncated
          ? `${
              warnings ? `${warnings}；` : ""
            }已达到扫描或命中数量上限，结果可能不完整`
          : warnings
      );
    } catch {
      setGroups([]);
    } finally {
      setLoading(false);
    }
  }, [form, loadContext, options.distributed]);

  const handleDownload = useCallback(() => {
    if (!selectedMatch || !context?.lines.length) {
      return;
    }
    const content = context.lines.map((line) => line.text).join("\n");
    const blob = new Blob(["\uFEFF", content], {
      type: "text/plain;charset=utf-8",
    });
    const url = URL.createObjectURL(blob);
    const originalName = selectedMatch.fileName.replace(/\.gz$/i, "");
    const baseName = originalName.replace(/\.log$/i, "");
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = `${baseName}-lines-${context.startLine}-${context.endLine}.log`;
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    URL.revokeObjectURL(url);
  }, [context, selectedMatch]);

  const serviceOptions = useMemo(
    () =>
      options.services.map((item) => ({ label: item.value, value: item.key })),
    [options.services]
  );
  const hits = useMemo(
    () => flattenHits(groups, resultMode),
    [groups, resultMode]
  );

  return (
    <div className={styles.panel}>
      <Form<SearchValues>
        className={styles.searchBar}
        form={form}
        initialValues={{
          mode: "content",
          range: [dayjs().startOf("day"), dayjs()],
        }}
        layout="inline"
      >
        <Form.Item name="mode">
          <Radio.Group
            options={[
              { label: "关键词", value: "content" },
              { label: "TID", value: "tid" },
            ]}
            optionType="button"
          />
        </Form.Item>
        <Form.Item
          name="query"
          rules={[
            {
              required: true,
              whitespace: true,
              message: `请输入${mode === "tid" ? "TID" : "关键词"}`,
            },
          ]}
        >
          <Input
            allowClear
            aria-label={mode === "tid" ? "TID" : "日志关键词"}
            placeholder={mode === "tid" ? "请输入TID" : "请输入日志关键词"}
          />
        </Form.Item>
        {options.distributed ? (
          <Form.Item name="serviceNames">
            <Select
              allowClear
              className={styles.serviceSelect}
              maxTagCount="responsive"
              mode="multiple"
              options={serviceOptions}
              placeholder="全部服务"
            />
          </Form.Item>
        ) : null}
        <Form.Item
          name="range"
          rules={[{ required: true, message: "请选择时间范围" }]}
        >
          <DatePicker.RangePicker showTime />
        </Form.Item>
        <Button
          icon={<SearchOutlined aria-hidden="true" />}
          loading={loading}
          onClick={() => void handleSearch()}
          type="primary"
        >
          检索
        </Button>
      </Form>

      {warningText ? (
        <Alert message={warningText} showIcon type="warning" />
      ) : null}

      <div className={styles.resultArea}>
        <aside className={styles.hitPane} aria-label="日志命中记录">
          <div className={styles.hitPaneHeader}>
            <span>命中记录</span>
            <span className={styles.hitCount}>{hits.length} 条</span>
          </div>
          <Spin spinning={loading} wrapperClassName={styles.hitSpin}>
            {hits.length ? (
              <div className={styles.hitList}>
                {hits.map(({ key, group, match }) => {
                  const selected =
                    group.targetId === selectedGroup?.targetId &&
                    match.fileId === selectedMatch?.fileId &&
                    match.lineNumber === selectedMatch?.lineNumber;
                  return (
                    <button
                      aria-pressed={selected}
                      className={`${styles.hitItem} ${
                        selected ? styles.hitItemSelected : ""
                      }`}
                      key={key}
                      onClick={() => void loadContext(group, match)}
                      type="button"
                    >
                      <span className={styles.hitMeta}>
                        <span>
                          {match.logTime
                            ? dayjs(match.logTime).format("HH:mm:ss.SSS")
                            : "时间未知"}
                        </span>
                        <span>第 {match.lineNumber} 行</span>
                      </span>
                      <span className={styles.hitService}>
                        {group.serviceNote || group.serviceName} ·{" "}
                        {group.instanceName}
                      </span>
                      <span className={styles.hitPreview}>{match.preview}</span>
                    </button>
                  );
                })}
              </div>
            ) : (
              <Empty className={styles.hitEmpty} description="暂无命中日志" />
            )}
          </Spin>
        </aside>

        <section className={styles.viewerPane} aria-label="日志上下文">
          <div className={styles.viewerToolbar}>
            <div className={styles.viewerHeading}>
              <span className={styles.viewerTitle}>
                {selectedMatch?.fileName ?? "日志正文"}
              </span>
              {selectedGroup && selectedMatch ? (
                <span className={styles.viewerMeta}>
                  {selectedGroup.serviceNote || selectedGroup.serviceName} ·{" "}
                  {selectedGroup.instanceName} · 命中第{" "}
                  {selectedMatch.lineNumber} 行
                  {context
                    ? ` · 已加载 ${context.startLine}-${context.endLine} 行`
                    : ""}
                </span>
              ) : null}
            </div>
            <div className={styles.viewerActions}>
              <Button
                aria-label="向首部追加上一页日志"
                disabled={!context || context.beginningOfFile || contextLoading}
                icon={<StepBackwardOutlined aria-hidden="true" />}
                onClick={() => void loadMoreContext("previous")}
                size="small"
              >
                上一页
              </Button>
              <Button
                aria-label="向尾部追加下一页日志"
                disabled={!context || context.endOfFile || contextLoading}
                icon={<StepForwardOutlined aria-hidden="true" />}
                onClick={() => void loadMoreContext("next")}
                size="small"
              >
                下一页
              </Button>
              <Button
                aria-label="下载界面已加载的日志"
                disabled={!context?.lines.length || contextLoading}
                icon={<DownloadOutlined aria-hidden="true" />}
                onClick={handleDownload}
                size="small"
              >
                下载已加载部分
              </Button>
            </div>
          </div>
          <Spin spinning={contextLoading} wrapperClassName={styles.viewerSpin}>
            {context?.lines.length ? (
              <div
                className={styles.logContent}
                ref={logContentRef}
                role="log"
                tabIndex={0}
              >
                {context.lines.map((line) => (
                  <div
                    className={`${styles.logLine} ${
                      line.lineNumber === selectedMatch?.lineNumber
                        ? styles.matchedLine
                        : ""
                    }`}
                    data-line-number={line.lineNumber}
                    key={line.lineNumber}
                  >
                    <span className={styles.lineNumber}>{line.lineNumber}</span>
                    <span>
                      {renderHighlightedText(line.text, highlightText)}
                    </span>
                  </div>
                ))}
              </div>
            ) : (
              <Empty description="请选择日志命中记录" />
            )}
          </Spin>
        </section>
      </div>
    </div>
  );
};

export default ServerLogPanel;
