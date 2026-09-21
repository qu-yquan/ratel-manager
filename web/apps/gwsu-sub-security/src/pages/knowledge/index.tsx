import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  Breadcrumb,
  Button,
  Form,
  Input,
  Modal,
  Space,
  Table,
  Tag,
  Tooltip,
  Tree,
  message,
} from "antd";
import type { TableProps, TreeDataNode } from "antd";
import {
  DeleteOutlined,
  FolderOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
} from "@ant-design/icons";
import { useAuth, useUserStore } from "@gwsu/core";
import DocumentDetailModal from "./components/DocumentDetailModal";
import KnowledgeCreateModal from "./components/KnowledgeCreateModal";
import SearchDebugModal from "./components/SearchDebugModal";
import {
  deleteKnowledgeNodes,
  getChildren,
  getDirectoryTree,
  getKnowledgeDocument,
  getRootCapabilities,
  searchKnowledgeDocuments,
  retryKnowledgeTask,
  saveDirectory,
  toggleDocument,
} from "./services/knowledge";
import type { DocumentStatus, KnowledgeNode, PageResult } from "./types";
import {
  PERM_KNOWLEDGE_DIRECTORY_EDIT,
  PERM_KNOWLEDGE_DISABLED_ENABLE,
  PERM_KNOWLEDGE_REMOVE,
  PERM_KNOWLEDGE_SEARCH_DEBUG,
  PERM_KNOWLEDGE_UPLOAD,
} from "./permissionConstants";
import styles from "./index.module.less";

const emptyPage = (): PageResult<KnowledgeNode> => ({
  records: [],
  total: 0,
  size: 20,
  current: 1,
  pages: 0,
});
const statusLabels: Record<DocumentStatus, string> = {
  UPLOADED: "待处理",
  PROCESSING: "处理中",
  PROCESSED: "已完成",
  FAILED: "失败",
};
const statusColors: Record<DocumentStatus, string> = {
  UPLOADED: "default",
  PROCESSING: "processing",
  PROCESSED: "success",
  FAILED: "error",
};

function asTree(nodes: KnowledgeNode[], rootAllowed: boolean): TreeDataNode[] {
  const children = new Map<string | undefined, KnowledgeNode[]>();
  nodes.forEach((node) => {
    const parent = node.parentId ?? undefined;
    children.set(parent, [...(children.get(parent) ?? []), node]);
  });
  const build = (parentId?: string): TreeDataNode[] =>
    (children.get(parentId) ?? []).map((node) => ({
      key: node.id,
      selectable: node.canSearch === true,
      title: (
        <span className={node.canSearch ? undefined : styles.treeGuide}>
          {node.name}
          <span className={styles.treeCount}>({node.documentCount ?? 0})</span>
        </span>
      ),
      icon: <FolderOutlined />,
      children: build(node.id),
    }));
  return [
    {
      key: "root",
      title: "全部目录",
      selectable: rootAllowed,
      icon: <FolderOutlined />,
      children: build(),
    },
  ];
}

const KnowledgePage: React.FC = () => {
  const isAdmin = useUserStore(
    (state) =>
      state.userInfo?.admin === true ||
      state.userInfo?.roles?.includes("super_admin") === true
  );
  const hasDirectoryEditPermission =
    useAuth(PERM_KNOWLEDGE_DIRECTORY_EDIT) || isAdmin;
  const hasUploadPermission = useAuth(PERM_KNOWLEDGE_UPLOAD) || isAdmin;
  const hasTogglePermission =
    useAuth(PERM_KNOWLEDGE_DISABLED_ENABLE) || isAdmin;
  const hasRemovePermission = useAuth(PERM_KNOWLEDGE_REMOVE) || isAdmin;
  const canDebug = useAuth(PERM_KNOWLEDGE_SEARCH_DEBUG) || isAdmin;
  const [tree, setTree] = useState<KnowledgeNode[]>([]);
  const [rootCapabilities, setRootCapabilities] = useState<KnowledgeNode>();
  const [selectedId, setSelectedId] = useState<string>();
  const [page, setPage] = useState<PageResult<KnowledgeNode>>(emptyPage);
  const [loading, setLoading] = useState(false);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [searchInput, setSearchInput] = useState("");
  const [nameQuery, setNameQuery] = useState("");
  const [renameNode, setRenameNode] = useState<KnowledgeNode>();
  const [createOpen, setCreateOpen] = useState(false);
  const [detailId, setDetailId] = useState<string>();
  const [debugOpen, setDebugOpen] = useState(false);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [selectedRows, setSelectedRows] = useState<KnowledgeNode[]>([]);
  const [directoryForm] = Form.useForm<{ name: string }>();

  const refreshTree = useCallback(async () => {
    const [directories, root] = await Promise.all([
      getDirectoryTree(),
      getRootCapabilities(),
    ]);
    setTree(directories);
    setRootCapabilities(root);
  }, []);
  const refreshChildren = useCallback(
    async (silent = false) => {
      if (!silent) setLoading(true);
      try {
        setPage(
          nameQuery
            ? await searchKnowledgeDocuments(nameQuery, pageNum, pageSize)
            : await getChildren(selectedId, "", pageNum, pageSize)
        );
      } finally {
        if (!silent) setLoading(false);
      }
    },
    [selectedId, nameQuery, pageNum, pageSize]
  );
  useEffect(() => {
    void refreshTree();
  }, [refreshTree]);
  useEffect(() => {
    if (!rootCapabilities || nameQuery) return;
    if (selectedId && tree.find((node) => node.id === selectedId)?.canSearch)
      return;
    if (!selectedId && rootCapabilities.canSearch) return;
    const firstAccessible = tree.find((node) => node.canSearch);
    setSelectedId(firstAccessible?.id);
  }, [rootCapabilities, selectedId, nameQuery, tree]);
  useEffect(() => {
    void refreshChildren();
  }, [refreshChildren]);
  useEffect(() => {
    const hasPendingDocument = page.records.some(
      (node) =>
        node.nodeType === "DOCUMENT" &&
        (node.documentStatus === "UPLOADED" ||
          node.documentStatus === "PROCESSING")
    );
    if (!hasPendingDocument) return;
    let active = true;
    let timer: number;
    const poll = async () => {
      try {
        await refreshChildren(true);
      } catch {
        /* 临时查询失败时继续下一轮。 */
      }
      if (active)
        timer = window.setTimeout(() => {
          void poll();
        }, 3000);
    };
    timer = window.setTimeout(() => {
      void poll();
    }, 3000);
    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [page.records, refreshChildren]);

  const treeData = useMemo(
    () => asTree(tree, rootCapabilities?.canSearch === true),
    [tree, rootCapabilities]
  );
  const currentDirectory = tree.find((item) => item.id === selectedId);
  const currentCapabilities = selectedId ? currentDirectory : rootCapabilities;
  const canEditDirectory =
    hasDirectoryEditPermission && currentCapabilities?.canManage === true;
  const canUpload =
    hasUploadPermission && currentCapabilities?.canUpload === true;
  const breadcrumbs = useMemo(() => {
    const chain: KnowledgeNode[] = [];
    let cursor = currentDirectory;
    while (cursor) {
      chain.unshift(cursor);
      cursor = tree.find((item) => item.id === cursor?.parentId);
    }
    return chain;
  }, [currentDirectory, tree]);

  const openDirectory = useCallback(
    (id?: string) => {
      if (
        id
          ? tree.find((node) => node.id === id)?.canSearch !== true
          : rootCapabilities?.canSearch !== true
      )
        return;
      setSelectedId(id);
      setSearchInput("");
      setNameQuery("");
      setPageNum(1);
      setSelectedRowKeys([]);
      setSelectedRows([]);
    },
    [tree, rootCapabilities]
  );

  const handleSearch = () => {
    const nextQuery = searchInput.trim();
    if (!nextQuery) {
      handleReset();
      return;
    }
    setSelectedRowKeys([]);
    setSelectedRows([]);
    setSelectedId(undefined);
    setPageNum(1);
    setNameQuery(nextQuery);
    if (pageNum === 1 && nameQuery === nextQuery && selectedId === undefined)
      void refreshChildren();
  };

  const handleReset = () => {
    setSearchInput("");
    setSelectedRowKeys([]);
    setSelectedRows([]);
    setSelectedId(undefined);
    setPageNum(1);
    setNameQuery("");
    if (pageNum === 1 && !nameQuery && selectedId === undefined)
      void refreshChildren();
  };

  const handleBatchDelete = () => {
    if (!hasRemovePermission || !selectedRowKeys.length) return;
    const directoryCount = selectedRows.filter(
      (node) => node.nodeType === "DIRECTORY"
    ).length;
    Modal.confirm({
      title: `删除选中的 ${selectedRowKeys.length} 项？`,
      content: directoryCount
        ? `其中包含 ${directoryCount} 个目录，目录下的子目录和文档也会一起删除。`
        : "选中的文档将被删除。",
      okText: "删除",
      okButtonProps: { danger: true, "data-ai-approval": "true" },
      onOk: async () => {
        await deleteKnowledgeNodes(selectedRowKeys.map(String));
        message.success("删除成功");
        setSelectedRowKeys([]);
        setSelectedRows([]);
        setPageNum(1);
        await Promise.all([refreshTree(), refreshChildren()]);
      },
    });
  };

  const handleDirectorySave = async () => {
    if (!renameNode) return;
    const values = await directoryForm.validateFields();
    await saveDirectory({ id: renameNode.id, name: values.name.trim() });
    message.success("目录已重命名");
    setRenameNode(undefined);
    directoryForm.resetFields();
    await Promise.all([refreshTree(), refreshChildren()]);
  };

  const handleRetry = async (id: string) => {
    const document = await getKnowledgeDocument(id);
    if (!document.latestTaskId) {
      message.warning("文档暂无可重试的导入任务");
      return;
    }
    Modal.confirm({
      title: "确认重新导入？",
      content: "重新导入成功后将覆盖当前 Markdown，包括人工编辑内容。",
      okButtonProps: { "data-ai-approval": "true" },
      onOk: async () => {
        await retryKnowledgeTask(document.latestTaskId!);
        message.success("已提交重新导入");
        await refreshChildren();
      },
    });
  };

  const handleToggle = (node: KnowledgeNode) => {
    const enabled = node.enabled === false;
    Modal.confirm({
      title: enabled ? "启用文档？" : "禁用文档？",
      okButtonProps: { "data-ai-approval": "true" },
      onOk: async () => {
        await toggleDocument(node.id, enabled);
        message.success(enabled ? "文档已启用" : "文档已禁用");
        await refreshChildren();
      },
    });
  };

  const columns: TableProps<KnowledgeNode>["columns"] = [
    {
      title: "名称",
      dataIndex: "name",
      ellipsis: true,
      render: (_, node) => (
        <Button
          type="link"
          className={styles.nameButton}
          disabled={node.nodeType === "DIRECTORY" && node.canSearch !== true}
          onClick={() =>
            node.nodeType === "DIRECTORY"
              ? openDirectory(node.id)
              : setDetailId(node.id)
          }
        >
          {node.nodeType === "DIRECTORY" && (
            <FolderOutlined aria-hidden="true" />
          )}{" "}
          {node.name}
        </Button>
      ),
    },
    {
      title: "类型",
      width: 110,
      render: (_, node) =>
        node.nodeType === "DIRECTORY" ? (
          <Tag color="blue">目录</Tag>
        ) : (
          <Tag>文档</Tag>
        ),
    },
    {
      title: "状态",
      width: 130,
      render: (_, node) => {
        if (node.nodeType !== "DOCUMENT") return "-";
        const status = node.documentStatus ?? "UPLOADED";
        const tag = (
          <Tag color={statusColors[status]}>{statusLabels[status]}</Tag>
        );
        return status === "FAILED" && node.processMessage ? (
          <Tooltip title={node.processMessage}>{tag}</Tooltip>
        ) : (
          tag
        );
      },
    },
    {
      title: "修改时间",
      dataIndex: "modifyTime",
      width: 180,
      render: (value, node) => value || node.createTime || "-",
    },
    {
      title: "操作",
      width: 220,
      render: (_, node) =>
        node.nodeType === "DIRECTORY" ? (
          hasDirectoryEditPermission && node.canManage ? (
            <Button
              type="link"
              onClick={() => {
                directoryForm.setFieldValue("name", node.name);
                setRenameNode(node);
              }}
            >
              重命名
            </Button>
          ) : null
        ) : (
          <Space size="small">
            {hasTogglePermission && node.canEdit && (
              <Button type="link" onClick={() => handleToggle(node)}>
                {node.enabled === false ? "启用" : "禁用"}
              </Button>
            )}
            {hasUploadPermission && node.canEdit && (
              <Button type="link" onClick={() => void handleRetry(node.id)}>
                重新导入
              </Button>
            )}
          </Space>
        ),
    },
  ];

  return (
    <div className={styles.knowledgePage}>
      <div
        className={styles.searchBar}
        role="search"
        aria-label="搜索全部可访问文档"
      >
        <div className={styles.searchItem}>
          <span className={styles.searchLabel}>文档名称</span>
          <Input
            className={styles.nameSearch}
            value={searchInput}
            allowClear
            placeholder="搜索全部有权限的文档"
            onChange={(event) => setSearchInput(event.target.value)}
            onPressEnter={handleSearch}
          />
        </div>
        <div className={styles.searchActions}>
          <Button
            type="primary"
            icon={<SearchOutlined />}
            onClick={handleSearch}
          >
            搜索
          </Button>
          <Button icon={<ReloadOutlined />} onClick={handleReset}>
            重置
          </Button>
        </div>
      </div>
      <div className={styles.body}>
        <aside className={styles.treePanel} aria-label="知识目录">
          <div className={styles.panelHeading}>目录</div>
          <Tree
            key={tree.length}
            showIcon
            defaultExpandAll
            treeData={treeData}
            selectedKeys={nameQuery ? [] : [selectedId ?? "root"]}
            onSelect={(keys) =>
              openDirectory(
                keys[0] && keys[0] !== "root" ? String(keys[0]) : undefined
              )
            }
          />
        </aside>
        <section
          className={styles.listPanel}
          aria-label={nameQuery ? "文档搜索结果" : "目录内容"}
        >
          <div className={styles.toolbar}>
            {nameQuery ? (
              <Breadcrumb
                items={[{ title: "文档搜索结果" }, { title: nameQuery }]}
              />
            ) : (
              <Breadcrumb
                items={[
                  {
                    title: (
                      <Button
                        type="link"
                        disabled={rootCapabilities?.canSearch !== true}
                        onClick={() => openDirectory()}
                      >
                        全部目录
                      </Button>
                    ),
                  },
                  ...breadcrumbs.map((item) => ({
                    title: (
                      <Button
                        type="link"
                        disabled={item.canSearch !== true}
                        onClick={() => openDirectory(item.id)}
                      >
                        {item.name}
                      </Button>
                    ),
                  })),
                ]}
              />
            )}
          </div>
          <div className={styles.tableHeader}>
            <Space>
              {(canEditDirectory || canUpload) && (
                <Button
                  type="primary"
                  icon={<PlusOutlined />}
                  onClick={() => setCreateOpen(true)}
                >
                  新建/上传
                </Button>
              )}
              {canDebug && (
                <Button
                  icon={<SearchOutlined />}
                  onClick={() => setDebugOpen(true)}
                >
                  检索调试
                </Button>
              )}
              {hasRemovePermission && (
                <Button
                  danger
                  icon={<DeleteOutlined />}
                  disabled={selectedRowKeys.length === 0}
                  onClick={handleBatchDelete}
                >
                  删除
                </Button>
              )}
            </Space>
          </div>
          <Table<KnowledgeNode>
            rowKey="id"
            columns={columns}
            dataSource={page.records}
            loading={loading}
            scroll={{ x: 800 }}
            rowSelection={
              hasRemovePermission
                ? {
                    selectedRowKeys,
                    getCheckboxProps: (record) => ({
                      disabled: record.canDelete !== true,
                    }),
                    onChange: (keys, rows) => {
                      setSelectedRowKeys(keys);
                      setSelectedRows(rows);
                    },
                  }
                : undefined
            }
            pagination={{
              current: page.current,
              pageSize: page.size,
              total: page.total,
              showSizeChanger: true,
              showTotal: (total) => `共 ${total} 项`,
              onChange: (nextPage, nextSize) => {
                setPageNum(nextPage);
                setPageSize(nextSize);
                setSelectedRowKeys([]);
                setSelectedRows([]);
              },
            }}
          />
        </section>
      </div>
      <Modal
        title="重命名目录"
        open={!!renameNode}
        onCancel={() => setRenameNode(undefined)}
        onOk={() => void handleDirectorySave()}
        okButtonProps={{ "data-ai-approval": "true" }}
        destroyOnHidden
      >
        <Form form={directoryForm} layout="vertical">
          <Form.Item
            name="name"
            label="目录名称"
            rules={[
              { required: true, message: "请输入目录名称" },
              { max: 200 },
            ]}
          >
            <Input maxLength={200} />
          </Form.Item>
        </Form>
      </Modal>
      <KnowledgeCreateModal
        open={createOpen}
        directories={tree}
        rootCapabilities={rootCapabilities}
        initialDirectoryId={selectedId}
        canCreateDirectory={canEditDirectory}
        canUpload={canUpload}
        onClose={() => setCreateOpen(false)}
        onCreated={async () => {
          await Promise.all([refreshTree(), refreshChildren()]);
        }}
      />
      <DocumentDetailModal
        documentId={detailId}
        canEdit={hasUploadPermission}
        onClose={() => setDetailId(undefined)}
        onUpdated={() => void refreshChildren()}
      />
      <SearchDebugModal open={debugOpen} onClose={() => setDebugOpen(false)} />
    </div>
  );
};

export default KnowledgePage;
