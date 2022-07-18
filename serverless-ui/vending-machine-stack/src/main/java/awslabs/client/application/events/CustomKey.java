package awslabs.client.application.events;

import awslabs.client.ssm.SsmWebSocket;
import com.google.gwt.event.dom.client.KeyCodeEvent;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import gwt.material.design.client.ui.MaterialToast;
import io.vavr.collection.List;

public class CustomKey {
    private static final boolean debug = false;
    public static GwtEvent.Type<Handler> TYPE = new GwtEvent.Type<>();

    public static GwtEvent.Type<Handler> getType() {
        return TYPE;
    }

    public interface Handler extends EventHandler {
        void onCustomKey(Event customKeyEvent);
    }

    public static class Event extends GwtEvent<CustomKey.Handler> {
        public static final int LOWERCASE_CHARACTER_OFFSET = 32;
        // Characters to pass through if they do not have any modifiers
        private static final List<Integer> PASSTHROUGH_WITHOUT_MODIFIERS = List.of(
                13, // ENTER
                32, // SPACE
                9   // TAB
        );
        public final SsmWebSocket ssmWebSocket;
        private final boolean shift;
        private final int value;
        private final boolean alt;
        private final boolean control;
        private final boolean meta;
        private final boolean anyModifier;

        public Event(SsmWebSocket ssmWebSocket, KeyCodeEvent<?> keyCodeEvent) {
            this.ssmWebSocket = ssmWebSocket;
            this.value = keyCodeEvent.getNativeKeyCode();
            this.shift = keyCodeEvent.isShiftKeyDown();
            this.alt = keyCodeEvent.isAltKeyDown();
            this.control = keyCodeEvent.isControlKeyDown();
            this.meta = keyCodeEvent.isMetaKeyDown();
            this.anyModifier = keyCodeEvent.isAnyModifierKeyDown();

            if (debug) {
                MaterialToast.fireToast(String.join(", ",
                        "value: " + this.value,
                        "shift: " + this.shift,
                        "alt: " + this.alt,
                        "control: " + this.control,
                        "meta: " + this.meta
                ));
            }
        }

        public List<Integer> getAsciiValue() {
            List<Integer> asciiValues = handleLetter()
                    .orElse(this::handlePassthrough)
                    .orElse(this::handleSpecialKeys);

            if (debug) {
                MaterialToast.fireToast("converted value: " + String.join(", ", asciiValues.map(Object::toString)));
            }

            return asciiValues;
        }

        private List<Integer> handleSpecialKeys() {
            if (!anyModifier) {
                return handleSpecialNoModifierKeys();
            }

            if (onlyShift()) {
                return handleSpecialShiftOnlyKeys();
            }

            return List.empty();
        }

        private List<Integer> handleSpecialNoModifierKeys() {
            if ((value == 219) || (value == 221)) {
                // [ and ]
                return List.of(value - 128);
            }

            if (value == 8) {
                // Backspace, translate to CTRL-?
                return List.of(127);
            }

            // Arrow keys reference: https://en.wikipedia.org/wiki/ANSI_escape_code

            if (value == 37) {
                // Left arrow
                // Escape, left bracket, D
                return List.of(27, 91, 68);
            }

            if (value == 38) {
                // Up arrow
                // Escape, left bracket, A
                return List.of(27, 91, 65);
            }

            if (value == 39) {
                // Right arrow
                // Escape, left bracket, C
                return List.of(27, 91, 67);
            }

            if (value == 40) {
                // Down arrow
                // Escape, left bracket, B
                return List.of(27, 91, 66);
            }

            if (value == 222) {
                // Quote
                return List.of(39);
            }

            if (value > 144) {
                MaterialToast.fireToast("special key: " + value + " -> " + (value - 144));
                return List.of(value - 144);
            }

            return List.empty();
        }

        private List<Integer> handleSpecialShiftOnlyKeys() {
            if ((value >= 219) && (value <= 221)) {
                // {, }, and |
                return List.of(value - 96);
            }

            if (value == 52) {
                // Dollar sign
                return List.of(36);
            }

            if (value == 189) {
                // Underscore
                return List.of(95);
            }

            if (value == 222) {
                // Double quote
                return List.of(34);
            }

            if (value > 128) {
                MaterialToast.fireToast("SHIFT special key: " + value + " -> " + (value - 128));
                return List.of(value - 128);
            }

            return List.empty();
        }

        private List<Integer> handlePassthrough() {
            if (anyModifier) {
                // Ignore keys with any modifiers for now
                return List.empty();
            }

            if (!PASSTHROUGH_WITHOUT_MODIFIERS.contains(value)) {
                return List.empty();
            }

            return List.of(value);
        }

        private List<Integer> handleLetter() {
            if (!isLetter()) {
                return List.empty();
            }

            if (!anyModifier) {
                // No modifiers, this is just a lowercase character
                return List.of(value + LOWERCASE_CHARACTER_OFFSET);
            }

            if (onlyShift()) {
                // Just shift, this is just an uppercase character
                return List.of(value);
            }

            if (onlyControlOrControlShift()) {
                // Control, this is a control + letter combo
                // Guidance for calculation from: https://www.eso.org/~ndelmott/ascii.html
                return List.of(value - 'A' + 1);
            }

            // No match
            return List.empty();
        }

        private boolean onlyShift() {
            return (shift && !alt && !control && !meta);
        }

        private boolean onlyControlOrControlShift() {
            // Ignore shift when looking for control + letter combos
            return (!alt && control && !meta);
        }

        private boolean isLetter() {
            return ((this.value >= 'A') && (this.value <= 'Z'));
        }

        @Override
        public Type<Handler> getAssociatedType() {
            return getType();
        }

        @Override
        protected void dispatch(Handler handler) {
            handler.onCustomKey(this);
        }
    }
}
