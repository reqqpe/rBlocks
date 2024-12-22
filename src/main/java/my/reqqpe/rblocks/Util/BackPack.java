package my.reqqpe.rblocks.Util;

import java.util.HashMap;
import java.util.UUID;

public class BackPack {
    private final HashMap<UUID, Integer> backpack = new HashMap<>();

    public HashMap<UUID, Integer> getBackpack() {
        return backpack;
    }
    public void addValue(UUID uuid, Integer value) {
        backpack.put(uuid, value);
    }
    public int getBackpackFill(UUID playerId) {
        return backpack.getOrDefault(playerId, 0);

    }
    public boolean isPlayer(UUID uuid) {
        return backpack.containsKey(uuid);
    }
}
