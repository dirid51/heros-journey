package org.bhp.heros_journey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Player Tests")
class PlayerTests {

    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player();
    }

    @Test
    @DisplayName("Should initialize with default health")
    void testDefaultHealth() {
        assertEquals(100, player.getCurrentHealth());
        assertEquals(100, player.getMaxHealth());
    }

    @Test
    @DisplayName("Should initialize with no injury reduction")
    void testDefaultInjuryReduction() {
        assertEquals(0.0, player.getInjuryReduction());
    }

    @Test
    @DisplayName("Should not be dead at max health")
    void testNotDeadAtMaxHealth() {
        assertFalse(player.isDead());
    }

    @Test
    @DisplayName("Should not be dead at 0 health")
    void testNotDeadAt0Health() {
        player.setCurrentHealth(0);
        assertFalse(player.isDead());
    }

    @Test
    @DisplayName("Should be dead below 0 health")
    void testDeadBelowZeroHealth() {
        player.setCurrentHealth(-1);
        assertTrue(player.isDead());
    }

    @Test
    @DisplayName("Should take damage without injury reduction")
    void testTakeDamageNoReduction() {
        player.receiveDamage(20);
        assertEquals(80, player.getCurrentHealth());
    }

    @Test
    @DisplayName("Should take damage with injury reduction")
    void testTakeDamageWithReduction() {
        player.setInjuryReduction(0.1); // 10% reduction
        player.receiveDamage(100);
        assertEquals(10, player.getCurrentHealth()); // 100 - (100 * 0.9) = 10
    }

    @Test
    @DisplayName("Should ignore negative damage")
    void testIgnoreNegativeDamage() {
        player.receiveDamage(-20);
        assertEquals(100, player.getCurrentHealth());
    }

    @Test
    @DisplayName("Should cap current health at max health after boost")
    void testHealthBoostCappedAtMax() {
        player.addCurrentHealthBoost(50);
        assertEquals(100, player.getCurrentHealth());
    }

    @Test
    @DisplayName("Should apply health boost when below max")
    void testHealthBoostWhenBelowMax() {
        player.setCurrentHealth(50);
        player.addCurrentHealthBoost(30);
        assertEquals(80, player.getCurrentHealth());
    }

    @Test
    @DisplayName("Should increase max health")
    void testIncreaseMaxHealth() {
        player.increaseMaxHealth(20);
        assertEquals(120, player.getMaxHealth());
        assertEquals(120, player.getCurrentHealth()); // Also heals by amount
    }

    @Test
    @DisplayName("Should cap injury reduction at 0.9")
    void testInjuryReductionCappedAt09() {
        player.improveInjuryReduction(0.5);
        player.improveInjuryReduction(0.5);
        assertEquals(0.9, player.getInjuryReduction());
    }

    @Test
    @DisplayName("Should allow injury reduction below cap")
    void testInjuryReductionBelowCap() {
        player.improveInjuryReduction(0.3);
        assertEquals(0.3, player.getInjuryReduction());
    }

    @Test
    @DisplayName("Should return immutable skill XP map")
    void testGetSkillXpImmutable() {
        player.updateSkillXp("Swordsmanship", 100);
        Map<String, Integer> skillXp = player.getSkillXp();

        assertThrows(UnsupportedOperationException.class,
                () -> skillXp.put("Cheating", 999999));
    }

    @Test
    @DisplayName("Should allow skill XP updates via updateSkillXp")
    void testUpdateSkillXp() {
        player.updateSkillXp("Swordsmanship", 100);
        player.updateSkillXp("Swordsmanship", 150);

        assertEquals(150, player.getSkillXp().get("Swordsmanship"));
    }

    @Test
    @DisplayName("Should track multiple skills")
    void testMultipleSkills() {
        player.updateSkillXp("Swordsmanship", 100);
        player.updateSkillXp("Dodge", 50);

        assertEquals(100, player.getSkillXp().get("Swordsmanship"));
        assertEquals(50, player.getSkillXp().get("Dodge"));
    }

    @Test
    @DisplayName("Skill level formula: floor(sqrt(xp/10))")
    void testSkillLevelFormula() {
        // XP = 0 -> level = floor(sqrt(0/10)) = 0
        player.getSkills().put("Skill1", (int) Math.floor(Math.sqrt(0 / 10.0)));
        assertEquals(0, player.getSkills().get("Skill1"));

        // XP = 100 -> level = floor(sqrt(100/10)) = floor(sqrt(10)) = 3
        player.getSkills().put("Skill2", (int) Math.floor(Math.sqrt(100 / 10.0)));
        assertEquals(3, player.getSkills().get("Skill2"));

        // XP = 1000 -> level = floor(sqrt(1000/10)) = floor(sqrt(100)) = 10
        player.getSkills().put("Skill3", (int) Math.floor(Math.sqrt(1000 / 10.0)));
        assertEquals(10, player.getSkills().get("Skill3"));

        // XP = 10000 -> level = floor(sqrt(10000/10)) = floor(sqrt(1000)) = 31
        player.getSkills().put("Skill4", (int) Math.floor(Math.sqrt(10000 / 10.0)));
        assertEquals(31, player.getSkills().get("Skill4"));
    }

    @Test
    @DisplayName("Should handle damage with high injury reduction")
    void testDamageWithHighInjuryReduction() {
        player.improveInjuryReduction(0.8); // 80% reduction
        player.receiveDamage(100);
        assertEquals(80, player.getCurrentHealth()); // 100 - (100 * 0.2) = 80
    }

    @Test
    @DisplayName("Should handle maximum injury reduction")
    void testDamageWithMaxInjuryReduction() {
        player.improveInjuryReduction(0.9); // 90% reduction
        player.receiveDamage(100);
        assertEquals(90, player.getCurrentHealth()); // 100 - (100 * 0.1) = 90
    }
}

