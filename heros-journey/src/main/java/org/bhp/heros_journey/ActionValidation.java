package org.bhp.heros_journey;

public record ActionValidation(boolean canDo,
                               int initialLevel,
                               String description,
                               String skillName){
}
