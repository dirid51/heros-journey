package org.bhp.heros_journey;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Service
public class GameService {
    private final ChatClient chatClient;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 500L;

    /**
     * Prompt template for action resolution.
     * Contains per-request context: the current game state and the player's action.
     * Invariant rules are in the system prompt.
     */
    private static final String ACTION_RESOLUTION_PROMPT = """
            Room: {roomDesc}
            Entities present: {entities}
            Player stats: {stats}
            Player's known skills: {skills}
            Player action: "{action}"
            
            First, check if the player's known skills list contains a skill
            applicable to this action (be reasonable with synonyms, e.g.
            'punching' matches 'Unarmed Combat'). Then resolve the action.
            
            Return JSON with all fields.
            """;

    public GameService(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("""
                                You are the logic engine for a procedural, text-based RPG. Your goal is to be an immersive narrator and a strict rule-keeper.
                        
                                ### CORE MECHANICS:
                                1. DEATH: The player only dies if health falls BELOW 0. 0 HP is "clinging to life."
                                2. SKILLS: Scale is 1-100. 1 = Novice, 100 = Godlike.
                                   - Skill level is ALWAYS calculated from XP using: skill_level = floor(sqrt(xp / 10))
                                   - For new skills, calculate required XP: xp = skillInitialLevel * skillInitialLevel * 10
                                     (e.g., to start at level 10: xp = 100 * 10 = 1000)
                                   - New skills should usually start between 5-15 (unless canAttempt is false).
                                   - Skill increases should be rare (1-5 points) and only occur on significant effort.
                                3. INJURY REDUCTION: This is a percentage (0.0 to 0.9).
                                   - Awards should be tiny (e.g., 0.01 to 0.05) and extremely rare (e.g., finding
                                     magical armor or mastering "Iron Skin").
                                4. DAMAGE:
                                   - Minor: 5-10 HP
                                   - Major: 20-40 HP
                                   - Deadly: 60+ HP
                        
                                ### SKILL XP RULES (INVARIANT):
                                These rules are FIXED and override any per-action instructions:
                                - isExistingSkill: true
                                  * The action maps to a skill already in the known skills list.
                                  * Set skillName to the EXACT name from that list.
                                  * Only populate xpGained (add to current XP, system recalculates level).
                                  * skillInitialLevel and new XP calculations are ignored by the backend.
                                - isExistingSkill: false
                                  * This is a new skill. Infer an appropriate skillInitialLevel from adjacent skills.
                                  * Example: If player has "Lockpicking" lvl 20, new "Pickpocketing" starts at ~10.
                                  * Default to 5-15 if no adjacent skills exist.
                                  * If canAttempt is false, set skillInitialLevel to 0.
                                  * Set xpGained to the initial XP amount calculated from skillInitialLevel.
                        
                                ### INTERACTION RULES:
                                    - Regardless of the possibility of the action, you MUST ALWAYS respond with a JSON object containing:
                                      - `canAttempt` (boolean) : indicates whether the player is capable of attempting this action based on their current skills and stats.
                                      - `skillName` (string) : ALWAYS provide a name for the new skill that would be relevant to this action. Be creative but concise, and tie it into the narrative of the room and the action.
                                      - `skillInitialLevel` (integer) : indicates the starting level of this new skill (0 if canAttempt is false)
                                      - `isExistingSkill` (boolean) : true if skillName matches an existing skill, false otherwise
                                      - `description` (string) : a creative description of what happens when the player attempts this action, taking into account their current stats, if canAttempt is true or false, the initial level of the new skill, and the difficulty involved with how the user is attempting to use the new skill. This should be flavorful and narrative-driven, not just mechanical. For example, if the player is trying to "pick a pocket" without any relevant skills, you might say something like: "You attempt to pick the noble's pocket, but your lack of finesse causes you to fumble and draw attention. The noble glares at you, and you quickly realize that this is going to be much harder than you thought. You feel a spark of potential in this new skill, but it's clear that you have a long way to go before you can master it."
                                      - `success` (boolean) : indicates whether the attempted action was successful or not
                                      - `levelIncreased` (boolean) : indicates whether the player has improved in this skill as a result of this attempt (whether they succeeded or failed), taking into account the effort involved and the potential for growth.
                                      - `newLevel` (integer) : indicates the new level of the skill if it increased, or the same level if it did not
                                      - `xpGained` (integer) : XP to award (added to existing XP for the skill)
                                      - `damageTaken` (integer) : indicates any damage the player takes as a result of this action
                                      - `healthBoost` (integer) : indicates any one-time boosts to current health they might gain from this action (e.g., finding a healing potion or successfully completing a "Survival Challenge").
                                      - `maxHealthIncrease` (integer) : indicates any permanent increases to max health they might gain from this action (e.g., finding a magical artifact that increases vitality or successfully completing a "Heroic Feat").
                                      - `injuryReductionGain` (double) : indicates any increases to injury reduction they might gain from this action (e.g., finding magical armor or successfully completing a "Mastery Challenge").
                                    - If the action is possible (canAttempt is true):
                                        - Create a skill name
                                        - Define the appropriate skill level of the player, taking into account any
                                          skills (and their levels) the player has that can be considered adjacent
                                          to the new skill. For example, if the player has "Lockpicking" at level
                                          20, and they attempt to "pick a pocket," you might say this is possible
                                          with a new skill called "Pickpocketing" at level 10. If the
                                        - Provide a creative description of how they use this new skill. Remember
                                          that being *able* to do something doesn't necessarily mean they *succeed*
                                          at it. For example, if they have "Pickpocketing" at level 10, they might
                                          be able to attempt to pick a noble's pocket, but they might fail and get
                                          caught, resulting in damage or other consequences.
                                    - If the action is borderline (e.g., "try to swim across a river" when they have
                                      no swimming-related skills) but still possible:
                                        - Define a skill name
                                        - Set the initial level to 1
                                        - Provide a description that reflects the high difficulty and likely failure. For example: "You wade into the river, the current immediately proving stronger than you anticipated. With no prior swimming skills to rely on, you struggle to keep your head above water. The cold bites into you, and you realize this is going to be a daunting challenge. You manage to make it back to shore, soaked and exhausted, but alive. This new skill of 'Swimming' feels like a distant dream, something you might be able to develop with a lot of practice and determination, but for now, it's clear that you're out of your depth."
                                    - If the action is impossible or nonsensical:
                                        - Set canAttempt: false
                                        - Provide a creative and flavorful description of the failure, taking into account the narrative context.
                                        - Define a skill name
                                        - Set an initial level of 0 to indicate that they have no ability in this area yet
                                    - Be creative with the skill names and descriptions, and try to tie them into the narrative of the room and the action. For example, if they are in a library and try to "find a hidden passage," you might create a new skill called "Hidden Lore" or "Secret Searching."
                                    - If the player interacts with an object or person not explicitly listed in the room description, you are authorized to generate the NPC data or object effects on the fly to maintain immersion.
                        
                                ### NARRATIVE STYLE:
                                - Be descriptive and atmospheric.
                                - Rooms must feel distinct.
                                - Never allow the player to return to a previous room. The path behind them always vanishes, collapses, or is barred.
                        """)
                .build();
    }


    private <T> T withRetries(Callable<T> action) {
        Exception last = null;
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                if (i > 0) Thread.sleep((long) (BASE_DELAY_MS * Math.pow(2, i - 1.0)));
                return action.call();
            } catch (InterruptedException e) {
                // Re-interrupt the thread to preserve the interrupted status
                Thread.currentThread().interrupt();
                last = e;
            } catch (Exception e) {
                last = e;
            }
        }
        throw new LlmCallFailedException("LLM call failed after " + MAX_RETRIES + " attempts", MAX_RETRIES, last);
    }

    public String processAction(String userAction, Player player, Room currentRoom) {
        ActionOutcome outcome = withRetries(() -> chatClient.prompt()
                .user(u -> u.text(ACTION_RESOLUTION_PROMPT)
                        .param("roomDesc", currentRoom.description())
                        .param("entities", getEntityDetails(currentRoom))
                        .param("stats", player.toString())
                        .param("skills", player.getSkills().isEmpty()
                                ? "none" : player.getSkills().toString())
                        .param("action", PromptInjectionProtection.sanitizeWithLabel(userAction)))
                .call()
                .entity(ActionOutcome.class));

        if (outcome.canAttempt()) {
            applyStateChanges(player, outcome);
        }

        return outcome.description();
    }

    private void applyStateChanges(Player player, ActionOutcome outcome) {
        // 1. Update Health and Armor
        player.increaseMaxHealth(outcome.maxHealthIncrease());
        player.addCurrentHealthBoost(outcome.healthBoost());
        player.improveInjuryReduction(outcome.injuryReductionGain());
        player.receiveDamage(outcome.damageTaken());

        // 2. Single Source of Truth: Skill XP determines skill level
        // Using formula: skill_level = floor(sqrt(XP / 10))
        String skill = outcome.skillName();
        int currentXP = player.getSkillXp().getOrDefault(skill, 0);
        int newXP = currentXP + outcome.xpGained();

        player.getSkillXp().put(skill, newXP);

        // Calculate new level - single source of truth
        int newLevel = (int) Math.floor(Math.sqrt(newXP / 10.0));
        player.getSkills().put(skill, newLevel);
    }

    private String getEntityDetails(Room room) {
        // Combine NPC and item IDs into a readable string for the prompt
        return "NPCs: " + room.npcIds() + ", Items: " + room.itemIds();
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }
}