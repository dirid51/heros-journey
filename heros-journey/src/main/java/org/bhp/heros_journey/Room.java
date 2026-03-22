package org.bhp.heros_journey;

import lombok.Data;

import java.util.List;

public record Room(
        String title,
        String description,
        List<Exit> exits,
        List<String> skillOpportunities
) {
}