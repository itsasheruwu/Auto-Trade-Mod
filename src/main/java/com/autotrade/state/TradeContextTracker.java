package com.autotrade.state;

import net.minecraft.entity.passive.VillagerEntity;

import java.util.UUID;

public final class TradeContextTracker {
    private UUID villagerUuid;
    private String professionId;

    public void capture(VillagerEntity villager) {
        villagerUuid = villager.getUuid();
        professionId = villager.getVillagerData()
                .profession()
                .getKey()
                .map(key -> key.getValue().toString())
                .orElse("minecraft:none");
    }

    public UUID getVillagerUuid() {
        return villagerUuid;
    }

    public String getProfessionId() {
        return professionId;
    }
}
