import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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

    public static void addSentence(String sentence){
        try {
            PreparedStatement st = db.prepareStatement("INSERT INTO sentence (text) VALUES (?)");
            st.setString(1, sentence);
            st.executeUpdate();
        } catch (SQLException e) { }
    }

    public static void addWord(String word){
        try {
            PreparedStatement st = db.prepareStatement("INSERT INTO word (text) VALUES (?)");
            st.setString(1, word);
            st.executeUpdate();
        } catch (SQLException e) { }
    }

    public static void addAssociation(int wordId, int sentenceId){
        try {
            PreparedStatement st = db.prepareStatement("INSERT INTO association (word_id, sentence_id) VALUES (?, ?)");
            st.setInt(1, wordId);
            st.setInt(2, sentenceId);
            st.executeUpdate();
        } catch (SQLException e) { }
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
