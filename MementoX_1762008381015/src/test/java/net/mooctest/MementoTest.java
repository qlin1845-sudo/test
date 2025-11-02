package net.mooctest;

import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.util.*;

import org.junit.Test;

public class MementoTest {

    private static class DummyMemento extends Memento {
        private final Object state;

        DummyMemento(Object state) {
            this.state = state;
        }

        @Override
        public Object getState() {
            return state;
        }
    }

    private static class RecordingPlugin implements Plugin {
        private final String name;
        private int executions;

        RecordingPlugin(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void execute(UserManager userManager) {
            executions += userManager.getAllUsers().size();
        }

        int getExecutions() {
            return executions;
        }
    }

    @Test
    public void testCalendarManagerAddAndRetrieveNotes() throws Exception {
        // 场景：验证按日期与月份检索笔记是否准确。
        CalendarManager manager = new CalendarManager();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Note note1 = new Note("工作计划");
        Note note2 = new Note("会议记录");
        Date dayOne = sdf.parse("2024-05-20");
        Date dayTwo = sdf.parse("2024-05-21");
        manager.addNoteByDate(note1, dayOne);
        manager.addNoteByDate(note2, dayTwo);

        List<Note> notesInDayOne = manager.getNotesByDay(dayOne);
        assertEquals(1, notesInDayOne.size());
        assertTrue(notesInDayOne.contains(note1));

        List<Note> notesInMonth = manager.getNotesByMonth(dayOne);
        assertEquals(2, notesInMonth.size());
        assertTrue(notesInMonth.contains(note1));
        assertTrue(notesInMonth.contains(note2));
    }

    @Test
    public void testCalendarManagerEmptyListImmutability() {
        // 场景：验证没有笔记时返回的空集合不可被外部修改。
        CalendarManager manager = new CalendarManager();
        Date today = new Date(0L);
        List<Note> emptyNotes = manager.getNotesByDay(today);
        assertTrue(emptyNotes.isEmpty());
        try {
            emptyNotes.add(new Note("任意"));
            fail("应该抛出不可修改集合的异常");
        } catch (UnsupportedOperationException expected) {
            assertNotNull(expected);
        }
    }

    @Test
    public void testCalendarReminderBehaviors() {
        // 场景：验证提醒对象的防御性拷贝和触发标记。
        Note note = new Note("提醒内容");
        Date remindDate = new Date();
        CalendarManager.Reminder reminder = new CalendarManager.Reminder(note, remindDate);
        assertSame(note, reminder.getNote());
        Date copied = reminder.getRemindTime();
        assertNotSame(remindDate, copied);
        long originalTime = copied.getTime();
        copied.setTime(copied.getTime() + 1000);
        assertEquals(originalTime, reminder.getRemindTime().getTime());
        assertFalse(reminder.isTriggered());
        reminder.setTriggered(true);
        assertTrue(reminder.isTriggered());
        try {
            new CalendarManager.Reminder(null, remindDate);
            fail("空笔记应当抛出异常");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("null"));
        }
        try {
            new CalendarManager.Reminder(note, null);
            fail("空时间应当抛出异常");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("null"));
        }
    }

    @Test
    public void testCaretakerSaveUndoRedo() throws Exception {
        // 场景：验证保存、撤销与重做的核心流程。
        Caretaker caretaker = new Caretaker();
        NoteMemento m1 = new NoteMemento("第一版");
        NoteMemento m2 = new NoteMemento("第二版");
        caretaker.save(m1);
        caretaker.save(m2);
        assertSame(m2, caretaker.getCurrent());
        assertSame(m1, caretaker.undo());
        assertSame(m1, caretaker.getCurrent());
        assertSame(m2, caretaker.redo());
        List<Memento> historyCopy = caretaker.getAllHistory();
        assertEquals(2, historyCopy.size());
        historyCopy.clear();
        assertEquals(2, caretaker.getAllHistory().size());
    }

    @Test
    public void testCaretakerBoundaryAndClear() {
        // 场景：验证边界撤销、重做以及清空后的行为。
        Caretaker caretaker = new Caretaker();
        NoteMemento m1 = new NoteMemento("初稿");
        caretaker.save(m1);
        try {
            caretaker.undo();
            fail("首次撤销应失败");
        } catch (MementoException e) {
            assertTrue(e.getMessage().contains("Cannot undo"));
        }
        try {
            caretaker.redo();
            fail("没有未来版本时重做应失败");
        } catch (MementoException e) {
            assertTrue(e.getMessage().contains("Cannot redo"));
        }
        caretaker.clear();
        assertTrue(caretaker.getAllHistory().isEmpty());
        try {
            caretaker.getCurrent();
            fail("清空后没有当前快照");
        } catch (MementoException e) {
            assertTrue(e.getMessage().contains("No current"));
        }
    }

    @Test
    public void testCaretakerSaveAfterUndoDropsFuture() throws MementoException {
        // 场景：验证撤销后再次保存会丢弃未来分支。
        Caretaker caretaker = new Caretaker();
        NoteMemento m1 = new NoteMemento("A");
        NoteMemento m2 = new NoteMemento("B");
        NoteMemento m3 = new NoteMemento("C");
        caretaker.save(m1);
        caretaker.save(m2);
        caretaker.undo();
        caretaker.save(m3);
        List<Memento> history = caretaker.getAllHistory();
        assertEquals(2, history.size());
        assertSame(m3, caretaker.getCurrent());
        try {
            caretaker.redo();
            fail("未来分支已被丢弃，应无法重做");
        } catch (MementoException e) {
            assertTrue(e.getMessage().contains("Cannot redo"));
        }
    }

    @Test
    public void testHistoryManagerSaveUndoRedo() throws Exception {
        // 场景：验证历史管理器的保存与撤销流程。
        Note note = new Note("v1");
        HistoryManager manager = new HistoryManager(note);
        note.setContent("v2");
        manager.save();
        note.setContent("v3");
        manager.save();
        manager.undo();
        assertEquals("v2", note.getContent());
        manager.undo();
        assertEquals("v1", note.getContent());
        try {
            manager.undo();
            fail("再撤销应失败");
        } catch (MementoException e) {
            assertTrue(e.getMessage().contains("Cannot undo"));
        }
        manager.redo();
        assertEquals("v2", note.getContent());
        manager.redo();
        assertEquals("v3", note.getContent());
        try {
            manager.redo();
            fail("没有更多未来状态");
        } catch (MementoException e) {
            assertTrue(e.getMessage().contains("Cannot redo"));
        }
        List<Memento> copy = manager.getHistory();
        copy.clear();
        assertEquals(3, manager.getHistory().size());
    }

    @Test
    public void testHistoryManagerBranchSwitching() throws Exception {
        // 场景：验证分支切换时快照与内容的恢复。
        Note note = new Note("主线");
        HistoryManager manager = new HistoryManager(note);
        note.setContent("主线v2");
        manager.save();
        manager.createBranch("feature");
        manager.switchBranch("feature");
        assertEquals("feature", manager.getCurrentBranch());
        assertEquals("主线v2", note.getContent());
        note.setContent("分支v1");
        manager.save();
        assertEquals(1, manager.getHistory().size());
        manager.switchBranch("main");
        assertEquals("main", manager.getCurrentBranch());
        assertEquals("主线v2", note.getContent());
        List<String> branches = manager.getAllBranches();
        assertTrue(branches.contains("main"));
        assertTrue(branches.contains("feature"));
    }

    @Test
    public void testHistoryManagerBranchNotFound() {
        // 场景：验证切换不存在的分支时抛出异常。
        HistoryManager manager = new HistoryManager(new Note("内容"));
        try {
            manager.switchBranch("ghost");
            fail("未知分支应抛出异常");
        } catch (MementoException e) {
            assertTrue(e.getMessage().contains("Branch"));
        }
    }

    @Test
    public void testHistoryManagerClearHistory() {
        // 场景：验证清空历史后无法再撤销。
        Note note = new Note("版本");
        HistoryManager manager = new HistoryManager(note);
        note.setContent("版本2");
        manager.save();
        manager.clearHistory();
        assertTrue(manager.getHistory().isEmpty());
        try {
            manager.undo();
            fail("历史已清空，不应允许撤销");
        } catch (MementoException e) {
            assertTrue(e.getMessage().contains("Cannot undo"));
        }
    }

    @Test
    public void testLabelConstructionAndHierarchy() {
        // 场景：验证标签名称修整、层级路径以及子节点集合不可变。
        Label root = new Label(" 根 标签 ");
        assertEquals("根 标签", root.getName());
        assertEquals("根 标签", root.getFullPath());
        assertTrue(root.getChildren().isEmpty());

        Label child = new Label(" 子 标签 ", root);
        assertEquals("子 标签", child.getName());
        assertEquals("根 标签/子 标签", child.getFullPath());
        assertTrue(root.getChildren().isEmpty());
        assertTrue(child.getChildren().isEmpty());
        try {
            root.getChildren().add(child);
            fail("子列表应不可修改");
        } catch (UnsupportedOperationException expected) {
            assertNotNull(expected);
        }
    }

    @Test
    public void testLabelEquality() {
        // 场景：验证标签的相等性与哈希码遵循名称一致性。
        Label a1 = new Label("标签A");
        Label a2 = new Label("标签A");
        Label b = new Label("标签B");
        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());
        assertNotEquals(a1, b);
    }

    @Test
    public void testLabelInvalidName() {
        // 场景：验证非法名称会触发异常。
        try {
            new Label(null);
            fail("空名称应抛出异常");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Label name"));
        }
        try {
            new Label("   ");
            fail("空白名称应抛出异常");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Label name"));
        }
    }

    @Test
    public void testLabelManagerAddRemove() {
        // 场景：验证标签与笔记的关联与解绑。
        LabelManager manager = new LabelManager();
        Label label = new Label("学习");
        Note note = new Note("学习计划");
        manager.addLabelToNote(label, note);
        assertTrue(manager.getNotesByLabel(label).contains(note));
        assertTrue(note.getLabels().contains(label));
        manager.removeLabelFromNote(label, note);
        assertFalse(manager.getNotesByLabel(label).contains(note));
        assertFalse(note.getLabels().contains(label));
        assertFalse(manager.getAllLabels().contains(label));
    }

    @Test
    public void testLabelManagerEmptyResultIsolation() {
        // 场景：验证不存在标签时返回的新集合互不影响。
        LabelManager manager = new LabelManager();
        Label label = new Label("不存在");
        Set<Note> notes = manager.getNotesByLabel(label);
        assertTrue(notes.isEmpty());
        notes.add(new Note("新增"));
        assertFalse(manager.getNotesByLabel(label).containsAll(notes));
    }

    @Test
    public void testLabelSuggestionService() {
        // 场景：验证基于内容的标签推荐区分大小写。
        LabelSuggestionService service = new LabelSuggestionService();
        Note note = new Note("今天学习Java的设计模式");
        List<Label> labels = Arrays.asList(new Label("java"), new Label("Python"), new Label("模式"));
        List<Label> suggestions = service.suggestLabels(note, labels);
        assertEquals(2, suggestions.size());
        assertTrue(suggestions.contains(labels.get(0)));
        assertTrue(suggestions.contains(labels.get(2)));
    }

    @Test
    public void testMementoExceptionConstructors() {
        // 场景：验证备忘录异常的消息与原因保持。
        Throwable cause = new IllegalStateException("根因");
        MementoException ex = new MementoException("提示", cause);
        assertEquals("提示", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    public void testNoteContentAndLabels() {
        // 场景：验证笔记内容的空值处理与标签集合隔离。
        Note note = new Note(null);
        assertEquals("", note.getContent());
        note.setContent("新的内容");
        assertEquals("新的内容", note.getContent());
        note.setContent(null);
        assertEquals("", note.getContent());
        Label label = new Label("重要");
        note.addLabel(null);
        note.addLabel(label);
        assertTrue(note.getLabels().contains(label));
        Set<Label> copy = note.getLabels();
        copy.add(new Label("附加"));
        assertFalse(note.getLabels().contains(new Label("附加")));
        note.removeLabel(label);
        assertFalse(note.getLabels().contains(label));
    }

    @Test
    public void testNoteRestoreWrongType() {
        // 场景：验证使用错误类型备忘录时抛出异常。
        Note note = new Note("原始");
        try {
            note.restoreMemento(new DummyMemento("其他"));
            fail("类型不匹配应抛出异常");
        } catch (MementoException e) {
            assertTrue(e.getMessage().contains("Wrong memento"));
        }
    }

    @Test
    public void testNoteDiffUtil() {
        // 场景：验证文本比较结果包含新增与删除标记。
        String oldContent = "第一行\n第二行";
        String newContent = "第一行修改\n第二行";
        String diff = NoteDiffUtil.diff(oldContent, newContent);
        assertTrue(diff.contains("- 第一行"));
        assertTrue(diff.contains("+ 第一行修改"));
        assertTrue(diff.contains("  第二行"));
    }

    @Test
    public void testNoteDiffUtilWithNull() {
        // 场景：验证空内容能安全比较并给出新增提示。
        String diff = NoteDiffUtil.diff(null, "唯一一行");
        assertTrue(diff.contains("+ 唯一一行"));
    }

    @Test
    public void testNoteEncryptor() {
        // 场景：验证加密与解密互为逆操作且处理空值。
        String original = "安全文本";
        String encrypted = NoteEncryptor.encrypt(original);
        assertNotEquals(original, encrypted);
        assertEquals(original, NoteEncryptor.decrypt(encrypted));
        assertNull(NoteEncryptor.encrypt(null));
    }

    @Test
    public void testNoteMementoState() {
        // 场景：验证备忘录封装的状态与时间戳隔离。
        NoteMemento memento = new NoteMemento("状态");
        assertEquals("状态", memento.getState());
        Date timestamp = memento.getTimestamp();
        long original = timestamp.getTime();
        timestamp.setTime(0L);
        assertEquals(original, memento.getTimestamp().getTime());
        assertNotNull(memento.getId());
    }

    @Test
    public void testPermissionManagerGrantRevoke() {
        // 场景：验证权限授予与撤销的行为。
        PermissionManager manager = new PermissionManager();
        User user = new User("权限用户");
        manager.grantPermission(user, Permission.OWNER);
        assertEquals(Permission.OWNER, manager.getPermission(user));
        manager.grantPermission(null, Permission.EDIT);
        manager.revokePermission(user);
        assertNull(manager.getPermission(user));
        assertFalse(manager.canView(user));
    }

    @Test
    public void testPermissionManagerCanEditView() {
        // 场景：验证不同权限对编辑与查看能力的影响。
        PermissionManager manager = new PermissionManager();
        User viewer = new User("只读");
        manager.grantPermission(viewer, Permission.VIEW);
        assertTrue(manager.canView(viewer));
        assertFalse(manager.canEdit(viewer));
        User editor = new User("编辑");
        manager.grantPermission(editor, Permission.EDIT);
        assertTrue(manager.canEdit(editor));
        User owner = new User("所有者");
        manager.grantPermission(owner, Permission.OWNER);
        assertTrue(manager.canEdit(owner));
        Set<User> collaborators = manager.listCollaborators();
        assertEquals(3, collaborators.size());
    }

    @Test
    public void testPluginManager() {
        // 场景：验证插件注册、不可变列表以及执行次数。
        PluginManager manager = new PluginManager();
        RecordingPlugin plugin = new RecordingPlugin("统计");
        manager.register(plugin);
        manager.register(null);
        assertEquals(1, manager.getPlugins().size());
        try {
            manager.getPlugins().add(plugin);
            fail("插件列表应为只读");
        } catch (UnsupportedOperationException expected) {
            assertNotNull(expected);
        }
        UserManager userManager = new UserManager();
        userManager.registerUser("用户1");
        manager.executeAll(userManager);
        assertEquals(1, plugin.getExecutions());
    }

    @Test
    public void testRecycleBin() {
        // 场景：验证回收站的收集、恢复与清空逻辑。
        RecycleBin bin = new RecycleBin();
        Note note = new Note("删除的笔记");
        bin.recycle(null);
        bin.recycle(note);
        assertTrue(bin.isInBin(note));
        assertTrue(bin.restore(note));
        assertFalse(bin.isInBin(note));
        bin.recycle(note);
        bin.clear();
        assertFalse(bin.isInBin(note));
        assertFalse(bin.restore(note));
        Set<Note> listed = bin.listDeletedNotes();
        assertTrue(listed.isEmpty());
    }

    @Test
    public void testRuleEngine() {
        // 场景：验证规则引擎添加与执行规则时的行为。
        RuleEngine engine = new RuleEngine();
        Note note = new Note("规则内容");
        UserManager userManager = new UserManager();
        userManager.registerUser("成员");
        final int[] counter = {0};
        engine.addRule((n, um) -> counter[0] = n.getContent().length() + um.getAllUsers().size());
        engine.addRule(null);
        engine.applyAll(note, userManager);
        assertEquals(note.getContent().length() + 1, counter[0]);
        try {
            engine.getRules().add((n, um) -> {});
            fail("规则列表应不可修改");
        } catch (UnsupportedOperationException expected) {
            assertNotNull(expected);
        }
    }

    @Test
    public void testSearchServiceSearch() {
        // 场景：验证标签搜索、关键词搜索与跨用户搜索。
        SearchService service = new SearchService();
        User user = new User("搜索用户");
        Note note1 = new Note("学习Java");
        Note note2 = new Note("整理日程");
        Label label = new Label("学习");
        note1.addLabel(label);
        user.addNote(note1);
        user.addNote(note2);
        assertEquals(1, service.searchByLabel(user, label).size());
        assertTrue(service.searchByKeyword(user, "Java").contains(note1));
        assertTrue(service.searchByKeyword(user, null).isEmpty());

        User another = new User("另一个");
        Note note3 = new Note("Java 面试");
        another.addNote(note3);
        List<Note> all = service.searchByKeywordAllUsers(Arrays.asList(user, another), "Java");
        assertEquals(2, all.size());

        List<Note> fuzzy = service.fuzzySearch(user, "java");
        assertEquals(1, fuzzy.size());
        assertTrue(service.fuzzySearch(user, "").isEmpty());
    }

    @Test
    public void testSearchServiceHighlight() {
        // 场景：验证高亮功能的大小写敏感与特殊字符处理。
        SearchService service = new SearchService();
        String highlighted = service.highlight("Hello java HELLO", "hello");
        assertEquals("[[Hello]] java [[HELLO]]", highlighted);
        assertEquals("a[[+]]b", service.highlight("a+b", "+"));
        assertNull(service.highlight(null, "+"));
        assertEquals("原文", service.highlight("原文", null));
    }

    @Test
    public void testStatisticsServiceLabelUsage() {
        // 场景：验证标签使用统计正确累加。
        StatisticsService service = new StatisticsService();
        User user1 = new User("用户甲");
        User user2 = new User("用户乙");
        Label labelJava = new Label("Java");
        Label labelAi = new Label("AI");
        Note note1 = new Note("Java 基础");
        note1.addLabel(labelJava);
        Note note2 = new Note("Java 高级");
        note2.addLabel(labelJava);
        note2.addLabel(labelAi);
        Note note3 = new Note("AI 概览");
        note3.addLabel(labelAi);
        user1.addNote(note1);
        user1.addNote(note2);
        user2.addNote(note3);
        Map<Label, Integer> usage = service.labelUsage(Arrays.asList(user1, user2));
        assertEquals(Integer.valueOf(2), usage.get(labelJava));
        assertEquals(Integer.valueOf(2), usage.get(labelAi));
    }

    @Test
    public void testStatisticsServiceNoteCount() {
        // 场景：验证笔记总数统计正确。
        StatisticsService service = new StatisticsService();
        User user1 = new User("甲");
        User user2 = new User("乙");
        user1.addNote(new Note("a"));
        user1.addNote(new Note("b"));
        user2.addNote(new Note("c"));
        assertEquals(3, service.noteCount(Arrays.asList(user1, user2)));
    }

    @Test
    public void testUserAddRemoveNote() {
        // 场景：验证用户新增、去重及删除笔记并保持内部集合安全。
        User user = new User("记录者");
        Note note = new Note("内容");
        user.addNote(note);
        user.addNote(note);
        assertEquals(1, user.getNotes().size());
        HistoryManager historyManager = user.getHistoryManager(note);
        assertNotNull(historyManager);
        List<Note> copy = user.getNotes();
        copy.clear();
        assertEquals(1, user.getNotes().size());
        user.removeNote(note);
        assertTrue(user.getNotes().isEmpty());
        assertNull(user.getHistoryManager(note));
    }

    @Test
    public void testUserInvalidNameThrows() {
        // 场景：验证非法用户名会触发异常保护。
        try {
            new User(null);
            fail("null 名称应抛出异常");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Username"));
        }
        try {
            new User("   ");
            fail("空白名称应抛出异常");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Username"));
        }
    }

    @Test
    public void testUserManagerRegister() {
        // 场景：验证用户注册后能正常检索与列举。
        UserManager manager = new UserManager();
        User user = manager.registerUser("Alice");
        assertSame(user, manager.getUser("Alice"));
        assertEquals(1, manager.getAllUsers().size());
    }

    @Test
    public void testUserManagerDuplicateAndRemove() {
        // 场景：验证重复注册与删除用户的异常处理。
        UserManager manager = new UserManager();
        manager.registerUser("Bob");
        try {
            manager.registerUser("Bob");
            fail("重复用户应抛出异常");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("exists"));
        }
        manager.removeUser("Bob");
        assertNull(manager.getUser("Bob"));
    }
}
