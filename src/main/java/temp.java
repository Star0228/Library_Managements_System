

import utils.ConnectConfig;
import utils.DatabaseConnector;
import utils.MysqlInitializer;
import utils.*;

import java.time.LocalDate;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Date;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;


import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

import queries.ApiResult;
import queries.BookQueryConditions;
import queries.BookQueryResults;
import queries.BorrowHistories;
import queries.CardList;
import queries.SortOrder;

import entities.Book;
import entities.Borrow;
import entities.Card;

public class temp {

    private static final Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        try {
            // parse connection config from "resources/application.yaml"
            ConnectConfig conf = new ConnectConfig();
            log.info("Success to parse connect config. " + conf.toString());
            // connect to database
            DatabaseConnector connector = new DatabaseConnector(conf);
            boolean connStatus = connector.connect();
            if (!connStatus) {
                log.severe("Failed to connect database.");
                System.exit(1);
            }
            /* do somethings */
            System.out.println("这是图书管理系统");
            System.out.println("输入1-10中的一个数字来启动对应的功能, 输入-1结束交互");
            System.out.println("1.添加图书");
            System.out.println("2.修改图书余量");
            System.out.println("3.修改图书信息");
            System.out.println("4.批量入库图书");
            System.out.println("5.注册借书证");
            System.out.println("6.查询借书证");
            System.out.println("7.借书");
            System.out.println("8.还书");
            System.out.println("9.查询借书记录");
            System.out.println("10.查询图书信息");
            LibraryManagementSystem library = new LibraryManagementSystemImpl(connector);
            //library.resetDatabase();
            String command;
            Scanner scanner = new Scanner(System.in);
            command = scanner.nextLine();
            int com = Integer.parseInt(command);

            while(com != -1){
                if(com == 1){//存入书籍
                    System.out.println("请输入<类别，书名，出版社，年份，作者，价格，初始库存>(系统将自动分配书号), 属性之间以逗号分隔");
                    command = scanner.nextLine();
                    String[] args1 = command.split(",");
                    //System.out.println(args1[7]);
                    entities.Book book = new Book(args1[0], args1[1], args1[2], Integer.parseInt(args1[3]), args1[4], Double.parseDouble(args1[5]), Integer.parseInt(args1[6]));
                    ApiResult apiResult = library.storeBook(book);
                    if(!apiResult.ok) System.out.println("添加失败!" + apiResult.message);
                    else System.out.println("《" + args1[1] + "》" +"添加成功!  BookID = " + Objects.toString(apiResult.payload));
                }else if(com == 2){//修改书籍库存
                    System.out.println("请输入<书号，增加的库存>, 属性之间以逗号分隔");
                    command = scanner.nextLine();
                    String[] args1 = command.split(",");
                    ApiResult apiResult = library.incBookStock(Integer.parseInt(args1[0]), Integer.parseInt(args1[1]));
                    if(!apiResult.ok) System.out.println("修改失败!" + apiResult.message);
                    else System.out.println("《" + args1[0] + "》" +"修改成功!");
                }else if (com == 3) {//修改图书信息
                    System.out.println("请输入<书号. 类别，书名，出版社，年份，作者，价格>, 属性之间以逗号分隔");
                    command = scanner.nextLine();
                    String[] args1 = command.split(",");
                    entities.Book book = new Book();
                    if(!Objects.equals(args1[1], " ")) book.setCategory(args1[1]);
                    if(!Objects.equals(args1[2], " ")) book.setTitle(args1[2]);
                    if(!Objects.equals(args1[3], " ")) book.setPress(args1[3]);
                    if(!Objects.equals(args1[4], " ")) book.setPublishYear(Integer.parseInt(args1[4]));
                    if(!Objects.equals(args1[5], " ")) book.setAuthor(args1[5]);
                    if(!Objects.equals(args1[6], " ")) book.setPrice(Double.parseDouble(args1[6]));

                    book.setBookId(Integer.parseInt(args1[0]));
                    ApiResult apiResult = library.modifyBookInfo(book);
                    if(!apiResult.ok) System.out.println("修改失败!");
                    else System.out.println("《" + args1[2] + "》" +"修改成功!");
                }else if(com == 4){//批量入库
                    System.out.println("请输入批量入库的文件路径名：");
                    command = scanner.nextLine();
                    String filename = command;
                    List<Book>books = new ArrayList<Book>();
                    try(Scanner sc = new Scanner(new File(filename))){
                        while(sc.hasNextLine()){
                            String[] args1 = sc.nextLine().split(",");
                            entities.Book book = new Book(args1[1], args1[2], args1[3], Integer.parseInt(args1[4]), args1[5], Double.parseDouble(args1[6]), Integer.parseInt(args1[7]));
                            book.setBookId(Integer.parseInt(args1[0]));
                            books.add(book);
                        }
                    }
                    ApiResult apiResult = library.storeBook(books);
                    if(!apiResult.ok) System.out.println("批量入库失败!" + apiResult.message);
                    else System.out.println("批量入库成功!");
                }else if(com == 5){//注册借书证
                    System.out.println("请输入你想要注册的借书证的姓名，单位以及身份，身份只能为T(Teacher) 或者 S(Student)");

                    command = scanner.nextLine();
                    String[] args1 = command.split(",");
                    entities.Card card = new Card();
                    card.setName(args1[0]);
                    card.setDepartment(args1[1]);
                    card.setType(Card.CardType.values(args1[2]));
                    ApiResult apiResult = library.registerCard(card);

                    String N;
                    if(args1[2].equals("T")) N = "老师";
                    else N = "学生";
                    String number = ((Integer)apiResult.payload).toString();
                    if(!apiResult.ok) System.out.println("注册失败!" + apiResult.message);
                    else System.out.println(N + args1[0] + "注册成功!" + number);
                }else if(com == 6){//查询借书证
                    List<Card> cards = ((CardList) library.showCards().payload).getCards();
                    for(Card card: cards){
                        System.out.println("姓名：" + card.getName() + " 单位：" + card.getDepartment() + " 身份：" + card.getType() + " 编号：" + card.getCardId());
                    }
                }else if(com == 7){//借书
                    System.out.println("欢迎借书！请输入<借书证号，书号>");
                    command = scanner.nextLine();
                    String[] args1 = command.split(",");
                    Borrow borrow = new Borrow(Integer.parseInt(args1[1]), Integer.parseInt(args1[0]));
                    LocalDate date = LocalDate.now();
                    borrow.resetBorrowTime();
                    //borrow.setBorrowTime(date.getYear()*10000 + date.getMonthValue()*100 + date.getDayOfMonth());
                    ApiResult apiResult = library.borrowBook(borrow);
                    if(!apiResult.ok) System.out.println("借书失败!" + apiResult.message);
                    else {
                        System.out.println("借书成功!");
                        System.out.println("借书证号：" + args1[0] + " 书号：" + args1[1] + " 借书日期：" + Integer.toString(date.getYear()) + "年" + Integer.toString(date.getMonthValue()) + "月" + Integer.toString(date.getDayOfMonth()) + "日");
                        //System.out.println("借书证号：" + args1[0] + " 书号：" + args1[1] + " 借书日期：" );
                    };
                }else if(com == 8){//还书
                    System.out.println("欢迎还书！请输入<借书证号. 书号>");
                    command = scanner.nextLine();
                    String[] args1 = command.split(",");
                    Borrow ret = new Borrow(Integer.parseInt(args1[1]), Integer.parseInt(args1[0]));
                    LocalDate date = LocalDate.now();
                    ret.resetReturnTime();
                    //ret.setReturnTime(date.getYear()*10000 + date.getMonthValue()*100 + date.getDayOfMonth());
                    //ret.setBorrowTime(Integer.parseInt(args1[2]));
                    ApiResult apiResult = library.returnBook(ret);
                    if(!apiResult.ok) System.out.println("还书失败!" + apiResult.message);
                    else {
                        System.out.println("还书成功!");
                        System.out.println("借书证号：" + args1[0] + " 书号：" + args1[1] + " 还书日期：" + Integer.toString(date.getYear()) + "年" + Integer.toString(date.getMonthValue()) + "月" + Integer.toString(date.getDayOfMonth()) + "日");
                    };
                }else if(com == 9){
                    System.out.println("查询借书记录，请输入借书证号");
                    command = scanner.nextLine();
                    ApiResult apiResult = library.showBorrowHistory(Integer.parseInt(command));
                    if(!apiResult.ok) System.out.println("查询失败!");
                    else{
                        BorrowHistories histories = (BorrowHistories) apiResult.payload;
                        System.out.println("借书证号：" + command + " 借书记录如下:");
                        for(BorrowHistories.Item borrow: histories.getItems()){
                            long timestamp = borrow.getBorrowTime(); // 毫秒级时间戳，例如：2024-03-29 12:00:05

                            // 使用 Instant.ofEpochMilli 方法将毫秒级时间戳转换为 Instant 类型
                            Instant instant = Instant.ofEpochMilli(timestamp);

                            // 使用 Instant 转换为 LocalDateTime 对象
                            LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

                            System.out.println("书号：" + borrow.getBookId() + " 借书时间：" + localDateTime);
                        }
                    }
                }else if(com == 10){
                    System.out.println("我们支持以下查询方式:<类别查询(模糊查询),书名查询(模糊查询)， 出版社查询(模糊查询)，出版社查询(模糊查询)，年份范围查询，作者查询(模糊查询)，价格范围差>");
                    System.out.println("请输入查询方式，以及对应的参数，参数之间以逗号分隔<category, title, Press, MinPublishYear, MaxPublishYear, Author, MinPrice, MaxPrice>");
                    command = scanner.nextLine();
                    String[] args1 = command.split(",");
                    BookQueryConditions conditions = new BookQueryConditions();
                    if(!Objects.equals(args1[0], " ")) conditions.setCategory(args1[0]);
                    if(!Objects.equals(args1[1], " ")) conditions.setTitle(args1[1]);
                    if(!Objects.equals(args1[2], " ")) conditions.setPress(args1[2]);
                    if(!Objects.equals(args1[3], " ")) conditions.setMinPublishYear(Integer.parseInt(args1[3]));
                    if(!Objects.equals(args1[4], " ")) conditions.setMaxPublishYear(Integer.parseInt(args1[4]));
                    if(!Objects.equals(args1[5], " ")) conditions.setAuthor(args1[5]);
                    //System.out.println(args1[6]);
                    if(!Objects.equals(args1[6], " ")) conditions.setMinPrice(Double.parseDouble(args1[6]));
                    if(!Objects.equals(args1[7], " ")) conditions.setMaxPrice(Double.parseDouble(args1[7]));
                    ApiResult apiResult = library.queryBook(conditions);
                    BookQueryResults results = (BookQueryResults) apiResult.payload;
                    if(!apiResult.ok) System.out.println("查询失败!" + apiResult.message);
                    else{
                        System.out.println("查询结果如下:" + Integer.toString(results.getCount()));
                        for(Book book: results.getResults()){
                            System.out.println("书号：" + book.getBookId() + " 类别：" + book.getCategory() + " 书名：" + book.getTitle() + " 出版社：" + book.getPress() + " 年份：" + Integer.toString(book.getPublishYear()) + " 作者：" + book.getAuthor() + " 价格：" + Double.toString(book.getPrice()) + " 库存：" + book.getStock());
                        }
                    }
                }
                System.out.println("输入1-10中的一个数字来启动对应的功能, 输入-1结束交互");
                System.out.println("1.添加图书");
                System.out.println("2.修改图书余量");
                System.out.println("3.修改图书信息");
                System.out.println("4.批量入库图书");
                System.out.println("5.注册借书证");
                System.out.println("6.查询借书证");
                System.out.println("7.借书");
                System.out.println("8.还书");
                System.out.println("9.查询借书记录");
                System.out.println("10.查询图书信息");
                command = scanner.nextLine();
                com = Integer.parseInt(command);
            }
            // release database connection handler
            if (connector.release()) {
                log.info("Success to release connection.");
            } else {
                log.warning("Failed to release connection.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

