package net.mooctest;

import static org.junit.Assert.*;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LibraryTest {

    private static class TestUser extends User {
        boolean borrowInvoked;
        boolean returnInvoked;
        Exception borrowException;

        TestUser(String name, String userId) {
            super(name, userId, UserType.REGULAR);
        }

        @Override
        public void borrowBook(Book book) throws Exception {
            if (borrowException != null) {
                throw borrowException;
            }
            borrowInvoked = true;
        }

        @Override
        public void returnBook(Book book) throws Exception {
            returnInvoked = true;
        }
    }

    private static class LenientBook extends Book {
        LenientBook(String title, BookType type, int total, int available) {
            super(title, "Author", "ISBN" + title, type, total);
            setAvailableCopies(available);
        }

        @Override
        public boolean isAvailable() {
            // 复杂逻辑说明：覆盖可用性校验以忽略库存，用于触发借书中的备用预约分支。
            return true;
        }
    }

    private Book createBook(String title, BookType type, int total, int available) {
        Book book = new Book(title, "Author", "ISBN" + title, type, total);
        book.setAvailableCopies(available);
        return book;
    }

    private Date daysFromNow(int days) {
        return new Date(System.currentTimeMillis() + days * 24L * 60L * 60L * 1000L);
    }

    @SuppressWarnings("unchecked")
    private <T> T getPrivateField(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private BorrowRecord createBorrowRecord(Book book, User user, Date borrowDate, Date dueDate) {
        return new BorrowRecord(book, user, borrowDate, dueDate);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void forceBorrowedBook(User user, Book book) {
        // 复杂逻辑说明：借阅列表类型为 BorrowRecord，此处通过原始类型插入 Book 以匹配 contains 判断。
        ((List) user.borrowedBooks).add(book);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void forceReservationEntry(User user, Book book) {
        // 复杂逻辑说明：预约列表声明为 Reservation，此处插入 Book 对象以命中已预约分支。
        ((List) user.reservations).add(book);
    }

    @Test
    public void testBookAvailabilityBranch() throws Exception {
        // 测试目的：验证图书可用性判断的多分支逻辑；期望：针对维修、损坏和库存不足分别返回false，其余情况返回true。
        Book book = createBook("Available", BookType.GENERAL, 2, 2);
        assertTrue(book.isAvailable());

        book.setInRepair(true);
        assertFalse(book.isAvailable());
        book.setInRepair(false);

        book.setDamaged(true);
        assertFalse(book.isAvailable());
        book.setDamaged(false);

        book.setAvailableCopies(0);
        assertFalse(book.isAvailable());

        // 验证借阅失败时抛出正确异常信息。
        try {
            book.borrow();
            fail("应该因为无库存而抛出BookNotAvailableException");
        } catch (BookNotAvailableException e) {
            assertEquals("The book is unavailable and cannot be borrowed.", e.getMessage());
        }
    }

    @Test
    public void testBookBorrowAndReturnFlow() throws Exception {
        // 测试目的：验证借书与还书流程的完整性；期望：正常借还时更新库存，异常场景抛出对应异常。
        Book book = createBook("BorrowFlow", BookType.GENERAL, 1, 1);

        book.borrow();
        assertEquals(0, book.getAvailableCopies());

        try {
            book.borrow();
            fail("重复借阅应当抛出BookNotAvailableException");
        } catch (BookNotAvailableException e) {
            assertEquals("The book is unavailable and cannot be borrowed.", e.getMessage());
        }

        book.returnBook();
        assertEquals(1, book.getAvailableCopies());

        try {
            book.returnBook();
            fail("库存已满时还书应抛出InvalidOperationException");
        } catch (InvalidOperationException e) {
            assertEquals("All copies are in the library.", e.getMessage());
        }
    }

    @Test
    public void testBookReportAndReservation() throws Exception {
        // 测试目的：验证损坏上报、维修上报与预约队列逻辑；期望：状态正确更新且队列按优先级维护。
        Book book = createBook("Reservation", BookType.GENERAL, 3, 3);

        book.reportDamage();
        assertTrue(book.isDamaged());
        book.reportDamage();
        assertTrue(book.isDamaged());

        book.reportRepair();
        assertFalse(book.isAvailable());
        book.reportRepair();
        assertFalse(book.isAvailable());
        book.setInRepair(false);
        book.setDamaged(false);
        assertTrue(book.isAvailable());

        RegularUser userLow = new RegularUser("Low", "U1");
        userLow.deductScore(30); // 调整信用分以降低优先级
        VIPUser userHigh = new VIPUser("High", "U2");
        Reservation lowPriority = new Reservation(book, userLow);
        Reservation highPriority = new Reservation(book, userHigh);
        book.addReservation(lowPriority);
        book.addReservation(highPriority);

        Reservation first = book.getReservationQueue().peek();
        assertSame(lowPriority, first);

        book.removeReservation(lowPriority);
        assertEquals(1, book.getReservationQueue().size());

        Reservation notQueued = new Reservation(book, new RegularUser("Other", "U3"));
        book.removeReservation(notQueued);
        assertEquals(1, book.getReservationQueue().size());
    }

    @Test
    public void testBookMetadataMutators() {
        // 测试目的：验证图书元数据相关的setter方法；期望：标题、损坏标记与维修标记切换后可用性状态正确反映。
        Book book = createBook("Meta", BookType.GENERAL, 2, 2);
        book.setTitle("元数据更新");
        assertEquals("元数据更新", book.getTitle());

        book.setDamaged(true);
        assertTrue(book.isDamaged());
        assertFalse(book.isAvailable());

        book.setDamaged(false);
        assertFalse(book.isDamaged());

        book.setInRepair(true);
        assertFalse(book.isAvailable());

        book.setInRepair(false);
        book.setAvailableCopies(2);
        assertTrue(book.isAvailable());
    }

    @Test
    public void testBorrowRecordFineAndExtension() {
        // 测试目的：验证借阅记录罚金计算与续期逻辑；期望：不同书籍类型、用户状态与损坏情况均正确计算罚金，续期增加到期日。
        RegularUser user = new RegularUser("User", "U1");
        Book general = createBook("General", BookType.GENERAL, 5, 5);
        long dayMillis = 24L * 60L * 60L * 1000L;
        BorrowRecord onTime = createBorrowRecord(general, user, daysFromNow(-5), daysFromNow(5));
        onTime.setReturnDate(daysFromNow(-1));
        assertEquals(0.0, onTime.getFineAmount(), 0.0001);

        Date overdueDue = daysFromNow(-3);
        BorrowRecord overdue = createBorrowRecord(general, user, daysFromNow(-10), overdueDue);
        Date overdueReturn = new Date();
        overdue.setReturnDate(overdueReturn);
        long overdueDays = (overdueReturn.getTime() - overdueDue.getTime()) / dayMillis;
        assertEquals(overdueDays, (long) overdue.getFineAmount());

        Book rare = createBook("Rare", BookType.RARE, 2, 2);
        RegularUser blacklisted = new RegularUser("Black", "U2");
        blacklisted.setAccountStatus(AccountStatus.BLACKLISTED);
        rare.setDamaged(true);
        Date seriousDue = daysFromNow(-4);
        BorrowRecord serious = createBorrowRecord(rare, blacklisted, daysFromNow(-5), seriousDue);
        Date seriousReturn = new Date();
        serious.setReturnDate(seriousReturn);
        long days = (seriousReturn.getTime() - seriousDue.getTime()) / dayMillis;
        assertEquals(days * 60.0, serious.getFineAmount(), 0.0001);

        Book journal = createBook("Journal", BookType.JOURNAL, 3, 3);
        BorrowRecord journalRecord = createBorrowRecord(journal, user, daysFromNow(-4), daysFromNow(-2));
        Date journalReturn = new Date();
        journalRecord.setReturnDate(journalReturn);
        long journalDays = (journalReturn.getTime() - journalRecord.getDueDate().getTime()) / dayMillis;
        assertEquals(journalDays * 2.0, journalRecord.getFineAmount(), 0.0001);

        BorrowRecord extendRecord = createBorrowRecord(general, user, daysFromNow(-2), daysFromNow(10));
        Date originalDue = extendRecord.getDueDate();
        extendRecord.extendDueDate(7);
        long diff = (extendRecord.getDueDate().getTime() - originalDue.getTime()) / dayMillis;
        assertEquals(7L, diff);
    }

    @Test
    public void testUserPayFineAndScore() throws Exception {
        // 测试目的：验证基础用户类的罚金支付与积分增减逻辑；期望：异常与状态切换均符合规则。
        RegularUser user = new RegularUser("User", "U1");
        user.fines = 100;
        user.setAccountStatus(AccountStatus.BLACKLISTED);
        try {
            user.payFine(10);
            fail("黑名单用户不能缴纳罚款");
        } catch (IllegalStateException e) {
            assertEquals("", e.getMessage());
        }

        RegularUser user2 = new RegularUser("User2", "U2");
        user2.fines = 10;
        try {
            user2.payFine(20);
            fail("付款超过欠款应报错");
        } catch (IllegalArgumentException e) {
            assertEquals("If the user is on the blacklist, they cannot pay the fine.", e.getMessage());
        }

        RegularUser user3 = new RegularUser("User3", "U3");
        user3.fines = 20;
        user3.setAccountStatus(AccountStatus.FROZEN);
        user3.payFine(20);
        assertEquals(0.0, user3.getFines(), 0.0001);
        assertEquals(AccountStatus.ACTIVE, user3.getAccountStatus());

        RegularUser user4 = new RegularUser("User4", "U4");
        user4.setAccountStatus(AccountStatus.BLACKLISTED);
        try {
            user4.addScore(5);
            fail("黑名单用户不能增加积分");
        } catch (IllegalStateException e) {
            assertEquals("Blacklisted users cannot increase their credit score.", e.getMessage());
        }

        RegularUser user5 = new RegularUser("User5", "U5");
        user5.creditScore = 3;
        user5.deductScore(10);
        assertEquals(0, user5.getCreditScore());
        assertEquals(AccountStatus.FROZEN, user5.getAccountStatus());
    }

    @Test
    public void testUserPartialFinePaymentAndAddScore() {
        // 测试目的：验证部分缴纳罚金与正常加分场景；期望：欠款余额正确减少且账户状态保持有效。
        RegularUser user = new RegularUser("Partial", "PF1");
        user.fines = 30;
        user.payFine(10);
        assertEquals(20.0, user.getFines(), 0.0001);
        assertEquals(AccountStatus.ACTIVE, user.getAccountStatus());

        user.addScore(5);
        assertEquals(105, user.getCreditScore());
    }

    @Test
    public void testUserReservationWorkflow() throws Exception {
        // 测试目的：验证预约与取消预约流程；期望：账户状态、信用分等限制均能触发对应异常，成功预约后可正常取消。
        RegularUser user = new RegularUser("User", "U1");
        Book book = createBook("Reserve", BookType.GENERAL, 1, 1);

        user.setAccountStatus(AccountStatus.BLACKLISTED);
        try {
            user.reserveBook(book);
            fail("黑名单用户无法预约");
        } catch (IllegalStateException e) {
            assertEquals("Blacklisted users cannot reserve books.", e.getMessage());
        }

        user.setAccountStatus(AccountStatus.FROZEN);
        try {
            user.reserveBook(book);
            fail("冻结账户无法预约");
        } catch (AccountFrozenException e) {
            assertEquals("The account is frozen and books cannot be reserved.", e.getMessage());
        }

        user.setAccountStatus(AccountStatus.ACTIVE);
        user.creditScore = 40;
        try {
            user.reserveBook(book);
            fail("信用分不足应报错");
        } catch (InsufficientCreditException e) {
            assertEquals("Insufficient credit score. Cannot reserve books.", e.getMessage());
        }

        user.creditScore = 80;
        book.setInRepair(true);
        user.reserveBook(book);
        assertEquals(1, book.getReservationQueue().size());
        assertEquals(1, user.reservations.size());

        user.cancelReservation(book);
        assertEquals(0, book.getReservationQueue().size());
        assertEquals(0, user.reservations.size());

        try {
            user.cancelReservation(book);
            fail("未预约图书不能取消");
        } catch (InvalidOperationException e) {
            assertEquals("This book has not been reserved.", e.getMessage());
        }
    }

    @Test
    public void testUserReservationDuplicateRestriction() throws Exception {
        // 测试目的：验证重复预约拦截逻辑；期望：检测到同一图书已在预约列表时抛出限制异常。
        RegularUser user = new RegularUser("Dup", "RD1");
        user.creditScore = 80;
        Book book = createBook("DupBook", BookType.GENERAL, 1, 1);
        forceReservationEntry(user, book);
        try {
            user.reserveBook(book);
            fail("重复预约应抛出ReservationNotAllowedException");
        } catch (ReservationNotAllowedException e) {
            assertEquals("This book has already been reserved.", e.getMessage());
        }
    }

    @Test
    public void testRegularUserBorrowingBranches() throws Exception {
        // 测试目的：覆盖普通用户借书的各种限制条件；期望：不同约束下抛出对应异常，条件满足时成功借书。
        Book available = createBook("Normal", BookType.GENERAL, 2, 2);
        long dayMillis = 24L * 60L * 60L * 1000L;

        RegularUser blacklisted = new RegularUser("Black", "R1");
        blacklisted.setAccountStatus(AccountStatus.BLACKLISTED);
        try {
            blacklisted.borrowBook(available);
            fail("黑名单应禁止借书");
        } catch (IllegalStateException e) {
            assertEquals("Blacklisted users cannot borrow books.", e.getMessage());
        }

        RegularUser frozen = new RegularUser("Frozen", "R2");
        frozen.setAccountStatus(AccountStatus.FROZEN);
        try {
            frozen.borrowBook(available);
            fail("冻结账户应禁止借书");
        } catch (AccountFrozenException e) {
            assertEquals("The account is frozen and books cannot be borrowed.", e.getMessage());
        }

        RegularUser limit = new RegularUser("Limit", "R3");
        for (int i = 0; i < 5; i++) {
            limit.borrowedBooks.add(createBorrowRecord(available, limit, new Date(), daysFromNow(1)));
        }
        try {
            limit.borrowBook(available);
            fail("达到借阅上限应报错");
        } catch (InvalidOperationException e) {
            assertEquals("The maximum number of books borrowed has been reached.", e.getMessage());
        }

        RegularUser fineUser = new RegularUser("Fine", "R4");
        fineUser.fines = 60;
        try {
            fineUser.borrowBook(available);
            fail("罚金过高应冻结并报错");
        } catch (OverdueFineException e) {
            assertEquals(AccountStatus.FROZEN, fineUser.getAccountStatus());
            assertEquals("The fine is too high and the account has been frozen.", e.getMessage());
        }

        Book unavailable = createBook("Unavailable", BookType.GENERAL, 1, 0);
        RegularUser user = new RegularUser("User", "R5");
        try {
            user.borrowBook(unavailable);
            fail("库存不足应报错");
        } catch (BookNotAvailableException e) {
            assertEquals("The book is unavailable and cannot be borrowed.", e.getMessage());
        }

        RegularUser lowCredit = new RegularUser("Low", "R6");
        lowCredit.creditScore = 59;
        try {
            lowCredit.borrowBook(available);
            fail("信用不足应报错");
        } catch (InsufficientCreditException e) {
            assertEquals("The credit score is too low and books cannot be borrowed.", e.getMessage());
        }

        Book rare = createBook("Rare", BookType.RARE, 1, 1);
        RegularUser normal = new RegularUser("Normal", "R7");
        try {
            normal.borrowBook(rare);
            fail("普通用户借珍本应报错");
        } catch (InvalidOperationException e) {
            assertEquals("Ordinary users cannot borrow rare books.", e.getMessage());
        }

        RegularUser success = new RegularUser("Success", "R8");
        Book successBook = createBook("SuccessBook", BookType.GENERAL, 2, 2);
        success.borrowBook(successBook);
        assertEquals(1, success.getBorrowedBooks().size());
        BorrowRecord successRecord = success.getBorrowedBooks().get(0);
        long borrowSpan = (successRecord.getDueDate().getTime() - successRecord.getBorrowDate().getTime()) / dayMillis;
        assertEquals(14L, borrowSpan);
        assertEquals(1, successBook.getAvailableCopies());
        assertEquals(101, success.getCreditScore());
    }

    @Test
    public void testRegularUserBorrowTriggersReservation() throws Exception {
        // 测试目的：验证库存不足但可借时转入预约流程；期望：借阅请求转为预约并记录在用户与图书队列。
        RegularUser user = new RegularUser("ReserveFlow", "RR1");
        user.creditScore = 80;
        LenientBook book = new LenientBook("Lenient", BookType.GENERAL, 1, 0);
        user.borrowBook(book);
        assertEquals(0, user.getBorrowedBooks().size());
        assertEquals(1, book.getReservationQueue().size());
        assertEquals(1, user.reservations.size());
    }

    @Test
    public void testRegularUserReturningBranches() throws Exception {
        // 测试目的：验证普通用户还书时的不同分支；期望：未借记录报错，按时归还加分，逾期处理罚金及冻结逻辑正确。
        RegularUser user = new RegularUser("Return", "R9");
        Book book = createBook("ReturnBook", BookType.GENERAL, 1, 1);
        try {
            user.returnBook(book);
            fail("未借书时无法归还");
        } catch (InvalidOperationException e) {
            assertEquals("The book has not been borrowed.", e.getMessage());
        }

        RegularUser onTimeUser = new RegularUser("OnTime", "R10");
        Book onTimeBook = createBook("OnTimeBook", BookType.GENERAL, 2, 2);
        onTimeBook.setAvailableCopies(1);
        BorrowRecord record = createBorrowRecord(onTimeBook, onTimeUser, daysFromNow(-5), daysFromNow(5));
        onTimeUser.borrowedBooks.add(record);
        onTimeUser.returnBook(onTimeBook);
        assertEquals(0, onTimeUser.getBorrowedBooks().size());
        assertEquals(2, onTimeBook.getAvailableCopies());
        assertEquals(102, onTimeUser.getCreditScore());

        RegularUser overdueUser = new RegularUser("Overdue", "R11");
        Book overdueBook = createBook("OverdueBook", BookType.GENERAL, 1, 1);
        overdueBook.setAvailableCopies(0);
        BorrowRecord overdueRecord = createBorrowRecord(overdueBook, overdueUser, daysFromNow(-10), daysFromNow(-3));
        overdueUser.borrowedBooks.add(overdueRecord);
        overdueUser.fines = 10;
        overdueUser.returnBook(overdueBook);
        assertEquals(95, overdueUser.getCreditScore());
        assertEquals(0, overdueUser.getBorrowedBooks().size());

        RegularUser freezeUser = new RegularUser("Freeze", "R12");
        Book freezeBook = createBook("FreezeBook", BookType.GENERAL, 1, 1);
        freezeBook.setAvailableCopies(0);
        BorrowRecord freezeRecord = createBorrowRecord(freezeBook, freezeUser, daysFromNow(-10), daysFromNow(-5));
        freezeUser.borrowedBooks.add(freezeRecord);
        freezeUser.fines = 98;
        try {
            freezeUser.returnBook(freezeBook);
            fail("罚金超限应冻结并抛出异常");
        } catch (OverdueFineException e) {
            assertEquals(AccountStatus.FROZEN, freezeUser.getAccountStatus());
            assertTrue(freezeUser.getFines() > 100);
        }
    }

    @Test
    public void testVIPUserBorrowingBranches() throws Exception {
        // 测试目的：覆盖VIP用户借书的限制条件与成功路径；期望：触发限制时抛出异常，正常借书增加积分。
        Book available = createBook("VIPAvailable", BookType.GENERAL, 3, 3);
        long dayMillis = 24L * 60L * 60L * 1000L;

        VIPUser blacklisted = new VIPUser("Black", "V1");
        blacklisted.setAccountStatus(AccountStatus.BLACKLISTED);
        try {
            blacklisted.borrowBook(available);
            fail("黑名单VIP也不能借书");
        } catch (IllegalStateException e) {
            assertEquals("Blacklisted users cannot borrow books.", e.getMessage());
        }

        VIPUser frozen = new VIPUser("Frozen", "V2");
        frozen.setAccountStatus(AccountStatus.FROZEN);
        try {
            frozen.borrowBook(available);
            fail("冻结VIP不能借书");
        } catch (AccountFrozenException e) {
            assertEquals("The account is frozen and cannot borrow books.", e.getMessage());
        }

        VIPUser limit = new VIPUser("Limit", "V3");
        for (int i = 0; i < 10; i++) {
            limit.borrowedBooks.add(createBorrowRecord(available, limit, new Date(), daysFromNow(1)));
        }
        try {
            limit.borrowBook(available);
            fail("达到VIP借书上限");
        } catch (InvalidOperationException e) {
            assertEquals("The maximum number of books borrowed has been reached.", e.getMessage());
        }

        VIPUser fineUser = new VIPUser("Fine", "V4");
        fineUser.fines = 80;
        try {
            fineUser.borrowBook(available);
            fail("罚金超标应冻结");
        } catch (OverdueFineException e) {
            assertEquals(AccountStatus.FROZEN, fineUser.getAccountStatus());
            assertEquals("The fine is too high. The account has been frozen.", e.getMessage());
        }

        Book unavailable = createBook("Unavailable", BookType.GENERAL, 1, 0);
        VIPUser user = new VIPUser("User", "V5");
        try {
            user.borrowBook(unavailable);
            fail("库存不足应抛出异常");
        } catch (BookNotAvailableException e) {
            assertEquals("The book is unavailable and cannot be borrowed.", e.getMessage());
        }

        VIPUser lowCredit = new VIPUser("Low", "V6");
        lowCredit.creditScore = 40;
        try {
            lowCredit.borrowBook(available);
            fail("信用不足应报错");
        } catch (InsufficientCreditException e) {
            assertEquals("The credit score is too low and borrowing is not allowed.", e.getMessage());
        }

        VIPUser success = new VIPUser("Success", "V7");
        Book rare = createBook("RareVIP", BookType.RARE, 2, 2);
        success.borrowBook(rare);
        assertEquals(1, success.getBorrowedBooks().size());
        BorrowRecord vipRecord = success.getBorrowedBooks().get(0);
        long vipSpan = (vipRecord.getDueDate().getTime() - vipRecord.getBorrowDate().getTime()) / dayMillis;
        assertEquals(30L, vipSpan);
        assertEquals(1, rare.getAvailableCopies());
        assertEquals(102, success.getCreditScore());
    }

    @Test
    public void testVIPUserReturningAndExtension() throws Exception {
        // 测试目的：验证VIP用户还书与续期逻辑；期望：未借书报错，按时归还加分，逾期扣分或冻结，续期仅允许一次。
        VIPUser user = new VIPUser("Return", "V8");
        Book book = createBook("ReturnVIP", BookType.GENERAL, 1, 1);
        try {
            user.returnBook(book);
            fail("未借书无法归还");
        } catch (InvalidOperationException e) {
            assertEquals("This book has not been borrowed.", e.getMessage());
        }

        VIPUser onTime = new VIPUser("OnTime", "V9");
        Book onTimeBook = createBook("OnTimeVIP", BookType.GENERAL, 1, 1);
        onTimeBook.setAvailableCopies(0);
        BorrowRecord record = createBorrowRecord(onTimeBook, onTime, daysFromNow(-5), daysFromNow(5));
        onTime.borrowedBooks.add(record);
        onTime.returnBook(onTimeBook);
        assertEquals(0, onTime.getBorrowedBooks().size());
        assertEquals(103, onTime.getCreditScore());

        VIPUser overdue = new VIPUser("Overdue", "V10");
        Book overdueBook = createBook("OverdueVIP", BookType.GENERAL, 1, 1);
        overdueBook.setAvailableCopies(0);
        BorrowRecord overdueRecord = createBorrowRecord(overdueBook, overdue, daysFromNow(-10), daysFromNow(-4));
        overdue.borrowedBooks.add(overdueRecord);
        overdue.fines = 20;
        overdue.returnBook(overdueBook);
        assertEquals(97, overdue.getCreditScore());
        assertEquals(AccountStatus.ACTIVE, overdue.getAccountStatus());

        VIPUser freeze = new VIPUser("Freeze", "V11");
        Book freezeBook = createBook("FreezeVIP", BookType.GENERAL, 1, 1);
        freezeBook.setAvailableCopies(0);
        BorrowRecord freezeRecord = createBorrowRecord(freezeBook, freeze, daysFromNow(-15), daysFromNow(-10));
        freeze.borrowedBooks.add(freezeRecord);
        freeze.fines = 99;
        try {
            freeze.returnBook(freezeBook);
            fail("罚金超限应冻结");
        } catch (OverdueFineException e) {
            assertEquals(AccountStatus.FROZEN, freeze.getAccountStatus());
        }

        VIPUser extend = new VIPUser("Extend", "V12");
        Book extendBook = createBook("ExtendVIP", BookType.GENERAL, 1, 1);
        BorrowRecord extendRecord = createBorrowRecord(extendBook, extend, new Date(), daysFromNow(10));
        extend.borrowedBooks.add(extendRecord);
        Date originalDue = extendRecord.getDueDate();
        extend.extendBorrowPeriod(extendBook);
        long diff = (extendRecord.getDueDate().getTime() - originalDue.getTime()) / (24L * 60L * 60L * 1000L);
        assertEquals(7L, diff);
        try {
            extend.extendBorrowPeriod(extendBook);
            fail("重复续期应抛出异常");
        } catch (InvalidOperationException e) {
            assertEquals("This book has already been renewed.", e.getMessage());
        }

        VIPUser noRecord = new VIPUser("NoRecord", "V13");
        try {
            noRecord.extendBorrowPeriod(extendBook);
            fail("未借书不能续期");
        } catch (InvalidOperationException e) {
            assertEquals("This book has not been borrowed.", e.getMessage());
        }
    }

    @Test
    public void testAutoRenewalServiceBranches() throws Exception {
        // 测试目的：验证自动续借服务的各类失败分支及成功续期；期望：状态、预约、信用等条件均触发对应异常，成功续期延长14天。
        AutoRenewalService service = new AutoRenewalService();
        Book book = createBook("Auto", BookType.GENERAL, 2, 2);
        RegularUser user = new RegularUser("User", "A1");

        user.setAccountStatus(AccountStatus.FROZEN);
        try {
            service.autoRenew(user, book);
            fail("冻结账户续借应失败");
        } catch (AccountFrozenException e) {
            assertEquals("The account is frozen and cannot be automatically renewed.", e.getMessage());
        }

        user.setAccountStatus(AccountStatus.ACTIVE);
        book.addReservation(new Reservation(book, new RegularUser("R", "A2")));
        try {
            service.autoRenew(user, book);
            fail("被预约的图书不允许续借");
        } catch (InvalidOperationException e) {
            assertEquals("The book has been reserved by other users and cannot be renewed.", e.getMessage());
        }
        book.getReservationQueue().clear();

        user.creditScore = 50;
        try {
            service.autoRenew(user, book);
            fail("信用不足应抛InsufficientCreditException");
        } catch (InsufficientCreditException e) {
            assertEquals("The credit score is too low to renew the loan.", e.getMessage());
        }

        user.creditScore = 80;
        try {
            service.autoRenew(user, book);
            fail("无借阅记录应抛InvalidOperationException");
        } catch (InvalidOperationException e) {
            assertEquals("The borrowing record of this book is not found.", e.getMessage());
        }

        BorrowRecord record = createBorrowRecord(book, user, new Date(), daysFromNow(5));
        user.borrowedBooks.add(record);
        long originalDueMillis = record.getDueDate().getTime();
        service.autoRenew(user, book);
        long diff = (record.getDueDate().getTime() - originalDueMillis) / (24L * 60L * 60L * 1000L);
        assertEquals(14L, diff);
    }

    @Test
    public void testBaseUserFindBorrowRecordDelegation() {
        // 测试目的：验证抽象用户基类的借阅记录查找逻辑；期望：基类实现能正确匹配借阅图书并在未命中时返回null。
        User baseUser = new User("Base", "BU1", UserType.REGULAR) {
            @Override
            public void borrowBook(Book book) {
                // 测试桩实现，保持借阅列表由测试用例控制。
            }

            @Override
            public void returnBook(Book book) {
                // 测试桩实现，保持借阅列表由测试用例控制。
            }
        };
        Book booked = createBook("BaseBook", BookType.GENERAL, 1, 1);
        BorrowRecord record = createBorrowRecord(booked, baseUser, new Date(), daysFromNow(2));
        baseUser.borrowedBooks.add(record);
        assertSame(record, baseUser.findBorrowRecord(booked));

        Book other = createBook("OtherBaseBook", BookType.GENERAL, 1, 1);
        assertNull(baseUser.findBorrowRecord(other));
    }

    @Test
    public void testCreditRepairService() throws Exception {
        // 测试目的：验证信用修复服务的金额限制与状态恢复；期望：金额不足抛异常，满足条件时积分提升并恢复账户状态。
        CreditRepairService service = new CreditRepairService();
        RegularUser user = new RegularUser("Credit", "C1");
        try {
            service.repairCredit(user, 5);
            fail("金额不足应抛InvalidOperationException");
        } catch (InvalidOperationException e) {
            assertEquals("The minimum payment amount is 10 yuan.", e.getMessage());
        }

        user.creditScore = 58;
        user.setAccountStatus(AccountStatus.FROZEN);
        service.repairCredit(user, 20);
        assertEquals(60, user.getCreditScore());
        assertEquals(AccountStatus.ACTIVE, user.getAccountStatus());
    }

    @Test
    public void testInventoryServiceBranches() throws Exception {
        // 测试目的：覆盖库存服务的报失与报修逻辑；期望：未借书时抛异常，成功时更新库存和罚金。
        InventoryService service = new InventoryService();
        Book book = createBook("Inventory", BookType.GENERAL, 3, 3);
        RegularUser user = new RegularUser("User", "I1");

        try {
            service.reportLost(book, user);
            fail("未借书不能报失");
        } catch (InvalidOperationException e) {
            assertEquals("The user has not borrowed this book and cannot report it as lost.", e.getMessage());
        }

        try {
            service.reportDamaged(book, user);
            fail("未借书不能报损");
        } catch (InvalidOperationException e) {
            assertEquals("The user has not borrowed this book and cannot report it as damaged.", e.getMessage());
        }

        BorrowRecord record = createBorrowRecord(book, user, new Date(), daysFromNow(5));
        user.borrowedBooks.add(record);
        // 复杂逻辑说明：为匹配 contains(book) 判断，借助工具方法向列表插入 Book 对象。
        forceBorrowedBook(user, book);
        user.fines = 500;
        book.setAvailableCopies(2);
        service.reportLost(book, user);
        assertEquals(2, book.getTotalCopies());
        assertEquals(1, book.getAvailableCopies());
        assertEquals(350.0, user.getFines(), 0.0001);

        BorrowRecord record2 = createBorrowRecord(book, user, new Date(), daysFromNow(5));
        user.borrowedBooks.add(record2);
        forceBorrowedBook(user, book);
        user.fines = 50;
        service.reportDamaged(book, user);
        assertFalse(book.isAvailable());
        assertEquals(20.0, user.getFines(), 0.0001);
    }

    @Test
    public void testNotificationServiceFallbacks() throws Exception {
        // 测试目的：验证通知服务的多级兜底策略；期望：黑名单直接返回，邮箱失败时转短信，短信失败时转应用内。
        NotificationService service = new NotificationService();
        RegularUser blacklisted = new RegularUser("Black", "N1");
        blacklisted.setAccountStatus(AccountStatus.BLACKLISTED);
        service.sendNotification(blacklisted, "msg");

        RegularUser emailUser = new RegularUser("Email", "N2");
        emailUser.setEmail("user@example.com");
        service.sendNotification(emailUser, "欢迎");

        RegularUser smsUser = new RegularUser("SMS", "N3");
        smsUser.setEmail(null);
        smsUser.setPhoneNumber("123456");
        service.sendNotification(smsUser, "短信兜底");

        RegularUser appUser = new RegularUser("App", "N4");
        appUser.setEmail("");
        appUser.setPhoneNumber("");
        service.sendNotification(appUser, "应用内兜底");

        try {
            service.sendEmail(null, "test");
            fail("缺少邮箱应抛异常");
        } catch (EmailException e) {
            assertEquals("The user does not have an email address.", e.getMessage());
        }

        try {
            service.sendSMS(null, "test");
            fail("缺少手机号应抛异常");
        } catch (SMSException e) {
            assertEquals("The user does not have a phone number.", e.getMessage());
        }
    }

    @Test
    public void testNotificationServiceFallbackOutputs() {
        // 测试目的：通过捕获日志验证邮件、短信与应用兜底依次执行；期望：输出包含各级兜底提示与成功信息。
        NotificationService service = new NotificationService();
        RegularUser emailUser = new RegularUser("EmailOk", "NL1");
        emailUser.setEmail("ok@example.com");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(output));
        try {
            service.sendNotification(emailUser, "邮件消息");

            RegularUser smsFallback = new RegularUser("SmsFallback", "NL2");
            smsFallback.setEmail(null);
            smsFallback.setPhoneNumber("555888");
            service.sendNotification(smsFallback, "短信消息");

            RegularUser appFallback = new RegularUser("AppFallback", "NL3");
            appFallback.setEmail("");
            appFallback.setPhoneNumber("");
            service.sendNotification(appFallback, "应用消息");
        } finally {
            System.setOut(original);
        }
        String logs = output.toString();
        assertTrue(logs.contains("Successfully sent email to ok@example.com: 邮件消息"));
        assertTrue(logs.contains("Email sending failed. Try sending a text message..."));
        assertTrue(logs.contains("Successfully sent text message to.555888: 短信消息"));
        assertTrue(logs.contains("Text message sending failed. Try using in-app notifications..."));
        assertTrue(logs.contains("Send an in-app notification to the user. [AppFallback]: 应用消息"));
    }

    @Test
    public void testUserReceiveNotificationBranches() {
        // 测试目的：验证用户接收通知时的黑名单分支与正常分支；期望：黑名单仅输出警告，正常用户输出通知内容。
        RegularUser blacklisted = new RegularUser("NotifyBlack", "NB1");
        blacklisted.setAccountStatus(AccountStatus.BLACKLISTED);
        RegularUser active = new RegularUser("NotifyActive", "NA1");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(output));
        try {
            blacklisted.receiveNotification("黑名单消息");
            active.receiveNotification("正常消息");
        } finally {
            System.setOut(original);
        }
        String logs = output.toString();
        assertTrue(logs.contains("Blacklisted users cannot receive notifications."));
        assertTrue(logs.contains("Notify user [NotifyActive]: 正常消息"));
    }

    @Test
    public void testReservationPriorityCalculation() {
        // 测试目的：验证预约优先级计算规则；期望：VIP额外加分、逾期扣分、黑名单禁止预约。
        Book book = createBook("ReservePriority", BookType.GENERAL, 1, 1);
        VIPUser vip = new VIPUser("VIP", "RP1");
        Reservation vipReservation = new Reservation(book, vip);
        assertEquals(110, vipReservation.getPriority());

        RegularUser overdueUser = new RegularUser("Overdue", "RP2");
        BorrowRecord overdueRecord = createBorrowRecord(book, overdueUser, daysFromNow(-10), daysFromNow(-5));
        overdueRecord.setReturnDate(daysFromNow(-3));
        overdueUser.borrowedBooks.add(overdueRecord);
        Reservation overdueReservation = new Reservation(book, overdueUser);
        assertEquals(overdueUser.getCreditScore() - 5, overdueReservation.getPriority());

        RegularUser blacklisted = new RegularUser("Black", "RP3");
        blacklisted.setAccountStatus(AccountStatus.BLACKLISTED);
        Reservation forbidden = new Reservation(book, blacklisted);
        assertEquals(-1, forbidden.getPriority());
    }

    @Test
    public void testLibraryUserAndBookManagement() throws Exception {
        // 测试目的：验证图书馆注册用户与添加图书的分支；期望：低信用拒绝注册、重复注册与重复添书不重复入库。
        Library library = new Library();
        // 复杂逻辑说明：通过反射读取私有集合以断言注册与入库结果。
        List<User> users = getPrivateField(library, "users");
        List<Book> books = getPrivateField(library, "books");

        RegularUser lowCredit = new RegularUser("Low", "L1");
        lowCredit.creditScore = 40;
        lowCredit.setAccountStatus(AccountStatus.ACTIVE);
        library.registerUser(lowCredit);
        assertEquals(0, users.size());

        RegularUser normal = new RegularUser("Normal", "L2");
        library.registerUser(normal);
        assertEquals(1, users.size());
        library.registerUser(normal);
        assertEquals(1, users.size());

        Book book = createBook("LibraryBook", BookType.GENERAL, 2, 2);
        library.addBook(book);
        assertEquals(1, books.size());
        library.addBook(book);
        assertEquals(1, books.size());
    }

    @Test
    public void testLibraryOperationalFlows() throws Exception {
        // 测试目的：覆盖图书馆业务操作流程；期望：预约处理、自动续借、信用修复及报损报失均按服务逻辑执行。
        Library library = new Library();
        Book book = createBook("Ops", BookType.GENERAL, 2, 2);
        Book unavailable = createBook("OpsUnavailable", BookType.GENERAL, 1, 0);

        book.setInRepair(true);
        library.processReservations(book);
        book.setInRepair(false);
        library.processReservations(book);

        TestUser successUser = new TestUser("Success", "T1");
        successUser.setEmail("success@example.com");
        Reservation reservation = new Reservation(book, successUser);
        book.addReservation(reservation);
        library.processReservations(book);
        assertTrue(successUser.borrowInvoked);
        assertEquals(0, book.getReservationQueue().size());

        TestUser failUser = new TestUser("Fail", "T2");
        failUser.borrowException = new InvalidOperationException("fail");
        Reservation reservation2 = new Reservation(book, failUser);
        book.addReservation(reservation2);
        library.processReservations(book);
        assertEquals(0, book.getReservationQueue().size());

        RegularUser renewUser = new RegularUser("Renew", "T3");
        renewUser.borrowedBooks.clear();
        Book renewBook = createBook("RenewBook", BookType.GENERAL, 1, 1);
        BorrowRecord renewRecord = createBorrowRecord(renewBook, renewUser, new Date(), daysFromNow(5));
        renewUser.borrowedBooks.add(renewRecord);
        long renewOriginal = renewRecord.getDueDate().getTime();
        library.autoRenewBook(renewUser, renewBook);
        Date afterSuccessDue = renewRecord.getDueDate();
        long diff = (afterSuccessDue.getTime() - renewOriginal) / (24L * 60L * 60L * 1000L);
        assertEquals(14L, diff);

        renewUser.setAccountStatus(AccountStatus.FROZEN);
        library.autoRenewBook(renewUser, renewBook);
        assertEquals(afterSuccessDue.getTime(), renewRecord.getDueDate().getTime());
        renewUser.setAccountStatus(AccountStatus.ACTIVE);

        RegularUser repairUser = new RegularUser("Repair", "T4");
        repairUser.setAccountStatus(AccountStatus.FROZEN);
        repairUser.creditScore = 58;
        library.repairUserCredit(repairUser, 20);
        assertEquals(AccountStatus.ACTIVE, repairUser.getAccountStatus());
        library.repairUserCredit(repairUser, 5);
        assertEquals(AccountStatus.ACTIVE, repairUser.getAccountStatus());

        RegularUser reportUser = new RegularUser("Report", "T5");
        BorrowRecord lostRecord = createBorrowRecord(book, reportUser, new Date(), daysFromNow(3));
        reportUser.borrowedBooks.add(lostRecord);
        // 复杂逻辑说明：为触发库存服务成功路径，同样借助工具方法插入 Book。
        forceBorrowedBook(reportUser, book);
        reportUser.fines = 500;
        book.setAvailableCopies(1);
        library.reportLostBook(reportUser, book);
        assertEquals(1, book.getTotalCopies());

        library.reportLostBook(reportUser, unavailable);
        BorrowRecord damageRecord = createBorrowRecord(book, reportUser, new Date(), daysFromNow(3));
        reportUser.borrowedBooks.add(damageRecord);
        forceBorrowedBook(reportUser, book);
        reportUser.fines = 30;
        library.reportDamagedBook(reportUser, book);
        library.reportDamagedBook(reportUser, unavailable);
    }

    @Test
    public void testExternalLibraryApi() {
        // 测试目的：验证外部图书馆接口的可用性查询随机性与请求流程；期望：随机结果涵盖true/false且请求方法无异常。
        Set<Boolean> results = new HashSet<>();
        for (int i = 0; i < 1000 && results.size() < 2; i++) {
            results.add(ExternalLibraryAPI.checkAvailability("外部图书"));
        }
        assertTrue(results.contains(Boolean.TRUE));
        assertTrue(results.contains(Boolean.FALSE));

        ExternalLibraryAPI.requestBook("UID", "外部图书");
    }

    @Test
    public void testCustomExceptionMessages() {
        // 测试目的：实例化未在业务流程中触发的异常类型；期望：确保构造器与消息正常。
        BlacklistedUserException blacklisted = new BlacklistedUserException("黑名单");
        assertEquals("黑名单", blacklisted.getMessage());

        ReservationNotAllowedException reservation = new ReservationNotAllowedException("不允许预约");
        assertEquals("不允许预约", reservation.getMessage());
    }
}
