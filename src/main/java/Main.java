import entities.Book;
import entities.Borrow;
import entities.Card;
import org.json.JSONArray;
import queries.*;
import utils.ConnectConfig;
import utils.DatabaseConnector;



import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.io.IOException;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import org.json.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.SimpleDateFormat;
import java.util.Date;


import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;



public class Main {

    private static final Logger log = Logger.getLogger(Main.class.getName());
    static LibraryManagementSystem library ;

    public static void main(String[] args) throws IOException {
        // 创建HTTP服务器，监听指定端口
        // 这里是8000，建议不要80端口，容易和其他的撞
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        // 添加handler，这里就绑定到/card路由
        // 所以localhost:8000/card是会有handler来处理
        server.createContext("/card", new CardHandler());
        server.createContext("/book", new BookHandler());
        server.createContext("/borrow",new BorrowHandler());
        // 启动服务器
        server.start();
        // 标识一下，这样才知道我的后端启动了（确信
        System.out.println("Server is listening on port 8000");
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
            //实例化接口
            library = new LibraryManagementSystemImpl(connector);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    static class CardHandler implements HttpHandler {
        // 关键重写handle方法
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 允许所有域的请求，cors处理
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            // 解析请求的方法，看GET还是POST
            String requestMethod = exchange.getRequestMethod();
            // 注意判断要用equals方法而不是==啊，java的小坑（
            if (requestMethod.equals("GET")) {
                // 处理GET
                handleGetRequest(exchange);
            } else if (requestMethod.equals("POST")) {
                // 处理POST
                handlePostRequest(exchange);
            } else if (requestMethod.equals("OPTIONS")) {
                // 处理OPTIONS
                handleOptionsRequest(exchange);
            } else {
                // 其他请求返回405 Method Not Allowed
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private void handleGetRequest(HttpExchange exchange) throws IOException {

            // 响应头，因为是JSON通信
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            // 状态码为200，也就是status ok
            exchange.sendResponseHeaders(200, 0);
            // 获取输出流，java用流对象来进行io操作
            OutputStream outputStream = exchange.getResponseBody();
            // 构建JSON响应数据，这里简化为字符串
            // 这里写的一个固定的JSON，实际可以查表获取数据，然后再拼出想要的JSON

            List<Card> cards = ((CardList) library.showCards().payload).getCards();
            JSONArray jsons = new JSONArray();

            // 遍历每张卡片，将其转换为 JSON 对象，并添加到 JSON 数组中
            for (Card card : cards) {
                JSONObject cardJson = new JSONObject();
                cardJson.put("name", card.getName());
                cardJson.put("department", card.getDepartment());
                cardJson.put("type", card.getType());
                cardJson.put("id", card.getCardId());
                jsons.put(cardJson);
            }
            // 将 JSON 数组转换为字符串
            String jsonResponse = jsons.toString();
            outputStream.write(jsonResponse.getBytes());
            // 流一定要close！！！小心泄漏
            outputStream.close();
        }
        private void handlePostRequest(HttpExchange exchange) throws IOException {
            // 读取POST请求体
            InputStream requestBody = exchange.getRequestBody();
            String path = exchange.getRequestURI().getPath();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }

            // 看看读到了啥
            // 实际处理可能会更复杂点
            if(path.equals("/card"))
            {
                JSONObject json = new JSONObject(requestBodyBuilder.toString());
                Card tem = new Card();
                tem.setName(json.getString("name"));
                tem.setDepartment(json.getString("department"));
                String tem_type = "T";
                if (json.getString("type").equals("Student")) {
                    tem_type = "S";
                }
                System.out.println("Received POST request to create card with data: " + json);
                tem.setType(Card.CardType.values(tem_type));
                ApiResult ar = library.registerCard(tem);

                if (!ar.ok) System.out.println("注册失败!" + ar.message);
                else System.out.println("注册成功!");
            }else if(path.equals("/card/remove")){
                JSONObject json = new JSONObject(requestBodyBuilder.toString());
                int id = json.getInt("id");
                System.out.println("Received POST request to create card with data: " + json);
                ApiResult ar = library.removeCard(id);

                if (!ar.ok) System.out.println("删除失败!" + ar.message);
                else System.out.println("删除成功!");
            }

            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            exchange.sendResponseHeaders(200, 0);

            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write("Card created successfully".getBytes());
            outputStream.close();
        }
        private void handleOptionsRequest(HttpExchange exchange) throws IOException {

            exchange.sendResponseHeaders(204, 0);

        }
    }
    static class BookHandler implements HttpHandler{
        // 关键重写handle方法
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 允许所有域的请求，cors处理
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            // 解析请求的方法，看GET还是POST
            String requestMethod = exchange.getRequestMethod();
            // 注意判断要用equals方法而不是==啊，java的小坑（
            if (requestMethod.equals("GET")) {
                // 处理GET
                handleGetRequest(exchange);
            } else if (requestMethod.equals("POST")) {
                // 处理POST
                handlePostRequest(exchange);
            } else if (requestMethod.equals("OPTIONS")) {
                // 处理OPTIONS
                handleOptionsRequest(exchange);
            } else {
                // 其他请求返回405 Method Not Allowed
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private void handleGetRequest(HttpExchange exchange) throws IOException {
            // 响应头，因为是JSON通信
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            // 状态码为200，也就是status ok
            exchange.sendResponseHeaders(200, 0);
            // 获取输出流，java用流对象来进行io操作
            OutputStream outputStream = exchange.getResponseBody();
            // 构建JSON响应数据，这里简化为字符串
            // 这里写的一个固定的JSON，实际可以查表获取数据，然后再拼出想要的JSON
            String path = exchange.getRequestURI().getQuery();
            System.out.println(path);

            ArrayList<String> left  = new ArrayList<String>();
            ArrayList<String> right = new ArrayList<String>();
            left.add("category");
            left.add("title");
            left.add("press");
            left.add("minPublishYear");
            left.add("maxPublishYear");
            left.add("author");
            left.add("minPrice");
            left.add("maxPrice");
            left.add("sortBy");
            left.add("sortOrder");
            for(int i = 0;i<9;i++){
                StringBuilder te = new StringBuilder();
                te.append(left.get(i));
                te.append("=([^&]+)&");
                Pattern pattern = Pattern.compile(te.toString());
                Matcher matcher = pattern.matcher(path);
                if (matcher.find()) {
                    right.add(matcher.group(1));

                }else{
                    right.add("null");
                }
            }
            Pattern pattern = Pattern.compile("sortOrder=([^&]*)$");
            Matcher matcher = pattern.matcher(path);
            if (matcher.find()) {
                right.add(matcher.group(1));
            }else{
                right.add("null");
            }
            BookQueryConditions cond = new BookQueryConditions();

            if(!right.get(0).equals("null")){
                cond.setCategory(right.get(0));
            }
            if(!right.get(1).equals("null")){
                cond.setTitle(right.get(1));
            }
            if(!right.get(2).equals("null")){
                cond.setPress(right.get(2));
            }
            if(!right.get(3).equals("null")){
                cond.setMinPublishYear(Integer.parseInt(right.get(3)));
            }
            if(!right.get(4).equals("null")){
                cond.setMaxPublishYear(Integer.parseInt(right.get(4)));
            }
            if(!right.get(5).equals("null")){
                cond.setAuthor(right.get(5));
            }
            if(!right.get(6).equals("null")){
                cond.setMinPrice(Double.parseDouble(right.get(6)));
            }
            if(!right.get(7).equals("null")){
                cond.setMaxPrice(Double.parseDouble(right.get(7)));
            }
            if(!right.get(8).equals("null")){
              Book.SortColumn tem1 = Book.SortColumn.BOOK_ID;
              if(right.get(8).equals("book_id")){
                  tem1 = Book.SortColumn.BOOK_ID;
              }
                if(right.get(8).equals("category")){
                    tem1 = Book.SortColumn.CATEGORY;
                }
                if(right.get(8).equals("title")){
                    tem1 = Book.SortColumn.TITLE;
                }
                if(right.get(8).equals("press")){
                    tem1 = Book.SortColumn.PRESS;
                }
                if(right.get(8).equals("publish_year")){
                    tem1 = Book.SortColumn.PUBLISH_YEAR;
                }
                if(right.get(8).equals("author")){
                    tem1 = Book.SortColumn.AUTHOR;
                }
                if(right.get(8).equals("price")){
                    tem1 = Book.SortColumn.PRICE;
                }
                if(right.get(8).equals("stock")){
                    tem1 = Book.SortColumn.STOCK;
                }
                cond.setSortBy(tem1);
            }
            if(!right.get(9).equals("null")){
                SortOrder tem2 = SortOrder.ASC;
                if(right.get(9).equals("desc")){
                    tem2 = SortOrder.DESC;
                }
                cond.setSortOrder(tem2);
            }

            BookQueryConditions cond1 = new BookQueryConditions();
            List<Book> books = ((BookQueryResults) library.queryBook(cond).payload).getResults();
            JSONArray jsons = new JSONArray();
            // 遍历每张卡片，将其转换为 JSON 对象，并添加到 JSON 数组中
            for (Book book : books) {
                JSONObject bookJson = new JSONObject();
                bookJson.put("bookId", book.getBookId());
                bookJson.put("category", book.getCategory());
                bookJson.put("title", book.getTitle());
                bookJson.put("press", book.getPress());
                bookJson.put("publishYear", book.getPublishYear());
                bookJson.put("author", book.getAuthor());
                bookJson.put("price", book.getPrice());
                bookJson.put("stock", book.getStock());
                jsons.put(bookJson);
            }
            // 将 JSON 数组转换为字符串
            String jsonResponse = jsons.toString();
            outputStream.write(jsonResponse.getBytes());
            // 流一定要close！！！小心泄漏
            outputStream.close();
        }
        private void handlePostRequest(HttpExchange exchange) throws IOException {
            // 读取POST请求体
            InputStream requestBody = exchange.getRequestBody();
            String path = exchange.getRequestURI().getPath();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }

            // 看看读到了啥
            // 实际处理可能会更复杂点
            ApiResult ar = new ApiResult(false,"fail");
            if(path.equals("/book"))
            {
                JSONObject json = new JSONObject(requestBodyBuilder.toString());
                Book tem = new Book();
                tem.setTitle(json.getString("title"));
                tem.setCategory(json.getString("category"));
                tem.setPress(json.getString("press"));
                tem.setPublishYear(json.getInt("publishYear"));
                tem.setAuthor(json.getString("author"));
                tem.setPrice(json.getDouble("price"));
                tem.setStock(json.getInt("stock"));
                System.out.println("Received POST request to create book with data: " + json);
                ar = library.storeBook(tem);
                if (!ar.ok) System.out.println("入库失败!" + ar.message);
                else System.out.println("入库成功!");
            }else if(path.equals("/book/remove")){
                JSONObject json = new JSONObject(requestBodyBuilder.toString());
                int bookId = json.getInt("bookId");
                System.out.println("Received POST request to create book with data: " + json);
                ar = library.removeBook(bookId);
                if (!ar.ok) System.out.println("删除失败!" + ar.message);
                else System.out.println("删除成功!");
            }else if(path.equals("/book/modifystock")){
                JSONObject json = new JSONObject(requestBodyBuilder.toString());
                int bookId = json.getInt("bookId");
                int delta = json.getInt("deltaStock");
                System.out.println("Received POST request to modify book with data: " + json);
                ar = library.incBookStock(bookId,delta);
                if (!ar.ok) System.out.println("修改失败!" + ar.message);
                else System.out.println("修改成功!");
            }else if(path.equals("/book/modify")){
                JSONObject json = new JSONObject(requestBodyBuilder.toString());
                Book tem = new Book();
                tem.setBookId(json.getInt("bookId"));
                tem.setTitle(json.getString("title"));
                tem.setCategory(json.getString("category"));
                tem.setPress(json.getString("press"));
                tem.setPublishYear(json.getInt("publishYear"));
                tem.setAuthor(json.getString("author"));
                tem.setPrice(json.getDouble("price"));
                System.out.println("Received POST request to modify book with data: " + json);
                ar = library.modifyBookInfo(tem);
                if (!ar.ok) System.out.println("修改失败!" + ar.message);
                else System.out.println("修改成功!");
            }else if(path.equals("/book/upload")){
                JSONObject json = new JSONObject(requestBodyBuilder.toString());
                System.out.println("Received POST request to modify book with data: " + json);
                // 创建ObjectMapper实例
                ObjectMapper objectMapper = new ObjectMapper();
                // 将JSON字符串转换为JsonNode
                JsonNode rootNode = objectMapper.readTree(requestBodyBuilder.toString());
                // 获取名为"json"的JsonNode数组
                JsonNode jsonNodeArray = rootNode.path("json");
                // 依次访问数组中的每个元素
                List<Book> books = new ArrayList<Book>();

                for (JsonNode node : jsonNodeArray) {
                    Book book = new Book();
                    // 使用asInt()方法获取name和title的值
                    book.setTitle(node.path("title").asText());
                    book.setCategory(node.path("category").asText());
                    book.setPress(node.path("press").asText());
                    book.setPublishYear(node.path("publishYear").asInt());
                    book.setAuthor(node.path("author").asText());
                    book.setPrice(node.path("price").asDouble());
                    book.setStock(node.path("stock").asInt());
                    books.add(book);
                    // 打印结果
                }
                ar = library.storeBook(books);
                if (!ar.ok) System.out.println("入库失败!" + ar.message);
                else System.out.println("入库成功!");
            }else if(path.equals("/book/borrow")){
                JSONObject json = new JSONObject(requestBodyBuilder.toString());
                Borrow tem = new Borrow();
                tem.setBookId(json.getInt("bookId"));
                tem.setCardId(json.getInt("cardId"));

                Date now = new Date();

                // 将Date对象转换为Unix时间戳（秒）
                long unixTime = now.getTime() / 1000L;
                tem.setBorrowTime(unixTime);
                tem.setReturnTime(0);
                ar = library.borrowBook(tem);
                if (!ar.ok) System.out.println("借书失败!" + ar.message);
                else System.out.println("借书成功!");
            }
            else if(path.equals("/book/return")){
                JSONObject json = new JSONObject(requestBodyBuilder.toString());
                Borrow tem = new Borrow();
                tem.setBookId(json.getInt("bookId"));
                tem.setCardId(json.getInt("cardId"));
                Date now = new Date();

                // 将Date对象转换为Unix时间戳（秒）
                long unixTime = now.getTime() / 1000L;
                tem.setBorrowTime(0);
                tem.setReturnTime(unixTime);
                ar = library.returnBook(tem);
                if (!ar.ok) System.out.println("还书失败!" + ar.message);
                else System.out.println("还书成功!");
            }
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            if(ar.ok){
                exchange.sendResponseHeaders(200, 0);
                // 剩下三个和GET一样
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write("Borrow created successfully".getBytes());
                outputStream.close();
            }else{
                exchange.sendResponseHeaders(404, 0);
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write("Borrow created fail".getBytes());
                outputStream.close();
            }

        }
        private void handleOptionsRequest(HttpExchange exchange) throws IOException {

            exchange.sendResponseHeaders(204, 0);

        }
    }

    static class BorrowHandler implements HttpHandler {
        // 关键重写handle方法
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 允许所有域的请求，cors处理
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            // 解析请求的方法，看GET还是POST
            String requestMethod = exchange.getRequestMethod();
            // 注意判断要用equals方法而不是==啊，java的小坑（
            if (requestMethod.equals("GET")) {
                // 处理GET
                handleGetRequest(exchange);
            } else if (requestMethod.equals("POST")) {
                // 处理POST
                handlePostRequest(exchange);
            } else if (requestMethod.equals("OPTIONS")) {
                // 处理OPTIONS
                handleOptionsRequest(exchange);
            } else {
                // 其他请求返回405 Method Not Allowed
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private void handleGetRequest(HttpExchange exchange) throws IOException {
            // 响应头，因为是JSON通信
            ApiResult ar = new ApiResult(false,"fail");
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            // 状态码为200，也就是status ok
            exchange.sendResponseHeaders(200, 0);
            // 获取输出流，java用流对象来进行io操作
            OutputStream outputStream = exchange.getResponseBody();

            String path = exchange.getRequestURI().getQuery();


            Pattern pattern = Pattern.compile("=(.+)$");
            Matcher matcher = pattern.matcher(path);
            List<BorrowHistories.Item> items = new ArrayList<>();
            if(matcher.find()){
                items =  ((BorrowHistories)library.showBorrowHistory(Integer.parseInt(matcher.group(1))).payload).getItems();
                ar = library.showBorrowHistory(Integer.parseInt(matcher.group(1)));
                if (!ar.ok) System.out.println("查询失败!" + ar.message);
                else System.out.println("查询成功!");
            }

            // 构建JSON响应数据，这里简化为字符串
            // 这里写的一个固定的JSON，实际可以查表获取数据，然后再拼出想要的JSON

            JSONArray jsons = new JSONArray();
            // 遍历每张卡片，将其转换为 JSON 对象，并添加到 JSON 数组中
            for (BorrowHistories.Item item : items) {
                JSONObject borrowJson = new JSONObject();
                borrowJson.put("cardID",item.getCardId());
                borrowJson.put("bookID",item.getBookId());
                borrowJson.put("borrowTime",item.getBorrowTime());
                borrowJson.put("returnTime",item.getReturnTime());
                jsons.put(borrowJson);
            }
            // 将 JSON 数组转换为字符串
            String jsonResponse = jsons.toString();
            outputStream.write(jsonResponse.getBytes());
            // 流一定要close！！！小心泄漏
            outputStream.close();
        }
        private void handlePostRequest(HttpExchange exchange) throws IOException {
            // 读取POST请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }

            // 看看读到了啥
            // 实际处理可能会更复杂点
            System.out.println("Received POST request to create card with data: " + requestBodyBuilder.toString());

            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            exchange.sendResponseHeaders(200, 0);

            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write("Card created successfully".getBytes());
            outputStream.close();
        }
        private void handleOptionsRequest(HttpExchange exchange) throws IOException {

            exchange.sendResponseHeaders(204, 0);

        }
    }
}


