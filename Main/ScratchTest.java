import database.DatabaseHelper;
import java.sql.*;

public class ScratchTest {
    public static void main(String[] args) throws Exception {
        try (Connection c = DatabaseHelper.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM transactions LIMIT 1")) {
            ResultSetMetaData meta = rs.getMetaData();
            for (int i=1; i<=meta.getColumnCount(); i++) {
                System.out.println(meta.getColumnName(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
