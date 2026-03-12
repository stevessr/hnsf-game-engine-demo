package lib.manager;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lib.object.GameObjectType;
import lib.object.MenuObject;
import lib.object.MonsterObject;
import lib.object.PlayerObject;
import lib.object.WallObject;

class EntityManagerTest {
    @Test
    void entityManagerShouldManageFilteringAndRemoval() {
        EntityManager entityManager = new EntityManager();
        PlayerObject player = new PlayerObject("hero", 10, 10);
        MonsterObject monster = new MonsterObject("slime", 30, 10, 5);
        WallObject wall = new WallObject("wall", 50, 10, 16, 16);
        MenuObject menu = new MenuObject("menu", 0, 0, 80, 60, "Main", java.util.List.of("Start"));
        monster.setActive(false);

        entityManager.add(player);
        entityManager.add(monster);
        entityManager.add(wall);
        entityManager.add(menu);

        assertEquals(4, entityManager.getObjects().size());
        assertEquals(3, entityManager.getActiveObjects().size());
        assertEquals(1, entityManager.getObjectsByType(GameObjectType.WALL).size());
        assertEquals(1, entityManager.getSolidScenes().size());
        assertTrue(entityManager.findFirstByType(GameObjectType.PLAYER).isPresent());
        assertTrue(entityManager.remove(menu));
        assertFalse(entityManager.remove(menu));
    }
}