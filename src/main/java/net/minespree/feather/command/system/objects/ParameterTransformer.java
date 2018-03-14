package net.minespree.feather.command.system.objects;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public interface ParameterTransformer {
    ParameterTransformer NONE = (sender, source) -> null;

    ErrorHandler NO_HANDLER = new ErrorHandler(null, null) {
        @Override
        public boolean verify(Object o) {
            return true;
        }

        @Override
        void startConversation(Player sender, Runnable next) {
            // It is assumed that you already send the message at this point
        }

        @Override
        public boolean hasException() {
            return false;
        }
    };

    Object transform(CommandSender sender, String source);

    default List<String> complete(CommandSender sender, String arg) {
        return Collections.emptyList();
    }

    default Object tryTransform(CommandSender sender, String source, ParamData parameter) {
        Exception exception = null;
        Object o;
        try {
            o = this.transform(sender, source);
        } catch (Exception ex) {
            exception = ex;
            o = null;
        }
        ErrorHandler handler = this.getErrorHandler(this, parameter, exception);
        if (!handler.hasException() && handler.verify(o)) {
            return o;
        } else {
            return handler;
        }
    }

    /**
     * SOON™
     *
     * @return a new instance of ErrorHandler for <i>every</i> call
     */
    default ErrorHandler getErrorHandler(ParameterTransformer transformer, ParamData data, Exception exception) {
        return NO_HANDLER;
    }
}
