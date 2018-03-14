package net.minespree.feather.achievements;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import net.minespree.feather.repository.types.BooleanType;
import net.minespree.feather.repository.types.Type;

import java.util.Collection;
import java.util.Set;

public class AchievementBuilder {
    private static final Type DEFAULT_TYPE = new BooleanType();
    protected final Set<AchievementReward> rewards = Sets.newLinkedHashSet();
    protected String id = null;
    protected String babel = null;
    protected String babelDescription = null;
    protected Type type = DEFAULT_TYPE;
    protected Object defaultValue = null;

    public AchievementBuilder() {
    }

    public AchievementBuilder id(String id) {
        Preconditions.checkNotNull(id);

        this.id = id;
        return this;
    }

    public AchievementBuilder name(String babelId) {
        Preconditions.checkNotNull(babelId);

        this.babel = babelId;
        return this;
    }

    public AchievementBuilder description(String babelId) {
        Preconditions.checkNotNull(babelId);

        this.babelDescription = babelId;
        return this;
    }

    public AchievementBuilder reward(AchievementReward reward) {
        Preconditions.checkNotNull(reward);

        this.rewards.add(reward);
        return this;
    }

    /**
     * Clears previous set rewards and adds all the new rewards to
     * the resultant {@link SimpleAchievement} obtained with {@link #get()}
     */
    public AchievementBuilder rewards(Collection<AchievementReward> rewards) {
        Preconditions.checkNotNull(rewards);

        this.rewards.clear();
        this.rewards.addAll(rewards);
        return this;
    }

    public AchievementBuilder type(Type type) {
        Preconditions.checkNotNull(type);

        // Reset default value if it doesn't work with the new type
        if (this.defaultValue != null && !type.isInstance(defaultValue)) {
            this.defaultValue = null;
        }

        this.type = type;
        return this;
    }

    public AchievementBuilder defaultValue(Object defaultValue) {
        Preconditions.checkState(type != null, "Achievement must have a type before a default value can be set");
        Preconditions.checkNotNull(defaultValue);
        Preconditions.checkArgument(type.isInstance(defaultValue), "Default value must be an instance of the provided type");

        this.defaultValue = defaultValue;
        return this;
    }

    public Achievement get() throws IllegalStateException {
        Preconditions.checkNotNull(id, "Achievement must have id");
        Preconditions.checkNotNull(babel, "Achievement must have babel name");
        Preconditions.checkNotNull(babelDescription, "Achievement must have babel description");
        Preconditions.checkNotNull(type, "Achievement must have type");

        if (defaultValue == null) {
            // Might be null
            defaultValue = type.getDefault();
        }

        return new SimpleAchievement(id, babel, babelDescription, rewards, type, defaultValue);
    }
}
