package lib.persistence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import lib.object.GameObjectType;
import lib.object.dto.MapData;
import lib.object.dto.ObjectData;

/**
 * 地图持久化仓库，基于 SQLite 数据库。
 * 处理地图元数据及其包含的所有游戏对象的 CRUD 操作。
 */
public final class MapRepository {
    private static final String DB_FILE_NAME = "maps.db";
    private final Path dbPath;

    /**
     * 使用默认路径创建仓库实例。
     */
    public MapRepository() {
        this(resolveDefaultPath());
    }

    /**
     * 为指定的数据库路径创建仓库。
     * 
     * @param dbPath 数据库文件路径
     */
    public MapRepository(Path dbPath) {
        this.dbPath = dbPath;
        initializeSchema();
    }

    /**
     * 保存或更新地图。
     * 
     * @param mapData 地图数据
     * @return 数据库中的地图 ID
     */
    public long saveMap(MapData mapData) {
        if (mapData == null) {
            return 0L;
        }
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            long mapId = upsertMap(connection, mapData);
            mapData.setId(mapId);
            deleteObjects(connection, mapId);
            insertObjects(connection, mapId, mapData.getObjects());
            connection.commit();
            return mapId;
        } catch (SQLException ex) {
            throw new IllegalStateException("保存地图失败：" + ex.getMessage(), ex);
        }
    }

    /**
     * 根据 ID 加载地图。
     * 
     * @param mapId 地图 ID
     * @return 包含所有对象的 MapData，如果不存在则返回 null
     */
    public MapData loadMapById(long mapId) {
        if (mapId <= 0) {
            return null;
        }
        try (Connection connection = openConnection()) {
            MapData mapData = fetchMapById(connection, mapId);
            if (mapData == null) {
                return null;
            }
            mapData.setObjects(fetchObjects(connection, mapId));
            return mapData;
        } catch (SQLException ex) {
            throw new IllegalStateException("加载地图失败：" + ex.getMessage(), ex);
        }
    }

    /**
     * 根据名称加载地图。
     * 
     * @param name 地图唯一名称
     * @return 包含所有对象的 MapData，如果不存在则返回 null
     */
    public MapData loadMapByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try (Connection connection = openConnection()) {
            MapData mapData = fetchMapByName(connection, name);
            if (mapData == null) {
                return null;
            }
            mapData.setObjects(fetchObjects(connection, mapData.getId()));
            return mapData;
        } catch (SQLException ex) {
            throw new IllegalStateException("加载地图失败：" + ex.getMessage(), ex);
        }
    }

    /**
     * 获取库中所有地图的名称列表。
     * 
     * @return 按名称排序的字符串列表
     */
    public List<String> listMapNames() {
        List<String> names = new ArrayList<>();
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT name FROM maps ORDER BY name COLLATE NOCASE"
             );
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String name = resultSet.getString("name");
                if (name != null && !name.isBlank()) {
                    names.add(name);
                }
            }
            return names;
        } catch (SQLException ex) {
            throw new IllegalStateException("加载地图列表失败：" + ex.getMessage(), ex);
        }
    }

    private Connection openConnection() throws SQLException {
        String url = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        return DriverManager.getConnection(url);
    }

    private void initializeSchema() {
        try {
            Files.createDirectories(dbPath.getParent());
        } catch (Exception ex) {
            throw new IllegalStateException("无法创建数据库目录：" + ex.getMessage(), ex);
        }

        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS maps ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "name TEXT NOT NULL UNIQUE,"
                    + "width INTEGER NOT NULL,"
                    + "height INTEGER NOT NULL,"
                    + "background_color INTEGER NOT NULL,"
                    + "gravity_enabled INTEGER NOT NULL DEFAULT 0,"
                    + "gravity_strength INTEGER NOT NULL DEFAULT 900"
                    + ")"
            );
            statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS map_objects ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "map_id INTEGER NOT NULL,"
                    + "type TEXT NOT NULL,"
                    + "name TEXT NOT NULL,"
                    + "x INTEGER NOT NULL,"
                    + "y INTEGER NOT NULL,"
                    + "width INTEGER NOT NULL,"
                    + "height INTEGER NOT NULL,"
                + "color INTEGER NOT NULL,"
                + "solid INTEGER NOT NULL,"
                + "background INTEGER NOT NULL,"
                + "texture_path TEXT,"
                + "material TEXT,"
                + "extra_json TEXT NOT NULL,"
                + "FOREIGN KEY(map_id) REFERENCES maps(id) ON DELETE CASCADE"
                + ")"
            );
            ensureMapColumn(connection, "gravity_enabled", "INTEGER NOT NULL DEFAULT 0");
            ensureMapColumn(connection, "gravity_strength", "INTEGER NOT NULL DEFAULT 900");
            ensureObjectColumn(connection, "texture_path", "TEXT");
            ensureObjectColumn(connection, "material", "TEXT");
        } catch (SQLException ex) {
            throw new IllegalStateException("初始化数据库失败：" + ex.getMessage(), ex);
        }
    }

    private long upsertMap(Connection connection, MapData mapData) throws SQLException {
        Long existingId = findMapIdByName(connection, mapData.getName());
        if (mapData.getId() > 0) {
            updateMap(connection, mapData);
            return mapData.getId();
        }
        if (existingId != null) {
            mapData.setId(existingId);
            updateMap(connection, mapData);
            return existingId;
        }
        return insertMap(connection, mapData);
    }

    private void updateMap(Connection connection, MapData mapData) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "UPDATE maps SET name = ?, width = ?, height = ?, background_color = ?, gravity_enabled = ?, gravity_strength = ? WHERE id = ?"
        )) {
            statement.setString(1, mapData.getName());
            statement.setInt(2, mapData.getWidth());
            statement.setInt(3, mapData.getHeight());
            statement.setInt(4, mapData.getBackgroundColor().getRGB());
            statement.setInt(5, mapData.isGravityEnabled() ? 1 : 0);
            statement.setInt(6, mapData.getGravityStrength());
            statement.setLong(7, mapData.getId());
            statement.executeUpdate();
        }
    }

    private long insertMap(Connection connection, MapData mapData) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO maps (name, width, height, background_color, gravity_enabled, gravity_strength) VALUES (?, ?, ?, ?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS
        )) {
            statement.setString(1, mapData.getName());
            statement.setInt(2, mapData.getWidth());
            statement.setInt(3, mapData.getHeight());
            statement.setInt(4, mapData.getBackgroundColor().getRGB());
            statement.setInt(5, mapData.isGravityEnabled() ? 1 : 0);
            statement.setInt(6, mapData.getGravityStrength());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        return 0L;
    }

    private void deleteObjects(Connection connection, long mapId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM map_objects WHERE map_id = ?")) {
            statement.setLong(1, mapId);
            statement.executeUpdate();
        }
    }

    private void insertObjects(Connection connection, long mapId, List<ObjectData> objects) throws SQLException {
        if (objects == null || objects.isEmpty()) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO map_objects (map_id, type, name, x, y, width, height, color, solid, background, texture_path, material, extra_json)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        )) {
            for (ObjectData object : objects) {
                statement.setLong(1, mapId);
                statement.setString(2, object.getType().name());
                statement.setString(3, object.getName());
                statement.setInt(4, object.getX());
                statement.setInt(5, object.getY());
                statement.setInt(6, object.getWidth());
                statement.setInt(7, object.getHeight());
                statement.setInt(8, object.getColor().getRGB());
                statement.setInt(9, object.isSolid() ? 1 : 0);
                statement.setInt(10, object.isBackground() ? 1 : 0);
                statement.setString(11, object.getTexturePath());
                statement.setString(12, object.getMaterial());
                statement.setString(13, object.getExtraJson());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private MapData fetchMapById(Connection connection, long mapId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT id, name, width, height, background_color, gravity_enabled, gravity_strength FROM maps WHERE id = ?"
        )) {
            statement.setLong(1, mapId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return mapFromRow(resultSet);
            }
        }
    }

    private MapData fetchMapByName(Connection connection, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT id, name, width, height, background_color, gravity_enabled, gravity_strength FROM maps WHERE name = ?"
        )) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return mapFromRow(resultSet);
            }
        }
    }

    private MapData mapFromRow(ResultSet resultSet) throws SQLException {
        MapData mapData = new MapData();
        mapData.setId(resultSet.getLong("id"));
        mapData.setName(resultSet.getString("name"));
        mapData.setWidth(resultSet.getInt("width"));
        mapData.setHeight(resultSet.getInt("height"));
        mapData.setBackgroundColor(new java.awt.Color(resultSet.getInt("background_color"), true));
        mapData.setGravityEnabled(resultSet.getInt("gravity_enabled") == 1);
        mapData.setGravityStrength(resultSet.getInt("gravity_strength"));
        return mapData;
    }

    private List<ObjectData> fetchObjects(Connection connection, long mapId) throws SQLException {
        List<ObjectData> objects = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT id, map_id, type, name, x, y, width, height, color, solid, background, texture_path, material, extra_json"
                + " FROM map_objects WHERE map_id = ? ORDER BY id"
        )) {
            statement.setLong(1, mapId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ObjectData data = new ObjectData();
                    data.setId(resultSet.getLong("id"));
                    data.setMapId(resultSet.getLong("map_id"));
                    data.setType(GameObjectType.valueOf(resultSet.getString("type")));
                    data.setName(resultSet.getString("name"));
                    data.setX(resultSet.getInt("x"));
                    data.setY(resultSet.getInt("y"));
                    data.setWidth(resultSet.getInt("width"));
                    data.setHeight(resultSet.getInt("height"));
                    data.setColor(new java.awt.Color(resultSet.getInt("color"), true));
                    data.setSolid(resultSet.getInt("solid") == 1);
                    data.setBackground(resultSet.getInt("background") == 1);
                    data.setTexturePath(resultSet.getString("texture_path"));
                    data.setMaterial(resultSet.getString("material"));
                    data.setExtraJson(resultSet.getString("extra_json"));
                    objects.add(data);
                }
            }
        }
        return objects;
    }

    private Long findMapIdByName(Connection connection, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM maps WHERE name = ?")) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("id");
                }
            }
        }
        return null;
    }

    private void ensureMapColumn(Connection connection, String columnName, String columnDefinition) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("PRAGMA table_info(maps)");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String existingName = resultSet.getString("name");
                if (columnName.equalsIgnoreCase(existingName)) {
                    return;
                }
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE maps ADD COLUMN " + columnName + " " + columnDefinition);
        }
    }

    private void ensureObjectColumn(Connection connection, String columnName, String columnDefinition) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("PRAGMA table_info(map_objects)");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String existingName = resultSet.getString("name");
                if (columnName.equalsIgnoreCase(existingName)) {
                    return;
                }
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE map_objects ADD COLUMN " + columnName + " " + columnDefinition);
        }
    }

    private static Path resolveDefaultPath() {
        String home = System.getProperty("user.home");
        return Paths.get(home, ".hnsfgame", DB_FILE_NAME);
    }
}
