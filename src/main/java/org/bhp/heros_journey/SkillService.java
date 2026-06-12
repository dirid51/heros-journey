package org.bhp.heros_journey;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class SkillService {
    // Loaded from skills.json

    public boolean isDiscoverable(String skillName, Set<String> unlocked) {
        // returns true if skill has no prerequisites
    }

    public boolean isAvailable(String skillName, Set<String> discovered, Set<String> unlocked) {
        // base skills: in discovered set
        // derived skills: all prerequisites in unlocked set
    }

    public int getCost(String skillName) {
        // based on prerequisite chain depth
    }

    public boolean canUnlock(String skillName, Set<String> discovered,
                             Set<String> unlocked, int currentSP) {
        return isAvailable(skillName, discovered, unlocked)
                && currentSP >= getCost(skillName);
    }

    public List<SkillDefinition> getAvailableSkills(Set<String> discovered, Set<String> unlocked) {
        // returns all purchasable skills sorted by cost/category
    }

    public List<String> getAllSkillNames() {
        // for feeding to the AI prompt
    }
}