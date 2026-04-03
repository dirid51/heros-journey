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

    public GameService(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("""
                                You are the logic engine for a procedural, text-based RPG. Your goal is to be an immersive narrator and a strict rule-keeper.
                        
                                ### CORE MECHANICS:
                                1. DEATH: The player only dies if health falls BELOW 0. 0 HP is "clinging to life."
                                2. SKILLS: Scale is 1-100. 1 = Novice, 100 = Godlike.
                                   - New skills should usually start between 5-15.
                                   - Skill increases should be rare (1-5 points) and only occur on significant effort.
                                3. INJURY REDUCTION: This is a percentage (0.0 to 0.9).
                                   - Awards should be tiny (e.g., 0.01 to 0.05) and extremely rare (e.g., finding
                                     magical armor or mastering "Iron Skin").
                                4. DAMAGE:
                                   - Minor: 5-10 HP
                                   - Major: 20-40 HP
                                   - Deadly: 60+ HP
                        
                                ### INTERACTION RULES:
                                    - Regardless of the possibility of the action, you MUST ALWAYS respond with a JSON object containing:
                                      - `canAttempt` (boolean) : indicates whether the player is capable of attempting this action based on their current skills and stats.
                                      - `skillName` (string) : ALWAYS provide a name for the new skill that would be relevant to this action. Be creative but concise, and tie it into the narrative of the room and the action.
                                      - `skillInitialLevel` (integer) : indicates the starting level of this new skill
                                      - `description` (string) : a creative description of what happens when the player attempts this action, taking into account their current stats, if canAttempt is true or false, the initial level of the new skill, and the difficulty involved with how the user is attempting to use the new skill. This should be flavorful and narrative-driven, not just mechanical. For example, if the player is trying to "pick a pocket" without any relevant skills, you might say something like: "You attempt to pick the noble's pocket, but your lack of finesse causes you to fumble and draw attention. The noble glares at you, and you quickly realize that this is going to be much harder than you thought. You feel a spark of potential in this new skill, but it's clear that you have a long way to go before you can master it."
                                      - `success` (boolean) : indicates whether the attempted action was successful or not
                                      - `levelIncreased` (boolean) : indicates whether the player has improved in this skill as a result of this attempt (whether they succeeded or failed), taking into account the effort involved and the potential for growth.
                                      - `newLevel` (integer) : indicates the new level of the skill if it increased, or the same level if it did not
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
                                        - Set `canDo: false`
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
                if (i > 0) Thread.sleep((long) (BASE_DELAY_MS * Math.pow(2, i - 1)));
                return action.call();
            } catch (Exception e) {
                last = e;
            }
        }
        throw new RuntimeException("LLM call failed after " + MAX_RETRIES + " attempts", last);
    }

    public String processAction(String userAction, Player player, Room currentRoom) {
        String identifiedSkill = identifyRelevantSkill(userAction, player);

        ActionOutcome outcome = identifiedSkill == null
                ? handleNewSkillAction(userAction, player, currentRoom)
                : handleExistingSkillAction(userAction, player, currentRoom, identifiedSkill);

        if (outcome.canAttempt()) {
            if (identifiedSkill == null) {
                player.getSkills().put(outcome.skillName(), outcome.skillInitialLevel());
            } else if (outcome.levelIncreased()) {
                player.getSkills().put(identifiedSkill, outcome.newLevel());
            }
            applyStateChanges(player, outcome);
        }

        return outcome.description();
    }

    private ActionOutcome handleNewSkillAction(String userAction, Player player, Room currentRoom) {
        return withRetries(() -> chatClient.prompt()
                .user(u -> u.text("""
                                Context: {roomDesc}
                                Action: {action}
                                Player Stats: {stats}
                                """)
                        .param("roomDesc", currentRoom.description())
                        .param("action", userAction)
                        .param("stats", player.toString())
                        .param("entities", getEntityDetails(currentRoom))) // Pass full text of NPCs/Items
                .call()
                .entity(ActionOutcome.class));
    }

    private ActionOutcome handleExistingSkillAction(String userAction, Player player, Room currentRoom, String skill) {
        return withRetries(() -> chatClient.prompt()
                .user(u -> u.text("""
                                Context: {roomDesc}
                                Action: {action}
                                Skill Used: {skill} (Level {level})
                                """)
                        .param("roomDesc", currentRoom.description())
                        .param("action", userAction)
                        .param("skill", skill)
                        .param("level", player.getSkills().get(skill)))
                .call()
                .entity(ActionOutcome.class));
    }

    private void applyStateChanges(Player player, ActionOutcome outcome) {
        // 1. Update Health and Armor
        player.increaseMaxHealth(outcome.maxHealthIncrease());
        player.addCurrentHealthBoost(outcome.healthBoost());
        player.improveInjuryReduction(outcome.injuryReductionGain());
        player.receiveDamage(outcome.damageTaken());

        // 2. Skill Calculation based on your formula: floor(sqrt(XP / 10))
        String skill = outcome.skillName();
        int currentXP = player.getSkillXp().getOrDefault(skill, 0); // You'll need an XP map in Player
        int newXP = currentXP + outcome.xpGained();

        player.getSkillXp().put(skill, newXP);

        // Calculate new level
        int newLevel = (int) Math.floor(Math.sqrt(newXP / 10.0));
        player.getSkills().put(skill, newLevel);
    }

    private String identifyRelevantSkill(String action, Player player) {
        // If the player has no skills yet, we can skip the LLM call entirely
        if (player.getSkills().isEmpty()) {
            return null;
        }

        // Convert the skill keys to a comma-separated string for the prompt
        String existingSkills = String.join(", ", player.getSkills().keySet());

        SkillMapping mapping = chatClient.prompt()
                .user(u -> u.text("""
                                A player is attempting this action: "{action}"
                                Their current known skills are: [{skills}]
                                
                                Determine if this action uses one of their existing skills.
                                - If it does, set isMatch to true and provide the exact skill name from the list.
                                - If the action describes something they don't have a skill for yet, set isMatch to false.
                                - Be reasonable with synonyms (e.g., 'punching' matches 'Unarmed Combat').
                                """)
                        .param("action", action)
                        .param("skills", existingSkills))
                .call()
                .entity(SkillMapping.class);

        return (mapping != null && mapping.isMatch()) ? mapping.matchedSkillName() : null;
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