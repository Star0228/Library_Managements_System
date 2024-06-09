import entities.Book;
import entities.Borrow;
import entities.Card;
import queries.*;
import utils.DBInitializer;
import utils.DatabaseConnector;

import java.sql.*;
import java.util.List;
import java.util.ArrayList;

public class LibraryManagementSystemImpl implements LibraryManagementSystem {

    private final DatabaseConnector connector;

    public LibraryManagementSystemImpl(DatabaseConnector connector) {
        this.connector = connector;
    }
    /*图书入库模块*/
    @Override
    public ApiResult storeBook(Book book) {
        Connection conn = connector.getConn();
        try{
            PreparedStatement pre1 = conn.prepareStatement("select book_id from book where category = ? and title = ? and press = ? and publish_year = ? and author = ?;");
            pre1.setString(1,book.getCategory());
            pre1.setString(2,book.getTitle());
            pre1.setString(3,book.getPress());
            pre1.setInt(4,book.getPublishYear());
            pre1.setString(5,book.getAuthor());
            ResultSet rs = pre1.executeQuery();
            if(rs.next()){
                return new ApiResult(false,"fail");
            }else{
                PreparedStatement pre = conn.prepareStatement("insert into book (category,title,press,publish_year,author,price,stock) values (?,?,?,?,?,?,?);");
                pre.setString(1,book.getCategory());
                pre.setString(2,book.getTitle());
                pre.setString(3,book.getPress());
                pre.setInt(4,book.getPublishYear());
                pre.setString(5,book.getAuthor());
                pre.setDouble(6,book.getPrice());
                pre.setInt(7,book.getStock());
                pre.executeUpdate();
                commit(conn);
                rs = pre1.executeQuery();
                while(rs.next()){
                    book.setBookId(rs.getInt(1));
                }
                return new ApiResult(true,"success");
            }
        }catch (Exception e){
            rollback(conn);
            return new ApiResult(false,e.getMessage());
        }
    }

    @Override
    /*图书增加库存模块*/
    public ApiResult incBookStock(int bookId, int deltaStock) {
        Connection conn = connector.getConn();
        try {
            PreparedStatement stmt = conn.prepareStatement("select stock from book where book_id = ? for update;");
            stmt.setInt(1,bookId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int sum = deltaStock + rs.getInt(1) >= 0 ? deltaStock + rs.getInt(1) : -1;
                if (sum == -1) {
                    conn.rollback();
                    return new ApiResult(false, "fail");
                } else {
                    PreparedStatement pre = conn.prepareStatement("update book set stock = ? where book_id = ?;");
                    pre.setInt(1, sum);
                    pre.setInt(2, bookId);
                    pre.executeUpdate();
                    conn.commit();
                    return new ApiResult(true, "success");
                }
            }else{
                conn.rollback();
                return new ApiResult(false, "fail");
            }
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
    }

    /*批量入库*/
    @Override
    public ApiResult storeBook(List<Book> books) {
        Connection conn = connector.getConn();
        try {
            int cnt = 0;
            PreparedStatement pre1 = conn.prepareStatement("select book_id from book where category = ? and title = ? and press = ? and publish_year = ? and author = ?;");
            while (cnt < books.size()) {
                Book book = books.get(cnt);
                pre1.setString(1, book.getCategory());
                pre1.setString(2, book.getTitle());
                pre1.setString(3, book.getPress());
                pre1.setInt(4, book.getPublishYear());
                pre1.setString(5, book.getAuthor());
                ResultSet rs = pre1.executeQuery();
                if (rs.next()) {
                    conn.rollback();
                    return new ApiResult(false, "fail");
                } else {
                    PreparedStatement pre = conn.prepareStatement("insert into book (category,title,press,publish_year,author,price,stock) values (?,?,?,?,?,?,?);");
                    pre.setString(1, book.getCategory());
                    pre.setString(2, book.getTitle());
                    pre.setString(3, book.getPress());
                    pre.setInt(4, book.getPublishYear());
                    pre.setString(5, book.getAuthor());
                    pre.setDouble(6, book.getPrice());
                    pre.setInt(7, book.getStock());
                    pre.executeUpdate();
                    rs = pre1.executeQuery();
                    while (rs.next()) {
                        books.get(cnt).setBookId(rs.getInt(1));
                    }
                }
                cnt += 1;
            }
            commit(conn);
            return new ApiResult(true, "success");
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }

    }


    /*图书删除模块*/
    @Override
    public ApiResult removeBook(int bookId) {
        Connection conn = connector.getConn();
        try{
            PreparedStatement pre = conn.prepareStatement("select book_id from borrow where book_id = ? and return_time = 0  ;");
            pre.setInt(1,bookId);
            ResultSet rs = pre.executeQuery();
            if(rs.next()){
                conn.rollback();
                return new ApiResult(false,"fail");
            }else{
                PreparedStatement pre1 = conn.prepareStatement("select book_id from book where book_id = ? ;");
                pre1.setInt(1,bookId);
                rs = pre1.executeQuery();
                if(rs.next()){
                    PreparedStatement pre2 = conn.prepareStatement("delete from book where book_id = ?;");
                    pre2.setInt(1,bookId);
                    pre2.executeUpdate();
                    conn.commit();
                    return new ApiResult(true,"success");

                }
                conn.rollback();
                return new ApiResult(false,"fail");
            }
        }catch (Exception e){
            rollback(conn);
            return new ApiResult(false,e.getMessage());
        }
    }

    /*修改图书信息*/
    @Override
    public ApiResult modifyBookInfo(Book book) {
        Connection conn = connector.getConn();
        try{
            PreparedStatement pre = conn.prepareStatement("update book set category = ?,title=?,press=?,publish_year=?,author=?,price=? where book_id = ?;");
            pre.setString(1, book.getCategory());
            pre.setString(2, book.getTitle());
            pre.setString(3, book.getPress());
            pre.setInt(4, book.getPublishYear());
            pre.setString(5, book.getAuthor());
            pre.setDouble(6, book.getPrice());
            pre.setInt(7, book.getBookId());
            int message =pre.executeUpdate();
            if(message == 0){
                rollback(conn);
                return new ApiResult(false,"fail");
            }else{
                conn.commit();
                return new ApiResult(true,"success");
            }
        }catch (Exception e){
            rollback(conn);
            return new ApiResult(false,e.getMessage());
        }
    }

    /*图书查询模块*/
    @Override
    public ApiResult queryBook(BookQueryConditions conditions) {
        Connection conn = connector.getConn();
        try{
            PreparedStatement pre = conn.prepareStatement("select * from book where category like ? and title like ? and press like ? and publish_year>= ? and publish_year<= ? and author like ? and price>= ? and price<= ?;");
            if(conditions.getCategory()!=null){
                pre.setString(1,conditions.getCategory());
            }else{
                pre.setString(1,"%");
            }
            if(conditions.getTitle()!=null){
                pre.setString(2,"%"+conditions.getTitle()+"%");
            }else{
                pre.setString(2,"%");
            }
            if(conditions.getPress()!=null){
                pre.setString(3,"%"+conditions.getPress()+"%");
            }else{
                pre.setString(3,"%");
            }
            if(conditions.getMinPublishYear()!=null){
                pre.setInt(4,conditions.getMinPublishYear());
            }else{
                pre.setInt(4,-9999999);
            }
            if(conditions.getMaxPublishYear()!=null){
                pre.setInt(5,conditions.getMaxPublishYear());
            }else{
                pre.setInt(5,9999999);
            }
            if(conditions.getAuthor()!=null){
                pre.setString(6,"%"+conditions.getAuthor()+"%");
            }else{
                pre.setString(6,"%");
            }
            if(conditions.getMinPrice()!=null){
                pre.setDouble(7,conditions.getMinPrice());
            }else{
                pre.setDouble(7,-9999999);
            }
            if(conditions.getMaxPrice()!=null){
                pre.setDouble(8,conditions.getMaxPrice());
            }else{
                pre.setDouble(8,9999999);
            }
            ResultSet rs = pre.executeQuery();
            List<Book> books = new ArrayList<>();
            while(rs.next()){
                Book book = new Book();
                book.setBookId(rs.getInt(1));
                book.setCategory(rs.getString(2));
                book.setTitle(rs.getString(3));
                book.setPress(rs.getString(4));
                book.setPublishYear(rs.getInt(5));
                book.setAuthor(rs.getString(6));
                book.setPrice(rs.getDouble(7));
                book.setStock(rs.getInt(8));
                books.add(book);
            }
            Book.SortColumn col1 = conditions.getSortBy();
            Book.SortColumn col2 = Book.SortColumn.BOOK_ID;
            if(conditions.getSortOrder().getValue() == "asc"){
                books.sort(col1.getComparator().thenComparing(col2.getComparator()));
            }else{
                books.sort(col1.getComparator().reversed().thenComparing(col2.getComparator()));
            }
            commit(conn);
            return new ApiResult(true,new BookQueryResults(books));
        }catch (Exception e){
            rollback(conn);
            return new ApiResult(false,e.getMessage());
        }
    }

    /*借书模块*/
    @Override
    public ApiResult borrowBook(Borrow borrow) {
        //return new ApiResult(false, "Unimplemented Function");
        Connection conn = connector.getConn();
        try{
            PreparedStatement sql2 = conn.prepareStatement("select stock from book where book_id = ? for update;");
            sql2.setInt(1,borrow.getBookId());
            ResultSet rs = sql2.executeQuery();
            if(rs.next()){
                PreparedStatement sql3 = conn.prepareStatement("select * from borrow where book_id = ? and card_id = ? and return_time = 0;");
                sql3.setInt(1,borrow.getBookId());
                sql3.setInt(2,borrow.getCardId());
                ResultSet rs1 = sql3.executeQuery();
                if(rs1.next()){
                    rollback(conn);
                    return new ApiResult(false,"fail");
                }else{
                    if(rs.getInt(1)==0){
                        rollback(conn);
                        return new ApiResult(false,"fail");
                    }
                    PreparedStatement sql = conn.prepareStatement("insert into borrow (card_id,book_id,borrow_time,return_time) values(?,?,?,?);");
                    sql.setInt(1,borrow.getCardId());
                    sql.setInt(2,borrow.getBookId());
                    sql.setLong(3,borrow.getBorrowTime());
                    sql.setLong(4,0);
                    sql.executeUpdate();
                    PreparedStatement sql1 = conn.prepareStatement("update book set stock = stock-1 where book_id = ?;");
                    sql1.setInt(1,borrow.getBookId());
                    sql1.executeUpdate();
                    commit(conn);
                    return new ApiResult(true,"success");
                }
            }else{
                rollback(conn);
                return new ApiResult(false,"fail");
            }

        }catch (Exception e){
            rollback(conn);
            return new ApiResult(false,e.getMessage());
        }
    }

    /*还书模块*/
    @Override
    public ApiResult returnBook(Borrow borrow) {
    //return new ApiResult(false, "Unimplemented Function");
        Connection conn = connector.getConn();
        try{
            PreparedStatement sql3 = conn.prepareStatement("select borrow_time from borrow where book_id = ? and card_id = ? and return_time = 0 for update;");
            sql3.setInt(1,borrow.getBookId());
            sql3.setInt(2,borrow.getCardId());
            ResultSet rs= sql3.executeQuery();
            if(rs.next()){
                if(borrow.getReturnTime()<=rs.getLong(1)){
                    rollback(conn);
                    return new ApiResult(false,"fail");
                }
                PreparedStatement sql1 = conn.prepareStatement("update book set stock = stock+1 where book_id = ?;");
                PreparedStatement sql2 = conn.prepareStatement("update borrow set return_time = ? where card_id = ? and book_id = ? and return_time = 0;");
                sql1.setInt(1,borrow.getBookId());
                sql2.setLong(1,borrow.getReturnTime());
                sql2.setInt(2,borrow.getCardId());
                sql2.setInt(3,borrow.getBookId());
                sql1.executeUpdate();
                sql2.executeUpdate();
                commit(conn);
                return new ApiResult(true,"success");
            }else{
                rollback(conn);
                return new ApiResult(false,"fail");
            }
        }catch (Exception e){
            rollback(conn);
            return new ApiResult(false,e.getMessage());
        }
    }

    /*借书记录查询*/
    @Override
    public ApiResult showBorrowHistory(int cardId) {
        //return new ApiResult(false, "Unimplemented Function");
        Connection conn = connector.getConn();
        try{
            PreparedStatement sql = conn.prepareStatement("select card_id,book_id,category,title,press,publish_year,author,price,borrow_time,return_time from book natural join borrow where card_id = ? order by borrow_time desc,book_id asc ;");
            sql.setInt(1,cardId);
            ResultSet rs = sql.executeQuery();
            List<BorrowHistories.Item> items = new ArrayList<>();
            while(rs.next()){
                BorrowHistories.Item item = new BorrowHistories.Item();
                item.setCardId(rs.getInt(1));
                item.setBookId(rs.getInt(2));
                item.setCategory(rs.getString(3));
                item.setTitle(rs.getString(4));
                item.setPress(rs.getString(5));
                item.setPublishYear(rs.getInt(6));
                item.setAuthor(rs.getString(7));
                item.setPrice(rs.getDouble(8));
                item.setBorrowTime(rs.getLong(9));
                item.setReturnTime(rs.getLong(10));
                items.add(item);
            }
            commit(conn);
            return new ApiResult(true,new BorrowHistories(items));
        }catch (Exception e){
            rollback(conn);
            return new ApiResult(false,e.getMessage());
        }
    }

    /*注册借书证*/
    @Override
    public ApiResult registerCard(Card card) {
        Connection conn = connector.getConn();
        try{
            PreparedStatement sql = conn.prepareStatement("select card_id from card where name = ? and department = ? and type = ?;");
            sql.setString(1,card.getName());
            sql.setString(2,card.getDepartment());
            sql.setString(3,card.getType().getStr());
            ResultSet rs= sql.executeQuery();
            if(rs.next()){
                rollback(conn);
                return new ApiResult(false,"fail");
            }else{
                PreparedStatement pre = conn.prepareStatement("insert into card (name,department,type) values (?,?,?);");
                pre.setString(1,card.getName());
                pre.setString(2,card.getDepartment());
                pre.setString(3,card.getType().getStr());
                pre.executeUpdate();
                rs = sql.executeQuery();
                while(rs.next()){
                    card.setCardId(rs.getInt(1));
                }
                commit(conn);
                return new ApiResult(true,"success");
            }
        }catch (Exception e){
            rollback(conn);
            return new ApiResult(false,e.getMessage());
        }
    }

    /*删除借书证*/
    @Override
    public ApiResult removeCard(int cardId) {
        Connection conn = connector.getConn();
        try{
            PreparedStatement stem = conn.prepareStatement("select * from borrow where card_id = ? and return_time = 0 for update;");
            stem.setInt(1,cardId);
            ResultSet rs = stem.executeQuery();
            if(rs.next()){
                rollback(conn);
                return new ApiResult(false,"fail");
            }else{
                PreparedStatement stmt = conn.prepareStatement("delete from card where card_id = ?;");
                PreparedStatement stmt1 = conn.prepareStatement("delete from borrow where card_id = ?;");
                stmt.setInt(1,cardId);
                stmt1.setInt(1,cardId);
                int message = stmt.executeUpdate();
                stmt1.executeUpdate();
                if(message==0){
                    rollback(conn);
                    return new ApiResult(false,"fail");
                }
                commit(conn);
                return new ApiResult(true,"success");
            }
        }catch (Exception e){
            rollback(conn);
            return new ApiResult(false,e.getMessage());
        }
    }

    /*查询借书证*/
    @Override
    public ApiResult showCards() {
        Connection conn = connector.getConn();
        List<Card> results = new ArrayList<>();
        try{
            PreparedStatement pStmt = conn.prepareStatement("SELECT card_id, name, department, type FROM card order by card_id ASC ;");
            ResultSet rs = pStmt.executeQuery();
            while(rs.next()){
                Card card = new Card(rs.getInt("card_id"), rs.getString("name"),rs.getString("department"), Card.CardType.values(rs.getString("type")));

                results.add(card);
            }
            pStmt.close();
            commit(conn);
        }catch(Exception e){
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, new CardList(results));
    }

    @Override
    public ApiResult resetDatabase() {
        Connection conn = connector.getConn();
        try {
            Statement stmt = conn.createStatement();
            DBInitializer initializer = connector.getConf().getType().getDbInitializer();
            stmt.addBatch(initializer.sqlDropBorrow());
            stmt.addBatch(initializer.sqlDropBook());
            stmt.addBatch(initializer.sqlDropCard());
            stmt.addBatch(initializer.sqlCreateCard());
            stmt.addBatch(initializer.sqlCreateBook());
            stmt.addBatch(initializer.sqlCreateBorrow());
            stmt.executeBatch();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    private void rollback(Connection conn) {
        try {
            conn.rollback();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void commit(Connection conn) {
        try {
            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
