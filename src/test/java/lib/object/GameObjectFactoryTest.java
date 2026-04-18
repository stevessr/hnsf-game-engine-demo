package lib.object;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import lib.object.dto.ObjectData;

public class GameObjectFactoryTest {

    @Test
    public void testPlayerSerialization() {
        PlayerObject player = new PlayerObject("Hero", 100, 200);
        player.setHealth(50);
        player.setLevel(5);
        
        ObjectData data = GameObjectFactory.toObjectData(player);
        assertEquals(GameObjectType.PLAYER, data.getType());
        assertTrue(data.getExtraJson().contains("\"health\":50"));
        assertTrue(data.getExtraJson().contains("\"level\":5"));
        
        PlayerObject restored = (PlayerObject) GameObjectFactory.fromObjectData(data);
        assertEquals("Hero", restored.getName());
        assertEquals(100, restored.getX());
        assertEquals(200, restored.getY());
        assertEquals(50, restored.getHealth());
        assertEquals(5, restored.getLevel());
    }

    @Test
    public void testMonsterSerialization() {
        MonsterObject monster = new MonsterObject("Griz", 300, 400, 100);
        monster.setAggressive(true);
        monster.setAttack(15);
        
        ObjectData data = GameObjectFactory.toObjectData(monster);
        assertEquals(GameObjectType.MONSTER, data.getType());
        
        MonsterObject restored = (MonsterObject) GameObjectFactory.fromObjectData(data);
        assertEquals(100, restored.getRewardExperience());
        assertTrue(restored.isAggressive());
        assertEquals(15, restored.getAttack());
    }

    @Test
    public void testItemSerialization() {
        ItemObject item = new ItemObject("Coin", 50, 50, 32, 32, "gold", 50, "You found gold!");
        
        ObjectData data = GameObjectFactory.toObjectData(item);
        assertEquals(GameObjectType.ITEM, data.getType());
        
        ItemObject restored = (ItemObject) GameObjectFactory.fromObjectData(data);
        assertEquals("gold", restored.getKind());
        assertEquals(50, restored.getValue());
        assertEquals("You found gold!", restored.getMessage());
    }

    @Test
    public void testMenuSerialization() {
        MenuObject menu = new MenuObject("Options", 0, 0, 200, 150, "Settings", List.of("Easy", "Hard"));
        menu.setSelectedIndex(1);
        
        ObjectData data = GameObjectFactory.toObjectData(menu);
        assertEquals(GameObjectType.MENU, data.getType());
        
        MenuObject restored = (MenuObject) GameObjectFactory.fromObjectData(data);
        assertEquals("Settings", restored.getTitle());
        assertEquals(2, restored.getOptions().size());
        assertEquals(1, restored.getSelectedIndex());
    }
}
