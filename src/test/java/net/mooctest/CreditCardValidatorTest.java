package net.mooctest;

import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class CreditCardValidatorTest {

    private String buildNumber(String prefix, int totalLength) {
        StringBuilder builder = new StringBuilder(prefix);
        while (builder.length() < totalLength) {
            builder.append('0');
        }
        return builder.toString();
    }

    @Test
    public void testBuildNumberPaddingAndNonPadding() {
        // 用例目的：验证辅助方法在不同长度下的行为，预期当长度不足时补零，长度充足时保持原样。
        assertEquals("123450000", buildNumber("12345", 9));
        assertEquals("123456", buildNumber("123456", 3));
    }

    @Test(expected = NegativeArraySizeException.class)
    public void testBuildNumberNegativeLengthThrows() {
        // 用例目的：验证辅助方法对非法长度的处理，预期抛出NegativeArraySizeException以覆盖异常路径。
        buildNumber("123", -1);
    }

    private static class StubCardValidator extends CreditCardValidator {
        private final boolean lengthResult;
        private final boolean iinResult;
        private int lengthCallCount;
        private int iinCallCount;

        StubCardValidator(boolean lengthResult, boolean iinResult) {
            this.lengthResult = lengthResult;
            this.iinResult = iinResult;
        }

        @Override
        boolean checkLength() {
            lengthCallCount++;
            return lengthResult;
        }

        @Override
        boolean checkIINRanges() {
            iinCallCount++;
            return iinResult;
        }
    }

    @Test
    public void testCreditCardParserParsesDigitsCorrectly() {
        // 用例目的：验证数字解析工具能够正确拆分字符串并抽取指定长度的IIN，预期返回的列表和数值与输入一致。
        List<Integer> numbers = CreditCardParser.parseNumber("12345");
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), numbers);
        assertEquals(123, CreditCardParser.parseIIN(numbers, 3));
    }

    @Test
    public void testCreditCardParserHandlesLeadingZeros() {
        // 用例目的：验证解析器在处理前导零时的表现，预期IIN仍旧能够正确转换为整数1。
        List<Integer> numbers = CreditCardParser.parseNumber("0000012345");
        assertEquals(Integer.valueOf(0), numbers.get(0));
        assertEquals(1, CreditCardParser.parseIIN(numbers, 6));
    }

    @Test
    public void testCreditCardParserHandlesAlphabeticDigit() {
        // 用例目的：验证解析器能够识别字母字符转换成对应数值，预期A被识别为10。
        List<Integer> numbers = CreditCardParser.parseNumber("1A");
        assertEquals(Arrays.asList(1, 10), numbers);
    }

    @Test
    public void testCreditCardParserHandlesEmptyInput() {
        // 用例目的：验证解析器在空字符串输入时的表现，预期返回的集合为空避免越界。
        List<Integer> numbers = CreditCardParser.parseNumber("");
        assertTrue(numbers.isEmpty());
    }

    @Test(expected = StringIndexOutOfBoundsException.class)
    public void testCreditCardParserParseIINWithInsufficientDigits() {
        // 用例目的：验证IIN解析在数据不足时的抛异常行为，预期触发StringIndexOutOfBoundsException。
        List<Integer> numbers = CreditCardParser.parseNumber("12");
        CreditCardParser.parseIIN(numbers, 3);
    }

    @Test
    public void testDateParserRemoveSlashBranch() {
        // 用例目的：验证日期解析时能够去除斜杠并正确分割月份和年份，预期得到的数值分别为12和34。
        StringBuilder builder = new StringBuilder("12/34");
        StringBuilder parsed = DateParser.parseDate(builder);
        assertEquals("1234", parsed.toString());
        assertEquals(12, DateParser.parseDate(parsed, 0, 2));
        assertEquals(34, DateParser.parseDate(parsed, 2, 4));
    }

    @Test
    public void testDateParserWithoutSlashBranch() {
        // 用例目的：验证无斜杠日期保持原值的场景，预期解析后字符串保持不变。
        StringBuilder builder = new StringBuilder("1124");
        StringBuilder parsed = DateParser.parseDate(builder);
        assertEquals("1124", parsed.toString());
    }

    @Test
    public void testDateParserOnlyRemovesFirstSlash() {
        // 用例目的：验证日期解析仅删除首个分隔符的逻辑，预期第二个斜杠保留以检测变异。
        StringBuilder builder = new StringBuilder("1/2/34");
        StringBuilder parsed = DateParser.parseDate(builder);
        assertEquals("12/34", parsed.toString());
    }

    @Test(expected = NumberFormatException.class)
    public void testDateParserParseDateWithNonDigits() {
        // 用例目的：验证日期分割在存在非法字符时的行为，预期抛出NumberFormatException。
        StringBuilder builder = new StringBuilder("1A34");
        DateParser.parseDate(builder, 0, 2);
    }

    @Test
    public void testDateFormatterPatterns() {
        // 用例目的：验证日期格式化器提供的格式模式，预期年份模式为yy，月份模式为MM。
        SimpleDateFormat yearFormat = (SimpleDateFormat) DateFormatter.yearFormat();
        SimpleDateFormat monthFormat = (SimpleDateFormat) DateFormatter.monthFormat();
        assertEquals("yy", yearFormat.toPattern());
        assertEquals("MM", monthFormat.toPattern());
    }

    @Test
    public void testDateCheckerCompareDatesBranches() {
        // 用例目的：验证日期比较的大小分支，预期大于时返回真，小于或等于时返回假。
        assertTrue(DateChecker.compareDates(10, 5));
        assertFalse(DateChecker.compareDates(5, 10));
        assertFalse(DateChecker.compareDates(5, 5));
    }

    @Test
    public void testDateCheckerUtilities() {
        // 用例目的：验证日期工具的转换和当前时间获取逻辑，预期转换后字符串一致，当前年月合法。
        String raw = "11/24";
        StringBuilder converted = DateChecker.convertDate(raw);
        assertEquals(raw, converted.toString());
        assertEquals(DateChecker.CURRENT_YEAR, DateChecker.getCurrentYear());
        assertEquals(DateChecker.CURRENT_MONTH, DateChecker.getCurrentMonth());
    }

    @Test
    public void testLuhnValidatorSumOfDigits() {
        // 用例目的：验证双倍后拆位求和的逻辑，预期数字18的各位和为9。
        LuhnValidator validator = new LuhnValidator();
        assertEquals(9, validator.sumOfDigits(18));
    }

    @Test
    public void testLuhnValidatorAlgorithmValid() {
        // 用例目的：验证Luhn算法在合法号码上的判定结果，预期返回真。
        LuhnValidator validator = new LuhnValidator();
        assertTrue(validator.algorithmCheck("79927398713"));
    }

    @Test
    public void testLuhnValidatorAlgorithmInvalid() {
        // 用例目的：验证Luhn算法在非法号码上的判定结果，预期返回假。
        LuhnValidator validator = new LuhnValidator();
        assertFalse(validator.algorithmCheck("79927398714"));
    }

    @Test
    public void testLuhnValidatorValidateWrapper() {
        // 用例目的：验证validate方法对算法结果的封装，预期合法返回true，非法返回false。
        LuhnValidator validator = new LuhnValidator();
        assertTrue(validator.validate("79927398713"));
        assertFalse(validator.validate("79927398714"));
    }

    @Test
    public void testLuhnValidatorSingleDigitCard() {
        // 用例目的：验证极短卡号在算法中的表现，预期单个数字0仍被视为有效。
        LuhnValidator validator = new LuhnValidator();
        assertTrue(validator.validate("0"));
    }

    @Test
    public void testLuhnValidatorSumOfDigitsSingleDigit() {
        // 用例目的：验证sumOfDigits在处理个位数时的分支逻辑，预期数值保持不变。
        LuhnValidator validator = new LuhnValidator();
        assertEquals(7, validator.sumOfDigits(7));
    }

    @Test(expected = NumberFormatException.class)
    public void testLuhnValidatorAlgorithmRejectsNonDigits() {
        // 用例目的：验证算法在包含非数字字符时的行为，预期抛出NumberFormatException。
        LuhnValidator validator = new LuhnValidator();
        validator.algorithmCheck("12A3");
    }

    @Test
    public void testAbstractValidatorSuccessfulValidation() {
        // 用例目的：验证抽象验证器在双检查均成功时返回true，预期方法调用各一次且结果为真。
        StubCardValidator validator = new StubCardValidator(true, true);
        assertTrue(validator.validate());
        assertEquals(1, validator.lengthCallCount);
        assertEquals(1, validator.iinCallCount);
    }

    @Test
    public void testAbstractValidatorLengthFailureStillCallsIIN() {
        // 用例目的：验证长度失败时依然会执行IIN校验，预期最终结果为假且IIN方法调用一次。
        StubCardValidator validator = new StubCardValidator(false, true);
        assertFalse(validator.validate());
        assertEquals(1, validator.lengthCallCount);
        assertEquals(1, validator.iinCallCount);
    }

    @Test
    public void testAbstractValidatorIINFailure() {
        // 用例目的：验证IIN校验失败导致整体失败，预期结果为假且调用计数正确。
        StubCardValidator validator = new StubCardValidator(true, false);
        assertFalse(validator.validate());
        assertEquals(1, validator.lengthCallCount);
        assertEquals(1, validator.iinCallCount);
    }

    @Test
    public void testAbstractValidatorBothChecksFailure() {
        // 用例目的：验证双重校验均失败时的返回值，预期validate结果为false并记录调用次数。
        StubCardValidator validator = new StubCardValidator(false, false);
        assertFalse(validator.validate());
        assertEquals(1, validator.lengthCallCount);
        assertEquals(1, validator.iinCallCount);
    }

    @Test
    public void testStubCardValidatorMultipleInvocations() {
        // 用例目的：验证重复调用时计数累加行为，预期每次调用都会增加计数。
        StubCardValidator validator = new StubCardValidator(true, true);
        assertTrue(validator.validate());
        assertTrue(validator.validate());
        assertEquals(2, validator.lengthCallCount);
        assertEquals(2, validator.iinCallCount);
    }

    @Test
    public void testInvalidCardExceptionMessage() {
        // 用例目的：验证自定义异常的层级与提示信息，预期为IllegalArgumentException子类且消息匹配。
        InvalidCardException exception = new InvalidCardException("msg");
        assertTrue(exception instanceof IllegalArgumentException);
        assertEquals("msg", exception.getMessage());
    }

    @Test
    public void testValidatorValidateSuccess() {
        // 用例目的：验证Validator在合法号码上的整体校验过程，预期通过Luhn并返回真。
        Validator validator = new Validator("79927398713");
        assertTrue(validator.validate());
    }

    @Test
    public void testValidatorValidateFailure() {
        // 用例目的：验证Validator在非法号码时抛出自定义异常，预期捕获InvalidCardException。
        Validator validator = new Validator("79927398714");
        try {
            validator.validate();
            fail("应当抛出InvalidCardException");
        } catch (InvalidCardException ex) {
            assertEquals("This card isn't invalid", ex.getMessage());
        }
    }

    @Test
    public void testValidatorCheckCVVValidLengths() {
        // 用例目的：验证CVV长度为3或4时通过校验，预期返回真。
        Validator validatorThree = new Validator("1234567890123456", "01/50", "123");
        assertTrue(validatorThree.checkCVV());
        Validator validatorFour = new Validator("1234567890123456", "01/50", "1234");
        assertTrue(validatorFour.checkCVV());
    }

    @Test
    public void testValidatorCheckCVVInvalidLength() {
        // 用例目的：验证CVV长度非法时返回假，预期长度两位时检查失败。
        Validator validator = new Validator("1234567890123456", "01/50", "12");
        assertFalse(validator.checkCVV());
    }

    @Test
    public void testValidatorCheckCVVZeroLength() {
        // 用例目的：验证CVV为空字符串时的处理，预期返回false防止通过校验。
        Validator validator = new Validator("1234567890123456", "01/50", "");
        assertFalse(validator.checkCVV());
    }

    @Test
    public void testValidatorValidateWithExpirationData() throws InvalidCardException {
        // 用例目的：验证带有效期和CVV构造的校验流程，预期合法卡号仍能够通过Luhn校验。
        Validator validator = new Validator("79927398713", "12/40", "999");
        assertTrue(validator.validate());
    }

    @Test
    public void testValidatorExpirationDateFutureYear() {
        // 用例目的：验证年份超前时到期校验通过，预期返回真。
        int currentYear = DateChecker.CURRENT_YEAR;
        int futureYear = currentYear < 99 ? currentYear + 1 : currentYear;
        String expiration = String.format("%02d/%02d", DateChecker.CURRENT_MONTH, futureYear);
        Validator validator = new Validator("79927398713", expiration, "123");
        assertTrue(validator.checkExpirationDate());
    }

    @Test
    public void testValidatorExpirationDateFutureMonth() {
        // 用例目的：验证年份相同但月份更大时到期校验通过，预期返回真。
        int currentYear = DateChecker.CURRENT_YEAR;
        int currentMonth = DateChecker.CURRENT_MONTH;
        int futureMonth = currentMonth == 12 ? 13 : currentMonth + 1;
        String expiration = String.format("%02d/%02d", futureMonth, currentYear);
        Validator validator = new Validator("79927398713", expiration, "123");
        if (futureMonth > currentMonth) {
            assertTrue(validator.checkExpirationDate());
        } else {
            assertTrue("当月份等于13时，应因月份大于当前月份而有效", validator.checkExpirationDate());
        }
    }

    @Test
    public void testValidatorExpirationDateExpired() {
        // 用例目的：验证过期时间领先条件不足时返回假，预期年份和月份均不大时检查失败。
        int currentYear = DateChecker.CURRENT_YEAR;
        int currentMonth = DateChecker.CURRENT_MONTH;
        int pastYear = currentYear == 0 ? 0 : currentYear - 1;
        int pastMonth = currentMonth == 1 ? 1 : currentMonth - 1;
        String expiration = String.format("%02d/%02d", pastMonth, pastYear);
        Validator validator = new Validator("79927398713", expiration, "123");
        assertFalse(validator.checkExpirationDate());
    }

    @Test
    public void testValidatorParsesExpirationFields() throws Exception {
        // 用例目的：验证构造函数能够正确解析到期年月，预期字段值分别为11和28。
        Validator validator = new Validator("79927398713", "11/28", "123");
        java.lang.reflect.Field dateField = Validator.class.getDeclaredField("expirationDate");
        dateField.setAccessible(true);
        StringBuilder stored = (StringBuilder) dateField.get(validator);
        assertEquals("1128", stored.toString());
        java.lang.reflect.Field monthField = Validator.class.getDeclaredField("expirationMonth");
        monthField.setAccessible(true);
        java.lang.reflect.Field yearField = Validator.class.getDeclaredField("expirationYear");
        yearField.setAccessible(true);
        assertEquals(Integer.valueOf(11), (Integer) monthField.get(validator));
        assertEquals(Integer.valueOf(28), (Integer) yearField.get(validator));
    }

    @Test
    public void testValidatorResetExpirationFields() throws Exception {
        // 用例目的：验证手动更新日期后重新计算月份和年份的逻辑，预期字段更新为12和25。
        Validator validator = new Validator("79927398713", "01/20", "123");
        java.lang.reflect.Field dateField = Validator.class.getDeclaredField("expirationDate");
        dateField.setAccessible(true);
        dateField.set(validator, new StringBuilder("1225"));
        validator.setExpirationMonth();
        validator.setExpirationYear();
        java.lang.reflect.Field monthField = Validator.class.getDeclaredField("expirationMonth");
        monthField.setAccessible(true);
        java.lang.reflect.Field yearField = Validator.class.getDeclaredField("expirationYear");
        yearField.setAccessible(true);
        assertEquals(Integer.valueOf(12), (Integer) monthField.get(validator));
        assertEquals(Integer.valueOf(25), (Integer) yearField.get(validator));
    }

    @Test
    public void testVisaValidatorLengthAndIIN() {
        // 用例目的：验证Visa卡长度与IIN判断逻辑，预期合法长度和IIN为真，非法为假。
        VisaValidator valid = new VisaValidator(buildNumber("4", 16));
        assertTrue(valid.checkLength());
        assertTrue(valid.checkIINRanges());

        VisaValidator shortCard = new VisaValidator(buildNumber("4", 12));
        assertFalse(shortCard.checkLength());

        VisaValidator wrongIIN = new VisaValidator(buildNumber("5", 16));
        assertFalse(wrongIIN.checkIINRanges());
    }

    @Test
    public void testAmericanExpressValidatorLengthAndIIN() {
        // 用例目的：验证美国运通卡的长度和IIN区间，预期34开头的15位号码通过，其它情况失败。
        AmericanExpressValidator valid = new AmericanExpressValidator(buildNumber("34", 15));
        assertTrue(valid.checkLength());
        assertTrue(valid.checkIINRanges());

        AmericanExpressValidator wrongLength = new AmericanExpressValidator(buildNumber("34", 14));
        assertFalse(wrongLength.checkLength());

        AmericanExpressValidator wrongIIN = new AmericanExpressValidator(buildNumber("39", 15));
        assertFalse(wrongIIN.checkIINRanges());
    }

    @Test
    public void testMasterCardValidatorIINRanges() {
        // 用例目的：验证万事达卡的双IIN区间逻辑，预期两种区间均能命中，同时非法值应失败。
        MasterCardValidator firstRange = new MasterCardValidator(buildNumber("51", 16));
        assertTrue(firstRange.checkLength());
        assertTrue(firstRange.checkIINRanges());

        MasterCardValidator secondRange = new MasterCardValidator(buildNumber("222100", 16));
        assertTrue(secondRange.checkIINRanges());

        MasterCardValidator wrongLength = new MasterCardValidator(buildNumber("51", 15));
        assertFalse(wrongLength.checkLength());

        MasterCardValidator wrongIIN = new MasterCardValidator(buildNumber("561234", 16));
        assertFalse(wrongIIN.checkIINRanges());
    }

    @Test
    public void testDiscoverValidatorMultipleRanges() {
        // 用例目的：验证Discover卡四段IIN区间的逻辑覆盖，预期各区间对应号码均通过，其它号码失败。
        DiscoverValidator firstBlock = new DiscoverValidator(buildNumber("6011", 16));
        assertTrue(firstBlock.checkIINRanges());

        DiscoverValidator secondBlock = new DiscoverValidator(buildNumber("622126", 16));
        assertTrue(secondBlock.checkIINRanges());

        DiscoverValidator thirdBlock = new DiscoverValidator(buildNumber("644", 16));
        assertTrue(thirdBlock.checkIINRanges());

        DiscoverValidator fourthBlock = new DiscoverValidator(buildNumber("65", 16));
        assertTrue(fourthBlock.checkIINRanges());

        DiscoverValidator wrongLength = new DiscoverValidator(buildNumber("6011", 15));
        assertFalse(wrongLength.checkLength());

        DiscoverValidator tooLong = new DiscoverValidator(buildNumber("6011", 17));
        assertFalse(tooLong.checkLength());

        DiscoverValidator invalid = new DiscoverValidator(buildNumber("6600", 16));
        assertFalse(invalid.checkIINRanges());
    }

    @Test
    public void testChinaTUnionValidatorRules() {
        // 用例目的：验证中国T-Union卡的长度与IIN要求，预期19位且31开头通过。
        ChinaTUnionValidator valid = new ChinaTUnionValidator(buildNumber("31", 19));
        assertTrue(valid.checkLength());
        assertTrue(valid.checkIINRanges());

        ChinaTUnionValidator wrongLength = new ChinaTUnionValidator(buildNumber("31", 18));
        assertFalse(wrongLength.checkLength());

        ChinaTUnionValidator wrongIIN = new ChinaTUnionValidator(buildNumber("32", 19));
        assertFalse(wrongIIN.checkIINRanges());
    }

    @Test
    public void testChinaUnionPayValidatorRules() {
        // 用例目的：验证银联卡的长度区间与62开头匹配逻辑，预期合法时通过非法时失败。
        ChinaUnionPayValidator valid = new ChinaUnionPayValidator(buildNumber("62", 16));
        assertTrue(valid.checkLength());
        assertTrue(valid.checkIINRanges());

        ChinaUnionPayValidator wrongLength = new ChinaUnionPayValidator(buildNumber("62", 15));
        assertFalse(wrongLength.checkLength());

        ChinaUnionPayValidator wrongIIN = new ChinaUnionPayValidator(buildNumber("63", 16));
        assertFalse(wrongIIN.checkIINRanges());
    }

    @Test
    public void testDankortValidatorRules() {
        // 用例目的：验证Dankort卡的定长与4571-5019区间，预期边界值4571通过，非范围值失败。
        DankortValidator valid = new DankortValidator(buildNumber("4571", 16));
        assertTrue(valid.checkLength());
        assertTrue(valid.checkIINRanges());

        DankortValidator highValid = new DankortValidator(buildNumber("5019", 16));
        assertTrue(highValid.checkIINRanges());

        DankortValidator wrongLength = new DankortValidator(buildNumber("4571", 15));
        assertFalse(wrongLength.checkLength());

        DankortValidator wrongIIN = new DankortValidator(buildNumber("4570", 16));
        assertFalse(wrongIIN.checkIINRanges());
    }

    @Test
    public void testDinersClubInternationalValidatorRanges() {
        // 用例目的：验证大莱国际卡的多段区间逻辑，预期各种范围匹配成功，非法值失败。
        DinersClubInternationalValidator threeHundreds = new DinersClubInternationalValidator(buildNumber("300", 16));
        assertTrue(threeHundreds.checkIINRanges());

        DinersClubInternationalValidator fourDigit = new DinersClubInternationalValidator(buildNumber("3095", 16));
        assertTrue(fourDigit.checkIINRanges());

        DinersClubInternationalValidator thirtyEight = new DinersClubInternationalValidator(buildNumber("38", 16));
        assertTrue(thirtyEight.checkIINRanges());

        DinersClubInternationalValidator wrongLength = new DinersClubInternationalValidator(buildNumber("300", 15));
        assertFalse(wrongLength.checkLength());

        DinersClubInternationalValidator invalid = new DinersClubInternationalValidator(buildNumber("360", 16));
        assertFalse(invalid.checkIINRanges());
    }

    @Test
    public void testDinersClubValidatorRules() {
        // 用例目的：验证大莱卡普通版的规则，预期54-55区间通过，其它失败。
        DinersClubValidator valid = new DinersClubValidator(buildNumber("54", 16));
        assertTrue(valid.checkLength());
        assertTrue(valid.checkIINRanges());

        DinersClubValidator wrongLength = new DinersClubValidator(buildNumber("54", 15));
        assertFalse(wrongLength.checkLength());

        DinersClubValidator wrongIIN = new DinersClubValidator(buildNumber("53", 16));
        assertFalse(wrongIIN.checkIINRanges());
    }

    @Test
    public void testInstaPaymentValidatorRules() {
        // 用例目的：验证InstaPayment卡的范围，预期637-639内通过，其它失败。
        InstaPaymenttValidator valid = new InstaPaymenttValidator(buildNumber("637", 16));
        assertTrue(valid.checkLength());
        assertTrue(valid.checkIINRanges());

        InstaPaymenttValidator wrongIIN = new InstaPaymenttValidator(buildNumber("640", 16));
        assertFalse(wrongIIN.checkIINRanges());
    }

    @Test
    public void testInterPaymentValidatorRules() {
        // 用例目的：验证InterPayment卡的长度区间和IIN，预期636开头通过，其它失败。
        InterPaymentValidator valid = new InterPaymentValidator(buildNumber("636", 16));
        assertTrue(valid.checkLength());
        assertTrue(valid.checkIINRanges());

        InterPaymentValidator wrongLength = new InterPaymentValidator(buildNumber("636", 15));
        assertFalse(wrongLength.checkLength());

        InterPaymentValidator wrongIIN = new InterPaymentValidator(buildNumber("637", 16));
        assertFalse(wrongIIN.checkIINRanges());
    }

    @Test
    public void testJCBValidatorRules() {
        // 用例目的：验证JCB卡的长度与IIN区间，预期3528通过，3590失败。
        JCBValidator valid = new JCBValidator(buildNumber("3528", 16));
        assertTrue(valid.checkLength());
        assertTrue(valid.checkIINRanges());

        JCBValidator wrongLength = new JCBValidator(buildNumber("3528", 15));
        assertFalse(wrongLength.checkLength());

        JCBValidator wrongIIN = new JCBValidator(buildNumber("3590", 16));
        assertFalse(wrongIIN.checkIINRanges());
    }

    @Test
    public void testLankaPayValidatorRules() {
        // 用例目的：验证LankaPay卡的固定IIN逻辑，预期357111通过，其它失败。
        LankaPayValidator valid = new LankaPayValidator(buildNumber("357111", 16));
        assertTrue(valid.checkLength());
        assertTrue(valid.checkIINRanges());

        LankaPayValidator wrongIIN = new LankaPayValidator(buildNumber("357112", 16));
        assertFalse(wrongIIN.checkIINRanges());
    }

    @Test
    public void testMIRValidatorRules() {
        // 用例目的：验证MIR卡的范围逻辑，预期2200通过，2205失败。
        MIRValidator valid = new MIRValidator(buildNumber("2200", 16));
        assertTrue(valid.checkLength());
        assertTrue(valid.checkIINRanges());

        MIRValidator wrongIIN = new MIRValidator(buildNumber("2205", 16));
        assertFalse(wrongIIN.checkIINRanges());
    }

    @Test
    public void testMaestroValidatorRanges() {
        // 用例目的：验证Maestro卡长度区间和三段IIN范围，预期三段均可命中，其它失败。
        MaestroValidator firstRange = new MaestroValidator(buildNumber("500000", 12));
        assertTrue(firstRange.checkLength());
        assertTrue(firstRange.checkIINRanges());

        MaestroValidator secondRange = new MaestroValidator(buildNumber("560000", 12));
        assertTrue(secondRange.checkIINRanges());

        MaestroValidator thirdRange = new MaestroValidator(buildNumber("600000", 12));
        assertTrue(thirdRange.checkIINRanges());

        MaestroValidator wrongLength = new MaestroValidator(buildNumber("500000", 11));
        assertFalse(wrongLength.checkLength());

        MaestroValidator wrongIIN = new MaestroValidator(buildNumber("700000", 12));
        assertFalse(wrongIIN.checkIINRanges());
    }

    @Test
    public void testMaestroUKValidatorRanges() {
        // 用例目的：验证Maestro UK卡的特定IIN范围，预期三个合法值通过，非法值失败。
        MaestroUKValidator fourDigits = new MaestroUKValidator(buildNumber("6759", 12));
        assertTrue(fourDigits.checkLength());
        assertTrue(fourDigits.checkIINRanges());

        MaestroUKValidator firstSix = new MaestroUKValidator(buildNumber("676770", 12));
        assertTrue(firstSix.checkIINRanges());

        MaestroUKValidator secondSix = new MaestroUKValidator(buildNumber("676774", 12));
        assertTrue(secondSix.checkIINRanges());

        MaestroUKValidator wrongLength = new MaestroUKValidator(buildNumber("6759", 11));
        assertFalse(wrongLength.checkLength());

        MaestroUKValidator wrongIIN = new MaestroUKValidator(buildNumber("676775", 12));
        assertFalse(wrongIIN.checkIINRanges());
    }

    @Test
    public void testNPSPridnestrovieValidatorRules() {
        // 用例目的：验证NPS Pridnestrovie卡的七位IIN范围，预期6054742通过，6054745失败。
        NPS_PridnestrovieValidator valid = new NPS_PridnestrovieValidator(buildNumber("6054742", 16));
        assertTrue(valid.checkLength());
        assertTrue(valid.checkIINRanges());

        NPS_PridnestrovieValidator wrongIIN = new NPS_PridnestrovieValidator(buildNumber("6054745", 16));
        assertFalse(wrongIIN.checkIINRanges());
    }

    @Test
    public void testRuPayValidatorRules() {
        // 用例目的：验证RuPay卡的IIN匹配逻辑，预期60和6521区间通过，其他值失败。
        RuPayValidator prefixSixty = new RuPayValidator(buildNumber("60", 16));
        assertTrue(prefixSixty.checkLength());
        assertTrue(prefixSixty.checkIINRanges());

        RuPayValidator prefix6521 = new RuPayValidator(buildNumber("6521", 16));
        assertTrue(prefix6521.checkIINRanges());

        RuPayValidator wrongIIN = new RuPayValidator(buildNumber("6123", 16));
        assertFalse(wrongIIN.checkIINRanges());
    }

    @Test
    public void testTroyValidatorRules() {
        // 用例目的：验证Troy卡的范围逻辑，预期979200通过，979290失败。
        TroyValidator valid = new TroyValidator(buildNumber("979200", 16));
        assertTrue(valid.checkLength());
        assertTrue(valid.checkIINRanges());

        TroyValidator wrongIIN = new TroyValidator(buildNumber("979290", 16));
        assertFalse(wrongIIN.checkIINRanges());
    }

    @Test
    public void testUATPValidatorRules() {
        // 用例目的：验证UATP卡的六位IIN解析，预期000001转换为1通过，000002失败。
        UATPValidator valid = new UATPValidator(buildNumber("000001", 15));
        assertTrue(valid.checkLength());
        assertTrue(valid.checkIINRanges());

        UATPValidator wrongIIN = new UATPValidator(buildNumber("000002", 15));
        assertFalse(wrongIIN.checkIINRanges());
    }

    @Test
    public void testVerveValidatorRanges() {
        // 用例目的：验证Verve卡的双区间逻辑，预期第一段命中成功，第二段受实现限制为假。
        VerveValidator firstRange = new VerveValidator(buildNumber("506100", 16));
        assertTrue(firstRange.checkLength());
        assertTrue(firstRange.checkIINRanges());

        VerveValidator supposedSecondRange = new VerveValidator(buildNumber("650010", 16));
        assertFalse(supposedSecondRange.checkIINRanges());

        VerveValidator wrongLength = new VerveValidator(buildNumber("506100", 15));
        assertFalse(wrongLength.checkLength());
    }

    @Test
    public void testVerveValidatorBoundaryValues() {
        // 用例目的：验证Verve卡首段区间的边界值，预期下界506099与上界506198均被代码接受。
        VerveValidator lowerBoundary = new VerveValidator(buildNumber("506099", 16));
        assertTrue(lowerBoundary.checkIINRanges());

        VerveValidator upperBoundary = new VerveValidator(buildNumber("506198", 16));
        assertTrue(upperBoundary.checkIINRanges());
    }

    @Test
    public void testVerveValidatorLengthTooLong() {
        // 用例目的：验证Verve卡长度超过19位时的处理，预期20位号码无效。
        VerveValidator validator = new VerveValidator(buildNumber("506100", 20));
        assertFalse(validator.checkLength());
    }

    @Test
    public void testMultipleValidatorsRejectImproperLengths() {
        // 用例目的：批量验证各卡种在超长或过短场景下的长度判断，预期全部返回false。
        assertFalse(new AmericanExpressValidator(buildNumber("34", 16)).checkLength());
        assertFalse(new DankortValidator(buildNumber("4571", 17)).checkLength());
        assertFalse(new DinersClubValidator(buildNumber("54", 17)).checkLength());
        assertFalse(new InstaPaymenttValidator(buildNumber("637", 17)).checkLength());
        assertFalse(new JCBValidator(buildNumber("3528", 17)).checkLength());
        assertFalse(new LankaPayValidator(buildNumber("357111", 17)).checkLength());
        assertFalse(new MIRValidator(buildNumber("2200", 17)).checkLength());
        assertFalse(new NPS_PridnestrovieValidator(buildNumber("6054740", 17)).checkLength());
        assertFalse(new RuPayValidator(buildNumber("60", 17)).checkLength());
        assertFalse(new TroyValidator(buildNumber("979200", 17)).checkLength());
        assertFalse(new VerveValidator(buildNumber("506100", 15)).checkLength());
        assertFalse(new UATPValidator(buildNumber("000001", 14)).checkLength());
    }

    @Test
    public void testVisaElectronValidatorRanges() {
        // 用例目的：验证Visa Electron卡多个区间，预期4026、4508、4913均通过，其它失败。
        VisaElectronValidator firstRange = new VisaElectronValidator(buildNumber("4026", 16));
        assertTrue(firstRange.checkLength());
        assertTrue(firstRange.checkIINRanges());

        VisaElectronValidator secondRange = new VisaElectronValidator(buildNumber("4508", 16));
        assertTrue(secondRange.checkIINRanges());

        VisaElectronValidator thirdRange = new VisaElectronValidator(buildNumber("4913", 16));
        assertTrue(thirdRange.checkIINRanges());

        VisaElectronValidator wrongLength = new VisaElectronValidator(buildNumber("4026", 15));
        assertFalse(wrongLength.checkLength());

        VisaElectronValidator invalid = new VisaElectronValidator(buildNumber("8888", 16));
        assertFalse(invalid.checkIINRanges());
    }

    @Test
    public void testVisaElectronValidatorAdditionalRanges() {
        // 用例目的：验证Visa Electron卡区间上界，预期4405、4844、4917均能匹配成功。
        VisaElectronValidator range4405 = new VisaElectronValidator(buildNumber("4405", 16));
        assertTrue(range4405.checkIINRanges());

        VisaElectronValidator range4844 = new VisaElectronValidator(buildNumber("4844", 16));
        assertTrue(range4844.checkIINRanges());

        VisaElectronValidator range4917 = new VisaElectronValidator(buildNumber("4917", 16));
        assertTrue(range4917.checkIINRanges());
    }

    @Test
    public void testVisaElectronValidatorHighSpecPrefix() {
        // 用例目的：验证实现对417500前缀的处理，预期因实现限制返回false便于捕获潜在缺陷。
        VisaElectronValidator validator = new VisaElectronValidator(buildNumber("417500", 16));
        assertFalse(validator.checkIINRanges());
    }

    @Test
    public void testTypeCheckerIdentifiesKnownBrands() {
        // 用例目的：验证类型识别器对常见四大卡种的识别能力，预期依次匹配Visa、American Express、MasterCard、Discover。
        assertEquals(CreditCardType.VISA, TypeChecker.checkType(buildNumber("4", 16)));
        assertEquals(CreditCardType.AMERICAN_EXPRESS, TypeChecker.checkType(buildNumber("34", 15)));
        assertEquals(CreditCardType.MASTERCARD, TypeChecker.checkType(buildNumber("51", 16)));
        assertEquals(CreditCardType.DISCOVER, TypeChecker.checkType(buildNumber("6011", 16)));
    }

    @Test
    public void testTypeCheckerFallsBackToOther() {
        // 用例目的：验证类型识别器在无法匹配任何已知品牌时的兜底行为，预期返回OTHER。
        assertEquals(CreditCardType.OTHER, TypeChecker.checkType(buildNumber("70", 16)));
    }

    @Test
    public void testTypeCheckerRecognizesExtendedMasterCardRange() {
        // 用例目的：验证类型识别器能识别万事达新IIN段，预期222100开头仍识别为MasterCard。
        assertEquals(CreditCardType.MASTERCARD, TypeChecker.checkType(buildNumber("222100", 16)));
    }

    @Test
    public void testCreditCardTypeEnumIntegrity() {
        // 用例目的：验证枚举类型的完整性，预期能够通过名称定位并包含MASTERCARD值。
        assertEquals(CreditCardType.VISA, CreditCardType.valueOf("VISA"));
        assertTrue(Arrays.asList(CreditCardType.values()).contains(CreditCardType.MASTERCARD));
    }

    @Test
    public void testVisaValidatorLengthBoundaries() {
        // 用例目的：验证Visa长度范围的边界，预期13位和19位通过，20位失败。
        VisaValidator minLength = new VisaValidator(buildNumber("4", 13));
        assertTrue(minLength.checkLength());

        VisaValidator maxLength = new VisaValidator(buildNumber("4", 19));
        assertTrue(maxLength.checkLength());

        VisaValidator overLength = new VisaValidator(buildNumber("4", 20));
        assertFalse(overLength.checkLength());
    }

    @Test
    public void testAmericanExpressValidatorUpperBoundary() {
        // 用例目的：验证美国运通IIN区间上界37的处理，预期37开头的15位号码成功。
        AmericanExpressValidator upper = new AmericanExpressValidator(buildNumber("37", 15));
        assertTrue(upper.checkIINRanges());
    }

    @Test
    public void testMasterCardValidatorUpperBoundaries() {
        // 用例目的：验证万事达两个区间的上界，预期55与272099均判断为合法。
        MasterCardValidator upperFirstRange = new MasterCardValidator(buildNumber("55", 16));
        assertTrue(upperFirstRange.checkIINRanges());

        MasterCardValidator upperSecondRange = new MasterCardValidator(buildNumber("272099", 16));
        assertTrue(upperSecondRange.checkIINRanges());
    }

    @Test
    public void testDiscoverValidatorUpperBoundaries() {
        // 用例目的：验证Discover多段区间的上界，预期622925与649均成功匹配。
        DiscoverValidator sixDigitUpper = new DiscoverValidator(buildNumber("622925", 16));
        assertTrue(sixDigitUpper.checkIINRanges());

        DiscoverValidator threeDigitUpper = new DiscoverValidator(buildNumber("649", 16));
        assertTrue(threeDigitUpper.checkIINRanges());
    }

    @Test
    public void testChinaUnionPayValidatorLengthLimits() {
        // 用例目的：验证银联卡长度上界，预期19位合法、20位非法。
        ChinaUnionPayValidator nineteen = new ChinaUnionPayValidator(buildNumber("62", 19));
        assertTrue(nineteen.checkLength());

        ChinaUnionPayValidator twenty = new ChinaUnionPayValidator(buildNumber("62", 20));
        assertFalse(twenty.checkLength());
    }

    @Test
    public void testChinaTUnionValidatorLengthUpperFailure() {
        // 用例目的：验证中国T-Union在超出19位时的处理，预期20位返回false。
        ChinaTUnionValidator validator = new ChinaTUnionValidator(buildNumber("31", 20));
        assertFalse(validator.checkLength());
    }

    @Test
    public void testDankortValidatorUpperBoundary() {
        // 用例目的：验证Dankort IIN上界5019的处理，预期认定为合法。
        DankortValidator validator = new DankortValidator(buildNumber("5019", 16));
        assertTrue(validator.checkIINRanges());
    }

    @Test
    public void testDinersClubInternationalValidatorUpperBoundaries() {
        // 用例目的：验证大莱国际区间上界305与39，预期均判定为合法。
        DinersClubInternationalValidator threeHundreds = new DinersClubInternationalValidator(buildNumber("305", 16));
        assertTrue(threeHundreds.checkIINRanges());

        DinersClubInternationalValidator thirtyNine = new DinersClubInternationalValidator(buildNumber("39", 16));
        assertTrue(thirtyNine.checkIINRanges());
    }

    @Test
    public void testDinersClubInternationalValidatorLengthTooLong() {
        // 用例目的：验证大莱国际超过规定上限19位时的处理，预期20位长度无效。
        DinersClubInternationalValidator validator = new DinersClubInternationalValidator(buildNumber("300", 20));
        assertFalse(validator.checkLength());
    }

    @Test
    public void testDinersClubValidatorUpperBoundary() {
        // 用例目的：验证大莱卡普通版上界55的处理，预期合法。
        DinersClubValidator validator = new DinersClubValidator(buildNumber("55", 16));
        assertTrue(validator.checkIINRanges());
    }

    @Test
    public void testInstaPaymenttValidatorUpperBoundary() {
        // 用例目的：验证InstaPayment区间上界639的处理，预期合法。
        InstaPaymenttValidator validator = new InstaPaymenttValidator(buildNumber("639", 16));
        assertTrue(validator.checkIINRanges());
    }

    @Test
    public void testInstaPaymenttValidatorLengthFailure() {
        // 用例目的：验证InstaPayment长度不足时的处理，预期长度为15位时校验失败。
        InstaPaymenttValidator validator = new InstaPaymenttValidator(buildNumber("637", 15));
        assertFalse(validator.checkLength());
    }

    @Test
    public void testInterPaymentValidatorLengthUpperBoundary() {
        // 用例目的：验证InterPayment在19位时仍合法，20位则非法。
        InterPaymentValidator nineteen = new InterPaymentValidator(buildNumber("636", 19));
        assertTrue(nineteen.checkLength());

        InterPaymentValidator twenty = new InterPaymentValidator(buildNumber("636", 20));
        assertFalse(twenty.checkLength());
    }

    @Test
    public void testJCBValidatorUpperBoundary() {
        // 用例目的：验证JCB区间上界3589的处理，预期合法。
        JCBValidator validator = new JCBValidator(buildNumber("3589", 16));
        assertTrue(validator.checkIINRanges());
    }

    @Test
    public void testLankaPayValidatorLengthFailure() {
        // 用例目的：验证LankaPay长度不足时的处理，预期长度为15时失败。
        LankaPayValidator validator = new LankaPayValidator(buildNumber("357111", 15));
        assertFalse(validator.checkLength());
    }

    @Test
    public void testMIRValidatorUpperBoundary() {
        // 用例目的：验证MIR区间上界2204的处理，预期合法。
        MIRValidator validator = new MIRValidator(buildNumber("2204", 16));
        assertTrue(validator.checkIINRanges());
    }

    @Test
    public void testMIRValidatorLengthFailure() {
        // 用例目的：验证MIR卡长度不足时的处理，预期长度15位无法通过校验。
        MIRValidator validator = new MIRValidator(buildNumber("2200", 15));
        assertFalse(validator.checkLength());
    }

    @Test
    public void testMaestroValidatorLengthLimits() {
        // 用例目的：验证Maestro长度上界，预期19位合法、20位非法。
        MaestroValidator nineteen = new MaestroValidator(buildNumber("500000", 19));
        assertTrue(nineteen.checkLength());

        MaestroValidator twenty = new MaestroValidator(buildNumber("500000", 20));
        assertFalse(twenty.checkLength());
    }

    @Test
    public void testMaestroValidatorRangeUpperBoundaries() {
        // 用例目的：验证Maestro三段区间的上界，预期509999、589999、699999均合法。
        MaestroValidator firstUpper = new MaestroValidator(buildNumber("509999", 12));
        assertTrue(firstUpper.checkIINRanges());

        MaestroValidator secondUpper = new MaestroValidator(buildNumber("589999", 12));
        assertTrue(secondUpper.checkIINRanges());

        MaestroValidator thirdUpper = new MaestroValidator(buildNumber("699999", 12));
        assertTrue(thirdUpper.checkIINRanges());
    }

    @Test
    public void testMaestroUKValidatorLengthLimits() {
        // 用例目的：验证Maestro UK长度上界，预期19位合法、20位非法。
        MaestroUKValidator nineteen = new MaestroUKValidator(buildNumber("6759", 19));
        assertTrue(nineteen.checkLength());

        MaestroUKValidator twenty = new MaestroUKValidator(buildNumber("6759", 20));
        assertFalse(twenty.checkLength());
    }

    @Test
    public void testNPSPridnestrovieValidatorBoundaryValues() {
        // 用例目的：验证NPS Pridnestrovie区间边界6054740与6054744，预期均合法。
        NPS_PridnestrovieValidator lower = new NPS_PridnestrovieValidator(buildNumber("6054740", 16));
        assertTrue(lower.checkIINRanges());

        NPS_PridnestrovieValidator upper = new NPS_PridnestrovieValidator(buildNumber("6054744", 16));
        assertTrue(upper.checkIINRanges());
    }

    @Test
    public void testNPSPridnestrovieValidatorLengthFailure() {
        // 用例目的：验证NPS Pridnestrovie长度不足时的处理，预期15位号码判定为非法。
        NPS_PridnestrovieValidator validator = new NPS_PridnestrovieValidator(buildNumber("6054740", 15));
        assertFalse(validator.checkLength());
    }

    @Test
    public void testRuPayValidatorUpperBoundary() {
        // 用例目的：验证RuPay四位区间上界6522的处理，预期合法。
        RuPayValidator validator = new RuPayValidator(buildNumber("6522", 16));
        assertTrue(validator.checkIINRanges());
    }

    @Test
    public void testRuPayValidatorLengthFailure() {
        // 用例目的：验证RuPay长度不足时的处理，预期15位号码无效。
        RuPayValidator validator = new RuPayValidator(buildNumber("60", 15));
        assertFalse(validator.checkLength());
    }

    @Test
    public void testTroyValidatorUpperBoundary() {
        // 用例目的：验证Troy区间上界979289的处理，预期合法。
        TroyValidator validator = new TroyValidator(buildNumber("979289", 16));
        assertTrue(validator.checkIINRanges());
    }

    @Test
    public void testTroyValidatorLengthFailure() {
        // 用例目的：验证Troy卡长度不足时的处理，预期15位号码不能通过校验。
        TroyValidator validator = new TroyValidator(buildNumber("979200", 15));
        assertFalse(validator.checkLength());
    }

    @Test
    public void testUATPValidatorLengthFailure() {
        // 用例目的：验证UATP卡长度超过15位时返回false。
        UATPValidator validator = new UATPValidator(buildNumber("000001", 16));
        assertFalse(validator.checkLength());
    }

    @Test
    public void testValidatorCheckCVVTooLong() {
        // 用例目的：验证CVV长度大于4位时的处理，预期返回false。
        Validator validator = new Validator("1234567890123456", "01/50", "12345");
        assertFalse(validator.checkCVV());
    }

    @Test
    public void testValidatorExpirationDateExactCurrentMonth() {
        // 用例目的：验证到期日恰好等于当前年月时应判定为过期，预期返回false。
        int currentYear = DateChecker.CURRENT_YEAR;
        int currentMonth = DateChecker.CURRENT_MONTH;
        String expiration = String.format("%02d/%02d", currentMonth, currentYear);
        Validator validator = new Validator("79927398713", expiration, "123");
        assertFalse(validator.checkExpirationDate());
    }

    @Test
    public void testVisaElectronValidatorLengthUpperBoundary() {
        // 用例目的：验证Visa Electron长度上限仍然有效，预期19位通过、20位失败。
        VisaElectronValidator nineteen = new VisaElectronValidator(buildNumber("4026", 19));
        assertTrue(nineteen.checkLength());

        VisaElectronValidator twenty = new VisaElectronValidator(buildNumber("4026", 20));
        assertFalse(twenty.checkLength());
    }
}
