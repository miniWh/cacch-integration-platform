-- =============================================
-- V2: 智能表格会议管理业务表结构
--     总控表驱动模式：负责人维护一张总控表，定时任务扫描后自动为员工创建会议管理文档
-- =============================================

-- ---------------------------------------------
-- 1. 智能表格配置表
--    双用途：
--      table_type = MASTER  → 总控申请表，由负责人统一管理，记录哪些员工需要创建会议管理表格
--      table_type = MEETING → 员工个人会议管理表格，定时任务自动创建后存入
-- ---------------------------------------------
CREATE TABLE IF NOT EXISTS t_integration_smart_table (
    id                      BIGINT       NOT NULL,
    table_type              VARCHAR(32)  NOT NULL DEFAULT 'MEETING',
    user_id                 VARCHAR(64)  NOT NULL,
    table_name              VARCHAR(200) NOT NULL,
    doc_id                  VARCHAR(128) NOT NULL,
    doc_url                 VARCHAR(500),
    meeting_sheet_id        VARCHAR(128) NOT NULL,
    meeting_column_mapping  JSONB        NOT NULL,
    todo_sheet_id           VARCHAR(128),
    todo_column_mapping     JSONB,
    status                  SMALLINT     NOT NULL DEFAULT 1,
    last_sync_time          TIMESTAMP,
    last_sync_error         TEXT,
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted              SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT pk_t_integration_smart_table PRIMARY KEY (id)
);

COMMENT ON TABLE  t_integration_smart_table IS '智能表格配置表——总控表(MASTER)与员工会议表(MEETING)合一，由table_type区分';
COMMENT ON COLUMN t_integration_smart_table.id                     IS '主键';
COMMENT ON COLUMN t_integration_smart_table.table_type             IS '表格类型：MASTER-总控申请表（负责人维护，全局仅一条） MEETING-员工个人会议管理表格';
COMMENT ON COLUMN t_integration_smart_table.user_id                IS '企微用户ID；MASTER时为负责人ID，MEETING时为表格归属员工ID';
COMMENT ON COLUMN t_integration_smart_table.table_name             IS '表格名称；MASTER时为"会议管理总控表"，MEETING时为"张三的会议管理"';
COMMENT ON COLUMN t_integration_smart_table.doc_id                 IS '企微文档ID';
COMMENT ON COLUMN t_integration_smart_table.doc_url                IS '企微文档访问链接';
COMMENT ON COLUMN t_integration_smart_table.meeting_sheet_id       IS '主sheet ID；MASTER时指向申请表sheet，MEETING时指向会议管理sheet';
COMMENT ON COLUMN t_integration_smart_table.meeting_column_mapping IS '主sheet列映射JSON，MASTER格式: {"user_id":"fld_xxx","user_name":"fld_xxx","apply_status":"fld_xxx","created_doc_id":"fld_xxx","created_doc_url":"fld_xxx"} ；MEETING格式: {"meeting_title":"fld_xxx","meeting_date":"fld_xxx","start_time":"fld_xxx","duration":"fld_xxx","attendees":"fld_xxx","meeting_link":"fld_xxx","status":"fld_xxx","minutes":"fld_xxx"}';
COMMENT ON COLUMN t_integration_smart_table.todo_sheet_id          IS '待办管理子表ID，MASTER时为NULL（总控表无需待办sheet）';
COMMENT ON COLUMN t_integration_smart_table.todo_column_mapping    IS '待办sheet列映射JSON，格式: {"todo_title":"fld_xxx","assignee":"fld_xxx","due_date":"fld_xxx","priority":"fld_xxx","status":"fld_xxx"}';
COMMENT ON COLUMN t_integration_smart_table.status                 IS '状态：1-启用 0-停用';
COMMENT ON COLUMN t_integration_smart_table.last_sync_time         IS '最近一次同步时间';
COMMENT ON COLUMN t_integration_smart_table.last_sync_error        IS '最近一次同步异常信息';
COMMENT ON COLUMN t_integration_smart_table.created_at             IS '创建时间';
COMMENT ON COLUMN t_integration_smart_table.updated_at             IS '更新时间';
COMMENT ON COLUMN t_integration_smart_table.is_deleted             IS '逻辑删除：0-正常 1-删除';

-- 智能表格索引
-- 总控表全局唯一（同时最多一条启用记录）
CREATE UNIQUE INDEX IF NOT EXISTS uk_smart_table_master  ON t_integration_smart_table(table_type)  WHERE is_deleted = 0 AND table_type = 'MASTER';
-- 员工会议表：同一员工+同一文档唯一
CREATE UNIQUE INDEX IF NOT EXISTS uk_smart_table_meeting ON t_integration_smart_table(user_id, doc_id) WHERE is_deleted = 0 AND table_type = 'MEETING';
CREATE INDEX        IF NOT EXISTS idx_smart_table_user   ON t_integration_smart_table(user_id)      WHERE is_deleted = 0;
CREATE INDEX        IF NOT EXISTS idx_smart_table_status ON t_integration_smart_table(status)       WHERE is_deleted = 0;
CREATE INDEX        IF NOT EXISTS idx_smart_table_type   ON t_integration_smart_table(table_type)   WHERE is_deleted = 0;


-- ---------------------------------------------
-- 2. 会议记录表
--    保存从员工智能表格扫描到的每条会议行数据及企微会议创建后的信息
-- ---------------------------------------------
CREATE TABLE IF NOT EXISTS t_integration_meeting_record (
    id              BIGINT       NOT NULL,
    smart_table_id  BIGINT       NOT NULL,
    record_id       VARCHAR(128) NOT NULL,
    meeting_title   VARCHAR(500) NOT NULL,
    meeting_date    DATE         NOT NULL,
    start_time      TIME         NOT NULL,
    duration        INT          NOT NULL DEFAULT 30,
    attendees       JSONB,
    meeting_link    VARCHAR(500),
    wecom_meeting_id VARCHAR(128),
    wecom_meeting_code VARCHAR(64),
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    minutes_status  VARCHAR(32)  NOT NULL DEFAULT 'NONE',
    reminder_sent   SMALLINT     NOT NULL DEFAULT 0,
    sync_version    INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted      SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT pk_t_integration_meeting_record PRIMARY KEY (id)
);

COMMENT ON TABLE  t_integration_meeting_record IS '会议记录表——智能表格中每一行即为一条会议记录';
COMMENT ON COLUMN t_integration_meeting_record.id                 IS '主键';
COMMENT ON COLUMN t_integration_meeting_record.smart_table_id     IS '关联 t_integration_smart_table.id';
COMMENT ON COLUMN t_integration_meeting_record.record_id          IS '智能表格中的行ID，用于回写更新';
COMMENT ON COLUMN t_integration_meeting_record.meeting_title      IS '会议主题';
COMMENT ON COLUMN t_integration_meeting_record.meeting_date       IS '会议日期';
COMMENT ON COLUMN t_integration_meeting_record.start_time         IS '开始时间';
COMMENT ON COLUMN t_integration_meeting_record.duration           IS '会议时长（分钟），默认30分钟';
COMMENT ON COLUMN t_integration_meeting_record.attendees          IS '参会人员列表JSON: ["userid1","userid2",...]';
COMMENT ON COLUMN t_integration_meeting_record.meeting_link       IS '会议链接（员工自行填写的线下/其他会议链接）';
COMMENT ON COLUMN t_integration_meeting_record.wecom_meeting_id   IS '企微会议ID（系统自动创建后回写）';
COMMENT ON COLUMN t_integration_meeting_record.wecom_meeting_code IS '企微会议号';
COMMENT ON COLUMN t_integration_meeting_record.status             IS '会议状态: PENDING-待发起 SCHEDULED-已创建 IN_PROGRESS-进行中 COMPLETED-已结束 CANCELLED-已取消';
COMMENT ON COLUMN t_integration_meeting_record.minutes_status     IS '纪要状态: NONE-无 PENDING-待解析 GENERATED-已生成';
COMMENT ON COLUMN t_integration_meeting_record.reminder_sent      IS '会前提醒是否已发送：0-未发送 1-已发送';
COMMENT ON COLUMN t_integration_meeting_record.sync_version       IS '同步版本号，用于乐观锁防并发冲突';
COMMENT ON COLUMN t_integration_meeting_record.created_at         IS '创建时间';
COMMENT ON COLUMN t_integration_meeting_record.updated_at         IS '更新时间';
COMMENT ON COLUMN t_integration_meeting_record.is_deleted         IS '逻辑删除：0-正常 1-删除';

-- 会议记录索引
CREATE UNIQUE INDEX IF NOT EXISTS uk_meeting_record_id    ON t_integration_meeting_record(smart_table_id, record_id) WHERE is_deleted = 0;
CREATE INDEX        IF NOT EXISTS idx_meeting_smart_table ON t_integration_meeting_record(smart_table_id)           WHERE is_deleted = 0;
CREATE INDEX        IF NOT EXISTS idx_meeting_status      ON t_integration_meeting_record(status)                   WHERE is_deleted = 0;
CREATE INDEX        IF NOT EXISTS idx_meeting_date        ON t_integration_meeting_record(meeting_date)             WHERE is_deleted = 0;
CREATE INDEX        IF NOT EXISTS idx_meeting_wecom_id    ON t_integration_meeting_record(wecom_meeting_id)         WHERE is_deleted = 0 AND wecom_meeting_id IS NOT NULL;


-- ---------------------------------------------
-- 3. 会议纪要表
--    存储企微会议结束后获取的纪要原文及结构化摘要
-- ---------------------------------------------
CREATE TABLE IF NOT EXISTS t_integration_meeting_minutes (
    id              BIGINT       NOT NULL,
    meeting_id      BIGINT       NOT NULL,
    raw_content     TEXT,
    summary         TEXT,
    keywords        JSONB,
    speaker_summary JSONB,
    todo_list       JSONB,
    status          SMALLINT     NOT NULL DEFAULT 0,
    fetch_time      TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted      SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT pk_t_integration_meeting_minutes PRIMARY KEY (id)
);

COMMENT ON TABLE  t_integration_meeting_minutes IS '会议纪要表——存储企微会议转写及AI摘要结果';
COMMENT ON COLUMN t_integration_meeting_minutes.id              IS '主键';
COMMENT ON COLUMN t_integration_meeting_minutes.meeting_id      IS '关联 t_integration_meeting_record.id';
COMMENT ON COLUMN t_integration_meeting_minutes.raw_content     IS '纪要原始文本（会议转写全文）';
COMMENT ON COLUMN t_integration_meeting_minutes.summary         IS 'AI生成的会议摘要';
COMMENT ON COLUMN t_integration_meeting_minutes.keywords        IS '关键词列表JSON: ["项目A","排期","预算",...]';
COMMENT ON COLUMN t_integration_meeting_minutes.speaker_summary IS '发言人总结JSON: [{"speaker":"张三","content":"..."},...]';
COMMENT ON COLUMN t_integration_meeting_minutes.todo_list       IS '从纪要解析出的待办JSON: [{"title":"...","assignee":"...","due_date":"..."},...]';
COMMENT ON COLUMN t_integration_meeting_minutes.status          IS '状态：0-未拉取 1-已拉取原文 2-已生成摘要 3-已解析待办';
COMMENT ON COLUMN t_integration_meeting_minutes.fetch_time      IS '纪要拉取时间';
COMMENT ON COLUMN t_integration_meeting_minutes.created_at      IS '创建时间';
COMMENT ON COLUMN t_integration_meeting_minutes.updated_at      IS '更新时间';
COMMENT ON COLUMN t_integration_meeting_minutes.is_deleted      IS '逻辑删除：0-正常 1-删除';

-- 会议纪要索引
CREATE UNIQUE INDEX IF NOT EXISTS uk_minutes_meeting ON t_integration_meeting_minutes(meeting_id) WHERE is_deleted = 0;
CREATE INDEX        IF NOT EXISTS idx_minutes_status  ON t_integration_meeting_minutes(status)     WHERE is_deleted = 0;


-- ---------------------------------------------
-- 4. 待办事项表
--    从会议纪要AI解析得出，或员工手动在待办sheet中添加
-- ---------------------------------------------
CREATE TABLE IF NOT EXISTS t_integration_todo_item (
    id              BIGINT       NOT NULL,
    meeting_id      BIGINT,
    smart_table_id  BIGINT       NOT NULL,
    record_id       VARCHAR(128),
    todo_title      VARCHAR(500) NOT NULL,
    assignee        VARCHAR(64),
    assignee_name   VARCHAR(100),
    due_date        DATE,
    priority        VARCHAR(16)  NOT NULL DEFAULT 'MEDIUM',
    source          VARCHAR(32)  NOT NULL DEFAULT 'FROM_MINUTES',
    source_text     TEXT,
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    completed_time  TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted      SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT pk_t_integration_todo_item PRIMARY KEY (id)
);

COMMENT ON TABLE  t_integration_todo_item IS '待办事项表——从会议纪要AI解析或手动添加';
COMMENT ON COLUMN t_integration_todo_item.id              IS '主键';
COMMENT ON COLUMN t_integration_todo_item.meeting_id      IS '关联 t_integration_meeting_record.id（来源会议的待办必填）';
COMMENT ON COLUMN t_integration_todo_item.smart_table_id  IS '关联 t_integration_smart_table.id';
COMMENT ON COLUMN t_integration_todo_item.record_id       IS '智能表格中对应的行ID，用于回写更新状态';
COMMENT ON COLUMN t_integration_todo_item.todo_title      IS '待办标题';
COMMENT ON COLUMN t_integration_todo_item.assignee        IS '负责人企微UserID';
COMMENT ON COLUMN t_integration_todo_item.assignee_name   IS '负责人姓名';
COMMENT ON COLUMN t_integration_todo_item.due_date        IS '截止日期';
COMMENT ON COLUMN t_integration_todo_item.priority        IS '优先级: HIGH-高 MEDIUM-中 LOW-低';
COMMENT ON COLUMN t_integration_todo_item.source          IS '来源: FROM_MINUTES-纪要解析 MANUAL-手动添加';
COMMENT ON COLUMN t_integration_todo_item.source_text     IS '来源原文片段（纪要中提取待办时的上下文）';
COMMENT ON COLUMN t_integration_todo_item.status          IS '状态: PENDING-待办 IN_PROGRESS-进行中 COMPLETED-已完成';
COMMENT ON COLUMN t_integration_todo_item.completed_time  IS '完成时间';
COMMENT ON COLUMN t_integration_todo_item.created_at      IS '创建时间';
COMMENT ON COLUMN t_integration_todo_item.updated_at      IS '更新时间';
COMMENT ON COLUMN t_integration_todo_item.is_deleted      IS '逻辑删除：0-正常 1-删除';

-- 待办事项索引
CREATE INDEX IF NOT EXISTS idx_todo_meeting     ON t_integration_todo_item(meeting_id)     WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_todo_smart_table ON t_integration_todo_item(smart_table_id) WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_todo_assignee    ON t_integration_todo_item(assignee)       WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_todo_status_due  ON t_integration_todo_item(status, due_date) WHERE is_deleted = 0;
