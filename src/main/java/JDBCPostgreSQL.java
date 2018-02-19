import java.sql.*;

public class JDBCPostgreSQL {

    static final String DB_URL = "jdbc:postgresql://127.0.0.1:5432/sonya";
    static final String USER = "sonya";
    static final String PASS = "xkl088";
    private static Connection db;

    public static void connect() {
        System.out.println("Testing connection to PostgreSQL JDBC");
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("PostgreSQL JDBC Driver is not found.");
            e.printStackTrace();
            return;
        }

        System.out.println("PostgreSQL JDBC Driver successfully connected");

        try {
            db = DriverManager.getConnection(DB_URL, USER, PASS);
        } catch (SQLException e) {
            System.out.println("Connection Failed");
            e.printStackTrace();
            return;
        }

        if (db != null) {
            System.out.println("You successfully connected to database now");
        } else {
            System.out.println("Failed to make connection to database");
        }
    }

    public static int addTextData(String text, String entity){
        try {
            int rowCounter = 0;
            PreparedStatement selectSt = db.prepareStatement("SELECT id FROM " + entity + " WHERE text = ?");
            PreparedStatement insertSt = db.prepareStatement("INSERT INTO " + entity + " (text) VALUES (?) RETURNING id");

            selectSt.setString(1, text);
            ResultSet rs = selectSt.executeQuery();

            while (rs.next()) rowCounter++;

            if (rowCounter == 0) {
                insertSt.setString(1, text);
                rs = insertSt.executeQuery();
                rs.next();
                return rs.getInt(1);
            }
            rs.first();
            return rs.getInt(1);
        } catch (SQLException e) { return 1; }
    }

    public static int addAssociation(int wordId, int sentenceId, int weight){
        try {
            int rowCounter = 0;
            PreparedStatement selectSt = db.prepareStatement("SELECT id, weight FROM association WHERE word_id = ? AND sentence_id = ?");
            PreparedStatement insertSt = db.prepareStatement("INSERT INTO association (word_id, sentence_id, weight) VALUES (?, ?, ?) RETURNING id");
            PreparedStatement updateSt = db.prepareStatement("UPDATE association SET weight = ? WHERE id = ?");

            selectSt.setInt(1, wordId);
            selectSt.setInt(2, sentenceId);
            ResultSet rs = selectSt.executeQuery();

            while (rs.next()) rowCounter++;

            if (rowCounter == 0) {
                insertSt.setInt(1, wordId);
                insertSt.setInt(2, sentenceId);
                insertSt.setInt(3, weight);
                rs = insertSt.executeQuery();
                rs.next();
                return rs.getInt(1);
            }
            rs.first();
            updateSt.setInt(1, weight + rs.getInt(2));
            updateSt.setInt(2, rs.getInt(1));
            updateSt.executeUpdate();
            return rs.getInt(1);

        } catch (SQLException e) { return 1; }
    }

    public static void addChat(String interlocutor, int sentenceId){
        try {
            PreparedStatement st = db.prepareStatement("INSERT INTO chat (interlocutor, sentence_id) VALUES (?, ?)");
            st.setString(1, interlocutor);
            st.setInt(2, sentenceId);
            st.executeUpdate();

        } catch (SQLException e) { }
    }
}
