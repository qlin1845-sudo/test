package net.mooctest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class HotelTest {

    private PrintStream originalOut;

    @Before
    public void setUp() {
        originalOut = System.out;
        new Hotel();
        Order.orders.clear();
    }

    @After
    public void tearDown() {
        System.setOut(originalOut);
        Order.orders.clear();
        Hotel.rooms.clear();
    }

    private String captureOutput(Runnable action) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream tempPrintStream = new PrintStream(outputStream);
        System.setOut(tempPrintStream);
        try {
            action.run();
        } finally {
            System.setOut(originalOut);
        }
        return outputStream.toString();
    }

    @Test
    public void testRoomInitializationAndToString() {
        // 验证房间在正常构造时的基础属性及字符串描述是否正确
        Room room = new Room(123, RoomType.ADVANCED, 3, 180.0);
        assertEquals(123, room.getRoomCode());
        assertEquals(RoomType.ADVANCED, room.getType());
        assertEquals(3, room.getCapacity());
        assertEquals(180.0, room.getPrice(), 0.0);
        assertTrue(room.getState() instanceof FreeTimeState);
        String description = room.toString();
        assertTrue(description.contains("RoomCode: 123"));
        assertTrue(description.contains("RoomState: Free"));
        assertTrue(description.contains("Price: 180.0"));
    }

    @Test
    public void testSetRoomCodeRejectsTooLarge() {
        // 验证房间号大于999时会抛出非法参数异常
        Room room = new Room();
        try {
            room.setRoomCode(1000);
            fail("应当抛出非法房间号异常");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Illegal RoomCode"));
        }
    }

    @Test
    public void testSetRoomCodeRejectsTooSmall() {
        // 验证房间号小于等于100时会抛出非法参数异常
        Room room = new Room();
        try {
            room.setRoomCode(100);
            fail("应当抛出非法房间号异常");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Illegal RoomCode"));
        }
    }

    @Test
    public void testSetRoomCodeRejectsDoubleZero() {
        // 验证房间号包含连续两个零时会抛出异常
        Room room = new Room();
        try {
            room.setRoomCode(200);
            fail("应当抛出非法房间号异常");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Illegal RoomCode"));
        }
    }

    @Test
    public void testSetTypeRejectsUnknownType() {
        // 验证设置不存在的房间类型时会抛出异常
        Room room = new Room();
        try {
            room.setType("VIP Room");
            fail("应当抛出房间类型不存在异常");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Type not exists"));
        }
    }

    @Test
    public void testSetCapacityRejectsOutOfRange() {
        // 验证房间容量过小或过大时的异常处理
        Room room = new Room();
        try {
            room.setCapacity(0);
            fail("应当抛出容量过小异常");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Capacity"));
        }
        try {
            room.setCapacity(11);
            fail("应当抛出容量过大异常");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Capacity"));
        }
    }

    @Test
    public void testSetPriceRejectsNonPositive() {
        // 验证价格小于等于零时抛出异常
        Room room = new Room();
        try {
            room.setPrice(0);
            fail("应当抛出价格非法异常");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Price cannot less than"));
        }
        try {
            room.setPrice(-10);
            fail("应当抛出价格非法异常");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Price cannot less than"));
        }
    }

    @Test
    public void testSetPriceRejectsTooManyDecimals() {
        // 验证价格小数位超过两位时抛出异常
        Room room = new Room();
        try {
            room.setPrice(100.123);
            fail("应当抛出价格格式异常");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Price length is wrong"));
        }
    }

    @Test
    public void testFreeTimeStateBehaviors() {
        // 验证空闲状态下的合法操作和非法操作
        Room roomForBooking = new Room(201, RoomType.STANDARD, 2, 120.0);
        roomForBooking.book();
        assertTrue(roomForBooking.getState() instanceof BookedState);

        Room roomForCheckIn = new Room(202, RoomType.STANDARD, 2, 120.0);
        roomForCheckIn.checkIn();
        assertTrue(roomForCheckIn.getState() instanceof CheckInState);

        Room roomForErrors = new Room(203, RoomType.STANDARD, 2, 120.0);
        try {
            roomForErrors.unsubscribe();
            fail("应当抛出非法退订异常");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Cannot unsubscribe"));
        }
        try {
            roomForErrors.checkOut();
            fail("应当抛出非法退房异常");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Cannot check out"));
        }
    }

    @Test
    public void testBookedStateBehaviors() {
        // 验证预定状态下的操作流程和异常提示
        Room room = new Room(301, RoomType.ADVANCED, 3, 180.0);
        room.book();
        try {
            room.book();
            fail("重复预定应当失败");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Cannot book"));
        }
        try {
            Room another = new Room(302, RoomType.ADVANCED, 2, 150.0);
            another.book();
            another.checkOut();
            fail("预定状态下不允许直接退房");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Cannot check out"));
        }
        Room unsubscribeRoom = new Room(303, RoomType.ADVANCED, 2, 150.0);
        unsubscribeRoom.book();
        unsubscribeRoom.unsubscribe();
        assertTrue(unsubscribeRoom.getState() instanceof FreeTimeState);

        Room checkInRoom = new Room(304, RoomType.ADVANCED, 2, 150.0);
        checkInRoom.book();
        checkInRoom.checkIn();
        assertTrue(checkInRoom.getState() instanceof CheckInState);
    }

    @Test
    public void testCheckInStateBehaviors() {
        // 验证入住状态下的非法操作和退房逻辑
        Room room = new Room(401, RoomType.DELUXE, 2, 240.0);
        room.checkIn();
        try {
            room.book();
            fail("入住状态不允许再预定");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Cannot book"));
        }
        try {
            room.unsubscribe();
            fail("入住状态不允许退订");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Cannot unsubscribe"));
        }
        try {
            room.checkIn();
            fail("入住状态不允许重复入住");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Cannot check in"));
        }
        room.checkOut();
        assertTrue(room.getState() instanceof FreeTimeState);
    }

    @Test
    public void testRoomTypeValidation() {
        // 验证房间类型枚举工具类的判断逻辑
        assertTrue(RoomType.isRoomType(RoomType.STANDARD));
        assertTrue(RoomType.isRoomType(RoomType.ADVANCED));
        assertTrue(RoomType.isRoomType(RoomType.DELUXE));
        assertFalse(RoomType.isRoomType("Unknown"));
    }

    @Test
    public void testHotelAddRoomAndQueryOutput() {
        // 验证新增房间后的查询输出内容
        Hotel.addRoom(RoomType.STANDARD, 511, 2);
        assertEquals(1, Hotel.rooms.size());
        String result = captureOutput(() -> Hotel.queryRoom(511));
        assertTrue(result.contains("RoomCode: 511"));
        assertTrue(result.contains("Price: 120.0"));
        assertTrue(result.contains("RoomState: Free"));
    }

    @Test
    public void testHotelQueryRoomNotExist() {
        // 验证查询不存在房间时的异常
        try {
            Hotel.queryRoom(999);
            fail("不存在的房间应当抛出异常");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Room not exist"));
        }
    }

    @Test
    public void testHotelAddRoomDuplicateAndInvalidType() {
        // 验证重复房间和非法类型的异常处理
        Hotel.addRoom(RoomType.ADVANCED, 601, 3);
        try {
            Hotel.addRoom(RoomType.ADVANCED, 601, 3);
            fail("重复房间应当抛出异常");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Room Exist"));
        }
        try {
            Hotel.addRoom("VIP", 602, 2);
            fail("非法类型应当在构造房间时失败");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Type not exists"));
        }
    }

    @Test
    public void testHotelGetFreeRoomsOrdering() {
        // 验证仅空闲房间被筛选并按类型与价格排序
        Hotel.addRoom(RoomType.DELUXE, 701, 2);
        Hotel.addRoom(RoomType.ADVANCED, 702, 2);
        Hotel.addRoom(RoomType.STANDARD, 703, 2);
        Hotel.rooms.stream().filter(r -> r.getRoomCode() == 703).findFirst().get().book();
        List<Room> freeRooms = Hotel.getFreeRooms();
        assertEquals(2, freeRooms.size());
        assertEquals(RoomType.DELUXE, freeRooms.get(0).getType());
        assertEquals(RoomType.ADVANCED, freeRooms.get(1).getType());
    }

    @Test
    public void testHotelPrintAllRoomsInfoOrdering() {
        // 验证全量房间打印的排序优先级（状态 > 类型 > 价格 > 房号）
        Hotel.addRoom(RoomType.ADVANCED, 811, 2);
        Hotel.addRoom(RoomType.DELUXE, 812, 2);
        Hotel.addRoom(RoomType.STANDARD, 813, 2);
        Hotel.addRoom(RoomType.ADVANCED, 814, 2);
        Hotel.rooms.stream().filter(r -> r.getRoomCode() == 813).findFirst().get().book();
        Hotel.rooms.stream().filter(r -> r.getRoomCode() == 814).findFirst().get().checkIn();
        String output = captureOutput(Hotel::printAllRoomsInfo);
        assertTrue(output.indexOf("RoomCode: 811") < output.indexOf("RoomCode: 812"));
        assertTrue(output.indexOf("RoomState: Free") < output.indexOf("RoomState: CheckIn"));
        assertTrue(output.indexOf("RoomState: CheckIn") < output.indexOf("RoomState: Booked"));
    }

    @Test
    public void testHotelRemoveRoomSuccess() {
        // 验证房间成功移除
        Hotel.addRoom(RoomType.STANDARD, 901, 2);
        assertEquals(1, Hotel.rooms.size());
        Hotel.removeRoomFromList(901);
        assertEquals(0, Hotel.rooms.size());
    }

    @Test
    public void testHotelRemoveRoomNotExist() {
        // 验证移除不存在的房间时的异常提示
        try {
            Hotel.removeRoomFromList(999);
            fail("应当抛出房间不存在异常");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Room not Exist"));
        }
    }

    @Test
    public void testManagerCheckInDiscountsAndStateChange() {
        // 验证不同入住天数的折扣计算及状态迁移
        Manager manager = new Manager();
        Hotel.addRoom(RoomType.ADVANCED, 1001, 4);
        Hotel.addRoom(RoomType.STANDARD, 1002, 2);
        Hotel.addRoom(RoomType.DELUXE, 1003, 2);

        double costLongStay = manager.checkIn(8, 1001);
        assertEquals(1536.0, costLongStay, 0.0);
        assertTrue(Hotel.rooms.stream().filter(r -> r.getRoomCode() == 1001).findFirst().get().getState() instanceof CheckInState);

        double costMidStay = manager.checkIn(5, 1002);
        assertEquals(540.0, costMidStay, 0.0);

        double costShortStay = manager.checkIn(3, 1003);
        assertEquals(720.0, costShortStay, 0.0);
    }

    @Test
    public void testManagerCheckInInvalidDays() {
        // 验证入住天数非法时的异常
        Manager manager = new Manager();
        Hotel.addRoom(RoomType.STANDARD, 1011, 2);
        try {
            manager.checkIn(0, 1011);
            fail("天数为零应当抛出异常");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Days should larger than zero"));
        }
    }

    @Test
    public void testManagerCheckInRoomNotExists() {
        // 验证找不到房间时的处理
        Manager manager = new Manager();
        try {
            manager.checkIn(2, 1999);
            fail("房间不存在应当抛出异常");
        } catch (NullPointerException e) {
            assertTrue(e.getMessage().contains("Room not exists"));
        }
    }

    @Test
    public void testManagerTransferPriceRomanConversion() {
        // 验证价格转换为罗马数字的正常场景
        Manager manager = new Manager();
        assertEquals("LVIII", manager.transferPrice(58.3));
        assertEquals("V", manager.transferPrice(4.6));
    }

    @Test
    public void testManagerTransferPriceBoundaryCases() {
        // 验证价格转换的边界情况（零与超出范围）
        Manager manager = new Manager();
        assertEquals("", manager.transferPrice(0));
        assertNull(manager.transferPrice(4000));
    }

    @Test
    public void testOrderCreateWithoutDiscount() {
        // 验证订单金额不足以触发折扣时的金额统计
        List<OrderItem> items = new ArrayList<>();
        items.add(new OrderItem("Water", 50.0, 4));
        Order.createOrder(items);
        assertEquals(1, Order.orders.size());
        Order order = Order.orders.get(0);
        assertEquals(200.0, order.totalAmount(), 0.0);
        assertEquals(50.0, order.getItems().get(0).getPaymentPrice(), 0.0);
    }

    @Test
    public void testOrderCreateWithNinetyPercentDiscount() {
        // 验证订单金额达到九折阈值时折扣是否生效
        List<OrderItem> items = new ArrayList<>();
        items.add(new OrderItem("Cola", 100.0, 4));
        items.add(new OrderItem("Snack", 50.0, 4));
        Order.createOrder(items);
        Order order = Order.orders.get(Order.orders.size() - 1);
        assertEquals(540.0, order.totalAmount(), 0.0);
        assertEquals(90.0, order.getItems().get(0).getPaymentPrice(), 0.0);
        assertEquals(45.0, order.getItems().get(1).getPaymentPrice(), 0.0);
    }

    @Test
    public void testOrderCreateWithEightyPercentDiscount() {
        // 验证订单金额达到八折阈值时折扣是否生效
        List<OrderItem> items = new ArrayList<>();
        items.add(new OrderItem("Cake", 200.0, 3));
        items.add(new OrderItem("Wine", 150.0, 3));
        Order.createOrder(items);
        Order order = Order.orders.get(Order.orders.size() - 1);
        assertEquals(840.0, order.totalAmount(), 0.0);
        assertEquals(160.0, order.getItems().get(0).getPaymentPrice(), 0.0);
        assertEquals(120.0, order.getItems().get(1).getPaymentPrice(), 0.0);
    }

    @Test
    public void testOrderPrintOrdersOutput() {
        // 验证打印全部订单时的文本格式与汇总金额
        Order.orders.clear();
        List<OrderItem> first = Arrays.asList(new OrderItem("Book", 100.0, 2));
        List<OrderItem> second = Arrays.asList(new OrderItem("Meal", 200.0, 3));
        Order.createOrder(first);
        Order.createOrder(second);
        String output = captureOutput(Order::printOrders);
        assertTrue(output.contains("Order No.1"));
        assertTrue(output.contains("Order Total"));
        assertTrue(output.contains("AllAmount:"));
    }

    @Test
    public void testOrderSearchMaxOrder() {
        // 验证最大订单检索逻辑
        Order.orders.clear();
        List<OrderItem> first = Arrays.asList(new OrderItem("Pen", 50.0, 2));
        List<OrderItem> second = Arrays.asList(new OrderItem("Phone", 500.0, 2));
        Order.createOrder(first);
        Order.createOrder(second);
        Order maxOrder = Order.searchMaxOrder();
        assertEquals(800.0, maxOrder.totalAmount(), 0.0);
    }

    @Test
    public void testOrderItemFormattingAndAmount() {
        // 验证订单项的金额计算与格式化输出
        OrderItem item = new OrderItem("Juice", 12.345, 3);
        assertEquals(37.035, item.getAmount(), 0.0);
        String line = item.PrintOrderItem();
        assertTrue(line.contains("Juice"));
        assertTrue(line.contains("12.34"));
        assertTrue(line.contains("37.03"));
    }

    @Test
    public void testOrderItemSetterGetter() {
        // 验证订单项的读写属性
        OrderItem item = new OrderItem("Tea", 10.0, 2);
        item.setProductName("Coffee");
        item.setPaymentPrice(20.0);
        item.setCount(5);
        assertEquals("Coffee", item.getProductName());
        assertEquals(20.0, item.getPaymentPrice(), 0.0);
        assertEquals(5, item.getCount());
    }

    @Test
    public void testProductNameValidation() {
        // 验证商品名称的格式限制
        Product product = new Product("Valid", 10.0, 1);
        try {
            product.setName("含中文");
            fail("商品名称只能包含字母");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("English characters"));
        }
        try {
            product.setName("ABCDEFGHIJKLMNOPQRSTU");
            fail("商品名称长度超过限制应当失败");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("longer than 20"));
        }
    }

    @Test
    public void testProductPriceValidation() {
        // 验证商品价格的合法性
        Product product = new Product("Price", 10.0, 1);
        try {
            product.setPrice(-1);
            fail("商品价格小于零应当失败");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Price cannot less than"));
        }
        try {
            product.setPrice(10.123);
            fail("商品价格小数位超限应当失败");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Price's length"));
        }
        product.setPrice(20.5);
        assertEquals(20.5, product.price, 0.0);
    }

    @Test
    public void testProductDiscountValidation() {
        // 验证商品折扣的边界条件
        Product product = new Product("Discount", 10.0, 1);
        try {
            product.setDiscount(0);
            fail("折扣小于等于零应当失败");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Discount cannot less"));
        }
        try {
            product.setDiscount(1.1);
            fail("折扣大于1应当失败");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("larger than 1"));
        }
        try {
            product.setDiscount(0.1234);
            fail("折扣小数位超限应当失败");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Discount's length"));
        }
        product.setDiscount(0.75);
        assertEquals(0.75, product.discount, 0.0);
        assertEquals(7.5, product.getPaymentPrice(), 0.0);
    }

    @Test
    public void testProductCountValidationAndInfo() {
        // 验证商品数量限制及信息展示格式
        Product product = new Product("Apple", 10.0, 5);
        try {
            product.setCount(0);
            fail("数量小于等于零应当失败");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Quantity should larger"));
        }
        product.setDiscount(0.8);
        String info = product.getInfo();
        assertTrue(info.contains("80%"));
        product.setDiscount(1);
        String infoNoDiscount = product.getInfo();
        assertTrue(infoNoDiscount.contains("No discount"));
    }

    @Test
    public void testShopGetAllProductsInfoSorting() {
        // 验证商品列表排序规则（先价格再名称）
        Shop shop = new Shop();
        Product tea = new Product("Tea", 30.0, 5);
        Product coffee = new Product("Coffee", 30.0, 3);
        coffee.setDiscount(0.5);
        Product bread = new Product("Bread", 10.0, 10);
        shop.addProduct(tea);
        shop.addProduct(coffee);
        shop.addProduct(bread);
        String info = shop.getAllProductsInfo();
        assertTrue(info.indexOf("Bread") < info.indexOf("Coffee"));
        assertTrue(info.indexOf("Coffee") < info.indexOf("Tea"));
    }

    @Test
    public void testShopAddProductMergeAndIndex() {
        // 验证重复添加商品时的库存合并与索引返回
        Shop shop = new Shop();
        Product juice = new Product("Juice", 20.0, 5);
        int firstIndex = shop.addProduct(juice);
        assertEquals(1, firstIndex);
        Product juice2 = new Product("Juice", 20.0, 2);
        int mergeIndex = shop.addProduct(juice2);
        assertEquals(0, mergeIndex);
        assertEquals(7, shop.getProductByName("Juice").count);
    }

    @Test
    public void testShopDeleteAndExceptions() {
        // 验证商品下架成功及不存在时的异常
        Shop shop = new Shop();
        Product wine = new Product("Wine", 50.0, 2);
        shop.addProduct(wine);
        assertEquals(0, shop.deletProduct("Wine"));
        try {
            shop.deletProduct("Beer");
            fail("不存在的商品应当抛出异常");
        } catch (NullPointerException e) {
            assertTrue(e.getMessage().contains("Product is not exists"));
        }
    }

    @Test
    public void testShopSellProductScenarios() {
        // 验证商品售卖的正常场景与异常分支
        Shop shop = new Shop();
        Product snack = new Product("Snack", 10.0, 5);
        shop.addProduct(snack);
        assertEquals(3, shop.sellProduct("Snack", 2));
        try {
            shop.sellProduct("Snack", 10);
            fail("库存不足应当失败");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Quantity of remaining"));
        }
        try {
            shop.sellProduct("Snack", 0);
            fail("售卖数量为零应当失败");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Count cannot less"));
        }
        try {
            shop.sellProduct("Unknown", 1);
            fail("未知商品应当失败");
        } catch (NullPointerException e) {
            assertTrue(e.getMessage().contains("Product is not exists"));
        }
    }

    @Test
    public void testShopUpdateProduct() {
        // 验证商品价格与折扣更新
        Shop shop = new Shop();
        Product cheese = new Product("Cheese", 40.0, 4);
        shop.addProduct(cheese);
        Product updated = shop.updateProduct("Cheese", 50.0, 0.8);
        assertEquals(50.0, updated.price, 0.0);
        assertEquals(0.8, updated.discount, 0.0);
        try {
            shop.updateProduct("Milk", 30.0, 0.9);
            fail("更新不存在商品应当失败");
        } catch (NullPointerException e) {
            assertTrue(e.getMessage().contains("Product is not exists"));
        }
    }

    @Test
    public void testShopSearchAndGetProductByName() {
        // 验证商品索引查询与按名称获取
        Shop shop = new Shop();
        Product soap = new Product("Soap", 5.0, 3);
        shop.addProduct(soap);
        assertEquals(0, shop.searchProduct("Soap"));
        assertEquals(-1, shop.searchProduct("Shampoo"));
        assertNull(shop.getProductByName("Shampoo"));
        assertNotNull(shop.getProductByName("Soap"));
    }

    @Test
    public void testShopKeeperSellProductsCreatesOrder() {
        // 验证售卖商品时成功与失败混合场景下的输出和订单生成
        Shop shop = new Shop();
        Product snack = new Product("Snack", 10.0, 5);
        Product cake = new Product("Cake", 20.0, 2);
        shop.addProduct(snack);
        shop.addProduct(cake);
        ShopKeeper keeper = new ShopKeeper();
        keeper.setShop(shop);
        Map<String, Integer> plan = new LinkedHashMap<>();
        plan.put("Snack", 2);
        plan.put("Ghost", 1);
        String output = captureOutput(() -> keeper.sellProducts(plan));
        assertTrue(output.contains("Selld Successfully:Snack*2"));
        assertTrue(output.contains("Selld Failed:Product is not exists."));
        assertEquals(1, Order.orders.size());
        Order order = Order.orders.get(0);
        assertEquals(20.0, order.totalAmount(), 0.0);
        assertEquals(3, shop.getProductByName("Snack").count);
    }

    @Test
    public void testShopKeeperShowAllProductsOutput() {
        // 验证前台展示商品信息的输出
        Shop shop = new Shop();
        Product bread = new Product("Bread", 10.0, 2);
        shop.addProduct(bread);
        ShopKeeper keeper = new ShopKeeper();
        keeper.setShop(shop);
        String output = captureOutput(keeper::showAllProducts);
        assertTrue(output.contains("Bread"));
        assertTrue(output.contains("No.1"));
    }
}
