package net.minespree.feather.crates;

import lombok.Getter;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.player.PlayerKey;
import org.bson.Document;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
public class PlayerCrateManager implements CrateManager {
    private NetworkPlayer player;
    private List<Crate> crates;

    public PlayerCrateManager(NetworkPlayer player, List<Document> crates) {
        this.player = player;

        this.crates = crates.stream()
                .map(object -> {
                    boolean bought = object.getBoolean("bought");
                    String typeId = object.getString("type");
                    CrateType type = CrateType.byId(typeId);

                    if (type == null) return null;

                    Date legacyObtained = object.getDate("obtained");
                    LocalDateTime obtained = dateToLocalDateTime(legacyObtained);

                    if (type.isSeasonal()) {
                        return new Crate(type, bought, obtained);
                    }

                    boolean superCrate = object.getBoolean("super");

                    return new Crate(type, bought, obtained, superCrate);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static LocalDateTime dateToLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    @Override
    public void addCrate(Crate crate) {
        player.addSetUpdate(PlayerKey.CRATES, crate.toDocument());
        crates.add(crate);
    }

    @Override
    public boolean removeCrate(Crate crate, boolean force) {
        int index = crates.indexOf(crate);

        if (index == -1) {
            return false;
        }

        if (!force && crate.expires() && crate.getType().isSeasonal()) {
            // Create new default crate and delete old one
            Crate normalCrate = new Crate(CrateType.DEFAULT, false, LocalDateTime.now(), false);
            addCrate(normalCrate);
        }

        // Mongo doesn't allow to remove elements by index:
        // https://stackoverflow.com/questions/4588303/in-mongodb-how-do-you-remove-an-array-element-by-its-index
        player.addSaveOperation("$unset", new Document(PlayerKey.CRATES + "." + index, 1));
        player.addSaveOperation("$pull", new Document(PlayerKey.CRATES, null));

        crates.remove(crate);
        return true;
    }

    @Override
    public void cleanupExpired() {
        crates.stream().filter(Crate::hasExpired).forEach(this::removeCrate);
    }
}
