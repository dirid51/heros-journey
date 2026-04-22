package org.bhp.heros_journey;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Data
public class Player implements Serializable {
    private int currentHealth = 100;
    private int maxHealth = 100;

    /**
     * Represented as a decimal (0.0 to 1.0).
     * 0.1 means 10% damage reduction.
     */
    private double injuryReduction = 0.0;

    private Map<String, Integer> skills = new HashMap<>(); // skill name, level
    private Map<String, Integer> skillXp = new HashMap<>(); // skill name, xp

    /**
     * Logic: The game ends only if health is BELOW zero.
     * 0 HP is technically "clinging to life."
     */
    public boolean isDead() {
        return this.currentHealth < 0;
    }

    /**
     * Reduces incoming damage based on the injuryReduction stat.
     */
    public void receiveDamage(int rawDamage) {
        if (rawDamage <= 0) return;

        // Example: 20 damage with 0.1 reduction = 18 damage taken.
        int finalDamage = (int) Math.round(rawDamage * (1.0 - injuryReduction));
        this.currentHealth -= finalDamage;
    }

    /**
     * REFRESH: A one-time boost to current health.
     * Usually capped at maxHealth, unless you want "Over-healing."
     */
    public void addCurrentHealthBoost(int amount) {
        this.currentHealth += amount;
        if (this.currentHealth > this.maxHealth) {
            this.currentHealth = this.maxHealth;
        }
    }

    /**
     * PERMANENT: Increases the total capacity for health.
     */
    public void increaseMaxHealth(int amount) {
        this.maxHealth += amount;
        // Usually, increasing max health also heals you by that amount
        this.currentHealth += amount;
    }

    /**
     * ARMOR: Increases resistance to future damage.
     * Caps at 0.9 (90% reduction) to prevent literal godhood.
     */
    public void improveInjuryReduction(double gain) {
        this.injuryReduction = Math.min(0.9, this.injuryReduction + gain);
    }

    /**
     * Returns an unmodifiable view of the skill XP map to prevent external mutations.
     * Use updateSkillXp() to modify XP values.
     */
    public Map<String, Integer> getSkillXp() {
        return Collections.unmodifiableMap(skillXp);
    }

    /**
     * Updates the XP for a specific skill.
     * Use this method instead of mutating the skillXp map directly.
     *
     * @param skillName the name of the skill
     * @param xp the new XP value
     */
    public void updateSkillXp(String skillName, int xp) {
        this.skillXp.put(skillName, xp);
    }
}