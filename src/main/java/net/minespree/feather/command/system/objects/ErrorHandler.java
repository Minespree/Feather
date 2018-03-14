package net.minespree.feather.command.system.objects;

import net.minespree.feather.FeatherPlugin;
import org.bukkit.ChatColor;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.InactivityConversationCanceller;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public abstract class ErrorHandler {

    private static final String PREFIX = ChatColor.AQUA + "<ASS NAME>: " + ChatColor.GRAY;

    // TODO: for the love of god, finish this
    private static final String[] INTRO_LINES = {
            "Oi baka, you did some shit wrong"
    };

    private final ParameterTransformer transformer;

    private final ParamData parameter;

    private Exception exception;

    protected ErrorHandler(ParameterTransformer transformer, ParamData parameter, Exception ex) {
        this(transformer, parameter);
        this.exception = ex;
    }

    protected ErrorHandler(ParameterTransformer transformer, ParamData parameter) {
        this.transformer = transformer;
        this.parameter = parameter;
    }

    public abstract boolean verify(Object object);

    void startConversation(Player sender, Runnable next) {
        ConversationFactory factory = new ConversationFactory(FeatherPlugin.get());
        factory.addConversationAbandonedListener(event -> {
            if (event.getCanceller() instanceof InactivityConversationCanceller) {
                event.getContext().getForWhom().sendRawMessage("Command cancelled due to 30 seconds of inactivity.");
            } else if (event.gracefulExit()) {
                next.run();
            }
        });
        factory.withEscapeSequence("cancel");
        factory.withTimeout(30);
        factory.withModality(true);
        factory.withLocalEcho(false);
        factory.withPrefix(context -> PREFIX);
        String message = getParameter().getName() + " requires a valid";
        factory.withFirstPrompt(new RecursiveParameterRequestPrompt(this, message));
        sender.sendRawMessage(PREFIX + INTRO_LINES[ThreadLocalRandom.current().nextInt(INTRO_LINES.length)]);
        Conversation convo = factory.buildConversation(sender);
        convo.begin();
    }

    public Exception getException() {
        return this.exception;
    }

    public void setException(Exception ex) {
        this.exception = ex;
    }

    public boolean hasException() {
        return this.exception == null;
    }

    public ParamData getParameter() {
        return this.parameter;
    }

    public List<String> getOptions() {
        return Collections.emptyList();
    }

    private class RecursiveParameterRequestPrompt extends StringPrompt {

        private final ErrorHandler handler;

        private final String message;

        RecursiveParameterRequestPrompt(ErrorHandler handler, String message) {
            this.handler = handler;
            this.message = message;
        }

        @Override
        public String getPromptText(ConversationContext conversationContext) {
            return this.message;
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            Object o = null;
            try {
                o = this.handler.transformer.transform((Player) context.getForWhom(), input);
            } catch (Exception ignored) {
            }

            if (this.handler.verify(o)) {
                return Prompt.END_OF_CONVERSATION;
            }
            return this;
        }
    }
}
