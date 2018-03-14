package net.minespree.feather.repository.player;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import lombok.Getter;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.feather.repository.Element;
import net.minespree.feather.repository.RepoCallbackManager;
import net.minespree.feather.repository.RepoRegistry;
import net.minespree.feather.repository.base.AbstractRepoManager;
import net.minespree.feather.repository.types.TypeParseException;
import org.bson.Document;

import java.util.Map;

@Getter
public class PlayerRepoManager<T extends Element> extends AbstractRepoManager<T> {
    protected final NetworkPlayer player;
    protected final RepoCallbackManager<T> callbackManager;

    protected final Map<T, Object> elements;

    public PlayerRepoManager(NetworkPlayer player, RepoCallbackManager<T> callbackManager) {
        Preconditions.checkNotNull(player);
        Preconditions.checkNotNull(callbackManager);

        this.player = player;
        this.callbackManager = callbackManager;
        this.elements = Maps.newHashMap();
    }

    public void loadData(RepoRegistry<T> registry, Document document) {
        document.forEach((id, stringValue) -> {
            T element = registry.get(id);

            if (element == null) {
                return;
            }

            // TODO See when Java will support catching lambda exceptions
            try {
                Object value = element.getType().parse((String) stringValue);

                this.elements.put(element, value);
            } catch (TypeParseException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public Object getRawValue(T element) {
        Preconditions.checkNotNull(element);

        Object value = elements.get(element);

        if (value != null && element.getType().isInstance(value)) {
            return value;
        }

        return null;
    }

    @Override
    protected void setRawValue(T element, Object value) {
        if (value == null) {
            elements.remove(element);
        } else {
            elements.put(element, value);
        }
    }
}
