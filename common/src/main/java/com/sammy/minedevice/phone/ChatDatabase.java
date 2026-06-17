package com.sammy.minedevice.phone;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class ChatDatabase {
    private static final String CREATE_FRIENDS_TABLE = """
        CREATE TABLE IF NOT EXISTS chat_friends (
            owner_uuid TEXT NOT NULL,
            friend_number TEXT NOT NULL,
            friend_name TEXT NOT NULL,
            profile_id TEXT,
            PRIMARY KEY (owner_uuid, friend_number)
        )
        """;

    private static final String CREATE_MESSAGES_TABLE = """
        CREATE TABLE IF NOT EXISTS chat_messages (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            owner_uuid TEXT NOT NULL,
            other_number TEXT NOT NULL,
            message_text TEXT NOT NULL,
            is_incoming INTEGER NOT NULL,
            timestamp INTEGER NOT NULL,
            other_name TEXT,
            other_profile_id TEXT
        )
        """;

    private static final String CREATE_MESSAGES_INDEX =
            "CREATE INDEX IF NOT EXISTS idx_messages_owner_number_time ON chat_messages(owner_uuid, other_number, timestamp DESC)";
    private static final String CREATE_FRIENDS_INDEX =
            "CREATE INDEX IF NOT EXISTS idx_friends_owner ON chat_friends(owner_uuid)";

    private final Connection connection;
    private final ReentrantReadWriteLock dbLock = new ReentrantReadWriteLock();

    public ChatDatabase(String dbPath) {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            initializeDatabase();
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException("Failed to initialize chat database", e);
        }
    }

    private void initializeDatabase() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA synchronous=NORMAL");
            stmt.execute("PRAGMA temp_store=MEMORY");
            stmt.execute("PRAGMA busy_timeout=3000");
            stmt.execute(CREATE_FRIENDS_TABLE);
            stmt.execute(CREATE_MESSAGES_TABLE);
            stmt.execute(CREATE_MESSAGES_INDEX);
            stmt.execute(CREATE_FRIENDS_INDEX);
        }
    }

    public void applyBatch(List<ChatCache.BatchOperation> operations) {
        if (operations == null || operations.isEmpty()) {
            return;
        }

        dbLock.writeLock().lock();
        try {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                for (ChatCache.BatchOperation operation : operations) {
                    switch (operation.type()) {
                        case ADD_MESSAGE -> addMessageInternal(
                                operation.ownerUuid(),
                                operation.otherNumber(),
                                operation.messageText(),
                                operation.isIncoming(),
                                operation.otherName(),
                                operation.otherProfileId()
                        );
                        case ADD_FRIEND -> addFriendInternal(
                                operation.ownerUuid(),
                                operation.otherNumber(),
                                operation.otherName(),
                                operation.otherProfileId()
                        );
                        case REMOVE_FRIEND -> removeFriendInternal(operation.ownerUuid(), operation.otherNumber());
                        case REMOVE_CONVERSATION -> removeConversationInternal(operation.ownerUuid(), operation.otherNumber());
                    }
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to save chat batch", exception);
        } finally {
            dbLock.writeLock().unlock();
        }
    }

    public boolean addFriend(UUID ownerUuid, String friendNumber, String friendName, UUID profileId) {
        dbLock.writeLock().lock();
        try {
            return addFriendInternal(ownerUuid, friendNumber, friendName, profileId);
        } catch (SQLException e) {
            return false;
        } finally {
            dbLock.writeLock().unlock();
        }
    }

    public List<StoredFriend> getFriends(UUID ownerUuid) {
        dbLock.readLock().lock();
        try {
            String sql = "SELECT friend_name, friend_number, profile_id FROM chat_friends WHERE owner_uuid = ?";
            List<StoredFriend> friends = new ArrayList<>();
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, ownerUuid.toString());
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    friends.add(new StoredFriend(
                            rs.getString("friend_name"),
                            rs.getString("friend_number"),
                            parseUuid(rs.getString("profile_id"))
                    ));
                }
            } catch (SQLException ignored) {
            }
            return friends;
        } finally {
            dbLock.readLock().unlock();
        }
    }

    public boolean removeFriend(UUID ownerUuid, String friendNumber) {
        dbLock.writeLock().lock();
        try {
            return removeFriendInternal(ownerUuid, friendNumber);
        } catch (SQLException e) {
            return false;
        } finally {
            dbLock.writeLock().unlock();
        }
    }

    public boolean addMessage(UUID ownerUuid, String otherNumber, String messageText,
                              boolean isIncoming, String otherName, UUID otherProfileId) {
        dbLock.writeLock().lock();
        try {
            return addMessageInternal(ownerUuid, otherNumber, messageText, isIncoming, otherName, otherProfileId);
        } catch (SQLException e) {
            return false;
        } finally {
            dbLock.writeLock().unlock();
        }
    }

    public List<PhoneChatMessage> getMessages(UUID ownerUuid, String otherNumber, int limit) {
        dbLock.readLock().lock();
        try {
            String sql = """
                    SELECT message_text, is_incoming
                    FROM chat_messages
                    WHERE owner_uuid = ? AND other_number = ?
                    ORDER BY timestamp DESC, id DESC
                    LIMIT ?
                    """;
            List<PhoneChatMessage> messages = new ArrayList<>();
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, ownerUuid.toString());
                pstmt.setString(2, otherNumber);
                pstmt.setInt(3, limit);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    messages.add(new PhoneChatMessage(rs.getString("message_text"), rs.getInt("is_incoming") == 1));
                }
                java.util.Collections.reverse(messages);
            } catch (SQLException ignored) {
            }
            return messages;
        } finally {
            dbLock.readLock().unlock();
        }
    }

    public boolean removeConversation(UUID ownerUuid, String otherNumber) {
        dbLock.writeLock().lock();
        try {
            return removeConversationInternal(ownerUuid, otherNumber);
        } catch (SQLException e) {
            return false;
        } finally {
            dbLock.writeLock().unlock();
        }
    }

    public String getFriendName(UUID ownerUuid, String friendNumber) {
        dbLock.readLock().lock();
        try {
            String sql = "SELECT friend_name FROM chat_friends WHERE owner_uuid = ? AND friend_number = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, ownerUuid.toString());
                pstmt.setString(2, friendNumber);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getString("friend_name");
                }
            } catch (SQLException ignored) {
            }

            String sql2 = """
                    SELECT other_name
                    FROM chat_messages
                    WHERE owner_uuid = ? AND other_number = ?
                    ORDER BY timestamp DESC, id DESC
                    LIMIT 1
                    """;
            try (PreparedStatement pstmt = connection.prepareStatement(sql2)) {
                pstmt.setString(1, ownerUuid.toString());
                pstmt.setString(2, friendNumber);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getString("other_name");
                }
            } catch (SQLException ignored) {
            }
            return null;
        } finally {
            dbLock.readLock().unlock();
        }
    }

    public UUID getFriendProfileId(UUID ownerUuid, String friendNumber) {
        dbLock.readLock().lock();
        try {
            String sql = "SELECT profile_id FROM chat_friends WHERE owner_uuid = ? AND friend_number = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, ownerUuid.toString());
                pstmt.setString(2, friendNumber);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return parseUuid(rs.getString("profile_id"));
                }
            } catch (SQLException ignored) {
            }

            String sql2 = """
                    SELECT other_profile_id
                    FROM chat_messages
                    WHERE owner_uuid = ? AND other_number = ? AND other_profile_id IS NOT NULL
                    ORDER BY timestamp DESC, id DESC
                    LIMIT 1
                    """;
            try (PreparedStatement pstmt = connection.prepareStatement(sql2)) {
                pstmt.setString(1, ownerUuid.toString());
                pstmt.setString(2, friendNumber);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return parseUuid(rs.getString("other_profile_id"));
                }
            } catch (SQLException ignored) {
            }
            return null;
        } finally {
            dbLock.readLock().unlock();
        }
    }

    public void close() {
        dbLock.writeLock().lock();
        try {
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
        } finally {
            dbLock.writeLock().unlock();
        }
    }

    private boolean addFriendInternal(UUID ownerUuid, String friendNumber, String friendName, UUID profileId) throws SQLException {
        String sql = "INSERT OR REPLACE INTO chat_friends (owner_uuid, friend_number, friend_name, profile_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ownerUuid.toString());
            pstmt.setString(2, friendNumber);
            pstmt.setString(3, friendName);
            pstmt.setString(4, profileId != null ? profileId.toString() : null);
            return pstmt.executeUpdate() > 0;
        }
    }

    private boolean removeFriendInternal(UUID ownerUuid, String friendNumber) throws SQLException {
        String sql = "DELETE FROM chat_friends WHERE owner_uuid = ? AND friend_number = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ownerUuid.toString());
            pstmt.setString(2, friendNumber);
            return pstmt.executeUpdate() > 0;
        }
    }

    private boolean addMessageInternal(UUID ownerUuid, String otherNumber, String messageText,
                                       boolean isIncoming, String otherName, UUID otherProfileId) throws SQLException {
        String sql = """
                INSERT INTO chat_messages
                (owner_uuid, other_number, message_text, is_incoming, timestamp, other_name, other_profile_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ownerUuid.toString());
            pstmt.setString(2, otherNumber);
            pstmt.setString(3, messageText);
            pstmt.setInt(4, isIncoming ? 1 : 0);
            pstmt.setLong(5, System.currentTimeMillis());
            pstmt.setString(6, otherName);
            pstmt.setString(7, otherProfileId != null ? otherProfileId.toString() : null);
            return pstmt.executeUpdate() > 0;
        }
    }

    private boolean removeConversationInternal(UUID ownerUuid, String otherNumber) throws SQLException {
        String sql1 = "DELETE FROM chat_friends WHERE owner_uuid = ? AND friend_number = ?";
        String sql2 = "DELETE FROM chat_messages WHERE owner_uuid = ? AND other_number = ?";
        try (PreparedStatement pstmt1 = connection.prepareStatement(sql1);
             PreparedStatement pstmt2 = connection.prepareStatement(sql2)) {
            pstmt1.setString(1, ownerUuid.toString());
            pstmt1.setString(2, otherNumber);
            pstmt1.executeUpdate();

            pstmt2.setString(1, ownerUuid.toString());
            pstmt2.setString(2, otherNumber);
            return pstmt2.executeUpdate() > 0;
        }
    }

    private UUID parseUuid(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(rawValue);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public record StoredFriend(String displayName, String number, UUID profileId) {
    }
}
